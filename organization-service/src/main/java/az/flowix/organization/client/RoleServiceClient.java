package az.flowix.organization.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.dto.RoleServiceRoleResponse;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "access-service-roles", url = "${service.access.url}")
public interface RoleServiceClient {

    @GetMapping("/api/access-ms/v1/roles/system/{code}")
    ApiResponse<RoleServiceRoleResponse> getSystemRole(@PathVariable("code") String code);

    @DeleteMapping("/api/access-ms/v1/roles/{id}")
    ApiResponse<Void> deleteRole(@PathVariable("id") UUID id);

}
