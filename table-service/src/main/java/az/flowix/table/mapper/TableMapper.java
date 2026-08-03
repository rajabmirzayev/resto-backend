package az.flowix.table.mapper;

import az.flowix.table.dto.TableResponse;
import az.flowix.table.entity.RestaurantTable;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableMapper {

    TableResponse toDto(RestaurantTable entity);

    List<TableResponse> toDtoList(List<RestaurantTable> entities);

}
