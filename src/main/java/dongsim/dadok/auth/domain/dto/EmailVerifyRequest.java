package dongsim.dadok.auth.domain.dto;

import dongsim.dadok.auth.domain.entity.Verification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String code,
        @NotNull Verification type) {
}
