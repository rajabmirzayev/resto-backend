package az.codlab.customer.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.customer.client.dto.SettingServiceSettingResponse;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "setting-service", url = "${service.setting.url}")
public interface SettingServiceClient {

    @GetMapping("/api/setting-ms/v1/settings")
    ApiResponse<SettingServiceSettingResponse> getSettings(@RequestParam UUID orgId);

}
