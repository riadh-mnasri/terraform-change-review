package dev.planreview

import com.fasterxml.jackson.databind.JsonNode

/** Validates the plan envelope without assuming provider-specific schemas. */
object PlanValidator {
    private val ACTIONS = setOf(
        listOf("no-op"), listOf("create"), listOf("read"), listOf("update"), listOf("delete"),
        listOf("delete", "create"), listOf("create", "delete"), listOf("forget"), listOf("create", "forget"),
    )

    class InvalidPlan(private val reasonCode: String) : IllegalArgumentException(reasonCode) {
        fun code(): String = reasonCode
    }

    @JvmStatic
    fun validate(plan: JsonNode?) {
        ensure(plan != null && plan.isObject, "INVALID_PLAN_OBJECT")
        val p = plan!!
        ensure(p.path("format_version").isTextual && p.path("format_version").asText().matches(Regex("1\\.\\d+")), "UNSUPPORTED_FORMAT_VERSION")
        ensure(p.path("resource_changes").isArray || p.path("planned_values").isObject, "NOT_A_PLAN")
        optional(p, "resource_changes", "array")
        optional(p, "output_changes", "object")
        optional(p, "planned_values", "object")
        optional(p, "complete", "boolean")
        optional(p, "errored", "boolean")
        optional(p, "deferred_changes", "array")
        ensure(!p.path("errored").asBoolean(false), "ERRORED_PLAN")
        val addresses = HashSet<List<String>>()
        for (resource in p.path("resource_changes")) {
            ensure(resource.isObject && text(resource.path("address")) && text(resource.path("type")), "INVALID_RESOURCE_IDENTITY")
            ensure(addresses.add(listOf(resource.path("address").asText(), resource.path("deposed").asText("current"))), "DUPLICATE_RESOURCE_ADDRESS")
            ensure(!resource.has("mode") || resource.path("mode").asText() in setOf("managed", "data"), "INVALID_RESOURCE_MODE")
            ensure(!resource.has("previous_address") || text(resource.path("previous_address")), "INVALID_PREVIOUS_ADDRESS")
            change(resource.path("change"), true)
        }
        p.path("output_changes").elements().forEachRemaining { change(it, false) }
    }

    private fun change(c: JsonNode, resource: Boolean) {
        ensure(c.isObject && c.path("actions").isArray, "INVALID_CHANGE_ACTIONS")
        for (action in c.path("actions")) ensure(action.isTextual, "INVALID_CHANGE_ACTIONS")
        val actions = JsonSupport.strings(c.path("actions"))
        ensure(actions in ACTIONS, "UNSUPPORTED_CHANGE_ACTIONS")
        for (key in listOf("before_sensitive", "after_sensitive", "after_unknown")) if (c.has(key)) mask(c.get(key))
        ensure(c.has("before"), "MISSING_BEFORE")
        ensure(c.has("after") || c.path("after_unknown").asBoolean(false), "MISSING_AFTER")
        if (!resource) return
        ensure(c.path("before").isNull || c.path("before").isObject, "INVALID_RESOURCE_BEFORE")
        ensure(c.path("after").isNull || c.path("after").isObject || c.path("after").isMissingNode && c.path("after_unknown").asBoolean(false), "INVALID_RESOURCE_AFTER")
        if (actions == listOf("create")) ensure(c.path("before").isNull, "CREATE_HAS_BEFORE")
        if (actions.any { it in listOf("update", "delete", "forget") }) ensure(c.path("before").isObject, "MISSING_EXISTING_RESOURCE")
        if (actions == listOf("delete") || actions == listOf("forget")) ensure(c.path("after").isNull, "REMOVE_HAS_AFTER")
        if (actions.any { it in listOf("create", "update", "read") }) ensure(c.path("after").isObject || c.path("after_unknown").asBoolean(false), "MISSING_PLANNED_RESOURCE")
    }

    private fun mask(node: JsonNode) {
        if (node.isBoolean) return
        ensure(node.isObject || node.isArray, "INVALID_VALUE_MASK")
        node.elements().forEachRemaining(::mask)
    }

    private fun optional(plan: JsonNode, key: String, type: String) {
        if (!plan.has(key)) return
        val node = plan.get(key)
        val valid = when (type) {
            "array" -> node.isArray
            "object" -> node.isObject
            else -> node.isBoolean
        }
        ensure(valid, "INVALID_PLAN_FIELD_TYPE")
    }

    private fun text(node: JsonNode) = node.isTextual && node.asText().isNotBlank()

    private fun ensure(valid: Boolean, code: String) {
        if (!valid) throw InvalidPlan(code)
    }
}
