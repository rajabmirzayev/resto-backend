package az.flowix.organization.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.dto.UserServiceCreateUserRequest;
import az.flowix.organization.client.dto.UserServiceUserResponse;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${service.user.url}")
public interface UserServiceClient {

    @PostMapping("/api/user-ms/v1/users")
    ApiResponse<UserServiceUserResponse> createUser(@RequestBody UserServiceCreateUserRequest request);

    @DeleteMapping("/api/user-ms/v1/users/{id}")
    ApiResponse<Void> deleteUser(@PathVariable("id") UUID id);

}
