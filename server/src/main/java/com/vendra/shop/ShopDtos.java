package com.vendra.shop;

import com.vendra.domain.Product;
import com.vendra.dto.Dtos.ProductDto;
import com.vendra.dto.Dtos.SubOrderDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Merchant-facing request/response records (kept out of the shared Dtos contract). */
public final class ShopDtos {
  private ShopDtos() {}

  /** Update my shop profile. Null fields are ignored. */
  public record UpdateShopRequest(
      String name, String description, String logoUrl, String bannerUrl) {}

  /** Create/update a product in my shop. */
  public record ProductRequest(
      @NotBlank String name,
      String categoryId,
      String description,
      @NotNull BigDecimal price,
      @Min(0) int stock,
      String imageUrl,
      List<String> images,
      String currency) {}

  /** Append an image URL to a product. */
  public record ProductImageRequest(@NotBlank String url, int sortOrder) {}

  /** Merchant dashboard snapshot. */
  public record ShopDashboardDto(
      long ordersTotal,
      long ordersToday,
      BigDecimal revenue,
      long pending,
      long productCount,
      BigDecimal ratingAvg,
      List<SubOrderDto> recentOrders) {}

  /** Stripe onboarding redirect URL. */
  public record OnboardingLinkResponse(String url) {}

  /** Stripe payout readiness. */
  public record OnboardingStatusResponse(boolean payoutsEnabled) {}

  /** Shared product -> DTO mapper (CatalogService's is package-private to catalog). */
  public static ProductDto toProductDto(Product p, String shopName) {
    return new ProductDto(
        p.getId(),
        p.getShopId(),
        shopName,
        p.getCategoryId(),
        p.getName(),
        p.getSlug(),
        p.getPrice(),
        p.getCurrency(),
        p.getStock(),
        p.getImageUrl(),
        p.getRatingAvg(),
        p.getRatingCount(),
        p.getSoldCount());
  }
}
