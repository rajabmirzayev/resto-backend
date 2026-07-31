package az.codlab.setting.dto;

import az.codlab.common.enums.CustomerTheme;
import az.codlab.common.enums.OrderMode;
import az.codlab.common.enums.PaymentTiming;
import az.codlab.common.exception.handling.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingRequest {

    @NotNull
    UUID orgId;

    @NotBlank
    @ValidEnum(enumClass = OrderMode.class)
    String orderMode;

    boolean customerPhotoRequired;

    @NotBlank
    @ValidEnum(enumClass = PaymentTiming.class)
    String paymentTiming;

    @NotBlank
    @ValidEnum(enumClass = CustomerTheme.class)
    String customerTheme;

}
