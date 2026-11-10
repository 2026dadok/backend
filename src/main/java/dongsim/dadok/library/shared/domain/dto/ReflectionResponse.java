package dongsim.dadok.library.shared.domain.dto;

import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import java.time.LocalDateTime;

public record ReflectionResponse(Long reflectionId, String userId, String name, int readingDay, String content, LocalDateTime createdAt) {
}
