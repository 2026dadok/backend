package dongsim.dadok.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiRestClientConfig {

    @Bean
    public RestClient aiRestClient(@Value("${ai.server.url}") String aiServerUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(30_000);

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
