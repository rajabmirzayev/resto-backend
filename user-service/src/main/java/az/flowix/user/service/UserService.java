package az.flowix.user.service;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.util.PhoneUtils;
import az.flowix.user.client.KeycloakAdminClient;
import az.flowix.user.client.RoleServiceClient;
import az.flowix.user.dto.CreateUserRequest;
import az.flowix.user.dto.StaffPerformanceResponse;
import az.flowix.user.dto.UpdateUserRequest;
import az.flowix.user.dto.UserResponse;
import az.flowix.user.entity.User;
import az.flowix.user.entity.UserRole;
import az.flowix.user.error.UserErrorCode;
import az.flowix.user.mapper.UserMapper;
import az.flowix.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleServiceClient roleServiceClient;
    private final KeycloakUserProvisioner keycloakUserProvisioner;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       RoleServiceClient roleServiceClient,
                       KeycloakUserProvisioner keycloakUserProvisioner) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleServiceClient = roleServiceClient;
        this.keycloakUserProvisioner = keycloakUserProvisioner;
    }

    public List<UserResponse> getAllUsers(UUID orgId, String role) {
        if (orgId != null && role != null) {
            return userMapper.toDtoList(
                    userRepository.findAllByOrgIdAndRoleAndDeletedFalse(orgId, UserRole.valueOf(role.toUpperCase())));
        }
        if (orgId != null) {
            return userMapper.toDtoList(
                    userRepository.findAllByOrgIdAndDeletedFalse(orgId));
        }
        return userMapper.toDtoList(
                userRepository.findAllByDeletedFalseOrderByCreatedAtDesc());
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .map(userMapper::toDto)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw UserErrorCode.USERNAME_DUPLICATE.conflict();
        }

        var role = resolveRole(request.getRole(), request.getRoleId());
        var email = normalizeEmail(request.getEmail());

        var user = User.builder()
                .name(request.getName().trim())
                .username(request.getUsername().trim())
                .email(email)
                .phone(PhoneUtils.normalize(request.getPhone()))
                .password(passwordEncoder.encode(request.getPassword()))
                .roleId(request.getRoleId())
                .orgId(request.getOrgId())
                .role(role)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String keycloakId = null;
        try {
            keycloakId = keycloakUserProvisioner.provision(
                    new KeycloakAdminClient.NewUser(user.getUsername(), email, user.getName(), user.getOrgId()),
                    request.getPassword(), role);
            user.setKeycloakId(keycloakId);
            user = userRepository.save(user);
        } catch (Exception ex) {
            log.error("Failed to provision Keycloak user '{}'", request.getUsername(), ex);
            if (keycloakId != null) {
                keycloakUserProvisioner.rollback(keycloakId);
            }
            throw UserErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }

        log.info("User created: {} ({})", user.getName(), user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);

        Map<String, Object> keycloakProfile = new java.util.LinkedHashMap<>();
        if (request.getName() != null) {
            user.setName(request.getName().trim());
            keycloakProfile.put("firstName", user.getName());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername().trim());
            keycloakProfile.put("username", user.getUsername());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRoleId() != null) {
            var oldRole = user.getRole();
            var newRole = resolveRole(null, request.getRoleId());
            user.setRoleId(request.getRoleId());
            user.setRole(newRole);
            if (user.getKeycloakId() != null && oldRole != newRole) {
                keycloakUserProvisioner.updateRole(user.getKeycloakId(), newRole, oldRole);
            }
        }
        if (request.getPhone() != null) {
            user.setPhone(PhoneUtils.normalize(request.getPhone()));
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        syncKeycloak(user, request, keycloakProfile);

        user = userRepository.save(user);
        log.info("User updated: {} ({})", user.getName(), user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public void clearRole(UUID roleId) {
        var users = userRepository.findAllByRoleIdAndDeletedFalse(roleId);
        for (var user : users) {
            user.setRoleId(null);
            userRepository.save(user);
        }
        log.info("Cleared roleId {} for {} users", roleId, users.size());
    }

    @Transactional
    public void deleteUser(UUID id) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);

        if (user.getKeycloakId() != null) {
            keycloakUserProvisioner.deactivate(user.getKeycloakId());
        }
        user.softDelete(null);
        userRepository.save(user);
        log.info("User soft-deleted: {}", id);
    }

    public List<StaffPerformanceResponse> getStaffPerformance(UUID orgId) {
        var users = userRepository.findAllByOrgIdAndDeletedFalse(orgId);
        return users.stream()
                .map(u -> StaffPerformanceResponse.builder()
                        .userId(u.getId())
                        .name(u.getName())
                        .role(u.getRole().name())
                        .totalOrders(0)
                        .completedOrders(0)
                        .revenue(java.math.BigDecimal.ZERO)
                        .activeOrders(0)
                        .build())
                .toList();
    }

    private void syncKeycloak(User user, UpdateUserRequest request, Map<String, Object> keycloakProfile) {
        if (user.getKeycloakId() == null) {
            return;
        }
        try {
            if (!keycloakProfile.isEmpty()) {
                keycloakUserProvisioner.updateProfile(user.getKeycloakId(), keycloakProfile);
            }
            if (request.getPassword() != null) {
                keycloakUserProvisioner.updatePassword(user.getKeycloakId(), request.getPassword());
            }
            if (request.getIsActive() != null) {
                keycloakUserProvisioner.setActive(user.getKeycloakId(), request.getIsActive());
            }
        } catch (Exception ex) {
            log.error("Failed to sync Keycloak user '{}'", user.getKeycloakId(), ex);
            throw UserErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }
    }

    private UserRole resolveRole(UserRole requested, UUID roleId) {
        if (requested != null) {
            return requested;
        }
        if (roleId == null) {
            return UserRole.WAITER;
        }
        var role = unwrap(roleServiceClient.getRole(roleId));
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            return UserRole.ADMIN;
        }
        boolean kitchenManager = role.getPermissions() != null
                && role.getPermissions().containsAll(List.of("kitchen.view", "kitchen.manage"));
        return kitchenManager ? UserRole.CHEF : UserRole.WAITER;
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }

    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("External service returned unsuccessful response");
        }
        return response.getData();
    }

}
