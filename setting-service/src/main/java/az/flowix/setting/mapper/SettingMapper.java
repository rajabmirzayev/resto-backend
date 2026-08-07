package az.flowix.setting.mapper;

import az.flowix.setting.dto.SettingResponse;
import az.flowix.setting.entity.OrgSetting;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    SettingResponse toDto(OrgSetting entity);

}
