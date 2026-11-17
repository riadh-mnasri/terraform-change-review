package dev.planreview;

import java.util.List;
import java.util.Set;

/** Versioned rule metadata is shared by discovery, evaluation and coverage reporting. */
final class RuleCatalog {
  static final Set<String> IAM =
      Set.of(
          "google_project_iam_member",
          "google_project_iam_binding",
          "google_storage_bucket_iam_member",
          "google_storage_bucket_iam_binding");

  record Rule(
      String id,
      String severity,
      String description,
      Set<String> resourceTypes,
      List<String> inputs) {}

  static final List<Rule> ALL =
      List.of(
          new Rule(
              "TF-DELETE",
              "HIGH",
              "Managed resource deletion requires manual impact review",
              Set.of(),
              List.of()),
          new Rule(
              "TF-REPLACE",
              "HIGH",
              "Managed resource replacement requires continuity review",
              Set.of(),
              List.of()),
          new Rule(
              "GCP-IAM-ELEVATED",
              "HIGH",
              "New assignment of a curated sensitive role",
              IAM,
              List.of("/role")),
          new Rule(
              "GCP-IAM-PUBLIC",
              "HIGH",
              "New assignment to a public principal",
              IAM,
              List.of("/role")),
          new Rule(
              "GCP-IAM-CONDITION-REMOVED",
              "HIGH",
              "IAM condition removed for an existing principal-role pair",
              IAM,
              List.of("/role", "/condition")),
          new Rule(
              "GCP-IAM-CONDITION-CHANGED",
              "MEDIUM",
              "IAM condition expression changed; semantic impact unresolved",
              IAM,
              List.of("/role", "/condition")),
          new Rule(
              "GCP-FIREWALL-WORLD",
              "HIGH",
              "Changed active ingress allow rule has a world source range",
              Set.of("google_compute_firewall"),
              List.of(
                  "/source_ranges",
                  "/allow",
                  "/direction",
                  "/disabled",
                  "/target_tags",
                  "/target_service_accounts",
                  "/priority")),
          new Rule(
              "GCP-BUCKET-DESTROY",
              "HIGH",
              "Bucket force_destroy enabled",
              Set.of("google_storage_bucket"),
              List.of("/force_destroy")),
          new Rule(
              "GCP-BUCKET-ACCESS",
              "MEDIUM",
              "Uniform bucket-level access disabled",
              Set.of("google_storage_bucket"),
              List.of("/uniform_bucket_level_access")),
          new Rule(
              "GCP-GKE-AUTOREPAIR",
              "MEDIUM",
              "Node auto-repair disabled",
              Set.of("google_container_node_pool"),
              List.of("/management/0/auto_repair")),
          new Rule(
              "GCP-SQL-BACKUP",
              "HIGH",
              "Automated database backups disabled",
              Set.of("google_sql_database_instance"),
              List.of("/settings/0/backup_configuration/0/enabled")));
}
