package dongsim.dadok.library.personal.domain.dto;

import java.time.LocalDate;
import java.util.List;

public record LibraryPageResponse(List<LibraryBookResponse> books, List<LocalDate> calendar) {
}
