package dongsim.dadok.library.shared.domain.dto;

import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgressRequest(@NotNull @Min(0) Integer currentPage) {
}
