package az.codlab.organization.entity;

import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Organization extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "slug", nullable = false)
    String slug;

    @Column(name = "admin_name", nullable = false)
    String adminName;

    @Column(name = "admin_email", nullable = false)
    String adminEmail;

    @Column(name = "logo_url")
    String logoUrl;

    @Column(name = "phone")
    String phone;

    @Column(name = "address")
    String address;

}
