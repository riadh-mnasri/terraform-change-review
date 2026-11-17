terraform {
  required_version = "= 1.15.7"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "7.13.0"
    }
  }
}

# Deliberately invalid credentials; these plans must never be applied.
provider "google" {
  project      = "fixture-project"
  access_token = "synthetic-nonfunctional-token"
}

variable "scenario" {
  type    = string
  default = "baseline"
  validation {
    condition     = contains(["baseline", "risky", "expression", "labels"], var.scenario)
    error_message = "Unknown fixture scenario."
  }
}

locals {
  risky = var.scenario == "risky"
}

resource "google_project_iam_member" "reviewer" {
  project = "fixture-project"
  role    = "roles/editor"
  member  = "user:reviewer@example.test"
  dynamic "condition" {
    for_each = local.risky ? [] : [1]
    content {
      title      = "temporary"
      expression = var.scenario == "expression" ? "true" : "request.time < timestamp(\"2027-01-01T00:00:00Z\")"
    }
  }
}

resource "google_project_iam_binding" "reviewers" {
  project = "fixture-project"
  role    = "roles/owner"
  members = local.risky ? ["user:reviewer@example.test", "allUsers"] : ["user:reviewer@example.test"]
}

resource "google_storage_bucket_iam_member" "reader" {
  bucket = "terraform-review-fixture-bucket"
  role   = "roles/storage.objectViewer"
  member = local.risky ? "allAuthenticatedUsers" : "user:reader@example.test"
}

resource "google_storage_bucket_iam_binding" "readers" {
  bucket  = "terraform-review-fixture-bucket"
  role    = "roles/storage.objectAdmin"
  members = local.risky ? ["user:reader@example.test", "allUsers"] : ["user:reader@example.test"]
}

resource "google_compute_firewall" "ingress" {
  name          = "terraform-review-fixture-ingress"
  network       = "projects/fixture-project/global/networks/fixture-network"
  direction     = "INGRESS"
  disabled      = false
  source_ranges = local.risky ? ["0.0.0.0/0", "::/0"] : ["10.0.0.0/8"]
  target_tags   = ["fixture-target"]
  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}

resource "google_storage_bucket" "archive" {
  name                        = "terraform-review-fixture-bucket"
  location                    = "EU"
  force_destroy               = local.risky
  uniform_bucket_level_access = !local.risky
  labels                      = { fixture = var.scenario == "labels" ? "renamed" : "baseline" }
}
