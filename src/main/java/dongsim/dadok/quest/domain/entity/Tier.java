package dongsim.dadok.quest.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 티어: 누적 XP 기준
@Getter
@RequiredArgsConstructor
public enum Tier {
    BRONZE("동", 0),
    SILVER("실버", 1001),
    GOLD("금", 5001),
    PLATINUM("백금", 10001),
    DIAMOND("금강", 50001),
    PAIDEIA("파이데이아", 100001);

    private final String displayName;
    private final int minXp;

    public static Tier of(int xp) {
        Tier result = BRONZE;
        for (Tier tier : values()) {
            if (xp >= tier.minXp) result = tier;
        }
        return result;
    }

    // 1부터 시작하는 단계 (동 = 1, 파이데이아 = 6)
    public int level() {
        return ordinal() + 1;
    }
}
