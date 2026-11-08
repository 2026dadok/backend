package dongsim.dadok.library.personal.domain.dto;

import jakarta.validation.constraints.NotNull;

public record AddLibraryBookRequest(@NotNull Long bookId) {
}
