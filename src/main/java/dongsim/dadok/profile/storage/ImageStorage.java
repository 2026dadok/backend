package dongsim.dadok.profile.storage;

import org.springframework.web.multipart.MultipartFile;

// 이미지 저장소 (서버 폴더 → 나중에 S3 등으로 교체 가능)
public interface ImageStorage {

    // 저장하고 접근 주소를 돌려줌 (예: /images/profile/abc.png)
    String save(MultipartFile file, String folder);

    // 주소에 해당하는 파일 삭제 (없으면 무시)
    void delete(String url);
}
