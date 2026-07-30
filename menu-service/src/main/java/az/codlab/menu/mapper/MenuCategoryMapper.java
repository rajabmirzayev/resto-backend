package az.codlab.menu.mapper;

import az.codlab.menu.dto.CategoryResponse;
import az.codlab.menu.entity.MenuCategory;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuCategoryMapper {

    CategoryResponse toDto(MenuCategory entity);

    List<CategoryResponse> toDtoList(List<MenuCategory> entities);

}
