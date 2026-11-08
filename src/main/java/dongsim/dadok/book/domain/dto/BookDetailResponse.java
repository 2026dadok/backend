package dongsim.dadok.book.domain.dto;

import dongsim.dadok.book.domain.entity.Book;

public record BookDetailResponse(
        Book book,
        double averageRating,
        long reviewCount
) {
}
