package az.codlab.user.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.user.dto.CreateUserRequest;
import az.codlab.user.dto.StaffPerformanceResponse;
import az.codlab.user.dto.UpdateUserRequest;
import az.codlab.user.dto.UserResponse;
import az.codlab.user.service.UserService;

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

    @GetMapping("/staff-performance")
    public ResponseEntity<ApiResponse<List<StaffPerformanceResponse>>> getStaffPerformance(
            @RequestParam UUID orgId) {
        var performance = userService.getStaffPerformance(orgId);
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

}
