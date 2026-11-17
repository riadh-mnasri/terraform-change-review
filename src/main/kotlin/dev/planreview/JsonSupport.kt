package dev.planreview

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.util.TreeSet

object JsonSupport {
    @JvmStatic
    fun evidence(change: JsonNode, path: String) = Review.Evidence(
        path,
        safe(change.path("before"), change.path("before_sensitive"), path),
        safe(change.path("after"), change.path("after_sensitive"), path),
    )

    @JvmStatic
    fun safe(data: JsonNode, mask: JsonNode, path: String): JsonNode {
        var parent = path
        while (true) {
            if (mask.at(parent).isBoolean && mask.at(parent).asBoolean()) return TextNode.valueOf("[REDACTED]")
            if (parent.isEmpty()) break
            parent = parent.substring(0, parent.lastIndexOf('/'))
        }
        return redact(data.at(path), mask.at(path))
    }

    @JvmStatic
    fun redact(value: JsonNode, mask: JsonNode): JsonNode {
        if (mask.isBoolean && mask.asBoolean()) return TextNode.valueOf("[REDACTED]")
        if (value.isMissingNode) return NullNode.instance
        if (value.isObject) {
            val copy = PlanReader.JSON.createObjectNode()
            value.fields().forEachRemaining { field -> copy.set<JsonNode>(field.key, redact(field.value, mask.path(field.key))) }
            return copy
        }
        if (value.isArray) {
            val copy = PlanReader.JSON.createArrayNode()
            for (i in 0 until value.size()) copy.add(redact(value.get(i), mask.path(i)))
            return copy
        }
        return value
    }

    @JvmStatic
    fun uncertain(change: JsonNode, path: String): Boolean {
        val mask = change.path("after_unknown")
        var parent = path
        while (true) {
            if (mask.at(parent).isBoolean && mask.at(parent).asBoolean()) return true
            if (parent.isEmpty()) break
            parent = parent.substring(0, parent.lastIndexOf('/'))
        }
        val marks = TreeSet<String>()
        marked(mask.at(path), "", marks)
        return marks.isNotEmpty()
    }

    @JvmStatic fun pointer(key: String) = key.replace("~", "~0").replace("/", "~1")

    @JvmStatic
    fun diff(before: JsonNode, after: JsonNode, path: String, out: MutableSet<String>) {
        if (before == after) return
        if (before.isObject || after.isObject) {
            val keys = TreeSet<String>()
            before.fieldNames().forEachRemaining(keys::add)
            after.fieldNames().forEachRemaining(keys::add)
            keys.forEach { diff(before.path(it), after.path(it), "$path/${pointer(it)}", out) }
        } else out.add(if (path.isEmpty()) "/" else path)
    }

    @JvmStatic
    fun marked(node: JsonNode, path: String, out: MutableSet<String>) {
        if (node.isBoolean && node.asBoolean()) out.add(if (path.isEmpty()) "/" else path)
        else if (node.isObject) node.fields().forEachRemaining { marked(it.value, "$path/${pointer(it.key)}", out) }
        else if (node.isArray) for (i in 0 until node.size()) marked(node.get(i), "$path/$i", out)
    }

    @JvmStatic
    fun strings(node: JsonNode): List<String> = if (!node.isArray) emptyList() else
        (0 until node.size()).map { node.get(it).asText() }
}
