package dongsim.dadok.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String userId,
        @NotBlank String password)
{}
