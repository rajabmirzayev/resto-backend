package az.codlab.organization.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.organization.client.dto.UserServiceCreateUserRequest;
import az.codlab.organization.client.dto.UserServiceUserResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${service.user.url}")
public interface UserServiceClient {

    @PostMapping("/api/user-ms/v1/users")
    ApiResponse<UserServiceUserResponse> createUser(@RequestBody UserServiceCreateUserRequest request);

}
