package com.vendra.account;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** Request/response records for account/profile surfaces (not part of the shared Dtos contract). */
public final class AccountDtos {
  private AccountDtos() {}

  /** Update the caller's profile. Null fields are ignored. */
  public record UpdateProfileRequest(String fullName, String phone, String avatarUrl) {}

  /** Create/update one of the caller's saved addresses. */
  public record AddressRequest(
      String label,
      @NotBlank String recipient,
      String phone,
      @NotBlank String line1,
      String line2,
      @NotBlank String city,
      String region,
      String postalCode,
      String country,
      BigDecimal lat,
      BigDecimal lng,
      boolean isDefault) {}
}
