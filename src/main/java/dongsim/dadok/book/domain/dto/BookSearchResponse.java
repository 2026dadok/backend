package dongsim.dadok.book.domain.dto;

import dongsim.dadok.book.domain.entity.Book;

import java.util.List;

public record BookSearchResponse(long total, List<Book> books) {
}
