package az.flowix.access.controller;

import az.flowix.access.dto.ModuleDto;
import az.flowix.access.service.PermissionService;
import az.flowix.common.exception.handling.dto.ApiResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/modules")
public class ModuleController {

    private final PermissionService permissionService;

    public ModuleController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('permission.view')")
    public ResponseEntity<ApiResponse<List<ModuleDto>>> getModules(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getModules(q)));
    }

}
