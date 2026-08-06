package az.flowix.access.mapper;

import az.flowix.access.dto.RoleResponse;
import az.flowix.access.entity.Role;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "isSystem", source = "system")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "permissionIds", ignore = true)
    RoleResponse toDto(Role entity);

    List<RoleResponse> toDtoList(List<Role> entities);

}
