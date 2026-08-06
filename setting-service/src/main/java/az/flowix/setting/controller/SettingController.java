package az.flowix.setting.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.setting.dto.SettingRequest;
import az.flowix.setting.dto.SettingResponse;
import az.flowix.setting.service.SettingService;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@perm.has('settings.view')")
    public ResponseEntity<ApiResponse<SettingResponse>> getSettings(@RequestParam UUID orgId) {
        var settings = settingService.getSettings(orgId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @PreAuthorize("@perm.has('settings.edit')")
    public ResponseEntity<ApiResponse<SettingResponse>> updateSettings(
            @Valid @RequestBody SettingRequest request) {
        var settings = settingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated"));
    }

}
