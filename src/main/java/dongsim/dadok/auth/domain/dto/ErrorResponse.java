package dongsim.dadok.auth.domain.dto;

public record ErrorResponse(
        String message,
        int status,
        String code
) {}
