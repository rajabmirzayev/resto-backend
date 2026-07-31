package az.codlab.organization.service;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.organization.client.RoleServiceClient;
import az.codlab.organization.client.SettingServiceClient;
import az.codlab.organization.client.TableServiceClient;
import az.codlab.organization.client.UserServiceClient;
import az.codlab.organization.client.dto.RoleServiceCreateRoleRequest;
import az.codlab.organization.client.dto.SettingServiceCreateSettingRequest;
import az.codlab.organization.client.dto.TableServiceSectionRequest;
import az.codlab.organization.client.dto.UserServiceCreateUserRequest;
import az.codlab.organization.dto.CreateOrganizationRequest;
import az.codlab.organization.dto.CreateOrganizationResponse;
import az.codlab.organization.dto.OrganizationDto;
import az.codlab.organization.entity.Organization;
import az.codlab.organization.error.OrganizationErrorCode;
import az.codlab.organization.mapper.OrganizationMapper;
import az.codlab.organization.repository.OrganizationRepository;

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
    private final RoleServiceClient roleServiceClient;
    private final UserServiceClient userServiceClient;
    private final SettingServiceClient settingServiceClient;
    private final TableServiceClient tableServiceClient;

    public OrganizationCreationOrchestrator(OrganizationRepository organizationRepository,
                                            OrganizationMapper organizationMapper,
                                            RoleServiceClient roleServiceClient,
                                            UserServiceClient userServiceClient,
                                            SettingServiceClient settingServiceClient,
                                            TableServiceClient tableServiceClient) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.roleServiceClient = roleServiceClient;
        this.userServiceClient = userServiceClient;
        this.settingServiceClient = settingServiceClient;
        this.tableServiceClient = tableServiceClient;
    }

    @Transactional
    public CreateOrganizationResponse createOrganization(CreateOrganizationRequest request) {
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
        var adminUser = createAdminUser(request, organization, adminRole.getId());

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
                .isSystem(Boolean.TRUE.equals(adminRole.getIsSystem()))
                .orgId(organization.getId())
                .build();

        log.info("Organization creation complete: {}", organization.getId());
        return new CreateOrganizationResponse(orgDto, userDto, roleDto);
    }

    private az.codlab.organization.client.dto.RoleServiceRoleResponse createAdminRole(UUID orgId, String orgName) {
        var request = RoleServiceCreateRoleRequest.builder()
                .name(orgName + " Admin")
                .permissions(ORG_ADMIN_PERMISSIONS)
                .orgId(orgId)
                .build();
        return unwrap(roleServiceClient.createRole(request));
    }

    private az.codlab.organization.client.dto.UserServiceUserResponse createAdminUser(
            CreateOrganizationRequest request, Organization organization, UUID roleId) {
        var userRequest = UserServiceCreateUserRequest.builder()
                .name(request.getAdminName().trim())
                .username(request.getAdminEmail().trim().toLowerCase())
                .email(request.getAdminEmail().trim().toLowerCase())
                .password(request.getAdminPassword())
                .roleId(roleId)
                .orgId(organization.getId())
                .build();
        return unwrap(userServiceClient.createUser(userRequest));
    }

    private void createDefaultSettings(UUID orgId) {
        var request = SettingServiceCreateSettingRequest.builder()
                .orgId(orgId)
                .orderMode("CUSTOMER")
                .customerPhotoRequired(false)
                .paymentTiming("AFTER")
                .customerTheme("CLASSIC")
                .build();
        settingServiceClient.createSettings(request);
    }

    private void createDefaultSection(UUID orgId) {
        var request = TableServiceSectionRequest.builder()
                .name("Zal 1")
                .orgId(orgId)
                .build();
        tableServiceClient.createSection(request);
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("Failed to create resource via internal service call");
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
