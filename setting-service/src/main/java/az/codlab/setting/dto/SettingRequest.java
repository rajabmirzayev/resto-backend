package az.codlab.setting.dto;

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
    String orderMode;

    boolean customerPhotoRequired;

    @NotBlank
    String paymentTiming;

    @NotBlank
    String customerTheme;

}
