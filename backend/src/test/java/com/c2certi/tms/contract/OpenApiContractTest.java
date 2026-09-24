package com.c2certi.tms.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2certi.tms.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.yaml.snakeyaml.Yaml;

/** Every operation in the published contract must exist in the running API. */
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiContractTest extends IntegrationTest {

  private static final Path CONTRACT =
      Path.of("../specs/001-support-ticket-management/contracts/openapi.yaml");
  private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");

  @Autowired ObjectMapper json;

  @Test
  @SuppressWarnings("unchecked")
  void implementationCoversEveryContractOperation() throws Exception {
    Map<String, Object> contract;
    try (InputStream in = Files.newInputStream(CONTRACT)) {
      contract = new Yaml().load(in);
    }
    String basePath =
        (String) ((List<Map<String, Object>>) contract.get("servers")).get(0).get("url");
    Map<String, Map<String, Object>> paths =
        (Map<String, Map<String, Object>>) contract.get("paths");

    String docs =
        mvc.perform(api.get(users.create("docs"), "/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode actualPaths = json.readTree(docs).path("paths");

    List<String> missing = new ArrayList<>();
    int checked = 0;
    for (Map.Entry<String, Map<String, Object>> path : paths.entrySet()) {
      for (String method : path.getValue().keySet()) {
        if (!METHODS.contains(method)) {
          continue;
        }
        checked++;
        String fullPath = basePath + path.getKey();
        if (actualPaths.path(fullPath).path(method).isMissingNode()) {
          missing.add(method.toUpperCase() + " " + fullPath);
        }
      }
    }
    assertThat(checked).isEqualTo(12);
    assertThat(missing).as("operations in contract but not implemented").isEmpty();
  }
}
