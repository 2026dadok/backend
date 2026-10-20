package dongsim.dadok.auth.domain.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken) {
}
