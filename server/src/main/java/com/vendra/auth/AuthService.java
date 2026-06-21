package com.vendra.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendra.auth.AuthDtos.AuthSession;
import com.vendra.auth.AuthDtos.AuthUser;
import com.vendra.common.ApiException;
import com.vendra.config.VendraProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Backend-mediated authentication. Clients never call Supabase Auth (GoTrue) directly — they hit
 * this API, and the backend exchanges credentials with GoTrue using the project's anon key. The
 * returned access token is a normal Supabase JWT, so the resource server still verifies it via JWKS
 * and Realtime/RLS keep working when a client sets that token on its Supabase client.
 */
@Service
public class AuthService {

  private static final List<String> SELF_SERVICE_ROLES = List.of("customer", "shop", "courier");

  private final RestClient gotrue;
  private final ObjectMapper mapper = new ObjectMapper();

  public AuthService(VendraProperties props) {
    String url = props.getSupabase().getUrl();
    String anonKey = props.getSupabase().getAnonKey();
    if (url == null || url.isBlank() || anonKey == null || anonKey.isBlank()) {
      // Built lazily-tolerant: calls will fail clearly if auth isn't configured.
      this.gotrue = RestClient.builder().baseUrl("http://localhost").build();
    } else {
      this.gotrue =
          RestClient.builder()
              .baseUrl(url.replaceAll("/+$", "") + "/auth/v1")
              .defaultHeader("apikey", anonKey)
              .defaultHeader("Authorization", "Bearer " + anonKey)
              .build();
    }
  }

  public AuthSession login(String email, String password) {
    JsonNode body =
        post(
            "/token?grant_type=password",
            Map.of("email", email, "password", password),
            "Invalid email or password");
    return toSession(body);
  }

  public AuthSession signup(String email, String password, String fullName, String role) {
    String safeRole = SELF_SERVICE_ROLES.contains(role) ? role : "customer";
    JsonNode body =
        post(
            "/signup",
            Map.of(
                "email", email,
                "password", password,
                "data", Map.of("full_name", fullName, "role", safeRole)),
            "Sign up failed");
    if (body.hasNonNull("access_token")) {
      return toSession(body);
    }
    // Email confirmation is required on this project — no session yet.
    throw new ApiException(
        HttpStatus.ACCEPTED, "Account created. Please confirm your email, then sign in.");
  }

  public AuthSession refresh(String refreshToken) {
    JsonNode body =
        post(
            "/token?grant_type=refresh_token",
            Map.of("refresh_token", refreshToken),
            "Could not refresh session");
    return toSession(body);
  }

  public void logout(String accessToken) {
    try {
      gotrue
          .post()
          .uri("/logout")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of())
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ignored) {
      // logout is best-effort; the client discards its tokens regardless
    }
  }

  // ---- helpers ----
  private JsonNode post(String uri, Object payload, String fallbackError) {
    try {
      String json =
          gotrue
              .post()
              .uri(uri)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(String.class);
      return mapper.readTree(json == null ? "{}" : json);
    } catch (org.springframework.web.client.RestClientResponseException e) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, extractError(e.getResponseBodyAsString(), fallbackError));
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Auth provider unavailable: " + e.getMessage());
    }
  }

  private String extractError(String body, String fallback) {
    try {
      JsonNode n = mapper.readTree(body);
      for (String f : List.of("error_description", "msg", "message", "error")) {
        if (n.hasNonNull(f)) return n.get(f).asText();
      }
    } catch (Exception ignored) {
      // fall through
    }
    return fallback;
  }

  private AuthSession toSession(JsonNode b) {
    JsonNode u = b.get("user");
    String role = "customer";
    String fullName = null;
    String id = null, email = null;
    if (u != null) {
      id = text(u, "id");
      email = text(u, "email");
      JsonNode appMeta = u.get("app_metadata");
      if (appMeta != null && appMeta.hasNonNull("role")) role = appMeta.get("role").asText();
      JsonNode userMeta = u.get("user_metadata");
      if (userMeta != null) {
        if (userMeta.hasNonNull("role")) role = userMeta.get("role").asText();
        if (userMeta.hasNonNull("full_name")) fullName = userMeta.get("full_name").asText();
      }
    }
    return new AuthSession(
        text(b, "access_token"),
        text(b, "refresh_token"),
        b.hasNonNull("token_type") ? b.get("token_type").asText() : "bearer",
        b.has("expires_in") ? b.get("expires_in").asLong() : 3600,
        b.has("expires_at") ? b.get("expires_at").asLong() : 0,
        new AuthUser(id, email, role, fullName));
  }

  private static String text(JsonNode n, String field) {
    return n.hasNonNull(field) ? n.get(field).asText() : null;
  }
}
