package az.codlab.organization.client.dto;

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
public class SettingServiceCreateSettingRequest {
    UUID orgId;
    String orderMode;
    boolean customerPhotoRequired;
    String paymentTiming;
    String customerTheme;
}
