package az.flowix.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSettingResponse {
    private UUID orgId;
    private String orderMode;
    private boolean customerPhotoRequired;
    private String paymentTiming;
    private String customerTheme;
}
