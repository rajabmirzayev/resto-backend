package az.codlab.table.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.table.dto.ReservationRequest;
import az.codlab.table.dto.SectionRequest;
import az.codlab.table.dto.SectionResponse;
import az.codlab.table.dto.SectionUpdateRequest;
import az.codlab.table.dto.StatusUpdateRequest;
import az.codlab.table.dto.TableRequest;
import az.codlab.table.dto.TableResponse;
import az.codlab.table.dto.TableUpdateRequest;
import az.codlab.table.service.TableService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getAllSections(
            @RequestParam UUID orgId) {
        var sections = tableService.getAllSections(orgId);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @PostMapping("/sections")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @Valid @RequestBody SectionRequest request) {
        var section = tableService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(section, "Section created"));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody SectionUpdateRequest request) {
        var section = tableService.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.success(section, "Section renamed"));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        tableService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Section deleted"));
    }

    // ======================== Tables ========================

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String status) {
        var tables = tableService.getAllTables(orgId, sectionId, status);
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(@PathVariable UUID id) {
        var table = tableService.getTableById(id);
        return ResponseEntity.ok(ApiResponse.success(table));
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @Valid @RequestBody TableRequest request) {
        var table = tableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(table, "Table created"));
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable UUID id,
            @Valid @RequestBody TableUpdateRequest request) {
        var table = tableService.updateTable(id, request);
        return ResponseEntity.ok(ApiResponse.success(table, "Table updated"));
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable UUID id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Table deleted"));
    }

    @PutMapping("/tables/{id}/status")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        var table = tableService.updateTableStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(table, "Table status updated"));
    }

    @PutMapping("/tables/{id}/reservation")
    public ResponseEntity<ApiResponse<TableResponse>> updateReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request) {
        var table = tableService.updateReservation(id, request);
        return ResponseEntity.ok(ApiResponse.success(table, "Reservation updated"));
    }

    @DeleteMapping("/tables/{id}/reservation")
    public ResponseEntity<ApiResponse<TableResponse>> deleteReservation(
            @PathVariable UUID id) {
        var table = tableService.deleteReservation(id);
        return ResponseEntity.ok(ApiResponse.success(table, "Reservation cancelled"));
    }

}
