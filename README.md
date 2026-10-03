# RideFare

Android route search and provider handoff for Uber, Ola and Rapido. **Live provider prices are not connected.** Version 1.3 removes the sample numeric fares, cheapest-provider rankings and manual sample-price checks from the app.

## Google place search

The app now integrates **Google Places Autocomplete (New)** for both pickup and destination. Google's own search screen provides predictions; selecting a result fetches its actual coordinates. Search is biased toward the pickup/selected city, without limiting matches to that city. Businesses and addresses are not excluded by category filters. Typing alone never silently chooses the first geocoder match.

**Activation requires your Google Cloud Places API key with billing and Places API (New) enabled.** Follow [GOOGLE_PLACES_SETUP.md](GOOGLE_PLACES_SETUP.md) for Android restrictions, local builds, and GitHub Actions secrets/signing configuration. API keys are not committed. No live Google requests have been validated without credentials.

Without a key, the APK explicitly says **Limited place search** and uses Photon/Android geocoding, whose coverage differs from Google Maps. This fallback cannot promise the same places as Google. Configured Google failures remain visible rather than silently switching search providers.

## Build

Use JDK 17, Android SDK platform 36.1 and build tools 36.0.0:

```sh
sdkmanager 'platforms;android-36.1' 'build-tools;36.0.0'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Set `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`. Version **1.3/code 4** is built by CI. The APK committed at the repository root is an old build—download a new workflow artifact instead.

The ordinary Android checks workflow compiles the full SDK integration without credentials and produces a limited-search APK when no key is set. **Build with Google Places** is a manual workflow that requires the Places and stable-debug-signing secrets; use its artifact once configured. Register the actual APK signing certificate with Google, including Play App Signing's certificate for Play-distributed builds.

Debug builds use standard debug signing. Release builds require `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` for a keystore with alias `upload`. Preserve your signing identity to update existing installations.

## Fares and provider apps

Every provider card now says **Live price unavailable** and lets you check its current quote in the provider app. No numeric fare, pickup ETA, discount or cheapest ranking is inferred from a sample formula. Old sample-price history is retained as route history but its prices are hidden. Price-target controls are disabled until a real quote source exists.

Uber receives resolved coordinates through its documented native link, with a web fallback. Confirm pickup in Uber to reveal the destination; check the vehicle and quote there. Ola/Rapido open for manual route entry. Missing apps go to their store listing. No booking is made automatically.

Real in-app pricing requires provider-approved integrations. Google's Places key only supplies location search; it cannot supply ride prices. Uber's documented estimate API requires approval and restricts competitive price comparisons. See the setup guide for primary references.

## Saved routes and privacy

Both endpoints must be selected before opening a provider or saving. Favorites retain their own coordinates; old database versions migrate without deleting data. Legacy routes without coordinates need resolution. Editing, swapping, GPS and asynchronous route lookups reject stale results.

OSRM still resolves route distances used by saved routes; routing failures retain an explicitly approximate internal distance. No external map tiles or invented traffic are shown. See [PRIVACY.md](PRIVACY.md) for location-data transfers and local storage.

## Validation

CI builds the APK and runs unit tests and lint. Regression tests cover endpoint invalidation, stale requests, favorites/migrations, provider URI parameters, limited-search parsing, absence of fabricated prices, and typed-text selection safety. A valid restricted Google key and a physical Android device are still needed to test actual search results, key/billing errors, and installed-provider handoffs.
