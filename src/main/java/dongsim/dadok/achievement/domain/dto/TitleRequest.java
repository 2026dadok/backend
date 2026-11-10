package dongsim.dadok.achievement.domain.dto;

import jakarta.validation.constraints.NotNull;

// 요청: { "titleId": 3 }
public record TitleRequest(
        @NotNull Long titleId
) {}
