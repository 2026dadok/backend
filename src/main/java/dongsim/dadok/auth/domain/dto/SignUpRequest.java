package dongsim.dadok.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank String userId,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
        @NotNull Integer defaultTimerTime) {
}
