package az.flowix.order.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.order.client.dto.ClientSettingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "setting-service", url = "${service.setting.url}")
public interface SettingServiceClient {

    @GetMapping("/api/setting-ms/v1/settings")
    ApiResponse<ClientSettingResponse> getSettings(@RequestParam("orgId") UUID orgId);
}
