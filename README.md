# RideFare

An Android app for comparing **model-based fare and pickup-time estimates** across Uber, Ola and Rapido. It is not connected to live provider pricing, fleet availability or verified coverage. Final fares, vehicle categories and booking details must be confirmed in the provider app.

## Build and verify

Use JDK 17 and Android SDK platform 36.1 with build tools 36.0.0. Android Studio can install the SDK, or run:

```sh
sdkmanager 'platforms;android-36.1' 'build-tools;36.0.0'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Set `ANDROID_HOME` or add `sdk.dir=/your/android/sdk` to an untracked `local.properties`. The complete Gradle wrapper is included and its distribution checksum is pinned. Debug builds use Android's default debug signing and do not require Firebase, API keys, `.env` or a custom keystore.

For release builds, supply `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` using a keystore with alias `upload`. Preserve the original application's signing key to update existing installs. Version 1.1 uses version code 2. The APK at the repository root is an older build; build the updated code or download the debug APK from the Android checks workflow.

## Behavior

- Resolve both endpoints before comparing, saving, opening a provider or navigating. Address edits invalidate old coordinates and pending route requests.
- Favorites save coordinates, city and traffic/weather assumptions. Version 1/2 databases migrate without deleting favorites or history. Legacy favorites without resolvable coordinates require selecting their addresses again.
- OSRM geometry is shown once as a schematic route preview. No simulated traffic, tracked vehicles or invented directions are shown. Routing failures show an unavailable state and use an explicitly approximate distance.
- Unknown cities have no invented presets. Provider coverage is always unverified; providers are offered as estimate models.
- Price targets are **manual checks**, not subscriptions. Tap **Check Estimate** in Favorites to compare a saved target using that route's saved distance and conditions. A check can optionally produce a notification; it does not monitor prices in the background. Notification taps reopen the saved route.
- Uber uses its documented `setPickup` URI with resolved endpoint coordinates. Ola and Rapido open their app/site without asserting that an undocumented destination URI worked. Confirm or enter addresses and select the vehicle in the provider app.

## Data and privacy

See [PRIVACY.md](PRIVACY.md). The home screen also explains third-party location transfers. No analytics, account or Firebase functionality is included in the app.

## Validation

Regression tests cover fare sums and rankings, traffic/weather ETA offsets, independent geocoding, stale results, address invalidation, favorites and legacy restoration, manual distance changes, notification identity, invalid booking and preserving data during v1/v2 migrations. GitHub Actions builds a debug APK and runs unit tests and Android lint.

Before a public release, test on actual devices: approximate/denied GPS permission, notifications denied or disabled, slow/offline search and routing, notification cold/warm starts, large text and narrow screens, and provider handoffs with Uber/Ola/Rapido installed and absent. Live quotes or background alerts require separately verified provider APIs and credentials; this build makes no such claims.
