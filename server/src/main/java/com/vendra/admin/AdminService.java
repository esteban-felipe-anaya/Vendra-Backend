package com.vendra.admin;

import com.vendra.admin.AdminDtos.AdminCourierDto;
import com.vendra.admin.AdminDtos.AdminShopDto;
import com.vendra.admin.AdminDtos.CategoryRequest;
import com.vendra.admin.AdminDtos.DashboardDto;
import com.vendra.admin.AdminDtos.LedgerDto;
import com.vendra.admin.AdminDtos.ModerateProductRequest;
import com.vendra.admin.AdminDtos.PayoutDto;
import com.vendra.admin.AdminDtos.PromoDto;
import com.vendra.admin.AdminDtos.PromoRequest;
import com.vendra.admin.AdminDtos.SettingsDto;
import com.vendra.admin.AdminDtos.SettingsRequest;
import com.vendra.admin.AdminDtos.TopShopDto;
import com.vendra.admin.AdminDtos.UserDto;
import com.vendra.common.ApiException;
import com.vendra.common.IdGenerator;
import com.vendra.domain.Category;
import com.vendra.domain.Ledger;
import com.vendra.domain.Order;
import com.vendra.domain.PlatformSettings;
import com.vendra.domain.Product;
import com.vendra.domain.Profile;
import com.vendra.domain.Promo;
import com.vendra.domain.Shop;
import com.vendra.domain.SubOrder;
import com.vendra.dto.Dtos.CategoryDto;
import com.vendra.dto.Dtos.OrderDto;
import com.vendra.dto.Dtos.ProductDto;
import com.vendra.notify.NotificationService;
import com.vendra.order.OrderMapper;
import com.vendra.repo.CategoryRepository;
import com.vendra.repo.CourierRepository;
import com.vendra.repo.LedgerRepository;
import com.vendra.repo.OrderRepository;
import com.vendra.repo.PayoutRepository;
import com.vendra.repo.PlatformSettingsRepository;
import com.vendra.repo.ProductRepository;
import com.vendra.repo.ProfileRepository;
import com.vendra.repo.PromoRepository;
import com.vendra.repo.ShopRepository;
import com.vendra.repo.SubOrderRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic backing the platform admin API. */
@Service
public class AdminService {

  private static final String SETTINGS_ID = "platform";
  private static final Set<String> ACTIVE_AVAILABILITY = Set.of("available", "busy");

  private final OrderRepository orders;
  private final SubOrderRepository subOrders;
  private final ShopRepository shops;
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final CourierRepository couriers;
  private final ProfileRepository profiles;
  private final PromoRepository promos;
  private final PlatformSettingsRepository settings;
  private final PayoutRepository payouts;
  private final LedgerRepository ledger;
  private final OrderMapper orderMapper;
  private final NotificationService notifications;

  public AdminService(
      OrderRepository orders,
      SubOrderRepository subOrders,
      ShopRepository shops,
      ProductRepository products,
      CategoryRepository categories,
      CourierRepository couriers,
      ProfileRepository profiles,
      PromoRepository promos,
      PlatformSettingsRepository settings,
      PayoutRepository payouts,
      LedgerRepository ledger,
      OrderMapper orderMapper,
      NotificationService notifications) {
    this.orders = orders;
    this.subOrders = subOrders;
    this.shops = shops;
    this.products = products;
    this.categories = categories;
    this.couriers = couriers;
    this.profiles = profiles;
    this.promos = promos;
    this.settings = settings;
    this.payouts = payouts;
    this.ledger = ledger;
    this.orderMapper = orderMapper;
    this.notifications = notifications;
  }

  // ---------- dashboard ----------
  public DashboardDto dashboard() {
    List<Order> allOrders = orders.findAll();

    BigDecimal gmv =
        allOrders.stream()
            .filter(o -> "captured".equals(o.getPaymentStatus()))
            .map(Order::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    long ordersCount = allOrders.size();

    long activeCouriers =
        couriers.findAll().stream()
            .filter(c -> ACTIVE_AVAILABILITY.contains(c.getAvailability()))
            .count();

    long shopsCount = shops.count();
    long productsCount = products.count();

    // top shops by summing delivered SubOrder.shopPayout
    Map<String, BigDecimal> revenueByShop = new HashMap<>();
    for (SubOrder so : subOrders.findByStatus("delivered")) {
      revenueByShop.merge(
          so.getShopId(),
          so.getShopPayout() == null ? BigDecimal.ZERO : so.getShopPayout(),
          BigDecimal::add);
    }
    TopShopDto[] topShops =
        revenueByShop.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .limit(5)
            .map(
                e ->
                    new TopShopDto(
                        e.getKey(),
                        shops.findById(e.getKey()).map(Shop::getName).orElse("Shop"),
                        e.getValue()))
            .toArray(TopShopDto[]::new);

    OrderDto[] recentOrders =
        allOrders.stream()
            .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(10)
            .map(orderMapper::toOrderDto)
            .toArray(OrderDto[]::new);

    BigDecimal ledgerBalance =
        ledger.findAll().stream().map(Ledger::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new DashboardDto(
        gmv, ordersCount, activeCouriers, shopsCount, productsCount, topShops, recentOrders,
        ledgerBalance);
  }

  // ---------- shops ----------
  public AdminShopDto[] listShops() {
    return shops.findAll().stream().map(this::toAdminShopDto).toArray(AdminShopDto[]::new);
  }

  @Transactional
  public AdminShopDto approveShop(String id) {
    return setShopStatus(id, "approved", "Shop approved", "Your shop has been approved.");
  }

  @Transactional
  public AdminShopDto suspendShop(String id) {
    return setShopStatus(id, "suspended", "Shop suspended", "Your shop has been suspended.");
  }

  private AdminShopDto setShopStatus(String id, String status, String title, String body) {
    Shop shop = shops.findById(id).orElseThrow(() -> ApiException.notFound("Shop"));
    shop.setStatus(status);
    Shop saved = shops.save(shop);
    notifications.notify(saved.getOwnerId(), "shop_status", title, body, "shop", saved.getId());
    return toAdminShopDto(saved);
  }

  private AdminShopDto toAdminShopDto(Shop s) {
    String ownerName =
        s.getOwnerId() == null
            ? null
            : profiles.findById(s.getOwnerId()).map(Profile::getFullName).orElse(null);
    return new AdminShopDto(
        s.getId(),
        s.getName(),
        s.getSlug(),
        s.getDescription(),
        s.getLogoUrl(),
        s.getBannerUrl(),
        s.getStatus(),
        s.getOwnerId() == null ? null : s.getOwnerId().toString(),
        ownerName,
        s.getRatingAvg(),
        s.getRatingCount());
  }

  // ---------- products ----------
  public ProductDto[] listProducts() {
    return products.findAll().stream().map(this::toProductDto).toArray(ProductDto[]::new);
  }

  @Transactional
  public ProductDto moderateProduct(String id, ModerateProductRequest req) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    p.setActive(req.isActive());
    return toProductDto(products.save(p));
  }

  private ProductDto toProductDto(Product p) {
    String shopName =
        p.getShopId() == null
            ? null
            : shops.findById(p.getShopId()).map(Shop::getName).orElse(null);
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

  // ---------- categories ----------
  public CategoryDto[] listCategories() {
    return categories.findAll().stream().map(AdminService::toCategoryDto).toArray(CategoryDto[]::new);
  }

  @Transactional
  public CategoryDto createCategory(CategoryRequest req) {
    if (req.name() == null || req.name().isBlank()) {
      throw ApiException.badRequest("Category name is required");
    }
    Category c = new Category();
    c.setName(req.name());
    c.setSlug(req.slug() != null && !req.slug().isBlank() ? req.slug() : Slugs.slugify(req.name()));
    c.setIcon(req.icon());
    c.setImageUrl(req.imageUrl());
    if (req.sortOrder() != null) {
      c.setSortOrder(req.sortOrder());
    }
    return toCategoryDto(categories.save(c));
  }

  @Transactional
  public CategoryDto updateCategory(String id, CategoryRequest req) {
    Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
    if (req.name() != null && !req.name().isBlank()) {
      c.setName(req.name());
    }
    if (req.slug() != null && !req.slug().isBlank()) {
      c.setSlug(req.slug());
    }
    if (req.icon() != null) {
      c.setIcon(req.icon());
    }
    if (req.imageUrl() != null) {
      c.setImageUrl(req.imageUrl());
    }
    if (req.sortOrder() != null) {
      c.setSortOrder(req.sortOrder());
    }
    return toCategoryDto(categories.save(c));
  }

  @Transactional
  public void deleteCategory(String id) {
    if (!categories.existsById(id)) {
      throw ApiException.notFound("Category");
    }
    categories.deleteById(id);
  }

  private static CategoryDto toCategoryDto(Category c) {
    return new CategoryDto(
        c.getId(), c.getName(), c.getSlug(), c.getIcon(), c.getImageUrl(), c.getSortOrder());
  }

  // ---------- couriers ----------
  public AdminCourierDto[] listCouriers() {
    return couriers.findAll().stream()
        .map(
            c -> {
              String name =
                  c.getProfileId() == null
                      ? null
                      : profiles.findById(c.getProfileId()).map(Profile::getFullName).orElse(null);
              return new AdminCourierDto(
                  c.getId(),
                  c.getProfileId() == null ? null : c.getProfileId().toString(),
                  name,
                  c.getVehicleType(),
                  c.getAvailability(),
                  c.getCurrentLat(),
                  c.getCurrentLng(),
                  c.isPayoutsEnabled(),
                  c.getRatingAvg(),
                  c.getRatingCount());
            })
        .toArray(AdminCourierDto[]::new);
  }

  // ---------- users ----------
  public UserDto[] listUsers() {
    return profiles.findAll().stream()
        .map(
            p ->
                new UserDto(
                    p.getId() == null ? null : p.getId().toString(),
                    p.getRole(),
                    p.getFullName(),
                    p.getPhone(),
                    p.getAvatarUrl()))
        .toArray(UserDto[]::new);
  }

  // ---------- promos ----------
  public PromoDto[] listPromos() {
    return promos.findAll().stream().map(AdminService::toPromoDto).toArray(PromoDto[]::new);
  }

  @Transactional
  public PromoDto createPromo(PromoRequest req) {
    if (req.code() == null || req.code().isBlank()) {
      throw ApiException.badRequest("Promo code is required");
    }
    if (req.discountType() == null || req.discountType().isBlank()) {
      throw ApiException.badRequest("discountType is required");
    }
    if (req.amount() == null) {
      throw ApiException.badRequest("amount is required");
    }
    Promo p = new Promo();
    p.setCode(req.code());
    p.setDescription(req.description());
    p.setDiscountType(req.discountType());
    p.setAmount(req.amount());
    p.setMinSubtotal(req.minSubtotal() != null ? req.minSubtotal() : BigDecimal.ZERO);
    p.setMaxRedemptions(req.maxRedemptions());
    p.setStartsAt(req.startsAt());
    p.setEndsAt(req.endsAt());
    if (req.isActive() != null) {
      p.setActive(req.isActive());
    }
    return toPromoDto(promos.save(p));
  }

  @Transactional
  public PromoDto updatePromo(String id, PromoRequest req) {
    Promo p = promos.findById(id).orElseThrow(() -> ApiException.notFound("Promo"));
    if (req.code() != null && !req.code().isBlank()) {
      p.setCode(req.code());
    }
    if (req.description() != null) {
      p.setDescription(req.description());
    }
    if (req.discountType() != null && !req.discountType().isBlank()) {
      p.setDiscountType(req.discountType());
    }
    if (req.amount() != null) {
      p.setAmount(req.amount());
    }
    if (req.minSubtotal() != null) {
      p.setMinSubtotal(req.minSubtotal());
    }
    if (req.maxRedemptions() != null) {
      p.setMaxRedemptions(req.maxRedemptions());
    }
    if (req.startsAt() != null) {
      p.setStartsAt(req.startsAt());
    }
    if (req.endsAt() != null) {
      p.setEndsAt(req.endsAt());
    }
    if (req.isActive() != null) {
      p.setActive(req.isActive());
    }
    return toPromoDto(promos.save(p));
  }

  @Transactional
  public void deletePromo(String id) {
    if (!promos.existsById(id)) {
      throw ApiException.notFound("Promo");
    }
    promos.deleteById(id);
  }

  private static PromoDto toPromoDto(Promo p) {
    return new PromoDto(
        p.getId(),
        p.getCode(),
        p.getDescription(),
        p.getDiscountType(),
        p.getAmount(),
        p.getMinSubtotal(),
        p.getMaxRedemptions(),
        p.getRedeemedCount(),
        p.getStartsAt(),
        p.getEndsAt(),
        p.isActive());
  }

  // ---------- settings ----------
  public SettingsDto getSettings() {
    return toSettingsDto(loadSettings());
  }

  @Transactional
  public SettingsDto updateSettings(SettingsRequest req) {
    PlatformSettings s = loadSettings();
    if (req.commissionRate() != null) {
      s.setCommissionRate(req.commissionRate());
    }
    if (req.baseDeliveryFee() != null) {
      s.setBaseDeliveryFee(req.baseDeliveryFee());
    }
    if (req.courierFeeShare() != null) {
      s.setCourierFeeShare(req.courierFeeShare());
    }
    if (req.taxRate() != null) {
      s.setTaxRate(req.taxRate());
    }
    if (req.currency() != null && !req.currency().isBlank()) {
      s.setCurrency(req.currency());
    }
    return toSettingsDto(settings.save(s));
  }

  private PlatformSettings loadSettings() {
    return settings
        .findById(SETTINGS_ID)
        .orElseGet(
            () -> {
              PlatformSettings s = new PlatformSettings();
              s.setId(SETTINGS_ID);
              return settings.save(s);
            });
  }

  private static SettingsDto toSettingsDto(PlatformSettings s) {
    return new SettingsDto(
        s.getCommissionRate(),
        s.getBaseDeliveryFee(),
        s.getCourierFeeShare(),
        s.getTaxRate(),
        s.getCurrency());
  }

  // ---------- orders ----------
  public OrderDto[] listOrders() {
    return orders.findAll().stream()
        .sorted(
            Comparator.comparing(
                    Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed())
        .map(orderMapper::toOrderDto)
        .toArray(OrderDto[]::new);
  }

  public OrderDto getOrder(String id) {
    Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    return orderMapper.toOrderDto(o);
  }

  // ---------- payouts / ledger ----------
  public PayoutDto[] listPayouts() {
    return payouts.findAll().stream()
        .map(
            p ->
                new PayoutDto(
                    p.getId(),
                    p.getPayeeType(),
                    p.getShopId(),
                    p.getCourierId(),
                    p.getSubOrderId(),
                    p.getDeliveryId(),
                    p.getAmount(),
                    p.getCurrency(),
                    p.getStatus(),
                    p.getStripeTransferId(),
                    p.getCreatedAt()))
        .toArray(PayoutDto[]::new);
  }

  public LedgerDto[] listLedger() {
    return ledger.findAll().stream()
        .map(
            l ->
                new LedgerDto(
                    l.getId(),
                    l.getOrderId(),
                    l.getSubOrderId(),
                    l.getEntryType(),
                    l.getAmount(),
                    l.getCurrency(),
                    l.getMemo(),
                    l.getCreatedAt()))
        .toArray(LedgerDto[]::new);
  }

  // minimal slug helper (avoids a cross-package dependency on shop.Slugs)
  static final class Slugs {
    private Slugs() {}

    static String slugify(String input) {
      String base =
          input == null
              ? ""
              : input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
      if (base.isBlank()) {
        base = "cat";
      }
      return base + "-" + IdGenerator.of("").substring(1, 7);
    }
  }
}
