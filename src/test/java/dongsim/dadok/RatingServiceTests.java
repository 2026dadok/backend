package dongsim.dadok;

import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import dongsim.dadok.rating.domain.dto.CreateRatingRequest;
import dongsim.dadok.rating.domain.entity.Rating;
import dongsim.dadok.rating.repository.RatingRepository;
import dongsim.dadok.rating.service.RatingService;
import dongsim.dadok.rating.domain.projection.RatingSummaryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RatingServiceTests {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private BookRepository bookRepository;

    @Test
    void 사용자는_같은_책에_리뷰를_두번_작성할_수_없다() {
        Book book = mock(Book.class);
        when(book.getBookId()).thenReturn(72L);
        when(bookRepository.findById(72L)).thenReturn(Optional.of(book));
        when(ratingRepository.existsByBookBookIdAndUserId(72L, "user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service().create("user@example.com", new CreateRatingRequest(72L, "좋은 책입니다.", 5)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void 리뷰를_작성한다() {
        Book book = mock(Book.class);
        when(book.getBookId()).thenReturn(72L);
        when(bookRepository.findById(72L)).thenReturn(Optional.of(book));
        when(ratingRepository.existsByBookBookIdAndUserId(72L, "user@example.com")).thenReturn(false);
        when(ratingRepository.save(ArgumentMatchers.any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().create("user@example.com", new CreateRatingRequest(72L, "좋은 책입니다.", 5));

        verify(ratingRepository).save(ArgumentMatchers.any(Rating.class));
    }

    @Test
    void 리뷰_평균과_개수를_소수점_한자리로_반환한다() {
        RatingSummaryProjection summary = mock(RatingSummaryProjection.class);
        when(summary.getAverageRating()).thenReturn(4.3333);
        when(summary.getReviewCount()).thenReturn(3L);
        when(ratingRepository.getSummaryByBookId(72L)).thenReturn(summary);

        var result = service().getSummary(72L);

        org.assertj.core.api.Assertions.assertThat(result.averageRating()).isEqualTo(4.3);
        org.assertj.core.api.Assertions.assertThat(result.reviewCount()).isEqualTo(3L);
    }

    private RatingService service() {
        return new RatingService(ratingRepository, bookRepository);
    }
}
