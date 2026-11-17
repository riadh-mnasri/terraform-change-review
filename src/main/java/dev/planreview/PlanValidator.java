package dev.planreview;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validate the plan envelope without assuming provider-specific schemas. Unknown minor fields are
 * allowed.
 */
final class PlanValidator {
  private static final Set<List<String>> ACTIONS =
      Set.of(
          List.of("no-op"),
          List.of("create"),
          List.of("read"),
          List.of("update"),
          List.of("delete"),
          List.of("delete", "create"),
          List.of("create", "delete"),
          List.of("forget"),
          List.of("create", "forget"));

  static final class InvalidPlan extends IllegalArgumentException {
    private final String code;

    InvalidPlan(String code) {
      super(code);
      this.code = code;
    }

    String code() {
      return code;
    }
  }

  static void validate(JsonNode p) {
    require(p != null && p.isObject(), "INVALID_PLAN_OBJECT");
    require(
        p.path("format_version").isTextual()
            && p.path("format_version").asText().matches("1\\.\\d+"),
        "UNSUPPORTED_FORMAT_VERSION");
    require(
        p.path("resource_changes").isArray() || p.path("planned_values").isObject(), "NOT_A_PLAN");
    optional(p, "resource_changes", "array");
    optional(p, "output_changes", "object");
    optional(p, "planned_values", "object");
    optional(p, "complete", "boolean");
    optional(p, "errored", "boolean");
    optional(p, "deferred_changes", "array");
    require(!p.path("errored").asBoolean(false), "ERRORED_PLAN");
    var addresses = new HashSet<List<String>>();
    for (JsonNode r : p.path("resource_changes")) {
      require(
          r.isObject() && text(r.path("address")) && text(r.path("type")),
          "INVALID_RESOURCE_IDENTITY");
      require(
          addresses.add(List.of(r.path("address").asText(), r.path("deposed").asText("current"))),
          "DUPLICATE_RESOURCE_ADDRESS");
      require(
          !r.has("mode") || Set.of("managed", "data").contains(r.path("mode").asText()),
          "INVALID_RESOURCE_MODE");
      require(
          !r.has("previous_address") || text(r.path("previous_address")),
          "INVALID_PREVIOUS_ADDRESS");
      change(r.path("change"), true);
    }
    p.path("output_changes").elements().forEachRemaining(c -> change(c, false));
  }

  private static void change(JsonNode c, boolean resource) {
    require(c.isObject() && c.path("actions").isArray(), "INVALID_CHANGE_ACTIONS");
    for (JsonNode a : c.path("actions")) require(a.isTextual(), "INVALID_CHANGE_ACTIONS");
    List<String> actions = JsonSupport.strings(c.path("actions"));
    require(ACTIONS.contains(actions), "UNSUPPORTED_CHANGE_ACTIONS");
    for (String key : List.of("before_sensitive", "after_sensitive", "after_unknown"))
      if (c.has(key)) mask(c.get(key));
    // Terraform omits an output's value when the entire value is unknown.
    require(c.has("before"), "MISSING_BEFORE");
    require(c.has("after") || c.path("after_unknown").asBoolean(false), "MISSING_AFTER");
    if (!resource) return;
    require(c.path("before").isNull() || c.path("before").isObject(), "INVALID_RESOURCE_BEFORE");
    require(
        c.path("after").isNull()
            || c.path("after").isObject()
            || c.path("after").isMissingNode() && c.path("after_unknown").asBoolean(false),
        "INVALID_RESOURCE_AFTER");
    if (actions.equals(List.of("create"))) require(c.path("before").isNull(), "CREATE_HAS_BEFORE");
    if (actions.contains("update") || actions.contains("delete") || actions.contains("forget"))
      require(c.path("before").isObject(), "MISSING_EXISTING_RESOURCE");
    if (actions.equals(List.of("delete")) || actions.equals(List.of("forget")))
      require(c.path("after").isNull(), "REMOVE_HAS_AFTER");
    if (actions.contains("create") || actions.contains("update") || actions.contains("read"))
      require(
          c.path("after").isObject() || c.path("after_unknown").asBoolean(false),
          "MISSING_PLANNED_RESOURCE");
  }

  private static void mask(JsonNode n) {
    if (n.isBoolean()) return;
    require(n.isObject() || n.isArray(), "INVALID_VALUE_MASK");
    n.elements().forEachRemaining(PlanValidator::mask);
  }

  private static void optional(JsonNode p, String key, String type) {
    if (!p.has(key)) return;
    JsonNode n = p.get(key);
    require(
        switch (type) {
          case "array" -> n.isArray();
          case "object" -> n.isObject();
          default -> n.isBoolean();
        },
        "INVALID_PLAN_FIELD_TYPE");
  }

  private static boolean text(JsonNode n) {
    return n.isTextual() && !n.asText().isBlank();
  }

  private static void require(boolean valid, String code) {
    if (!valid) throw new InvalidPlan(code);
  }
}
