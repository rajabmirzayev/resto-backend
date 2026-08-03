package az.flowix.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import java.util.UUID;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SoftDeletableEntity extends BaseAuditableEntity {

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    boolean deleted = false;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @Column(name = "deleted_by")
    UUID deletedBy;

    public void softDelete(UUID deletedByUserId) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

}
