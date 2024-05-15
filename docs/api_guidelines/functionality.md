## Functionality {#functionality}

### Network access {#functionality-network}

Jetpack libraries may only access the network as part of the library's
advertised functionality or at the explicit request of the client as part of a
documented API contract.

For example, an image loading library *may* download an image from the network
as part of handling an API call to obtain a `Bitmap` from a `URL`. However, the
image loading library **must not** report API usage metrics to a Google server
because that is not required for image loading, nor is it behavior that the
client explicitly asked for.

### Notifications {#functionality-notifications}

Jetpack libraries may only post notifications at the explicit request of the
client as part of a documented API contract.

For example, the `compat` library *may* post notifications as the result of a
client calling `NotificationsCompat` APIs. However, the `compat` library **must
not** post notifications to advertise a feature in the library.

### Logging {#functionality-logging}

Jetpack libraries should do no logging (`android.util.Log.v`,
`android.util.Log.d`, `android.util.Log.i`, `System.out.println`, etc) in
production library code by default. All error states must be handled through
standard error return methanisms like return codes and exceptions with detailed
messages instead of relying on logging. Logging is easy to miss and hard to
build around. A library *may* provide an optional API to enable debug logging
when library has complex state management.
