# Package androidx.core.locationbutton.compose

The `LocationButton` is a secure Jetpack Compose widget designed to streamline session-based precise location permission requests.

On Android 17 (SDK 37) and above, the widget renders a **remote system component** inside your app via a `SurfaceView`. On older Android versions, it seamlessly falls back to an identical **Material 3 Expressive Button** that triggers the standard permission dialog.

---

## Integration Guide

### 1. Basic Setup (Composable)

Add the `LocationButton` to your Composable hierarchy. The button handles its own sizing, styling, and positioning via standard Compose `Modifier` configurations.

```kotlin
LocationButton(
    onPermissionResult = { granted ->
        if (granted) {
            // Permission acquired! Proceed with location logic.
        } else {
            // Handle permission denial.
        }
    },
    textType = LocationButtonTextType.UsePreciseLocation
)
```

### 2. Handling Results (Callback)

The `onPermissionResult` callback is invoked on both older and newer Android versions to receive the permission grant result:
*   **On API 37+:** Triggered after the user interacts with the secure, system-rendered button and makes a decision on the system permission prompt.
*   **On older versions:** Triggered after the user makes a decision on the standard local permission dialog.

### 3. Fallback & Custom Flows (Older Android Versions)

On Android versions prior to API 37, the widget automatically launches a standard permission request flow when clicked.

#### **Custom Permission Flow (Optional)**
If you want to display a custom rationale dialog or handle the permission launcher manually on older platforms, provide the `onRequestPermissions` lambda:

```kotlin
LocationButton(
    onPermissionResult = { granted -> ... },
    onRequestPermissions = {
        // 1. Show your custom rationale dialog (optional)
        // 2. Trigger your own permission request launcher
    }
)
```

---

## Critical Layout & Security Constraints

To satisfy the platform's anti-tapjacking and layout security requirements, the following constraints are strictly enforced:

*   **Size Constraints:** The button's width and height must be at least **48dp** (otherwise click events are disabled). The maximum height is capped at **136dp**. Out-of-bounds values are automatically coerced by the system.
*   **Padding Limits:** Clickable padding is strictly clamped to a **minimum of 4dp** and a **maximum of 8dp**. Out-of-bounds values are automatically coerced.
*   **Anti-Tapjacking:** The remote button must remain 100% unobscured. If any overlay (such as a dialog shadow, tooltip, or floating menu) covers any part of the button, the OS will **silently drop all click events** on Android 17+.
*   **Composition Order:** Controls the Z-order of the remote `SurfaceView`. This is a safe no-op on older platforms where the local fallback button is rendered.
