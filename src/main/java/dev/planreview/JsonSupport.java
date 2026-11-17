package dev.planreview;

import static dev.planreview.Review.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.*;

final class JsonSupport {
  static Evidence evidence(JsonNode c, String path) {
    return new Evidence(
        path,
        safe(c.path("before"), c.path("before_sensitive"), path),
        safe(c.path("after"), c.path("after_sensitive"), path));
  }

  static JsonNode safe(JsonNode data, JsonNode mask, String path) {
    String p = path;
    while (true) {
      if (mask.at(p).isBoolean() && mask.at(p).asBoolean()) return TextNode.valueOf("[REDACTED]");
      if (p.isEmpty()) break;
      p = p.substring(0, p.lastIndexOf('/'));
    }
    return redact(data.at(path), mask.at(path));
  }

  static JsonNode redact(JsonNode value, JsonNode mask) {
    if (mask.isBoolean() && mask.asBoolean()) return TextNode.valueOf("[REDACTED]");
    if (value.isMissingNode()) return NullNode.instance;
    if (value.isObject()) {
      var copy = PlanReader.JSON.createObjectNode();
      value
          .fields()
          .forEachRemaining(e -> copy.set(e.getKey(), redact(e.getValue(), mask.path(e.getKey()))));
      return copy;
    }
    if (value.isArray()) {
      var copy = PlanReader.JSON.createArrayNode();
      for (int i = 0; i < value.size(); i++) copy.add(redact(value.get(i), mask.path(i)));
      return copy;
    }
    return value;
  }

  static boolean uncertain(JsonNode c, String path) {
    JsonNode mask = c.path("after_unknown");
    String p = path;
    while (true) {
      if (mask.at(p).isBoolean() && mask.at(p).asBoolean()) return true;
      if (p.isEmpty()) break;
      p = p.substring(0, p.lastIndexOf('/'));
    }
    var marks = new TreeSet<String>();
    marked(mask.at(path), "", marks);
    return !marks.isEmpty();
  }

  static String pointer(String key) {
    return key.replace("~", "~0").replace("/", "~1");
  }

  static void diff(JsonNode before, JsonNode after, String path, Set<String> out) {
    if (before.equals(after)) return;
    if (before.isObject() || after.isObject()) {
      Set<String> keys = new TreeSet<>();
      before.fieldNames().forEachRemaining(keys::add);
      after.fieldNames().forEachRemaining(keys::add);
      keys.forEach(k -> diff(before.path(k), after.path(k), path + "/" + pointer(k), out));
    } else out.add(path.isEmpty() ? "/" : path);
  }

  static void marked(JsonNode n, String path, Set<String> out) {
    if (n.isBoolean() && n.asBoolean()) out.add(path.isEmpty() ? "/" : path);
    else if (n.isObject())
      n.fields().forEachRemaining(e -> marked(e.getValue(), path + "/" + pointer(e.getKey()), out));
    else if (n.isArray()) for (int i = 0; i < n.size(); i++) marked(n.get(i), path + "/" + i, out);
  }

  static List<String> strings(JsonNode n) {
    if (!n.isArray()) return List.of();
    var result = new ArrayList<String>();
    n.forEach(v -> result.add(v.asText()));
    return result;
  }
}
