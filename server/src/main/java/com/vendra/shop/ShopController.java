package com.vendra.shop;

import com.vendra.account.AccountService;
import com.vendra.domain.OrderItem;
import com.vendra.domain.Shop;
import com.vendra.domain.SubOrder;
import com.vendra.dto.Dtos.DeliveryDto;
import com.vendra.dto.Dtos.OrderItemDto;
import com.vendra.dto.Dtos.ShopDto;
import com.vendra.dto.Dtos.SubOrderDto;
import com.vendra.repo.OrderItemRepository;
import com.vendra.repo.ProductRepository;
import com.vendra.repo.ShopRepository;
import com.vendra.repo.SubOrderRepository;
import com.vendra.shop.ShopDtos.ShopDashboardDto;
import com.vendra.shop.ShopDtos.UpdateShopRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Merchant shop profile + dashboard. */
@RestController
@RequestMapping("/api/v1/shop")
@PreAuthorize("hasRole('shop')")
@Tag(name = "Shop", description = "Merchant shop profile and dashboard")
public class ShopController {

  private static final Set<String> PENDING_STATES = Set.of("placed", "accepted", "preparing");

  private final AccountService account;
  private final ShopRepository shops;
  private final SubOrderRepository subOrders;
  private final OrderItemRepository orderItems;
  private final ProductRepository products;

  public ShopController(
      AccountService account,
      ShopRepository shops,
      SubOrderRepository subOrders,
      OrderItemRepository orderItems,
      ProductRepository products) {
    this.account = account;
    this.shops = shops;
    this.subOrders = subOrders;
    this.orderItems = orderItems;
    this.products = products;
  }

  @GetMapping
  @Operation(summary = "Get my shop profile")
  public ShopDto myShop() {
    return toShopDto(account.currentShop());
  }

  @PatchMapping
  @Operation(summary = "Update my shop (name, description, logoUrl, bannerUrl)")
  @Transactional
  public ShopDto update(@Valid @RequestBody UpdateShopRequest req) {
    Shop shop = account.currentShop();
    if (req.name() != null && !req.name().isBlank()) {
      shop.setName(req.name());
      shop.setSlug(Slugs.slugify(req.name()));
    }
    if (req.description() != null) {
      shop.setDescription(req.description());
    }
    if (req.logoUrl() != null) {
      shop.setLogoUrl(req.logoUrl());
    }
    if (req.bannerUrl() != null) {
      shop.setBannerUrl(req.bannerUrl());
    }
    return toShopDto(shops.save(shop));
  }

  @GetMapping("/dashboard")
  @Operation(summary = "Merchant dashboard metrics")
  public ShopDashboardDto dashboard() {
    Shop shop = account.currentShop();
    List<SubOrder> all = subOrders.findByShopIdOrderByCreatedAtDesc(shop.getId());

    OffsetDateTime startOfToday =
        OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);

    long ordersTotal = all.size();
    long ordersToday =
        all.stream()
            .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(startOfToday))
            .count();
    BigDecimal revenue =
        all.stream()
            .filter(s -> "delivered".equalsIgnoreCase(s.getStatus()))
            .map(SubOrder::getShopPayout)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long pending =
        all.stream()
            .filter(s -> PENDING_STATES.contains(s.getStatus() == null ? "" : s.getStatus().toLowerCase()))
            .count();
    long productCount = products.findByShopId(shop.getId()).size();

    List<SubOrderDto> recent =
        all.stream().limit(10).map(s -> toSubOrderDto(s, shop.getName())).toList();

    return new ShopDashboardDto(
        ordersTotal, ordersToday, revenue, pending, productCount, shop.getRatingAvg(), recent);
  }

  private SubOrderDto toSubOrderDto(SubOrder s, String shopName) {
    List<OrderItemDto> items =
        orderItems.findBySubOrderId(s.getId()).stream()
            .map(ShopController::toOrderItemDto)
            .toList();
    DeliveryDto delivery = null;
    return new SubOrderDto(
        s.getId(),
        s.getOrderId(),
        s.getShopId(),
        shopName,
        s.getStatus(),
        s.getItemsSubtotal(),
        s.getShopPayout(),
        s.getPayoutStatus(),
        items,
        delivery,
        s.getCreatedAt());
  }

  private static OrderItemDto toOrderItemDto(OrderItem i) {
    return new OrderItemDto(
        i.getId(),
        i.getProductId(),
        i.getNameSnapshot(),
        i.getImageSnapshot(),
        i.getUnitPrice(),
        i.getQuantity(),
        i.getLineTotal());
  }

  private static ShopDto toShopDto(Shop s) {
    return new ShopDto(
        s.getId(),
        s.getName(),
        s.getSlug(),
        s.getDescription(),
        s.getLogoUrl(),
        s.getBannerUrl(),
        s.getStatus(),
        s.getRatingAvg(),
        s.getRatingCount());
  }
}
