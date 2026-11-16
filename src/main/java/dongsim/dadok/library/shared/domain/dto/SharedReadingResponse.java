package dongsim.dadok.library.shared.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SharedReadingResponse(
        Long sharedReadingId,
        Long bookId,
        String bookTitle,
        String coverUrl,
        SharedReadingStatus status,
        String winnerUserId,
        List<MemberProgressResponse> members,
        int totalPages,
        LocalDateTime createdAt,
        @JsonIgnore String viewerUserId
) {
    public record MemberProgressResponse(String userId, String name, int currentPage, int totalPages, int progressPercent, LocalDateTime completedAt) {
    }

    // 프론트 호환 키
    @JsonProperty("shared_reading_id")
    public Long shared_reading_id() {
        return sharedReadingId;
    }

    @JsonProperty("book_id")
    public Long book_id() {
        return bookId;
    }

    @JsonProperty("book_title")
    public String book_title() {
        return bookTitle;
    }

    @JsonProperty("cover_url")
    public String cover_url() {
        return coverUrl;
    }

    @JsonProperty("total_page")
    public int total_page() {
        return totalPages;
    }

    @JsonProperty("started_at")
    public LocalDateTime started_at() {
        return createdAt;
    }

    // 조회한 사람 기준 내 진행률 / 상대 정보
    @JsonProperty("my_percent")
    public int my_percent() {
        MemberProgressResponse me = me();
        return me == null ? 0 : me.progressPercent();
    }

    @JsonProperty("partner_percent")
    public int partner_percent() {
        MemberProgressResponse p = partnerMember();
        return p == null ? 0 : p.progressPercent();
    }

    @JsonProperty("partner")
    public Map<String, Object> partner() {
        MemberProgressResponse p = partnerMember();
        if (p == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ID", p.userId());
        m.put("name", p.name());
        return m;
    }

    private MemberProgressResponse me() {
        if (members == null || viewerUserId == null) return null;
        return members.stream().filter(m -> viewerUserId.equals(m.userId())).findFirst().orElse(null);
    }

    private MemberProgressResponse partnerMember() {
        if (members == null || viewerUserId == null) return null;
        return members.stream().filter(m -> !viewerUserId.equals(m.userId())).findFirst().orElse(null);
    }
}
