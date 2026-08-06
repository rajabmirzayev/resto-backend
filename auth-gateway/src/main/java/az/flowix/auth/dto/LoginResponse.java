package az.flowix.auth.dto;

import az.flowix.common.enums.UiScope;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        User user,
        UiScope uiScope,
        List<String> permissions
) {

    public record User(String username, List<String> roles) {
    }
}
