package az.flowix.organization.service;

import az.flowix.common.enums.CustomerTheme;
import az.flowix.common.enums.OrderMode;
import az.flowix.common.enums.PaymentTiming;
import az.flowix.common.exception.handling.decoder.FeignClientException;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.RoleServiceClient;
import az.flowix.organization.client.SettingServiceClient;
import az.flowix.organization.client.TableServiceClient;
import az.flowix.organization.client.UserServiceClient;
import az.flowix.organization.client.dto.RoleServiceRoleResponse;
import az.flowix.organization.client.dto.SettingServiceCreateSettingRequest;
import az.flowix.organization.client.dto.TableServiceSectionRequest;
import az.flowix.organization.client.dto.UserServiceCreateUserRequest;
import az.flowix.organization.client.dto.UserServiceUserResponse;
import az.flowix.organization.dto.CreateOrganizationRequest;
import az.flowix.organization.dto.CreateOrganizationResponse;
import az.flowix.organization.entity.Organization;
import az.flowix.organization.error.OrganizationErrorCode;
import az.flowix.organization.error.OrganizationException;
import az.flowix.organization.mapper.OrganizationMapper;
import az.flowix.organization.repository.OrganizationRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class OrganizationCreationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrganizationCreationOrchestrator.class);
    private static final String ORG_ADMIN_ROLE_CODE = "ORG_ADMIN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final RoleServiceClient roleServiceClient;
    private final UserServiceClient userServiceClient;
    private final SettingServiceClient settingServiceClient;
    private final TableServiceClient tableServiceClient;
    private final OrganizationService organizationService;
    private final Executor provisioningExecutor;

    public OrganizationCreationOrchestrator(OrganizationRepository organizationRepository,
                                            OrganizationMapper organizationMapper,
                                            RoleServiceClient roleServiceClient,
                                            UserServiceClient userServiceClient,
                                            SettingServiceClient settingServiceClient,
                                            TableServiceClient tableServiceClient,
                                            OrganizationService organizationService,
                                            @Qualifier("organizationProvisioningExecutor")
                                            Executor provisioningExecutor) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.roleServiceClient = roleServiceClient;
        this.userServiceClient = userServiceClient;
        this.settingServiceClient = settingServiceClient;
        this.tableServiceClient = tableServiceClient;
        this.organizationService = organizationService;
        this.provisioningExecutor = provisioningExecutor;
    }

    public CreateOrganizationResponse createOrganization(CreateOrganizationRequest request) {
        String slug = generateSlug(request.getName());
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
            organizationRepository.save(organization);
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate slug on save: {}", slug, e);
            throw OrganizationErrorCode.ORGANIZATION_SLUG_DUPLICATE.conflict();
        }
        UUID orgId = organization.getId();
        log.info("Organization persisted: {} ({})", organization.getName(), orgId);

        UUID userId = null;
        try {
            var adminRole = getAdminRole();
            var adminUser = createAdminUser(request, orgId, adminRole.getId());
            userId = adminUser.getId();

            createDefaultResources(orgId);

            log.info("Organization creation complete: {}", orgId);
            return buildResponse(organization, adminRole, adminUser);
        } catch (OrganizationException e) {
            compensate(userId, orgId);
            throw e;
        } catch (FeignClientException e) {
            log.error("Downstream service error for org {}: status={}, key={}, title={}, detail={}",
                    orgId, e.getStatus(), e.getSourceKey(), e.getSourceTitle(), e.getDetail());
            compensate(userId, orgId);
            throw e;
        } catch (Exception e) {
            log.error("Organization creation failed for {}, compensating resources", orgId, e);
            compensate(userId, orgId);
            throw OrganizationErrorCode.ORGANIZATION_CREATION_FAILED.internal();
        }
    }

    private RoleServiceRoleResponse getAdminRole() {
        return unwrap(roleServiceClient.getSystemRole(ORG_ADMIN_ROLE_CODE));
    }

    private UserServiceUserResponse createAdminUser(CreateOrganizationRequest request,
                                                    UUID orgId, UUID roleId) {
        var userRequest = UserServiceCreateUserRequest.builder()
                .name(request.getAdminName().trim())
                .username(request.getAdminEmail().trim().toLowerCase())
                .email(request.getAdminEmail().trim().toLowerCase())
                .password(request.getAdminPassword())
                .roleId(roleId)
                .orgId(orgId)
                .build();
        return unwrap(userServiceClient.createUser(userRequest));
    }

    private void createDefaultResources(UUID orgId) {
        var settingsFuture = CompletableFuture.runAsync(() -> createDefaultSettings(orgId), provisioningExecutor);
        var sectionFuture = CompletableFuture.runAsync(() -> createDefaultSection(orgId), provisioningExecutor);
        CompletableFuture.allOf(settingsFuture, sectionFuture).join();
    }

    private void createDefaultSettings(UUID orgId) {
        var request = SettingServiceCreateSettingRequest.builder()
                .orgId(orgId)
                .orderMode(OrderMode.CUSTOMER.name())
                .customerPhotoRequired(false)
                .paymentTiming(PaymentTiming.AFTER.name())
                .customerTheme(CustomerTheme.CLASSIC.name())
                .build();
        unwrap(settingServiceClient.createSettings(request));
    }

    private void createDefaultSection(UUID orgId) {
        var request = TableServiceSectionRequest.builder()
                .name("Zal 1")
                .orgId(orgId)
                .build();
        tableServiceClient.createSection(request);
    }

    private CreateOrganizationResponse buildResponse(Organization organization,
                                                     RoleServiceRoleResponse adminRole,
                                                     UserServiceUserResponse adminUser) {
        var orgDto = organizationMapper.toDto(organization);
        var userDto = CreateOrganizationResponse.UserDto.builder()
                .id(adminUser.getId())
                .name(adminUser.getName())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .role(adminUser.getRole() != null ? adminUser.getRole().getCode() : null)
                .roleId(adminRole.getId())
                .orgId(organization.getId())
                .build();
        List<String> permissions = adminRole.getPermissions() != null
                ? adminRole.getPermissions().stream().map(p -> p.getCode()).toList()
                : List.of();
        var roleDto = CreateOrganizationResponse.RoleDto.builder()
                .id(adminRole.getId())
                .name(adminRole.getName())
                .permissions(permissions)
                .isSystem(Boolean.TRUE.equals(adminRole.getIsSystem()))
                .orgId(organization.getId())
                .build();
        return new CreateOrganizationResponse(orgDto, userDto, roleDto);
    }

    private void compensate(UUID userId, UUID orgId) {
        if (userId != null) {
            try {
                userServiceClient.deleteUser(userId);
                log.info("Compensated: deleted user {}", userId);
            } catch (Exception ex) {
                log.warn("Compensation failed: delete user {}", userId, ex);
            }
        }
        try {
            organizationService.deleteOrganizationInternal(orgId);
            log.info("Compensated: deleted org {}", orgId);
        } catch (Exception ex) {
            log.warn("Compensation failed: delete org {}", orgId, ex);
        }
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new RuntimeException("No response from downstream service");
        }
        if (!response.isSuccess() || response.getData() == null) {
            throw new RuntimeException(response.getMessage() != null ? response.getMessage() : "Downstream service call failed");
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
