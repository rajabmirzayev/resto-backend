package az.flowix.table.mapper;

import az.flowix.table.dto.SectionResponse;
import az.flowix.table.entity.Section;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SectionMapper {

    SectionResponse toDto(Section entity);

    List<SectionResponse> toDtoList(List<Section> entities);

}
