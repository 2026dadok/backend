package dongsim.dadok.league.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 순위 1줄
public record RankResponse(
        Integer rank,                                   // 0분이면 null ("-위")
        String nickname,
        String title,                                   // 사용 중인 칭호 (없으면 null)
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("total_seconds") long totalSeconds,
        @JsonProperty("is_me") boolean isMe
) {}
