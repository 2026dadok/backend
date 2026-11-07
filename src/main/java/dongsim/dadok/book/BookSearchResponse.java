package dongsim.dadok.book;

import java.util.List;

public record BookSearchResponse(long total, List<Book> books) {
}
