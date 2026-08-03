package az.flowix.organization.mapper;

import az.flowix.organization.dto.OrganizationDto;
import az.flowix.organization.entity.Organization;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationDto toDto(Organization entity);

    List<OrganizationDto> toDtoList(List<Organization> entities);

}
