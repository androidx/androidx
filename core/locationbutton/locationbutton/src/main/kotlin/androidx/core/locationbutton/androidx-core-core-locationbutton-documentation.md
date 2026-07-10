# Package androidx.core.locationbutton

The `LocationButton` is a secure widget designed to streamline session-based precise location permission requests.

On Android 17 (SDK 37) and above, the widget renders a **remote system component** inside your app via a `SurfaceView`. On older Android versions, it seamlessly falls back to an identical **Material 3 Expressive Button** that triggers the standard permission dialog.

---

## Integration Guide

### 1. Basic Setup (XML Layout)

Add the `LocationButton` directly to your XML layouts.

```xml
<androidx.core.locationbutton.LocationButton
    android:id="@+id/location_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:locationButtonTextType="use_precise_location" />
```

### 2. Handling Results (Listener)

In your Activity or Fragment, register the `OnPermissionResultListener` to receive the permission grant result on both older and newer Android versions.

```kotlin
val locationButton = findViewById<LocationButton>(R.id.location_button)

locationButton.setOnPermissionResultListener { granted ->
    if (granted) {
        // Permission acquired! Proceed with location logic.
    } else {
        // Handle denial gracefully.
    }
}
```

### 3. Fallback & Custom Flows (Older Android Versions)

On Android versions prior to API 37, the widget automatically requests location permissions when clicked.

#### **Requirements for Automatic Flow:**
1.  The hosting `Activity` must implement `ActivityResultRegistryOwner` (implemented by default in `ComponentActivity` and `AppCompatActivity`).
2.  The `LocationButton` in the XML layout **must have a valid `android:id`**.

#### **Custom Permission Flow (Optional)**
If you want to display a custom rationale dialog or handle the permissions request in your app on older platforms, you can override the automatic flow by registering a custom request listener.

```kotlin
locationButton.setOnRequestPermissionsListener {
    // 1. Show your custom rationale dialog (optional)
    // 2. Trigger your own permission request
}
```

#### **Specifying the Parent Activity (Optional)**
In environments where the button's `Context` is not an `Activity` (such as themed context wrappers), you can explicitly set the hosting activity:

```kotlin
locationButton.parentActivity = myActivity
```

---

## Critical Layout & Security Constraints

To satisfy the platform's anti-tapjacking and layout security requirements, the following constraints are strictly enforced:

*   **Size Constraints:** The button's width and height must be at least **48dp** (otherwise click events are disabled). The maximum height is capped at **136dp**. Out-of-bounds values are automatically coerced by the system.
*   **Padding Limits:** Clickable padding is strictly clamped to a **minimum of 4dp** and a **maximum of 8dp**. Out-of-bounds values are automatically coerced.
*   **Anti-Tapjacking:** The remote button must remain 100% unobscured. If any overlay (such as a dialog shadow, tooltip, or floating menu) covers any part of the button, the OS will **silently drop all click events** on Android 17+.
*   **Composition Order:** Controls the Z-order of the remote `SurfaceView`. This is a safe no-op on older platforms where the local fallback button is rendered.
