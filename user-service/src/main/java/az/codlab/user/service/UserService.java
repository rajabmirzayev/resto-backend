package az.codlab.user.service;

import az.codlab.user.dto.CreateUserRequest;
import az.codlab.user.dto.StaffPerformanceResponse;
import az.codlab.user.dto.UpdateUserRequest;
import az.codlab.user.dto.UserResponse;
import az.codlab.user.entity.User;
import az.codlab.user.entity.UserRole;
import az.codlab.user.error.UserErrorCode;
import az.codlab.user.mapper.UserMapper;
import az.codlab.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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

    // TODO: role-service-den role melumatlarini cekib UserRole enum-nu avtomatik teyin et
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw UserErrorCode.USERNAME_DUPLICATE.conflict();
        }

        var user = User.builder()
                .name(request.getName().trim())
                .username(request.getUsername().trim())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .phone(request.getPhone())
                .roleId(request.getRoleId())
                .orgId(request.getOrgId())
                .role(UserRole.WAITER)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // TODO: Keycloak-da da istifadecini yarat
        log.info("User created: {} ({})", user.getName(), user.getId());
        return userMapper.toDto(user);
    }

    // TODO: role-service-den role melumatlarini cekib UserRole enum-nu avtomatik teyin et (roleId deyisilende)
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);

        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getRoleId() != null) {
            user.setRoleId(request.getRoleId());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        user = userRepository.save(user);
        // TODO: Keycloak-da melumatlari yenile
        log.info("User updated: {} ({})", user.getName(), user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
        // TODO: Keycloak-da istifadecini deaktiv et
        user.softDelete(null);
        userRepository.save(user);
        log.info("User soft-deleted: {}", id);
    }

    // TODO: order-service hazir olanda real melumatlarla doldur
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

}
