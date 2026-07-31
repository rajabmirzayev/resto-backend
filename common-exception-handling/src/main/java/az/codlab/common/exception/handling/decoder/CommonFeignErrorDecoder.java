package az.codlab.common.exception.handling.decoder;

import az.codlab.common.exception.handling.error.CommonErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import feign.Response;
import feign.codec.ErrorDecoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

public class CommonFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public CommonFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {

        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (response.body() != null) {
            try (InputStream is = response.body().asInputStream()) {

                String body = StreamUtils.copyToString(is, StandardCharsets.UTF_8);

                if (isProblemDetail(body)) {
                    return mapProblemDetail(body, status);
                }

                return CommonErrorCode.CLIENT_ERROR
                        .exceptionWithMessage(status, response.reason());

            } catch (Exception e) {
                return CommonErrorCode.CLIENT_ERROR.exception();
            }
        }

        return CommonErrorCode.CLIENT_ERROR
                .exceptionWithMessage(status, "Feign client error: " + response.reason());
    }

    protected boolean isProblemDetail(String body) {
        return body.contains("\"type\"")
                && body.contains("\"title\"")
                && body.contains("\"status\"");
    }

    protected BaseException mapProblemDetail(String body, HttpStatus status) throws Exception {
        JsonNode node = objectMapper.readTree(body);

        JsonNode titleNode = node.get("title");
        JsonNode detailNode = node.get("detail");
        JsonNode keyNode = node.get("key");

        String title = titleNode != null && titleNode.isTextual() ? titleNode.asText() : null;
        String detail = detailNode != null && detailNode.isTextual() ? detailNode.asText() : title;
        String key = keyNode != null && keyNode.isTextual() ? keyNode.asText() : null;

        return new FeignClientException(
                CommonErrorCode.CLIENT_ERROR,
                status,
                detail,
                key,
                title
        );
    }

}

