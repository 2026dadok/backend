package dongsim.dadok.league.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 주간 정산 같은 정해진 시간 작업(@Scheduled) 사용
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
