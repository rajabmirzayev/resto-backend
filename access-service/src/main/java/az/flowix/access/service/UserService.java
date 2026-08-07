package az.flowix.access.service;

import az.flowix.access.client.KeycloakAdminClient;
import az.flowix.access.dto.CreateUserRequest;
import az.flowix.access.dto.StaffPerformanceResponse;
import az.flowix.access.dto.UpdateUserRequest;
import az.flowix.access.dto.UserDto;
import az.flowix.access.entity.Role;
import az.flowix.access.entity.User;
import az.flowix.access.error.RoleErrorCode;
import az.flowix.access.error.UserErrorCode;
import az.flowix.access.error.UserException;
import az.flowix.access.mapper.UserMapper;
import az.flowix.access.repository.RoleRepository;
import az.flowix.access.repository.UserRepository;
import az.flowix.access.repository.UserSpecifications;
import az.flowix.common.dto.PageDto;
import az.flowix.common.security.context.SecurityContextFacade;
import az.flowix.common.util.PhoneUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakUserProvisioner keycloakUserProvisioner;
    private final KeycloakSyncService keycloakSyncService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       KeycloakAdminClient keycloakAdminClient,
                       KeycloakUserProvisioner keycloakUserProvisioner,
                       KeycloakSyncService keycloakSyncService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakUserProvisioner = keycloakUserProvisioner;
        this.keycloakSyncService = keycloakSyncService;
    }

    public PageDto<UserDto> getAllUsers(UUID orgId, UUID roleId, String q, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Specification<User> spec = combine(
                UserSpecifications.active(),
                UserSpecifications.orgId(effectiveOrgId(orgId)),
                UserSpecifications.roleId(roleId),
                UserSpecifications.q(q));

        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        if (result.getContent().isEmpty()) {
            return PageDto.of(List.of(), result.getTotalElements(), result.getNumber(), result.getSize());
        }

        List<User> hydrated = userRepository.findAllByIdInWithRole(
                result.getContent().stream().map(User::getId).toList());
        Map<UUID, User> byId = hydrated.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserDto> dtos = result.getContent().stream()
                .map(u -> userMapper.toDto(byId.getOrDefault(u.getId(), u)))
                .toList();
        return PageDto.of(dtos, result.getTotalElements(), result.getNumber(), result.getSize());
    }

    public UserDto getUserById(UUID id) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
        enforceUserOrgAccess(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw UserErrorCode.USERNAME_DUPLICATE.conflict();
        }

        enforceOrgScope(request.getOrgId());

        String email = normalizeEmail(request.getEmail());
        ensureKeycloakUsernameFree(request.getUsername(), email);

        var role = resolveRole(request.getRoleId(), request.getOrgId());

        var user = User.builder()
                .name(request.getName().trim())
                .username(request.getUsername().trim())
                .email(email)
                .phone(PhoneUtils.normalize(request.getPhone()))
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .orgId(request.getOrgId())
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
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
        enforceUserOrgAccess(user);

        Map<String, Object> keycloakProfile = new LinkedHashMap<>();
        if (request.getName() != null) {
            user.setName(request.getName().trim());
            keycloakProfile.put("firstName", user.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(PhoneUtils.normalize(request.getPhone()));
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        syncKeycloak(user, keycloakProfile, request.getIsActive());
        user = userRepository.save(user);

        log.info("User updated: {} ({})", user.getName(), user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public void unassignRole(UUID userId) {
        var user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
        enforceUserOrgAccess(user);

        if (user.getRole() == null) {
            return;
        }

        user.setRole(null);
        userRepository.save(user);
        keycloakSyncService.clearUserRole(user);
        log.info("Role unassigned from user {}", userId);
    }

    @Transactional
    public void deleteByUsername(String username) {
        userRepository.findByUsernameAndDeletedFalse(username).ifPresent(user -> {
            enforceUserOrgAccess(user);
            if (user.getKeycloakId() != null) {
                keycloakUserProvisioner.deactivate(user.getKeycloakId());
            }
            user.softDelete(null);
            userRepository.save(user);
            log.info("User soft-deleted by username: {} ({})", username, user.getId());
        });
    }

    @Transactional
    public void deleteUser(UUID id) {
        var user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);
        enforceUserOrgAccess(user);

        if (user.getKeycloakId() != null) {
            keycloakUserProvisioner.deactivate(user.getKeycloakId());
        }
        user.softDelete(null);
        userRepository.save(user);
        log.info("User soft-deleted: {}", id);
    }

    public List<StaffPerformanceResponse> getStaffPerformance(UUID orgId, UUID roleId) {
        UUID effectiveOrg = effectiveOrgId(orgId);
        List<User> users = roleId != null
                ? userRepository.findAllByOrgIdAndRole_IdAndDeletedFalse(effectiveOrg, roleId)
                : userRepository.findAllByOrgIdAndDeletedFalse(effectiveOrg);
        return users.stream()
                .map(u -> StaffPerformanceResponse.builder()
                        .userId(u.getId())
                        .name(u.getName())
                        .role(u.getRole() == null ? null : u.getRole().getCode())
                        .totalOrders(0)
                        .completedOrders(0)
                        .revenue(BigDecimal.ZERO)
                        .activeOrders(0)
                        .build())
                .toList();
    }

    private void syncKeycloak(User user, Map<String, Object> keycloakProfile, Boolean isActive) {
        if (user.getKeycloakId() == null) {
            return;
        }
        try {
            if (!keycloakProfile.isEmpty()) {
                keycloakUserProvisioner.updateProfile(user.getKeycloakId(), keycloakProfile);
            }
            if (isActive != null) {
                keycloakUserProvisioner.setActive(user.getKeycloakId(), isActive);
            }
        } catch (Exception ex) {
            log.error("Failed to sync Keycloak user '{}'", user.getKeycloakId(), ex);
            throw UserErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }
    }

    private void ensureKeycloakUsernameFree(String username, String email) {
        try {
            if (keycloakAdminClient.userExistsByUsername(username)
                    || (email != null && keycloakAdminClient.userExistsByEmail(email))) {
                throw UserErrorCode.USERNAME_DUPLICATE.conflict();
            }
        } catch (UserException ex) {
            throw ex;
        } catch (Exception ex) {
            throw UserErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }
    }

    private Role resolveRole(UUID roleId, UUID orgId) {
        var role = roleRepository.findByIdAndDeletedFalse(roleId)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
        if (role.getOrgId() != null && !role.getOrgId().equals(orgId)) {
            throw RoleErrorCode.ROLE_ORG_MISMATCH.forbidden();
        }
        return role;
    }

    /**
     * Non-admins are confined to their own organization; a requested org that
     * differs from the caller's org is rejected. Platform admins see everything.
     */
    private UUID effectiveOrgId(UUID requestedOrgId) {
        if (SecurityContextFacade.isPlatformAdmin()) {
            return requestedOrgId;
        }
        UUID currentOrg = currentOrgId();
        if (currentOrg == null) {
            throw UserErrorCode.USER_ORG_MISMATCH.forbidden();
        }
        if (requestedOrgId != null && !requestedOrgId.equals(currentOrg)) {
            throw UserErrorCode.USER_ORG_MISMATCH.forbidden();
        }
        return currentOrg;
    }

    private void enforceOrgScope(UUID orgId) {
        if (SecurityContextFacade.isPlatformAdmin()) {
            return;
        }
        UUID currentOrg = currentOrgId();
        if (currentOrg == null || orgId == null || !orgId.equals(currentOrg)) {
            throw UserErrorCode.USER_ORG_MISMATCH.forbidden();
        }
    }

    private void enforceUserOrgAccess(User user) {
        if (SecurityContextFacade.isPlatformAdmin()) {
            return;
        }
        UUID currentOrg = currentOrgId();
        if (currentOrg == null || !currentOrg.equals(user.getOrgId())) {
            throw UserErrorCode.USER_ORG_MISMATCH.forbidden();
        }
    }

    private UUID currentOrgId() {
        String orgId = SecurityContextFacade.getCurrentOrgId();
        if (orgId == null || orgId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(orgId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }

    @SafeVarargs
    private static Specification<User> combine(Specification<User>... specs) {
        List<Specification<User>> nonNull = Arrays.stream(specs)
                .filter(Objects::nonNull)
                .toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        if (nonNull.size() == 1) {
            return nonNull.get(0);
        }
        return nonNull.stream().reduce(Specification::and).orElse(null);
    }

}
