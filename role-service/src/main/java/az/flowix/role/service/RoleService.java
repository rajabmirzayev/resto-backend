package az.flowix.role.service;

import az.flowix.role.client.UserServiceClient;
import az.flowix.role.dto.CreateRoleRequest;
import az.flowix.role.dto.RoleResponse;
import az.flowix.role.dto.UpdateRoleRequest;
import az.flowix.role.entity.Role;
import az.flowix.role.error.RoleErrorCode;
import az.flowix.role.mapper.RoleMapper;
import az.flowix.role.repository.RoleRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserServiceClient userServiceClient;

    public RoleService(RoleRepository roleRepository,
                       RoleMapper roleMapper,
                       UserServiceClient userServiceClient) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.userServiceClient = userServiceClient;
    }

    public List<RoleResponse> getAllRoles(UUID orgId) {
        if (orgId != null) {
            return roleMapper.toDtoList(
                    roleRepository.findAllByOrgIdAndDeletedFalse(orgId));
        }
        return roleMapper.toDtoList(
                roleRepository.findAllByDeletedFalseOrderByCreatedAtDesc());
    }

    public RoleResponse getRoleById(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .map(roleMapper::toDto)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        var role = Role.builder()
                .name(request.getName().trim())
                .permissions(request.getPermissions())
                .orgId(request.getOrgId())
                .isSystem(false)
                .build();
        role = roleRepository.save(role);
        log.info("Role created: {} ({})", role.getName(), role.getId());
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        var role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);

        if (role.isSystem()) {
            throw RoleErrorCode.ROLE_IS_SYSTEM.forbidden();
        }

        if (request.getName() != null) {
            role.setName(request.getName().trim());
        }
        if (request.getPermissions() != null) {
            role.setPermissions(request.getPermissions());
        }

        role = roleRepository.save(role);
        log.info("Role updated: {} ({})", role.getName(), role.getId());
        return roleMapper.toDto(role);
    }

    @Transactional
    public void deleteRole(UUID id) {
        var role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);

        if (role.isSystem()) {
            throw RoleErrorCode.ROLE_IS_SYSTEM.forbidden();
        }

        role.softDelete(null);
        roleRepository.save(role);

        userServiceClient.clearRole(id);

        log.info("Role soft-deleted: {}", id);
    }

    public Role getRoleEntity(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
    }

}
