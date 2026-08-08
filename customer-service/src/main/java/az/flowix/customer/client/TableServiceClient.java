package az.flowix.customer.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.customer.client.dto.TableServiceTableResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "table-service", url = "${service.table.url}")
public interface TableServiceClient {

    @GetMapping("/api/table-ms/v1/internal/tables")
    ApiResponse<List<TableServiceTableResponse>> getTables(@RequestParam UUID orgId);

}
