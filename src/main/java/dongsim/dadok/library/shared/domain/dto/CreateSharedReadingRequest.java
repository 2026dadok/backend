package dongsim.dadok.library.shared.domain.dto;

import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSharedReadingRequest(
        @NotNull Long bookId,
        @NotBlank @Email String partnerEmail,
        @NotBlank String partnerName
) {
}
