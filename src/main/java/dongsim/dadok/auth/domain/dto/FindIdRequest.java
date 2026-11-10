package dongsim.dadok.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FindIdRequest(
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotBlank @Email String email) {
}
