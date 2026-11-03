package dongsim.dadok.profile.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// 서버 폴더(upload.profile-img-dir)에 이미지 저장
// 저장한 파일은 WebConfig의 /images/profileImg/** 로 제공됨
@Component
public class LocalImageStorage implements ImageStorage {

    public static final String URL_PREFIX = "/images/profileImg/";

    private final Path rootDir;

    public LocalImageStorage(@Value("${upload.profile-img-dir}") String uploadDir) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String save(MultipartFile file) {
        String fileName = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
        try {
            Files.createDirectories(rootDir);
            file.transferTo(rootDir.resolve(fileName));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
        }
        return URL_PREFIX + fileName;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        Path target = rootDir.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!target.startsWith(rootDir)) return;   // 저장 폴더 밖은 삭제하지 않음
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 파일이 없거나 지울 수 없어도 DB 값은 정리
        }
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "png";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
