package az.codlab.table.service;

import az.codlab.common.enums.TableStatus;
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

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableService {

    private static final Logger log = LoggerFactory.getLogger(TableService.class);

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

    public List<SectionResponse> getAllSections(UUID orgId) {
        return sectionMapper.toDtoList(
                sectionRepository.findAllByOrgIdAndDeletedFalseOrderByCreatedAtAsc(orgId));
    }

    @Transactional
    public SectionResponse createSection(SectionRequest request) {
        var section = Section.builder()
                .name(request.getName().trim())
                .orgId(request.getOrgId())
                .build();
        section = sectionRepository.save(section);
        log.info("Section created: {} ({})", section.getName(), section.getId());
        return sectionMapper.toDto(section);
    }

    @Transactional
    public SectionResponse updateSection(UUID id, SectionUpdateRequest request) {
        var section = sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.SECTION_NOT_FOUND::notFound);
        section.setName(request.getName().trim());
        section = sectionRepository.save(section);
        log.info("Section renamed: {} ({})", section.getName(), section.getId());
        return sectionMapper.toDto(section);
    }

    @Transactional
    public void deleteSection(UUID id) {
        var section = sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.SECTION_NOT_FOUND::notFound);

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

        section.softDelete(null);
        sectionRepository.save(section);
        log.info("Section soft-deleted: {}", id);
    }

    // ======================== Tables ========================

    public List<TableResponse> getAllTables(UUID orgId, UUID sectionId, String status) {
        if (orgId != null && sectionId != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndSectionIdAndDeletedFalseOrderByTableNumberAsc(
                            orgId, sectionId));
        }
        if (orgId != null && status != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndStatusAndDeletedFalseOrderByTableNumberAsc(
                            orgId, TableStatus.valueOf(status.toUpperCase())));
        }
        if (orgId != null) {
            return tableMapper.toDtoList(
                    restaurantTableRepository.findAllByOrgIdAndDeletedFalseOrderByTableNumberAsc(orgId));
        }
        return List.of();
    }

    public TableResponse getTableById(UUID id) {
        return restaurantTableRepository.findByIdAndDeletedFalse(id)
                .map(tableMapper::toDto)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
    }

    @Transactional
    public TableResponse createTable(TableRequest request) {
        var table = RestaurantTable.builder()
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .sectionId(request.getSectionId())
                .orgId(request.getOrgId())
                .status(TableStatus.AVAILABLE)
                .build();
        table = restaurantTableRepository.save(table);
        log.info("Table created: #{} ({})", table.getTableNumber(), table.getId());
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse updateTable(UUID id, TableUpdateRequest request) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);

        if (request.getTableNumber() != null) {
            table.setTableNumber(request.getTableNumber());
        }
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getSectionId() != null) {
            table.setSectionId(request.getSectionId());
        }
        if (request.getStatus() != null) {
            table.setStatus(parseStatus(request.getStatus()));
        }

        table = restaurantTableRepository.save(table);
        log.info("Table updated: #{} ({})", table.getTableNumber(), table.getId());
        return tableMapper.toDto(table);
    }

    @Transactional
    public void deleteTable(UUID id) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);

        if (table.getCurrentOrderId() != null) {
            throw TableErrorCode.TABLE_HAS_ACTIVE_ORDER.conflict();
        }

        table.softDelete(null);
        restaurantTableRepository.save(table);
        log.info("Table soft-deleted: {}", id);
    }

    @Transactional
    public TableResponse updateTableStatus(UUID id, StatusUpdateRequest request) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        var newStatus = parseStatus(request.getStatus());
        table.setStatus(newStatus);

        if (newStatus == TableStatus.OCCUPIED && request.getCurrentOrderId() != null) {
            table.setCurrentOrderId(request.getCurrentOrderId());
        } else if (newStatus == TableStatus.AVAILABLE || newStatus == TableStatus.CLEANING) {
            table.setCurrentOrderId(null);
        }

        table = restaurantTableRepository.save(table);
        log.info("Table {} status changed to {}", id, request.getStatus());
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse updateReservation(UUID id, ReservationRequest request) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);

        var reservation = TableReservation.builder()
                .guestName(request.getGuestName().trim())
                .phone(request.getPhone().trim())
                .time(request.getTime())
                .guestCount(request.getGuestCount())
                .notes(request.getNotes())
                .build();
        table.setReservation(reservation);
        table.setStatus(TableStatus.RESERVED);

        table = restaurantTableRepository.save(table);
        log.info("Table {} reservation updated", id);
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableResponse deleteReservation(UUID id) {
        var table = restaurantTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(TableErrorCode.TABLE_NOT_FOUND::notFound);
        table.setReservation(null);
        table.setStatus(TableStatus.AVAILABLE);
        table = restaurantTableRepository.save(table);
        log.info("Table {} reservation cancelled", id);
        return tableMapper.toDto(table);
    }

    private TableStatus parseStatus(String status) {
        try {
            return TableStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw TableErrorCode.INVALID_STATUS.badRequest();
        }
    }

}
