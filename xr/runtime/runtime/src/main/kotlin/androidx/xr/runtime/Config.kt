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
 * configuration. Use [Builder] to specify individual configuration settings, and use
 * [Builder.build] to create an instance of [Config] to pass to [Session.configure].
 *
 * @property planeTracking Feature that allows tracking of and provides information about scene
 *   planes. See [PlaneTrackingMode].
 * @property handTracking Feature that allows tracking of the user's hands and hand joints. See
 *   [HandTrackingMode].
 * @property deviceTracking Feature that allows tracking of the AR device. See [DeviceTrackingMode].
 * @property depthEstimation Feature that allows more accurate information about scene depth and
 *   meshes. See [DepthEstimationMode].
 * @property anchorPersistence Feature that allows anchors to be persisted through sessions. See
 *   [AnchorPersistenceMode].
 * @property faceTracking Feature that allows the tracking of human faces. See [FaceTrackingMode].
 * @property geospatial Feature that allows geospatial localization and tracking. See
 *   [GeospatialMode].
 * @property augmentedObjectCategories Feature that allows tracking of recognizable objects in the
 *   environment. See [AugmentedObjectCategory].
 * @property eyeTracking Feature that allows tracking of the users gaze direction. See
 *   [EyeTrackingMode].
 * @property augmentedImageDatabase The current active [AugmentedImageDatabase]. If not empty, the
 *   image tracking feature will be enabled.
 * @property qrCodeTracking Feature that allows tracking of and provides information about QR codes.
 *   See [QrCodeTrackingMode].
 * @property qrCodeSizeMeters The physical size in meters of the QR code. If zero, the physical size
 *   will be estimated if the device supports it. If physical size estimation is not supported,
 *   configuring the [Session] adding an entry with qrCodeSizeMeters being 0f or lower will throw an
 *   [IllegalStateException]. It requires [qrCodeTracking] to be different from
 *   [QrCodeTrackingMode.DISABLED].
 */
public class Config
private constructor(
    public val planeTracking: PlaneTrackingMode,
    public val handTracking: HandTrackingMode,
    public val deviceTracking: DeviceTrackingMode,
    public val depthEstimation: DepthEstimationMode,
    public val anchorPersistence: AnchorPersistenceMode,
    public val faceTracking: FaceTrackingMode,
    public val geospatial: GeospatialMode,
    public val augmentedObjectCategories: Set<AugmentedObjectCategory>,
    public val eyeTracking: EyeTrackingMode,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val cameraFacingDirection: CameraFacingDirection,
    public val augmentedImageDatabase: AugmentedImageDatabase?,
    public val qrCodeTracking: QrCodeTrackingMode = QrCodeTrackingMode.DISABLED,
    public val qrCodeSizeMeters: Float = 0f,
    @OptIn(PreviewSpatialApi::class) private val sceneSignalTypes: Set<SceneSignalType>,
) {
    // TODO(b/513553206) - Remove this constructor when 1P apps are migrated to use Config.Builder.
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(PreviewSpatialApi::class)
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
        qrCodeTracking: QrCodeTrackingMode = QrCodeTrackingMode.DISABLED,
        qrCodeSizeMeters: Float = 0f,
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
        qrCodeTracking,
        qrCodeSizeMeters,
        sceneSignalTypes = setOf(),
    )

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
        if (qrCodeTracking != other.qrCodeTracking) return false
        if (qrCodeSizeMeters != other.qrCodeSizeMeters) return false
        if (sceneSignalTypes != other.getSceneSignalTypes()) return false

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
        result = 31 * result + qrCodeTracking.hashCode()
        result = 31 * result + qrCodeSizeMeters.hashCode()
        result = 31 * result + sceneSignalTypes.hashCode()
        return result
    }

    @PreviewSpatialApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getSceneSignalTypes(): Set<SceneSignalType> = sceneSignalTypes

    /**
     * This class can be used to create a [Config] instance.
     *
     * Apps can create a default [Builder] object and then call the appropriate setter methods on
     * the builder to specify any non-default settings. Default settings for each configuration
     * parameter are specified for each setter method. Setters return the builder object so that
     * setter methods can be chained. [Builder.build] can be used to create a [Config] with the
     * configuration specified in the builder.
     */
    public class Builder
    internal constructor(
        private var planeTracking: PlaneTrackingMode,
        private var handTracking: HandTrackingMode,
        private var deviceTracking: DeviceTrackingMode,
        private var depthEstimation: DepthEstimationMode,
        private var anchorPersistence: AnchorPersistenceMode,
        private var faceTracking: FaceTrackingMode,
        private var geospatial: GeospatialMode,
        private var augmentedObjectCategories: Set<AugmentedObjectCategory>,
        private var eyeTracking: EyeTrackingMode,
        private var cameraFacingDirection: CameraFacingDirection,
        private var augmentedImageDatabase: AugmentedImageDatabase?,
        private var qrCodeTracking: QrCodeTrackingMode,
        private var qrCodeSizeMeters: Float,
        @OptIn(PreviewSpatialApi::class) private var sceneSignalTypes: Set<SceneSignalType>,
    ) {

        /** Creates a [Builder] instance for a [Config] with default values. */
        @OptIn(PreviewSpatialApi::class)
        public constructor() :
            this(
                planeTracking = PlaneTrackingMode.DISABLED,
                handTracking = HandTrackingMode.DISABLED,
                deviceTracking = DeviceTrackingMode.DISABLED,
                depthEstimation = DepthEstimationMode.DISABLED,
                anchorPersistence = AnchorPersistenceMode.DISABLED,
                faceTracking = FaceTrackingMode.DISABLED,
                geospatial = GeospatialMode.DISABLED,
                augmentedObjectCategories = setOf(),
                eyeTracking = EyeTrackingMode.DISABLED,
                cameraFacingDirection = CameraFacingDirection.WORLD,
                augmentedImageDatabase = null,
                qrCodeTracking = QrCodeTrackingMode.DISABLED,
                qrCodeSizeMeters = 0f,
                sceneSignalTypes = emptySet(),
            )

        /**
         * Creates a [Builder] instance with the same configuration settings as the provided
         * [Config].
         *
         * @param config the configuration for the [Builder] instance
         */
        @OptIn(PreviewSpatialApi::class)
        public constructor(
            config: Config
        ) : this(
            config.planeTracking,
            config.handTracking,
            config.deviceTracking,
            config.depthEstimation,
            config.anchorPersistence,
            config.faceTracking,
            config.geospatial,
            config.augmentedObjectCategories,
            config.eyeTracking,
            config.cameraFacingDirection,
            config.augmentedImageDatabase,
            config.qrCodeTracking,
            config.qrCodeSizeMeters,
            config.sceneSignalTypes,
        )

        /**
         * Sets the [PlaneTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [PlaneTrackingMode.DISABLED].
         *
         * @param planeTracking [PlaneTrackingMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied plane tracking mode
         */
        public fun setPlaneTracking(planeTracking: PlaneTrackingMode): Builder {
            this.planeTracking = planeTracking
            return this
        }

        /**
         * Sets the [HandTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [HandTrackingMode.DISABLED].
         *
         * @param handTracking [HandTrackingMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied hand tracking mode
         */
        public fun setHandTracking(handTracking: HandTrackingMode): Builder {
            this.handTracking = handTracking
            return this
        }

        /**
         * Sets the [DeviceTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [DeviceTrackingMode.DISABLED].
         *
         * @param deviceTracking [DeviceTrackingMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied device tracking mode
         */
        public fun setDeviceTracking(deviceTracking: DeviceTrackingMode): Builder {
            this.deviceTracking = deviceTracking
            return this
        }

        /**
         * Sets the [DepthEstimationMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [DepthEstimationMode.DISABLED].
         *
         * @param depthEstimation [DepthEstimationMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied depth estimation mode
         */
        public fun setDepthEstimation(depthEstimation: DepthEstimationMode): Builder {
            this.depthEstimation = depthEstimation
            return this
        }

        /**
         * Sets the [AnchorPersistenceMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [AnchorPersistenceMode.DISABLED].
         *
         * @param anchorPersistence [AnchorPersistenceMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied anchor persistence mode
         */
        public fun setAnchorPersistence(anchorPersistence: AnchorPersistenceMode): Builder {
            this.anchorPersistence = anchorPersistence
            return this
        }

        /**
         * Sets the [FaceTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [FaceTrackingMode.DISABLED].
         *
         * @param faceTracking [FaceTrackingMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied face tracking mode
         */
        public fun setFaceTracking(faceTracking: FaceTrackingMode): Builder {
            this.faceTracking = faceTracking
            return this
        }

        /**
         * Sets the [GeospatialMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [GeospatialMode.DISABLED].
         *
         * @param geospatial [GeospatialMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied geospatial mode
         */
        public fun setGeospatial(geospatial: GeospatialMode): Builder {
            this.geospatial = geospatial
            return this
        }

        /**
         * Sets the augmented object categories this [Builder] instance will use to build a
         * [Config].
         *
         * The default value is an empty set.
         *
         * @param augmentedObjectCategories [AugmentedObjectCategory] set to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied [AugmentedObjectCategory]
         *   set
         */
        public fun setAugmentedObjectCategories(
            augmentedObjectCategories: Set<AugmentedObjectCategory>
        ): Builder {
            this.augmentedObjectCategories = augmentedObjectCategories
            return this
        }

        /**
         * Sets the [EyeTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [EyeTrackingMode.DISABLED].
         *
         * @param eyeTracking [EyeTrackingMode] value to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied eye tracking mode
         */
        public fun setEyeTracking(eyeTracking: EyeTrackingMode): Builder {
            this.eyeTracking = eyeTracking
            return this
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setCameraFacingDirection(cameraFacingDirection: CameraFacingDirection): Builder {
            this.cameraFacingDirection = cameraFacingDirection
            return this
        }

        /**
         * Sets the [AugmentedImageDatabase] this [Builder] instance will use to build a [Config].
         *
         * The default value is null.
         *
         * @param augmentedImageDatabase Nullable [AugmentedImageDatabase] value to configure the
         *   [Session]
         * @return a [Builder] that builds a [Config] with the supplied augmented image database
         */
        public fun setAugmentedImageDatabase(
            augmentedImageDatabase: AugmentedImageDatabase?
        ): Builder {
            this.augmentedImageDatabase = augmentedImageDatabase
            return this
        }

        /**
         * Sets the [QrCodeTrackingMode] this [Builder] instance will use to build a [Config].
         *
         * The default value is [QrCodeTrackingMode.DISABLED].
         *
         * @param qrCodeTracking [QrCodeTrackingMode] value configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied QR code tracking mode
         */
        public fun setQrCodeTracking(qrCodeTracking: QrCodeTrackingMode): Builder {
            this.qrCodeTracking = qrCodeTracking
            return this
        }

        /**
         * Sets the QR code size this [Builder] instance will use to build a [Config].
         *
         * The default value is 0.0, indicating that the system should attempt to estimate the QR
         * code size.
         *
         * @param qrCodeSizeMeters size of QR code to configure the [Session]
         * @return a [Builder] that builds a [Config] with the supplied QR code size
         */
        public fun setQrCodeSizeMeters(qrCodeSizeMeters: Float): Builder {
            this.qrCodeSizeMeters = qrCodeSizeMeters
            return this
        }

        @PreviewSpatialApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setSceneSignalTypes(sceneSignalTypes: Set<SceneSignalType>): Builder {
            this.sceneSignalTypes = sceneSignalTypes
            return this
        }

        /** Creates a new instance of [Config] with the configuration specified in this instance. */
        @OptIn(PreviewSpatialApi::class)
        public fun build(): Config {
            return Config(
                planeTracking,
                handTracking,
                deviceTracking,
                depthEstimation,
                anchorPersistence,
                faceTracking,
                geospatial,
                augmentedObjectCategories,
                eyeTracking,
                cameraFacingDirection,
                augmentedImageDatabase,
                qrCodeTracking,
                qrCodeSizeMeters,
                sceneSignalTypes,
            )
        }
    }
}
