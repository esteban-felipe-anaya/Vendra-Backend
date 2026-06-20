package com.vendra.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.TransferCreateParams;
import com.vendra.common.ApiException;
import com.vendra.config.VendraProperties;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Stripe Connect (Express) integration — real API calls in TEST mode. Onboards shops/couriers as
 * connected accounts, captures the order charge at checkout, and transfers payouts on delivery.
 */
@Service
public class StripeService {

  private final VendraProperties props;

  public StripeService(VendraProperties props) {
    this.props = props;
  }

  @PostConstruct
  void init() {
    String key = props.getStripe().getSecretKey();
    if (key != null && !key.isBlank()) {
      Stripe.apiKey = key;
    }
  }

  public boolean enabled() {
    String key = props.getStripe().getSecretKey();
    return key != null && !key.isBlank();
  }

  private void requireEnabled() {
    if (!enabled()) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Stripe is not configured. Set STRIPE_SECRET_KEY (test mode).");
    }
  }

  /** Minor units (cents) for Stripe amounts. */
  static long cents(BigDecimal major) {
    return major.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }

  // ---- connected accounts (shops & couriers) ----
  public String createExpressAccount(String email, String type) {
    requireEnabled();
    try {
      Account account =
          Account.create(
              AccountCreateParams.builder()
                  .setType(AccountCreateParams.Type.EXPRESS)
                  .setEmail(email)
                  .setCapabilities(
                      AccountCreateParams.Capabilities.builder()
                          .setTransfers(
                              AccountCreateParams.Capabilities.Transfers.builder()
                                  .setRequested(true)
                                  .build())
                          .build())
                  .putMetadata("vendra_type", type)
                  .build());
      return account.getId();
    } catch (StripeException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe account create failed: " + e.getMessage());
    }
  }

  public String createOnboardingLink(String accountId) {
    requireEnabled();
    try {
      AccountLink link =
          AccountLink.create(
              AccountLinkCreateParams.builder()
                  .setAccount(accountId)
                  .setRefreshUrl(props.getStripe().getConnectRefreshUrl())
                  .setReturnUrl(props.getStripe().getConnectReturnUrl())
                  .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                  .build());
      return link.getUrl();
    } catch (StripeException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe onboarding link failed: " + e.getMessage());
    }
  }

  public boolean payoutsEnabled(String accountId) {
    requireEnabled();
    try {
      Account a = Account.retrieve(accountId);
      return Boolean.TRUE.equals(a.getPayoutsEnabled()) && Boolean.TRUE.equals(a.getChargesEnabled());
    } catch (StripeException e) {
      return false;
    }
  }

  // ---- charge (capture at checkout) ----
  public PaymentIntent createPaymentIntent(
      BigDecimal amount, String currency, String orderId, String customerEmail) {
    requireEnabled();
    try {
      return PaymentIntent.create(
          PaymentIntentCreateParams.builder()
              .setAmount(cents(amount))
              .setCurrency(currency)
              .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
              .setReceiptEmail(customerEmail)
              .setDescription("Vendra order " + orderId)
              .putMetadata("order_id", orderId)
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .build())
              .build());
    } catch (StripeException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe PaymentIntent failed: " + e.getMessage());
    }
  }

  public PaymentIntent retrievePaymentIntent(String id) {
    requireEnabled();
    try {
      return PaymentIntent.retrieve(id);
    } catch (StripeException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe retrieve failed: " + e.getMessage());
    }
  }

  // ---- payout transfer to a connected account (on delivered) ----
  public String transfer(
      BigDecimal amount, String currency, String destinationAccountId, Map<String, String> meta) {
    requireEnabled();
    try {
      TransferCreateParams.Builder b =
          TransferCreateParams.builder()
              .setAmount(cents(amount))
              .setCurrency(currency)
              .setDestination(destinationAccountId);
      meta.forEach(b::putMetadata);
      Transfer t = Transfer.create(b.build());
      return t.getId();
    } catch (StripeException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe transfer failed: " + e.getMessage());
    }
  }
}
