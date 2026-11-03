package dongsim.dadok.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

// 업로드 폴더·주소·크기 설정은 WebConfig와 application.yaml(upload, spring.servlet.multipart)을 따름
@Configuration
public class ImageConfig {

    // 이미지는 로그인 없이도 볼 수 있게 (img 태그에는 토큰이 안 붙음)
    @Bean
    public WebSecurityCustomizer imageSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/images/**");
    }
}
