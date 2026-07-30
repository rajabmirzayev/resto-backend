package az.codlab.setting.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.setting.dto.SettingRequest;
import az.codlab.setting.dto.SettingResponse;
import az.codlab.setting.service.SettingService;

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
@RequestMapping("/v1/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SettingResponse>> getSettings(@RequestParam UUID orgId) {
        var settings = settingService.getSettings(orgId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SettingResponse>> updateSettings(
            @Valid @RequestBody SettingRequest request) {
        var settings = settingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated"));
    }

}
