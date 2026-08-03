package az.flowix.organization.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.security.model.UserPrincipal;
import az.flowix.organization.dto.CreateOrganizationRequest;
import az.flowix.organization.dto.CreateOrganizationResponse;
import az.flowix.organization.dto.OrganizationDto;
import az.flowix.organization.dto.QrCodeResponse;
import az.flowix.organization.service.OrganizationCreationOrchestrator;
import az.flowix.organization.service.OrganizationService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationCreationOrchestrator creationOrchestrator;

    public OrganizationController(OrganizationService organizationService,
                                  OrganizationCreationOrchestrator creationOrchestrator) {
        this.organizationService = organizationService;
        this.creationOrchestrator = creationOrchestrator;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getAllOrganizations() {
        var organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CreateOrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        var response = creationOrchestrator.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Organization created"));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(@PathVariable UUID orgId,
                                                                        @AuthenticationPrincipal UserPrincipal principal) {
        var organization = organizationService.getOrganizationById(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success(organization));
    }

    @GetMapping("/{orgId}/qr-code")
    public ResponseEntity<ApiResponse<QrCodeResponse>> getQrCode(@PathVariable UUID orgId,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        var qrCode = organizationService.getQrCode(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success(qrCode));
    }

}
