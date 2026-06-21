package com.vendra.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response contracts for the backend-mediated auth API. */
public final class AuthDtos {
  private AuthDtos() {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record SignupRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password,
      @NotBlank String fullName,
      String role // optional; clamped to customer|shop|courier (never admin)
      ) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record LogoutRequest(@NotBlank String accessToken) {}

  public record AuthUser(String id, String email, String role, String fullName) {}

  /** Mirrors a Supabase session; clients store the tokens and send the access token as Bearer. */
  public record AuthSession(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      long expiresAt,
      AuthUser user) {}
}
