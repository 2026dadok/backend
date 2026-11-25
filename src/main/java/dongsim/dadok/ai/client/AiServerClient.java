package dongsim.dadok.ai.client;

import dongsim.dadok.ai.domain.dto.AiInterviewRequest;
import dongsim.dadok.ai.domain.dto.AiInterviewResponse;
import dongsim.dadok.ai.domain.dto.AiReviewRequest;
import dongsim.dadok.ai.domain.dto.AiReviewResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
public class AiServerClient {

    private final RestClient restClient;

    public AiServerClient(RestClient aiRestClient) {
        this.restClient = aiRestClient;
    }

    public AiInterviewResponse interview(AiInterviewRequest request) {
        return post("/interview", request, AiInterviewResponse.class);
    }

    public AiReviewResponse generateReview(AiReviewRequest request) {
        return post("/generate-review", request, AiReviewResponse.class);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 서버 응답이 비어 있습니다.");
            }
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 서버 응답 시간이 초과되었습니다.", e);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 서버가 오류 응답을 반환했습니다.", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 서버 통신에 실패했습니다.", e);
        }
    }
}
