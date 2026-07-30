package az.codlab.organization.mapper;

import az.codlab.organization.dto.OrganizationDto;
import az.codlab.organization.entity.Organization;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationDto toDto(Organization entity);

    List<OrganizationDto> toDtoList(List<Organization> entities);

}
