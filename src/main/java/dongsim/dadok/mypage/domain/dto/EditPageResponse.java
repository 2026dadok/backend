package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

// 응답: 200, name, birthdate, goal_time, email
public record EditPageResponse(
        @JsonProperty("name") String name,
        @JsonProperty("birthdate") LocalDate birthdate,   // yyyy-MM-dd
        @JsonProperty("goal_time") int goalTime,          // 목표 독서 시간 (분)
        @JsonProperty("email") String email               // 표시용, 수정 불가
) {}
