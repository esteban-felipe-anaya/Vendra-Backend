package com.vendra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API DTOs (request/response contracts). Grouped as nested records so the OpenAPI schema names stay
 * readable (e.g. Dtos$ProductDto → ProductDto). Money fields are plain JSON numbers (BigDecimal);
 * image/URL fields are plain Strings.
 */
public final class Dtos {
  private Dtos() {}

  // ---------- catalog ----------
  public record CategoryDto(
      String id, String name, String slug, String icon, String imageUrl, int sortOrder) {}

  public record ShopDto(
      String id,
      String name,
      String slug,
      String description,
      String logoUrl,
      String bannerUrl,
      String status,
      BigDecimal ratingAvg,
      int ratingCount) {}

  public record VariantDto(String id, String name, String sku, BigDecimal price, int stock) {}

  public record ProductDto(
      String id,
      String shopId,
      String shopName,
      String categoryId,
      String name,
      String slug,
      BigDecimal price,
      String currency,
      int stock,
      String imageUrl,
      BigDecimal ratingAvg,
      int ratingCount,
      int soldCount) {}

  public record ProductDetailDto(
      ProductDto product,
      String description,
      List<String> images,
      List<VariantDto> variants,
      List<ReviewDto> reviews) {}

  public record ReviewDto(
      String id,
      String productId,
      String userId,
      String userName,
      String userAvatar,
      int rating,
      String title,
      String body,
      OffsetDateTime createdAt) {}

  public record CreateReviewRequest(
      @NotBlank String productId,
      @Min(1) int rating,
      String title,
      String body) {}

  // ---------- cart ----------
  public record AddToCartRequest(
      @NotBlank String productId, String variantId, @Min(1) int quantity) {}

  public record UpdateCartItemRequest(@Min(1) int quantity) {}

  public record CartItemDto(
      String id,
      String productId,
      String variantId,
      String shopId,
      String shopName,
      String name,
      String imageUrl,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal,
      int stockAvailable) {}

  public record CartShopGroupDto(
      String shopId, String shopName, List<CartItemDto> items, BigDecimal subtotal) {}

  public record CartDto(
      String id, List<CartShopGroupDto> shops, BigDecimal itemsSubtotal, int itemCount) {}

  // ---------- checkout ----------
  public record CheckoutRequest(@NotBlank String addressId, String promoCode) {}

  public record CheckoutQuoteDto(
      BigDecimal itemsSubtotal,
      BigDecimal deliveryFee,
      BigDecimal tax,
      BigDecimal discount,
      BigDecimal total,
      BigDecimal platformCommission,
      List<ShopBreakdown> perShop) {}

  public record ShopBreakdown(
      String shopId,
      String shopName,
      BigDecimal subtotal,
      BigDecimal commission,
      BigDecimal payout) {}

  public record CheckoutResponse(
      String orderId, String clientSecret, String paymentIntentId, CheckoutQuoteDto quote) {}

  // ---------- orders ----------
  public record OrderItemDto(
      String id,
      String productId,
      String name,
      String imageUrl,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal) {}

  public record SubOrderDto(
      String id,
      String orderId,
      String shopId,
      String shopName,
      String status,
      BigDecimal itemsSubtotal,
      BigDecimal shopPayout,
      String payoutStatus,
      List<OrderItemDto> items,
      DeliveryDto delivery,
      OffsetDateTime createdAt) {}

  public record OrderDto(
      String id,
      String customerId,
      String status,
      String paymentStatus,
      BigDecimal itemsSubtotal,
      BigDecimal deliveryFee,
      BigDecimal tax,
      BigDecimal discount,
      BigDecimal total,
      Object shipTo,
      List<SubOrderDto> subOrders,
      OffsetDateTime createdAt) {}

  public record TransitionRequest(@NotBlank String to) {}

  // ---------- delivery ----------
  public record DeliveryDto(
      String id,
      String subOrderId,
      String orderId,
      String courierId,
      String courierName,
      String status,
      BigDecimal pickupLat,
      BigDecimal pickupLng,
      String pickupLabel,
      BigDecimal dropoffLat,
      BigDecimal dropoffLng,
      String dropoffLabel,
      BigDecimal courierFee,
      LocationDto lastLocation) {}

  public record LocationDto(BigDecimal lat, BigDecimal lng, BigDecimal heading, OffsetDateTime at) {}

  public record LocationPingRequest(
      @NotNull BigDecimal lat, @NotNull BigDecimal lng, BigDecimal heading) {}

  public record AvailabilityRequest(@NotBlank String availability) {}

  // ---------- account / misc ----------
  public record AddressDto(
      String id,
      String label,
      String recipient,
      String phone,
      String line1,
      String line2,
      String city,
      String region,
      String postalCode,
      String country,
      BigDecimal lat,
      BigDecimal lng,
      boolean isDefault) {}

  public record ProfileDto(
      String id, String role, String fullName, String phone, String avatarUrl) {}

  public record NotificationDto(
      String id,
      String type,
      String title,
      String body,
      String refType,
      String refId,
      boolean isRead,
      OffsetDateTime createdAt) {}

  public record UploadUrlResponse(String uploadUrl, String token, String path, String publicUrl) {}

  public record IdResponse(String id) {}

  public record MessageResponse(String message) {}
}
