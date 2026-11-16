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

    public LibraryService(
            PersonalLibraryBookRepository personalLibraryBookRepository,
            PersonalReadingLogRepository personalReadingLogRepository,
            UserRepository userRepository,
            BookRepository bookRepository
    ) {
        this.personalLibraryBookRepository = personalLibraryBookRepository;
        this.personalReadingLogRepository = personalReadingLogRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public LibraryBookResponse addBook(String email, AddLibraryBookRequest request) {
        User user = getUser(email);
        Book book = resolveBook(request);
        if (personalLibraryBookRepository.existsByUserAndBookBookId(user, book.getBookId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 개인 서재에 추가한 책입니다.");
        }
        return toResponse(personalLibraryBookRepository.save(new PersonalLibraryBook(user, book)));
    }

    @Transactional
    public void deleteBook(String email, Long bookId) {
        PersonalLibraryBook libraryBook = getLibraryBook(getUser(email), bookId);
        personalLibraryBookRepository.delete(libraryBook);
    }

    public LibraryPageResponse getMyLibrary(String email) {
        User user = getUser(email);
        List<LibraryBookResponse> books = personalLibraryBookRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
        return new LibraryPageResponse(books, personalReadingLogRepository.findDistinctReadDatesByUser(user));
    }

    @Transactional
    public LibraryBookResponse updateProgress(String email, Long bookId, UpdatePersonalProgressRequest request) {
        PersonalLibraryBook libraryBook = getLibraryBook(getUser(email), bookId);
        int totalPages = libraryBook.getBook().getPages();
        if (request.currentPage() > totalPages) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 페이지는 책의 전체 페이지를 넘을 수 없습니다.");
        }
        if (request.currentPage() < libraryBook.getCurrentPage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "읽은 페이지는 이전 진행도보다 낮게 설정할 수 없습니다.");
        }
        if (libraryBook.updateProgress(request.currentPage(), totalPages)) {
            personalReadingLogRepository.save(new PersonalReadingLog(libraryBook, LocalDate.now(KST)));
        }
        return toResponse(libraryBook);
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
        int pages = request.totalPage() == null ? 0 : request.totalPage();
        return bookRepository.findFirstByBookTitleAndAuthor(title, author)
                .orElseGet(() -> bookRepository.save(new Book(title, null, null, author, null, null, pages, null)));
    }

    private User getUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByUserId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private LibraryBookResponse toResponse(PersonalLibraryBook libraryBook) {
        Book book = libraryBook.getBook();
        int totalPages = book.getPages();
        int percent = totalPages == 0 ? 0 : Math.min(100, libraryBook.getCurrentPage() * 100 / totalPages);
        return new LibraryBookResponse(
                libraryBook.getPersonalLibraryBookId(), book.getBookId(), book.getBookTitle(), book.getCoverUrl(), book.getAuthor(),
                libraryBook.getCurrentPage(), totalPages, percent, libraryBook.getCompletedAt() != null, libraryBook.getCompletedAt()
        );
    }
}
