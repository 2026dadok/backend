package dongsim.dadok.quest.domain.entity;

// 퀘스트 완료 조건 종류
public enum QuestCondition {
    ATTENDANCE,     // 일일 출석하기 (버튼 클릭)
    READ_MINUTES,   // N분 독서하기 (타이머)
    BOOK_REGISTER,  // 새로운 도서 등록하기
    ALL_DAILY,      // 일일 퀘스트 전체 완료 보상
    READ_PAGES      // 이번 주 N페이지 읽기
}
