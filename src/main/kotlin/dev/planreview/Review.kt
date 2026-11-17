package dev.planreview

import com.fasterxml.jackson.databind.JsonNode

@JvmRecord
data class Review(
    val schemaVersion: String,
    val summary: Summary,
    val changes: List<Change>,
    val findings: List<Finding>,
    val checks: List<Check>,
    val limitations: List<String>,
) {
    @JvmRecord
    data class Summary(
        val changedResources: Int,
        val changedOutputs: Int,
        val findings: Int,
        val high: Int,
        val medium: Int,
        val resourcesWithoutSpecializedChecks: Int,
        val complete: Boolean?,
        val checksExecuted: Int,
        val checksSkipped: Int,
        val itemsWithoutSpecializedChecks: Int,
    )

    @JvmRecord
    data class Change(
        val address: String,
        val type: String,
        val actions: List<String>,
        val changedPaths: List<String>,
        val unknownPaths: List<String>,
        val coverage: String,
        val previousAddress: String?,
        val importing: Boolean,
    )

    @JvmRecord
    data class Check(val resource: String, val ruleId: String, val status: String, val reasonCode: String)
    @JvmRecord
    data class Evidence(val path: String, val before: JsonNode, val after: JsonNode)
    @JvmRecord
    data class Finding(
        val ruleId: String,
        val severity: String,
        val resource: String,
        val title: String,
        val impact: String,
        val evidence: List<Evidence>,
        val nextCheck: String,
    )
}
