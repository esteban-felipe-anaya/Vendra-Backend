package com.vendra.storage;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.common.IdGenerator;
import com.vendra.config.VendraProperties;
import com.vendra.domain.Shop;
import com.vendra.dto.Dtos.UploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issues signed upload URLs so clients can PUT images straight to Supabase Storage. */
@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Uploads", description = "Signed direct-to-storage upload URLs")
public class UploadController {

  private final SupabaseStorageService storage;
  private final AccountService account;
  private final VendraProperties props;

  public UploadController(
      SupabaseStorageService storage, AccountService account, VendraProperties props) {
    this.storage = storage;
    this.account = account;
    this.props = props;
  }

  public record SignUploadRequest(@NotBlank String bucket, @NotBlank String filename) {}

  @PostMapping("/sign")
  @Operation(summary = "Get a signed upload URL for products/shops/avatars")
  public UploadUrlResponse sign(@Valid @RequestBody SignUploadRequest req) {
    var buckets = props.getSupabase().getBuckets();
    String products = buckets.getProducts();
    String shops = buckets.getShops();
    String avatars = buckets.getAvatars();
    Set<String> allowed = Set.of(products, shops, avatars);
    if (!allowed.contains(req.bucket())) {
      throw ApiException.badRequest("bucket must be one of " + allowed);
    }

    String owner;
    if (req.bucket().equals(shops)) {
      Shop shop = account.currentShop();
      owner = shop.getId();
    } else {
      owner = account.currentUserId().toString();
    }

    String safeName = sanitizeFilename(req.filename());
    String path = owner + "/" + IdGenerator.of("file") + "-" + safeName;
    return storage.createSignedUploadUrl(req.bucket(), path);
  }

  private static String sanitizeFilename(String name) {
    String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "-");
    return cleaned.isBlank() ? "upload" : cleaned;
  }
}
