package az.codlab.organization.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.organization.client.dto.RoleServiceCreateRoleRequest;
import az.codlab.organization.client.dto.RoleServiceRoleResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "role-service", url = "${service.role.url}")
public interface RoleServiceClient {

    @PostMapping("/api/role-ms/v1/roles")
    ApiResponse<RoleServiceRoleResponse> createRole(@RequestBody RoleServiceCreateRoleRequest request);

}
