package az.flowix.organization.client.dto;

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
public class SettingServiceSettingResponse {
    UUID orgId;
    String orderMode;
    Boolean customerPhotoRequired;
    String paymentTiming;
    String customerTheme;
}
