package az.flowix.access.service;

import az.flowix.access.dto.ModuleDto;
import az.flowix.access.dto.ModuleTreeDto;
import az.flowix.access.dto.PermissionDto;
import az.flowix.access.dto.UiGroupDto;
import az.flowix.access.entity.Module;
import az.flowix.access.entity.Permission;
import az.flowix.access.entity.UiGroup;
import az.flowix.access.mapper.PermissionMapper;
import az.flowix.access.repository.ModuleRepository;
import az.flowix.access.repository.PermissionRepository;
import az.flowix.access.repository.PermissionSpecifications;
import az.flowix.access.repository.UiGroupRepository;
import az.flowix.common.dto.PageDto;
import az.flowix.common.security.context.SecurityContextFacade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Comparator<Module> MODULE_ORDER = Comparator.comparingInt(Module::getSortOrder);
    private static final Comparator<UiGroup> UI_GROUP_ORDER = Comparator.comparingInt(UiGroup::getSortOrder);
    private static final Comparator<Permission> PERMISSION_ORDER = Comparator.comparingInt(Permission::getSortOrder);

    private final PermissionRepository permissionRepository;
    private final ModuleRepository moduleRepository;
    private final UiGroupRepository uiGroupRepository;
    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionRepository permissionRepository,
                             ModuleRepository moduleRepository,
                             UiGroupRepository uiGroupRepository,
                             PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.moduleRepository = moduleRepository;
        this.uiGroupRepository = uiGroupRepository;
        this.permissionMapper = permissionMapper;
    }

    public List<PermissionDto> getMyPermissions() {
        Set<String> codes = SecurityContextFacade.getPermissions();
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return codes.stream()
                .sorted()
                .map(code -> PermissionDto.builder().code(code).build())
                .toList();
    }

    public PageDto<PermissionDto> search(String q, String moduleCode, String uiGroupCode, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        UUID moduleId = resolveModuleId(moduleCode);
        if (moduleCode != null && !moduleCode.isBlank() && moduleId == null) {
            return PageDto.of(List.of(), 0, safePage, safeSize);
        }
        UUID uiGroupId = resolveUiGroupId(uiGroupCode);
        if (uiGroupCode != null && !uiGroupCode.isBlank() && uiGroupId == null) {
            return PageDto.of(List.of(), 0, safePage, safeSize);
        }

        Specification<Permission> spec = combine(
                PermissionSpecifications.active(),
                PermissionSpecifications.q(q),
                PermissionSpecifications.moduleId(moduleId),
                PermissionSpecifications.uiGroupId(uiGroupId));

        Page<Permission> result = permissionRepository.findAll(
                spec, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "sortOrder")));
        return PageDto.of(toPermissionDtos(result.getContent()), result.getTotalElements(), safePage, safeSize);
    }

    public List<ModuleTreeDto> getTree(String q) {
        String filter = normalize(q);
        Map<UUID, Module> modulesById = activeModulesById();
        Map<UUID, List<UiGroup>> uiGroupsByModule = activeUiGroups().stream()
                .collect(Collectors.groupingBy(UiGroup::getModuleId));
        Map<UUID, List<Permission>> permissionsByUiGroup = activePermissionsByUiGroup();

        return modulesById.values().stream()
                .sorted(MODULE_ORDER)
                .map(module -> {
                    List<UiGroupDto> groupDtos = uiGroupsByModule.getOrDefault(module.getId(), List.of()).stream()
                            .sorted(UI_GROUP_ORDER)
                            .map(group -> buildUiGroup(group, module, permissionsByUiGroup.getOrDefault(
                                    group.getId(), List.of()).stream()
                                    .filter(p -> filter == null || matches(p, filter))
                                    .sorted(PERMISSION_ORDER)
                                    .toList()))
                            .toList();
                    return permissionMapper.toModuleTreeDto(module, groupDtos);
                })
                .toList();
    }

    public List<PermissionDto> getByModule(String moduleCode, String q) {
        UUID moduleId = resolveModuleId(moduleCode);
        if (moduleId == null) {
            return List.of();
        }
        Specification<Permission> spec = combine(
                PermissionSpecifications.active(),
                PermissionSpecifications.q(q),
                PermissionSpecifications.moduleId(moduleId));
        List<Permission> permissions = permissionRepository.findAll(spec,
                Sort.by(Sort.Direction.ASC, "sortOrder"));
        return toPermissionDtos(permissions);
    }

    public List<PermissionDto> getByUiGroup(String uiGroupCode, String q) {
        UUID uiGroupId = resolveUiGroupId(uiGroupCode);
        if (uiGroupId == null) {
            return List.of();
        }
        Specification<Permission> spec = combine(
                PermissionSpecifications.active(),
                PermissionSpecifications.q(q),
                PermissionSpecifications.uiGroupId(uiGroupId));
        List<Permission> permissions = permissionRepository.findAll(spec,
                Sort.by(Sort.Direction.ASC, "sortOrder"));
        return toPermissionDtos(permissions);
    }

    public List<ModuleDto> getModules(String q) {
        String filter = normalize(q);
        Map<UUID, List<UiGroup>> uiGroupsByModule = activeUiGroups().stream()
                .collect(Collectors.groupingBy(UiGroup::getModuleId));
        Map<UUID, List<Permission>> permissionsByUiGroup = activePermissionsByUiGroup();

        return activeModulesById().values().stream()
                .filter(module -> filter == null || matches(module, filter))
                .sorted(MODULE_ORDER)
                .map(module -> {
                    List<UiGroupDto> groupDtos = uiGroupsByModule.getOrDefault(module.getId(), List.of()).stream()
                            .sorted(UI_GROUP_ORDER)
                            .map(group -> buildUiGroup(group, module, permissionsByUiGroup.getOrDefault(
                                    group.getId(), List.of()).stream()
                                    .sorted(PERMISSION_ORDER)
                                    .toList()))
                            .toList();
                    return permissionMapper.toModuleDto(module, groupDtos);
                })
                .toList();
    }

    public List<UiGroupDto> getUiGroups(String q, String moduleCode) {
        String filter = normalize(q);
        UUID moduleId = resolveModuleId(moduleCode);
        if (moduleCode != null && !moduleCode.isBlank() && moduleId == null) {
            return List.of();
        }
        Map<UUID, Module> modulesById = activeModulesById();
        Map<UUID, List<Permission>> permissionsByUiGroup = activePermissionsByUiGroup();

        return activeUiGroups().stream()
                .filter(group -> moduleId == null || group.getModuleId().equals(moduleId))
                .filter(group -> filter == null || matches(group, filter))
                .sorted(UI_GROUP_ORDER)
                .map(group -> buildUiGroup(group, modulesById.get(group.getModuleId()),
                        permissionsByUiGroup.getOrDefault(group.getId(), List.of()).stream()
                                .sorted(PERMISSION_ORDER)
                                .toList()))
                .toList();
    }

    private UiGroupDto buildUiGroup(UiGroup group, Module module, List<Permission> permissions) {
        return permissionMapper.toUiGroupDto(group, module, permissions);
    }

    @SafeVarargs
    private Specification<Permission> combine(Specification<Permission>... specifications) {
        return java.util.Arrays.stream(specifications)
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(null);
    }

    private List<PermissionDto> toPermissionDtos(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        Set<UUID> moduleIds = permissions.stream()
                .map(Permission::getModuleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> uiGroupIds = permissions.stream()
                .map(Permission::getUiGroupId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Module> modules = moduleIds.isEmpty()
                ? Map.of()
                : moduleRepository.findAllById(moduleIds).stream()
                        .collect(Collectors.toMap(Module::getId, Function.identity()));
        Map<UUID, UiGroup> uiGroups = uiGroupIds.isEmpty()
                ? Map.of()
                : uiGroupRepository.findAllById(uiGroupIds).stream()
                        .collect(Collectors.toMap(UiGroup::getId, Function.identity()));
        return permissionMapper.toDtoList(permissions, modules, uiGroups);
    }

    private Map<UUID, List<Permission>> activePermissionsByUiGroup() {
        return permissionRepository.findAllByDeletedFalseAndIsActiveTrue().stream()
                .filter(p -> p.getUiGroupId() != null)
                .collect(Collectors.groupingBy(Permission::getUiGroupId));
    }

    private Map<UUID, Module> activeModulesById() {
        return moduleRepository.findAllByDeletedFalseAndIsActiveTrue().stream()
                .collect(Collectors.toMap(Module::getId, Function.identity()));
    }

    private List<UiGroup> activeUiGroups() {
        return uiGroupRepository.findAllByDeletedFalseAndIsActiveTrue();
    }

    private UUID resolveModuleId(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return null;
        }
        return moduleRepository.findByCodeAndDeletedFalse(moduleCode.trim())
                .map(Module::getId).orElse(null);
    }

    private UUID resolveUiGroupId(String uiGroupCode) {
        if (uiGroupCode == null || uiGroupCode.isBlank()) {
            return null;
        }
        return uiGroupRepository.findByCodeAndDeletedFalse(uiGroupCode.trim())
                .map(UiGroup::getId).orElse(null);
    }

    private static String normalize(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String value = q.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    private static boolean matches(Module module, String filter) {
        return contains(module.getCode(), filter) || contains(module.getName(), filter);
    }

    private static boolean matches(UiGroup group, String filter) {
        return contains(group.getCode(), filter) || contains(group.getName(), filter);
    }

    private static boolean matches(Permission permission, String filter) {
        return contains(permission.getCode(), filter) || contains(permission.getName(), filter);
    }

    private static boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

}
