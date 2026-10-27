package dongsim.dadok.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDate;

public record FindIdRequest(
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotBlank @Email String email) {
}
