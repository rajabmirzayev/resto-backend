package az.codlab.table.mapper;

import az.codlab.table.dto.TableResponse;
import az.codlab.table.entity.RestaurantTable;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TableMapper {

    @Mapping(target = "status", source = "status")
    TableResponse toDto(RestaurantTable entity);

    List<TableResponse> toDtoList(List<RestaurantTable> entities);

}
