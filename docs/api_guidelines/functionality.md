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
