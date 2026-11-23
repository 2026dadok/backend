package dongsim.dadok.note.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.note.domain.entity.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @EntityGraph(attributePaths = "book")
    List<Note> findByUserOrderByCreatedAtDesc(User user);

    Optional<Note> findByNoteIdAndUser(Long noteId, User user);

    long countByUser(User user);
}
