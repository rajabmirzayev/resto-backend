package az.flowix.organization.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.dto.SettingServiceCreateSettingRequest;
import az.flowix.organization.client.dto.SettingServiceSettingResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "setting-service", url = "${service.setting.url}")
public interface SettingServiceClient {

    @PutMapping("/api/setting-ms/v1/settings")
    ApiResponse<SettingServiceSettingResponse> createSettings(@RequestBody SettingServiceCreateSettingRequest request);

}
