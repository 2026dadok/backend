package dongsim.dadok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.TimeZone;
@SpringBootApplication
public class DadokApplication {

    public static void main(String[] args) {
        // 서버 시간대와 상관없이 LocalDateTime.now() 가 한국 시간이 되도록
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(DadokApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }


}
