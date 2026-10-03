# Enable Google place search

Version 1.3 includes Google's Places Autocomplete (New) widget for both pickup and drop, followed by Place Details for the selected coordinates. A Google Cloud project/key is still required. Autocomplete shares Google's place database but is not a guarantee of identical rankings or every result shown by the consumer Google Maps app.

1. In your Google Cloud project, enable billing and **Places API (New)**. Apply usage quotas/budget alerts appropriate to your deployment.
2. Create an API key restricted to **Android apps**. Add package `com.aistudio.ridefare.xkrqzp` and the **SHA-1 of the certificate signing the APK you install**. Restrict the key to Places API (New).
3. Add this line to the existing, ignored `local.properties`, alongside `sdk.dir`:

   ```properties
   GOOGLE_PLACES_API_KEY=YOUR_ANDROID_RESTRICTED_KEY
   ```

   Or supply `GOOGLE_PLACES_API_KEY` in the build environment. Do not commit a real key to the repository or paste it into an issue/chat. An Android key is included in the APK by design; its package/certificate/API restrictions are essential.
4. Run `./gradlew signingReport` to obtain the signing SHA-1 and `./gradlew :app:assembleDebug` to build. Install `app/build/outputs/apk/debug/app-debug.apk`.
5. Version 1.3 with a key shows **Search places with Google**. Tap pickup or drop to open Google's search screen; select a result, wait for its coordinates, and confirm the displayed address. If the UI says **Limited place search**, the APK was built without a key.

## GitHub Actions

Add repository Actions secret `GOOGLE_PLACES_API_KEY`. To keep the Android certificate restriction stable across CI builds, also add `ANDROID_DEBUG_KEYSTORE_BASE64`: the base64 encoding of your chosen **debug** keystore (standard alias/password `androiddebugkey` / `android`). Register that certificate's SHA-1 in Google Cloud. Never use your production signing key for this debug workflow.

The workflow restores this keystore before building and injects the Places key. It fails explicitly when a Places key is supplied without a stable debug keystore. Without either secret, CI tests compile the full integration but the produced APK clearly uses limited search. Fork PRs receive no secrets.

There is also a **Build with Google Places** manually triggered workflow: it requires both secrets and cannot produce a supposedly Google-enabled APK without them. Download its `ridefare-google-places` artifact.

## Live fares

Google Places provides locations, not Uber/Ola/Rapido quotes. This app has no approved provider quote feed. Version 1.3 therefore displays **Live price unavailable** and links to each provider instead of generating numeric rates or cheapest-provider claims from sample formulas. Old sample prices are hidden in history, and price-target checks are disabled.

Accurate in-app prices require a separately authorized provider/backend integration that supplies current route-specific quotes. Uber's public price-estimates documentation explicitly requires approval and restricts competitive price comparisons; plan the business integration with the providers before implementing a comparison feed. No scraping or accessibility-based extraction is implemented.

References: [Google setup](https://developers.google.com/maps/documentation/places/android-sdk/get-api-key), [Autocomplete (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), [Uber price-estimate access](https://developer.uber.com/docs/riders/references/api/v1.2/estimates-price-get).
