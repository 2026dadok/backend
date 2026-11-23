package dongsim.dadok.achievement.domain.entity;

// 업적 달성 조건 종류
public enum AchievementCondition {
    FINISH_COUNT,           // 완독 권수
    FINISH_BESTSELLER,      // 베스트셀러 완독 권수
    FINISH_STEADYSELLER,    // 사용 안 함 (스테디셀러 업적 삭제, 기존 DB 행을 읽고 지우기 위해 남겨 둠)
    FINISH_BOOK,            // 특정 도서 완독
    FIRST_READ,             // 첫 독서
    NIGHT_READ,             // 23시 ~ 05시 독서
    ONE_SESSION_FINISH,     // 첫 책을 한 번에 완독
    REVIEW_EVERY_SESSION,   // 완독까지 매 독서마다 감상문 작성
    FEELING_COUNT,          // 감상문 작성 개수
    QUOTE_COUNT,            // 인용구 저장 개수
    STREAK,                 // 연속 독서일
    RESUME_READ,            // 읽던 책 이어 읽기
    REREAD,                 // 완독한 책 다시 읽기
    WEEKLY_RANK_ENTER,      // 주간 경쟁 N위 안에 진입
    WEEKLY_RANK_FINISH,     // 주간 경쟁 N위 안으로 마무리
    JOINT_FINISH,           // 공동 서재 함께 완독
    JOINT_WIN,              // 공동 서재 먼저 완독
    TIER_REACH              // 티어 달성 (1: 동 ~ 6: 파이데이아)
}
