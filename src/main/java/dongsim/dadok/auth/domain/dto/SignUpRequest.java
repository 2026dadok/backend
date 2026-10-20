package dongsim.dadok.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotNull Integer defaultTimerTime) {
}
