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
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
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
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config as ArConfig
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
 * @property perceptionManager that manages the perception capabilities of a runtime using ARCore
 * @property timeSource the [ArCoreTimeSource] instance
 * @property config the current [Config] of the session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@OptIn(ExperimentalInertialTrackingApi::class)
public class ArCoreRuntime
internal constructor(
    private val context: Context,
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

    public override var config: Config = Config.Builder().build()
        private set

    override fun initialize() {
        checkARCoreSupportedAndUpToDate(context)
        _session = Session(context)
        perceptionManager.session = _session
        perceptionManager.geospatial.arCoreSession = _session
    }

    override fun resume() {
        perceptionManager.arDevice.resume()
        _session.resume()
    }

    override fun pause() {
        perceptionManager.arDevice.pause()
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

    @OptIn(androidx.xr.runtime.PreviewSpatialApi::class)
    @SuppressWarnings("RestrictedApiAndroidX")
    override fun configure(config: Config) {
        val arConfig = _session.config

        perceptionManager.arDevice.configureTracking(config.deviceTracking, context)

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

        config.augmentedImageDatabase?.let {
            if (it.entries.isEmpty()) {
                throw UnsupportedOperationException(
                    "Failed to configure session, the image database has exceeded the maximum number of entries."
                )
            }

            val augmentedImageDatabase = AugmentedImageDatabase(_session)
            it.entries.forEach { entry ->
                augmentedImageDatabase.addImage("", entry.bitmap, entry.widthInMeters)
            }
            arConfig.augmentedImageDatabase = augmentedImageDatabase
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
            when (config.geospatial) {
                GeospatialMode.SPATIAL -> ArGeospatialMode.ENABLED
                else -> ArGeospatialMode.DISABLED
            }

        // TODO: b/510879776 - Remove this code once GeospatialMode.INERTIAL is out in ARCore 1.55
        if (config.geospatial == GeospatialMode.INERTIAL) {
            if (!isPrototypeGeospatialModeSupported(ARCORE_GEOSPATIAL_MODE_INERTIAL)) {
                throw UnsupportedOperationException(
                    "Failed to configure session, runtime does not support GeospatialMode.INERTIAL"
                )
            }
            setPrototypeGeospatialMode(arConfig, ARCORE_GEOSPATIAL_MODE_INERTIAL)
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

    // TODO: b/510879776 - Remove this method once GeospatialMode.INERTIAL is out in ARCore 1.55
    @Suppress("BanUncheckedReflection") // Using reflection to access unreleased ARCore 1.55 API
    internal fun setPrototypeGeospatialMode(config: ArConfig, mode: Int) {
        try {
            val nativeSymbolTableHandleField =
                config.javaClass.getDeclaredField("nativeSymbolTableHandle").apply {
                    isAccessible = true
                }
            val nativeSymbolTableHandle = nativeSymbolTableHandleField.getLong(config)

            val nativeHandleField =
                config.javaClass.getDeclaredField("nativeHandle").apply { isAccessible = true }
            val nativeHandle = nativeHandleField.getLong(config)

            val nativeSessionField =
                _session.javaClass.getDeclaredField("nativeWrapperHandle").apply {
                    isAccessible = true
                }
            val nativeSession = nativeSessionField.getLong(_session)

            val nativeSetGeospatialMode =
                config.javaClass
                    .getDeclaredMethod(
                        "nativeSetGeospatialMode",
                        Long::class.java,
                        Long::class.java,
                        Long::class.java,
                        Int::class.java,
                    )
                    .apply { isAccessible = true }

            nativeSetGeospatialMode.invoke(
                config,
                nativeSymbolTableHandle,
                nativeSession,
                nativeHandle,
                mode,
            )
        } catch (e: Exception) {
            throw UnsupportedOperationException(
                "GeospatialMode.INERTIAL is not supported on this device or ARCore version.",
                e,
            )
        }
    }

    // TODO: b/510879776 - Remove this method once GeospatialMode.INERTIAL is out in ARCore 1.55
    @Suppress("BanUncheckedReflection") // Using reflection to access unreleased ARCore 1.55 API
    internal fun isPrototypeGeospatialModeSupported(mode: Int): Boolean {
        return try {
            val nativeHandleField =
                _session.javaClass.getDeclaredField("nativeWrapperHandle").apply {
                    isAccessible = true
                }
            val nativeHandle = nativeHandleField.getLong(_session)

            val nativeCheckMethod =
                _session.javaClass
                    .getDeclaredMethod(
                        "nativeIsGeospatialModeSupported",
                        Long::class.java,
                        Int::class.java,
                    )
                    .apply { isAccessible = true }

            nativeCheckMethod.invoke(_session, nativeHandle, mode) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    internal companion object {
        // TODO: b/510879776 - Remove this constant once GeospatialMode.INERTIAL is out in ARCore
        // 1.55
        private const val ARCORE_GEOSPATIAL_MODE_INERTIAL =
            3 /* com.google.ar.core.Config.GeospatialMode.INERTIAL */
        private const val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
