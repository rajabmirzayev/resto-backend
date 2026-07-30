package az.codlab.organization.service;

import az.codlab.organization.dto.CreateOrganizationRequest;
import az.codlab.organization.dto.CreateOrganizationResponse;
import az.codlab.organization.dto.OrganizationDto;
import az.codlab.organization.entity.LocalOrgSetting;
import az.codlab.organization.entity.LocalRole;
import az.codlab.organization.entity.LocalSection;
import az.codlab.organization.entity.LocalUser;
import az.codlab.organization.entity.Organization;
import az.codlab.organization.error.OrganizationErrorCode;
import az.codlab.organization.mapper.OrganizationMapper;
import az.codlab.organization.repository.LocalOrgSettingRepository;
import az.codlab.organization.repository.LocalRoleRepository;
import az.codlab.organization.repository.LocalSectionRepository;
import az.codlab.organization.repository.LocalUserRepository;
import az.codlab.organization.repository.OrganizationRepository;

// TODO: user-service, role-service, setting-service, table-service yaradilanda
//  LocalUser/LocalRole/LocalOrgSetting/LocalSection entity-lerini sil,
//  evezine RestClient/Feign ile hemin servislere HTTP call et.
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationCreationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrganizationCreationOrchestrator.class);

    private static final List<String> ORG_ADMIN_PERMISSIONS = List.of(
            "dashboard.view",
            "menu.view", "menu.create", "menu.edit", "menu.delete",
            "tables.view", "tables.manage", "tables.status",
            "orders.view", "orders.manage", "orders.cancel",
            "kitchen.view", "kitchen.manage"
    );

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final LocalUserRepository localUserRepository;
    private final LocalRoleRepository localRoleRepository;
    private final LocalOrgSettingRepository localOrgSettingRepository;
    private final LocalSectionRepository localSectionRepository;

    public OrganizationCreationOrchestrator(OrganizationRepository organizationRepository,
                                            OrganizationMapper organizationMapper,
                                            LocalUserRepository localUserRepository,
                                            LocalRoleRepository localRoleRepository,
                                            LocalOrgSettingRepository localOrgSettingRepository,
                                            LocalSectionRepository localSectionRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.localUserRepository = localUserRepository;
        this.localRoleRepository = localRoleRepository;
        this.localOrgSettingRepository = localOrgSettingRepository;
        this.localSectionRepository = localSectionRepository;
    }

    @Transactional
    public CreateOrganizationResponse createOrganization(CreateOrganizationRequest request) {
        // TODO: password-i hashle (BCrypt etc) user-ms yaradilana qeder bele qala biler
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
        organization = organizationRepository.save(organization);
        log.info("Organization created: {} ({})", organization.getName(), organization.getId());

        var adminRole = createAdminRole(organization.getId(), request.getName().trim());
        var adminUser = createAdminUser(request, organization, adminRole);

        createDefaultSettings(organization.getId());
        createDefaultSection(organization.getId());

        var orgDto = organizationMapper.toDto(organization);
        var userDto = CreateOrganizationResponse.UserDto.builder()
                .id(adminUser.getId())
                .name(adminUser.getName())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .role(adminUser.getRole())
                .roleId(adminRole.getId())
                .orgId(organization.getId())
                .build();
        var roleDto = CreateOrganizationResponse.RoleDto.builder()
                .id(adminRole.getId())
                .name(adminRole.getName())
                .permissions(adminRole.getPermissions())
                .isSystem(false)
                .orgId(organization.getId())
                .build();

        log.info("Organization creation complete: {}", organization.getId());
        return new CreateOrganizationResponse(orgDto, userDto, roleDto);
    }

    // TODO: role-service yaradilanda bu metodu HTTP call-e cevir
    private LocalRole createAdminRole(UUID orgId, String orgName) {
        var role = LocalRole.builder()
                .name(orgName + " Admin")
                .permissions(ORG_ADMIN_PERMISSIONS)
                .isSystem(false)
                .orgId(orgId)
                .build();
        return localRoleRepository.save(role);
    }

    // TODO: user-service yaradilanda bu metodu HTTP call-e cevir
    private LocalUser createAdminUser(CreateOrganizationRequest request,
                                      Organization organization,
                                      LocalRole role) {
        var user = LocalUser.builder()
                .name(request.getAdminName().trim())
                .username(request.getAdminEmail().trim().toLowerCase())
                .email(request.getAdminEmail().trim().toLowerCase())
                .password(request.getAdminPassword())
                .role("ORG_ADMIN")
                .roleId(role.getId())
                .orgId(organization.getId())
                .isActive(true)
                .build();
        return localUserRepository.save(user);
    }

    // TODO: setting-service yaradilanda bu metodu HTTP call-e cevir
    private void createDefaultSettings(UUID orgId) {
        var settings = LocalOrgSetting.builder()
                .orgId(orgId)
                .orderMode("CUSTOMER")
                .customerPhotoRequired(false)
                .paymentTiming("AFTER")
                .customerTheme("CLASSIC")
                .build();
        localOrgSettingRepository.save(settings);
    }

    // TODO: table-service yaradilanda bu metodu HTTP call-e cevir
    private void createDefaultSection(UUID orgId) {
        var section = LocalSection.builder()
                .name("Zal 1")
                .orgId(orgId)
                .build();
        localSectionRepository.save(section);
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
