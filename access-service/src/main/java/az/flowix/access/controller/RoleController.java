package az.flowix.access.controller;

import az.flowix.access.dto.AssignRoleRequest;
import az.flowix.access.dto.CreateRoleRequest;
import az.flowix.access.dto.RolePermissionRequest;
import az.flowix.access.dto.RoleResponse;
import az.flowix.access.dto.UpdateRoleRequest;
import az.flowix.access.dto.UserDto;
import az.flowix.access.service.RoleService;
import az.flowix.common.dto.PageDto;
import az.flowix.common.exception.handling.dto.ApiResponse;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('role.view')")
    public ResponseEntity<ApiResponse<PageDto<RoleResponse>>> getAllRoles(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var roles = roleService.getAllRoles(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('role.view')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable UUID id) {
        var role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping
    @PreAuthorize("@perm.has('role.create')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        var role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(role));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('role.edit')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        var role = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('role.delete')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("@perm.has('role.edit')")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody RolePermissionRequest request) {
        var role = roleService.addPermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@perm.has('role.edit')")
    public ResponseEntity<ApiResponse<RoleResponse>> setPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody RolePermissionRequest request) {
        var role = roleService.setPermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("@perm.has('role.edit')")
    public ResponseEntity<Void> removePermission(@PathVariable UUID id,
                                                 @PathVariable UUID permissionId) {
        roleService.removePermission(id, permissionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/users")
    @PreAuthorize("@perm.has('role.assign')")
    public ResponseEntity<Void> assignUsers(@PathVariable UUID id,
                                            @Valid @RequestBody AssignRoleRequest request) {
        roleService.assignUsers(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("@perm.has('role.view')")
    public ResponseEntity<ApiResponse<PageDto<UserDto>>> getRoleUsers(
            @PathVariable UUID id,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var users = roleService.getRoleUsers(id, q, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @DeleteMapping("/{id}/users/{userId}")
    @PreAuthorize("@perm.has('role.assign')")
    public ResponseEntity<Void> unassignUser(@PathVariable UUID id,
                                             @PathVariable UUID userId) {
        roleService.unassignUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/system/{code}")
    @PreAuthorize("@perm.has('role.view')")
    public ResponseEntity<ApiResponse<RoleResponse>> getSystemRole(@PathVariable String code) {
        var role = roleService.getSystemRoleByCode(code);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

}
