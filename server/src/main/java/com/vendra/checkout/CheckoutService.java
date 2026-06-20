package com.vendra.checkout;

import com.vendra.account.AccountService;
import com.vendra.cart.CartService;
import com.vendra.common.ApiException;
import com.vendra.domain.*;
import com.vendra.dto.Dtos.*;
import com.vendra.notify.NotificationService;
import com.vendra.order.PricingService;
import com.vendra.payment.StripeService;
import com.vendra.repo.*;
import com.vendra.security.AuthUser;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout: splits the multi-shop cart into one sub-order per shop under a parent order, computes
 * all money server-side, atomically decrements stock, and creates a Stripe PaymentIntent (capture
 * at checkout). Funds are transferred to shops/courier on delivery (see PayoutService).
 */
@Service
public class CheckoutService {

  private final CartService cartService;
  private final AccountService account;
  private final CartRepository carts;
  private final CartItemRepository cartItems;
  private final ProductRepository products;
  private final ProductVariantRepository variants;
  private final ShopRepository shops;
  private final AddressRepository addresses;
  private final PlatformSettingsRepository settingsRepo;
  private final PromoRepository promos;
  private final OrderRepository orders;
  private final SubOrderRepository subOrders;
  private final OrderItemRepository orderItems;
  private final NotificationService notifications;
  private final StripeService stripe;

  public CheckoutService(
      CartService cartService, AccountService account, CartRepository carts,
      CartItemRepository cartItems, ProductRepository products, ProductVariantRepository variants,
      ShopRepository shops, AddressRepository addresses, PlatformSettingsRepository settingsRepo,
      PromoRepository promos, OrderRepository orders, SubOrderRepository subOrders,
      OrderItemRepository orderItems, NotificationService notifications, StripeService stripe) {
    this.cartService = cartService;
    this.account = account;
    this.carts = carts;
    this.cartItems = cartItems;
    this.products = products;
    this.variants = variants;
    this.shops = shops;
    this.addresses = addresses;
    this.settingsRepo = settingsRepo;
    this.promos = promos;
    this.orders = orders;
    this.subOrders = subOrders;
    this.orderItems = orderItems;
    this.notifications = notifications;
    this.stripe = stripe;
  }

  private PlatformSettings settings() {
    return settingsRepo.findById("platform")
        .orElseThrow(() -> ApiException.badRequest("Platform settings missing"));
  }

  /** Compute a quote for the current cart without creating an order. */
  public CheckoutQuoteDto quote(String promoCode) {
    return computeQuote(loadGroups(), promoCode).dto();
  }

  @Transactional
  public CheckoutResponse checkout(CheckoutRequest req) {
    UUID uid = account.currentUserId();
    Address addr =
        addresses.findById(req.addressId()).orElseThrow(() -> ApiException.notFound("Address"));
    if (!addr.getUserId().equals(uid)) throw ApiException.forbidden("Not your address");

    Map<String, List<CartItem>> groups = loadGroups();
    if (groups.isEmpty()) throw ApiException.badRequest("Cart is empty");
    Quote q = computeQuote(groups, req.promoCode());

    // 1) atomically reserve stock for every line (conditional decrement)
    for (List<CartItem> shopItems : groups.values()) {
      for (CartItem ci : shopItems) {
        int updated =
            ci.getVariantId() != null
                ? variants.decrementStock(ci.getVariantId(), ci.getQuantity())
                : products.decrementStock(ci.getProductId(), ci.getQuantity());
        if (updated == 0) {
          Product p = products.findById(ci.getProductId()).orElse(null);
          throw ApiException.conflict(
              "Out of stock: " + (p != null ? p.getName() : ci.getProductId()));
        }
      }
    }

    // 2) create parent order
    Order order = new Order();
    order.setCustomerId(uid);
    order.setAddressId(addr.getId());
    order.setShipTo(shipToJson(addr));
    order.setItemsSubtotal(q.itemsSubtotal);
    order.setDeliveryFee(q.deliveryFee);
    order.setTax(q.tax);
    order.setDiscount(q.discount);
    order.setTotal(q.total);
    order.setPlatformCommission(q.commissionTotal);
    order.setPromoId(q.promoId);
    order.setPaymentStatus("requires_payment");
    order.setStatus("placed");
    orders.save(order);

    // 3) one sub-order per shop + its line items
    PlatformSettings s = settings();
    for (var e : groups.entrySet()) {
      String shopId = e.getKey();
      BigDecimal sub = q.perShopSubtotal.get(shopId);
      BigDecimal commission = PricingService.commission(sub, s.getCommissionRate());
      SubOrder so = new SubOrder();
      so.setOrderId(order.getId());
      so.setShopId(shopId);
      so.setStatus("placed");
      so.setItemsSubtotal(sub);
      so.setCommissionRate(s.getCommissionRate());
      so.setCommissionAmount(commission);
      so.setShopPayout(PricingService.shopPayout(sub, commission));
      so.setPayoutStatus("pending");
      subOrders.save(so);

      for (CartItem ci : e.getValue()) {
        Product p = products.findById(ci.getProductId()).orElseThrow();
        BigDecimal unit = cartService.unitPrice(p, ci.getVariantId());
        OrderItem oi = new OrderItem();
        oi.setSubOrderId(so.getId());
        oi.setProductId(p.getId());
        oi.setVariantId(ci.getVariantId());
        oi.setNameSnapshot(p.getName());
        oi.setImageSnapshot(p.getImageUrl());
        oi.setUnitPrice(unit);
        oi.setQuantity(ci.getQuantity());
        oi.setLineTotal(PricingService.lineTotal(unit, ci.getQuantity()));
        orderItems.save(oi);
      }
    }

    // 4) Stripe PaymentIntent (capture at checkout)
    String clientSecret = null, piId = null;
    if (stripe.enabled()) {
      var pi = stripe.createPaymentIntent(q.total, "usd", order.getId(), AuthUser.email());
      order.setStripePaymentIntentId(pi.getId());
      orders.save(order);
      clientSecret = pi.getClientSecret();
      piId = pi.getId();
    }

    // 5) empty the cart
    cartItems.deleteByCartId(carts.findByUserId(uid).map(Cart::getId).orElse(""));

    notifications.notify(uid, "order_placed", "Order placed",
        "Your order " + order.getId() + " has been placed.", "order", order.getId());

    return new CheckoutResponse(order.getId(), clientSecret, piId, q.dto());
  }

  // ---- internals ----
  private Map<String, List<CartItem>> loadGroups() {
    UUID uid = account.currentUserId();
    String cartId = carts.findByUserId(uid).map(Cart::getId).orElse(null);
    Map<String, List<CartItem>> groups = new LinkedHashMap<>();
    if (cartId == null) return groups;
    for (CartItem ci : cartItems.findByCartId(cartId)) {
      groups.computeIfAbsent(ci.getShopId(), k -> new ArrayList<>()).add(ci);
    }
    return groups;
  }

  private Quote computeQuote(Map<String, List<CartItem>> groups, String promoCode) {
    PlatformSettings s = settings();
    Quote q = new Quote();
    BigDecimal itemsSubtotal = BigDecimal.ZERO;
    List<ShopBreakdown> breakdown = new ArrayList<>();

    for (var e : groups.entrySet()) {
      String shopId = e.getKey();
      BigDecimal sub = BigDecimal.ZERO;
      for (CartItem ci : e.getValue()) {
        Product p =
            products.findById(ci.getProductId()).orElseThrow(() -> ApiException.notFound("Product"));
        sub = sub.add(PricingService.lineTotal(
            cartService.unitPrice(p, ci.getVariantId()), ci.getQuantity()));
      }
      sub = PricingService.money(sub);
      q.perShopSubtotal.put(shopId, sub);
      itemsSubtotal = itemsSubtotal.add(sub);
      BigDecimal commission = PricingService.commission(sub, s.getCommissionRate());
      q.commissionTotal = q.commissionTotal.add(commission);
      breakdown.add(new ShopBreakdown(shopId,
          shops.findById(shopId).map(Shop::getName).orElse("Shop"),
          sub, commission, PricingService.shopPayout(sub, commission)));
    }

    Optional<Promo> promo =
        (promoCode == null || promoCode.isBlank()) ? Optional.empty() : promos.findByCode(promoCode);
    q.promoId = promo.map(Promo::getId).orElse(null);
    q.itemsSubtotal = PricingService.money(itemsSubtotal);
    q.discount = PricingService.discount(q.itemsSubtotal, promo);
    q.deliveryFee = groups.isEmpty() ? BigDecimal.ZERO : s.getBaseDeliveryFee();
    BigDecimal taxable = q.itemsSubtotal.subtract(q.discount).add(q.deliveryFee);
    q.tax = PricingService.tax(taxable, s);
    q.total = PricingService.money(taxable.add(q.tax));
    q.breakdown = breakdown;
    return q;
  }

  private String shipToJson(Address a) {
    return String.format(
        "{\"recipient\":%s,\"line1\":%s,\"line2\":%s,\"city\":%s,\"region\":%s,"
            + "\"postal_code\":%s,\"country\":%s,\"lat\":%s,\"lng\":%s}",
        js(a.getRecipient()), js(a.getLine1()), js(a.getLine2()), js(a.getCity()),
        js(a.getRegion()), js(a.getPostalCode()), js(a.getCountry()),
        a.getLat() == null ? "null" : a.getLat(), a.getLng() == null ? "null" : a.getLng());
  }

  private static String js(String v) {
    return v == null ? "null" : "\"" + v.replace("\"", "\\\"") + "\"";
  }

  /** Internal computed quote. */
  private static class Quote {
    Map<String, BigDecimal> perShopSubtotal = new LinkedHashMap<>();
    BigDecimal itemsSubtotal = BigDecimal.ZERO;
    BigDecimal deliveryFee = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal commissionTotal = BigDecimal.ZERO;
    String promoId;
    List<ShopBreakdown> breakdown = new ArrayList<>();

    CheckoutQuoteDto dto() {
      return new CheckoutQuoteDto(
          itemsSubtotal, deliveryFee, tax, discount, total, commissionTotal, breakdown);
    }
  }
}
