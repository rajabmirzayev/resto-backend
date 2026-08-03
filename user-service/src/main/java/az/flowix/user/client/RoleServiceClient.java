package az.flowix.user.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.user.client.dto.RoleServiceRoleResponse;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "role-service", url = "${service.role.url}")
public interface RoleServiceClient {

    @GetMapping("/api/role-ms/v1/roles/{id}")
    ApiResponse<RoleServiceRoleResponse> getRole(@PathVariable("id") UUID id);

}
