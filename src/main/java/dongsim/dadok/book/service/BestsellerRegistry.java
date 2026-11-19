package dongsim.dadok.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// YES24 월간 베스트셀러(책 검색 화면의 "베스트셀러순"과 같은 목록) 상위 책을 주기적으로 받아서
// DB에 있는 같은 ISBN의 책을 베스트셀러로 표시한다. 요청 처리 중에는 YES24를 부르지 않는다.
@Component
public class BestsellerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(BestsellerRegistry.class);
    private static final int PAGE_SIZE = 20;
    private static final int PAGES = 10; // 상위 200권

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BookRepository bookRepository;
    private final TransactionTemplate transactionTemplate;
    private final String apiKey;
    private final String monthlyUrl;
    private final String categoryId;

    // 마지막으로 받은 베스트셀러 ISBN (서재에 새로 담는 책 표시용)
    private volatile Set<String> isbns = Set.of();

    public BestsellerRegistry(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            BookRepository bookRepository,
            TransactionTemplate transactionTemplate,
            @Value("${yes24.api.key}") String apiKey,
            @Value("${yes24.api.bestseller-monthly-url:https://apis.yes24.com/v1/category/bestsellerMonthly}") String monthlyUrl,
            @Value("${yes24.api.bestseller-category-id}") String categoryId
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.bookRepository = bookRepository;
        this.transactionTemplate = transactionTemplate;
        this.apiKey = apiKey;
        this.monthlyUrl = monthlyUrl;
        this.categoryId = categoryId;
    }

    // 서버 시작 1분 뒤, 그 뒤로 6시간마다
    @Scheduled(initialDelay = 60_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void refresh() {
        Set<String> fetched = fetchIsbns();
        if (fetched.isEmpty()) return; // 받지 못했으면 이전 목록을 그대로 쓴다
        isbns = Set.copyOf(fetched);
        Integer marked = transactionTemplate.execute(status -> {
            int count = 0;
            for (Book book : bookRepository.findAllByIsbn13In(fetched)) {
                if (!book.isBestseller()) {
                    book.markBestseller();
                    count++;
                }
            }
            return count;
        });
        LOG.info("베스트셀러 목록 갱신: {}권, 새로 표시 {}권", fetched.size(), marked);
    }

    // 지금 베스트셀러 목록에 있는 ISBN 인지 (YES24를 부르지 않고 마지막으로 받은 목록으로 판단)
    public boolean contains(String isbn13) {
        return isbn13 != null && isbns.contains(isbn13);
    }

    private Set<String> fetchIsbns() {
        Set<String> result = new HashSet<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = monthlyUrl + "?categoryId={categoryId}&page={page}&pageSize=" + PAGE_SIZE;
        for (int page = 1; page <= PAGES; page++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class,
                        Map.of("categoryId", categoryId, "page", String.valueOf(page)));
                JsonNode items = objectMapper.readTree(response.getBody()).path("data").path("items");
                if (!items.isArray() || items.isEmpty()) break;
                for (JsonNode item : items) {
                    String isbn = item.path("isbn13").asText("");
                    if (!isbn.isBlank()) result.add(isbn);
                }
            } catch (Exception e) {
                LOG.warn("베스트셀러 목록을 받지 못했습니다 (page={})", page, e);
                break;
            }
        }
        return result;
    }
}
