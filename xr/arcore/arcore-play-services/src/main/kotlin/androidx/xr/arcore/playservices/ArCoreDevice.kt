/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalInertialTrackingApi::class)

package androidx.xr.arcore.playservices

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.ArDevice
import androidx.xr.arcore.runtime.TrackingState as RuntimeTrackingState
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState

/**
 * Provides access to the current [Frame]'s camera pose.
 *
 * @property devicePose the [Pose] of the device
 * @property trackingState the tracking state of the device
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ArCoreDevice internal constructor() : ArDevice {

    override var devicePose: Pose = Pose()
        private set

    override var trackingState: RuntimeTrackingState = RuntimeTrackingState.STOPPED
        private set

    @Volatile private var deviceTrackingMode: DeviceTrackingMode = DeviceTrackingMode.SPATIAL
    @Volatile private var latestSensorRotation: Quaternion? = null
    @Volatile private var sensorListener: SensorEventListener? = null
    @Volatile private var sensorManager: SensorManager? = null
    @Volatile private var isSensorAvailable = true

    private val tempQuaternionArray = ThreadLocal.withInitial { FloatArray(4) }

    @Volatile private var isResumed = false

    private inner class GameRotationSensorListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val tempArray = tempQuaternionArray.get()!!
                SensorManager.getQuaternionFromVector(tempArray, it.values)
                val w = tempArray[0]
                val x = tempArray[1]
                val y = tempArray[2]
                val z = tempArray[3]
                val lengthSq = x * x + y * y + z * z + w * w
                // Filter out invalid sensor values where the quaternion calculates to 0 length or
                // NaNs
                if (lengthSq.isNaN() || lengthSq < 1e-6f) return@let

                val sensorRotation = Quaternion(x = x, y = y, z = z, w = w)
                latestSensorRotation = CORRECTION_QUATERNION * sensorRotation
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateSensorListener() {
        synchronized(this) {
            if (!isResumed || deviceTrackingMode != DeviceTrackingMode.INERTIAL) {
                sensorListener?.let { sensorManager?.unregisterListener(it) }
                sensorListener = null
                return
            }

            if (sensorListener != null) return

            val sm = sensorManager ?: return
            // TODO: b/514701571 - Firmware bug causes TYPE_GAME_ROTATION_VECTOR to
            // return continuous [0.0, 0.0, 0.0] event payloads on Projected environment.
            val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: return

            val listener = GameRotationSensorListener()
            sensorListener = listener
            sm.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    internal fun configureTracking(mode: DeviceTrackingMode, context: Context) {
        synchronized(this) {
            if (sensorManager == null) {
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                // Explicitly reset the flag based on hardware support
                isSensorAvailable =
                    sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null
            }
            deviceTrackingMode = mode
            updateSensorListener()
        }
    }

    internal fun resume() {
        isResumed = true
        updateSensorListener()
    }

    internal fun pause() {
        isResumed = false
        updateSensorListener()
    }

    internal fun dispose() {
        isResumed = false
        updateSensorListener()
    }

    public fun update(frame: Frame) {
        when (deviceTrackingMode) {
            DeviceTrackingMode.DISABLED -> {
                devicePose = Pose()
                trackingState = RuntimeTrackingState.STOPPED
            }
            DeviceTrackingMode.INERTIAL -> {
                val currentRotation = latestSensorRotation
                if (!isSensorAvailable) {
                    devicePose = Pose()
                    trackingState = RuntimeTrackingState.STOPPED
                } else if (!isResumed || currentRotation == null) {
                    // Stay paused until we receive the first real sensor event
                    devicePose = Pose(rotation = currentRotation ?: Quaternion.Identity)
                    trackingState = RuntimeTrackingState.PAUSED
                } else {
                    val neckOffset = Vector3(0f, VERTICAL_OFFSET, HORIZONTAL_OFFSET)
                    val pivotOffset = Vector3(0f, VERTICAL_OFFSET, 0f)
                    val calculatedTranslation = (currentRotation * neckOffset) - pivotOffset
                    devicePose =
                        Pose(translation = calculatedTranslation, rotation = currentRotation)
                    trackingState = RuntimeTrackingState.TRACKING
                }
            }
            else -> {
                // Handles DeviceTrackingMode.SPATIAL (the default ARCore tracking)
                devicePose = frame.camera.pose.toRuntimePose()
                val currentTrackingState: TrackingState? = frame.camera.trackingState
                val mappedState =
                    when (currentTrackingState) {
                        TrackingState.TRACKING -> RuntimeTrackingState.TRACKING
                        TrackingState.PAUSED -> RuntimeTrackingState.PAUSED
                        TrackingState.STOPPED,
                        null -> RuntimeTrackingState.STOPPED
                    }
                this.trackingState = mappedState
            }
        }
    }

    internal companion object {
        // Rotate 90 degrees around X-axis to convert from Android sensor coordinates (Z is up) to
        // ARCore world coordinates (Y is up).
        val CORRECTION_QUATERNION = Quaternion.fromEulerAngles(90f, 0f, 0f)

        @androidx.annotation.VisibleForTesting internal const val HORIZONTAL_OFFSET = -0.080f
        @androidx.annotation.VisibleForTesting internal const val VERTICAL_OFFSET = -0.075f
    }
}
