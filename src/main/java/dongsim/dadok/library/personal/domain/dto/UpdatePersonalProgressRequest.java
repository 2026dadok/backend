package dongsim.dadok.library.personal.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePersonalProgressRequest(@NotNull @Min(0) @Max(20000) Integer currentPage) {
}
