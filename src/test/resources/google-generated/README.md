# Google-provider plan fixtures

Generated on macOS AMD64 with Terraform 1.15.7 and hashicorp/google 7.13.0. The five JSON files are complete `terraform show -json` exports; their values have not been hand-edited. The adjacent HCL and lockfile pin the input configuration and provider.

All identities and values are synthetic. Prior state is fabricated from known planned values by [the generator](../../../../scripts/generate-google-fixtures.py), then passed to `terraform plan -refresh=false`. No real cloud state, credentials, apply or import is used. Computed-only differences can remain because the state does not come from an apply.

From the repository root, run:

```sh
python3 scripts/generate-google-fixtures.py
./mvnw clean verify
```

Terraform 1.15.7 and Python 3.9+ are required for regeneration; the tests themselves need only Java and Maven. See [the validation guide](../../../../docs/google-provider-validation.md) for provider installation, expected findings, provenance and limitations. Regeneration changes timestamps; tests assert behavior rather than exact bytes.

Never add production plans, state files, provider binaries, binary plans or credentials here.
