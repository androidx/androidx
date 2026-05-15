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
 * @property augmentedImageDatabase The current active [AugmentedImageDatabase]. If not empty, the
 *   image tracking feature will be enabled.
 */
public class Config
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    public val planeTracking: PlaneTrackingMode = PlaneTrackingMode.DISABLED,
    public val handTracking: HandTrackingMode = HandTrackingMode.DISABLED,
    public val deviceTracking: DeviceTrackingMode = DeviceTrackingMode.DISABLED,
    public val depthEstimation: DepthEstimationMode = DepthEstimationMode.DISABLED,
    public val anchorPersistence: AnchorPersistenceMode = AnchorPersistenceMode.DISABLED,
    public val faceTracking: FaceTrackingMode = FaceTrackingMode.DISABLED,
    public val geospatial: GeospatialMode = GeospatialMode.DISABLED,
    public val augmentedObjectCategories: Set<AugmentedObjectCategory> = setOf(),
    public val eyeTracking: EyeTrackingMode = EyeTrackingMode.DISABLED,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val cameraFacingDirection: CameraFacingDirection = CameraFacingDirection.WORLD,
    public val augmentedImageDatabase: AugmentedImageDatabase? = null,
) {

    @OptIn(PreviewSpatialApi::class)
    private var _sceneSignalTypes: Set<SceneSignalType> = emptySet()

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
     * @param augmentedImageDatabase Feature that allows tracking of recognizable images in the
     *   environment. See [AugmentedImageDatabase].
     * @param eyeTracking Feature that allows tracking of user eye movements. See [EyeTrackingMode].
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
        augmentedImageDatabase: AugmentedImageDatabase? = null,
        eyeTracking: EyeTrackingMode = EyeTrackingMode.DISABLED,
    ) : this(
        planeTracking,
        handTracking,
        deviceTracking,
        depthEstimation,
        anchorPersistence,
        faceTracking,
        geospatial,
        augmentedObjectCategories,
        eyeTracking,
        CameraFacingDirection.WORLD,
        augmentedImageDatabase,
    )

    /**
     * Creates a new [Config] from an existing [Config], updating the [sceneSignalTypes].
     *
     * @param config The existing configuration to copy.
     * @param sceneSignalTypes The set of scene signal types to enable for this session. See
     *   [SceneSignalType]. Setting this to an empty set (default) will disable all scene signal
     *   types.
     */
    @PreviewSpatialApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        config: Config,
        sceneSignalTypes: Set<SceneSignalType>,
    ) : this(
        planeTracking = config.planeTracking,
        handTracking = config.handTracking,
        deviceTracking = config.deviceTracking,
        depthEstimation = config.depthEstimation,
        anchorPersistence = config.anchorPersistence,
        faceTracking = config.faceTracking,
        geospatial = config.geospatial,
        augmentedObjectCategories = config.augmentedObjectCategories,
        eyeTracking = config.eyeTracking,
        cameraFacingDirection = config.cameraFacingDirection,
        augmentedImageDatabase = config.augmentedImageDatabase,
    ) {
        this._sceneSignalTypes = sceneSignalTypes
    }

    @OptIn(PreviewSpatialApi::class)
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
        if (augmentedImageDatabase != other.augmentedImageDatabase) return false
        if (_sceneSignalTypes != other.getSceneSignalTypes()) return false

        return true
    }

    @OptIn(PreviewSpatialApi::class)
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
        result = 31 * result + augmentedImageDatabase.hashCode()
        result = 31 * result + _sceneSignalTypes.hashCode()
        return result
    }

    @PreviewSpatialApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getSceneSignalTypes(): Set<SceneSignalType> = _sceneSignalTypes

    @JvmOverloads
    @OptIn(PreviewSpatialApi::class)
    public fun copy(
        planeTracking: PlaneTrackingMode = this.planeTracking,
        handTracking: HandTrackingMode = this.handTracking,
        deviceTracking: DeviceTrackingMode = this.deviceTracking,
        depthEstimation: DepthEstimationMode = this.depthEstimation,
        anchorPersistence: AnchorPersistenceMode = this.anchorPersistence,
        faceTracking: FaceTrackingMode = this.faceTracking,
        geospatial: GeospatialMode = this.geospatial,
        augmentedObjectCategories: Set<AugmentedObjectCategory> = this.augmentedObjectCategories,
        augmentedImageDatabase: AugmentedImageDatabase? = this.augmentedImageDatabase,
    ): Config {
        val newConfig =
            Config(
                planeTracking = planeTracking,
                handTracking = handTracking,
                deviceTracking = deviceTracking,
                depthEstimation = depthEstimation,
                anchorPersistence = anchorPersistence,
                faceTracking = faceTracking,
                geospatial = geospatial,
                augmentedObjectCategories = augmentedObjectCategories,
                eyeTracking = this.eyeTracking,
                cameraFacingDirection = this.cameraFacingDirection,
                augmentedImageDatabase = augmentedImageDatabase,
            )
        newConfig._sceneSignalTypes = this._sceneSignalTypes
        return newConfig
    }

    @Suppress("MissingJvmstatic")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(PreviewSpatialApi::class)
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
        augmentedImageDatabase: AugmentedImageDatabase? = this.augmentedImageDatabase,
    ): Config {
        val newConfig =
            Config(
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
                augmentedImageDatabase = augmentedImageDatabase,
            )
        newConfig._sceneSignalTypes = this._sceneSignalTypes
        return newConfig
    }
}
