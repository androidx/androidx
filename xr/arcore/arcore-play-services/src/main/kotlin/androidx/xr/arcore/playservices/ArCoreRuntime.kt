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

package androidx.xr.arcore.playservices

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.CameraFacingDirection
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config as ArConfig
import com.google.ar.core.Config as ArCoreConfig
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Config.GeospatialMode as ArGeospatialMode
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Config.TextureUpdateMode
import com.google.ar.core.Session
import com.google.ar.core.exceptions.FineLocationPermissionNotGrantedException
import com.google.ar.core.exceptions.GooglePlayServicesLocationLibraryNotLinkedException as ARCore1xGooglePlayServicesLocationLibraryNotLinkedException
import com.google.ar.core.exceptions.UnsupportedConfigurationException
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Implementation of the [androidx.xr.arcore.runtime.PerceptionRuntime] interface using ARCore.
 *
 * @property context The [Context] instance
 * @property lifecycleManager that manages the lifecycle of the ARCore session
 * @property perceptionManager that manages the perception capabilities of a runtime using ARCore
 * @property timeSource the [ArCoreTimeSource] instance
 * @property config the current [Config] of the session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ArCoreRuntime
internal constructor(
    private val context: Context,
    override val lifecycleManager: ArCoreManager,
    override val perceptionManager: ArCorePerceptionManager,
    internal val timeSource: ArCoreTimeSource,
    private val arCoreApkInstance: ArCoreApk = ArCoreApk.getInstance(),
) : PerceptionRuntime {

    internal lateinit var _session: Session

    /**
     * The underlying [Session] instance.
     *
     * @sample androidx.xr.arcore.samples.getARCoreSession
     */
    @UnsupportedArCoreCompatApi public fun session(): Session = _session

    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    public override var config: Config = Config()
        private set(value) {
            this.lifecycleManager.configure(value)
            field = value
        }

    override fun initialize() {
        checkARCoreSupportedAndUpToDate(context)
        _session = Session(context)
        perceptionManager.session = _session
        perceptionManager.geospatial.arCoreSession = _session
    }

    override fun resume() {
        _session.resume()
    }

    override fun pause() {
        _session.pause()
    }

    override suspend fun update(): ComparableTimeMark {
        // Delay for average time between frames based on camera config fps setting. This frees up
        // the thread this method is scheduled to run on to do other work. Note that this can result
        // in the emission of duplicated CoreStates by the core Session if the underlying ARCore 1.x
        // Session has not produced a new frame by the time the delay has expired.
        val avgFps =
            (_session.cameraConfig.fpsRange.lower + _session.cameraConfig.fpsRange.upper) / 2
        val delayTime = (1000L / avgFps).milliseconds
        delay(delayTime)

        perceptionManager.update()

        return timeSource.markNow()
    }

    override fun configure(config: Config) {
        val arConfig = _session.config

        if (config.cameraFacingDirection != this.config.cameraFacingDirection) {
            try {
                perceptionManager.setCameraFacingDirection(config.cameraFacingDirection)
            } catch (e: Exception) {
                val message =
                    when (e) {
                        is UnsupportedDeviceException ->
                            "This device does not have a front-facing (selfie) camera"
                        is IllegalArgumentException ->
                            "${config.cameraFacingDirection} is not supported."
                        else -> throw (e)
                    }
                throw UnsupportedOperationException(message, e)
            }
        }

        if (Build.VERSION.SDK_INT >= 27) {
            setTextureUpdateModeToHardwareBuffer(arConfig)
        } else {
            setTextureUpdateModeToExternalOES(arConfig)
        }

        arConfig.planeFindingMode =
            if (config.planeTracking == PlaneTrackingMode.HORIZONTAL_AND_VERTICAL) {
                PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            } else {
                PlaneFindingMode.DISABLED
            }

        if (config.handTracking != HandTrackingMode.DISABLED) {
            throw UnsupportedOperationException()
        }

        arConfig.depthMode =
            when (config.depthEstimation) {
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW -> DepthMode.AUTOMATIC
                DepthEstimationMode.RAW_ONLY -> DepthMode.RAW_DEPTH_ONLY
                else -> DepthMode.DISABLED
            }

        perceptionManager.setDepthEstimationMode(config.depthEstimation)

        if (config.anchorPersistence != AnchorPersistenceMode.DISABLED) {
            throw UnsupportedOperationException()
        }

        arConfig.augmentedFaceMode =
            when (config.faceTracking) {
                FaceTrackingMode.MESHES -> AugmentedFaceMode.MESH3D
                FaceTrackingMode.DISABLED -> AugmentedFaceMode.DISABLED
                else -> throw UnsupportedOperationException()
            }

        arConfig.geospatialMode =
            if (config.geospatial == GeospatialMode.VPS_AND_GPS) {
                ArGeospatialMode.ENABLED
            } else {
                ArGeospatialMode.DISABLED
            }

        try {
            _session.configure(arConfig)
        } catch (e: FineLocationPermissionNotGrantedException) {
            throw SecurityException(e)
        } catch (e: ARCore1xGooglePlayServicesLocationLibraryNotLinkedException) {
            throw LibraryNotLinkedException("com.google.android.gms:play-services-location", e)
        } catch (e: UnsupportedConfigurationException) {
            throw UnsupportedOperationException(e)
        }

        this.config = config
    }

    override fun isSupported(configMode: ConfigMode): Boolean {
        if (configMode is DepthEstimationMode) {
            return isDepthModeSupportedInArCore1x(configMode)
        } else if (configMode is GeospatialMode) {
            return isGeoSpatialModeSupportedInArCore1x(configMode)
        }
        return SUPPORTED_CONFIG_MODES.contains(configMode)
    }

    override fun destroy() {
        perceptionManager.dispose()
        _session.close()
    }

    // Verify that ARCore is installed and using the current version.
    // This implementation is derived from
    // https://developers.google.com/ar/develop/java/session-config#verify_that_arcore_is_installed_and_up_to_date
    internal fun checkARCoreSupportedAndUpToDate(context: Context) {
        when (arCoreApkInstance.checkAvailability(context)) {
            Availability.SUPPORTED_INSTALLED -> {
                return
            }
            Availability.SUPPORTED_APK_TOO_OLD,
            Availability.SUPPORTED_NOT_INSTALLED -> {
                throw ApkNotInstalledException(ARCORE_PACKAGE_NAME)
            }
            Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                throw UnsupportedDeviceException()
            }
            Availability.UNKNOWN_CHECKING -> {
                throw ApkCheckAvailabilityInProgressException(ARCORE_PACKAGE_NAME)
            }
            Availability.UNKNOWN_ERROR,
            Availability.UNKNOWN_TIMED_OUT -> {
                throw ApkCheckAvailabilityErrorException(ARCORE_PACKAGE_NAME)
            }
        }
    }

    private fun setTextureUpdateModeToExternalOES(config: ArConfig) {
        config.textureUpdateMode = TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES
    }

    @RequiresApi(27)
    private fun setTextureUpdateModeToHardwareBuffer(config: ArConfig) {
        config.textureUpdateMode = TextureUpdateMode.EXPOSE_HARDWARE_BUFFER
    }

    private fun isDepthModeSupportedInArCore1x(depthEstimationMode: DepthEstimationMode): Boolean {
        val arCoreDepthMode =
            when (depthEstimationMode) {
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW -> ArCoreConfig.DepthMode.AUTOMATIC
                DepthEstimationMode.RAW_ONLY -> ArCoreConfig.DepthMode.RAW_DEPTH_ONLY
                else -> ArCoreConfig.DepthMode.DISABLED
            }
        return _session.isDepthModeSupported(arCoreDepthMode)
    }

    private fun isGeoSpatialModeSupportedInArCore1x(geospatialMode: GeospatialMode): Boolean {
        val arCoreGeospatialMode =
            when (geospatialMode) {
                GeospatialMode.VPS_AND_GPS -> ArCoreConfig.GeospatialMode.ENABLED
                else -> ArCoreConfig.GeospatialMode.DISABLED
            }
        return _session.isGeospatialModeSupported(arCoreGeospatialMode)
    }

    internal companion object {
        const private val ARCORE_PACKAGE_NAME = "com.google.ar.core"

        internal val SUPPORTED_CONFIG_MODES: Set<ConfigMode> =
            setOf(
                CameraFacingDirection.WORLD,
                CameraFacingDirection.USER,
                DeviceTrackingMode.DISABLED,
                DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                FaceTrackingMode.DISABLED,
                FaceTrackingMode.MESHES,
                PlaneTrackingMode.DISABLED,
                PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            )
    }
}
