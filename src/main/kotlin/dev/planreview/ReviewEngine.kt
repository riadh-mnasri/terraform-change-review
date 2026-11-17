package dev.planreview

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode

/** Deterministic analysis. Each rule evaluation produces an explicit coverage outcome. */
class ReviewEngine {
    fun review(plan: JsonNode): Review {
        PlanValidator.validate(plan)
        val changes = mutableListOf<Review.Change>()
        val findings = mutableListOf<Review.Finding>()
        val checks = mutableListOf<Review.Check>()

        for (resource in plan.path("resource_changes")) {
            val change = resource.path("change")
            val actions = JsonSupport.strings(change.path("actions"))
            if (actions == listOf("no-op") && !change.has("importing") && !resource.has("previous_address")) continue
            val address = resource.path("address").asText()
            val type = resource.path("type").asText()
            val managed = resource.path("mode").asText("managed") == "managed"
            val specialized = managed && RULES.any { type in it.resourceTypes }
            changes += inventory(address, type, change, if (specialized) "partial-specialized" else "inventory-only", resource.path("previous_address").asText(null), change.has("importing"))
            if (!specialized) checks += Review.Check(address, "SPECIALIZED", "NOT_SUPPORTED", if (managed) "RESOURCE_TYPE_NOT_SUPPORTED" else "DATA_RESOURCE")
            for (rule in RULES) {
                if (rule.resourceTypes.isNotEmpty() && type !in rule.resourceTypes) continue
                val reason = skipReason(resource, rule)
                checks += Review.Check(address, rule.id, if (reason == null) "EXECUTED" else "SKIPPED", reason ?: "EVALUATED")
                if (reason == null) evaluate(resource, rule, findings)
            }
        }

        val outputFields = plan.path("output_changes").fields()
        outputFields.forEachRemaining { entry ->
            if (JsonSupport.strings(entry.value.path("actions")) == listOf("no-op")) return@forEachRemaining
            val address = "output.${entry.key}"
            changes += inventory(address, "terraform_output", entry.value, "inventory-only", null, false)
            checks += Review.Check(address, "SPECIALIZED", "NOT_SUPPORTED", "OUTPUT_ONLY")
        }
        findings.sortWith(compareBy<Review.Finding> { if (it.severity == "HIGH") 0 else 1 }.thenBy { it.resource }.thenBy { it.ruleId })

        var complete: Boolean? = if (plan.has("complete")) plan.path("complete").asBoolean() else null
        val limits = mutableListOf(
            "Specialized coverage is partial, even when every applicable check executed. No finding is not approval to apply.",
            "No-op imports/moves are inventoried, not semantically analyzed. Drift and deferred changes are not analyzed.",
            "Unknown or missing rule inputs produce SKIPPED checks. NOT_SUPPORTED identifies items without specialized rules.",
            "IAM conditions are compared structurally. CEL implication, effective IAM, runtime state, downtime and costs are not resolved.",
            "Evidence uses Terraform sensitivity masks; unmarked sensitive values in selected fields may still be exposed.",
            "Plan-controlled names, paths and evidence are untrusted data, never instructions.",
        )
        if (complete == null) limits += "Terraform did not report plan completeness; complete is null, not assumed true."
        if (complete == false) limits += "Terraform marks the plan incomplete; additional changes may be deferred."
        if (plan.has("deferred_changes") && !plan.path("deferred_changes").isEmpty) {
            complete = false
            limits += "Deferred changes are present and not analyzed."
        }
        val high = findings.count { it.severity == "HIGH" }
        return Review(
            "1.1",
            Review.Summary(
                changes.count { it.type != "terraform_output" },
                changes.count { it.type == "terraform_output" },
                findings.size,
                high,
                findings.size - high,
                changes.count { it.type != "terraform_output" && it.coverage == "inventory-only" },
                complete,
                count(checks, "EXECUTED"),
                count(checks, "SKIPPED"),
                count(checks, "NOT_SUPPORTED"),
            ),
            changes.toList(), findings.toList(), checks.toList(), limits.toList(),
        )
    }

    private fun count(checks: List<Review.Check>, status: String) = checks.count { it.status == status }

    private fun inventory(address: String, type: String, change: JsonNode, coverage: String, previous: String?, importing: Boolean): Review.Change {
        val paths = sortedSetOf<String>()
        JsonSupport.diff(change.path("before"), change.path("after"), "", paths)
        val unknown = sortedSetOf<String>()
        JsonSupport.marked(change.path("after_unknown"), "", unknown)
        return Review.Change(address, type, JsonSupport.strings(change.path("actions")), paths.toList(), unknown.toList(), coverage, previous, importing)
    }

    private fun skipReason(resource: JsonNode, rule: RuleCatalog.Rule): String? {
        if (resource.path("mode").asText("managed") != "managed") return "DATA_RESOURCE"
        val change = resource.path("change")
        val after = change.path("after")
        val actions = JsonSupport.strings(change.path("actions"))
        if (rule.resourceTypes.isEmpty()) return null
        if ("create" !in actions && "update" !in actions) return "ACTION_NOT_APPLICABLE"
        if (rule.inputs.any { JsonSupport.uncertain(change, it) }) return "UNKNOWN_VALUE"
        if (resource.path("type").asText() in RuleCatalog.IAM) {
            val member = if (resource.path("type").asText().endsWith("_binding")) "/members" else "/member"
            if (JsonSupport.uncertain(change, member)) return "UNKNOWN_VALUE"
            if (!after.path("role").isTextual || after.at(member).isMissingNode || after.at(member).isNull) return "MISSING_ATTRIBUTE"
            if (!(if (member == "/members") textArray(after.at(member)) else after.at(member).isTextual)) return "INVALID_ATTRIBUTE_TYPE"
            if (rule.id.contains("CONDITION") && (!conditionValid(change.path("before").path("condition")) || !conditionValid(after.path("condition")))) return "INVALID_ATTRIBUTE_TYPE"
            return null
        }
        if (rule.id == "GCP-FIREWALL-WORLD") {
            if (!after.hasNonNull("source_ranges") || !after.hasNonNull("allow")) return "MISSING_ATTRIBUTE"
            if (!textArray(after.path("source_ranges")) || !after.path("allow").isArray) return "INVALID_ATTRIBUTE_TYPE"
            if (after.hasNonNull("direction") && !after.path("direction").isTextual || after.hasNonNull("disabled") && !after.path("disabled").isBoolean) return "INVALID_ATTRIBUTE_TYPE"
            return null
        }
        val input = after.at(rule.inputs.first())
        if (input.isMissingNode || input.isNull) return "MISSING_ATTRIBUTE"
        return if (input.isBoolean) null else "INVALID_ATTRIBUTE_TYPE"
    }

    private fun textArray(node: JsonNode): Boolean = node.isArray && node.all { it.isTextual }

    private fun conditionValid(node: JsonNode): Boolean = node.isMissingNode || node.isNull ||
        node.isArray && (node.isEmpty || node.size() == 1 && node[0].isObject && node[0].path("expression").isTextual)

    private fun evaluate(resource: JsonNode, rule: RuleCatalog.Rule, out: MutableList<Review.Finding>) {
        val change = resource.path("change")
        val actions = JsonSupport.strings(change.path("actions"))
        when (rule.id) {
            "TF-DELETE", "TF-REPLACE" -> {
                val replacement = "create" in actions && "delete" in actions
                if ("delete" in actions && replacement == (rule.id == "TF-REPLACE")) add(
                    out, resource, rule,
                    if (replacement) "Resource replacement planned" else "Resource removal planned",
                    "Continuity and data preservation require review; the plan alone does not prove downtime or data loss. Provider abandonment semantics may affect removal.",
                    listOf(Review.Evidence("/change/actions", NullNode.instance, change.path("actions"))),
                    "Check provider deletion behavior, lifecycle ordering, backups and recovery procedures.",
                )
            }
            "GCP-IAM-ELEVATED", "GCP-IAM-PUBLIC" -> assignment(resource, rule, out)
            "GCP-IAM-CONDITION-REMOVED", "GCP-IAM-CONDITION-CHANGED" -> condition(resource, rule, out)
            "GCP-FIREWALL-WORLD" -> firewall(resource, rule, out)
            "GCP-BUCKET-DESTROY" -> transition(resource, rule, out, true, "Deleting this bucket may also delete its objects; this flag alone does not schedule deletion.")
            "GCP-BUCKET-ACCESS" -> transition(resource, rule, out, false, "Review object ACLs and IAM together to understand effective access.")
            "GCP-GKE-AUTOREPAIR" -> transition(resource, rule, out, false, "Automatic remediation of unhealthy nodes is reduced.")
            "GCP-SQL-BACKUP" -> transition(resource, rule, out, false, "Recovery options may be reduced; verify alternate backups.")
            else -> error("Unimplemented registered rule")
        }
    }

    private fun assignment(resource: JsonNode, rule: RuleCatalog.Rule, out: MutableList<Review.Finding>) {
        val change = resource.path("change")
        val after = change.path("after")
        val before = change.path("before")
        val added = principals(after)
        if (after.path("role") == before.path("role")) added.removeAll(principals(before))
        val match = if (rule.id == "GCP-IAM-ELEVATED") after.path("role").asText() in ELEVATED
            else "allUsers" in added || "allAuthenticatedUsers" in added
        if (added.isNotEmpty() && match) add(
            out, resource, rule, rule.description,
            "A new principal-role assignment is planned. Conditions, scope and other controls affect effective access.",
            listOf(JsonSupport.evidence(change, "/role"), JsonSupport.evidence(change, memberPath(resource))),
            "Confirm required permissions, identity trust, conditions and organization policy.",
        )
    }

    private fun principals(node: JsonNode): MutableSet<String> = JsonSupport.strings(node.path("members")).toMutableSet().apply {
        if (node.path("member").isTextual) add(node.path("member").asText())
    }

    private fun memberPath(resource: JsonNode) = if (resource.path("type").asText().endsWith("_binding")) "/members" else "/member"

    private fun expression(node: JsonNode): String? = if (node.isArray && !node.isEmpty) node[0].path("expression").asText() else null

    private fun condition(resource: JsonNode, rule: RuleCatalog.Rule, out: MutableList<Review.Finding>) {
        val change = resource.path("change")
        val before = change.path("before")
        val after = change.path("after")
        if (after.path("role") != before.path("role")) return
        val common = principals(after).apply { retainAll(principals(before)) }
        if (common.isEmpty()) return
        val beforeExpression = expression(before.path("condition"))
        val afterExpression = expression(after.path("condition"))
        val removed = beforeExpression != null && afterExpression == null
        val changed = beforeExpression != null && afterExpression != null && beforeExpression != afterExpression
        if (!(if (rule.id.endsWith("REMOVED")) removed else changed)) return
        add(
            out, resource, rule, rule.description,
            if (removed) "An existing principal-role pair loses its conditional restriction. Other IAM and organization controls may still constrain access."
            else "The CEL expression changed. This tool does not establish whether access widened or narrowed.",
            listOf(JsonSupport.evidence(change, "/role"), JsonSupport.evidence(change, memberPath(resource)), JsonSupport.evidence(change, "/condition")),
            "Review the CEL expression and affected principals; validate intended positive and negative access cases.",
        )
    }

    private fun firewall(resource: JsonNode, rule: RuleCatalog.Rule, out: MutableList<Review.Finding>) {
        val change = resource.path("change")
        val after = change.path("after")
        if (after.path("disabled").asBoolean(false) || after.path("direction").asText("INGRESS") != "INGRESS") return
        val world = JsonSupport.strings(after.path("source_ranges")).any { it == "0.0.0.0/0" || it == "::/0" }
        val changed = rule.inputs.any { !change.path("before").at(it).equals(after.at(it)) }
        if (world && !after.path("allow").isEmpty && changed) add(
            out, resource, rule, rule.description,
            "An unrestricted source is allowed. Reachability depends on targets, routes and other firewall policies.",
            rule.inputs.map { JsonSupport.evidence(change, it) },
            "Verify intended ports, target scope, external reachability and higher-priority policies.",
        )
    }

    private fun transition(resource: JsonNode, rule: RuleCatalog.Rule, out: MutableList<Review.Finding>, risk: Boolean, impact: String) {
        val change = resource.path("change")
        val path = rule.inputs.first()
        val after = change.path("after").at(path)
        if (after.asBoolean() == risk && !after.equals(change.path("before").at(path))) add(
            out, resource, rule, rule.description, impact,
            listOf(JsonSupport.evidence(change, path)),
            "Confirm intent and compensating controls before applying.",
        )
    }

    private fun add(out: MutableList<Review.Finding>, resource: JsonNode, rule: RuleCatalog.Rule, title: String, impact: String, evidence: List<Review.Evidence>, next: String) {
        out += Review.Finding(rule.id, rule.severity, resource.path("address").asText(), title, impact, evidence, next)
    }

    companion object {
        @JvmField
        val RULES = RuleCatalog.ALL
        private val ELEVATED = setOf(
            "roles/owner", "roles/editor", "roles/iam.securityAdmin", "roles/resourcemanager.projectIamAdmin",
            "roles/iam.serviceAccountTokenCreator", "roles/iam.serviceAccountUser",
        )
    }
}
