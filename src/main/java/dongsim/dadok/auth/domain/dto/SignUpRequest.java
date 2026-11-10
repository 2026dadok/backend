package dongsim.dadok.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank String userId,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotNull Integer defaultTimerTime) {
}
