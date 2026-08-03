package az.flowix.role.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${service.user.url}")
public interface UserServiceClient {

    @PutMapping("/api/user-ms/v1/users/clear-role")
    ApiResponse<Void> clearRole(@RequestParam("roleId") UUID roleId);
}
