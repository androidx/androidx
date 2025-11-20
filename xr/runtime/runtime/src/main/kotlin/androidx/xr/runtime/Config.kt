/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * Defines a configuration state of all available features to be set at runtime.
 *
 * An instance of this class should be passed to [Session.configure] to set the current
 * configuration. Use [Config.copy] on [Session.config] to modify a copy of the existing
 * configuration to pass to [Session.configure].
 *
 * @property planeTracking Feature that allows tracking of and provides information about scene
 *   planes. See [Config.PlaneTrackingMode].
 * @property handTracking Feature that allows tracking of the user's hands and hand joints. See
 *   [Config.HandTrackingMode].
 * @property depthEstimation Feature that allows more accurate information about scene depth and
 *   meshes. See [Config.DepthEstimationMode].
 * @property anchorPersistence Feature that allows anchors to be persisted through sessions. See
 *   [Config.AnchorPersistenceMode].
 * @property geospatial Feature that allows geospatial localization and tracking. See
 *   [Config.GeospatialMode].
 */
public class Config
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    public val planeTracking: PlaneTrackingMode = PlaneTrackingMode.DISABLED,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val augmentedObjectCategories: List<AugmentedObjectCategory> = listOf(),
    public val handTracking: HandTrackingMode = HandTrackingMode.DISABLED,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val deviceTracking: DeviceTrackingMode = DeviceTrackingMode.DISABLED,
    public val depthEstimation: DepthEstimationMode = DepthEstimationMode.DISABLED,
    public val anchorPersistence: AnchorPersistenceMode = AnchorPersistenceMode.DISABLED,
    public val faceTracking: FaceTrackingMode = FaceTrackingMode.DISABLED,
    public val geospatial: GeospatialMode = GeospatialMode.DISABLED,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val eyeTracking: EyeTrackingMode = EyeTrackingMode.DISABLED,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val cameraFacingDirection: CameraFacingDirection = CameraFacingDirection.WORLD,
) {

    /**
     * Defines a configuration state of all available features to be set at runtime.
     *
     * An instance of this class should be passed to [Session.configure] to set the current
     * configuration. Use [Config.copy] on [Session.config] to modify a copy of the existing
     * configuration to pass to [Session.configure].
     *
     * @param planeTracking Feature that allows tracking of and provides information about scene
     *   planes. See [Config.PlaneTrackingMode].
     * @param handTracking Feature that allows tracking of the user's hands and hand joints. See
     *   [Config.HandTrackingMode].
     * @param headTracking Feature that allows tracking of the user's head position. See
     *   [Config.HeadTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [Config.DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [Config.AnchorPersistenceMode].
     * @param faceTracking Feature that allows tracking of human faces. See
     *   [Config.FaceTrackingMode].
     * @param geospatial Feature that allows geospatial localization and tracking. See
     *   [Config.GeospatialMode].
     */
    @JvmOverloads
    public constructor(
        planeTracking: PlaneTrackingMode = PlaneTrackingMode.DISABLED,
        handTracking: HandTrackingMode = HandTrackingMode.DISABLED,
        headTracking: HeadTrackingMode = HeadTrackingMode.DISABLED,
        depthEstimation: DepthEstimationMode = DepthEstimationMode.DISABLED,
        anchorPersistence: AnchorPersistenceMode = AnchorPersistenceMode.DISABLED,
        faceTracking: FaceTrackingMode = FaceTrackingMode.DISABLED,
        geospatial: GeospatialMode = GeospatialMode.DISABLED,
    ) : this(
        planeTracking,
        /* augmentedObjectCategories= */ listOf(),
        handTracking,
        headTracking.toDeviceTrackingMode(),
        depthEstimation,
        anchorPersistence,
        faceTracking,
        geospatial,
        eyeTracking = EyeTrackingMode.DISABLED,
    )

    /** Feature that allows tracking of the user's head position. See [Config.HeadTrackingMode]. */
    public val headTracking: HeadTrackingMode = deviceTracking.toHeadTrackingMode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Config) return false

        if (planeTracking != other.planeTracking) return false
        if (handTracking != other.handTracking) return false
        if (deviceTracking != other.deviceTracking) return false
        if (depthEstimation != other.depthEstimation) return false
        if (anchorPersistence != other.anchorPersistence) return false
        if (faceTracking != other.faceTracking) return false
        if (geospatial != other.geospatial) return false
        if (augmentedObjectCategories != other.augmentedObjectCategories) return false
        if (eyeTracking != other.eyeTracking) return false
        if (cameraFacingDirection != other.cameraFacingDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = planeTracking.hashCode()
        result = 31 * result + handTracking.hashCode()
        result = 31 * result + deviceTracking.hashCode()
        result = 31 * result + depthEstimation.hashCode()
        result = 31 * result + anchorPersistence.hashCode()
        result = 31 * result + faceTracking.hashCode()
        result = 31 * result + geospatial.hashCode()
        result = 31 * result + augmentedObjectCategories.hashCode()
        result = 31 * result + eyeTracking.hashCode()
        result = 31 * result + cameraFacingDirection.hashCode()
        return result
    }

    @JvmOverloads
    public fun copy(
        planeTracking: PlaneTrackingMode = this.planeTracking,
        handTracking: HandTrackingMode = this.handTracking,
        headTracking: HeadTrackingMode = this.headTracking,
        depthEstimation: DepthEstimationMode = this.depthEstimation,
        anchorPersistence: AnchorPersistenceMode = this.anchorPersistence,
    ): Config {
        return Config(
            planeTracking = planeTracking,
            augmentedObjectCategories = this.augmentedObjectCategories,
            handTracking = handTracking,
            deviceTracking = headTracking.toDeviceTrackingMode(),
            depthEstimation = depthEstimation,
            anchorPersistence = anchorPersistence,
            faceTracking = this.faceTracking,
            geospatial = this.geospatial,
            eyeTracking = this.eyeTracking,
            cameraFacingDirection = this.cameraFacingDirection,
        )
    }

    @Suppress("MissingJvmstatic")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun copy(
        planeTracking: PlaneTrackingMode = this.planeTracking,
        handTracking: HandTrackingMode = this.handTracking,
        deviceTracking: DeviceTrackingMode = this.deviceTracking,
        depthEstimation: DepthEstimationMode = this.depthEstimation,
        anchorPersistence: AnchorPersistenceMode = this.anchorPersistence,
        faceTracking: FaceTrackingMode = this.faceTracking,
        geospatial: GeospatialMode = this.geospatial,
        augmentedObjectCategories: List<AugmentedObjectCategory> = this.augmentedObjectCategories,
        eyeTracking: EyeTrackingMode = this.eyeTracking,
        cameraFacingDirection: CameraFacingDirection = this.cameraFacingDirection,
    ): Config {
        return Config(
            planeTracking = planeTracking,
            augmentedObjectCategories = augmentedObjectCategories,
            handTracking = handTracking,
            deviceTracking = deviceTracking,
            depthEstimation = depthEstimation,
            anchorPersistence = anchorPersistence,
            faceTracking = faceTracking,
            geospatial = geospatial,
            eyeTracking = eyeTracking,
            cameraFacingDirection = cameraFacingDirection,
        )
    }

    /** Describes a specific value used to set the configuration via [Session.configure]. */
    public interface ConfigMode {
        /**
         * Queries whether the [ConfigMode] is supported and is available to be configured for the
         * [session] via [Session.configure]. Attempting to configure this [ConfigMode] if it is not
         * supported will result in [Session.configure] returning [UnsupportedOperationException].
         *
         * @param session the [Session] to check support for.
         * @return true if supported, else false.
         */
        public fun isSupported(session: Session): Boolean {
            return session.runtimes.map { it.isSupported(this) }.contains(true)
        }
    }

    /**
     * Feature that allows tracking of and provides information about scene planes.
     *
     * Setting this feature to [PlaneTrackingMode.HORIZONTAL_AND_VERTICAL] requires that the
     * `SCENE_UNDERSTANDING_COARSE` Android permission is granted.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class PlaneTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** Planes will not be tracked. */
            @JvmField public val DISABLED: PlaneTrackingMode = PlaneTrackingMode(0)
            /**
             * Horizontal and vertical planes will be tracked. Note that setting this mode will
             * consume additional runtime resources.
             */
            @JvmField public val HORIZONTAL_AND_VERTICAL: PlaneTrackingMode = PlaneTrackingMode(1)
        }

        override fun toString(): String {
            return "PlaneTracking_" + if (mode == 0) "DISABLED" else "HORIZONTAL_AND_VERTICAL"
        }
    }

    /**
     * Feature that allows tracking of the user's hands and hand joints.
     *
     * Setting this feature to [HandTrackingMode.BOTH] requires that the `HAND_TRACKING` Android
     * permission is granted by the calling application.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class HandTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** Hands will not be tracked. */
            @JvmField public val DISABLED: HandTrackingMode = HandTrackingMode(0)
            /**
             * Both the left and right hands will be tracked. Note that setting this mode will
             * consume additional runtime resources.
             */
            @JvmField public val BOTH: HandTrackingMode = HandTrackingMode(1)
        }

        override fun toString(): String {
            return "HandTracking_" + if (mode == 0) "DISABLED" else "BOTH"
        }
    }

    /**
     * Feature that allows tracking of the AR device.
     *
     * This feature does not require any additional application permissions.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class DeviceTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /**
             * The device pose will not be tracked. In this mode,
             * [androidx.xr.arcore.RenderViewpoint] will not emit updates to
             * [androidx.xr.arcore.RenderViewpoint.State.pose].
             */
            @JvmField public val DISABLED: DeviceTrackingMode = DeviceTrackingMode(0)
            /**
             * The device pose will be tracked and the last known pose from the system at the time
             * of runtime update will be provided. Note that there is generally a delay between the
             * actual device pose and the pose provided by the system by the time of the update.
             */
            @JvmField public val LAST_KNOWN: DeviceTrackingMode = DeviceTrackingMode(1)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toHeadTrackingMode(): HeadTrackingMode {
            return if (mode == 0) HeadTrackingMode.DISABLED else HeadTrackingMode.LAST_KNOWN
        }

        override fun toString(): String {
            return "DeviceTracking_" + if (mode == 0) "DISABLED" else "LAST_KNOWN"
        }
    }

    /**
     * Feature that allows tracking of the user's head pose.
     *
     * Setting this feature to [HeadTrackingMode.LAST_KNOWN] requires that the `HEAD_TRACKING`
     * Android permission is granted by the calling application.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class HeadTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** The head pose is not updated. It remains at the origin (an identity pose). */
            @JvmField public val DISABLED: HeadTrackingMode = HeadTrackingMode(0)
            /**
             * Head pose will be tracked and the last known pose from the system at the time of
             * runtime update will be provided. Note that there is generally a delay between the
             * actual head pose and the pose provided by the system by the time of the update.
             */
            @JvmField public val LAST_KNOWN: HeadTrackingMode = HeadTrackingMode(1)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toDeviceTrackingMode(): DeviceTrackingMode {
            return if (mode == 0) DeviceTrackingMode.DISABLED else DeviceTrackingMode.LAST_KNOWN
        }

        override fun toString(): String {
            return "HeadTracking_" + if (mode == 0) "DISABLED" else "LAST_KNOWN"
        }
    }

    /**
     * Feature that allows more accurate information about scene depth and meshes.
     *
     * Setting this feature to any of [DepthEstimationMode.RAW_ONLY],
     * [DepthEstimationMode.SMOOTH_ONLY] or [DepthEstimationMode.SMOOTH_AND_RAW] requires that the
     * `SCENE_UNDERSTANDING_FINE` Android permission is granted by the calling application.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class DepthEstimationMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** No information about scene depth will be provided. */
            @JvmField public val DISABLED: DepthEstimationMode = DepthEstimationMode(0)

            /** Depth estimation will be enabled with raw depth and confidence. */
            @JvmField public val RAW_ONLY: DepthEstimationMode = DepthEstimationMode(1)

            /** Depth estimation will be enabled with smooth depth and confidence. */
            @JvmField public val SMOOTH_ONLY: DepthEstimationMode = DepthEstimationMode(2)

            /**
             * Depth estimation will be enabled with both raw and smooth depth and confidence. Note
             * that setting this mode will consume additional runtime resources.
             */
            @JvmField public val SMOOTH_AND_RAW: DepthEstimationMode = DepthEstimationMode(3)
        }

        override fun toString(): String {
            return "DepthEstimation_" +
                when (mode) {
                    0 -> "DISABLED"
                    1 -> "RAW_ONLY"
                    2 -> "SMOOTH_ONLY"
                    3 -> "SMOOTH_AND_RAW"
                    else -> "UNKNOWN"
                }
        }
    }

    /**
     * Feature that allows anchors to be persisted through sessions.
     *
     * This feature does not require any additional application permissions.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class AnchorPersistenceMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** Anchors cannot be persisted. */
            @JvmField public val DISABLED: AnchorPersistenceMode = AnchorPersistenceMode(0)
            /** Anchors may be persisted and will be saved in the application's local storage. */
            @JvmField public val LOCAL: AnchorPersistenceMode = AnchorPersistenceMode(1)
        }

        override fun toString(): String {
            return "AnchorPersistence_" + if (mode == 0) "DISABLED" else "LOCAL"
        }
    }

    /**
     * Feature that allows tracking of human faces.
     *
     * Setting this feature to [FaceTrackingMode.USER] requires that the `FACE_TRACKING` Android
     * permission is granted by the calling application.
     *
     * Setting this feature to [FaceTrackingMode.MESHES] requires the `CAMERA` Android permission to
     * be granted and that [CameraFacingDirection] is set to [CameraFacingDirection.USER].
     */
    @SuppressWarnings("HiddenSuperclass")
    public class FaceTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /** Faces will not be tracked. */
            @JvmField public val DISABLED: FaceTrackingMode = FaceTrackingMode(0)

            // TODO b/451663642: Rename Config.FaceTrackingMode.USER to better reflect its use case
            /** Blend shapes of the user's face will be tracked. */
            @JvmField public val USER: FaceTrackingMode = FaceTrackingMode(1)

            /** Face meshes will be tracked using the front-facing camera. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            @JvmField
            public val MESHES: FaceTrackingMode = FaceTrackingMode(2)
        }
    }

    /**
     * Feature that allows Geospatial localization and tracking. The Geospatial API uses a
     * combination of Google's Visual Positioning System (VPS) and GPS to determine the geospatial
     * pose.
     *
     * The Geospatial API is able to provide the best user experience when it is able to generate
     * high accuracy poses. However, the Geospatial API can be used anywhere, as long as the device
     * is able to determine its location, even if the available location information has low
     * accuracy.
     * - In areas with VPS coverage, the Geospatial API is able to generate high accuracy poses.
     *   This can work even where GPS accuracy is low, such as dense urban environments. Under
     *   typical conditions, VPS can be expected to provide positional accuracy typically better
     *   than 5 meters and often around 1 meter, and a rotational accuracy of better than 5 degrees.
     *   Use [Geospatial.checkVpsAvailability] to determine if a given location has VPS coverage.
     * - In outdoor environments with few or no overhead obstructions, GPS may be sufficient to
     *   generate high accuracy poses. GPS accuracy may be low in dense urban environments and
     *   indoors.
     *
     * Note that setting this mode will consume additional runtime resources.
     */
    @SuppressWarnings("HiddenSuperclass")
    public class GeospatialMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode {
        public companion object {
            /**
             * The Geospatial API is disabled. When GeospatialMode is disabled, current [Anchor]
             * objects created from [Geospatial] will stop updating, and have their [TrackingState]
             * set to [TrackingState.STOPPED].
             */
            @JvmField public val DISABLED: GeospatialMode = GeospatialMode(0)

            /**
             * The Geospatial API is enabled. [Geospatial] should enter the running state shortly
             * after this mode is set.
             *
             * Using this mode requires your app do the following, depending on the Runtime:
             *
             * On mobile and projected devices:
             * - Include the
             *   [ACCESS_INTERNET](https://developer.android.com/training/basics/network-ops/connecting)
             *   permission to the app's AndroidManifest
             * - Request and be granted the
             *   [ACCESS_FINE_LOCATION permission](https://developer.android.com/training/location/permissions);
             *   otherwise, [Session.configure] throws [SecurityException].
             *
             * On mobile devices:
             * - Include the Google Play Services Location Library as a dependency for your app. See
             *   [dependencies for Google Play services](https://developers.google.com/android/guides/setup#declare-dependencies)
             *   for instructions on how to include this library in your app. If this library is not
             *   linked, [Session.configure] returns
             *   [SessionResultGooglePlayServicesLocationLibraryNotLinked].
             *
             * Location is tracked only while the [Session] is resumed.
             *
             * On mobile devices, when the Geospatial API and the Depth API are enabled, output
             * images from the Depth API will include terrain and building geometry when in a
             * location with VPS coverage.
             *
             * Not all devices support GeospatialMode.VPS_AND_GPS, use [ConfigMode.isSupported] to
             * check if the current device and selected camera support enabling this mode. These
             * checks are done in the call to [Session.configure].
             */
            @JvmField public val VPS_AND_GPS: GeospatialMode = GeospatialMode(1)
        }

        override fun toString(): String {
            return "Geospatial_" + if (mode == 0) "DISABLED" else "VPS_AND_GPS"
        }
    }

    /**
     * Feature that allows tracking of the user's eyes.
     *
     * Setting this feature to any mode other than [EyeTrackingMode.DISABLED] requires that the
     * `EYE_TRACKING` Android permission is granted by the calling application.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @SuppressWarnings("HiddenSuperclass")
    public class EyeTrackingMode private constructor(public val mode: Int) : ConfigMode {
        public companion object {
            /** Eye tracking is disabled. */
            @JvmField public val DISABLED: EyeTrackingMode = EyeTrackingMode(0)
            /**
             * Enables coarse eye tracking, providing general gaze direction without high precision.
             */
            @JvmField public val COARSE_TRACKING: EyeTrackingMode = EyeTrackingMode(1)
            /** Enables fine eye tracking, providing more precise gaze direction. */
            @JvmField public val FINE_TRACKING: EyeTrackingMode = EyeTrackingMode(2)
        }
    }

    /** Declare whether the Session should use the world-facing or user-facing camera. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public class CameraFacingDirection
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) {
        public companion object {
            /** Use the world-facing camera. This is the default behavior across all devices. */
            @JvmField public val WORLD: CameraFacingDirection = CameraFacingDirection(0)

            /** Use the user-facing camera. */
            @JvmField public val USER: CameraFacingDirection = CameraFacingDirection(1)
        }
    }
}
