# Package androidx.core.locationbutton

The `LocationButton` is a secure widget designed to streamline session-based precise location permission requests.

On Android 17 (SDK 37) and above, this widget renders a **remote system component** directly inside your app via `SurfaceView`. On older Android versions, it seamlessly falls back to an identical locally rendered button that triggers the standard permission dialog.

---

## Integration Guide

### 1. Add the Widget to your XML Layout

You can add the `LocationButton` directly to your XML layouts. It **does not support child views**.

```xml
<androidx.core.locationbutton.LocationButton
    android:id="@+id/location_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:backgroundColor="@color/custom_button_background"
    app:cornerRadius="24dp"
    android:iconTint="@color/custom_button_foreground"
    app:locationButtonTextType="use_precise_location"
    android:textColor="@color/custom_button_foreground" />
```

> **Tip:** Ensure your color choices maintain a high contrast ratio (at least 4.5:1) for accessibility compliance.

### 2. Register the Listener

In your Activity or Fragment, set the `LocationButtonListener` to receive the permission grant results.

```kotlin
val locationButton = findViewById<LocationButton>(R.id.location_button)

locationButton.setLocationButtonListener(object : LocationButton.LocationButtonListener {
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // Permission acquired! Proceed with location logic.
        } else {
            // Handle denial gracefully.
        }
    }

    // Optional: Override if you want to track remote session failures in your analytics.
    override fun onSessionError(throwable: Throwable) {
        // Handle the throwable
    }
})
```

### 3. Handle Fallback Permissions (Older Android Versions)

On Android versions prior to 17 (SDK 37), the widget triggers the standard permission dialog using `ActivityCompat.requestPermissions`. Because the `LocationButton` is a view and cannot intercept `onRequestPermissionsResult` from the Activity, you must handle the permission result manually in your Activity or Fragment.

```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1001) { // Default request code used by LocationButton
        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        // Proceed with location logic based on 'granted'
    }
}
```

---

## Architectural Constraints & Gotchas

Because the `LocationButton` bridges the app process and the system process, it enforces strict security and layout constraints.

### Padding Limits
To prevent invisible tapjacking surfaces, padding is strictly clamped to a **maximum of 8dp** and a **minimum of 4dp**. If you supply values outside this range, the library will automatically clamp it.

### Size Constraints
The minimum width and height must be at least **48dp**; otherwise, click events will be disabled. The **maximum height is capped at 136dp**.

### View Composition (Z-Order)
By default, the remote SystemUI `SurfaceView` draws **on top** of your app window (`setCompositionOrder(1)`).
- If you place another View (like a floating translucent overlay or a custom dialog shadow) over the `LocationButton`, the OS anti-tapjacking security will **silently drop all click events**.
- The button must remain 100% unobscured on the screen.
