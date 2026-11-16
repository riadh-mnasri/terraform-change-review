# Rules v0.1

Rules are change-focused and deliberately narrow. HIGH means requires attention, not a proven incident. IDs are stable within this release.

| ID | Trigger | Important limits |
|---|---|---|
| TF-DELETE | Managed resource has delete without create | No data-loss or downtime inference |
| TF-REPLACE | Managed resource actions contain delete and create, either order | Does not simulate lifecycle/dependency ordering |
| GCP-IAM-ELEVATED | New principal-role assignment for a curated sensitive role | Project/bucket member and binding resources only; no effective-permission expansion or condition analysis |
| GCP-IAM-PUBLIC | New allUsers/allAuthenticatedUsers assignment | Same types; role and organization controls affect actual exposure |
| GCP-FIREWALL-WORLD | Relevant fields change on active ingress allow rule with explicit 0.0.0.0/0 or ::/0 source | No implied default source, hierarchical firewall evaluation, route or target reachability simulation |
| GCP-BUCKET-DESTROY | force_destroy becomes true, including creation with true | Does not mean bucket is being deleted in this plan |
| GCP-BUCKET-ACCESS | uniform_bucket_level_access becomes false | Does not inspect object ACLs |
| GCP-GKE-AUTOREPAIR | management[0].auto_repair becomes false | No cluster health prediction |
| GCP-SQL-BACKUP | settings[0].backup_configuration[0].enabled becomes false | Alternate backups are not evaluated |

Sensitive role list: roles/owner, roles/editor, roles/iam.securityAdmin, roles/resourcemanager.projectIamAdmin, roles/iam.serviceAccountTokenCreator, roles/iam.serviceAccountUser. This is not exhaustive and these roles differ materially in scope and semantics.

IAM resource types: google_project_iam_member, google_project_iam_binding, google_storage_bucket_iam_member, google_storage_bucket_iam_binding. IAM policy JSON, custom roles, deny policies, organization/folder bindings, effective inheritance and trust-chain evaluation are not implemented.

Arrays are reported as a changed parent path in the generic diff. Unknown markers are reported down to their marked paths. Checks do not resolve unknown values, and changes to unknown values alone may produce no finding. No-op resources are omitted unless imported or moved. Their import/move semantics are not yet interpreted. Removed/forgotten actions are inventoried but do not imply destruction. Output changes are inventoried without values. Drift and deferred changes are not analyzed; deferred presence marks the report incomplete.

## Provider references

Attribute semantics were checked against the provider documentation during implementation. Fixtures are synthetic plan fragments; they do not constitute a compatibility matrix for every provider version.

- [Storage bucket](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/storage_bucket)
- [Compute firewall](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/compute_firewall)
- [GKE node pool](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/container_node_pool)
- [Cloud SQL instance](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/sql_database_instance)
