package az.flowix.auth.dto;

public record RefreshResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
