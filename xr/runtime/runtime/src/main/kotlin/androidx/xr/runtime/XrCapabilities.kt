/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** A device capability that determines how virtual content is added to the real world. */
public class DisplayBlendMode private constructor(private val value: Int) {

    public companion object {
        /** Blending is not supported. */
        @JvmField public val NO_DISPLAY: DisplayBlendMode = DisplayBlendMode(0)
        /**
         * Virtual content is added to the real world by adding the pixel values for each of Red,
         * Green, and Blue components. Alpha is ignored. Black pixels will appear transparent.
         */
        @JvmField public val ADDITIVE: DisplayBlendMode = DisplayBlendMode(1)
        /**
         * Virtual content is added to the real world by alpha blending the pixel values based on
         * the Alpha component.
         */
        @JvmField public val ALPHA_BLEND: DisplayBlendMode = DisplayBlendMode(2)
    }
}

/** Contextual label describing the type of detected object. */
public class AugmentedObjectCategory private constructor(private val value: Int) {
    public companion object {
        /** Category value indicating the tracked object is of unknown type. */
        @JvmField public val UNKNOWN: AugmentedObjectCategory = AugmentedObjectCategory(0)
        /** Category value indicating the tracked object is believed to be a keyboard. */
        @JvmField public val KEYBOARD: AugmentedObjectCategory = AugmentedObjectCategory(1)
        /** Category value indicating the tracked object is believed to be a mouse. */
        @JvmField public val MOUSE: AugmentedObjectCategory = AugmentedObjectCategory(2)
        /** Category value indicating the tracked object is believed to be a laptop. */
        @JvmField public val LAPTOP: AugmentedObjectCategory = AugmentedObjectCategory(3)

        @JvmStatic
        @Deprecated("Use allSupported() instead.", ReplaceWith("allSupported()"))
        /** Returns an array of all available [AugmentedObjectCategory] values. */
        public fun all(): List<AugmentedObjectCategory> = listOf(KEYBOARD, MOUSE, LAPTOP)

        /**
         * Returns a list of all [AugmentedObjectCategories][AugmentedObjectCategory] supported by
         * the device.
         */
        @JvmStatic
        public fun allSupported(): Set<AugmentedObjectCategory> =
            setOf(
                // TODO b/483728983 determine contents of this list dynamically based on device
                // capability
                KEYBOARD,
                MOUSE,
                LAPTOP,
            )
    }
}

/** Feature that allows tracking of and provides information about scene planes. */
public class PlaneTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /** Planes will not be tracked. */
        @JvmField public val DISABLED: PlaneTrackingMode = PlaneTrackingMode(0)
        /**
         * Horizontal and vertical planes will be tracked. Note that setting this mode will consume
         * additional runtime resources.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services
         *
         * Required permissions:
         * - [SCENE_UNDERSTANDING_COARSE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE]
         *   (OpenXR runtimes only)
         * - [ACCESS_COARSE_LOCATION][android.Manifest.permission.ACCESS_COARSE_LOCATION] (Play
         *   Services runtimes only)
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val HORIZONTAL_AND_VERTICAL: PlaneTrackingMode = PlaneTrackingMode(1)
    }
}

/** Feature that allows tracking of the user's hands and hand joints. */
public class HandTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /** Hands will not be tracked. */
        @JvmField public val DISABLED: HandTrackingMode = HandTrackingMode(0)
        /**
         * Both the left and right hands will be tracked. Note that setting this mode will consume
         * additional runtime resources.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions:
         * - [HAND_TRACKING][androidx.xr.runtime.manifest.HAND_TRACKING]
         */
        @JvmField public val BOTH: HandTrackingMode = HandTrackingMode(1)
    }
}

/** Feature that allows tracking of the AR device. */
public class DeviceTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /**
         * The device pose will not be tracked. In this mode, [androidx.xr.arcore.RenderViewpoint]
         * will not emit updates to [androidx.xr.arcore.RenderViewpoint.State.pose].
         */
        @JvmField public val DISABLED: DeviceTrackingMode = DeviceTrackingMode(0)
        /**
         * The device pose will be tracked and the last known pose from the system at the time of
         * runtime update will be provided. Note that there is generally a delay between the actual
         * device pose and the pose provided by the system by the time of the update.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services
         *
         * Required permissions:
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val LAST_KNOWN: DeviceTrackingMode = DeviceTrackingMode(1)
    }
}

/** Feature that allows more accurate information about scene depth and meshes. */
public class DepthEstimationMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /** No information about scene depth will be provided. */
        @JvmField public val DISABLED: DepthEstimationMode = DepthEstimationMode(0)

        /**
         * Depth estimation will be enabled with raw depth and confidence.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services (on supported devices)
         *
         * Required permissions:
         * - [SCENE_UNDERSTANDING_FINE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE]
         *   (OpenXR runtimes only)
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val RAW_ONLY: DepthEstimationMode = DepthEstimationMode(1)

        /**
         * Depth estimation will be enabled with smooth depth and confidence.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services (on supported devices)
         *
         * Required permissions:
         * - [SCENE_UNDERSTANDING_FINE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE]
         *   (OpenXR runtimes only)
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val SMOOTH_ONLY: DepthEstimationMode = DepthEstimationMode(2)

        /**
         * Depth estimation will be enabled with both raw and smooth depth and confidence. Note that
         * setting this mode will consume additional runtime resources.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services (on supported devices)
         *
         * Required permissions:
         * - [SCENE_UNDERSTANDING_FINE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE]
         *   (OpenXR runtimes only)
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val SMOOTH_AND_RAW: DepthEstimationMode = DepthEstimationMode(3)
    }
}

/** Feature that allows anchors to be persisted through sessions. */
public class AnchorPersistenceMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /** Anchors cannot be persisted. */
        @JvmField public val DISABLED: AnchorPersistenceMode = AnchorPersistenceMode(0)
        /**
         * Anchors may be persisted and will be saved in the application's local storage.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions: None
         */
        @JvmField public val LOCAL: AnchorPersistenceMode = AnchorPersistenceMode(1)
    }
}

/**
 * Feature that allows tracking of human faces.
 *
 * Setting this feature to [FaceTrackingMode.BLEND_SHAPES] requires that the `FACE_TRACKING` Android
 * permission is granted by the calling application.
 *
 * Setting this feature to [FaceTrackingMode.MESHES] requires the `CAMERA` Android permission to be
 * granted and that [CameraFacingDirection] is set to [CameraFacingDirection.Companion.USER].
 */
public class FaceTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /** Faces will not be tracked. */
        @JvmField public val DISABLED: FaceTrackingMode = FaceTrackingMode(0)

        /**
         * Blend shapes of the user's face will be tracked.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions:
         * - [FACE_TRACKING][androidx.xr.runtime.manifest.FACE_TRACKING]
         */
        @JvmField public val BLEND_SHAPES: FaceTrackingMode = FaceTrackingMode(1)

        /**
         * Face meshes will be tracked using the front-facing camera.
         *
         * Supported runtimes:
         * - Play Services
         *
         * Required permissions:
         * - [CAMERA][android.Manifest.permission.CAMERA]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmField
        public val MESHES: FaceTrackingMode = FaceTrackingMode(2)
    }
}

/**
 * Feature that allows Geospatial localization and tracking. The Geospatial API uses a combination
 * of Google's Visual Positioning System (VPS) and GPS to determine the geospatial pose.
 *
 * The Geospatial API is able to provide the best user experience when it is able to generate high
 * accuracy poses. However, the Geospatial API can be used anywhere, as long as the device is able
 * to determine its location, even if the available location information has low accuracy.
 * - In areas with VPS coverage, the Geospatial API is able to generate high accuracy poses. This
 *   can work even where GPS accuracy is low, such as dense urban environments. Under typical
 *   conditions, VPS can be expected to provide positional accuracy typically better than 5 meters
 *   and often around 1 meter, and a rotational accuracy of better than 5 degrees. Use
 *   `Geospatial.checkVpsAvailability` to determine if a given location has VPS coverage.
 * - In outdoor environments with few or no overhead obstructions, GPS may be sufficient to generate
 *   high accuracy poses. GPS accuracy may be low in dense urban environments and indoors.
 *
 * Note that setting this mode will consume additional runtime resources.
 */
public class GeospatialMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /**
         * The Geospatial API is disabled. When GeospatialMode is disabled, current `Anchor` objects
         * created from `Geospatial` will stop updating, and have their [TrackingState] set to
         * [TrackingState.STOPPED].
         */
        @JvmField public val DISABLED: GeospatialMode = GeospatialMode(0)

        /**
         * The Geospatial API is enabled. `Geospatial` should enter the running state shortly after
         * this mode is set.
         *
         * Using this mode requires your app do the following, depending on the Runtime:
         *
         * On mobile and projected devices:
         * - Include the
         *   [INTERNET](https://developer.android.com/training/basics/network-ops/connecting)
         *   permission to the app's AndroidManifest
         * - Request and be granted the
         *   [ACCESS_FINE_LOCATION permission](https://developer.android.com/training/location/permissions);
         *   otherwise, [Session.configure] throws [SecurityException].
         *
         * On mobile devices:
         * - Include the Google Play Services Location Library as a dependency for your app. See
         *   [dependencies for Google Play services](https://developers.google.com/android/guides/setup#declare-dependencies)
         *   for instructions on how to include this library in your app. If this library is not
         *   linked, [Session.configure] returns [SessionConfigureLibraryNotLinked].
         *
         * Location is tracked only while the [Session] is resumed.
         *
         * On mobile devices, when the Geospatial API and the Depth API are enabled, output images
         * from the Depth API will include terrain and building geometry when in a location with VPS
         * coverage.
         *
         * Not all devices support GeospatialMode.VPS_AND_GPS, use [Config.ConfigMode.isSupported]
         * to check if the current device and selected camera support enabling this mode. These
         * checks are done in the call to [Session.configure].
         *
         * Supported runtimes:
         * - Play Services (on supported devices)
         * - Projected
         *
         * Required permissions:
         * - [INTERNET][android.Manifest.permission.INTERNET]
         * - [ACCESS_FINE_LOCATION][android.Manifest.permission.ACCESS_FINE_LOCATION]
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val VPS_AND_GPS: GeospatialMode = GeospatialMode(1)
    }
}

/** Feature that allows tracking of the user's eyes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class EyeTrackingMode private constructor(public val mode: Int) : Config.ConfigMode() {
    public companion object {
        /** Eye tracking is disabled. */
        @JvmField public val DISABLED: EyeTrackingMode = EyeTrackingMode(0)
        /**
         * Enables coarse eye tracking, providing general gaze direction without high precision.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions:
         * - [EYE_TRACKING_COARSE][androidx.xr.runtime.manifest.EYE_TRACKING_COARSE]
         */
        @JvmField public val COARSE_TRACKING: EyeTrackingMode = EyeTrackingMode(1)
        /**
         * Enables fine eye tracking, providing more precise gaze direction.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions:
         * - [EYE_TRACKING_FINE][androidx.xr.runtime.manifest.EYE_TRACKING_FINE]
         */
        @JvmField public val FINE_TRACKING: EyeTrackingMode = EyeTrackingMode(2)
    }
}

/** Declare whether the Session should use the world-facing or user-facing camera. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CameraFacingDirection
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode() {
    public companion object {
        /**
         * Use the world-facing camera. This is the default behavior across all devices.
         *
         * Supported runtimes:
         * - Play Services
         *
         * Required permissions:
         * - [CAMERA][android.Manifest.permission.CAMERA]
         */
        @JvmField public val WORLD: CameraFacingDirection = CameraFacingDirection(0)

        /**
         * Use the user-facing camera.
         *
         * Supported runtimes:
         * - Play Services
         *
         * Required permissions:
         * - [CAMERA][android.Manifest.permission.CAMERA]
         */
        @JvmField public val USER: CameraFacingDirection = CameraFacingDirection(1)
    }
}
