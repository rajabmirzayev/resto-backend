package az.codlab.setting.mapper;

import az.codlab.setting.dto.SettingResponse;
import az.codlab.setting.entity.OrgSetting;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    @Mapping(target = "orderMode", source = "orderMode")
    @Mapping(target = "paymentTiming", source = "paymentTiming")
    @Mapping(target = "customerTheme", source = "customerTheme")
    SettingResponse toDto(OrgSetting entity);

}
