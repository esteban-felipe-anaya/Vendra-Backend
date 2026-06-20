package com.vendra.admin;

import com.vendra.dto.Dtos.OrderDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Request/response records for the platform admin API. Reuses {@code Dtos.*} where an existing
 * contract fits (ShopDto, ProductDto, CategoryDto, OrderDto). Money is {@link BigDecimal}; lists are
 * plain arrays so the OpenAPI schema stays flat.
 */
public final class AdminDtos {
  private AdminDtos() {}

  // ---------- dashboard ----------
  public record TopShopDto(String shopId, String name, BigDecimal revenue) {}

  public record DashboardDto(
      BigDecimal gmv,
      long ordersCount,
      long activeCouriers,
      long shopsCount,
      long productsCount,
      TopShopDto[] topShops,
      OrderDto[] recentOrders,
      BigDecimal ledgerBalance) {}

  // ---------- shops ----------
  public record AdminShopDto(
      String id,
      String name,
      String slug,
      String description,
      String logoUrl,
      String bannerUrl,
      String status,
      String ownerId,
      String ownerName,
      BigDecimal ratingAvg,
      int ratingCount) {}

  // ---------- product moderation ----------
  public record ModerateProductRequest(boolean isActive) {}

  // ---------- categories ----------
  public record CategoryRequest(
      String name, String slug, String icon, String imageUrl, Integer sortOrder) {}

  // ---------- couriers ----------
  public record AdminCourierDto(
      String id,
      String profileId,
      String profileName,
      String vehicleType,
      String availability,
      BigDecimal currentLat,
      BigDecimal currentLng,
      boolean payoutsEnabled,
      BigDecimal ratingAvg,
      int ratingCount) {}

  // ---------- users ----------
  public record UserDto(String id, String role, String fullName, String phone, String avatarUrl) {}

  // ---------- promos ----------
  public record PromoRequest(
      String code,
      String description,
      String discountType,
      BigDecimal amount,
      BigDecimal minSubtotal,
      Integer maxRedemptions,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt,
      Boolean isActive) {}

  public record PromoDto(
      String id,
      String code,
      String description,
      String discountType,
      BigDecimal amount,
      BigDecimal minSubtotal,
      Integer maxRedemptions,
      int redeemedCount,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt,
      boolean isActive) {}

  // ---------- settings ----------
  public record SettingsRequest(
      BigDecimal commissionRate,
      BigDecimal baseDeliveryFee,
      BigDecimal courierFeeShare,
      BigDecimal taxRate,
      String currency) {}

  public record SettingsDto(
      BigDecimal commissionRate,
      BigDecimal baseDeliveryFee,
      BigDecimal courierFeeShare,
      BigDecimal taxRate,
      String currency) {}

  // ---------- payouts / ledger ----------
  public record PayoutDto(
      String id,
      String payeeType,
      String shopId,
      String courierId,
      String subOrderId,
      String deliveryId,
      BigDecimal amount,
      String currency,
      String status,
      String stripeTransferId,
      OffsetDateTime createdAt) {}

  public record LedgerDto(
      String id,
      String orderId,
      String subOrderId,
      String entryType,
      BigDecimal amount,
      String currency,
      String memo,
      OffsetDateTime createdAt) {}
}
