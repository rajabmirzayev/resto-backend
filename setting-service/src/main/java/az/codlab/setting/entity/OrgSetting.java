package az.codlab.setting.entity;

import az.codlab.common.enums.CustomerTheme;
import az.codlab.common.enums.OrderMode;
import az.codlab.common.enums.PaymentTiming;
import az.codlab.common.jpa.entity.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "org_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrgSetting extends CoreEntity {

    @Column(name = "org_id", nullable = false, unique = true)
    UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_mode", nullable = false, length = 30)
    OrderMode orderMode;

    @Column(name = "customer_photo_required", nullable = false)
    boolean customerPhotoRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_timing", nullable = false, length = 10)
    PaymentTiming paymentTiming;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_theme", nullable = false, length = 20)
    CustomerTheme customerTheme;

}
