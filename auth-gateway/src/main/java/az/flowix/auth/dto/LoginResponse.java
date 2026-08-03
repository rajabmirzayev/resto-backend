package az.flowix.auth.dto;

import az.flowix.auth.enums.UiScope;
import java.util.List;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    List<String> roles,
    UiScope uiScope
) {}
