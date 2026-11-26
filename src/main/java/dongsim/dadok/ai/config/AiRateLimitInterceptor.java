package dongsim.dadok.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dongsim.dadok.auth.domain.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/v1/reviews/interview", new Limit(10),
            "/api/v1/reviews/generate", new Limit(2)
    );

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AiRateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        Limit limit = LIMITS.get(request.getRequestURI());
        if (limit == null) {
            return true;
        }

        String path = request.getRequestURI();
        String userId = authenticatedUserId();
        if (StringUtils.hasText(userId)
                && !tryConsume("user:" + userId + ":" + path, limit.maxRequests())) {
            return reject(response);
        }

        String clientIp = request.getRemoteAddr();
        if (!tryConsume("ip:" + clientIp + ":" + path, limit.maxRequests())) {
            return reject(response);
        }

        return true;
    }

    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private boolean tryConsume(String key, int maxRequests) {
        AtomicBoolean allowed = new AtomicBoolean(true);
        long now = System.currentTimeMillis();

        windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            if (current.count >= maxRequests) {
                allowed.set(false);
                return current;
            }
            current.count++;
            return current;
        });

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry ->
                    now - entry.getValue().startedAt >= WINDOW_MILLIS
            );
        }
        return allowed.get();
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(
                        "AI 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
                        TOO_MANY_REQUESTS.value(),
                        "TOO_MANY_REQUESTS"
                )
        );
        return false;
    }

    private record Limit(int maxRequests) {
    }

    private static final class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
