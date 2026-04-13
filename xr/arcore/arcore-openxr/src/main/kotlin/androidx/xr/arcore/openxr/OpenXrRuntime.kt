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
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.getNativeInstanceData
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.manifest.HAND_TRACKING
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Implementation of the [PerceptionRuntime] interface using OpenXR.
 *
 * @property lifecycleManager that manages the lifecycle of the OpenXR session
 * @property perceptionManager that manages the perception capabilities of a runtime using OpenXR
 */
internal class OpenXrRuntime(
    private val context: Context,
    override val lifecycleManager: OpenXrManager,
    override val perceptionManager: OpenXrPerceptionManager,
    val timeSource: OpenXrTimeSource,
) : PerceptionRuntime {
    companion object {
        private const val KEY_API_KEY = "com.google.android.ar.API_KEY"
        private val contextList = mutableListOf<Context>()

        @VisibleForTesting
        val SUPPORTED_CONFIG_MODES: Set<ConfigMode> =
            setOf(
                PlaneTrackingMode.DISABLED,
                PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                HandTrackingMode.DISABLED,
                HandTrackingMode.BOTH,
                DeviceTrackingMode.DISABLED,
                DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                DepthEstimationMode.DISABLED,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                AnchorPersistenceMode.DISABLED,
                AnchorPersistenceMode.LOCAL,
                FaceTrackingMode.DISABLED,
                FaceTrackingMode.BLEND_SHAPES,
                GeospatialMode.DISABLED,
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.COARSE_TRACKING,
                EyeTrackingMode.FINE_TRACKING,
            )
    }

    /**
     * A pointer to the native OpenXrManager. Only valid after [initialize] and before [destroy]
     * have been called.
     */
    var nativePointer: Long = 0L
        private set(value) {
            this.lifecycleManager.nativePointer = value
            field = value
        }

    /**
     * A pointer to the native XrSession. Only valid after [initialize] and before [destroy] have
     * been called.
     */
    override var sessionPointer: Long = 0L
        private set(value) {
            this.lifecycleManager.sessionPointer = value
            field = value
        }

    /**
     * A pointer to the native XrInstance. Only valid after [initialize] and before [destroy] have
     * been called.
     */
    var instancePointer: Long = 0L
        private set(value) {
            this.lifecycleManager.instancePointer = value
            field = value
        }

    /** The current state of the runtime configuration for the session. */
    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    override var config: Config =
        Config(
            PlaneTrackingMode.DISABLED,
            HandTrackingMode.DISABLED,
            DeviceTrackingMode.DISABLED,
            DepthEstimationMode.DISABLED,
            AnchorPersistenceMode.LOCAL,
            augmentedObjectCategories = setOf(),
        )
        private set(value) {
            lifecycleManager.configure(value)
            field = value
        }

    var instanceProcAddr: Long = 0L
        private set

    @OptIn(
        androidx.xr.runtime.UnstableNativeResourceApi::class,
        androidx.xr.runtime.ExperimentalXrDeviceLifecycleApi::class,
    )
    override fun initialize() {
        nativePointer = nativeGetPointer()
        val nativeInstanceData = XrDevice.getCurrentDevice(context).getNativeInstanceData(context)
        instancePointer = nativeInstanceData.instancePointer
        instanceProcAddr = nativeInstanceData.functionTablePointer
        // Only initialize the OpenXrManager and bring up resources.
        check(nativeInit(context, startPollingThread = false, instancePointer, instanceProcAddr))
        contextList.add(context)
        setAuthentication(context)
        sessionPointer = nativeGetXrSessionHandle()
    }

    override fun resume() {
        // (b/412663675): This is a temporary solution to split the init and resume portions of the
        // lifecycle. Ideally make this two different functions.
        // The initialization will be a no-op but it will start the polling loop for the resumed
        // lifecycle.
        check(nativeInit(context, startPollingThread = true, instancePointer, instanceProcAddr))
    }

    override fun pause() {
        if (!nativePause()) {
            // Native pause fails when the OpenXR runtime is not running, so
            // we should clean up its state so that it can be re-initialized
            // later when resume() is called.
            nativeDeInit()
        }
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

        val objectLabels: MutableList<Long> = mutableListOf()
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
            if (config.deviceTracking == DeviceTrackingMode.SPATIAL_LAST_KNOWN) {
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

    override fun isSupported(configMode: ConfigMode): Boolean {
        if (configMode == GeospatialMode.VPS_AND_GPS) {
            return nativeIsGeospatialSupported()
        }
        return SUPPORTED_CONFIG_MODES.contains(configMode)
    }

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        val blendMode = nativeGetPreferredBlendMode()
        return blendMode ?: DisplayBlendMode.NO_DISPLAY
    }

    override fun destroy() {
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
            XrLog.verbose("No API Key provided, using keyless authentication.")
            nativeSetKeylessAuth()
        } else {
            XrLog.verbose("Using provided API Key.")
            nativeSetApiKeyAuth(apiKey)
        }
    }

    private external fun nativeGetPreferredBlendMode(): DisplayBlendMode?

    private external fun nativeIsGeospatialSupported(): Boolean

    private external fun nativeGetPointer(): Long

    private external fun nativeInit(
        context: Context,
        startPollingThread: Boolean,
        instancePointer: Long,
        instanceProcAddr: Long,
    ): Boolean

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

    private external fun nativeGetXrSessionHandle(): Long

    private external fun nativeSetApiKeyAuth(apiKey: String)

    private external fun nativeSetKeylessAuth()
}

internal fun DisplayBlendMode.Companion.fromOpenXrEnvironmentBlendMode(
    type: Int
): DisplayBlendMode =
    when (type) {
        1 -> NO_DISPLAY // XR_ENVIRONMENT_BLEND_MODE_OPAQUE
        2 -> ADDITIVE // XR_ENVIRONMENT_BLEND_MODE_ADDITIVE
        3 -> ALPHA_BLEND // XR_ENVIRONMENT_BLEND_MODE_ALPHA_BLEND
        else -> {
            throw IllegalStateException("Invalid environment blend mode.")
        }
    }
