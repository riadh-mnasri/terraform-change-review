package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

/** Full plan exports produced by the real Terraform binary, not hand-written envelopes. */
class GeneratedPlanTest {
  private Review fixture(String name) throws Exception {
    try (InputStream in =
        getClass().getResourceAsStream("/terraform-generated/" + name + ".json")) {
      assertNotNull(in);
      return new ReviewEngine().review(PlanReader.JSON.readTree(in));
    }
  }

  @Test
  void realCreateSupportsEntirelyUnknownOutputAndSensitiveValues() throws Exception {
    var r = fixture("create");
    assertEquals(1, r.summary().changedResources());
    assertEquals(2, r.summary().changedOutputs());
    assertTrue(
        r.changes().stream()
            .anyMatch(c -> c.address().equals("output.result") && c.unknownPaths().contains("/")));
    assertFalse(
        PlanReader.JSON.writeValueAsString(r).contains("SYNTHETIC_SECRET_FOR_REDACTION_TEST"));
    assertTrue(r.checks().stream().anyMatch(c -> c.status().equals("NOT_SUPPORTED")));
  }

  @Test
  void realReplacementAndDestructionAreDetected() throws Exception {
    assertEquals("TF-REPLACE", fixture("replace").findings().get(0).ruleId());
    assertEquals("TF-DELETE", fixture("delete").findings().get(0).ruleId());
  }

  @Test
  void realNoopProducesNoChangedItems() throws Exception {
    var r = fixture("noop");
    assertTrue(r.changes().isEmpty());
    assertTrue(r.findings().isEmpty());
  }
}
