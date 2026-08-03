package az.flowix.menu.mapper;

import az.flowix.menu.dto.CategoryResponse;
import az.flowix.menu.entity.MenuCategory;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuCategoryMapper {

    CategoryResponse toDto(MenuCategory entity);

    List<CategoryResponse> toDtoList(List<MenuCategory> entities);

}
