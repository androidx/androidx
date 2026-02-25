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
 *   planes. See [androidx.xr.runtime.PlaneTrackingMode].
 * @property handTracking Feature that allows tracking of the user's hands and hand joints. See
 *   [androidx.xr.runtime.HandTrackingMode].
 * @property deviceTracking Feature that allows tracking of the AR device. See
 *   [androidx.xr.runtime.DeviceTrackingMode].
 * @property depthEstimation Feature that allows more accurate information about scene depth and
 *   meshes. See [androidx.xr.runtime.DepthEstimationMode].
 * @property anchorPersistence Feature that allows anchors to be persisted through sessions. See
 *   [androidx.xr.runtime.AnchorPersistenceMode].
 * @property geospatial Feature that allows geospatial localization and tracking. See
 *   [androidx.xr.runtime.GeospatialMode].
 * @property augmentedObjectCategories Feature that allows tracking of recognizable objects in the
 *   environment. See [androidx.xr.runtime.AugmentedObjectCategory].
 */
public class Config
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    public val planeTracking: androidx.xr.runtime.PlaneTrackingMode =
        androidx.xr.runtime.PlaneTrackingMode.DISABLED,
    public val handTracking: androidx.xr.runtime.HandTrackingMode =
        androidx.xr.runtime.HandTrackingMode.DISABLED,
    public val deviceTracking: androidx.xr.runtime.DeviceTrackingMode =
        androidx.xr.runtime.DeviceTrackingMode.DISABLED,
    public val depthEstimation: androidx.xr.runtime.DepthEstimationMode =
        androidx.xr.runtime.DepthEstimationMode.DISABLED,
    public val anchorPersistence: androidx.xr.runtime.AnchorPersistenceMode =
        androidx.xr.runtime.AnchorPersistenceMode.DISABLED,
    public val faceTracking: androidx.xr.runtime.FaceTrackingMode =
        androidx.xr.runtime.FaceTrackingMode.DISABLED,
    public val geospatial: androidx.xr.runtime.GeospatialMode =
        androidx.xr.runtime.GeospatialMode.DISABLED,
    public val augmentedObjectCategories: Set<AugmentedObjectCategory> = setOf(),
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
     *   planes. See [androidx.xr.runtime.PlaneTrackingMode].
     * @param handTracking Feature that allows tracking of the user's hands and hand joints. See
     *   [androidx.xr.runtime.HandTrackingMode].
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [androidx.xr.runtime.DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [androidx.xr.runtime.DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [androidx.xr.runtime.AnchorPersistenceMode].
     * @param faceTracking Feature that allows tracking of human faces. See
     *   [androidx.xr.runtime.FaceTrackingMode].
     * @param geospatial Feature that allows geospatial localization and tracking. See
     *   [androidx.xr.runtime.GeospatialMode].
     * @param augmentedObjectCategories Feature that allows tracking of recognizable objects in the
     *   environment. See [androidx.xr.runtime.AugmentedObjectCategory].
     */
    @JvmOverloads
    public constructor(
        planeTracking: androidx.xr.runtime.PlaneTrackingMode =
            androidx.xr.runtime.PlaneTrackingMode.DISABLED,
        handTracking: androidx.xr.runtime.HandTrackingMode =
            androidx.xr.runtime.HandTrackingMode.DISABLED,
        deviceTracking: androidx.xr.runtime.DeviceTrackingMode =
            androidx.xr.runtime.DeviceTrackingMode.DISABLED,
        depthEstimation: androidx.xr.runtime.DepthEstimationMode =
            androidx.xr.runtime.DepthEstimationMode.DISABLED,
        anchorPersistence: androidx.xr.runtime.AnchorPersistenceMode =
            androidx.xr.runtime.AnchorPersistenceMode.DISABLED,
        faceTracking: androidx.xr.runtime.FaceTrackingMode =
            androidx.xr.runtime.FaceTrackingMode.DISABLED,
        geospatial: androidx.xr.runtime.GeospatialMode =
            androidx.xr.runtime.GeospatialMode.DISABLED,
        augmentedObjectCategories: Set<AugmentedObjectCategory> = setOf(),
    ) : this(
        planeTracking,
        handTracking,
        deviceTracking,
        depthEstimation,
        anchorPersistence,
        faceTracking,
        geospatial,
        augmentedObjectCategories,
        eyeTracking = EyeTrackingMode.DISABLED,
    )

    /**
     * Defines a configuration state of all available features to be set at runtime.
     *
     * An instance of this class should be passed to [Session.configure] to set the current
     * configuration. Use [Config.copy] on [Session.config] to modify a copy of the existing
     * configuration to pass to [Session.configure].
     *
     * @param planeTracking Feature that allows tracking of and provides information about scene
     *   planes. See [Config.PlaneTrackingMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith = ReplaceWith("Config(" + "planeTracking = planeTracking.toNewType(), " + ")"),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode
    ) : this(planeTracking = planeTracking.toNewType(), augmentedObjectCategories = setOf())

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
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
    )

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
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [Config.DeviceTrackingMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    "deviceTracking = deviceTracking.toNewType(), " +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
        deviceTracking: Config.DeviceTrackingMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
        deviceTracking = deviceTracking.toNewType(),
    )

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
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [Config.DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [Config.DepthEstimationMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    "deviceTracking = deviceTracking.toNewType(), " +
                    "depthEstimation = depthEstimation.toNewType(), " +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
        deviceTracking: Config.DeviceTrackingMode,
        depthEstimation: Config.DepthEstimationMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
        deviceTracking = deviceTracking.toNewType(),
        depthEstimation = depthEstimation.toNewType(),
    )

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
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [Config.DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [Config.DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [Config.AnchorPersistenceMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    "deviceTracking = deviceTracking.toNewType(), " +
                    "depthEstimation = depthEstimation.toNewType(), " +
                    "anchorPersistence = anchorPersistence.toNewType(), " +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
        deviceTracking: Config.DeviceTrackingMode,
        depthEstimation: Config.DepthEstimationMode,
        anchorPersistence: Config.AnchorPersistenceMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
        deviceTracking = deviceTracking.toNewType(),
        depthEstimation = depthEstimation.toNewType(),
        anchorPersistence = anchorPersistence.toNewType(),
    )

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
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [Config.DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [Config.DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [Config.AnchorPersistenceMode].
     * @param faceTracking Feature that allows tracking of human faces. See
     *   [Config.FaceTrackingMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    "deviceTracking = deviceTracking.toNewType(), " +
                    "depthEstimation = depthEstimation.toNewType(), " +
                    "anchorPersistence = anchorPersistence.toNewType(), " +
                    "faceTracking = faceTracking.toNewType(), " +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
        deviceTracking: Config.DeviceTrackingMode,
        depthEstimation: Config.DepthEstimationMode,
        anchorPersistence: Config.AnchorPersistenceMode,
        faceTracking: Config.FaceTrackingMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
        deviceTracking = deviceTracking.toNewType(),
        depthEstimation = depthEstimation.toNewType(),
        anchorPersistence = anchorPersistence.toNewType(),
        faceTracking = faceTracking.toNewType(),
    )

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
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [Config.DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [Config.DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [Config.AnchorPersistenceMode].
     * @param faceTracking Feature that allows tracking of human faces. See
     *   [Config.FaceTrackingMode].
     * @param geospatial Feature that allows geospatial localization and tracking. See
     *   [Config.GeospatialMode].
     */
    @Deprecated(
        "Use the constructor with androidx.xr.runtime.* ConfigMode classes instead.",
        replaceWith =
            ReplaceWith(
                "Config(" +
                    "planeTracking = planeTracking.toNewType(), " +
                    "handTracking = handTracking.toNewType(), " +
                    "deviceTracking = deviceTracking.toNewType(), " +
                    "depthEstimation = depthEstimation.toNewType(), " +
                    "anchorPersistence = anchorPersistence.toNewType(), " +
                    "faceTracking = faceTracking.toNewType(), " +
                    "geospatial = geospatial.toNewType()" +
                    ")"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        planeTracking: Config.PlaneTrackingMode,
        handTracking: Config.HandTrackingMode,
        deviceTracking: Config.DeviceTrackingMode,
        depthEstimation: Config.DepthEstimationMode,
        anchorPersistence: Config.AnchorPersistenceMode,
        faceTracking: Config.FaceTrackingMode,
        geospatial: Config.GeospatialMode,
    ) : this(
        planeTracking = planeTracking.toNewType(),
        augmentedObjectCategories = setOf(),
        handTracking = handTracking.toNewType(),
        deviceTracking = deviceTracking.toNewType(),
        depthEstimation = depthEstimation.toNewType(),
        anchorPersistence = anchorPersistence.toNewType(),
        faceTracking = faceTracking.toNewType(),
        geospatial = geospatial.toNewType(),
    )

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
        planeTracking: androidx.xr.runtime.PlaneTrackingMode = this.planeTracking,
        handTracking: androidx.xr.runtime.HandTrackingMode = this.handTracking,
        deviceTracking: androidx.xr.runtime.DeviceTrackingMode = this.deviceTracking,
        depthEstimation: androidx.xr.runtime.DepthEstimationMode = this.depthEstimation,
        anchorPersistence: androidx.xr.runtime.AnchorPersistenceMode = this.anchorPersistence,
    ): Config {
        return Config(
            planeTracking = planeTracking,
            augmentedObjectCategories = this.augmentedObjectCategories,
            handTracking = handTracking,
            deviceTracking = deviceTracking,
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
        planeTracking: androidx.xr.runtime.PlaneTrackingMode = this.planeTracking,
        handTracking: androidx.xr.runtime.HandTrackingMode = this.handTracking,
        deviceTracking: androidx.xr.runtime.DeviceTrackingMode = this.deviceTracking,
        depthEstimation: androidx.xr.runtime.DepthEstimationMode = this.depthEstimation,
        anchorPersistence: androidx.xr.runtime.AnchorPersistenceMode = this.anchorPersistence,
        faceTracking: androidx.xr.runtime.FaceTrackingMode = this.faceTracking,
        geospatial: androidx.xr.runtime.GeospatialMode = this.geospatial,
        augmentedObjectCategories: Set<AugmentedObjectCategory> = this.augmentedObjectCategories,
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
    public abstract class ConfigMode {
        /**
         * Queries whether the [ConfigMode] is supported and is available to be configured for the
         * [session] via [Session.configure]. Attempting to configure this [ConfigMode] if it is not
         * supported will result in [Session.configure] returning [UnsupportedOperationException].
         *
         * @param session the [Session] to check support for.
         * @return true if supported, else false.
         */
        internal fun isSupported(session: Session): Boolean {
            return session.runtimes.map { it.isSupported(this) }.contains(true)
        }
    }

    /** Feature that allows tracking of and provides information about scene planes. */
    @Deprecated(
        "Use androidx.xr.runtime.PlaneTrackingMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.PlaneTrackingMode"),
    )
    public class PlaneTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
        public companion object {
            /** Planes will not be tracked. */
            @JvmField public val DISABLED: PlaneTrackingMode = PlaneTrackingMode(0)
            /**
             * Horizontal and vertical planes will be tracked. Note that setting this mode will
             * consume additional runtime resources.
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.PlaneTrackingMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.PlaneTrackingMode.DISABLED
                HORIZONTAL_AND_VERTICAL ->
                    androidx.xr.runtime.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL
                else -> androidx.xr.runtime.PlaneTrackingMode.DISABLED
            }
    }

    /** Feature that allows tracking of the user's hands and hand joints. */
    @Deprecated(
        "Use androidx.xr.runtime.HandTrackingMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.HandTrackingMode"),
    )
    public class HandTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
        public companion object {
            /** Hands will not be tracked. */
            @JvmField public val DISABLED: HandTrackingMode = HandTrackingMode(0)
            /**
             * Both the left and right hands will be tracked. Note that setting this mode will
             * consume additional runtime resources.
             *
             * Supported runtimes:
             * - OpenXR
             *
             * Required permissions:
             * - [HAND_TRACKING][androidx.xr.runtime.manifest.HAND_TRACKING]
             */
            @JvmField public val BOTH: HandTrackingMode = HandTrackingMode(1)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.HandTrackingMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.HandTrackingMode.DISABLED
                BOTH -> androidx.xr.runtime.HandTrackingMode.BOTH
                else -> androidx.xr.runtime.HandTrackingMode.DISABLED
            }
    }

    /** Feature that allows tracking of the AR device. */
    @Deprecated(
        "Use androidx.xr.runtime.DeviceTrackingMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.DeviceTrackingMode"),
    )
    public class DeviceTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.DeviceTrackingMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.DeviceTrackingMode.DISABLED
                LAST_KNOWN -> androidx.xr.runtime.DeviceTrackingMode.LAST_KNOWN
                else -> androidx.xr.runtime.DeviceTrackingMode.DISABLED
            }
    }

    /** Feature that allows more accurate information about scene depth and meshes. */
    @Deprecated(
        "Use androidx.xr.runtime.DepthEstimationMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.DepthEstimationMode"),
    )
    public class DepthEstimationMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
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
             * Depth estimation will be enabled with both raw and smooth depth and confidence. Note
             * that setting this mode will consume additional runtime resources.
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.DepthEstimationMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.DepthEstimationMode.DISABLED
                RAW_ONLY -> androidx.xr.runtime.DepthEstimationMode.RAW_ONLY
                SMOOTH_ONLY -> androidx.xr.runtime.DepthEstimationMode.SMOOTH_ONLY
                SMOOTH_AND_RAW -> androidx.xr.runtime.DepthEstimationMode.SMOOTH_AND_RAW
                else -> androidx.xr.runtime.DepthEstimationMode.DISABLED
            }
    }

    /** Feature that allows anchors to be persisted through sessions. */
    @Deprecated(
        "Use androidx.xr.runtime.AnchorPersistenceMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.AnchorPersistenceMode"),
    )
    public class AnchorPersistenceMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.AnchorPersistenceMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.AnchorPersistenceMode.DISABLED
                LOCAL -> androidx.xr.runtime.AnchorPersistenceMode.LOCAL
                else -> androidx.xr.runtime.AnchorPersistenceMode.DISABLED
            }
    }

    /**
     * Feature that allows tracking of human faces.
     *
     * Setting this feature to [FaceTrackingMode.BLEND_SHAPES] requires that the `FACE_TRACKING`
     * Android permission is granted by the calling application.
     *
     * Setting this feature to [FaceTrackingMode.MESHES] requires the `CAMERA` Android permission to
     * be granted and that [CameraFacingDirection] is set to [CameraFacingDirection.USER].
     */
    @Deprecated(
        "Use androidx.xr.runtime.FaceTrackingMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.FaceTrackingMode"),
    )
    public class FaceTrackingMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.FaceTrackingMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.FaceTrackingMode.DISABLED
                BLEND_SHAPES -> androidx.xr.runtime.FaceTrackingMode.BLEND_SHAPES
                MESHES -> androidx.xr.runtime.FaceTrackingMode.MESHES
                else -> androidx.xr.runtime.FaceTrackingMode.DISABLED
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
     *   Use `Geospatial.checkVpsAvailability` to determine if a given location has VPS coverage.
     * - In outdoor environments with few or no overhead obstructions, GPS may be sufficient to
     *   generate high accuracy poses. GPS accuracy may be low in dense urban environments and
     *   indoors.
     *
     * Note that setting this mode will consume additional runtime resources.
     */
    @Deprecated(
        "Use androidx.xr.runtime.GeospatialMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.GeospatialMode"),
    )
    public class GeospatialMode
    private constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int
    ) : ConfigMode() {
        @Suppress("DEPRECATION")
        public companion object {
            /**
             * The Geospatial API is disabled. When GeospatialMode is disabled, current `Anchor`
             * objects created from `Geospatial` will stop updating, and have their [TrackingState]
             * set to [TrackingState.STOPPED].
             */
            @JvmField public val DISABLED: GeospatialMode = GeospatialMode(0)

            /**
             * The Geospatial API is enabled. `Geospatial` should enter the running state shortly
             * after this mode is set.
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
             * On mobile devices, when the Geospatial API and the Depth API are enabled, output
             * images from the Depth API will include terrain and building geometry when in a
             * location with VPS coverage.
             *
             * Not all devices support GeospatialMode.VPS_AND_GPS, use [ConfigMode.isSupported] to
             * check if the current device and selected camera support enabling this mode. These
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun toNewType(): androidx.xr.runtime.GeospatialMode =
            when (this) {
                DISABLED -> androidx.xr.runtime.GeospatialMode.DISABLED
                VPS_AND_GPS -> androidx.xr.runtime.GeospatialMode.VPS_AND_GPS
                else -> androidx.xr.runtime.GeospatialMode.DISABLED
            }
    }
}
