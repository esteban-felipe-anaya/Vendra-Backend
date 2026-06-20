package com.vendra.shop;

import com.vendra.account.AccountService;
import com.vendra.domain.Shop;
import com.vendra.payment.StripeService;
import com.vendra.repo.ShopRepository;
import com.vendra.security.AuthUser;
import com.vendra.shop.ShopDtos.OnboardingLinkResponse;
import com.vendra.shop.ShopDtos.OnboardingStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.vendra.common.ApiException;

/** Stripe Connect (Express) onboarding for the merchant's shop. */
@RestController
@RequestMapping("/api/v1/shop/onboarding")
@PreAuthorize("hasRole('shop')")
@Tag(name = "Shop onboarding", description = "Stripe Connect onboarding for payouts")
public class ShopOnboardingController {

  private final AccountService account;
  private final ShopRepository shops;
  private final StripeService stripe;

  public ShopOnboardingController(
      AccountService account, ShopRepository shops, StripeService stripe) {
    this.account = account;
    this.shops = shops;
    this.stripe = stripe;
  }

  @PostMapping("/link")
  @Operation(summary = "Create (or reuse) a Stripe account and return an onboarding link")
  @Transactional
  public OnboardingLinkResponse link() {
    requireStripe();
    Shop shop = account.currentShop();
    if (shop.getStripeAccountId() == null || shop.getStripeAccountId().isBlank()) {
      String accountId = stripe.createExpressAccount(AuthUser.email(), "shop");
      shop.setStripeAccountId(accountId);
      shops.save(shop);
    }
    String url = stripe.createOnboardingLink(shop.getStripeAccountId());
    return new OnboardingLinkResponse(url);
  }

  @GetMapping("/status")
  @Operation(summary = "Check whether payouts are enabled for my shop")
  @Transactional
  public OnboardingStatusResponse status() {
    requireStripe();
    Shop shop = account.currentShop();
    if (shop.getStripeAccountId() == null || shop.getStripeAccountId().isBlank()) {
      return new OnboardingStatusResponse(false);
    }
    boolean enabled = stripe.payoutsEnabled(shop.getStripeAccountId());
    if (shop.isPayoutsEnabled() != enabled) {
      shop.setPayoutsEnabled(enabled);
      shops.save(shop);
    }
    return new OnboardingStatusResponse(enabled);
  }

  private void requireStripe() {
    if (!stripe.enabled()) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Stripe is not configured. Set STRIPE_SECRET_KEY (test mode).");
    }
  }
}
