package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

// 요청: name, birthdate, goal_time("HH:mm"), current_password, new_password(passward)
// 비밀번호를 바꾸지 않으면 current_password, new_password는 빼고 보냄
public record EditInfoRequest(
        @JsonProperty("name") @NotBlank String name,
        @JsonProperty("birthdate") @NotNull @Past LocalDate birthdate,       // yyyy-MM-dd
        @JsonProperty("goal_time") @NotBlank @Pattern(regexp = "^\\d{1,2}:\\d{1,2}(:\\d{1,2})?$") String goalTime,
        @JsonProperty("current_password") String currentPassword,
        @JsonProperty("new_password") @JsonAlias({"passward", "password"}) String newPassword
) {
    // "01:05" → 3900초
    public int goalSeconds() {
        String[] p = goalTime.split(":");
        return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60;
    }
}
