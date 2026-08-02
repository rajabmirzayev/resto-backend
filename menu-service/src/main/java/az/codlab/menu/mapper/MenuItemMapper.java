package az.codlab.menu.mapper;

import az.codlab.menu.dto.MenuItemResponse;
import az.codlab.menu.entity.MenuItem;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    @Mapping(target = "available", source = "available")
    MenuItemResponse toDto(MenuItem entity);

    List<MenuItemResponse> toDtoList(List<MenuItem> entities);

}
