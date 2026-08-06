package az.flowix.access.controller;

import az.flowix.access.dto.ModuleTreeDto;
import az.flowix.access.dto.PermissionDto;
import az.flowix.access.service.PermissionService;
import az.flowix.common.dto.PageDto;
import az.flowix.common.exception.handling.dto.ApiResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getMyPermissions()));
    }

    @GetMapping
    @PreAuthorize("@perm.has('permission.view')")
    public ResponseEntity<ApiResponse<PageDto<PermissionDto>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String uiGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                permissionService.search(q, module, uiGroup, page, size)));
    }

    @GetMapping("/tree")
    @PreAuthorize("@perm.has('permission.view')")
    public ResponseEntity<ApiResponse<List<ModuleTreeDto>>> getTree(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getTree(q)));
    }

    @GetMapping("/by-module")
    @PreAuthorize("@perm.has('permission.view')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getByModule(
            @RequestParam String module,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getByModule(module, q)));
    }

    @GetMapping("/by-ui-group")
    @PreAuthorize("@perm.has('permission.view')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getByUiGroup(
            @RequestParam String uiGroup,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getByUiGroup(uiGroup, q)));
    }

}
