package az.flowix.waiter.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.waiter.client.dto.TableServiceSectionResponse;
import az.flowix.waiter.client.dto.TableServiceTableResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "table-service", url = "${service.table.url}")
public interface TableServiceClient {

    @GetMapping("/api/table-ms/v1/tables")
    ApiResponse<List<TableServiceTableResponse>> getTables(@RequestParam UUID orgId);

    @GetMapping("/api/table-ms/v1/sections")
    ApiResponse<List<TableServiceSectionResponse>> getSections(@RequestParam UUID orgId);

}
