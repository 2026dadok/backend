package dongsim.dadok.achievement.init;

import dongsim.dadok.achievement.domain.entity.Achievement;
import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.domain.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static dongsim.dadok.achievement.domain.entity.AchievementCondition.*;

// 서버가 켜질 때 업적 목록이 비어 있으면 자동으로 넣음
@Component
@RequiredArgsConstructor
public class AchievementInitializer implements ApplicationRunner {

    private final AchievementRepository achievementRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (achievementRepository.count() > 0) return;

        achievementRepository.saveAll(List.of(
                a("베스트셀러", "베스트셀러 3권을 완독했다.", FINISH_BESTSELLER, 3, null),
                a("스테디셀러", "스테디셀러 3권을 완독했다.", FINISH_STEADYSELLER, 3, null),
                a("목차 탈출 성공", "책을 처음 읽었다.", FIRST_READ, 1, null),
                a("완독의 맛", "책을 처음으로 완독했다.", FINISH_COUNT, 1, null),
                a("주경야독", "오후 11시 ~ 오전 5시 사이에 책을 읽는다.", NIGHT_READ, 1, null),
                a("똑똑이", "책 10권을 완독한다.", FINISH_COUNT, 10, null),
                a("영특이", "책 50권을 완독한다.", FINISH_COUNT, 50, null),
                a("비범이", "책 100권을 완독한다.", FINISH_COUNT, 100, null),
                a("지식의 나무", "책 1000권을 완독한다.", FINISH_COUNT, 1000, null),
                a("속독가", "처음 읽기 시작한 책을 타이머 작동 후 단번에 완독한다.", ONE_SESSION_FINISH, 1, null),
                a("정독가", "한 책을 완독할 때까지 독서 후 감상문을 쓴다.", REVIEW_EVERY_SESSION, 1, null),
                a("독서왕", "주간 경쟁 순위 10위권에 진입한다.", WEEKLY_RANK_ENTER, 10, null),
                a("다독왕", "주간 경쟁 순위 10위권으로 마무리한다.", WEEKLY_RANK_FINISH, 10, null),
                a("일일천하", "주간 경쟁 1위를 달성한다.", WEEKLY_RANK_ENTER, 1, null),
                a("책갈피", "읽다가 중단한 책을 다시 이어 읽는다.", RESUME_READ, 1, null),
                a("작심삼일", "3일 연속 책을 읽는다.", STREAK, 3, null),
                a("백일기도", "100일 연속 책을 읽는다.", STREAK, 100, null),
                a("문장 수집가", "마음에 드는 문장을 30개 저장한다.", QUOTE_COUNT, 30, null),
                a("첫 감상", "감상문을 처음 작성한다.", FEELING_COUNT, 1, null),
                a("다시 읽는 자", "읽었던 책을 다시 읽는다.", REREAD, 1, null),
                a("책친구", "공동 서재 멤버와 함께 완독을 끝낸다.", JOINT_FINISH, 1, null),
                a("승리자", "공동 서재에서 멤버보다 먼저 책을 읽는다.", JOINT_WIN, 1, null),
                a("브론즈", "동 등급을 달성한다.", TIER_REACH, 1, null),
                a("실버", "은 등급을 달성한다.", TIER_REACH, 2, null),
                a("골드", "금 등급을 달성한다.", TIER_REACH, 3, null),
                a("플래티넘", "백금 등급을 달성한다.", TIER_REACH, 4, null),
                a("다이아몬드", "금강 등급을 달성한다.", TIER_REACH, 5, null),
                a("파이데이아", "병연 등급을 달성한다.", TIER_REACH, 6, null),
                a("어느 날 갑자기", "프란츠 카프카의 변신을 완독했다.", FINISH_BOOK, null, "변신"),
                a("비범인", "표도르 도스토옙스키의 죄와 벌을 완독한다.", FINISH_BOOK, null, "죄와 벌"),
                a("알을 깨는 새", "헤르만 헤세의 데미안을 완독한다.", FINISH_BOOK, null, "데미안"),
                a("박제된 천재", "김해경의 날개를 완독한다.", FINISH_BOOK, null, "날개"),
                a("찢어진 마음", "에밀리 브론테의 폭풍의 언덕을 완독한다.", FINISH_BOOK, null, "폭풍의 언덕"),
                a("이어지는 꿈", "미겔 데 세르반테스의 돈키호테 중 1권을 완독한다.", FINISH_BOOK, null, "돈키호테"),
                a("오디세이", "호메로스의 오디세이아 중 1권을 완독한다.", FINISH_BOOK, null, "오디세이아"),
                a("순간이여 멈추어라", "요한 볼프강 폰 괴테의 파우스트를 완독한다.", FINISH_BOOK, null, "파우스트"),
                a("영혼의 구원", "단테 알리기에리의 신곡 중 1권을 완독한다.", FINISH_BOOK, null, "신곡")
        ));
    }

    private Achievement a(String title, String description, AchievementCondition condition,
                          Integer target, String book) {
        return Achievement.builder()
                .title(title).description(description).conditionType(condition)
                .targetValue(target).targetBook(book)
                .build();
    }
}
