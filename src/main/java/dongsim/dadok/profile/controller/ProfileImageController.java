package dongsim.dadok.profile.controller;

import dongsim.dadok.profile.domain.dto.ImageResponse;
import dongsim.dadok.profile.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    // 프로필·배경 변경: form-data { type: profile | banner, image: 파일 }
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(@AuthenticationPrincipal String email,
                                                @RequestParam("type") String type,
                                                @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(profileImageService.upload(email, type, image));
    }

    // 프로필·배경 삭제 → 기본 이미지: /users/image?type=profile
    @DeleteMapping
    public ResponseEntity<ImageResponse> delete(@AuthenticationPrincipal String email,
                                                @RequestParam("type") String type) {
        return ResponseEntity.ok(profileImageService.delete(email, type));
    }
}
