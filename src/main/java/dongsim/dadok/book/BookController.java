package dongsim.dadok.book;

import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@Validated
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/search")
    public ResponseEntity<BookSearchResponse> searchBooks(
            @RequestParam("keyword") @NotBlank String keyword,
            @RequestParam(value = "page", defaultValue = "1") @Positive int page
    ) {
        return ResponseEntity.ok(bookService.searchBooks(keyword, page));
    }

    @GetMapping("/bestsellers")
    public ResponseEntity<BookSearchResponse> getBestsellers(
            @RequestParam(value = "page", defaultValue = "1") @Positive int page
    ) {
        return ResponseEntity.ok(bookService.getBestsellers(page));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<Book> getBookDetail(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookDetail(bookId));
    }
}
