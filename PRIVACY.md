# RideFare data and privacy

RideFare calculates fare estimates locally. It does not fetch live Uber, Ola or Rapido quotes, monitor drivers, or monitor prices in the background.

- **Location:** GPS is requested only when you tap Use GPS. A recent location is used as the pickup endpoint. Android's geocoder may send coordinates to the geocoding service configured on your device to obtain an address.
- **Address search:** typed queries are sent to Photon at `photon.komoot.io`, with pickup coordinates when available to bias results, and to Android's geocoding service. These services receive your IP address. Geocoding may also use the selected city.
- **Routes:** resolved pickup and destination coordinates are sent to OSRM at `router.project-osrm.org` for driving geometry and distance. This service also receives your IP address. The app does not supply a user account identifier.
- **Local storage:** favorites, endpoint coordinates, distance, price targets, traffic/weather assumptions and recent comparisons are stored in a Room database on your device. The database is excluded from Android cloud backup and device transfer. Delete favorites or clear comparison history in Favorites; Android's Clear Storage removes all app data.
- **Notifications:** only a manual estimate check can generate a notification, when allowed. It may display your route title, estimated fare and target on your lock screen according to Android notification settings. There are no scheduled price checks.
- **External apps:** Open Maps shares endpoint coordinates with the Maps app/site. Opening Uber shares resolved endpoint coordinates and address labels. Ola/Rapido handoffs open their app/site for you to enter the route. Copy and Share explicitly place comparison details into the clipboard or selected share destination.

The public Photon/OSRM services and your device's geocoder operate independently of RideFare. Network features require those services to be reachable. RideFare includes no analytics, Firebase SDKs, advertising or account backend. These statements describe this source version; the older APK in the repository may behave differently.
