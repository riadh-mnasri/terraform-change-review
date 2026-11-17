package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Unmodified Terraform/Google-provider exports planned against fabricated local state. */
class GoogleProviderPlanTest {
  private JsonNode fixture(String name) throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/google-generated/" + name + ".json")) {
      assertNotNull(in);
      return PlanReader.JSON.readTree(in);
    }
  }

  private Set<String> findings(Review review) {
    return review.findings().stream()
        .map(f -> f.ruleId() + ":" + f.resource())
        .collect(Collectors.toSet());
  }

  @Test
  void creationHandlesProviderDefaultsSetsAndComputedUnknowns() throws Exception {
    var plan = fixture("create");
    assertEquals("1.15.7", plan.path("terraform_version").asText());
    var review = new ReviewEngine().review(plan);
    assertEquals(6, review.summary().changedResources());
    assertTrue(review.summary().complete());
    assertEquals(
        Set.of(
            "GCP-IAM-ELEVATED:google_project_iam_member.reviewer",
            "GCP-IAM-ELEVATED:google_project_iam_binding.reviewers"),
        findings(review));
    assertTrue(review.changes().stream().allMatch(c -> c.unknownPaths().contains("/id")));
    assertEquals(0, review.summary().checksSkipped());
  }

  @Test
  void riskyTransitionsPreserveProviderReplacementSemanticsAndEvidence() throws Exception {
    var review = new ReviewEngine().review(fixture("risky-update"));
    assertEquals(
        Set.of(
            "GCP-FIREWALL-WORLD:google_compute_firewall.ingress",
            "GCP-IAM-ELEVATED:google_project_iam_binding.reviewers",
            "GCP-IAM-PUBLIC:google_project_iam_binding.reviewers",
            "GCP-IAM-CONDITION-REMOVED:google_project_iam_member.reviewer",
            "TF-REPLACE:google_project_iam_member.reviewer",
            "GCP-BUCKET-DESTROY:google_storage_bucket.archive",
            "GCP-BUCKET-ACCESS:google_storage_bucket.archive",
            "GCP-IAM-PUBLIC:google_storage_bucket_iam_binding.readers",
            "GCP-IAM-PUBLIC:google_storage_bucket_iam_member.reader",
            "TF-REPLACE:google_storage_bucket_iam_member.reader"),
        findings(review));
    var condition =
        review.findings().stream()
            .filter(f -> f.ruleId().equals("GCP-IAM-CONDITION-REMOVED"))
            .findFirst()
            .orElseThrow();
    var evidence =
        condition.evidence().stream()
            .filter(e -> e.path().equals("/condition"))
            .findFirst()
            .orElseThrow();
    assertTrue(evidence.before().get(0).path("expression").asText().contains("request.time"));
    assertTrue(evidence.after().isEmpty());
    assertEquals(0, review.summary().checksSkipped());
  }

  @Test
  void expressionEditReportsSemanticUncertaintyAlongsideReplacement() throws Exception {
    var review = new ReviewEngine().review(fixture("condition-expression"));
    assertEquals(
        Set.of(
            "TF-REPLACE:google_project_iam_member.reviewer",
            "GCP-IAM-CONDITION-CHANGED:google_project_iam_member.reviewer"),
        findings(review));
    var condition =
        review.findings().stream()
            .filter(f -> f.ruleId().equals("GCP-IAM-CONDITION-CHANGED"))
            .findFirst()
            .orElseThrow();
    assertEquals("MEDIUM", condition.severity());
    assertTrue(condition.impact().contains("does not establish"));
  }

  @Test
  void labelsAndComputedOnlyChangesDoNotTriggerRiskFindings() throws Exception {
    var review = new ReviewEngine().review(fixture("labels-only"));
    assertTrue(review.findings().isEmpty());
    assertTrue(
        review.changes().stream()
            .anyMatch(
                c ->
                    c.address().equals("google_storage_bucket.archive")
                        && c.changedPaths().contains("/labels/fixture")));
    assertTrue(
        review.changes().stream()
            .anyMatch(
                c ->
                    c.address().equals("google_compute_firewall.ingress")
                        && c.unknownPaths().contains("/destination_ranges")));
  }

  @Test
  void recoveryRemovesSpecializedFindingsButStillReportsIamReplacements() throws Exception {
    var review = new ReviewEngine().review(fixture("recovery"));
    assertEquals(
        Set.of(
            "TF-REPLACE:google_project_iam_member.reviewer",
            "TF-REPLACE:google_storage_bucket_iam_member.reader"),
        findings(review));
    assertEquals(0, review.summary().checksSkipped());
  }
}
