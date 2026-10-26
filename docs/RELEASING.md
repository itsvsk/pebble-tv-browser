# Releasing Pebble on GitHub

GitHub Releases hosts the APK. Actions runs checks, signs the release, verifies its signature, generates a checksum, and publishes it. Workflows use GitHub's automatic token; no separate CI service or personal access token is needed.

## Signing setup

The first release uses a dedicated RSA key. Keep an offline backup of the keystore, passwords, and alias: GitHub secrets cannot be downloaded again, and Android updates require the same signing key. The [public certificate](release-signing-certificate.pem) is committed and checked against every release APK.

Under **Settings → Environments → github-release → Environment secrets**, configure:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ANDROID_STORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

The environment permits `main` and `v*` tags. Restore secrets from a secure backup using your personal GitHub account:

```sh
base64 < /secure/path/pebble-release.jks | \
  gh secret set ANDROID_KEYSTORE_BASE64 --repo itsvsk/pebble-tv-browser --env github-release
gh secret set ANDROID_STORE_PASSWORD --repo itsvsk/pebble-tv-browser --env github-release
gh secret set ANDROID_KEY_ALIAS --repo itsvsk/pebble-tv-browser --env github-release
gh secret set ANDROID_KEY_PASSWORD --repo itsvsk/pebble-tv-browser --env github-release
```

The last three commands prompt for values. Keep signing material outside the checkout; keystores and local signing configuration are ignored by Git.

## Publish and retry

1. Merge changes into `main` after CI and code review.
2. Update local `main`, create an annotated version tag such as `v0.1.1`, and push it.
3. Follow **Actions → Release APK**. CI runs on the exact tagged commit before signing; an Android TV emulator then checks startup and entry into Browse using the signed, optimized APK.
4. Download the APK from the release and verify `SHA256SUMS`.

Publication first uploads assets to a draft, then publishes the completed release. Assets include the signed `pebble-vX.Y.Z.apk`, `SHA256SUMS`, the public certificate, and an R8 mapping file for crash diagnosis.

Version codes are derived from the tag: `major × 1,000,000 + minor × 1,000 + patch`. For example, `v0.1.0` has name `0.1.0` and code `1000`. Minor/patch are 0–999, major is 0–2000, and `0.0.0`/leading zeroes are rejected. Always increase the version.

For a secret or service failure, fix it and re-run the workflow, or manually run **Release APK** from `main` with the existing tag. Incomplete drafts can be retried. Published releases are never overwritten; source fixes need a new version tag.

To withdraw a bad release, mark it as a draft and point users to a known good version. Android normally blocks version-code downgrades, so deliver a rollback fix in a higher version signed with the original key. Uninstalling removes app data.

## Local signed build

In Bash, load signing values from a secure source without putting passwords in command arguments:

```sh
export PEBBLE_KEYSTORE_PATH=/secure/path/pebble-release.jks
export PEBBLE_KEY_ALIAS=pebble-release
read -r -s -p 'Keystore password: ' PEBBLE_STORE_PASSWORD
export PEBBLE_STORE_PASSWORD
read -r -s -p 'Key password: ' PEBBLE_KEY_PASSWORD
export PEBBLE_KEY_PASSWORD
./gradlew :app:assembleRelease -PpebbleVersionName=0.1.0 \
  -PrequireReleaseSigning=true --no-configuration-cache
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Disable shell tracing while handling credentials. Missing or partial signing configuration fails a required signed release instead of falling back to a debug key. A local `assembleRelease` without credentials builds an unsigned artifact for compilation checks.

## CI maintenance

Require the build and both Android TV test checks in `main` branch protection. Keep Codex review enabled, review Dependabot changes, and inspect job logs when checks fail. The live-network `DownloadFailureTest` is run separately from deterministic emulator CI.

## Billing controls

Both workflows allow jobs only when the event repository is public and use standard `ubuntu-latest` runners. Private repositories skip these workflows; skipped checks do not provide test coverage. All existing lint, test, build, and release checks still run for the public repository.

Actions artifact uploads and cross-job Gradle caches are disabled. Dependencies are downloaded on each job, so builds take longer. Build outputs and detailed reports exist only on the temporary runner; use job logs for CI diagnostics or reproduce locally for HTML reports. Published APKs, checksums, certificates, and mappings remain GitHub Release assets, which have no release bandwidth quota.

For a billing-side hard stop, open **GitHub Settings → Billing and licensing → Budgets and alerts**. Create a product-level **Actions** budget scoped to `itsvsk/pebble-tv-browser`, set it to **$0**, and enable **Stop usage when budget limit is reached**. An alert-only budget does not block charges. Keep this control if payment methods or workflow settings change. Repository YAML cannot enforce account billing settings or reverse charges already accrued.

When adopting this policy, delete pre-existing Actions artifacts and caches for this repository. Do not delete GitHub Release assets or signing secrets. Re-check the policy before adding a workflow, cache, artifact upload, paid runner, or external CI service.

References: [Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions), [hard-stop budgets](https://docs.github.com/en/billing/how-tos/set-up-budgets), [release storage and bandwidth](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).

References: [GitHub Gradle CI](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle), [Android signing](https://developer.android.com/studio/publish/app-signing), [GitHub CLI releases](https://cli.github.com/manual/gh_release_create).
