package dongsim.dadok.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SignUpRequest(
        @NotBlank String userId,
        @NotBlank @Email String email,

        @JsonProperty("ID")
        @NotBlank String loginId,

        @JsonProperty("passward")
        @NotBlank String password,

        @NotBlank String name,

        @JsonProperty("birthdate")
        @NotNull LocalDate birthDate,

        @JsonProperty("goal_time")
        @NotNull LocalTime goalTime
) {
    public Integer defaultTimerTime() {
        return goalTime.toSecondOfDay();
    }
}
