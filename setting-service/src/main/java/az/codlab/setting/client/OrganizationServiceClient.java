package az.codlab.setting.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.setting.client.dto.ClientOrganizationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "organization-service", url = "${service.organization.url}")
public interface OrganizationServiceClient {

    @GetMapping("/api/organization-ms/v1/organizations/{orgId}")
    ApiResponse<ClientOrganizationDto> getOrganization(@PathVariable("orgId") UUID orgId);
}
