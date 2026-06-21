package com.vendra.auth;

import com.vendra.auth.AuthDtos.*;
import com.vendra.dto.Dtos.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Backend-mediated auth. Public endpoints (no JWT yet) — the clients call these instead of talking
 * to Supabase Auth directly. The returned access token is used as the Bearer on every other call.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Email/password auth proxied to Supabase (no direct client access)")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  @Operation(summary = "Sign in with email + password; returns a session")
  public AuthSession login(@Valid @RequestBody LoginRequest req) {
    return auth.login(req.email(), req.password());
  }

  @PostMapping("/signup")
  @Operation(summary = "Create an account (role clamped to customer|shop|courier)")
  public AuthSession signup(@Valid @RequestBody SignupRequest req) {
    return auth.signup(req.email(), req.password(), req.fullName(), req.role());
  }

  @PostMapping("/refresh")
  @Operation(summary = "Exchange a refresh token for a new session")
  public AuthSession refresh(@Valid @RequestBody RefreshRequest req) {
    return auth.refresh(req.refreshToken());
  }

  @PostMapping("/logout")
  @Operation(summary = "Revoke the session (best-effort)")
  public MessageResponse logout(@Valid @RequestBody LogoutRequest req) {
    auth.logout(req.accessToken());
    return new MessageResponse("Signed out");
  }
}
