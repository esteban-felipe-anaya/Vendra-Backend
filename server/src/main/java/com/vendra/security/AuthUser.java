package com.vendra.security;

import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Convenience accessors for the authenticated principal derived from the Supabase JWT. */
public final class AuthUser {

  private AuthUser() {}

  /** The auth user id (UUID, the JWT {@code sub} claim). */
  public static String id() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      return jwt.getToken().getSubject();
    }
    throw new IllegalStateException("No authenticated user");
  }

  public static String role() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      Jwt token = jwt.getToken();
      Object appMeta = token.getClaim("app_metadata");
      if (appMeta instanceof Map<?, ?> m && m.get("role") instanceof String r) {
        return r;
      }
    }
    return "customer";
  }

  public static String email() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      return jwt.getToken().getClaimAsString("email");
    }
    return null;
  }
}
