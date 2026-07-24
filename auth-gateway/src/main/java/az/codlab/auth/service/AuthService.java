package az.codlab.auth.service;

import az.codlab.auth.dto.LoginRequest;
import az.codlab.auth.dto.LoginResponse;
import az.codlab.auth.dto.LogoutRequest;
import az.codlab.auth.dto.RefreshRequest;
import az.codlab.auth.dto.RefreshResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshResponse refresh(RefreshRequest request);

    void logout(LogoutRequest request);

}
