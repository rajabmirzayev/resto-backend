package az.flowix.organization.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.dto.TableServiceSectionRequest;
import az.flowix.organization.client.dto.TableServiceSectionResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "table-service", url = "${service.table.url}")
public interface TableServiceClient {

    @PostMapping("/api/table-ms/v1/sections")
    ApiResponse<TableServiceSectionResponse> createSection(@RequestBody TableServiceSectionRequest request);

}
