package dongsim.dadok.auth.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Verification {
    SIGNUP,
    FIND_ID,
    FIND_PASSWORD;

    @JsonCreator
    public static Verification from(String value) {
        return switch (value) {
            case "signup" -> SIGNUP;
            case "find_id" -> FIND_ID;
            case "find_pw" -> FIND_PASSWORD;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 인증 목적: " + value
            );
        };
    }
}