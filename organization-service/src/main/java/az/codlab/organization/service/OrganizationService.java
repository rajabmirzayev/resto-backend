package az.codlab.organization.service;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.organization.client.OrderServiceClient;
import az.codlab.organization.dto.CreateOrganizationRequest;
import az.codlab.organization.dto.OrganizationDto;
import az.codlab.organization.dto.QrCodeResponse;
import az.codlab.organization.entity.Organization;
import az.codlab.organization.error.OrganizationErrorCode;
import az.codlab.organization.mapper.OrganizationMapper;
import az.codlab.organization.repository.OrganizationRepository;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMapper organizationMapper,
                               OrderServiceClient orderServiceClient) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.orderServiceClient = orderServiceClient;
    }

    public List<OrganizationDto> getAllOrganizations() {
        return organizationMapper.toDtoList(
                organizationRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
        );
    }

    public OrganizationDto getOrganizationById(UUID id) {
        return organizationRepository.findByIdAndDeletedFalse(id)
                .map(organizationMapper::toDto)
                .orElseThrow(OrganizationErrorCode.ORGANIZATION_NOT_FOUND::notFound);
    }

    public Organization getOrganizationEntity(UUID id) {
        return organizationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(OrganizationErrorCode.ORGANIZATION_NOT_FOUND::notFound);
    }

    public QrCodeResponse getQrCode(UUID orgId) {
        var org = getOrganizationEntity(orgId);
        var menuUrl = "https://tabler.az/org/" + org.getId() + "/menu";
        return new QrCodeResponse(generateQrDataUrl(menuUrl));
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
    public Organization persistOrganization(CreateOrganizationRequest request) {
        var slug = generateSlug(request.getName());
        if (organizationRepository.existsBySlugAndDeletedFalse(slug)) {
            throw OrganizationErrorCode.ORGANIZATION_SLUG_DUPLICATE.conflict();
        }

        var organization = Organization.builder()
                .name(request.getName().trim())
                .slug(slug)
                .adminName(request.getAdminName().trim())
                .adminEmail(request.getAdminEmail().trim().toLowerCase())
                .build();
        try {
            var saved = organizationRepository.saveAndFlush(organization);
            log.info("Organization persisted: {} ({})", saved.getName(), saved.getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw OrganizationErrorCode.ORGANIZATION_SLUG_DUPLICATE.conflict();
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

    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("External service returned unsuccessful response");
        }
        return response.getData();
    }

    private static String generateSlug(String name) {
        var normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD);
        var pattern = Pattern.compile("[^a-zA-Z0-9\\s-]");
        var cleaned = pattern.matcher(normalized).replaceAll("");
        var slug = cleaned.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

}
