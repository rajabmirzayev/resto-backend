package az.codlab.auth.dto;

import az.codlab.auth.enums.UiScope;
import java.util.List;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    List<String> roles,
    UiScope uiScope
) {}
