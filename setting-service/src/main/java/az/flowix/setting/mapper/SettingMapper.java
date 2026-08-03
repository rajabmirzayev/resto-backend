package az.flowix.setting.mapper;

import az.flowix.setting.dto.SettingResponse;
import az.flowix.setting.entity.OrgSetting;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    @Mapping(target = "orderMode", source = "orderMode")
    @Mapping(target = "paymentTiming", source = "paymentTiming")
    @Mapping(target = "customerTheme", source = "customerTheme")
    SettingResponse toDto(OrgSetting entity);

}
