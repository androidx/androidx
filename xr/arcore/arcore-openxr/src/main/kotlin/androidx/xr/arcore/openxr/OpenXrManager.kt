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

package androidx.xr.arcore.openxr

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.Log
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.manifest.HAND_TRACKING
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Manages the lifecycle of an OpenXR session.
 *
 * @property activity the [Activity] instance
 * @property perceptionManager the [OpenXrPerceptionManager] instance
 * @property timeSource the [OpenXrTimeSource] instance
 * @property nativePointer a pointer to the native `OpenXrManager`
 * @property sessionPointer a pointer to the native
 *   [XrSession](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrSession)
 * @property instancePointer a pointer to the native
 *   [XrInstance](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrInstance)
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrManager
internal constructor(
    private val context: Context,
    private val perceptionManager: OpenXrPerceptionManager,
    internal val timeSource: OpenXrTimeSource,
) : LifecycleManager {

    private companion object {
        private const val KEY_API_KEY = "com.google.android.ar.API_KEY"
        private val contextList = mutableListOf<Context>()
    }

    internal var nativePointer: Long = 0L
        private set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override var sessionPointer: Long = 0L
        private set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override var instancePointer: Long = 0L
        private set

    override fun create() {
        nativePointer = nativeGetPointer()
        // Only initialize the OpenXrManager and bring up resources.
        check(nativeInit(context, startPollingThread = false))
        contextList.add(context)
        setAuthentication(context)
        sessionPointer = nativeGetXrSessionHandle()
        instancePointer = nativeGetXrInstanceHandle()
    }

    override var config: Config =
        Config(
            PlaneTrackingMode.DISABLED,
            HandTrackingMode.DISABLED,
            DeviceTrackingMode.DISABLED,
            DepthEstimationMode.DISABLED,
            AnchorPersistenceMode.LOCAL,
        )
        private set

    override fun configure(config: Config) {
        if (config.depthEstimation == DepthEstimationMode.SMOOTH_AND_RAW) {
            throw UnsupportedOperationException(
                "Failed to configure session, runtime does not support raw and smooth depth simultaneously."
            )
        }

        // TODO(b/422808099): OpenXR does not properly return
        // XR_ERROR_PERMISSION_INSUFFICIENT when the HAND_TRACKING permission is not
        // granted, so we manually check it here.
        if (
            config.handTracking != HandTrackingMode.DISABLED &&
                ContextCompat.checkSelfPermission(context, HAND_TRACKING) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException()
        }

        var objectLabels: MutableList<Long> = mutableListOf()
        var objectMode: Int = 0

        for (category in config.augmentedObjectCategories) {
            objectLabels.add(nativeValueFromCategory(category))
            // Set objectMode to 1 to indicate that object tracking is enabled.
            objectMode = 1
        }

        // TODO(b/425697141): Remove this when instrumentation tests support HEAD_TRACKING
        // permission so we can call native functions.
        if (!Build.FINGERPRINT.contains("robolectric")) {
            when (
                nativeConfigureSession(
                    planeTracking = config.planeTracking.mode,
                    handTracking = config.handTracking.mode,
                    deviceTracking = config.deviceTracking.mode,
                    depthEstimation = config.depthEstimation.mode,
                    anchorPersistence = config.anchorPersistence.mode,
                    faceTracking = config.faceTracking.mode,
                    eyeTracking = config.eyeTracking.mode,
                    objectTracking = objectMode,
                    objectLabels = objectLabels.toLongArray(),
                    geospatial = config.geospatial.mode,
                )
            ) {
                -2L ->
                    throw RuntimeException(
                        "There was an unknown runtime error configuring the session."
                    ) // XR_ERROR_RUNTIME_FAILURE
                -8L ->
                    throw UnsupportedOperationException(
                        "Feature not supported."
                    ) // XR_ERROR_FEATURE_UNSUPPORTED
                -12L ->
                    throw IllegalStateException(
                        "One or more objects are null. Has the OpenXrManager been created?"
                    ) // XR_ERROR_HANDLE_INVALID
                -1000710000L -> throw SecurityException() // XR_ERROR_PERMISSION_INSUFFICIENT
            }
        }

        if (config.handTracking != this.config.handTracking) {
            if (config.handTracking == HandTrackingMode.BOTH) {
                perceptionManager.xrResources.addUpdatable(perceptionManager.xrResources.leftHand)
                perceptionManager.xrResources.addUpdatable(perceptionManager.xrResources.rightHand)
            } else {
                perceptionManager.xrResources.removeUpdatable(
                    perceptionManager.xrResources.leftHand
                )
                perceptionManager.xrResources.removeUpdatable(
                    perceptionManager.xrResources.rightHand
                )
            }
        }

        if (config.deviceTracking != this.config.deviceTracking) {
            if (config.deviceTracking == DeviceTrackingMode.LAST_KNOWN) {
                perceptionManager.xrResources.addUpdatable(perceptionManager.xrResources.arDevice)
            } else {
                perceptionManager.xrResources.removeUpdatable(
                    perceptionManager.xrResources.arDevice
                )
            }
        }

        if (config.depthEstimation != this.config.depthEstimation) {
            perceptionManager.xrResources.leftDepthMap.updateDepthEstimationMode(
                config.depthEstimation
            )
            perceptionManager.xrResources.rightDepthMap.updateDepthEstimationMode(
                config.depthEstimation
            )
            perceptionManager.depthEstimationMode = config.depthEstimation
        }

        if (config.faceTracking != this.config.faceTracking) {
            if (config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
                if (!nativeGetFaceTrackerCalibration()) {
                    throw FaceTrackingNotCalibratedException()
                }
                perceptionManager.xrResources.addUpdatable(perceptionManager.xrResources.userFace)
            } else {
                perceptionManager.xrResources.removeUpdatable(
                    perceptionManager.xrResources.userFace
                )
            }
        }

        if (config.eyeTracking != this.config.eyeTracking) {
            perceptionManager.eyeTrackingMode = config.eyeTracking
        }

        if (config.geospatial != this.config.geospatial) {
            if (config.geospatial == GeospatialMode.VPS_AND_GPS) {
                perceptionManager.xrResources.addUpdatable(perceptionManager.xrResources.geospatial)
            } else {
                perceptionManager.xrResources.removeUpdatable(
                    perceptionManager.xrResources.geospatial
                )
            }
        }

        this.config = config
    }

    override fun resume() {
        // (b/412663675): This is a temporary solution to split the init and resume portions of the
        // lifecycle. Ideally make this two different functions.
        // The initialization will be a no-op but it will start the polling loop for the resumed
        // lifecycle.
        check(nativeInit(context, startPollingThread = true))
    }

    override suspend fun update(): ComparableTimeMark {
        // TODO: b/345314364 - Implement this method properly once the native manager supports it.
        // Currently the native manager handles this via an internal looping mechanism.
        val now = timeSource.markNow()
        val xrTime = timeSource.getXrTime(now)

        if (config.planeTracking != PlaneTrackingMode.DISABLED) {
            perceptionManager.updatePlanes(xrTime)
        }

        if (!config.augmentedObjectCategories.isEmpty()) {
            perceptionManager.updateAugmentedObjects(xrTime)
        }

        perceptionManager.update(xrTime)
        // Block the call for a time that is appropriate for OpenXR devices.
        // TODO: b/359871229 - Implement dynamic delay. We start with a fixed 20ms delay as it is
        // a nice round number that produces a reasonable frame rate @50 Hz, but this value may need
        // to be adjusted in the future.
        delay(20.milliseconds)
        return now
    }

    override fun pause() {
        if (!nativePause()) {
            // Native pause fails when the OpenXR runtime is not running, so
            // we should clean up its state so that it can be re-initialized
            // later when resume() is called.
            nativeDeInit()
        }
    }

    override fun stop() {
        // TODO: b/422830134 - Remove this check once there are multiple OpenXrManagers.
        contextList.remove(context)
        if (contextList.isEmpty()) {
            nativeDeInit()
            nativePointer = 0L
            sessionPointer = 0L
            instancePointer = 0L
            perceptionManager.clear()
        }
    }

    private fun setAuthentication(context: Context) {
        var apiKey: String? = null
        try {
            val appInfo =
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA,
                )
            apiKey = appInfo.metaData?.getString(KEY_API_KEY)?.takeIf { it.isNotEmpty() }
        } catch (e: PackageManager.NameNotFoundException) {
            // Did not read an API key from Application
        }

        if (apiKey == null && context is Activity) {
            try {
                val activityInfo =
                    context.packageManager.getActivityInfo(
                        context.componentName,
                        PackageManager.GET_META_DATA,
                    )
                apiKey = activityInfo.metaData?.getString(KEY_API_KEY)?.takeIf { it.isNotEmpty() }
            } catch (e: PackageManager.NameNotFoundException) {
                // Did not read an API key from Activity
            }
        }

        if (apiKey == null) {
            Log.verbose("No API Key provided, using keyless authentication.")
            nativeSetKeylessAuth()
        } else {
            Log.verbose("Using provided API Key.")
            nativeSetApiKeyAuth(apiKey)
        }
    }

    private external fun nativeGetPointer(): Long

    private external fun nativeGetXrSessionHandle(): Long

    private external fun nativeGetXrInstanceHandle(): Long

    private external fun nativeInit(context: Context, startPollingThread: Boolean): Boolean

    private external fun nativeDeInit(): Boolean

    private external fun nativePause(): Boolean

    private external fun nativeConfigureSession(
        planeTracking: Int,
        handTracking: Int,
        deviceTracking: Int,
        depthEstimation: Int,
        anchorPersistence: Int,
        faceTracking: Int = 0,
        eyeTracking: Int,
        objectTracking: Int,
        objectLabels: LongArray,
        geospatial: Int,
    ): Long

    private external fun nativeGetFaceTrackerCalibration(): Boolean

    private external fun nativeSetApiKeyAuth(apiKey: String)

    private external fun nativeSetKeylessAuth()
}
