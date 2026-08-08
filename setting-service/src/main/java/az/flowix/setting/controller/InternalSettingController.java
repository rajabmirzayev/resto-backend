package az.flowix.setting.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.setting.dto.SettingRequest;
import az.flowix.setting.dto.SettingResponse;
import az.flowix.setting.service.SettingService;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal")
public class InternalSettingController {

    private final SettingService settingService;

    public InternalSettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SettingResponse>> getSettings(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(settingService.getSettings(orgId)));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SettingResponse>> updateSettings(@Valid @RequestBody SettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingService.updateSettings(request), "Settings updated"));
    }

}
