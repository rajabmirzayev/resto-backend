package az.flowix.organization.service;

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
import az.flowix.organization.mapper.OrganizationMapper;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orchestrates organization provisioning across the bounded-context services.
 *
 * <p>The local organization row is persisted and committed first, so downstream
 * services can safely read it (fixes the stale-transaction 404). All remote calls
 * then run outside the DB transaction, and independent default-resource calls are
 * fanned out on a dedicated executor. On failure the already-created resources are
 * compensated (user, organization).
 */
@Service
public class OrganizationCreationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrganizationCreationOrchestrator.class);

    private static final String ORG_ADMIN_ROLE_CODE = "ORG_ADMIN";

    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;
    private final RoleServiceClient roleServiceClient;
    private final UserServiceClient userServiceClient;
    private final SettingServiceClient settingServiceClient;
    private final TableServiceClient tableServiceClient;
    private final Executor provisioningExecutor;

    public OrganizationCreationOrchestrator(OrganizationService organizationService,
                                            OrganizationMapper organizationMapper,
                                            RoleServiceClient roleServiceClient,
                                            UserServiceClient userServiceClient,
                                            SettingServiceClient settingServiceClient,
                                            TableServiceClient tableServiceClient,
                                            @Qualifier("organizationProvisioningExecutor")
                                            Executor provisioningExecutor) {
        this.organizationService = organizationService;
        this.organizationMapper = organizationMapper;
        this.roleServiceClient = roleServiceClient;
        this.userServiceClient = userServiceClient;
        this.settingServiceClient = settingServiceClient;
        this.tableServiceClient = tableServiceClient;
        this.provisioningExecutor = provisioningExecutor;
    }

    public CreateOrganizationResponse createOrganization(CreateOrganizationRequest request) {
        var organization = organizationService.persistOrganization(request);

        UUID userId = null;
        try {
            var adminRole = getAdminRole();
            var adminUser = createAdminUser(request, organization, adminRole.getId());
            userId = adminUser.getId();

            createDefaultResources(organization.getId());

            log.info("Organization creation complete: {}", organization.getId());
            return buildResponse(organization, adminRole, adminUser);
        } catch (Exception e) {
            log.error("Organization creation failed for {}, compensating resources", organization.getId(), e);
            compensate(userId, organization.getId());
            throw OrganizationErrorCode.ORGANIZATION_CREATION_FAILED.internal();
        }
    }

    private RoleServiceRoleResponse getAdminRole() {
        return unwrap(roleServiceClient.getSystemRole(ORG_ADMIN_ROLE_CODE));
    }

    private UserServiceUserResponse createAdminUser(CreateOrganizationRequest request,
                                                    Organization organization, UUID roleId) {
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

    private void createDefaultResources(UUID orgId) {
        var settingsFuture = CompletableFuture.runAsync(() -> createDefaultSettings(orgId), provisioningExecutor);
        var sectionFuture = CompletableFuture.runAsync(() -> createDefaultSection(orgId), provisioningExecutor);
        CompletableFuture.allOf(settingsFuture, sectionFuture).join();
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

    private CreateOrganizationResponse buildResponse(Organization organization,
                                                     RoleServiceRoleResponse adminRole,
                                                     UserServiceUserResponse adminUser) {
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
        return new CreateOrganizationResponse(orgDto, userDto, roleDto);
    }

    private void compensate(UUID userId, UUID orgId) {
        if (userId != null) {
            try {
                userServiceClient.deleteUser(userId);
            } catch (Exception ex) {
                log.warn("Failed to clean up user: {}", userId, ex);
            }
        }
        if (orgId != null) {
            try {
                organizationService.deleteOrganizationInternal(orgId);
            } catch (Exception ex) {
                log.warn("Failed to clean up organization: {}", orgId, ex);
            }
        }
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("Failed to create resource via internal service call");
        }
        return response.getData();
    }

}
