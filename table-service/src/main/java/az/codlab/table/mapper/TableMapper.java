package az.codlab.table.mapper;

import az.codlab.table.dto.TableResponse;
import az.codlab.table.entity.RestaurantTable;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableMapper {

    TableResponse toDto(RestaurantTable entity);

    List<TableResponse> toDtoList(List<RestaurantTable> entities);

}
