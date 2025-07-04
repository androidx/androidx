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

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.GooglePlayServicesLocationLibraryNotLinkedException
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.PermissionNotGrantedException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
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
 * @property activity The [Activity] instance.
 * @property perceptionManager The [ArCorePerceptionManager] instance.
 * @property timeSource The [ArCoreTimeSource] instance.
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreManager
internal constructor(
    private val activity: Activity,
    internal val perceptionManager: ArCorePerceptionManager,
    internal val timeSource: ArCoreTimeSource,
    private val arCoreApkInstance: ArCoreApk = ArCoreApk.getInstance(),
) : LifecycleManager {

    internal lateinit var _session: Session

    /** The underlying [Session] instance. */
    @UnsupportedArCoreCompatApi public fun session(): Session = _session

    // TODO(b/411154789): Remove once Session runtime invocations are forced to run sequentially.
    internal var running: Boolean = false
        private set

    /**
     * This method implements the [LifecycleManager.create] method.
     *
     * This method must be called before any operations can be performed by the
     * [ArCorePerceptionManager].
     */
    override fun create() {
        try {
            checkARCoreSupportedAndUpToDate(activity)
            _session = Session(activity)
        } catch (e: SecurityException) {
            throw PermissionNotGrantedException(listOf(Manifest.permission.CAMERA), e)
        }
        perceptionManager.session = _session
    }

    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    override var config: Config = Config()
        private set

    override fun configure(config: Config) {
        val arConfig = _session.config

        arConfig.textureUpdateMode = TextureUpdateMode.EXPOSE_HARDWARE_BUFFER

        arConfig.planeFindingMode =
            if (config.planeTracking == Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL) {
                PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            } else {
                PlaneFindingMode.DISABLED
            }

        if (config.handTracking != Config.HandTrackingMode.DISABLED) {
            throw ConfigurationNotSupportedException()
        }

        if (config.depthEstimation != Config.DepthEstimationMode.DISABLED) {
            throw ConfigurationNotSupportedException()
        }

        if (config.anchorPersistence != Config.AnchorPersistenceMode.DISABLED) {
            throw ConfigurationNotSupportedException()
        }

        arConfig.geospatialMode =
            if (config.geospatial == Config.GeospatialMode.EARTH) {
                GeospatialMode.ENABLED
            } else {
                GeospatialMode.DISABLED
            }

        try {
            _session.configure(arConfig)
        } catch (e: FineLocationPermissionNotGrantedException) {
            throw PermissionNotGrantedException(listOf(Manifest.permission.ACCESS_FINE_LOCATION), e)
        } catch (e: ARCore1xGooglePlayServicesLocationLibraryNotLinkedException) {
            throw GooglePlayServicesLocationLibraryNotLinkedException(e)
        } catch (e: UnsupportedConfigurationException) {
            throw ConfigurationNotSupportedException(cause = e)
        }

        this.config = config
    }

    override fun resume() {
        _session.resume()
        running = true
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

        if (running) {
            perceptionManager.update()
        }

        return timeSource.markNow()
    }

    override fun pause() {
        running = false
        _session.pause()
    }

    override fun stop() {
        _session.close()
    }

    // Verify that ARCore is installed and using the current version.
    // This implementation is derived from
    // https://developers.google.com/ar/develop/java/session-config#verify_that_arcore_is_installed_and_up_to_date
    internal fun checkARCoreSupportedAndUpToDate(activity: Activity) {
        when (arCoreApkInstance.checkAvailability(activity)) {
            Availability.SUPPORTED_INSTALLED -> {
                return
            }
            Availability.SUPPORTED_APK_TOO_OLD,
            Availability.SUPPORTED_NOT_INSTALLED -> {
                throw ApkNotInstalledException(ARCORE_PACKAGE_NAME)
            }
            Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Log.e(
                    "ArCoreManager",
                    "Session cannot be created because ARCore is not supported on this device.",
                )
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

    private companion object {
        const private val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
