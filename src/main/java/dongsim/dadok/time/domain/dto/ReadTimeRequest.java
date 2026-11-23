package dongsim.dadok.time.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

// 요청: { "bookId": 3, "readTime": 1800 }  (readTime: 초)
//   또는 { "bookID": 3, "readtime": 30 }   (readtime: 분)
public record ReadTimeRequest(
        @JsonAlias("bookID") @NotNull Long bookId,
        @JsonProperty("readTime") Integer readTime,      // 초
        @JsonProperty("readtime") Integer readMinutes    // 분
) {
    // 읽은 시간 (초) - isValidTime 검증 후 사용
    @JsonIgnore
    public int seconds() {
        return (int) rawSeconds();
    }

    // 1초 ~ 6시간 (분 → 초 변환 시 오버플로 방지를 위해 long으로 계산)
    @JsonIgnore
    @AssertTrue(message = "읽은 시간은 1초 ~ 6시간이어야 합니다.")
    public boolean isValidTime() {
        long s = rawSeconds();
        return s >= 1 && s <= 21600;
    }

    private long rawSeconds() {
        if (readTime != null) return readTime;
        return readMinutes == null ? 0 : readMinutes * 60L;
    }
}
