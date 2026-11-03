package dongsim.dadok.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    private static final String API_URL = "https://apis.yes24.com/v1";
    private static final String API_KEY = "yk_live_92c5590aa345c4a122f2aeef876f3b1e4f109d9a0ace3526";

    @Transactional
    public void fetchAndSaveBooks() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.GET, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // 응답 형태에 맞게 목록 추출
            JsonNode dataNode = root.path("data");
            JsonNode items = dataNode.isArray() ? dataNode : (dataNode.has("items") ? dataNode.path("items") : dataNode.path("goodsList"));

            System.out.println("가져온 도서 건수: " + items.size());

            for (JsonNode item : items) {
                // 1. 도서 제목
                String title = item.path("title").asText(item.path("goodsName").asText("제목 없음"));
                if (title.length() > 200) title = title.substring(0, 200);

                // 2. 표지 이미지
                String coverUrl = item.hasNonNull("imageUrl") ? item.path("imageUrl").asText() : item.path("image").asText(null);

                // 3. 출판사
                String publisher = item.path("publisher").asText(item.path("company").asText("미상"));
                if (publisher.length() > 100) publisher = publisher.substring(0, 100);

                // 4. 저자
                String author = item.path("author").asText("미상");
                if (author.length() > 100) author = author.substring(0, 100);

                // 5. 장르
                String genre = item.path("categoryName").asText("소설");
                if (genre.length() > 50) genre = genre.substring(0, 50);

                // 6. ISBN13 (중복 체크)
                String isbn13 = item.hasNonNull("isbn13") ? item.path("isbn13").asText() : item.path("isbn").asText(null);
                if (isbn13 != null && bookRepository.existsByIsbn13(isbn13)) {
                    continue;
                }

                // 7. 페이지 수
                int pages = item.path("pageCount").asInt(item.path("pages").asInt(0));

                // DB에 저장 실행
                Book book = new Book(title, coverUrl, publisher, author, genre, isbn13, pages);
                bookRepository.save(book);
            }

            System.out.println("=== 도서 데이터가 DB에 정상적으로 저장되었습니다! ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}