package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReviewEngineTest {
  private final ReviewEngine engine = new ReviewEngine();
  @TempDir Path temp;

  private JsonNode plan(String type, String actions, String before, String after, String extra)
      throws Exception {
    return PlanReader.JSON.readTree(
        """
        {"format_version":"1.2","resource_changes":[{"address":"test.resource","type":"%s",
        "change":{"actions":%s,"before":%s,"after":%s,%s}}]}
        """
            .formatted(type, actions, before, after, extra));
  }

  private Review review(String type, String actions, String before, String after) throws Exception {
    return engine.review(plan(type, actions, before, after, "\"after_unknown\":{}"));
  }

  private List<String> ids(Review r) {
    return r.findings().stream().map(Review.Finding::ruleId).toList();
  }

  @Test
  void deleteAndBothReplacementOrdersAreFlagged() throws Exception {
    assertEquals(List.of("TF-DELETE"), ids(review("other", "[\"delete\"]", "{}", "null")));
    for (String actions : List.of("[\"delete\",\"create\"]", "[\"create\",\"delete\"]"))
      assertEquals(List.of("TF-REPLACE"), ids(review("other", actions, "{}", "{}")));
  }

  @Test
  void arbitraryProviderIsInventoriedWithoutClaimingSafety() throws Exception {
    var r = review("aws_instance", "[\"update\"]", "{\"a/b~c\":1}", "{\"a/b~c\":2}");
    assertEquals(List.of("/a~1b~0c"), r.changes().get(0).changedPaths());
    assertEquals("inventory-only", r.changes().get(0).coverage());
    assertEquals(0, r.findings().size());
  }

  @Test
  void noOpIsExcludedButImportIsIncluded() throws Exception {
    assertEquals(0, review("other", "[\"no-op\"]", "{}", "{}").changes().size());
    var p = plan("other", "[\"no-op\"]", "{}", "{}", "\"importing\":{\"id\":\"x\"}");
    assertEquals(1, engine.review(p).changes().size());
  }

  @Test
  void iamOnlyNewAssignmentsTrigger() throws Exception {
    String old = "{\"role\":\"roles/owner\",\"members\":[\"user:existing@example.test\"]}";
    String next =
        "{\"role\":\"roles/owner\",\"members\":[\"user:existing@example.test\",\"user:new@example.test\"]}";
    assertTrue(
        ids(review("google_project_iam_binding", "[\"update\"]", old, next))
            .contains("GCP-IAM-ELEVATED"));
    assertTrue(
        review("google_project_iam_binding", "[\"update\"]", next, old).findings().isEmpty());
    assertTrue(review("google_project_iam_binding", "[\"update\"]", old, old).findings().isEmpty());
  }

  @Test
  void publicPrincipalIsDetectedWithoutAssumingInternetReachability() throws Exception {
    var r =
        review(
            "google_storage_bucket_iam_member",
            "[\"create\"]",
            "null",
            "{\"role\":\"roles/storage.objectViewer\",\"member\":\"allUsers\"}");
    assertEquals(List.of("GCP-IAM-PUBLIC"), ids(r));
  }

  @Test
  void unknownAncestorAndNestedUnknownPreventDefinitiveRule() throws Exception {
    for (String unknown : List.of("true", "{\"members\":[true]}", "{\"role\":true}")) {
      var p =
          plan(
              "google_project_iam_binding",
              "[\"create\"]",
              "null",
              "{\"role\":\"roles/owner\",\"members\":[\"user:x\"]}",
              "\"after_unknown\":" + unknown);
      var r = engine.review(p);
      assertTrue(r.findings().isEmpty());
      assertFalse(r.changes().get(0).unknownPaths().isEmpty());
    }
  }

  @Test
  void sensitiveEvidenceIsRecursivelyAndAncestorRedacted() throws Exception {
    for (String mask : List.of("true", "{\"member\":true}", "{\"role\":true,\"member\":true}")) {
      var p =
          plan(
              "google_project_iam_member",
              "[\"create\"]",
              "null",
              "{\"role\":\"roles/owner\",\"member\":\"TOP_SECRET\"}",
              "\"after_sensitive\":" + mask);
      String result = PlanReader.JSON.writeValueAsString(engine.review(p));
      assertFalse(result.contains("TOP_SECRET"));
      assertTrue(result.contains("[REDACTED]"));
    }
    var p =
        plan(
            "google_project_iam_binding",
            "[\"create\"]",
            "null",
            "{\"role\":\"roles/owner\",\"members\":[\"TOP_SECRET\",\"user:other\"]}",
            "\"after_sensitive\":{\"members\":[true,false]}");
    assertFalse(PlanReader.JSON.writeValueAsString(engine.review(p)).contains("TOP_SECRET"));
  }

  @Test
  void unrelatedSecretsAreNeverReturned() throws Exception {
    var r = review("other", "[\"create\"]", "null", "{\"password\":\"TOP_SECRET\"}");
    assertFalse(PlanReader.JSON.writeValueAsString(r).contains("TOP_SECRET"));
  }

  @Test
  void firewallDetectsChangedActiveIngressButNotEgressDisabledOrDescriptionOnly() throws Exception {
    String active =
        "{\"source_ranges\":[\"::/0\"],\"allow\":[{\"protocol\":\"tcp\",\"ports\":[\"22\"]}]}";
    assertEquals(
        List.of("GCP-FIREWALL-WORLD"),
        ids(review("google_compute_firewall", "[\"create\"]", "null", active)));
    for (String suffix : List.of(",\"direction\":\"EGRESS\"}", ",\"disabled\":true}")) {
      String a = active.substring(0, active.length() - 1) + suffix;
      assertTrue(review("google_compute_firewall", "[\"create\"]", "null", a).findings().isEmpty());
    }
    String described = active.substring(0, active.length() - 1) + ",\"description\":\"new\"}";
    assertTrue(
        review("google_compute_firewall", "[\"update\"]", active, described).findings().isEmpty());
  }

  @Test
  void protectionChangesAreDetectedAndImprovementsAreNot() throws Exception {
    assertEquals(
        List.of("GCP-BUCKET-DESTROY"),
        ids(
            review(
                "google_storage_bucket",
                "[\"update\"]",
                "{\"force_destroy\":false}",
                "{\"force_destroy\":true}")));
    assertTrue(
        review(
                "google_storage_bucket",
                "[\"update\"]",
                "{\"force_destroy\":true}",
                "{\"force_destroy\":false}")
            .findings()
            .isEmpty());
    assertEquals(
        List.of("GCP-GKE-AUTOREPAIR"),
        ids(
            review(
                "google_container_node_pool",
                "[\"update\"]",
                "{\"management\":[{\"auto_repair\":true}]}",
                "{\"management\":[{\"auto_repair\":false}]}")));
    assertEquals(
        List.of("GCP-SQL-BACKUP"),
        ids(
            review(
                "google_sql_database_instance",
                "[\"update\"]",
                "{\"settings\":[{\"backup_configuration\":[{\"enabled\":true}]}]}",
                "{\"settings\":[{\"backup_configuration\":[{\"enabled\":false}]}]}")));
  }

  @Test
  void incompleteAndDeferredPlansAreExplicit() throws Exception {
    var p =
        PlanReader.JSON.readTree(
            "{\"format_version\":\"1.2\",\"planned_values\":{},\"complete\":false,\"deferred_changes\":[{}]}");
    assertFalse(engine.review(p).summary().complete());
  }

  @Test
  void outputsAreInventoriedWithoutValues() throws Exception {
    var p =
        PlanReader.JSON.readTree(
            "{\"format_version\":\"1.2\",\"planned_values\":{},\"output_changes\":{\"token\":{\"actions\":[\"update\"],\"before\":\"SECRET_OLD\",\"after\":\"SECRET_NEW\"}}}");
    var r = engine.review(p);
    assertEquals("output.token", r.changes().get(0).address());
    assertFalse(PlanReader.JSON.writeValueAsString(r).contains("SECRET_"));
  }

  @Test
  void invalidOrUnsupportedPlansAreRejected() throws Exception {
    for (String value :
        List.of(
            "null",
            "{}",
            "{\"format_version\":\"2.0\",\"resource_changes\":[]}",
            "{\"format_version\":\"1.0\",\"values\":{}}",
            "{\"format_version\":\"1.0\",\"resource_changes\":[],\"errored\":true}")) {
      assertThrows(
          IllegalArgumentException.class, () -> engine.review(PlanReader.JSON.readTree(value)));
    }
  }

  @Test
  void readerConfinesPathsIncludingSymlinksAndRejectsTrailingJson() throws Exception {
    Path root = Files.createDirectory(temp.resolve("root"));
    Files.writeString(temp.resolve("outside.json"), "{}");
    Files.createSymbolicLink(root.resolve("link.json"), temp.resolve("outside.json"));
    var reader = new PlanReader(root);
    assertThrows(IllegalArgumentException.class, () -> reader.read("../outside.json"));
    assertThrows(IllegalArgumentException.class, () -> reader.read("link.json"));
    Files.writeString(root.resolve("trailing.json"), "{} {}");
    assertThrows(IllegalArgumentException.class, () -> reader.read("trailing.json"));
  }

  @Test
  void markdownEscapesPlanControlledHtmlAndLinks() throws Exception {
    var p = plan("other", "[\"delete\"]", "{}", "null", "\"after_unknown\":{}");
    ((com.fasterxml.jackson.databind.node.ObjectNode) p.path("resource_changes").get(0))
        .put("address", "<script>[click](evil)\n# heading");
    String md = Markdown.render(engine.review(p));
    assertFalse(md.contains("<script>"));
    assertFalse(md.contains("[click]"));
  }

  @Test
  void readerRejectsOversizedInputs() throws Exception {
    Path file = temp.resolve("oversized.json");
    try (var output = Files.newOutputStream(file)) {
      byte[] block = new byte[1024 * 1024];
      for (int i = 0; i < 21; i++) output.write(block);
    }
    assertThrows(IllegalArgumentException.class, () -> new PlanReader(temp).read("oversized.json"));
  }

  @Test
  void beforeSensitivityMaskIsAppliedToEvidence() throws Exception {
    var p =
        plan(
            "google_project_iam_member",
            "[\"update\"]",
            "{\"role\":\"roles/viewer\",\"member\":\"BEFORE_SECRET\"}",
            "{\"role\":\"roles/owner\",\"member\":\"user:next\"}",
            "\"before_sensitive\":{\"member\":true}");
    assertFalse(PlanReader.JSON.writeValueAsString(engine.review(p)).contains("BEFORE_SECRET"));
  }
}
