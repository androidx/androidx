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

package androidx.xr.runtime.openxr

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.PermissionNotGrantedException
import androidx.xr.runtime.manifest.HAND_TRACKING
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** Manages the lifecycle of an OpenXR session. */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrManager
internal constructor(
    private val activity: Activity,
    private val perceptionManager: OpenXrPerceptionManager,
    internal val timeSource: OpenXrTimeSource,
) : LifecycleManager {

    /**
     * A pointer to the native OpenXrManager. Only valid after [create] and before [stop] have been
     * called.
     */
    internal var nativePointer: Long = 0L
        private set

    override fun create() {
        nativePointer = nativeGetPointer()
        // Only initialize the OpenXrManager and bring up resources.
        check(nativeInit(activity, startPollingThread = false))
    }

    /** The current state of the runtime configuration for the session. */
    // TODO(b/392660855): Disable all features by default once this API is fully implemented.
    override var config: Config =
        Config(
            Config.PlaneTrackingMode.DISABLED,
            augmentedObjectCategories = listOf(),
            Config.HandTrackingMode.DISABLED,
            Config.DeviceTrackingMode.DISABLED,
            Config.DepthEstimationMode.DISABLED,
            Config.AnchorPersistenceMode.LOCAL,
        )
        private set

    override fun configure(config: Config) {
        if (config.depthEstimation == Config.DepthEstimationMode.SMOOTH_AND_RAW) {
            throw ConfigurationNotSupportedException(
                "Failed to configure session, runtime does not support raw and smooth depth simultaneously."
            )
        }

        // TODO(b/422808099): OpenXR does not properly return
        // XR_ERROR_PERMISSION_INSUFFICIENT when the HAND_TRACKING permission is not
        // granted, so we manually check it here.
        if (
            config.handTracking != Config.HandTrackingMode.DISABLED &&
                ContextCompat.checkSelfPermission(activity, HAND_TRACKING) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            throw PermissionNotGrantedException()
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
                    objectLabels = objectLabels.toLongArray(),
                    objectTracking = objectMode,
                )
            ) {
                -2L ->
                    throw RuntimeException(
                        "There was an unknown runtime error configuring the session."
                    ) // XR_ERROR_RUNTIME_FAILURE
                -8L ->
                    throw ConfigurationNotSupportedException(
                        "Feature not supported."
                    ) // XR_ERROR_FEATURE_UNSUPPORTED
                -12L ->
                    throw IllegalStateException(
                        "One or more objects are null. Has the OpenXrManager been created?"
                    ) // XR_ERROR_HANDLE_INVALID
                -1000710000L ->
                    throw PermissionNotGrantedException() // XR_ERROR_PERMISSION_INSUFFICIENT
            }
        }

        if (config.handTracking != this.config.handTracking) {
            if (config.handTracking == Config.HandTrackingMode.BOTH) {
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
            if (config.deviceTracking == Config.DeviceTrackingMode.LAST_KNOWN) {
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
            if (config.faceTracking == Config.FaceTrackingMode.USER) {
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

        this.config = config
    }

    override fun resume() {
        // (b/412663675): This is a temporary solution to split the init and resume portions of the
        // lifecycle. Ideally make this two different functions.
        // The initialization will be a no-op but it will start the polling loop for the resumed
        // lifecycle.
        check(nativeInit(activity, startPollingThread = true))
    }

    override suspend fun update(): ComparableTimeMark {
        // TODO: b/345314364 - Implement this method properly once the native manager supports it.
        // Currently the native manager handles this via an internal looping mechanism.
        val now = timeSource.markNow()
        val xrTime = timeSource.getXrTime(now)

        if (config.planeTracking != Config.PlaneTrackingMode.DISABLED) {
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
        nativeDeInit()
        nativePointer = 0L
        perceptionManager.clear()
    }

    private external fun nativeGetPointer(): Long

    private external fun nativeInit(activity: Activity, startPollingThread: Boolean): Boolean

    private external fun nativeDeInit(): Boolean

    private external fun nativePause(): Boolean

    private external fun nativeConfigureSession(
        planeTracking: Int,
        handTracking: Int,
        deviceTracking: Int,
        depthEstimation: Int,
        anchorPersistence: Int,
        faceTracking: Int = 0,
        objectTracking: Int,
        objectLabels: LongArray,
    ): Long

    private external fun nativeGetFaceTrackerCalibration(): Boolean
}
