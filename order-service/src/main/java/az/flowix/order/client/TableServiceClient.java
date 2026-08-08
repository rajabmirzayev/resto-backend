package az.flowix.order.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.order.client.dto.ClientStatusUpdateRequest;
import az.flowix.order.client.dto.ClientTableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "table-service", url = "${service.table.url}")
public interface TableServiceClient {

    @GetMapping("/api/table-ms/v1/internal/tables/{id}")
    ApiResponse<ClientTableResponse> getTable(@PathVariable("id") UUID id);

    @PutMapping("/api/table-ms/v1/internal/tables/{id}/status")
    ApiResponse<ClientTableResponse> updateTableStatus(@PathVariable("id") UUID id,
                                                       @RequestBody ClientStatusUpdateRequest request);
}
