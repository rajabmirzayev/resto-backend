package az.flowix.access.service;

import az.flowix.access.dto.AssignRoleRequest;
import az.flowix.access.dto.CreateRoleRequest;
import az.flowix.access.dto.RoleBriefDto;
import az.flowix.access.dto.RolePermissionRequest;
import az.flowix.access.dto.RoleResponse;
import az.flowix.access.dto.UpdateRoleRequest;
import az.flowix.access.dto.UserDto;
import az.flowix.access.entity.Module;
import az.flowix.access.entity.Permission;
import az.flowix.access.entity.Role;
import az.flowix.access.entity.UiGroup;
import az.flowix.access.entity.User;
import az.flowix.access.error.RoleErrorCode;
import az.flowix.access.error.UserErrorCode;
import az.flowix.access.mapper.PermissionMapper;
import az.flowix.access.mapper.RoleMapper;
import az.flowix.access.repository.ModuleRepository;
import az.flowix.access.repository.PermissionRepository;
import az.flowix.access.repository.RoleRepository;
import az.flowix.access.repository.RoleSpecifications;
import az.flowix.access.repository.UiGroupRepository;
import az.flowix.access.repository.UserRepository;
import az.flowix.common.dto.PageDto;
import az.flowix.common.security.context.SecurityContextFacade;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final UiGroupRepository uiGroupRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final KeycloakSyncService keycloakSyncService;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       UserRepository userRepository,
                       ModuleRepository moduleRepository,
                       UiGroupRepository uiGroupRepository,
                       RoleMapper roleMapper,
                       PermissionMapper permissionMapper,
                       KeycloakSyncService keycloakSyncService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.uiGroupRepository = uiGroupRepository;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.keycloakSyncService = keycloakSyncService;
    }

    public PageDto<RoleResponse> getAllRoles(String q, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Specification<Role> spec = combine(
                RoleSpecifications.active(),
                RoleSpecifications.q(q),
                tenantScope());

        Page<Role> result = roleRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        if (result.getContent().isEmpty()) {
            return PageDto.of(List.of(), result.getTotalElements(), result.getNumber(), result.getSize());
        }

        List<Role> hydrated = roleRepository.findAllByIdInWithPermissions(
                result.getContent().stream().map(Role::getId).toList());
        Map<UUID, Role> byId = hydrated.stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        List<RoleResponse> dtos = result.getContent().stream()
                .map(r -> toRoleResponse(byId.getOrDefault(r.getId(), r)))
                .toList();
        return PageDto.of(dtos, result.getTotalElements(), result.getNumber(), result.getSize());
    }

    public RoleResponse getRoleById(UUID id) {
        return toRoleResponse(findVisibleRole(id));
    }

    public RoleResponse getSystemRoleByCode(String code) {
        return roleRepository.findByCodeAndOrgIdIsNullAndDeletedFalse(code)
                .map(this::toRoleResponse)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        boolean admin = SecurityContextFacade.isPlatformAdmin();
        UUID currentOrg = currentOrgId();
        UUID orgId = request.getOrgId() != null ? request.getOrgId() : currentOrg;

        if (!admin && (orgId == null || currentOrg == null || !orgId.equals(currentOrg))) {
            throw RoleErrorCode.ROLE_ORG_MISMATCH.forbidden();
        }

        String code = request.getCode().trim();
        if (orgId != null) {
            if (roleRepository.existsByCodeAndOrgIdAndDeletedFalse(code, orgId)) {
                throw RoleErrorCode.ROLE_CODE_DUPLICATE.conflict();
            }
        } else if (roleRepository.existsByCodeAndOrgIdIsNullAndDeletedFalse(code)) {
            throw RoleErrorCode.ROLE_CODE_DUPLICATE.conflict();
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());

        Role role = Role.builder()
                .code(code)
                .name(request.getName().trim())
                .uiScope(request.getUiScope())
                .orgId(orgId)
                .isSystem(false)
                .isActive(true)
                .permissions(permissions)
                .build();
        role = roleRepository.save(role);
        log.info("Role created: {} ({})", role.getName(), role.getId());
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        Role role = getOwnedRole(id);

        if (request.getName() != null) {
            role.setName(request.getName().trim());
        }
        if (request.getUiScope() != null) {
            role.setUiScope(request.getUiScope());
        }

        role = roleRepository.save(role);
        keycloakSyncService.syncUsersOfRole(role);
        log.info("Role updated: {} ({})", role.getName(), role.getId());
        return toRoleResponse(role);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = getOwnedRole(id);

        role.setPermissions(new HashSet<>());
        role.softDelete(null);
        roleRepository.save(role);

        for (User user : userRepository.findAllByRole_IdAndDeletedFalse(id)) {
            user.setRole(null);
            userRepository.save(user);
            keycloakSyncService.clearUserRole(user);
        }

        log.info("Role soft-deleted: {}", id);
    }

    @Transactional
    public RoleResponse addPermissions(UUID id, RolePermissionRequest request) {
        Role role = getOwnedRole(id);
        role.getPermissions().addAll(resolvePermissions(request.getPermissionIds()));
        role = roleRepository.save(role);
        keycloakSyncService.syncUsersOfRole(role);
        log.info("Permissions added to role {}", role.getId());
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse setPermissions(UUID id, RolePermissionRequest request) {
        Role role = getOwnedRole(id);
        role.setPermissions(resolvePermissions(request.getPermissionIds()));
        role = roleRepository.save(role);
        keycloakSyncService.syncUsersOfRole(role);
        log.info("Permissions replaced on role {}", role.getId());
        return toRoleResponse(role);
    }

    @Transactional
    public void removePermission(UUID id, UUID permissionId) {
        Role role = getOwnedRole(id);
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        roleRepository.save(role);
        keycloakSyncService.syncUsersOfRole(role);
        log.info("Permission {} removed from role {}", permissionId, role.getId());
    }

    @Transactional
    public void assignUsers(UUID id, AssignRoleRequest request) {
        Role role = findVisibleRole(id);
        boolean admin = SecurityContextFacade.isPlatformAdmin();
        UUID orgId = currentOrgId();

        Set<UUID> uniqueIds = new HashSet<>(request.getUserIds());
        List<User> users = userRepository.findAllByIdInAndDeletedFalse(uniqueIds);
        if (users.size() != uniqueIds.size()) {
            throw UserErrorCode.USER_NOT_FOUND.notFound();
        }

        for (User user : users) {
            if (!admin && (orgId == null || !orgId.equals(user.getOrgId()))) {
                throw UserErrorCode.USER_ORG_MISMATCH.forbidden();
            }
        }

        for (User user : users) {
            user.setRole(role);
            userRepository.save(user);
            keycloakSyncService.syncUserRole(user);
        }
        log.info("Role {} assigned to {} users", role.getId(), users.size());
    }

    @Transactional
    public void unassignUser(UUID id, UUID userId) {
        Role role = findVisibleRole(id);
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::notFound);

        if (user.getRole() == null || !role.getId().equals(user.getRole().getId())) {
            return;
        }

        user.setRole(null);
        userRepository.save(user);
        keycloakSyncService.clearUserRole(user);
        log.info("Role {} unassigned from user {}", role.getId(), userId);
    }

    public PageDto<UserDto> getRoleUsers(UUID id, String q, int page, int size) {
        findVisibleRole(id);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Page<User> result = userRepository.searchByRole(id, normalizeSearch(q),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<UserDto> dtos = result.getContent().stream().map(this::toUserDto).toList();
        return PageDto.of(dtos, result.getTotalElements(), result.getNumber(), result.getSize());
    }

    public Role getRoleEntity(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
    }

    private Role findVisibleRole(UUID id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
        if (!isVisibleToCurrentOrg(role)) {
            throw RoleErrorCode.ROLE_ORG_MISMATCH.forbidden();
        }
        return role;
    }

    private Role getOwnedRole(UUID id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
        if (role.isSystem()) {
            throw RoleErrorCode.ROLE_IS_SYSTEM.forbidden();
        }
        if (!SecurityContextFacade.isPlatformAdmin() && !isOwnedByCurrentOrg(role)) {
            throw RoleErrorCode.ROLE_ORG_MISMATCH.forbidden();
        }
        return role;
    }

    private boolean isVisibleToCurrentOrg(Role role) {
        if (SecurityContextFacade.isPlatformAdmin()) {
            return true;
        }
        if (role.isSystem() && role.getOrgId() == null) {
            return true;
        }
        return isOwnedByCurrentOrg(role);
    }

    private boolean isOwnedByCurrentOrg(Role role) {
        UUID orgId = currentOrgId();
        return orgId != null && orgId.equals(role.getOrgId());
    }

    private Specification<Role> tenantScope() {
        if (SecurityContextFacade.isPlatformAdmin()) {
            return null;
        }
        return RoleSpecifications.visibleToOrg(currentOrgId());
    }

    private Set<Permission> resolvePermissions(List<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<UUID> uniqueIds = new HashSet<>(permissionIds);
        List<Permission> found = permissionRepository.findAllByIdIn(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            throw RoleErrorCode.PERMISSION_NOT_FOUND.notFound();
        }
        return new HashSet<>(found);
    }

    private RoleResponse toRoleResponse(Role role) {
        RoleResponse dto = roleMapper.toDto(role);

        Set<Permission> permissions = role.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            dto.setPermissionIds(List.of());
            dto.setPermissions(List.of());
            return dto;
        }

        List<Permission> sorted = permissions.stream()
                .sorted(java.util.Comparator.comparingInt(Permission::getSortOrder)
                        .thenComparing(Permission::getCode))
                .toList();

        Set<UUID> moduleIds = sorted.stream()
                .map(Permission::getModuleId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> uiGroupIds = sorted.stream()
                .map(Permission::getUiGroupId).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Module> modules = moduleIds.isEmpty()
                ? Map.of()
                : moduleRepository.findAllById(moduleIds).stream()
                        .collect(Collectors.toMap(Module::getId, Function.identity()));
        Map<UUID, UiGroup> uiGroups = uiGroupIds.isEmpty()
                ? Map.of()
                : uiGroupRepository.findAllById(uiGroupIds).stream()
                        .collect(Collectors.toMap(UiGroup::getId, Function.identity()));

        dto.setPermissionIds(sorted.stream().map(Permission::getId).toList());
        dto.setPermissions(permissionMapper.toDtoList(sorted, modules, uiGroups));
        return dto;
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .orgId(user.getOrgId())
                .role(toRoleBrief(user.getRole()))
                .active(user.getActive())
                .build();
    }

    private RoleBriefDto toRoleBrief(Role role) {
        if (role == null) {
            return null;
        }
        return RoleBriefDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .uiScope(role.getUiScope())
                .build();
    }

    private String normalizeSearch(String q) {
        if (q == null) {
            return "";
        }
        return q.trim();
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

    @SafeVarargs
    private static Specification<Role> combine(Specification<Role>... specs) {
        List<Specification<Role>> nonNull = Arrays.stream(specs)
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
