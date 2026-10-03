# RideFare data and privacy

RideFare does not fetch live Uber, Ola or Rapido quotes, monitor drivers, or monitor prices in the background. It no longer displays numeric prices calculated from sample rates.

- **Location:** GPS is requested when you tap Use GPS. Android's geocoder may send coordinates to the device's geocoding service to obtain an address.
- **Google search, when configured:** the Google Places SDK receives your search text and a pickup/city location bias. Selecting a prediction sends its place ID and autocomplete session token to Google to retrieve the address and coordinates. Google's own autocomplete screen displays predictions and attribution. See [Google Privacy Policy](https://policies.google.com/privacy).
- **Limited search, without a Google key:** queries and pickup/city focus are sent to Photon at `photon.komoot.io`; Android's geocoding service is a fallback. These services receive your IP address. The screen identifies this limited search mode.
- **Routes:** endpoint coordinates are sent to OSRM at `router.project-osrm.org` for driving geometry and distance. The app does not send a user account identifier.
- **Local storage:** favorites and their selected endpoint coordinates, distance and legacy route history are stored in a Room database on the device, excluded from Android backup/device transfer. Old price targets/sample history may remain in the database but do not generate quotes or alerts. Delete favorites or clear history in Favorites; Android's Clear Storage removes all app data.
- **Notifications:** the current app does not generate fare-target notifications. Previously created notifications may remain until dismissed.
- **External apps:** opening Uber shares resolved endpoint coordinates/address labels. Ola/Rapido open for manual route entry. Missing provider apps open their store listing.

Network features require the selected service to be reachable and appropriately configured. There is no RideFare account backend, Firebase, or advertising integration. Google Places has its own SDK/service data processing. These statements describe version 1.3; the old APK committed at the repository root may behave differently.
