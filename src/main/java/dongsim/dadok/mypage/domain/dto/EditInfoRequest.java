package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
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
        @JsonProperty("goal_time") @NotBlank @Pattern(regexp = "^\\d{1,2}:\\d{2}$") String goalTime,
        @JsonProperty("current_password") String currentPassword,
        @JsonProperty("new_password") @JsonAlias({"passward", "password"}) String newPassword
) {
    // "01:05" → 3900초 - isValidGoalTime 검증 후 사용
    public int goalSeconds() {
        String[] p = goalTime.split(":");
        return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60;
    }

    // 분은 0~59, 전체 1분 ~ 24시간
    @JsonIgnore
    @AssertTrue(message = "목표 시간은 00:01 ~ 24:00 사이여야 합니다.")
    public boolean isValidGoalTime() {
        if (goalTime == null || !goalTime.matches("^\\d{1,2}:\\d{2}$")) return true; // 형식 오류는 @Pattern이 처리
        String[] p = goalTime.split(":");
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        int total = h * 60 + m;
        return m < 60 && total >= 1 && total <= 1440;
    }
}
