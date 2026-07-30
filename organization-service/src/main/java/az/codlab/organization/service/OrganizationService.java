package az.codlab.organization.service;

import az.codlab.organization.dto.OrganizationDto;
import az.codlab.organization.dto.QrCodeResponse;
import az.codlab.organization.entity.Organization;
import az.codlab.organization.error.OrganizationErrorCode;
import az.codlab.organization.mapper.OrganizationMapper;
import az.codlab.organization.repository.OrganizationRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
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

    // TODO: oz QR generator library-i (QRGen/ZXing) ile evez et, external API-ye bagli qalmasin
    public QrCodeResponse getQrCode(UUID orgId) {
        var org = getOrganizationEntity(orgId);
        var menuUrl = "https://tabler.az/org/" + org.getId() + "/menu";
        var encodedUrl = java.net.URLEncoder.encode(menuUrl, java.nio.charset.StandardCharsets.UTF_8);
        var qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=512x512&data=" + encodedUrl;
        return new QrCodeResponse(qrUrl);
    }

    @Transactional
    public void deleteOrganization(UUID id) {
        var org = getOrganizationEntity(id);
        // TODO: active orders yoxlanisini elave et (order-service hazir olandan sonra)
        org.softDelete(null);
        organizationRepository.save(org);
        log.info("Organization {} soft-deleted", id);
    }

}
