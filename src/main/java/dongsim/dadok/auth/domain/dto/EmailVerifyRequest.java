package dongsim.dadok.auth.domain.dto;

import dongsim.dadok.auth.domain.entity.Verification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.antlr.v4.runtime.misc.NotNull;

public record EmailVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String code,
        @NotNull Verification purpose) {
}
