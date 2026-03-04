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
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config as ArConfig
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Config.GeospatialMode
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Config.TextureUpdateMode
import com.google.ar.core.Session
import com.google.ar.core.exceptions.FineLocationPermissionNotGrantedException
import com.google.ar.core.exceptions.GooglePlayServicesLocationLibraryNotLinkedException as ARCore1xGooglePlayServicesLocationLibraryNotLinkedException
import com.google.ar.core.exceptions.UnsupportedConfigurationException
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// TODO:  b/396240241 -- Appropriately handle or translate any exceptions thrown by Android 1.x
/**
 * Manages the lifecycle of an ARCore session.
 *
 * @property context The [Context] instance
 * @property perceptionManager the [ArCorePerceptionManager] instance
 * @property timeSource the [ArCoreTimeSource] instance
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreManager
internal constructor(
    private val context: Context,
    internal val perceptionManager: ArCorePerceptionManager,
    internal val timeSource: ArCoreTimeSource,
    private val arCoreApkInstance: ArCoreApk = ArCoreApk.getInstance(),
) : LifecycleManager {

    internal lateinit var _session: Session

    /**
     * The underlying [Session] instance.
     *
     * @return the underlying [Session] instance
     * @sample androidx.xr.arcore.samples.getARCoreSession
     */
    @UnsupportedArCoreCompatApi public fun session(): Session = _session

    /**
     * This method implements the [LifecycleManager.create] method.
     *
     * This method must be called before any operations can be performed by the
     * [ArCorePerceptionManager].
     */
    override fun create() {
        checkARCoreSupportedAndUpToDate(context)
        _session = Session(context)
        perceptionManager.session = _session
    }

    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    override var config: Config = Config()
        private set

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
            if (config.geospatial == androidx.xr.runtime.GeospatialMode.VPS_AND_GPS) {
                GeospatialMode.ENABLED
            } else {
                GeospatialMode.DISABLED
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

    override fun resume() {
        _session.resume()
    }

    override suspend fun update(): ComparableTimeMark {
        // Delay for average time between frames based on camera config fps setting. This frees up
        // the
        // thread this method is scheduled to run on to do other work. Note that this can result in
        // the
        // emission of duplicated CoreStates by the core Session if the underlying ARCore 1.x
        // Session has not produced a new frame by the time the delay has expired.
        val avgFps =
            (_session.cameraConfig.fpsRange.lower + _session.cameraConfig.fpsRange.upper) / 2
        val delayTime = (1000L / avgFps).milliseconds
        delay(delayTime)

        perceptionManager.update()

        return timeSource.markNow()
    }

    override fun pause() {
        _session.pause()
    }

    override fun stop() {
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
                XrLog.error {
                    "Session cannot be created because ARCore is not supported on this device."
                }
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

    private companion object {
        const private val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
