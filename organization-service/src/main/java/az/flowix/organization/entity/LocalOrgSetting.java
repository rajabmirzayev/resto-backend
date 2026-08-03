package az.flowix.organization.entity;

import az.flowix.common.jpa.entity.CoreEntity;
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
@Table(name = "org_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalOrgSetting extends CoreEntity {

    @Column(name = "org_id", nullable = false, unique = true)
    UUID orgId;

    @Column(name = "order_mode", nullable = false, length = 30)
    String orderMode;

    @Column(name = "customer_photo_required", nullable = false)
    boolean customerPhotoRequired;

    @Column(name = "payment_timing", nullable = false, length = 10)
    String paymentTiming;

    @Column(name = "customer_theme", nullable = false, length = 20)
    String customerTheme;

}
