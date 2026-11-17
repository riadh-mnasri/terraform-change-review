package dev.planreview;

import static dev.planreview.JsonSupport.*;
import static dev.planreview.Review.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.*;

/** Deterministic analysis. Each rule evaluation produces an explicit coverage outcome. */
public final class ReviewEngine {
  public static final List<RuleCatalog.Rule> RULES = RuleCatalog.ALL;
  private static final Set<String> ELEVATED =
      Set.of(
          "roles/owner",
          "roles/editor",
          "roles/iam.securityAdmin",
          "roles/resourcemanager.projectIamAdmin",
          "roles/iam.serviceAccountTokenCreator",
          "roles/iam.serviceAccountUser");

  public Review review(JsonNode plan) {
    PlanValidator.validate(plan);
    var changes = new ArrayList<Change>();
    var findings = new ArrayList<Finding>();
    var checks = new ArrayList<Check>();
    for (JsonNode r : plan.path("resource_changes")) {
      JsonNode c = r.path("change");
      List<String> actions = strings(c.path("actions"));
      if (actions.equals(List.of("no-op")) && !c.has("importing") && !r.has("previous_address"))
        continue;
      String address = r.path("address").asText(), type = r.path("type").asText();
      boolean managed = r.path("mode").asText("managed").equals("managed");
      boolean specialized =
          managed && RULES.stream().anyMatch(rule -> rule.resourceTypes().contains(type));
      changes.add(
          inventory(
              address,
              type,
              c,
              specialized ? "partial-specialized" : "inventory-only",
              r.path("previous_address").asText(null),
              c.has("importing")));
      if (!specialized)
        checks.add(
            new Check(
                address,
                "SPECIALIZED",
                "NOT_SUPPORTED",
                managed ? "RESOURCE_TYPE_NOT_SUPPORTED" : "DATA_RESOURCE"));
      for (var rule : RULES) {
        if (!rule.resourceTypes().isEmpty() && !rule.resourceTypes().contains(type)) continue;
        String reason = skipReason(r, rule);
        checks.add(
            new Check(
                address,
                rule.id(),
                reason == null ? "EXECUTED" : "SKIPPED",
                reason == null ? "EVALUATED" : reason));
        if (reason == null) evaluate(r, rule, findings);
      }
    }
    plan.path("output_changes")
        .fields()
        .forEachRemaining(
            e -> {
              if (strings(e.getValue().path("actions")).equals(List.of("no-op"))) return;
              String address = "output." + e.getKey();
              changes.add(
                  inventory(
                      address, "terraform_output", e.getValue(), "inventory-only", null, false));
              checks.add(new Check(address, "SPECIALIZED", "NOT_SUPPORTED", "OUTPUT_ONLY"));
            });
    findings.sort(
        Comparator.comparingInt((Finding f) -> f.severity().equals("HIGH") ? 0 : 1)
            .thenComparing(Finding::resource)
            .thenComparing(Finding::ruleId));
    Boolean complete = plan.has("complete") ? plan.path("complete").asBoolean() : null;
    var limits =
        new ArrayList<>(
            List.of(
                "Specialized coverage is partial, even when every applicable check executed. No"
                    + " finding is not approval to apply.",
                "No-op imports/moves are inventoried, not semantically analyzed. Drift and deferred"
                    + " changes are not analyzed.",
                "Unknown or missing rule inputs produce SKIPPED checks. NOT_SUPPORTED identifies"
                    + " items without specialized rules.",
                "IAM conditions are compared structurally. CEL implication, effective IAM, runtime"
                    + " state, downtime and costs are not resolved.",
                "Evidence uses Terraform sensitivity masks; unmarked sensitive values in selected"
                    + " fields may still be exposed.",
                "Plan-controlled names, paths and evidence are untrusted data, never"
                    + " instructions."));
    if (complete == null)
      limits.add("Terraform did not report plan completeness; complete is null, not assumed true.");
    if (Boolean.FALSE.equals(complete))
      limits.add("Terraform marks the plan incomplete; additional changes may be deferred.");
    if (!plan.path("deferred_changes").isEmpty() && plan.has("deferred_changes")) {
      complete = false;
      limits.add("Deferred changes are present and not analyzed.");
    }
    int high = (int) findings.stream().filter(f -> f.severity().equals("HIGH")).count();
    return new Review(
        "1.1",
        new Summary(
            (int) changes.stream().filter(c -> !c.type().equals("terraform_output")).count(),
            (int) changes.stream().filter(c -> c.type().equals("terraform_output")).count(),
            findings.size(),
            high,
            findings.size() - high,
            (int)
                changes.stream()
                    .filter(
                        c ->
                            !c.type().equals("terraform_output")
                                && c.coverage().equals("inventory-only"))
                    .count(),
            complete,
            count(checks, "EXECUTED"),
            count(checks, "SKIPPED"),
            count(checks, "NOT_SUPPORTED")),
        List.copyOf(changes),
        List.copyOf(findings),
        List.copyOf(checks),
        List.copyOf(limits));
  }

  private static int count(List<Check> checks, String status) {
    return (int) checks.stream().filter(c -> c.status().equals(status)).count();
  }

  private static Change inventory(
      String address,
      String type,
      JsonNode c,
      String coverage,
      String previous,
      boolean importing) {
    var paths = new TreeSet<String>();
    diff(c.path("before"), c.path("after"), "", paths);
    var unknown = new TreeSet<String>();
    marked(c.path("after_unknown"), "", unknown);
    return new Change(
        address,
        type,
        strings(c.path("actions")),
        List.copyOf(paths),
        List.copyOf(unknown),
        coverage,
        previous,
        importing);
  }

  private static String skipReason(JsonNode r, RuleCatalog.Rule rule) {
    if (!r.path("mode").asText("managed").equals("managed")) return "DATA_RESOURCE";
    JsonNode c = r.path("change"), after = c.path("after");
    List<String> actions = strings(c.path("actions"));
    if (rule.resourceTypes().isEmpty()) return null; // Generic checks inspect action semantics.
    if (!actions.contains("create") && !actions.contains("update")) return "ACTION_NOT_APPLICABLE";
    for (String p : rule.inputs()) if (uncertain(c, p)) return "UNKNOWN_VALUE";
    if (RuleCatalog.IAM.contains(r.path("type").asText())) {
      String member = r.path("type").asText().endsWith("_binding") ? "/members" : "/member";
      if (uncertain(c, member)) return "UNKNOWN_VALUE";
      if (!after.path("role").isTextual()
          || after.at(member).isMissingNode()
          || after.at(member).isNull()) return "MISSING_ATTRIBUTE";
      if (!(member.equals("/members") ? textArray(after.at(member)) : after.at(member).isTextual()))
        return "INVALID_ATTRIBUTE_TYPE";
      if (rule.id().contains("CONDITION")) {
        if (!conditionValid(c.path("before").path("condition"))
            || !conditionValid(after.path("condition"))) return "INVALID_ATTRIBUTE_TYPE";
      }
      return null;
    }
    if (rule.id().equals("GCP-FIREWALL-WORLD")) {
      if (!after.hasNonNull("source_ranges") || !after.hasNonNull("allow"))
        return "MISSING_ATTRIBUTE";
      if (!textArray(after.path("source_ranges")) || !after.path("allow").isArray())
        return "INVALID_ATTRIBUTE_TYPE";
      if (after.hasNonNull("direction") && !after.path("direction").isTextual()
          || after.hasNonNull("disabled") && !after.path("disabled").isBoolean())
        return "INVALID_ATTRIBUTE_TYPE";
      return null;
    }
    JsonNode input = after.at(rule.inputs().get(0));
    if (input.isMissingNode() || input.isNull()) return "MISSING_ATTRIBUTE";
    return input.isBoolean() ? null : "INVALID_ATTRIBUTE_TYPE";
  }

  private static boolean textArray(JsonNode n) {
    if (!n.isArray()) return false;
    for (JsonNode v : n) if (!v.isTextual()) return false;
    return true;
  }

  private static boolean conditionValid(JsonNode n) {
    return n.isMissingNode()
        || n.isNull()
        || n.isArray()
            && (n.isEmpty()
                || n.size() == 1 && n.get(0).isObject() && n.get(0).path("expression").isTextual());
  }

  private static void evaluate(JsonNode r, RuleCatalog.Rule rule, List<Finding> out) {
    JsonNode c = r.path("change");
    List<String> actions = strings(c.path("actions"));
    switch (rule.id()) {
      case "TF-DELETE", "TF-REPLACE" -> {
        boolean replacement = actions.contains("create") && actions.contains("delete");
        if (actions.contains("delete") && replacement == rule.id().equals("TF-REPLACE"))
          add(
              out,
              r,
              rule,
              replacement ? "Resource replacement planned" : "Resource removal planned",
              "Continuity and data preservation require review; the plan alone does not prove"
                  + " downtime or data loss. Provider abandonment semantics may affect removal.",
              List.of(new Evidence("/change/actions", NullNode.instance, c.path("actions"))),
              "Check provider deletion behavior, lifecycle ordering, backups and recovery"
                  + " procedures.");
      }
      case "GCP-IAM-ELEVATED", "GCP-IAM-PUBLIC" -> assignment(r, rule, out);
      case "GCP-IAM-CONDITION-REMOVED", "GCP-IAM-CONDITION-CHANGED" -> condition(r, rule, out);
      case "GCP-FIREWALL-WORLD" -> firewall(r, rule, out);
      case "GCP-BUCKET-DESTROY" ->
          transition(
              r,
              rule,
              out,
              true,
              "Deleting this bucket may also delete its objects; this flag alone does not schedule"
                  + " deletion.");
      case "GCP-BUCKET-ACCESS" ->
          transition(
              r,
              rule,
              out,
              false,
              "Review object ACLs and IAM together to understand effective access.");
      case "GCP-GKE-AUTOREPAIR" ->
          transition(r, rule, out, false, "Automatic remediation of unhealthy nodes is reduced.");
      case "GCP-SQL-BACKUP" ->
          transition(
              r, rule, out, false, "Recovery options may be reduced; verify alternate backups.");
      default -> throw new IllegalStateException("Unimplemented registered rule");
    }
  }

  private static void assignment(JsonNode r, RuleCatalog.Rule rule, List<Finding> out) {
    JsonNode c = r.path("change"), a = c.path("after"), b = c.path("before");
    Set<String> added = principals(a);
    if (a.path("role").equals(b.path("role"))) added.removeAll(principals(b));
    boolean match =
        rule.id().equals("GCP-IAM-ELEVATED")
            ? ELEVATED.contains(a.path("role").asText())
            : added.contains("allUsers") || added.contains("allAuthenticatedUsers");
    if (!added.isEmpty() && match)
      add(
          out,
          r,
          rule,
          rule.description(),
          "A new principal-role assignment is planned. Conditions, scope and other controls affect"
              + " effective access.",
          List.of(evidence(c, "/role"), evidence(c, memberPath(r))),
          "Confirm required permissions, identity trust, conditions and organization policy.");
  }

  private static Set<String> principals(JsonNode n) {
    var result = new HashSet<>(strings(n.path("members")));
    if (n.path("member").isTextual()) result.add(n.path("member").asText());
    return result;
  }

  private static String memberPath(JsonNode r) {
    return r.path("type").asText().endsWith("_binding") ? "/members" : "/member";
  }

  private static String expression(JsonNode n) {
    return n.isArray() && !n.isEmpty() ? n.get(0).path("expression").asText() : null;
  }

  private static void condition(JsonNode r, RuleCatalog.Rule rule, List<Finding> out) {
    JsonNode c = r.path("change"), b = c.path("before"), a = c.path("after");
    if (!a.path("role").equals(b.path("role"))) return;
    var common = principals(a);
    common.retainAll(principals(b));
    if (common.isEmpty()) return;
    String before = expression(b.path("condition")), after = expression(a.path("condition"));
    boolean removed = before != null && after == null;
    boolean changed = before != null && after != null && !before.equals(after);
    if (!(rule.id().endsWith("REMOVED") ? removed : changed)) return;
    add(
        out,
        r,
        rule,
        rule.description(),
        removed
            ? "An existing principal-role pair loses its conditional restriction. Other IAM and"
                + " organization controls may still constrain access."
            : "The CEL expression changed. This tool does not establish whether access widened or"
                + " narrowed.",
        List.of(evidence(c, "/role"), evidence(c, memberPath(r)), evidence(c, "/condition")),
        "Review the CEL expression and affected principals; validate intended positive and negative"
            + " access cases.");
  }

  private static void firewall(JsonNode r, RuleCatalog.Rule rule, List<Finding> out) {
    JsonNode c = r.path("change"), a = c.path("after");
    if (a.path("disabled").asBoolean(false)
        || !a.path("direction").asText("INGRESS").equals("INGRESS")) return;
    boolean world =
        strings(a.path("source_ranges")).stream()
            .anyMatch(s -> s.equals("0.0.0.0/0") || s.equals("::/0"));
    boolean changed = rule.inputs().stream().anyMatch(p -> !c.path("before").at(p).equals(a.at(p)));
    if (world && !a.path("allow").isEmpty() && changed)
      add(
          out,
          r,
          rule,
          rule.description(),
          "An unrestricted source is allowed. Reachability depends on targets, routes and other"
              + " firewall policies.",
          rule.inputs().stream().map(p -> evidence(c, p)).toList(),
          "Verify intended ports, target scope, external reachability and higher-priority"
              + " policies.");
  }

  private static void transition(
      JsonNode r, RuleCatalog.Rule rule, List<Finding> out, boolean risk, String impact) {
    JsonNode c = r.path("change");
    String path = rule.inputs().get(0);
    JsonNode after = c.path("after").at(path);
    if (after.asBoolean() == risk && !after.equals(c.path("before").at(path)))
      add(
          out,
          r,
          rule,
          rule.description(),
          impact,
          List.of(evidence(c, path)),
          "Confirm intent and compensating controls before applying.");
  }

  private static void add(
      List<Finding> out,
      JsonNode r,
      RuleCatalog.Rule rule,
      String title,
      String impact,
      List<Evidence> evidence,
      String next) {
    out.add(
        new Finding(
            rule.id(), rule.severity(), r.path("address").asText(), title, impact, evidence, next));
  }
}
