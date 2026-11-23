package dongsim.dadok.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonProperty("ID") @JsonAlias("userId")
        @NotBlank String userId,
        @NotBlank String password)
{}
