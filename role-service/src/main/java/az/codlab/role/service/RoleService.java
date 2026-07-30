package az.codlab.role.service;

import az.codlab.role.dto.CreateRoleRequest;
import az.codlab.role.dto.RoleResponse;
import az.codlab.role.dto.UpdateRoleRequest;
import az.codlab.role.entity.Role;
import az.codlab.role.error.RoleErrorCode;
import az.codlab.role.mapper.RoleMapper;
import az.codlab.role.repository.RoleRepository;

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

    public RoleService(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
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

        // TODO: user-service hazir olanda bu rola aid userlerin roleId-ni null set et (cascade set-null)
        role.softDelete(null);
        roleRepository.save(role);
        log.info("Role soft-deleted: {}", id);
    }

    public Role getRoleEntity(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(RoleErrorCode.ROLE_NOT_FOUND::notFound);
    }

}
