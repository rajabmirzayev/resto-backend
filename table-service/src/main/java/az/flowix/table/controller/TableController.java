package az.flowix.table.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.security.model.UserPrincipal;
import az.flowix.table.dto.ReservationRequest;
import az.flowix.table.dto.SectionRequest;
import az.flowix.table.dto.SectionResponse;
import az.flowix.table.dto.SectionUpdateRequest;
import az.flowix.table.dto.StatusUpdateRequest;
import az.flowix.table.dto.TableRequest;
import az.flowix.table.dto.TableResponse;
import az.flowix.table.dto.TableUpdateRequest;
import az.flowix.table.service.TableService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    // ======================== Sections ========================

    @GetMapping("/sections")
    @PreAuthorize("@perm.has('table.view')")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getAllSections(
            @RequestParam UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var sections = tableService.getAllSections(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @PostMapping("/sections")
    @PreAuthorize("@perm.has('table.create')")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var section = tableService.createSection(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(section, "Section created"));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("@perm.has('table.edit')")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody SectionUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var section = tableService.updateSection(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(section, "Section renamed"));
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("@perm.has('table.delete')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tableService.deleteSection(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Section deleted"));
    }

    // ======================== Tables ========================

    @GetMapping("/tables")
    @PreAuthorize("@perm.has('table.view')")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables(
            @RequestParam UUID orgId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        var tables = tableService.getAllTables(orgId, sectionId, status, principal);
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.getTableById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(table));
    }

    @PostMapping("/tables")
    @PreAuthorize("@perm.has('table.create')")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @Valid @RequestBody TableRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.createTable(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(table, "Table created"));
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("@perm.has('table.edit')")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable UUID id,
            @Valid @RequestBody TableUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.updateTable(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(table, "Table updated"));
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("@perm.has('table.delete')")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tableService.deleteTable(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Table deleted"));
    }

    @PutMapping("/tables/{id}/status")
    @PreAuthorize("@perm.has('table.status')")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.updateTableStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(table, "Table status updated"));
    }

    @PutMapping("/tables/{id}/reservation")
    @PreAuthorize("@perm.has('table.reserve')")
    public ResponseEntity<ApiResponse<TableResponse>> updateReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.updateReservation(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(table, "Reservation updated"));
    }

    @DeleteMapping("/tables/{id}/reservation")
    @PreAuthorize("@perm.has('table.reserve')")
    public ResponseEntity<ApiResponse<TableResponse>> deleteReservation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var table = tableService.deleteReservation(id, principal);
        return ResponseEntity.ok(ApiResponse.success(table, "Reservation cancelled"));
    }

}
