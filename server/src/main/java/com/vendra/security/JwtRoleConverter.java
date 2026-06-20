package com.vendra.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps a Supabase JWT into a Spring {@link AbstractAuthenticationToken}. The role lives in {@code
 * app_metadata.role} (mirrored from {@code profiles.role} by a DB trigger); we expose it as {@code
 * ROLE_<role>} so {@code @PreAuthorize("hasRole('shop')")} works. The principal name is the {@code
 * sub} claim (the auth user id).
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  @SuppressWarnings("unchecked")
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String role = "customer";
    Object appMeta = jwt.getClaim("app_metadata");
    if (appMeta instanceof Map<?, ?> map && map.get("role") instanceof String r && !r.isBlank()) {
      role = r;
    } else if (jwt.getClaim("role") instanceof String topRole
        && List.of("customer", "shop", "courier", "admin").contains(topRole)) {
      // some token shapes expose role at top level
      role = topRole;
    }
    Collection<GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + role));
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }
}
