package az.codlab.table.mapper;

import az.codlab.table.dto.SectionResponse;
import az.codlab.table.entity.Section;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SectionMapper {

    SectionResponse toDto(Section entity);

    List<SectionResponse> toDtoList(List<Section> entities);

}
