package dongsim.dadok.auth.domain.dto;

import dongsim.dadok.auth.domain.entity.Verification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailSendRequest(
        @NotBlank @Email String email,
        @NotNull Verification purpose)
{}