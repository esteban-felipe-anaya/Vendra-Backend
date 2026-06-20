package com.vendra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full app against in-memory H2 and writes the generated OpenAPI document to
 * docs/openapi.json — the shared client contract. Run with: {@code mvn test -Dtest=OpenApiExportTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("openapidoc")
class OpenApiExportTest {

  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;

  @Test
  void exportOpenApiSpec() throws Exception {
    String json = rest.getForObject("http://localhost:" + port + "/v3/api-docs", String.class);
    assertThat(json).contains("\"openapi\"").contains("/api/v1/checkout");

    ObjectMapper mapper = new ObjectMapper();
    Object pretty = mapper.readValue(json, Object.class);
    // module dir is backend/server → repo root is ../../ ; docs lives at ../../../docs
    Path out = Path.of("..", "..", "docs", "openapi.json").toAbsolutePath().normalize();
    Files.createDirectories(out.getParent());
    Files.writeString(out, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pretty));
    System.out.println("[vendra] OpenAPI spec written to " + out);
  }
}
