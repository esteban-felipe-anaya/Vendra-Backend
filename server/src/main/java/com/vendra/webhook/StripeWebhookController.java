package com.vendra.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import com.vendra.config.VendraProperties;
import com.vendra.domain.Ledger;
import com.vendra.domain.Order;
import com.vendra.domain.Shop;
import com.vendra.domain.SubOrder;
import com.vendra.dto.Dtos.MessageResponse;
import com.vendra.notify.NotificationService;
import com.vendra.repo.LedgerRepository;
import com.vendra.repo.OrderRepository;
import com.vendra.repo.ShopRepository;
import com.vendra.repo.SubOrderRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Stripe webhook events (authenticated by signature, not JWT — see SecurityConfig). Returns
 * 200 for every handled or ignored event so Stripe does not retry; 400 only on signature failure.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class StripeWebhookController {

  private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

  private final VendraProperties props;
  private final OrderRepository orders;
  private final SubOrderRepository subOrders;
  private final ShopRepository shops;
  private final LedgerRepository ledger;
  private final NotificationService notifications;
  private final ObjectMapper mapper = new ObjectMapper();

  public StripeWebhookController(
      VendraProperties props,
      OrderRepository orders,
      SubOrderRepository subOrders,
      ShopRepository shops,
      LedgerRepository ledger,
      NotificationService notifications) {
    this.props = props;
    this.orders = orders;
    this.subOrders = subOrders;
    this.shops = shops;
    this.ledger = ledger;
    this.notifications = notifications;
  }

  @PostMapping("/stripe")
  @Transactional
  public ResponseEntity<MessageResponse> handle(
      @RequestBody String payload,
      @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader) {

    String secret = props.getStripe().getWebhookSecret();

    // 1) Verify signature when a secret is configured.
    if (secret != null && !secret.isBlank()) {
      try {
        Webhook.constructEvent(payload, sigHeader, secret);
      } catch (SignatureVerificationException e) {
        log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new MessageResponse("Invalid signature"));
      }
    }

    // 2) Parse leniently with Jackson (robust across Stripe API versions).
    JsonNode root;
    try {
      root = mapper.readTree(payload);
    } catch (Exception e) {
      log.warn("Stripe webhook payload not parseable as JSON: {}", e.getMessage());
      return ResponseEntity.ok(new MessageResponse("ignored: unparseable payload"));
    }

    String type = text(root, "type");
    JsonNode object = root.path("data").path("object");
    String paymentIntentId = text(object, "id");
    String metadataOrderId = text(object.path("metadata"), "order_id");

    if (type == null) {
      return ResponseEntity.ok(new MessageResponse("ignored: missing type"));
    }

    switch (type) {
      case "payment_intent.succeeded" ->
          onPaymentSucceeded(paymentIntentId, metadataOrderId, object);
      case "payment_intent.payment_failed" -> onPaymentFailed(paymentIntentId, metadataOrderId);
      default -> log.debug("Ignoring Stripe event type {}", type);
    }

    // Always 200 (other than signature failure) so Stripe stops retrying.
    return ResponseEntity.ok(new MessageResponse("ok"));
  }

  private void onPaymentSucceeded(String paymentIntentId, String orderId, JsonNode object) {
    Optional<Order> found = findOrder(paymentIntentId, orderId);
    if (found.isEmpty()) {
      log.warn(
          "payment_intent.succeeded: no order for pi={} order_id={}", paymentIntentId, orderId);
      return;
    }
    Order order = found.get();

    // Idempotent: already captured → do nothing.
    if ("captured".equals(order.getPaymentStatus())) {
      return;
    }

    if (paymentIntentId != null && order.getStripePaymentIntentId() == null) {
      order.setStripePaymentIntentId(paymentIntentId);
    }
    order.setPaymentStatus("captured");
    order.setStatus("processing");
    orders.save(order);

    // Ledger charge entry.
    Ledger entry = new Ledger();
    entry.setOrderId(order.getId());
    entry.setEntryType("charge");
    entry.setAmount(order.getTotal());
    entry.setCurrency(order.getCurrency());
    entry.setMemo("Payment captured for order " + order.getId());
    ledger.save(entry);

    // Notify each shop owner of the new order.
    for (SubOrder so : subOrders.findByOrderId(order.getId())) {
      Shop shop = shops.findById(so.getShopId()).orElse(null);
      if (shop != null && shop.getOwnerId() != null) {
        notifications.notify(
            shop.getOwnerId(),
            "new_order",
            "New order received",
            "You have a new order to fulfill.",
            "sub_order",
            so.getId());
      }
    }
  }

  private void onPaymentFailed(String paymentIntentId, String orderId) {
    Optional<Order> found = findOrder(paymentIntentId, orderId);
    if (found.isEmpty()) {
      log.warn(
          "payment_intent.payment_failed: no order for pi={} order_id={}",
          paymentIntentId,
          orderId);
      return;
    }
    Order order = found.get();
    order.setPaymentStatus("failed");
    orders.save(order);
  }

  /**
   * Locates the order by metadata order_id first (direct lookup), else by an in-memory scan over all
   * orders matching the payment intent id (no finder exists for stripePaymentIntentId).
   */
  private Optional<Order> findOrder(String paymentIntentId, String orderId) {
    if (orderId != null && !orderId.isBlank()) {
      Optional<Order> byId = orders.findById(orderId);
      if (byId.isPresent()) {
        return byId;
      }
    }
    if (paymentIntentId == null || paymentIntentId.isBlank()) {
      return Optional.empty();
    }
    List<Order> all = orders.findAll();
    return all.stream()
        .filter(o -> paymentIntentId.equals(o.getStripePaymentIntentId()))
        .findFirst();
  }

  private static String text(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isMissingNode() || v.isNull() ? null : v.asText();
  }
}
