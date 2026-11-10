package dongsim.dadok.profile.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.profile.domain.ImageType;
import dongsim.dadok.profile.domain.dto.ImageResponse;
import dongsim.dadok.profile.storage.ImageStorage;
import dongsim.dadok.quest.domain.entity.UserStat;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileImageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserStatRepository userStatRepository;
    private final ImageStorage imageStorage;

    // 프로필·배경 변경 (PUT /users/image)
    public ImageResponse upload(String email, String typeValue, MultipartFile file) {
        ImageType type = parseType(typeValue);
        validateImage(file);

        UserStat stat = getOrCreateStat(findUser(email));
        String oldUrl = currentUrl(stat, type);

        String newUrl = imageStorage.save(file);
        setUrl(stat, type, newUrl);
        imageStorage.delete(oldUrl);

        return new ImageResponse("이미지가 변경되었습니다.", newUrl);
    }

    // 프로필·배경 삭제 → 기본 이미지로 (DELETE /users/image)
    public ImageResponse delete(String email, String typeValue) {
        ImageType type = parseType(typeValue);
        UserStat stat = getOrCreateStat(findUser(email));

        imageStorage.delete(currentUrl(stat, type));
        setUrl(stat, type, null);

        return new ImageResponse("기본 이미지로 변경되었습니다.", type.getDefaultUrl());
    }

    // 화면에 보여줄 주소 (없으면 기본 이미지)
    @Transactional(readOnly = true)
    public String profileUrl(User user) {
        return displayUrl(user, ImageType.PROFILE);
    }

    @Transactional(readOnly = true)
    public String bannerUrl(User user) {
        return displayUrl(user, ImageType.BANNER);
    }

    private String displayUrl(User user, ImageType type) {
        return userStatRepository.findByUser(user)
                .map(stat -> currentUrl(stat, type))
                .orElse(type.getDefaultUrl());
    }

    private String currentUrl(UserStat stat, ImageType type) {
        String url = type == ImageType.PROFILE ? stat.getProfileImage() : stat.getBannerImage();
        return url;
    }

    private void setUrl(UserStat stat, ImageType type, String url) {
        if (type == ImageType.PROFILE) stat.updateProfileImage(url);
        else stat.updateBannerImage(url);
    }

    private ImageType parseType(String value) {
        ImageType type = ImageType.from(value);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type은 profile 또는 banner만 가능합니다.");
        }
        return type;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해 주세요.");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        String ext = (name != null && name.contains(".")) ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (contentType == null || !contentType.startsWith("image/") || !ALLOWED_EXT.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jpg, png, gif, webp 이미지만 올릴 수 있습니다.");
        }
    }

    private UserStat getOrCreateStat(User user) {
        return userStatRepository.findByUser(user)
                .orElseGet(() -> userStatRepository.save(new UserStat(user)));
    }

    private User findUser(String email) {
        return userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
