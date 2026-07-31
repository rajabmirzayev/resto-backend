package az.codlab.organization.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.organization.client.dto.SettingServiceCreateSettingRequest;
import az.codlab.organization.client.dto.SettingServiceSettingResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "setting-service", url = "${service.setting.url}")
public interface SettingServiceClient {

    @PutMapping("/api/setting-ms/v1/settings")
    ApiResponse<SettingServiceSettingResponse> createSettings(@RequestBody SettingServiceCreateSettingRequest request);

}
