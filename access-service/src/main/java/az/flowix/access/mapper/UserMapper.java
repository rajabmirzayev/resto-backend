package az.flowix.access.mapper;

import az.flowix.access.dto.UserDto;
import az.flowix.access.entity.User;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "active", source = "active")
    UserDto toDto(User entity);

    List<UserDto> toDtoList(List<User> entities);

}
