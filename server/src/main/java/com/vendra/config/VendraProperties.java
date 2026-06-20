package com.vendra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/** Strongly-typed access to the {@code vendra.*} config tree. */
@Component
@ConfigurationProperties(prefix = "vendra")
@Getter
@Setter
public class VendraProperties {

  private Jwt jwt = new Jwt();
  private Supabase supabase = new Supabase();
  private Stripe stripe = new Stripe();

  @Getter @Setter
  public static class Jwt {
    private String audience = "authenticated";
    private String hs256Secret = "";
  }

  @Getter @Setter
  public static class Supabase {
    private String url;
    private String serviceRoleKey;
    private String storageUrl;
    private Buckets buckets = new Buckets();

    @Getter @Setter
    public static class Buckets {
      private String products = "products";
      private String shops = "shops";
      private String avatars = "avatars";
    }
  }

  @Getter @Setter
  public static class Stripe {
    private String secretKey = "";
    private String webhookSecret = "";
    private String connectRefreshUrl;
    private String connectReturnUrl;
  }
}
