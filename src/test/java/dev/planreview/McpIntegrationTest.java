package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class McpIntegrationTest {
  @TempDir Path root;

  @Test
  @Timeout(30)
  void realSdkClientInitializesDiscoversReviewsAndRejectsEscapes() throws Exception {
    Files.copy(Path.of("examples/gcp-risky.json"), root.resolve("gcp-risky.json"));
    Files.writeString(
        root.resolve("malformed.json"), "{\"private\":\"ERROR_SECRET_MARKER\",BROKEN}");
    var mapper = new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build());
    var parameters =
        ServerParameters.builder(Path.of(System.getProperty("java.home"), "bin", "java").toString())
            .args(
                "-cp",
                System.getProperty("java.class.path"),
                Main.class.getName(),
                "serve",
                "--root",
                root.toString())
            .build();
    var transport = new StdioClientTransport(parameters, mapper);
    var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
    try {
      assertEquals("terraform-change-review", client.initialize().serverInfo().name());
      assertEquals(
          List.of("list_review_rules", "review_terraform_plan"),
          client.listTools().tools().stream().map(McpSchema.Tool::name).sorted().toList());
      var result =
          client.callTool(
              new McpSchema.CallToolRequest(
                  "review_terraform_plan", Map.of("path", "gcp-risky.json")));
      assertFalse(result.isError());
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      var review = PlanReader.JSON.readTree(text);
      assertEquals(6, review.path("summary").path("changedResources").asInt());
      assertTrue(review.path("summary").path("high").asInt() > 0);
      assertFalse(text.contains("DEMO_SECRET_MUST_NEVER_APPEAR"));
      assertTrue(
          client
              .callTool(
                  new McpSchema.CallToolRequest(
                      "review_terraform_plan", Map.of("path", "../pom.xml")))
              .isError());
      var invalid =
          client.callTool(
              new McpSchema.CallToolRequest(
                  "review_terraform_plan", Map.of("path", "malformed.json")));
      assertTrue(invalid.isError());
      assertFalse(
          ((McpSchema.TextContent) invalid.content().get(0))
              .text()
              .contains("ERROR_SECRET_MARKER"));
      assertFalse(
          client.callTool(new McpSchema.CallToolRequest("list_review_rules", Map.of())).isError());
    } finally {
      client.closeGracefully();
    }
  }
}
