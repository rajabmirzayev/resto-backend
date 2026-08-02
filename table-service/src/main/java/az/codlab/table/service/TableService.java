package az.codlab.table.service;

import az.codlab.common.enums.TableStatus;
import az.codlab.common.security.model.UserPrincipal;
import az.codlab.common.type.TableReservation;
import az.codlab.table.dto.ReservationRequest;
import az.codlab.table.dto.SectionRequest;
import az.codlab.table.dto.SectionResponse;
import az.codlab.table.dto.SectionUpdateRequest;
import az.codlab.table.dto.StatusUpdateRequest;
import az.codlab.table.dto.TableRequest;
import az.codlab.table.dto.TableResponse;
import az.codlab.table.dto.TableUpdateRequest;
import az.codlab.table.entity.RestaurantTable;
import az.codlab.table.entity.Section;
import az.codlab.table.error.TableErrorCode;
import az.codlab.table.mapper.SectionMapper;
import az.codlab.table.mapper.TableMapper;
import az.codlab.table.repository.RestaurantTableRepository;
import az.codlab.table.repository.SectionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableService {

    private static final Logger log = LoggerFactory.getLogger(TableService.class);

    private static final Set<TableStatus> RESERVABLE_STATUSES =
            Set.of(TableStatus.AVAILABLE, TableStatus.CLEANING, TableStatus.RESERVED);

    /**
     * Legal transitions for the status endpoint (PUT /tables/{id}/status).
     * RESERVED is intentionally managed only through the reservation endpoints.
     */
    private static final java.util.Map<TableStatus, Set<TableStatus>> ALLOWED_TRANSITIONS =
            java.util.Map.of(
                    TableStatus.AVAILABLE, Set.of(TableStatus.AVAILABLE, TableStatus.OCCUPIED, TableStatus.CLEANING),
                    TableStatus.OCCUPIED, Set.of(TableStatus.AVAILABLE, TableStatus.OCCUPIED, TableStatus.CLEANING),
                    TableStatus.RESERVED, Set.of(TableStatus.OCCUPIED),
                    TableStatus.CLEANING, Set.of(TableStatus.AVAILABLE, TableStatus.CLEANING)
            );

    private final SectionRepository sectionRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final SectionMapper sectionMapper;
    private final TableMapper tableMapper;

    public TableService(SectionRepository sectionRepository,
                        RestaurantTableRepository restaurantTableRepository,
                        SectionMapper sectionMapper,
                        TableMapper tableMapper) {
        this.sectionRepository = sectionRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.sectionMapper = sectionMapper;
        this.tableMapper = tableMapper;
    }

    // ======================== Sections ========================

    public List<SectionResponse> getAllSections(UUID orgId, UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        return sectionMapper.toDtoList(
                sectionRepository.findAllByOrgIdAndDeletedFalseOrderByCreatedAtAsc(orgId));
    }

    @Transactional
    public SectionResponse createSection(SectionRequest request, UserPrincipal principal) {
        var orgId = resolveOrgForCreate(principal, request.getOrgId());
        var name = normalizeName(request.getName());
        assertSectionNameFree(orgId, name, null);

        var section = Section.builder()
                .name(name)
                .orgId(orgId)
                .build();
        section = sectionRepository.save(section);
        log.info("Section created: {} ({})", section.getName(), section.getId());
        return sectionMapper.toDto(section);
    }

    @Transactional
    public SectionResponse updateSection(UUID id, SectionUpdateRequest request, UserPrincipal principal) {
        var section = sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.SECTION_NOT_FOUND::notFound);
        assertOrgAccess(section.getOrgId(), principal);

        var name = normalizeName(request.getName());
        if (!name.equals(section.getName())) {
            assertSectionNameFree(section.getOrgId(), name, section.getId());
        }
        section.setName(name);
        section = sectionRepository.save(section);
        log.info("Section renamed: {} ({})", section.getName(), section.getId());
        return sectionMapper.toDto(section);
    }

    @Transactional
    public void deleteSection(UUID id, UserPrincipal principal) {
        var section = sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.SECTION_NOT_FOUND::notFound);
        assertOrgAccess(section.getOrgId(), principal);

        var sectionCount = sectionRepository.countByOrgIdAndDeletedFalse(section.getOrgId());
        if (sectionCount <= 1) {
            throw TableErrorCode.SECTION_IS_LAST.conflict();
        }

        var firstRemaining = sectionRepository
                .findAllByOrgIdAndDeletedFalseOrderByCreatedAtAsc(section.getOrgId())
                .stream()
                .filter(s -> !s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> TableErrorCode.SECTION_IS_LAST.conflict());

        var tables = restaurantTableRepository.findAllBySectionIdAndDeletedFalse(id);
        for (var table : tables) {
            table.setSectionId(firstRemaining.getId());
            restaurantTableRepository.save(table);
        }
        log.info("Moved {} tables to section {}", tables.size(), firstRemaining.getId());

        section.softDelete(userId(principal));
        sectionRepository.save(section);
        log.info("Section soft-deleted: {}", id);
    }

    // ======================== Tables ========================

    public List<TableResponse> getAllTables(UUID orgId, UUID sectionId, String status,
                                            UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        if (orgId == null) {
            return List.of();
        }
        if (sectionId != null && status != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndSectionIdAndStatusAndDeletedFalseOrderByTableNumberAsc(
                            orgId, sectionId, parseStatus(status)));
        }
        if (sectionId != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndSectionIdAndDeletedFalseOrderByTableNumberAsc(
                            orgId, sectionId));
        }
        if (status != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndStatusAndDeletedFalseOrderByTableNumberAsc(
                            orgId, parseStatus(status)));
        }
        return tableMapper.toDtoList(
                restaurantTableRepository.findAllByOrgIdAndDeletedFalseOrderByTableNumberAsc(orgId));
    }

    public TableResponse getTableById(UUID id, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertCanReadOrg(table.getOrgId(), principal);
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse createTable(TableRequest request, UserPrincipal principal) {
        var orgId = resolveOrgForCreate(principal, request.getOrgId());
        assertSectionOwnedByOrg(request.getSectionId(), orgId);
        assertTableNumberFree(orgId, request.getTableNumber(), null);

        var table = RestaurantTable.builder()
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .sectionId(request.getSectionId())
                .orgId(orgId)
                .status(TableStatus.AVAILABLE)
                .build();
        table = restaurantTableRepository.save(table);
        log.info("Table created: #{} ({})", table.getTableNumber(), table.getId());
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse updateTable(UUID id, TableUpdateRequest request, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertOrgAccess(table.getOrgId(), principal);

        if (request.getTableNumber() != null) {
            assertTableNumberFree(table.getOrgId(), request.getTableNumber(), table.getId());
            table.setTableNumber(request.getTableNumber());
        }
        if (request.getCapacity() != null) {
            assertCapacityAllowsReservation(table, request.getCapacity());
            table.setCapacity(request.getCapacity());
        }
        if (request.getSectionId() != null) {
            assertSectionOwnedByOrg(request.getSectionId(), table.getOrgId());
            table.setSectionId(request.getSectionId());
        }
        if (request.getStatus() != null) {
            applyStatusTransition(table, parseStatus(request.getStatus()), null);
        }

        table = restaurantTableRepository.save(table);
        log.info("Table updated: #{} ({})", table.getTableNumber(), table.getId());
        return tableMapper.toDto(table);
    }

    @Transactional
    public void deleteTable(UUID id, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertOrgAccess(table.getOrgId(), principal);

        if (table.getCurrentOrderId() != null) {
            throw TableErrorCode.TABLE_HAS_ACTIVE_ORDER.conflict();
        }
        assertNoUpcomingReservation(table);

        table.softDelete(userId(principal));
        restaurantTableRepository.save(table);
        log.info("Table soft-deleted: {}", id);
    }

    @Transactional
    public TableResponse updateTableStatus(UUID id, StatusUpdateRequest request, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertOrgAccess(table.getOrgId(), principal);

        applyStatusTransition(table, parseStatus(request.getStatus()), request.getCurrentOrderId());

        table = restaurantTableRepository.save(table);
        log.info("Table {} status changed to {}", id, table.getStatus());
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse updateReservation(UUID id, ReservationRequest request, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertOrgAccess(table.getOrgId(), principal);
        assertTableReservable(table);
        assertReservationFitsCapacity(table.getCapacity(), request.getGuestCount());

        var reservation = TableReservation.builder()
                .guestName(normalizeName(request.getGuestName()))
                .phone(normalizeString(request.getPhone()))
                .time(request.getTime())
                .guestCount(request.getGuestCount())
                .notes(normalizeString(request.getNotes()))
                .build();
        table.setReservation(reservation);
        table.setStatus(TableStatus.RESERVED);

        table = restaurantTableRepository.save(table);
        log.info("Table {} reservation updated", id);
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse deleteReservation(UUID id, UserPrincipal principal) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        assertOrgAccess(table.getOrgId(), principal);
        assertTableReservable(table);

        table.setReservation(null);
        if (table.getStatus() == TableStatus.RESERVED) {
            table.setStatus(TableStatus.AVAILABLE);
        }

        table = restaurantTableRepository.save(table);
        log.info("Table {} reservation cancelled", id);
        return tableMapper.toDto(table);
    }

    // ======================== Business rules ========================

    private void assertStatusTransition(RestaurantTable table, TableStatus target) {
        if (target == TableStatus.RESERVED) {
            throw TableErrorCode.INVALID_STATUS_TRANSITION.conflict();
        }
        if (target == table.getStatus()) {
            return;
        }
        Set<TableStatus> allowed = ALLOWED_TRANSITIONS.get(table.getStatus());
        if (allowed == null || !allowed.contains(target)) {
            throw TableErrorCode.INVALID_STATUS_TRANSITION.conflict();
        }
    }

    private void applyStatusTransition(RestaurantTable table, TableStatus target, UUID orderId) {
        assertStatusTransition(table, target);
        var from = table.getStatus();

        if (target == TableStatus.OCCUPIED) {
            if (orderId == null && table.getCurrentOrderId() == null) {
                throw TableErrorCode.ORDER_ID_REQUIRED.badRequest();
            }
            if (orderId != null) {
                table.setCurrentOrderId(orderId);
            }
            if (from == TableStatus.RESERVED) {
                table.setReservation(null);
            }
        } else {
            table.setCurrentOrderId(null);
        }
        table.setStatus(target);
    }

    private void assertTableReservable(RestaurantTable table) {
        if (table.getCurrentOrderId() != null) {
            throw TableErrorCode.TABLE_IS_OCCUPIED.conflict();
        }
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw TableErrorCode.TABLE_IS_OCCUPIED.conflict();
        }
        if (!RESERVABLE_STATUSES.contains(table.getStatus())) {
            throw TableErrorCode.TABLE_IS_OCCUPIED.conflict();
        }
    }

    private void assertReservationFitsCapacity(Integer capacity, Integer guestCount) {
        if (capacity != null && guestCount != null && guestCount > capacity) {
            throw TableErrorCode.RESERVATION_EXCEEDS_CAPACITY.conflict();
        }
    }

    private void assertCapacityAllowsReservation(RestaurantTable table, Integer newCapacity) {
        var reservation = table.getReservation();
        if (reservation != null && reservation.getGuestCount() != null && newCapacity != null
                && reservation.getGuestCount() > newCapacity) {
            throw TableErrorCode.RESERVATION_EXCEEDS_CAPACITY.conflict();
        }
    }

    private void assertNoUpcomingReservation(RestaurantTable table) {
        var reservation = table.getReservation();
        if (reservation != null && reservation.getTime() != null
                && reservation.getTime().isAfter(Instant.now())) {
            throw TableErrorCode.TABLE_HAS_RESERVATION.conflict();
        }
    }

    // ======================== Org tenancy ========================

    private void assertOrgAccess(UUID orgId, UserPrincipal principal) {
        if (principal != null
                && (principal.isPlatformAdmin()
                    || (principal.getOrgId() != null && principal.getOrgId().equals(orgId.toString())))) {
            return;
        }
        throw TableErrorCode.ACCESS_DENIED.forbidden();
    }

    private UUID resolveOrgForCreate(UserPrincipal principal, UUID requestedOrgId) {
        if (principal == null || principal.getUserId() == null) {
            throw TableErrorCode.ACCESS_DENIED.forbidden();
        }
        if (principal.isPlatformAdmin()) {
            if (requestedOrgId == null) {
                throw TableErrorCode.ACCESS_DENIED.forbidden();
            }
            return requestedOrgId;
        }
        if (principal.getOrgId() == null || !principal.getOrgId().equals(requestedOrgId.toString())) {
            throw TableErrorCode.ACCESS_DENIED.forbidden();
        }
        return UUID.fromString(principal.getOrgId());
    }

    private void assertCanReadOrg(UUID orgId, UserPrincipal principal) {
        if (orgId == null || principal == null) {
            return;
        }
        if (principal.getUserId() != null
                && !principal.isPlatformAdmin()
                && (principal.getOrgId() == null || !principal.getOrgId().equals(orgId.toString()))) {
            throw TableErrorCode.ACCESS_DENIED.forbidden();
        }
    }

    private void assertSectionOwnedByOrg(UUID sectionId, UUID orgId) {
        if (sectionId == null) {
            return;
        }
        var section = sectionRepository.findByIdAndDeletedFalse(sectionId)
                .orElseThrow(TableErrorCode.SECTION_NOT_FOUND::notFound);
        if (!section.getOrgId().equals(orgId)) {
            throw TableErrorCode.ACCESS_DENIED.forbidden();
        }
    }

    private void assertSectionNameFree(UUID orgId, String name, UUID excludeId) {
        boolean taken = excludeId == null
                ? sectionRepository.existsByOrgIdAndNameIgnoreCase(orgId, name)
                : sectionRepository.existsByOrgIdAndNameIgnoreCase(orgId, name, excludeId);
        if (taken) {
            throw TableErrorCode.SECTION_NAME_TAKEN.conflict();
        }
    }

    private void assertTableNumberFree(UUID orgId, Integer tableNumber, UUID excludeId) {
        boolean taken = excludeId == null
                ? restaurantTableRepository.existsByOrgIdAndTableNumberAndDeletedFalse(orgId, tableNumber)
                : restaurantTableRepository.existsByOrgIdAndTableNumberAndIdNotAndDeletedFalse(
                        orgId, tableNumber, excludeId);
        if (taken) {
            throw TableErrorCode.TABLE_NUMBER_TAKEN.conflict();
        }
    }

    private TableStatus parseStatus(String status) {
        try {
            return TableStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw TableErrorCode.INVALID_STATUS.badRequest();
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID userId(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        try {
            return UUID.fromString(principal.getUserId());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
