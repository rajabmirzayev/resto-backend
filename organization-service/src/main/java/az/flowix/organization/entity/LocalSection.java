package az.flowix.organization.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalSection extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
