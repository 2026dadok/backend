package dongsim.dadok.profile.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 이미지 종류: 프로필 / 배경
@Getter
@RequiredArgsConstructor
public enum ImageType {
    PROFILE("profile", "/images/default-profile.png"),
    BANNER("banner", "/images/default-banner.png");

    private final String value;         // 요청의 type 값
    private final String defaultUrl;    // 사진이 없을 때 보여줄 기본 이미지

    public static ImageType from(String value) {
        for (ImageType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        if ("background".equalsIgnoreCase(value)) {
            return BANNER;
        }
        return null;
    }
}
