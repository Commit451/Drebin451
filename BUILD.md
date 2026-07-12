## Build

A clean public checkout builds and tests without production credentials:

```shell
./gradlew test
```

Firebase-backed sign-in, production Android signing, deployment, and release publishing require
local credentials. See [docs/secrets.md](docs/secrets.md) for the complete GitHub Actions and local
fallback setup.

### Local Android release

1. Download your Firebase Android `google-services.json` to
   `app/androidApp/google-services.json`.
2. Put the signing keystore at `app/androidApp/keystore.jks` or set
   `ANDROID_KEYSTORE_PATH`.
3. Add the following to ignored `local.properties`, `~/.gradle/gradle.properties`, or your shell
   environment:

```properties
ANDROID_KEYSTORE_ALIAS=your-key-alias
ANDROID_KEYSTORE_PASSWORD=your-store-password
ANDROID_KEY_PASSWORD=your-key-password
```

Then build:

```shell
./gradlew :app:androidApp:assembleRelease
./gradlew :app:androidApp:bundleRelease
```

Without signing credentials, release compilation remains available but the release artifact is not
production-signed.

### Local web configuration

Copy the safe template and replace its placeholders with your Firebase web app settings:

```shell
cp app/shared/firebase-web-config.example.properties \
  app/shared/firebase-web-config.properties
./gradlew :app:webApp:composeCompatibilityBrowserDistribution
```

Firebase web identifiers are public in the deployed browser bundle, but production values are kept
out of the source repository so forks can use their own Firebase project.

### Local server

Set Firebase Admin credentials using one of these fallbacks, in precedence order:

- `FIREBASE_SERVICE_ACCOUNT_BASE64` — base64-encoded service-account JSON (production contract)
- `FIREBASE_SERVICE_ACCOUNT` — raw JSON
- `FIREBASE_SERVICE_ACCOUNT_PATH` — path to a local JSON file
- ignored `server/src/main/resources/drebin451-firebase-adminsdk.json`

The local JSON file is explicitly excluded from server resources and Docker build context, so it is
never packaged into the fat jar or container image.