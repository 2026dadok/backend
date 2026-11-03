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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    private static final String API_KEY = "yk_live_92c5590aa345c4a122f2aeef876f3b1e4f109d9a0ace3526";

    @Transactional
    public List<Book> searchBooksByTitle(String title) {
        // 1. DB 우선 조회
        List<Book> localBooks = bookRepository.findByBookTitleContaining(title);
        if (!localBooks.isEmpty()) {
            System.out.println(">>> [DB 검색 성공] " + localBooks.size() + "건 발견");
            return localBooks;
        }

        // 2. DB에 없으면 YES24 API 호출
        System.out.println(">>> [DB에 없음] YES24 API 조회 시작: " + title);
        List<Book> fetchedBooks = fetchBooksFromYes24(title);

        if (fetchedBooks.isEmpty()) {
            throw new IllegalArgumentException("해당 조건에 맞는 책이 없습니다.");
        }

        // 3. API 검색 결과 DB 저장 (ISBN 중복 체크)
        List<Book> savedBooks = new ArrayList<>();
        for (Book book : fetchedBooks) {
            if (book.getIsbn13() != null && bookRepository.existsByIsbn13(book.getIsbn13())) {
                continue;
            }
            savedBooks.add(bookRepository.save(book));
        }

        return savedBooks.isEmpty() ? fetchedBooks : savedBooks;
    }

    private List<Book> fetchBooksFromYes24(String searchTitle) {
        List<Book> resultList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 공식 규격: query, detail=Y, pageSize=20, category=BOOK
        String url = "https://apis.yes24.com/v1/goods/itemList?query={query}&detail=Y&pageSize=20&category=BOOK";
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("query", searchTitle);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    uriVariables
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            JsonNode items = root.path("data").path("items");
            if (!items.isArray() || items.isEmpty()) {
                return resultList;
            }

            for (JsonNode item : items) {
                // 1. 책 제목
                String bookTitle = item.path("title").asText("제목 없음");
                if (bookTitle.length() > 200) bookTitle = bookTitle.substring(0, 200);

                // 2. cover_url
                String coverUrl = item.hasNonNull("cover") ? item.path("cover").asText() : null;

                // 3. 출판사
                String publisher = item.path("publisher").asText("미상");
                if (publisher.length() > 100) publisher = publisher.substring(0, 100);

                // 4. 작가
                String author = item.path("author").asText("미상");
                if (author.length() > 100) author = author.substring(0, 100);

                // 5. 장르
                String genre = item.path("goodsSortNm").asText(item.path("goodsType").asText("기타"));
                if (genre.length() > 50) genre = genre.substring(0, 50);

                // 6. isbn13 코드
                String isbn13 = item.hasNonNull("isbn13") ? item.path("isbn13").asText() : null;

                // 7. 책 페이지 수
                int pages = item.path("pages").asInt(0);

                // 8. 책 소개
                JsonNode contentDetail = item.path("contentDetail");
                String description = contentDetail.hasNonNull("bookIntroduction")
                        ? contentDetail.path("bookIntroduction").asText()
                        : null;

                resultList.add(new Book(bookTitle, coverUrl, publisher, author, genre, isbn13, pages, description));
            }

        } catch (HttpClientErrorException.NotFound e) {
            // YES24 규격: 검색 결과가 없으면 404 SEARCH_001 응답이 오므로 빈 리스트 반환
            System.out.println("YES24 검색 결과 없음 (404 Not Found)");
            return resultList;
        } catch (Exception e) {
            System.err.println("YES24 API 호출 오류: " + e.getMessage());
        }

        return resultList;
    }
}