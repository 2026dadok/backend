package dongsim.dadok.ai.service;

import dongsim.dadok.ai.domain.dto.AiMessage;
import dongsim.dadok.review.domain.dto.request.AiReviewSaveRequest;
import dongsim.dadok.review.domain.dto.response.AiReviewSaveResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class AiReviewIdempotencyStore {

    private static final Duration COMPLETED_TTL = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, CachedResponse> completedResponses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AiReviewSaveResponse>> inFlight =
            new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong();

    public AiReviewSaveResponse execute(
            String userId,
            String idempotencyKey,
            AiReviewSaveRequest request,
            Supplier<AiReviewSaveResponse> action
    ) {
        cleanupExpiredResponses();
        String cacheKey = buildCacheKey(userId, idempotencyKey, request);
        CachedResponse cached = completedResponses.get(cacheKey);
        if (cached != null) {
            if (!cached.isExpired(System.currentTimeMillis())) {
                return cached.response();
            }
            completedResponses.remove(cacheKey, cached);
        }

        CompletableFuture<AiReviewSaveResponse> newFuture = new CompletableFuture<>();
        CompletableFuture<AiReviewSaveResponse> existing = inFlight.putIfAbsent(cacheKey, newFuture);
        if (existing != null) {
            return await(existing);
        }

        try {
            AiReviewSaveResponse response = action.get();
            completedResponses.put(cacheKey, new CachedResponse(response));
            newFuture.complete(response);
            return response;
        } catch (RuntimeException e) {
            newFuture.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(cacheKey, newFuture);
        }
    }

    private void cleanupExpiredResponses() {
        long now = System.currentTimeMillis();
        long previous = lastCleanupAt.get();
        if (now - previous < 60_000L || !lastCleanupAt.compareAndSet(previous, now)) {
            return;
        }

        completedResponses.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private AiReviewSaveResponse await(CompletableFuture<AiReviewSaveResponse> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private String buildCacheKey(
            String userId,
            String idempotencyKey,
            AiReviewSaveRequest request
    ) {
        if (StringUtils.hasText(idempotencyKey)) {
            return "user:" + userId + ":header:" + idempotencyKey.trim();
        }

        return "user:" + userId + ":fingerprint:" + fingerprint(request);
    }

    private String fingerprint(AiReviewSaveRequest request) {
        StringBuilder raw = new StringBuilder()
                .append(request.libBookId()).append('\n')
                .append(request.bookTitle()).append('\n')
                .append(request.author()).append('\n');
        for (AiMessage message : request.messages()) {
            raw.append(message.role()).append('\n')
                    .append(message.content()).append('\n');
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Idempotency fingerprint를 생성할 수 없습니다.", e);
        }
    }

    private record CachedResponse(
            AiReviewSaveResponse response,
            long expiresAt
    ) {
        private CachedResponse(AiReviewSaveResponse response) {
            this(response, System.currentTimeMillis() + COMPLETED_TTL.toMillis());
        }

        private boolean isExpired(long now) {
            return now >= expiresAt;
        }
    }
}
