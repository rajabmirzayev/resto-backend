package az.flowix.auth.service;

import az.flowix.auth.dto.LoginRequest;
import az.flowix.auth.dto.LoginResponse;
import az.flowix.auth.dto.LogoutRequest;
import az.flowix.auth.dto.RefreshRequest;
import az.flowix.auth.dto.RefreshResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshResponse refresh(RefreshRequest request);

    void logout(LogoutRequest request);

}
