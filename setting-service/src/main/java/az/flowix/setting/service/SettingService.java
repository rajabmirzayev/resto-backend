package az.flowix.setting.service;

import az.flowix.common.enums.CustomerTheme;
import az.flowix.common.enums.OrderMode;
import az.flowix.common.enums.PaymentTiming;
import az.flowix.setting.dto.SettingRequest;
import az.flowix.setting.dto.SettingResponse;
import az.flowix.setting.entity.OrgSetting;
import az.flowix.setting.error.SettingErrorCode;
import az.flowix.setting.mapper.SettingMapper;
import az.flowix.setting.repository.OrgSettingRepository;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final OrgSettingRepository orgSettingRepository;
    private final SettingMapper settingMapper;

    public SettingService(OrgSettingRepository orgSettingRepository,
                          SettingMapper settingMapper) {
        this.orgSettingRepository = orgSettingRepository;
        this.settingMapper = settingMapper;
    }

    public SettingResponse getSettings(UUID orgId) {
        return orgSettingRepository.findByOrgId(orgId)
                .map(settingMapper::toDto)
                .orElseThrow(SettingErrorCode.SETTINGS_NOT_FOUND::notFound);
    }

    @Transactional
    public SettingResponse updateSettings(SettingRequest request) {
        var settings = orgSettingRepository.findByOrgId(request.getOrgId())
                .orElseGet(() -> OrgSetting.builder()
                        .orgId(request.getOrgId())
                        .build());

        settings.setOrderMode(OrderMode.valueOf(request.getOrderMode().toUpperCase(Locale.ROOT)));
        settings.setCustomerPhotoRequired(request.isCustomerPhotoRequired());
        settings.setPaymentTiming(PaymentTiming.valueOf(request.getPaymentTiming().toUpperCase(Locale.ROOT)));
        settings.setCustomerTheme(CustomerTheme.valueOf(request.getCustomerTheme().toUpperCase(Locale.ROOT)));

        try {
            settings = orgSettingRepository.save(settings);
        } catch (DataIntegrityViolationException e) {
            settings = orgSettingRepository.findByOrgId(request.getOrgId())
                    .orElseThrow(SettingErrorCode.SETTINGS_NOT_FOUND::notFound);
            settings.setOrderMode(OrderMode.valueOf(request.getOrderMode().toUpperCase(Locale.ROOT)));
            settings.setCustomerPhotoRequired(request.isCustomerPhotoRequired());
            settings.setPaymentTiming(PaymentTiming.valueOf(request.getPaymentTiming().toUpperCase(Locale.ROOT)));
            settings.setCustomerTheme(CustomerTheme.valueOf(request.getCustomerTheme().toUpperCase(Locale.ROOT)));
            settings = orgSettingRepository.save(settings);
        }

        log.info("Settings updated for org: {}", request.getOrgId());
        return settingMapper.toDto(settings);
    }

}
