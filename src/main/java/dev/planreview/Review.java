package dev.planreview;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record Review(
    String schemaVersion,
    Summary summary,
    List<Change> changes,
    List<Finding> findings,
    List<Check> checks,
    List<String> limitations) {
  public record Summary(
      int changedResources,
      int changedOutputs,
      int findings,
      int high,
      int medium,
      int resourcesWithoutSpecializedChecks,
      Boolean complete,
      int checksExecuted,
      int checksSkipped,
      int itemsWithoutSpecializedChecks) {}

  public record Change(
      String address,
      String type,
      List<String> actions,
      List<String> changedPaths,
      List<String> unknownPaths,
      String coverage,
      String previousAddress,
      boolean importing) {}

  public record Check(String resource, String ruleId, String status, String reasonCode) {}

  public record Evidence(String path, JsonNode before, JsonNode after) {}

  public record Finding(
      String ruleId,
      String severity,
      String resource,
      String title,
      String impact,
      List<Evidence> evidence,
      String nextCheck) {}
}
