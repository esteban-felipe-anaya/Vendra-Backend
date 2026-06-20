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
import com.vendra.admin.AdminDtos.UserDto;
import com.vendra.dto.Dtos.CategoryDto;
import com.vendra.dto.Dtos.MessageResponse;
import com.vendra.dto.Dtos.OrderDto;
import com.vendra.dto.Dtos.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform admin API: dashboard, moderation, catalog/promo/settings management, oversight. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('admin')")
@Tag(name = "Admin", description = "Platform administration: moderation, settings, oversight")
public class AdminController {

  private final AdminService admin;

  public AdminController(AdminService admin) {
    this.admin = admin;
  }

  // ---------- dashboard ----------
  @GetMapping("/dashboard")
  @Operation(summary = "Platform dashboard metrics")
  public DashboardDto dashboard() {
    return admin.dashboard();
  }

  // ---------- shops ----------
  @GetMapping("/shops")
  @Operation(summary = "List all shops")
  public AdminShopDto[] shops() {
    return admin.listShops();
  }

  @PostMapping("/shops/{id}/approve")
  @Operation(summary = "Approve a shop")
  public AdminShopDto approveShop(@PathVariable String id) {
    return admin.approveShop(id);
  }

  @PostMapping("/shops/{id}/suspend")
  @Operation(summary = "Suspend a shop")
  public AdminShopDto suspendShop(@PathVariable String id) {
    return admin.suspendShop(id);
  }

  // ---------- products ----------
  @GetMapping("/products")
  @Operation(summary = "List all products")
  public ProductDto[] products() {
    return admin.listProducts();
  }

  @PostMapping("/products/{id}/moderate")
  @Operation(summary = "Activate or deactivate a product")
  public ProductDto moderateProduct(
      @PathVariable String id, @RequestBody ModerateProductRequest req) {
    return admin.moderateProduct(id, req);
  }

  // ---------- categories ----------
  @GetMapping("/categories")
  @Operation(summary = "List all categories")
  public CategoryDto[] categories() {
    return admin.listCategories();
  }

  @PostMapping("/categories")
  @Operation(summary = "Create a category")
  public CategoryDto createCategory(@RequestBody CategoryRequest req) {
    return admin.createCategory(req);
  }

  @PutMapping("/categories/{id}")
  @Operation(summary = "Update a category")
  public CategoryDto updateCategory(@PathVariable String id, @RequestBody CategoryRequest req) {
    return admin.updateCategory(id, req);
  }

  @DeleteMapping("/categories/{id}")
  @Operation(summary = "Delete a category")
  public MessageResponse deleteCategory(@PathVariable String id) {
    admin.deleteCategory(id);
    return new MessageResponse("Category deleted");
  }

  // ---------- couriers ----------
  @GetMapping("/couriers")
  @Operation(summary = "List all couriers")
  public AdminCourierDto[] couriers() {
    return admin.listCouriers();
  }

  // ---------- users ----------
  @GetMapping("/users")
  @Operation(summary = "List all user profiles")
  public UserDto[] users() {
    return admin.listUsers();
  }

  // ---------- promos ----------
  @GetMapping("/promos")
  @Operation(summary = "List all promos")
  public PromoDto[] promos() {
    return admin.listPromos();
  }

  @PostMapping("/promos")
  @Operation(summary = "Create a promo")
  public PromoDto createPromo(@RequestBody PromoRequest req) {
    return admin.createPromo(req);
  }

  @PutMapping("/promos/{id}")
  @Operation(summary = "Update a promo")
  public PromoDto updatePromo(@PathVariable String id, @RequestBody PromoRequest req) {
    return admin.updatePromo(id, req);
  }

  @DeleteMapping("/promos/{id}")
  @Operation(summary = "Delete a promo")
  public MessageResponse deletePromo(@PathVariable String id) {
    admin.deletePromo(id);
    return new MessageResponse("Promo deleted");
  }

  // ---------- settings ----------
  @GetMapping("/settings")
  @Operation(summary = "Get platform commission/fee settings")
  public SettingsDto settings() {
    return admin.getSettings();
  }

  @PutMapping("/settings")
  @Operation(summary = "Update platform commission/fee settings")
  public SettingsDto updateSettings(@RequestBody SettingsRequest req) {
    return admin.updateSettings(req);
  }

  // ---------- orders ----------
  @GetMapping("/orders")
  @Operation(summary = "List all orders (newest first)")
  public OrderDto[] orders() {
    return admin.listOrders();
  }

  @GetMapping("/orders/{id}")
  @Operation(summary = "Get one order")
  public OrderDto order(@PathVariable String id) {
    return admin.getOrder(id);
  }

  // ---------- payouts / ledger ----------
  @GetMapping("/payouts")
  @Operation(summary = "List all payout rows")
  public PayoutDto[] payouts() {
    return admin.listPayouts();
  }

  @GetMapping("/ledger")
  @Operation(summary = "List all ledger entries")
  public LedgerDto[] ledger() {
    return admin.listLedger();
  }
}
