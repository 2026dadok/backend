package dongsim.dadok;

import dongsim.dadok.book.BookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DadokApplication {

    public static void main(String[] args) {
        SpringApplication.run(DadokApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(BookService bookService) {
        return args -> {
            System.out.println("=== YES24 도서 수집 및 DB 저장 시작 ===");
            bookService.fetchAndSaveBooks();
            System.out.println("=== 작업 완료 ===");
        };
    }
}
