package az.codlab.user.mapper;

import az.codlab.user.dto.UserResponse;
import az.codlab.user.entity.User;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "isActive", source = "active")
    UserResponse toDto(User entity);

    List<UserResponse> toDtoList(List<User> entities);

}
