package az.flowix.access.controller;

import az.flowix.access.dto.CreateUserRequest;
import az.flowix.access.dto.StaffPerformanceResponse;
import az.flowix.access.dto.UpdateUserRequest;
import az.flowix.access.dto.UserDto;
import az.flowix.access.service.UserService;
import az.flowix.common.dto.PageDto;
import az.flowix.common.exception.handling.dto.ApiResponse;

import java.util.List;
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
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('staff.view')")
    public ResponseEntity<ApiResponse<PageDto<UserDto>>> getAllUsers(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var users = userService.getAllUsers(orgId, roleId, q, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('staff.view')")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable UUID id) {
        var user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("@perm.has('staff.create')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('staff.edit')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        var user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('staff.delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-username/{username}")
    @PreAuthorize("@perm.has('staff.delete')")
    public ResponseEntity<Void> deleteUserByUsername(@PathVariable String username) {
        userService.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/role")
    @PreAuthorize("@perm.has('role.assign')")
    public ResponseEntity<Void> unassignRole(@PathVariable UUID id) {
        userService.unassignRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/staff-performance")
    @PreAuthorize("@perm.has('staff.view')")
    public ResponseEntity<ApiResponse<List<StaffPerformanceResponse>>> getStaffPerformance(
            @RequestParam UUID orgId,
            @RequestParam(required = false) UUID roleId) {
        var performance = userService.getStaffPerformance(orgId, roleId);
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

}
