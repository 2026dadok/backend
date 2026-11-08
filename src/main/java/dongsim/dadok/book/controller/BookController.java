package dongsim.dadok.book.controller;

import dongsim.dadok.book.domain.dto.*;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.service.BookService;
import dongsim.dadok.rating.domain.dto.*;
import dongsim.dadok.rating.service.RatingService;

import dongsim.dadok.rating.service.RatingService;
import dongsim.dadok.rating.domain.dto.RatingSummaryResponse;
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
    private final RatingService ratingService;

    public BookController(BookService bookService, RatingService ratingService) {
        this.bookService = bookService;
        this.ratingService = ratingService;
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
    public ResponseEntity<BookDetailResponse> getBookDetail(@PathVariable Long bookId) {
        Book book = bookService.getBookDetail(bookId);
        RatingSummaryResponse summary = ratingService.getSummary(bookId);
        return ResponseEntity.ok(new BookDetailResponse(book, summary.averageRating(), summary.reviewCount()));
    }
}
