package az.codlab.setting.service;

import az.codlab.common.enums.CustomerTheme;
import az.codlab.common.enums.OrderMode;
import az.codlab.common.enums.PaymentTiming;
import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.setting.client.OrganizationServiceClient;
import az.codlab.setting.dto.SettingRequest;
import az.codlab.setting.dto.SettingResponse;
import az.codlab.setting.entity.OrgSetting;
import az.codlab.setting.error.SettingErrorCode;
import az.codlab.setting.mapper.SettingMapper;
import az.codlab.setting.repository.OrgSettingRepository;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final OrgSettingRepository orgSettingRepository;
    private final SettingMapper settingMapper;
    private final OrganizationServiceClient organizationServiceClient;

    public SettingService(OrgSettingRepository orgSettingRepository,
                          SettingMapper settingMapper,
                          OrganizationServiceClient organizationServiceClient) {
        this.orgSettingRepository = orgSettingRepository;
        this.settingMapper = settingMapper;
        this.organizationServiceClient = organizationServiceClient;
    }

    public SettingResponse getSettings(UUID orgId) {
        validateOrganization(orgId);
        return orgSettingRepository.findByOrgId(orgId)
                .map(settingMapper::toDto)
                .orElseThrow(SettingErrorCode.SETTINGS_NOT_FOUND::notFound);
    }

    @Transactional
    public SettingResponse updateSettings(SettingRequest request) {
        validateOrganization(request.getOrgId());
        var settings = orgSettingRepository.findByOrgId(request.getOrgId())
                .orElseGet(() -> OrgSetting.builder()
                        .orgId(request.getOrgId())
                        .build());

        settings.setOrderMode(OrderMode.valueOf(request.getOrderMode().toUpperCase()));
        settings.setCustomerPhotoRequired(request.isCustomerPhotoRequired());
        settings.setPaymentTiming(PaymentTiming.valueOf(request.getPaymentTiming().toUpperCase()));
        settings.setCustomerTheme(CustomerTheme.valueOf(request.getCustomerTheme().toUpperCase()));

        settings = orgSettingRepository.save(settings);
        log.info("Settings updated for org: {}", request.getOrgId());
        return settingMapper.toDto(settings);
    }

    private void validateOrganization(UUID orgId) {
        var response = organizationServiceClient.getOrganization(orgId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw SettingErrorCode.ORGANIZATION_NOT_FOUND.notFound();
        }
    }

}
