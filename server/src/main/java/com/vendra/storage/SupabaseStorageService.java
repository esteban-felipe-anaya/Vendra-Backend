package com.vendra.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendra.common.ApiException;
import com.vendra.config.VendraProperties;
import com.vendra.dto.Dtos.UploadUrlResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Signs uploads against the Supabase Storage REST API. Clients receive a one-time signed upload URL
 * + token, PUT the bytes directly to Supabase, then persist the returned {@code publicUrl} string.
 */
@Service
public class SupabaseStorageService {

  private final VendraProperties props;
  private final ObjectMapper mapper;
  private final HttpClient http;

  public SupabaseStorageService(VendraProperties props, ObjectMapper mapper) {
    this.props = props;
    this.mapper = mapper;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  /**
   * Asks Supabase Storage for a signed upload URL. Returns the absolute upload URL, the upload
   * token, the storage path, and the eventual public URL of the object.
   */
  public UploadUrlResponse createSignedUploadUrl(String bucket, String path) {
    var sb = props.getSupabase();
    String storageUrl = trimTrailingSlash(sb.getStorageUrl());
    String endpoint = storageUrl + "/object/upload/sign/" + bucket + "/" + path;

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + sb.getServiceRoleKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new ApiException(
          HttpStatus.BAD_GATEWAY, "Storage sign request failed: " + e.getMessage());
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          "Storage sign failed (" + response.statusCode() + "): " + response.body());
    }

    String returnedUrl;
    String token;
    try {
      JsonNode node = mapper.readTree(response.body());
      returnedUrl = node.path("url").asText(null);
      token = node.path("token").asText(null);
    } catch (Exception e) {
      throw new ApiException(
          HttpStatus.BAD_GATEWAY, "Storage sign returned unparseable JSON: " + e.getMessage());
    }

    if (returnedUrl == null || token == null) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Storage sign response missing url/token");
    }

    String uploadUrl = storageUrl + returnedUrl;
    String publicUrl =
        trimTrailingSlash(sb.getUrl())
            + "/storage/v1/object/public/"
            + bucket
            + "/"
            + path;
    return new UploadUrlResponse(uploadUrl, token, path, publicUrl);
  }

  private static String trimTrailingSlash(String s) {
    if (s == null) {
      return "";
    }
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }
}
