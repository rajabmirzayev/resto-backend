package az.codlab.role.mapper;

import az.codlab.role.dto.RoleResponse;
import az.codlab.role.entity.Role;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "isSystem", source = "system")
    RoleResponse toDto(Role entity);

    List<RoleResponse> toDtoList(List<Role> entities);

}
