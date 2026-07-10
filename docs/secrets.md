# Secrets and deployment configuration

Production credentials must never be committed. Base64 is only a transport encoding—not
encryption—so every base64 value below remains a GitHub Actions secret.

## GitHub Actions secrets

| Secret | Purpose | Runtime/build destination |
| --- | --- | --- |
| `GCP_DEPLOY_SERVICE_ACCOUNT_JSON_BASE64` | Dedicated least-privilege `github-actions-deploy@drebin451` service-account JSON | Decoded under runner temp and used only for gcloud/Firebase Hosting deployment auth |
| `FIREBASE_ADMIN_JSON_BASE64` | Firebase Admin runtime service-account JSON encoded as one base64 line | Synced to Secret Manager and exposed to Cloud Run as `FIREBASE_SERVICE_ACCOUNT_BASE64` |
| `FIREBASE_ANDROID_CONFIG_BASE64` | Production `google-services.json` | Decoded to the ignored Android module file before production builds |
| `FIREBASE_WEB_CONFIG_BASE64` | Production `firebase-web-config.properties` | Decoded to the ignored shared-module file before web builds |
| `ANDROID_KEYSTORE_BASE64` | Android release JKS/keystore | Decoded under the runner temp directory |
| `ANDROID_KEYSTORE_ALIAS` | Android signing key alias | Gradle `keyAlias` |
| `ANDROID_KEYSTORE_PASSWORD` | Android keystore password | Gradle `storePassword` |
| `ANDROID_KEY_PASSWORD` | Android private-key password | Gradle `keyPassword` |
| `B2_KEY_ID` | Backblaze B2 application key ID | Secret Manager → Cloud Run `B2_KEY_ID` |
| `B2_APPLICATION_KEY` | Backblaze B2 application key | Secret Manager → Cloud Run `B2_APPLICATION_KEY` |
| `STRIPE_SECRET_KEY` | Stripe server API key | Secret Manager → Cloud Run `STRIPE_SECRET_KEY` |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | Secret Manager → Cloud Run `STRIPE_WEBHOOK_SECRET` |
| `STRIPE_PRO_PRICE_ID` | Production Stripe recurring price ID | Cloud Run `STRIPE_PRO_PRICE_ID` |
| `DREBIN451_CRON_SECRET` | Storage reconciliation scheduler credential | Secret Manager → Cloud Run `DREBIN451_CRON_SECRET` |
| `DREBIN451_FIREBASE_WEB_API_KEY` | Public Firebase Identity Toolkit client key used for password-reset email requests | Plain Cloud Run `DREBIN451_FIREBASE_WEB_API_KEY` environment variable |
| `DREBIN_API_KEY` | Upload-only Drebin451 API key used to publish the Android release | `Commit451/drebin451-release` action input |

The server deploy keeps six sensitive runtime values in Google Secret Manager: Firebase Admin,
both Backblaze B2 credential fields, the Stripe API key, the Stripe webhook signing secret, and the
storage-reconciliation scheduler credential. On each trusted `main` deploy, GitHub Actions writes
new Secret Manager versions and binds them to Cloud Run. The Firebase Web API key is a public client
identifier already observable in shipped Firebase clients, so it is passed to Cloud Run as a plain
environment variable instead. Non-secret URLs, regions, buckets, price IDs, and project/service
names also stay in plain Cloud Run environment variables.

Deployment authentication and runtime Firebase access are intentionally separate. The dedicated
`github-actions-deploy@drebin451` account can deploy Cloud Run/Hosting, push Artifact Registry
images, and update only the pre-provisioned runtime secrets. The Firebase Admin account is not used
to deploy and should retain only the Firestore, Firebase Auth, FCM, and other explicitly required
runtime roles.

## Local fallbacks

The following files are ignored:

- `app/androidApp/google-services.json`
- `app/androidApp/keystore.jks`
- `app/shared/firebase-web-config.properties`
- `server/src/main/resources/drebin451-firebase-adminsdk.json`
- `.secrets/`
- `local.properties`

Android signing resolves each setting in this order:

1. Environment variable
2. Gradle property (`-P...` or `~/.gradle/gradle.properties`)
3. Ignored root `local.properties`

The keystore path defaults to `app/androidApp/keystore.jks`. If any signing value is missing, debug
builds use normal debug signing and release builds remain unsigned rather than using a committed
fallback password.

The web build uses `app/shared/firebase-web-config.properties` when present, otherwise it generates
safe placeholder constants from `firebase-web-config.example.properties`. Placeholder builds are
suitable for compilation/tests but do not connect to production Firebase.

The server resolves Firebase Admin credentials in this order:

1. `FIREBASE_SERVICE_ACCOUNT_BASE64`
2. Raw `FIREBASE_SERVICE_ACCOUNT`
3. `FIREBASE_SERVICE_ACCOUNT_PATH`
4. The ignored historical local path under `server/src/main/resources`

## Pull-request security

GitHub Actions workflows run on GitHub-hosted `ubuntu-latest` runners. The `pull_request` workflow
receives no production secrets, and deploy/release jobs only run from trusted pushes/tags. Every
referenced action is pinned to an immutable commit SHA, with Dependabot configured to propose
GitHub Actions updates.

## Before making the repository public

The current repository history still contains deleted credentials and must be rewritten as the
separate final publication step. At minimum:

1. Remove every historical version/rename of Firebase Admin JSON, Android `google-services.json`,
   Android keystores, hardcoded signing passwords, and hardcoded Firebase API-key/client-config
   source lines from all refs.
2. Re-run Gitleaks across the rewritten full history and require zero unreviewed findings.
3. Force-push the rewritten clean history only after coordinating with every existing clone/fork.
4. Revoke/rotate both historical Firebase Admin service-account keys after the rewrite.
5. Verify Firebase client API-key restrictions, OAuth origins, Android package/SHA restrictions,
   and App Check; browser/mobile Firebase identifiers are observable in shipped clients.
6. Review IAM once more with a project IAM administrator: remove the Firebase Admin runtime
   account's remaining `roles/storage.admin` grant if no GCS path is reintroduced, and scope the
   deploy account's project-level `roles/iam.serviceAccountUser` grant to only the Cloud Run runtime
   service account. The server currently uses Firestore, Firebase Auth, FCM, and Backblaze B2.
7. Consider replacing the dedicated deploy account's long-lived JSON key with GitHub OIDC/Google
   Workload Identity Federation after this requested Actions-secret migration is stable.
8. Decide whether the historical Android signing key needs rotation. If it has ever left trusted
   private storage, treat it as compromised; Play App Signing key rotation has separate constraints.
9. Enable GitHub secret scanning/push protection and branch protection once repository visibility
   and organization policy allow it.
10. Rotate any credential that may have been copied into logs, old clones, backups, or artifacts,
   even if the history rewrite no longer contains it.
