package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 응답: 200, ID, name, title, all_reading_time, sequence_day, all_reading_page, league_in5,
//       profile_image, background_image(banner_image), achievements
public record MyPageResponse(
        @JsonProperty("ID") String loginId,
        @JsonProperty("name") String name,
        @JsonProperty("title") String title,                     // 사용 중인 칭호 (없으면 null)
        @JsonProperty("all_reading_time") long allReadingTime,   // 총 독서 시간 (초)
        @JsonProperty("sequence_day") int sequenceDay,           // 최대 연속 독서일
        @JsonProperty("all_reading_page") int allReadingPage,    // 총 독서 페이지
        @JsonProperty("league_in5") int leagueIn5,               // 주간 경쟁 5위권 진입 횟수
        @JsonProperty("profile_image") String profileImage,      // 프로필 사진 (없으면 기본 이미지)
        @JsonProperty("background_image") String backgroundImage, // 배경 사진 (없으면 기본 이미지)
        @JsonProperty("achievements") List<Achievement> achievements
) {
    @JsonProperty("banner_image")
    public String bannerImage() {
        return backgroundImage;
    }

    // 업적 1개: used = 칭호로 사용 중
    public record Achievement(Long id, String title, String description, boolean achieved, boolean used) {}
}
