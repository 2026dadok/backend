package dongsim.dadok.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// 요청: { "ID", "email", "password", "name", "birthdate": "yyyy-MM-dd", "goal_time": "HH:mm" }
public record SignUpRequest(
        @JsonProperty("ID") @JsonAlias("userId")
        @NotBlank String userId,

        @NotBlank @Email String email,

        @JsonProperty("password") @JsonAlias("passward")
        @NotBlank String password,

        @NotBlank String name,

        @JsonProperty("birthdate") @JsonAlias("birthDate")
        @NotNull LocalDate birthDate,

        @JsonProperty("goal_time")
        @NotNull LocalTime goalTime
) {
    // 목표 독서 시간 (초)
    public Integer defaultTimerTime() {
        return goalTime.toSecondOfDay();
    }
}
