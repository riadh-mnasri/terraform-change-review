package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsolidationTest {
  private final ReviewEngine engine = new ReviewEngine();

  private ObjectNode plan() throws Exception {
    return (ObjectNode)
        PlanReader.JSON.readTree(
            """
{"format_version":"1.2","complete":true,"resource_changes":[
  {"address":"google_project_iam_member.test","type":"google_project_iam_member","change":{
    "actions":["update"],"before":{"role":"roles/editor","member":"user:demo@example.test",
      "condition":[{"title":"temporary","expression":"request.time < timestamp('2027-01-01T00:00:00Z')"}]},
    "after":{"role":"roles/editor","member":"user:demo@example.test","condition":[]},
    "after_unknown":{}}}]}
""");
  }

  private ObjectNode change(ObjectNode p) {
    return (ObjectNode) p.path("resource_changes").get(0).path("change");
  }

  private Review.Check check(Review r, String id) {
    return r.checks().stream().filter(c -> c.ruleId().equals(id)).findFirst().orElseThrow();
  }

  @Test
  void removalOfConditionForUnchangedAssignmentIsHigh() throws Exception {
    var r = engine.review(plan());
    assertEquals(
        List.of("GCP-IAM-CONDITION-REMOVED"),
        r.findings().stream().map(Review.Finding::ruleId).toList());
    assertEquals("HIGH", r.findings().get(0).severity());
    assertEquals("EXECUTED", check(r, "GCP-IAM-CONDITION-REMOVED").status());
  }

  @Test
  void conditionExpressionChangesAreReportedWithoutInferringWidening() throws Exception {
    var p = plan();
    ((ObjectNode) change(p).path("after"))
        .set("condition", PlanReader.JSON.readTree("[{\"expression\":\"true\"}]"));
    var r = engine.review(p);
    assertEquals("GCP-IAM-CONDITION-CHANGED", r.findings().get(0).ruleId());
    assertEquals("MEDIUM", r.findings().get(0).severity());
    assertTrue(r.findings().get(0).impact().contains("does not establish"));
  }

  @Test
  void titleOnlyConditionEditAndAddedRestrictionProduceNoFinding() throws Exception {
    var p = plan();
    change(p).set("after", change(p).path("before").deepCopy());
    ((ObjectNode) change(p).path("after").path("condition").get(0)).put("title", "renamed");
    assertTrue(engine.review(p).findings().isEmpty());
    var c = change(p);
    var before = c.path("before").deepCopy();
    c.set("before", c.path("after"));
    c.set("after", before);
    ((ObjectNode) c.path("before")).putArray("condition");
    assertTrue(engine.review(p).findings().isEmpty());
  }

  @Test
  void removedPrincipalDoesNotTriggerConditionRemovalFinding() throws Exception {
    var p = plan();
    ((ObjectNode) change(p).path("after")).put("member", "user:someone-else@example.test");
    assertFalse(
        engine.review(p).findings().stream().anyMatch(f -> f.ruleId().contains("CONDITION")));
  }

  @Test
  void bindingWithRetainedPrincipalAlsoDetectsConditionRemoval() throws Exception {
    var p = plan();
    ((ObjectNode) p.path("resource_changes").get(0)).put("type", "google_project_iam_binding");
    for (String side : List.of("before", "after")) {
      ObjectNode n = (ObjectNode) change(p).path(side);
      n.remove("member");
      n.putArray("members").add("user:retained@example.test");
    }
    assertTrue(
        engine.review(p).findings().stream()
            .anyMatch(f -> f.ruleId().equals("GCP-IAM-CONDITION-REMOVED")));
  }

  @Test
  void unknownConditionIsExplicitlySkippedButIndependentChecksRun() throws Exception {
    var p = plan();
    change(p)
        .set("after_unknown", PlanReader.JSON.readTree("{\"condition\":[{\"expression\":true}]}"));
    var r = engine.review(p);
    assertEquals("SKIPPED", check(r, "GCP-IAM-CONDITION-REMOVED").status());
    assertEquals("UNKNOWN_VALUE", check(r, "GCP-IAM-CONDITION-REMOVED").reasonCode());
    assertEquals("EXECUTED", check(r, "GCP-IAM-ELEVATED").status());
    assertTrue(r.findings().isEmpty());
    assertEquals(2, r.summary().checksSkipped());
  }

  @Test
  void redactionCoversBothConditionExpressions() throws Exception {
    var p = plan();
    ((ObjectNode) change(p).path("before").path("condition").get(0))
        .put("expression", "CONDITION_SECRET");
    change(p)
        .set(
            "before_sensitive",
            PlanReader.JSON.readTree("{\"condition\":[{\"expression\":true}]}"));
    assertFalse(PlanReader.JSON.writeValueAsString(engine.review(p)).contains("CONDITION_SECRET"));
  }

  @Test
  void missingCompletenessIsUnknownAndMalformedCompletenessIsRejected() throws Exception {
    var p = plan();
    p.remove("complete");
    assertNull(engine.review(p).summary().complete());
    p.put("complete", "true");
    assertThrows(PlanValidator.InvalidPlan.class, () -> engine.review(p));
  }

  @Test
  void missingBeforeOrAfterAndMalformedShapesAreRejected() throws Exception {
    for (String key : List.of("before", "after", "actions")) {
      var p = plan();
      change(p).remove(key);
      assertThrows(PlanValidator.InvalidPlan.class, () -> engine.review(p));
    }
    for (String key : List.of("before_sensitive", "after_sensitive", "after_unknown")) {
      var p = plan();
      change(p).put(key, "not-a-mask");
      assertThrows(PlanValidator.InvalidPlan.class, () -> engine.review(p));
    }
    var p = plan();
    p.putArray("output_changes");
    assertThrows(PlanValidator.InvalidPlan.class, () -> engine.review(p));
  }

  @Test
  void missingRuleAttributeDoesNotCountAsExecuted() throws Exception {
    var p = plan();
    ((ObjectNode) change(p).path("after")).remove("member");
    var r = engine.review(p);
    assertEquals("MISSING_ATTRIBUTE", check(r, "GCP-IAM-ELEVATED").reasonCode());
    assertEquals("SKIPPED", check(r, "GCP-IAM-ELEVATED").status());
  }
}
