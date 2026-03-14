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
    public val planeTracking: PlaneTrackingMode = PlaneTrackingMode.DISABLED,
    public val handTracking: HandTrackingMode = HandTrackingMode.DISABLED,
    public val deviceTracking: DeviceTrackingMode = DeviceTrackingMode.DISABLED,
    public val depthEstimation: DepthEstimationMode = DepthEstimationMode.DISABLED,
    public val anchorPersistence: AnchorPersistenceMode = AnchorPersistenceMode.DISABLED,
    public val faceTracking: FaceTrackingMode = FaceTrackingMode.DISABLED,
    public val geospatial: GeospatialMode = GeospatialMode.DISABLED,
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
     *   planes. See [PlaneTrackingMode].
     * @param handTracking Feature that allows tracking of the user's hands and hand joints. See
     *   [HandTrackingMode].
     * @param deviceTracking Feature that allows tracking of the AR device. See
     *   [DeviceTrackingMode].
     * @param depthEstimation Feature that allows more accurate information about scene depth and
     *   meshes. See [DepthEstimationMode].
     * @param anchorPersistence Feature that allows anchors to be persisted through sessions. See
     *   [AnchorPersistenceMode].
     * @param faceTracking Feature that allows tracking of human faces. See [FaceTrackingMode].
     * @param geospatial Feature that allows geospatial localization and tracking. See
     *   [GeospatialMode].
     * @param augmentedObjectCategories Feature that allows tracking of recognizable objects in the
     *   environment. See [AugmentedObjectCategory].
     */
    @JvmOverloads
    public constructor(
        planeTracking: PlaneTrackingMode = PlaneTrackingMode.DISABLED,
        handTracking: HandTrackingMode = HandTrackingMode.DISABLED,
        deviceTracking: DeviceTrackingMode = DeviceTrackingMode.DISABLED,
        depthEstimation: DepthEstimationMode = DepthEstimationMode.DISABLED,
        anchorPersistence: AnchorPersistenceMode = AnchorPersistenceMode.DISABLED,
        faceTracking: FaceTrackingMode = FaceTrackingMode.DISABLED,
        geospatial: GeospatialMode = GeospatialMode.DISABLED,
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
        deviceTracking: DeviceTrackingMode = this.deviceTracking,
        depthEstimation: DepthEstimationMode = this.depthEstimation,
        anchorPersistence: AnchorPersistenceMode = this.anchorPersistence,
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
        planeTracking: PlaneTrackingMode = this.planeTracking,
        handTracking: HandTrackingMode = this.handTracking,
        deviceTracking: DeviceTrackingMode = this.deviceTracking,
        depthEstimation: DepthEstimationMode = this.depthEstimation,
        anchorPersistence: AnchorPersistenceMode = this.anchorPersistence,
        faceTracking: FaceTrackingMode = this.faceTracking,
        geospatial: GeospatialMode = this.geospatial,
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
}
