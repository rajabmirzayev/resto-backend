package az.flowix.access.mapper;

import az.flowix.access.dto.ModuleDto;
import az.flowix.access.dto.ModuleRefDto;
import az.flowix.access.dto.ModuleTreeDto;
import az.flowix.access.dto.PermissionDto;
import az.flowix.access.dto.UiGroupDto;
import az.flowix.access.dto.UiGroupRefDto;
import az.flowix.access.entity.Module;
import az.flowix.access.entity.Permission;
import az.flowix.access.entity.UiGroup;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionDto toDto(Permission permission, Module module, UiGroup uiGroup) {
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(toModuleRef(module))
                .uiGroup(toUiGroupRef(uiGroup))
                .sortOrder(permission.getSortOrder())
                .active(permission.isActive())
                .build();
    }

    public List<PermissionDto> toDtoList(List<Permission> permissions,
                                         Map<UUID, Module> modules,
                                         Map<UUID, UiGroup> uiGroups) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return permissions.stream()
                .map(p -> toDto(p, modules.get(p.getModuleId()), uiGroups.get(p.getUiGroupId())))
                .toList();
    }

    public UiGroupDto toUiGroupDto(UiGroup uiGroup, Module module, List<Permission> permissions) {
        return UiGroupDto.builder()
                .id(uiGroup.getId())
                .code(uiGroup.getCode())
                .name(uiGroup.getName())
                .sortOrder(uiGroup.getSortOrder())
                .permissions(toDtoList(permissions,
                        module != null ? Map.of(module.getId(), module) : Map.of(),
                        Map.of(uiGroup.getId(), uiGroup)))
                .build();
    }

    public ModuleDto toModuleDto(Module module, List<UiGroupDto> uiGroups) {
        return ModuleDto.builder()
                .id(module.getId())
                .code(module.getCode())
                .name(module.getName())
                .sortOrder(module.getSortOrder())
                .uiGroups(uiGroups != null ? uiGroups : List.of())
                .build();
    }

    public ModuleTreeDto toModuleTreeDto(Module module, List<UiGroupDto> uiGroups) {
        return ModuleTreeDto.builder()
                .id(module.getId())
                .code(module.getCode())
                .name(module.getName())
                .sortOrder(module.getSortOrder())
                .uiGroups(uiGroups != null ? uiGroups : List.of())
                .build();
    }

    public ModuleRefDto toModuleRef(Module module) {
        if (module == null) {
            return null;
        }
        return ModuleRefDto.builder()
                .id(module.getId())
                .code(module.getCode())
                .name(module.getName())
                .build();
    }

    public UiGroupRefDto toUiGroupRef(UiGroup uiGroup) {
        if (uiGroup == null) {
            return null;
        }
        return UiGroupRefDto.builder()
                .id(uiGroup.getId())
                .code(uiGroup.getCode())
                .name(uiGroup.getName())
                .build();
    }

}
