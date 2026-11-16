package dongsim.dadok.library.shared.service;

import dongsim.dadok.library.shared.domain.dto.*;
import dongsim.dadok.library.shared.domain.entity.*;
import dongsim.dadok.library.shared.repository.DailyReflectionRepository;
import dongsim.dadok.library.shared.repository.SharedReadingRepository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SharedReadingService {

    private final SharedReadingRepository sharedReadingRepository;
    private final DailyReflectionRepository dailyReflectionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public SharedReadingService(
            SharedReadingRepository sharedReadingRepository,
            DailyReflectionRepository dailyReflectionRepository,
            BookRepository bookRepository,
            UserRepository userRepository
    ) {
        this.sharedReadingRepository = sharedReadingRepository;
        this.dailyReflectionRepository = dailyReflectionRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long create(String creatorUserId, CreateSharedReadingRequest request) {
        if (request.partnerName() == null || request.partnerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대할 친구의 이름을 입력해 주세요.");
        }
        Optional<User> found;
        if (request.partnerUserId() != null && !request.partnerUserId().isBlank()) {
            found = userRepository.findByUserId(request.partnerUserId().trim());
        } else if (request.partnerEmail() != null && !request.partnerEmail().isBlank()) {
            found = userRepository.findByEmail(request.partnerEmail().trim());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대할 친구의 아이디를 입력해 주세요.");
        }
        User partner = found
                .filter(user -> user.getName().equals(request.partnerName().trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대할 친구의 계정 아이디 또는 이름이 일치하지 않습니다."));
        if (creatorUserId.equals(partner.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인은 공동 읽기에 초대할 수 없습니다.");
        }

        Book book = findBook(request);
        String pairKey = createPairKey(creatorUserId, partner.getUserId());
        if (sharedReadingRepository.existsByBookBookIdAndPairKeyAndStatusIn(
                book.getBookId(), pairKey, List.of(SharedReadingStatus.ACTIVE, SharedReadingStatus.WINNER_DECIDED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "두 사용자는 이 책을 이미 함께 읽고 있습니다.");
        }

        SharedReading sharedReading = new SharedReading(book, creatorUserId, pairKey);
        sharedReading.addMember(creatorUserId);
        sharedReading.addMember(partner.getUserId());
        return sharedReadingRepository.save(sharedReading).getSharedReadingId();
    }

    public List<SharedReadingResponse> getMine(String userId) {
        return sharedReadingRepository.findDistinctByMembersUserIdOrderByCreatedAtDesc(userId).stream()
                .map(reading -> toResponse(reading, userId))
                .toList();
    }

    public SharedReadingResponse getDetail(String userId, Long sharedReadingId) {
        SharedReading sharedReading = getParticipantReading(userId, sharedReadingId);
        return toResponse(sharedReading, userId);
    }

    @Transactional
    public SharedReadingResponse updateProgress(String userId, Long sharedReadingId, UpdateProgressRequest request) {
        SharedReading sharedReading = getParticipantReading(userId, sharedReadingId);
        if (sharedReading.getStatus() == SharedReadingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "종료된 공동 읽기입니다.");
        }

        int totalPages = sharedReading.getBook().getPages();
        if (request.currentPage() > totalPages) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 페이지는 책의 전체 페이지를 넘을 수 없습니다.");
        }

        SharedReadingMember member = sharedReading.getMembers().stream()
                .filter(it -> it.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "공동 읽기 참여자가 아닙니다."));
        if (request.currentPage() < member.getCurrentPage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "읽은 페이지는 이전 진행도보다 낮게 설정할 수 없습니다.");
        }

        boolean justCompleted = member.updateProgress(request.currentPage(), totalPages);
        if (justCompleted) {
            sharedReading.decideWinner(userId);
        }
        if (sharedReading.getMembers().stream().allMatch(it -> it.getCompletedAt() != null)) {
            sharedReading.complete();
        }
        return toResponse(sharedReading, userId);
    }

    @Transactional
    public Long createReflection(String userId, Long sharedReadingId, CreateReflectionRequest request) {
        SharedReading sharedReading = getParticipantReading(userId, sharedReadingId);
        // "하루 한 번"은 24시간 간격이 아니라 날짜가 바뀌면 새 감상문을 쓸 수 있게 처리한다.
        int readingDay = (int) ChronoUnit.DAYS.between(
                sharedReading.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate()
        ) + 1;
        if (dailyReflectionRepository.existsBySharedReadingSharedReadingIdAndUserIdAndReadingDay(sharedReadingId, userId, readingDay)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘의 감상문은 이미 작성했습니다.");
        }
        DailyReflection reflection = new DailyReflection(sharedReading, userId, readingDay, request.content().trim());
        return dailyReflectionRepository.save(reflection).getReflectionId();
    }

    public List<ReflectionResponse> getReflections(String userId, Long sharedReadingId, String target) {
        SharedReading reading = getParticipantReading(userId, sharedReadingId);
        List<DailyReflection> reflections;
        if (target == null) {
            reflections = dailyReflectionRepository.findBySharedReadingSharedReadingIdOrderByReadingDayDescCreatedAtDesc(sharedReadingId);
        } else if (target.equals("me")) {
            reflections = dailyReflectionRepository.findBySharedReadingSharedReadingIdAndUserIdOrderByReadingDayDescCreatedAtDesc(sharedReadingId, userId);
        } else if (target.equals("partner")) {
            String partnerId = reading.getMembers().stream()
                    .map(SharedReadingMember::getUserId)
                    .filter(id -> !id.equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "상대 참여자를 찾을 수 없습니다."));
            reflections = dailyReflectionRepository.findBySharedReadingSharedReadingIdAndUserIdOrderByReadingDayDescCreatedAtDesc(sharedReadingId, partnerId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target은 me 또는 partner여야 합니다.");
        }
        return reflections.stream()
                .map(reflection -> new ReflectionResponse(
                        reflection.getReflectionId(), reflection.getUserId(), getUserName(reflection.getUserId()),
                        reflection.getReadingDay(), reflection.getContent(), reflection.getCreatedAt()
                ))
                .toList();
    }

    private SharedReading getParticipantReading(String userId, Long sharedReadingId) {
        SharedReading sharedReading = sharedReadingRepository.findWithBookAndMembersBySharedReadingId(sharedReadingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공동 읽기를 찾을 수 없습니다."));
        if (sharedReading.getMembers().stream().noneMatch(member -> member.getUserId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공동 읽기 참여자만 볼 수 있습니다.");
        }
        return sharedReading;
    }

    private SharedReadingResponse toResponse(SharedReading sharedReading, String viewerUserId) {
        int totalPages = sharedReading.getBook().getPages();
        List<SharedReadingResponse.MemberProgressResponse> members = sharedReading.getMembers().stream()
                .map(member -> new SharedReadingResponse.MemberProgressResponse(
                        member.getUserId(), getUserName(member.getUserId()), member.getCurrentPage(), totalPages,
                        totalPages == 0 ? 0 : Math.min(100, member.getCurrentPage() * 100 / totalPages), member.getCompletedAt()
                ))
                .toList();
        return new SharedReadingResponse(
                sharedReading.getSharedReadingId(), sharedReading.getBook().getBookId(), sharedReading.getBook().getBookTitle(),
                sharedReading.getBook().getCoverUrl(), sharedReading.getStatus(), sharedReading.getWinnerUserId(), members,
                totalPages, sharedReading.getCreatedAt(), viewerUserId
        );
    }

    // bookId로 찾고, 없으면 ISBN → 제목·저자 순으로 찾고, 그래도 없으면 새로 등록
    private Book findBook(CreateSharedReadingRequest request) {
        if (request.bookId() != null) {
            return bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "책 번호 또는 제목이 필요합니다.");
        }
        String title = request.title().trim();
        String author = request.author() == null ? null : request.author().trim();
        String isbn = request.isbn() == null || request.isbn().isBlank() ? null : request.isbn().trim();
        int pages = request.totalPage() == null ? 0 : request.totalPage();
        if (isbn != null) {
            Book byIsbn = bookRepository.findByIsbn13(isbn).orElse(null);
            if (byIsbn != null) return byIsbn;
        }
        return bookRepository.findFirstByBookTitleAndAuthor(title, author)
                .orElseGet(() -> bookRepository.save(new Book(title, null, null, author, null, isbn, pages, null)));
    }

    private String getUserName(String userId) {
        return userRepository.findByUserId(userId).map(User::getName).orElse("알 수 없음");
    }

    private String createPairKey(String firstUserId, String secondUserId) {
        return firstUserId.compareTo(secondUserId) < 0
                ? firstUserId + "|" + secondUserId
                : secondUserId + "|" + firstUserId;
    }
}
