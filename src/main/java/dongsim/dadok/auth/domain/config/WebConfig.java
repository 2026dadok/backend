package dongsim.dadok.auth.domain.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final Path uploadPath;

    public WebConfig( @Value("${upload.profile-img-dir}") String uploadDir) throws IOException {
        this.uploadPath = Path.of(uploadDir)
                              .toAbsolutePath()
                              .normalize();
        Files.createDirectories(this.uploadPath);
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://10.80.162.156:5173",
                        "http://10.80.162.140:5173"

                ) // 특정 도메인 명시 (* 사용 불가)
                .allowedMethods("*")
                .allowCredentials(true); // 쿠키 허용
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath.toUri().toString();

        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/images/profileImg/**") // 요청 url
                .addResourceLocations(location); //실제 저장폴더
    }


}

