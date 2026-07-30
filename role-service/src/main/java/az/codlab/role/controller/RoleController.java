package az.codlab.role.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.role.dto.CreateRoleRequest;
import az.codlab.role.dto.PermissionsResponse;
import az.codlab.role.dto.RoleResponse;
import az.codlab.role.dto.UpdateRoleRequest;
import az.codlab.role.service.RoleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final Map<String, List<String>> PERMISSION_GROUPS = Map.of(
            "dashboard", List.of("dashboard.view"),
            "menu", List.of("menu.view", "menu.create", "menu.edit", "menu.delete"),
            "tables", List.of("tables.view", "tables.manage", "tables.status"),
            "orders", List.of("orders.view", "orders.manage", "orders.cancel"),
            "reports", List.of("reports.view"),
            "staff", List.of("staff.view", "staff.create", "staff.edit", "staff.delete"),
            "roles", List.of("roles.view", "roles.create", "roles.edit", "roles.delete"),
            "kitchen", List.of("kitchen.view", "kitchen.manage"),
            "settings", List.of("settings.view", "settings.edit")
    );

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles(
            @RequestParam(required = false) UUID orgId) {
        var roles = roleService.getAllRoles(orgId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        var role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable UUID id) {
        var role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        var role = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<PermissionsResponse>> getPermissions() {
        var response = new PermissionsResponse(PERMISSION_GROUPS);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
