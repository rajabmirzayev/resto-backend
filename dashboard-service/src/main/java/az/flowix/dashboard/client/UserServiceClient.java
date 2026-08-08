package az.flowix.dashboard.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.dashboard.client.dto.UserServiceUserResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "access-service", url = "${service.access.url}")
public interface UserServiceClient {

    @GetMapping("/api/access-ms/v1/users")
    ApiResponse<PageDto<UserServiceUserResponse>> getUsers(@RequestParam UUID orgId,
                                                           @RequestParam(defaultValue = "100") int size);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

}
