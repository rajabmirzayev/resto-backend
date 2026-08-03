package az.flowix.role.mapper;

import az.flowix.role.dto.RoleResponse;
import az.flowix.role.entity.Role;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "isSystem", source = "system")
    RoleResponse toDto(Role entity);

    List<RoleResponse> toDtoList(List<Role> entities);

}
