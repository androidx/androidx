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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Quaternion
import kotlin.math.asin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.runningFold

/** Represents the vertical tilt state of the device. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Tilt private constructor(private val value: Int) {
    public companion object {
        /** Tilt is not yet detected. */
        @JvmStatic public val UNKNOWN: Tilt = Tilt(0)

        /** The device is tilted upwards, surpassing the defined upper threshold. */
        @JvmStatic public val UP: Tilt = Tilt(1)

        /** The device is tilted downwards, surpassing the defined lower threshold. */
        @JvmStatic public val DOWN: Tilt = Tilt(2)
    }

    override fun toString(): String {
        return when (this) {
            UP -> "Tilt.UP"
            DOWN -> "Tilt.DOWN"
            UNKNOWN -> "Tilt.UNKNOWN"
            else -> "Tilt(value=$value)"
        }
    }
}

/**
 * Container for device tilt gesture detection logic and the [Tilt] state representation.
 *
 * The primary way to use this is via the [TiltGesture.detect] method, which provides a [Flow] of
 * [Tilt] states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object TiltGesture {
    /**
     * The lower angle threshold in degrees to trigger a transition to the `Tilt.DOWN` state. See
     * [getTiltAngleFromQuaternion] for angle definition.
     */
    private const val TILT_DOWN_THRESHOLD = 60.0f

    /**
     * The upper angle threshold in degrees to trigger a transition to the `Tilt.UP` state. See
     * [getTiltAngleFromQuaternion] for angle definition.
     */
    private const val TILT_UP_THRESHOLD = 85.0f

    /**
     * Begins detecting the device's tilt state and returns a cold [Flow] of [Tilt] updates.
     *
     * This function observes changes to the [ArDevice.state] obtained from the provided [session].
     * The device's tilt is calculated based on the `devicePose.rotation` from this state.
     *
     * **Precondition**: The [session] must be configured with device tracking set to
     * [androidx.xr.runtime.Config.DeviceTrackingMode.LAST_KNOWN]. If
     * [androidx.xr.runtime.Config.DeviceTrackingMode.DISABLED] is used, this function will throw an
     * [IllegalStateException] when attempting to acquire the [ArDevice] instance.
     *
     * The returned flow is **cold**: a new stream of tilt updates is created for each collector.
     * The flow initiates with [Tilt.UNKNOWN] and subsequently emits new [Tilt] states (i.e.,
     * [Tilt.UP] or [Tilt.DOWN]) based on the orientation derived from the device pose. If the
     * device pose is indeterminate or if an internal error prevents accurate calculation, the flow
     * may revert to emitting [Tilt.UNKNOWN].
     *
     * It is recommended to collect this flow from a coroutine scope with the same coroutine context
     * used to create the XR [Session], as changes to [ArDevice.state] drive the emissions.
     *
     * @param session The active XR session. It must be configured with device tracking enabled
     *   [androidx.xr.runtime.Config.DeviceTrackingMode.LAST_KNOWN]
     * @return A [Flow] that emits the current [Tilt] state, starting with [Tilt.UNKNOWN].
     * @throws IllegalStateException if [session] is configured with
     *   [androidx.xr.runtime.Config.DeviceTrackingMode.DISABLED].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun detect(session: Session): Flow<Tilt> {
        return ArDevice.getInstance(session).state.runningFold(Tilt.UNKNOWN) { lastValue, state ->
            getTiltFromPoseRotation(lastValue, state.devicePose.rotation)
        }
    }

    /**
     * Determines the new tilt state based on the device's rotation and the previous tilt state,
     * incorporating a hysteresis mechanism to prevent rapid state flipping.
     *
     * Hysteresis is achieved by using separate thresholds for tilting up and down:
     * - To transition from [Tilt.UP] to [Tilt.DOWN], the angle must fall below the
     *   [TILT_DOWN_THRESHOLD].
     * - To transition from [Tilt.DOWN] to [Tilt.UP], the angle must rise above the
     *   [TILT_UP_THRESHOLD].
     *
     * This ensures that when the device's tilt angle is between the two thresholds, the state
     * remains stable and only changes when a threshold is definitively crossed.
     *
     * @param previousTilt The most recent [Tilt] state of the device.
     * @param rotation The device's current rotation, represented as a [Quaternion].
     * @return The new [Tilt] state, which will be either [Tilt.UP], [Tilt.DOWN], or [Tilt.UNKNOWN].
     */
    private fun getTiltFromPoseRotation(previousTilt: Tilt, rotation: Quaternion): Tilt {
        val angle = getTiltAngleFromQuaternion(rotation)

        return when (previousTilt) {
            Tilt.UNKNOWN -> { // Initial state or recovery from an unknown state
                if (angle <= TILT_DOWN_THRESHOLD) Tilt.DOWN else Tilt.UP
            }
            Tilt.UP -> {
                if (angle <= TILT_DOWN_THRESHOLD) Tilt.DOWN else Tilt.UP // Remains UP
            }
            Tilt.DOWN -> {
                if (angle >= TILT_UP_THRESHOLD) Tilt.UP else Tilt.DOWN // Remains DOWN
            }
            else -> Tilt.UNKNOWN
        }
    }

    /**
     * Calculates the tilt angle in degrees from a device's rotation quaternion.
     *
     * The tilt angle is defined within a specific coordinate system where:
     * - **0 degrees** corresponds to the device looking vertically downwards (-Z axis of device
     *   points towards world -Y).
     * - **90 degrees** corresponds to the device looking horizontally straight ahead.
     * - **180 degrees** corresponds to the device looking vertically upwards (-Z axis of device
     *   points towards world +Y).
     *
     * This function derives the angle by calculating the pitch from the quaternion and mapping it
     * to the desired 0-to-180-degree range.
     *
     * @param quaternion The rotation of the device pose.
     * @return The tilt angle in degrees (0-180), where 90 is horizontal. Returns Float.
     */
    private fun getTiltAngleFromQuaternion(quaternion: Quaternion): Float {
        // The device's forward vector is the local -Z axis. Its projection onto the world Y-axis
        // can be calculated from the quaternion. This value is equivalent to sin(pitch), where
        // a positive pitch indicates looking up and a negative pitch indicates looking down.
        val sinPitch = -(2.0f * quaternion.y * quaternion.z - 2.0f * quaternion.w * quaternion.x)

        // Clamp the value to the [-1.0, 1.0] range to prevent floating-point inaccuracies from
        // causing a domain error with asin().
        val clampedSinPitch = sinPitch.coerceIn(-1.0f, 1.0f)

        // Calculate the pitch in radians. This will range from -PI/2 (looking straight down)
        // to +PI/2 (looking straight up).
        val pitchRadians = asin(clampedSinPitch)

        // Convert pitch to degrees, resulting in a range of [-90, 90].
        val pitchDegrees = Math.toDegrees(pitchRadians.toDouble()).toFloat()

        // Remap the [-90, 90] pitch range to our target [0, 180] tilt angle range.
        // - A pitch of -90 degrees (down) becomes a tilt angle of 0.
        // - A pitch of 0 degrees (horizontal) becomes a tilt angle of 90.
        return pitchDegrees + 90.0f
    }
}
