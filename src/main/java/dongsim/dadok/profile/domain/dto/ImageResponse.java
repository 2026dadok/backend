package dongsim.dadok.profile.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 응답: 200, message, image_url
public record ImageResponse(
        String message,
        @JsonProperty("image_url") String imageUrl
) {}
