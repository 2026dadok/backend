package dongsim.dadok.profile.config;

import dongsim.dadok.profile.storage.LocalImageStorage;
import jakarta.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ImageConfig implements WebMvcConfigurer {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;      // 사진 1장 최대 5MB
    private static final long MAX_REQUEST_SIZE = 6L * 1024 * 1024;

    private final LocalImageStorage localImageStorage;

    // /images/** 주소로 업로드한 사진(서버 폴더)과 기본 이미지(static/images) 제공
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        localImageStorage.getRootDir().toUri().toString(),
                        "classpath:/static/images/");
    }

    // 이미지는 로그인 없이도 볼 수 있게 (img 태그에는 토큰이 안 붙음)
    @Bean
    public WebSecurityCustomizer imageSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/images/**");
    }

    // 업로드 크기 제한 (기본값 1MB는 휴대폰 사진에 너무 작음)
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", MAX_FILE_SIZE, MAX_REQUEST_SIZE, 0);
    }
}
