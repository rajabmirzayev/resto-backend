package az.codlab.dashboard.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.dashboard.client.dto.UserServiceUserResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${service.user.url}")
public interface UserServiceClient {

    @GetMapping("/api/user-ms/v1/users")
    ApiResponse<List<UserServiceUserResponse>> getUsers(@RequestParam UUID orgId);

}
