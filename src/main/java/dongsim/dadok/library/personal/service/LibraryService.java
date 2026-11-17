package dongsim.dadok.library.personal.service;

import dongsim.dadok.library.personal.domain.dto.*;
import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.domain.entity.PersonalReadingLog;
import dongsim.dadok.library.personal.repository.PersonalLibraryBookRepository;
import dongsim.dadok.library.personal.repository.PersonalReadingLogRepository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import dongsim.dadok.quest.service.QuestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LibraryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PersonalLibraryBookRepository personalLibraryBookRepository;
    private final PersonalReadingLogRepository personalReadingLogRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final QuestService questService;

    public LibraryService(
            PersonalLibraryBookRepository personalLibraryBookRepository,
            PersonalReadingLogRepository personalReadingLogRepository,
            UserRepository userRepository,
            BookRepository bookRepository,
            QuestService questService
    ) {
        this.personalLibraryBookRepository = personalLibraryBookRepository;
        this.personalReadingLogRepository = personalReadingLogRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.questService = questService;
    }

    @Transactional
    public LibraryBookResponse addBook(String userId, AddLibraryBookRequest request) {
        User user = getUser(userId);
        Book book = resolveBook(request);
        if (personalLibraryBookRepository.existsByUserAndBookBookId(user, book.getBookId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 개인 서재에 추가한 책입니다.");
        }
        PersonalLibraryBook saved = personalLibraryBookRepository.save(new PersonalLibraryBook(user, book));
        int gainedXp = questService.onBookRegistered(user);
        return toResponse(saved, gainedXp);
    }

    @Transactional
    public void deleteBook(String userId, Long bookId) {
        PersonalLibraryBook libraryBook = getLibraryBook(getUser(userId), bookId);
        personalLibraryBookRepository.delete(libraryBook);
    }

    public LibraryPageResponse getMyLibrary(String userId) {
        User user = getUser(userId);
        List<LibraryBookResponse> books = personalLibraryBookRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(b -> toResponse(b, 0))
                .toList();
        return new LibraryPageResponse(books, personalReadingLogRepository.findDistinctReadDatesByUser(user));
    }

    @Transactional
    public LibraryBookResponse updateProgress(String userId, Long bookId, UpdatePersonalProgressRequest request) {
        User user = getUser(userId);
        PersonalLibraryBook libraryBook = getLibraryBook(user, bookId);
        int totalPages = libraryBook.getBook().getPages();
        // 전체 쪽수를 모르는 책(0)은 상한 검사를 하지 않는다
        if (totalPages > 0 && request.currentPage() > totalPages) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 페이지는 책의 전체 페이지를 넘을 수 없습니다.");
        }
        int before = libraryBook.getCurrentPage();
        if (request.currentPage() < before) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "읽은 페이지는 이전 진행도보다 낮게 설정할 수 없습니다.");
        }
        int gainedXp = 0;
        if (libraryBook.updateProgress(request.currentPage(), totalPages)) {
            personalReadingLogRepository.save(new PersonalReadingLog(libraryBook, LocalDate.now(KST)));
            gainedXp = questService.addReadPages(user, request.currentPage() - before);
        }
        return toResponse(libraryBook, gainedXp);
    }

    private PersonalLibraryBook getLibraryBook(User user, Long bookId) {
        return personalLibraryBookRepository.findByUserAndBookBookId(user, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "개인 서재에 추가되지 않은 책입니다."));
    }

    // bookId로 찾고, 없으면 제목·저자로 찾거나 새로 등록
    private Book resolveBook(AddLibraryBookRequest request) {
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
                .orElseGet(() -> bookRepository.save(new Book(title, request.coverUrl(), null, author, null, isbn, pages, null)));
    }

    private User getUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private LibraryBookResponse toResponse(PersonalLibraryBook libraryBook, int gainedXp) {
        Book book = libraryBook.getBook();
        int totalPages = book.getPages();
        int percent = totalPages == 0 ? 0 : Math.min(100, libraryBook.getCurrentPage() * 100 / totalPages);
        return new LibraryBookResponse(
                libraryBook.getPersonalLibraryBookId(), book.getBookId(), book.getBookTitle(), book.getCoverUrl(), book.getAuthor(),
                libraryBook.getCurrentPage(), totalPages, percent, libraryBook.getCompletedAt() != null, libraryBook.getCompletedAt(),
                libraryBook.getCreatedAt(), gainedXp
        );
    }
}
