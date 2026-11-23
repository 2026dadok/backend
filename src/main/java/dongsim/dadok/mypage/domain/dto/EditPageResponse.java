package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

// 응답: 200, ID, name, birthdate, goal_time, email, profile_image, background_image
public record EditPageResponse(
        @JsonProperty("ID") String loginId,
        @JsonProperty("name") String name,
        @JsonProperty("birthdate") LocalDate birthdate,   // yyyy-MM-dd
        @JsonProperty("goal_time") String goalTime,       // 목표 독서 시간 "HH:mm"
        @JsonProperty("email") String email,              // 표시용, 수정 불가
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("background_image") String backgroundImage
) {}
