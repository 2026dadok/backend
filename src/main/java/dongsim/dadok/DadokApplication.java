package dongsim.dadok;

import dongsim.dadok.book.Book;
import dongsim.dadok.book.BookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DadokApplication {

    public static void main(String[] args) {
        SpringApplication.run(DadokApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(BookService bookService) {
        return args -> {
            System.out.println("\n=== 도서 제목 검색 및 저장 테스트 시작 ===");

            // 1. 정상 도서 검색 (클린 코드)
            try {
                List<Book> books = bookService.searchBooksByTitle("클린 코드");
                System.out.println(">>> 총 " + books.size() + "권 검색 완료");
                for (Book b : books) {
                    System.out.println("- 제목: " + b.getBookTitle() + " | 저자: " + b.getAuthor() + " | 페이지: " + b.getPages());
                }
            } catch (IllegalArgumentException e) {
                System.out.println("결과 안내: " + e.getMessage());
            }

            // 2. 존재하지 않는 도서 검색 (404 예외 처리 테스트)
            try {
                bookService.searchBooksByTitle("없는책이름가나다라9999");
            } catch (IllegalArgumentException e) {
                System.out.println("결과 안내: " + e.getMessage());
            }

            System.out.println("=== 테스트 완료 ===\n");
        };
    }
}