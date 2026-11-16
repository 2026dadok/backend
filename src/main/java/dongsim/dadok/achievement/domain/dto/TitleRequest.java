package dongsim.dadok.achievement.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

// 요청: { "titleId": 3 } 또는 { "titleID": 3 }
public record TitleRequest(
        @JsonAlias("titleID") @NotNull Long titleId
) {}
