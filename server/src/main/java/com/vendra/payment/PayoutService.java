package com.vendra.payment;

import com.vendra.domain.*;
import com.vendra.notify.NotificationService;
import com.vendra.repo.*;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles funds on delivery via Stripe Connect: transfers the shop payout (subtotal − commission)
 * and the courier fee from the captured charge, records payouts + ledger entries, and keeps the
 * platform commission. If Stripe is not configured the movements are still recorded (status
 * pending) so the rest of the flow proceeds; the operator settles later.
 */
@Service
public class PayoutService {

  private final ShopRepository shops;
  private final CourierRepository couriers;
  private final PayoutRepository payouts;
  private final LedgerRepository ledger;
  private final StripeService stripe;
  private final NotificationService notifications;

  public PayoutService(
      ShopRepository shops, CourierRepository couriers, PayoutRepository payouts,
      LedgerRepository ledger, StripeService stripe, NotificationService notifications) {
    this.shops = shops;
    this.couriers = couriers;
    this.payouts = payouts;
    this.ledger = ledger;
    this.stripe = stripe;
    this.notifications = notifications;
  }

  /** Pay the shop its payout for a delivered sub-order. */
  @Transactional
  public void settleShop(SubOrder so) {
    if ("paid".equals(so.getPayoutStatus())) return;
    Shop shop = shops.findById(so.getShopId()).orElse(null);
    BigDecimal amount = so.getShopPayout();

    String transferId = null;
    String status = "pending";
    if (shop != null && stripe.enabled() && shop.getStripeAccountId() != null && shop.isPayoutsEnabled()) {
      transferId =
          stripe.transfer(amount, "usd", shop.getStripeAccountId(),
              Map.of("sub_order_id", so.getId(), "shop_id", shop.getId()));
      status = "paid";
    }

    Payout p = new Payout();
    p.setPayeeType("shop");
    p.setShopId(so.getShopId());
    p.setSubOrderId(so.getId());
    p.setAmount(amount);
    p.setStatus(status);
    p.setStripeTransferId(transferId);
    payouts.save(p);

    Ledger commissionEntry = new Ledger();
    commissionEntry.setOrderId(so.getOrderId());
    commissionEntry.setSubOrderId(so.getId());
    commissionEntry.setEntryType("commission");
    commissionEntry.setAmount(so.getCommissionAmount());
    commissionEntry.setMemo("Platform commission");
    ledger.save(commissionEntry);

    Ledger payoutEntry = new Ledger();
    payoutEntry.setOrderId(so.getOrderId());
    payoutEntry.setSubOrderId(so.getId());
    payoutEntry.setEntryType("shop_payout");
    payoutEntry.setAmount(amount.negate());
    payoutEntry.setMemo("Shop payout for " + so.getId());
    ledger.save(payoutEntry);

    so.setStripeTransferId(transferId);
    so.setPayoutStatus(status);

    if (shop != null) {
      notifications.notify(shop.getOwnerId(), "payout",
          "Payout " + status, "Payout of $" + amount + " for order " + so.getOrderId() + ".",
          "sub_order", so.getId());
    }
  }

  /** Pay the courier its fee for a completed delivery. */
  @Transactional
  public void settleCourier(Delivery del) {
    if (del.getCourierId() == null || "paid".equals(del.getPayoutStatus())) return;
    Courier courier = couriers.findById(del.getCourierId()).orElse(null);
    BigDecimal amount = del.getCourierFee();

    String transferId = null;
    String status = "pending";
    if (courier != null && stripe.enabled() && courier.getStripeAccountId() != null
        && courier.isPayoutsEnabled()) {
      transferId =
          stripe.transfer(amount, "usd", courier.getStripeAccountId(),
              Map.of("delivery_id", del.getId(), "courier_id", courier.getId()));
      status = "paid";
    }

    Payout p = new Payout();
    p.setPayeeType("courier");
    p.setCourierId(del.getCourierId());
    p.setDeliveryId(del.getId());
    p.setAmount(amount);
    p.setStatus(status);
    p.setStripeTransferId(transferId);
    payouts.save(p);

    Ledger entry = new Ledger();
    entry.setOrderId(del.getOrderId());
    entry.setEntryType("courier_payout");
    entry.setAmount(amount.negate());
    entry.setMemo("Courier fee for " + del.getId());
    ledger.save(entry);

    del.setStripeTransferId(transferId);
    del.setPayoutStatus(status);

    if (courier != null) {
      notifications.notify(courier.getProfileId(), "payout",
          "Earnings added", "You earned $" + amount + " for a delivery.", "delivery", del.getId());
    }
  }
}
