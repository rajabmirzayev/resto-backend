package az.flowix.menu.mapper;

import az.flowix.menu.dto.MenuItemResponse;
import az.flowix.menu.entity.MenuItem;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    @Mapping(target = "available", source = "available")
    MenuItemResponse toDto(MenuItem entity);

    List<MenuItemResponse> toDtoList(List<MenuItem> entities);

}
