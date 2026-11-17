package dev.planreview

/** Versioned rule metadata shared by discovery, evaluation and coverage reporting. */
object RuleCatalog {
    @JvmField
    val IAM = setOf(
        "google_project_iam_member", "google_project_iam_binding",
        "google_storage_bucket_iam_member", "google_storage_bucket_iam_binding",
    )

    @JvmRecord
    data class Rule(
        val id: String,
        val severity: String,
        val description: String,
        val resourceTypes: Set<String>,
        val inputs: List<String>,
    )

    @JvmField
    val ALL = listOf(
        Rule("TF-DELETE", "HIGH", "Managed resource deletion requires manual impact review", emptySet(), emptyList()),
        Rule("TF-REPLACE", "HIGH", "Managed resource replacement requires continuity review", emptySet(), emptyList()),
        Rule("GCP-IAM-ELEVATED", "HIGH", "New assignment of a curated sensitive role", IAM, listOf("/role")),
        Rule("GCP-IAM-PUBLIC", "HIGH", "New assignment to a public principal", IAM, listOf("/role")),
        Rule("GCP-IAM-CONDITION-REMOVED", "HIGH", "IAM condition removed for an existing principal-role pair", IAM, listOf("/role", "/condition")),
        Rule("GCP-IAM-CONDITION-CHANGED", "MEDIUM", "IAM condition expression changed; semantic impact unresolved", IAM, listOf("/role", "/condition")),
        Rule("GCP-FIREWALL-WORLD", "HIGH", "Changed active ingress allow rule has a world source range", setOf("google_compute_firewall"), listOf("/source_ranges", "/allow", "/direction", "/disabled", "/target_tags", "/target_service_accounts", "/priority")),
        Rule("GCP-BUCKET-DESTROY", "HIGH", "Bucket force_destroy enabled", setOf("google_storage_bucket"), listOf("/force_destroy")),
        Rule("GCP-BUCKET-ACCESS", "MEDIUM", "Uniform bucket-level access disabled", setOf("google_storage_bucket"), listOf("/uniform_bucket_level_access")),
        Rule("GCP-GKE-AUTOREPAIR", "MEDIUM", "Node auto-repair disabled", setOf("google_container_node_pool"), listOf("/management/0/auto_repair")),
        Rule("GCP-SQL-BACKUP", "HIGH", "Automated database backups disabled", setOf("google_sql_database_instance"), listOf("/settings/0/backup_configuration/0/enabled")),
    )
}
