package dongsim.dadok.book.service;

import dongsim.dadok.book.domain.dto.*;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private static final int SEARCH_PAGE_SIZE = 5;
    private static final int BESTSELLER_PAGE_SIZE = 20;

    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String yes24ApiUrl;
    private final String yes24DetailApiUrl;
    private final String yes24BestsellerApiUrl;
    private final String yes24BestsellerCategoryId;
    private final String yes24ApiKey;

    public BookService(
            BookRepository bookRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${yes24.api.url}") String yes24ApiUrl,
            @Value("${yes24.api.detail-url}") String yes24DetailApiUrl,
            @Value("${yes24.api.bestseller-url}") String yes24BestsellerApiUrl,
            @Value("${yes24.api.bestseller-category-id}") String yes24BestsellerCategoryId,
            @Value("${yes24.api.key}") String yes24ApiKey
    ) {
        this.bookRepository = bookRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.yes24ApiUrl = yes24ApiUrl;
        this.yes24DetailApiUrl = yes24DetailApiUrl;
        this.yes24BestsellerApiUrl = yes24BestsellerApiUrl;
        this.yes24BestsellerCategoryId = yes24BestsellerCategoryId;
        this.yes24ApiKey = yes24ApiKey;
    }

    @Transactional
    public BookSearchResponse searchBooks(String keyword, int page) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력해 주세요.");
        }

        String normalizedKeyword = keyword.trim();
        PageRequest pageRequest = PageRequest.of(page - 1, SEARCH_PAGE_SIZE);

        // 1. DB 우선 조회
        Page<Book> localBooks = bookRepository.findByBookTitleContaining(normalizedKeyword, pageRequest);
        if (!localBooks.isEmpty()) {
            return new BookSearchResponse(localBooks.getTotalElements(), localBooks.getContent());
        }

        // 2. DB에 없으면 YES24 API 호출
        Yes24SearchResult yes24Result = fetchBooksFromYes24(normalizedKeyword, page);
        List<Book> fetchedBooks = yes24Result.books();

        if (fetchedBooks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 조건에 맞는 책이 없습니다.");
        }

        return new BookSearchResponse(yes24Result.total(), saveBooks(fetchedBooks));
    }

    @Transactional
    public BookSearchResponse getBestsellers(int page) {
        Yes24SearchResult yes24Result = fetchBestsellersFromYes24(page);
        if (yes24Result.books().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "베스트셀러 결과가 없습니다.");
        }
        // YES24가 제공한 sortOrder 순서를 그대로 유지해 반환한다.
        return new BookSearchResponse(yes24Result.total(), saveBooks(yes24Result.books()));
    }

    // API 결과를 DB에 저장하되, 이미 저장된 ISBN은 기존 엔티티를 반환한다.
    private List<Book> saveBooks(List<Book> books) {
        List<Book> savedBooks = new ArrayList<>();
        for (Book book : books) {
            if (book.getIsbn13() == null || book.getIsbn13().isBlank()) {
                savedBooks.add(bookRepository.save(book));
                continue;
            }

            Book storedBook = bookRepository.findByIsbn13(book.getIsbn13()).orElse(null);
            if (storedBook != null) {
                if ((storedBook.getDescription() == null || storedBook.getDescription().isBlank())
                        && book.getDescription() != null && !book.getDescription().isBlank()) {
                    storedBook.updateDescription(book.getDescription());
                }
                savedBooks.add(storedBook);
                continue;
            }

            try {
                savedBooks.add(bookRepository.save(book));
            } catch (DataIntegrityViolationException e) {
                // 동시에 같은 ISBN이 저장된 경우 유니크 제약 오류 대신 기존 데이터를 반환한다.
                Book concurrentlySavedBook = bookRepository.findByIsbn13(book.getIsbn13())
                        .orElseThrow(() -> e);
                savedBooks.add(concurrentlySavedBook);
            }
        }

        return savedBooks;
    }

    @Transactional
    public Book getBookDetail(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            return book;
        }
        if (book.getIsbn13() == null || book.getIsbn13().isBlank()) {
            return book;
        }

        String description = fetchDescriptionFromYes24(book.getIsbn13());
        if (description != null && !description.isBlank()) {
            // 트랜잭션 종료 시 JPA 변경 감지가 description을 DB에 저장한다.
            book.updateDescription(description);
        }
        return book;
    }

    private Yes24SearchResult fetchBooksFromYes24(String searchTitle, int page) {
        List<Book> resultList = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", yes24ApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // detail=Y일 때 페이지 수와 도서 소개 등 확장 정보가 포함된다.
        String url = yes24ApiUrl + "?query={query}&detail=Y&page={page}&pageSize=" + SEARCH_PAGE_SIZE + "&category=BOOK";
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("query", searchTitle);
        uriVariables.put("page", String.valueOf(page));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    uriVariables
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                if ("SEARCH_001".equals(root.path("errorCode").asText())) {
                    return new Yes24SearchResult(0, resultList);
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 도서 검색에 실패했습니다.");
            }

            long totalCount = root.path("data").path("totalCount").asLong(0);
            JsonNode items = root.path("data").path("items");
            if (!items.isArray() || items.isEmpty()) {
                return new Yes24SearchResult(totalCount, resultList);
            }

            for (JsonNode item : items) {
                resultList.add(toBook(item));
            }

            return new Yes24SearchResult(totalCount, resultList);

        } catch (HttpClientErrorException.NotFound e) {
            return new Yes24SearchResult(0, resultList);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 도서 검색에 실패했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 응답을 처리하지 못했습니다.", e);
        }

    }

    private Yes24SearchResult fetchBestsellersFromYes24(int page) {
        List<Book> resultList = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", yes24ApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = yes24BestsellerApiUrl
                + "?categoryId={categoryId}&page={page}&pageSize=" + BESTSELLER_PAGE_SIZE + "&detail=Y";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    Map.of("categoryId", yes24BestsellerCategoryId, "page", String.valueOf(page))
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 베스트셀러 조회에 실패했습니다.");
            }

            long totalCount = root.path("data").path("totalCount").asLong(0);
            JsonNode items = root.path("data").path("items");
            if (!items.isArray() || items.isEmpty()) {
                return new Yes24SearchResult(totalCount, resultList);
            }
            for (JsonNode item : items) {
                resultList.add(toBook(item));
            }
            return new Yes24SearchResult(totalCount, resultList);
        } catch (HttpClientErrorException.NotFound e) {
            return new Yes24SearchResult(0, resultList);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 베스트셀러 조회에 실패했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 베스트셀러 응답을 처리하지 못했습니다.", e);
        }
    }

    private Book toBook(JsonNode item) {
        String bookTitle = item.path("title").asText("제목 없음");
        if (bookTitle.length() > 200) bookTitle = bookTitle.substring(0, 200);

        String coverUrl = item.hasNonNull("cover") ? item.path("cover").asText() : null;
        String publisher = item.path("publisher").asText("미상");
        if (publisher.length() > 100) publisher = publisher.substring(0, 100);

        String author = item.path("author").asText("미상");
        if (author.length() > 100) author = author.substring(0, 100);

        String genre = item.path("goodsSortNm").asText(item.path("goodsType").asText("기타"));
        if (genre.length() > 50) genre = genre.substring(0, 50);

        String isbn13 = item.hasNonNull("isbn13") ? item.path("isbn13").asText() : null;
        int pages = item.path("pages").asInt(0);
        JsonNode contentDetail = item.path("contentDetail");
        String description = contentDetail.hasNonNull("bookIntroduction")
                ? contentDetail.path("bookIntroduction").asText()
                : null;

        return new Book(bookTitle, coverUrl, publisher, author, genre, isbn13, pages, description);
    }

    private String fetchDescriptionFromYes24(String isbn13) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", yes24ApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = yes24DetailApiUrl + "?searchType=ISBN13&query={isbn13}&detail=Y";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    Map.of("isbn13", isbn13)
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 도서 상세 조회에 실패했습니다.");
            }

            JsonNode items = root.path("data").path("items");
            if (!items.isArray() || items.isEmpty()) {
                return null;
            }

            JsonNode contentDetail = items.get(0).path("contentDetail");
            String introduction = contentDetail.path("bookIntroduction").asText("");
            return introduction.isBlank() ? contentDetail.path("bookSummary").asText(null) : introduction;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 도서 상세 조회에 실패했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YES24 상세 응답을 처리하지 못했습니다.", e);
        }
    }

    private record Yes24SearchResult(long total, List<Book> books) {
    }
}
