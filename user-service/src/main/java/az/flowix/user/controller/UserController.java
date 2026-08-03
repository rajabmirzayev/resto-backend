package az.flowix.user.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.user.dto.CreateUserRequest;
import az.flowix.user.dto.StaffPerformanceResponse;
import az.flowix.user.dto.UpdateUserRequest;
import az.flowix.user.dto.UserResponse;
import az.flowix.user.service.UserService;

import java.util.List;
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
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) String role) {
        var users = userService.getAllUsers(orgId, role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        var user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        var user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }

    @PutMapping("/clear-role")
    public ResponseEntity<ApiResponse<Void>> clearRole(@RequestParam UUID roleId) {
        userService.clearRole(roleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Role cleared from users"));
    }

    @GetMapping("/staff-performance")
    public ResponseEntity<ApiResponse<List<StaffPerformanceResponse>>> getStaffPerformance(
            @RequestParam UUID orgId) {
        var performance = userService.getStaffPerformance(orgId);
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

}
