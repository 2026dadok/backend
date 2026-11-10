package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

// 요청: name, birthdate, goal_time, current_password, new_password
// 비밀번호를 바꾸지 않으면 current_password, new_password는 비워서 보냄
public record EditInfoRequest(
        @JsonProperty("name") @NotBlank String name,
        @JsonProperty("birthdate") @NotNull @Past LocalDate birthdate,       // yyyy-MM-dd
        @JsonProperty("goal_time") @NotNull @Min(1) @Max(1440) Integer goalTime, // 목표 독서 시간 (분)
        @JsonProperty("current_password") String currentPassword,
        @JsonProperty("new_password") String newPassword
) {}
