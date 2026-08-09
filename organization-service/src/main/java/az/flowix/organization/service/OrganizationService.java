package az.flowix.organization.service;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.security.model.UserPrincipal;
import az.flowix.organization.client.OrderServiceClient;
import az.flowix.organization.dto.OrganizationDto;
import az.flowix.organization.dto.QrCodeResponse;
import az.flowix.organization.entity.Organization;
import az.flowix.organization.error.OrganizationErrorCode;
import az.flowix.organization.mapper.OrganizationMapper;
import az.flowix.organization.repository.LocalOrgSettingRepository;
import az.flowix.organization.repository.LocalSectionRepository;
import az.flowix.organization.repository.OrganizationRepository;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private static final int QR_SIZE = 512;

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final OrderServiceClient orderServiceClient;
    private final LocalSectionRepository localSectionRepository;
    private final LocalOrgSettingRepository localOrgSettingRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMapper organizationMapper,
                               OrderServiceClient orderServiceClient,
                               LocalSectionRepository localSectionRepository,
                               LocalOrgSettingRepository localOrgSettingRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.orderServiceClient = orderServiceClient;
        this.localSectionRepository = localSectionRepository;
        this.localOrgSettingRepository = localOrgSettingRepository;
    }

    public List<OrganizationDto> getAllOrganizations() {
        return organizationMapper.toDtoList(
                organizationRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
        );
    }

    public OrganizationDto getOrganizationById(UUID id, UserPrincipal principal) {
        var org = getOrganizationEntity(id);
        assertOrgAccess(org, principal);
        return organizationMapper.toDto(org);
    }

    public Organization getOrganizationEntity(UUID id) {
        return organizationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(OrganizationErrorCode.ORGANIZATION_NOT_FOUND::notFound);
    }

    public QrCodeResponse getQrCode(UUID orgId, UserPrincipal principal) {
        var org = getOrganizationEntity(orgId);
        assertOrgAccess(org, principal);
        var menuUrl = "https://resto.az/org/" + org.getId() + "/menu";
        return new QrCodeResponse(generateQrDataUrl(menuUrl));
    }

    private void assertOrgAccess(Organization org, UserPrincipal principal) {
        if (principal != null
                && (principal.isPlatformAdmin()
                    || (principal.getOrgId() != null && principal.getOrgId().equals(org.getId().toString())))) {
            return;
        }
        throw OrganizationErrorCode.ORGANIZATION_ACCESS_DENIED.forbidden();
    }

    private String generateQrDataUrl(String content) {
        try {
            var qrCodeWriter = new QRCodeWriter();
            var bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            var image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            var base64 = Base64.getEncoder().encodeToString(output.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Transactional
    public void deleteOrganizationInternal(UUID id) {
        var org = getOrganizationEntity(id);
        org.softDelete(null);
        organizationRepository.save(org);
        log.info("Organization compensated (soft-deleted): {}", id);
    }

    @Transactional
    public void deleteOrganization(UUID id) {
        var org = getOrganizationEntity(id);

        var orders = unwrap(orderServiceClient.getOrders(id));
        boolean hasActiveOrders = orders.stream()
                .anyMatch(o -> !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()));
        if (hasActiveOrders) {
            throw OrganizationErrorCode.ORGANIZATION_HAS_ACTIVE_ORDERS.conflict();
        }

        org.softDelete(null);
        organizationRepository.save(org);
        log.info("Organization {} soft-deleted", id);
    }

    /**
     * Best-effort cleanup of the default section created during org provisioning.
     * Called by the orchestrator during compensation; failures are not rethrown.
     */
    public void cleanupSection(UUID orgId) {
        var sections = localSectionRepository.findAllByOrgIdAndDeletedFalseOrderByNameAsc(orgId);
        if (!sections.isEmpty()) {
            localSectionRepository.deleteAll(sections);
            log.info("Compensated: removed {} section(s) for org {}", sections.size(), orgId);
        }
    }

    /**
     * Best-effort cleanup of the default settings created during org provisioning.
     * Called by the orchestrator during compensation; failures are not rethrown.
     */
    public void cleanupSettings(UUID orgId) {
        localOrgSettingRepository.findByOrgId(orgId).ifPresent(settings -> {
            localOrgSettingRepository.delete(settings);
            log.info("Compensated: removed settings for org {}", orgId);
        });
    }

    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new RuntimeException("No response from downstream service");
        }
        if (!response.isSuccess() || response.getData() == null) {
            throw new RuntimeException(response.getMessage() != null ? response.getMessage() : "External service returned unsuccessful response");
        }
        return response.getData();
    }

}
