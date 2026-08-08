package az.flowix.table.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.table.dto.SectionResponse;
import az.flowix.table.dto.StatusUpdateRequest;
import az.flowix.table.dto.TableResponse;
import az.flowix.table.error.TableErrorCode;
import az.flowix.table.mapper.TableMapper;
import az.flowix.table.repository.RestaurantTableRepository;
import az.flowix.table.service.TableService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal")
public class InternalTableController {

    private final TableService tableService;
    private final RestaurantTableRepository tableRepository;
    private final TableMapper tableMapper;

    public InternalTableController(TableService tableService,
                                   RestaurantTableRepository tableRepository,
                                   TableMapper tableMapper) {
        this.tableService = tableService;
        this.tableRepository = tableRepository;
        this.tableMapper = tableMapper;
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getTables(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(tableService.getAllTables(orgId, null, null, null)));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(@PathVariable UUID id) {
        var table = tableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        return ResponseEntity.ok(ApiResponse.success(tableMapper.toDto(table)));
    }

    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSections(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(tableService.getAllSections(orgId, null)));
    }

    @PutMapping("/tables/{id}/status")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tableService.updateTableStatus(id, request, null), "Table status updated"));
    }

}
