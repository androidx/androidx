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

import androidx.annotation.FloatRange
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Quaternion
import kotlin.math.asin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.runningFold

/**
 * Marks declarations that are part of the experimental Tilt Gesture API.
 *
 * This API is subject to change or removal in a future release.
 */
@RequiresOptIn(message = "This is an experimental API. It may be changed or removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalGesturesApi

/** Represents the vertical tilt state of the device. */
@ExperimentalGesturesApi
public class Tilt private constructor(private val value: Int) {
    public companion object {
        /** The device is tilted upwards, surpassing the defined upper threshold. */
        @JvmField public val UP: Tilt = Tilt(0)

        /** The device is tilted downwards, surpassing the defined lower threshold. */
        @JvmField public val DOWN: Tilt = Tilt(1)

        /** The device is in the process of tilting upwards. */
        internal val TRANSITIONING_UP: Tilt = Tilt(2)

        /** The device is in the process of tilting downwards. */
        internal val TRANSITIONING_DOWN: Tilt = Tilt(3)
    }

    override fun toString(): String {
        return when (this) {
            UP -> "Tilt.UP"
            DOWN -> "Tilt.DOWN"
            TRANSITIONING_UP -> "Tilt.TRANSITIONING_UP"
            TRANSITIONING_DOWN -> "Tilt.TRANSITIONING_DOWN"
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
@ExperimentalGesturesApi
public object TiltGesture {
    /**
     * The lower angle threshold in degrees to trigger a transition to the `Tilt.DOWN` state. See
     * [getTiltAngleFromQuaternion] for angle definition.
     */
    private const val TILT_DOWN_START_THRESHOLD = 65.0f

    /** The angle at which the `Tilt.DOWN` transition is considered complete. */
    private const val TILT_DOWN_COMPLETE_THRESHOLD = 60.0f

    /**
     * The upper angle threshold in degrees to trigger a transition to the `Tilt.UP` state. See
     * [getTiltAngleFromQuaternion] for angle definition.
     */
    private const val TILT_UP_START_THRESHOLD = 80.0f

    /** The angle at which the `Tilt.UP` transition is considered complete. */
    private const val TILT_UP_COMPLETE_THRESHOLD = 85.0f

    /**
     * Begins detecting the device's tilt state and returns a cold [Flow] of [Tilt] updates.
     *
     * This function observes changes to the [ArDevice.state] obtained from the provided [session].
     * The device's tilt is calculated based on the `devicePose.rotation` from this state.
     *
     * **Precondition**: The [session] must be configured with device tracking set to
     * [androidx.xr.runtime.DeviceTrackingMode.SPATIAL_LAST_KNOWN]. If
     * [androidx.xr.runtime.DeviceTrackingMode.DISABLED] is used, this function will throw an
     * [IllegalStateException] when attempting to acquire the [ArDevice] instance.
     *
     * The returned flow is **cold**: a new stream of tilt updates is created for each collector.
     * The flow emits new [State] objects containing [Tilt] (i.e., [Tilt.UP] or [Tilt.DOWN]) based
     * on the orientation derived from the device pose.
     *
     * It is recommended to collect this flow from a coroutine scope with the same coroutine context
     * used to create the XR [Session], as changes to [ArDevice.state] drive the emissions.
     *
     * @param session the active XR session configured with
     *   [androidx.xr.runtime.DeviceTrackingMode.SPATIAL_LAST_KNOWN]
     * @return a [Flow] that emits the current [State], starting with an initial state of
     *   [State.tilt] as [Tilt.UP] and [State.progress] as 0f
     * @throws IllegalStateException if [session] is configured with
     *   [androidx.xr.runtime.DeviceTrackingMode.DISABLED]
     */
    public fun detect(session: Session): Flow<State> {
        return ArDevice.getInstance(session).state.runningFold(State()) { lastValue, state ->
            getTiltFromPoseRotation(lastValue.internalTiltValue, state.devicePose.rotation)
        }
    }

    /**
     * Represents the tilt state of the device, transition progress.
     *
     * @property tilt the current tilt state ([Tilt.UP] or [Tilt.DOWN])
     * @property progress a value from 0.0 to 1.0 indicating the progress of the current transition
     */
    @ExperimentalGesturesApi
    public class State(
        public val tilt: Tilt = Tilt.UP,
        @FloatRange(from = 0.0, to = 1.0) public val progress: Float = 0f,
    ) {
        internal var internalTiltValue: Tilt = Tilt.UP
            private set

        internal constructor(
            tilt: Tilt,
            internalTiltValue: Tilt,
            progress: Float,
        ) : this(tilt = tilt, progress = progress) {
            this.internalTiltValue = internalTiltValue
        }

        override fun toString(): String {
            return "TiltGesture.State(tilt=$tilt, progress=$progress)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return tilt == other.tilt && progress == other.progress
        }

        override fun hashCode(): Int {
            var result = tilt.hashCode()
            result = 31 * result + progress.hashCode()
            return result
        }
    }

    /**
     * Determines the new tilt state based on the device's rotation and the previous tilt state,
     * incorporating a hysteresis mechanism to prevent rapid state flipping.
     *
     * Hysteresis is achieved by using separate thresholds for tilting up and down: - To transition
     * from [Tilt.UP] to [Tilt.DOWN], the angle must fall below the
     * [TILT_DOWN_COMPLETE_THRESHOLD]. - To transition from [Tilt.DOWN] to [Tilt.UP], the angle must
     * rise above the [TILT_UP_COMPLETE_THRESHOLD].
     *
     * This ensures that when the device's tilt angle is between the two thresholds, the state
     * remains stable and only changes when a threshold is definitively crossed.
     *
     * @param previousTilt the most recent [Tilt] state of the device
     * @param rotation the device's current rotation, represented as a [Quaternion]
     * @return the new [State] including tilt and progress
     */
    private fun getTiltFromPoseRotation(previousTilt: Tilt, rotation: Quaternion): State {
        val angle = getTiltAngleFromQuaternion(rotation)
        when (previousTilt) {
            Tilt.UP -> return handleStateUp(angle)
            Tilt.DOWN -> return handleStateDown(angle)
            Tilt.TRANSITIONING_DOWN -> return handleStateTransitioningDown(angle)
            Tilt.TRANSITIONING_UP -> return handleStateTransitioningUp(angle)
        }

        return State()
    }

    /**
     * Handles the tilt state logic when the device's internal state is [Tilt.UP].
     *
     * It checks if the current tilt [angle] has crossed the [TILT_DOWN_START_THRESHOLD] to initiate
     * a downward transition.
     *
     * @param angle the current tilt angle in degrees (0-180)
     * @return a new [State] reflecting the potential transition
     */
    private fun handleStateUp(angle: Float): State {
        val newInternalTiltValue =
            if (angle > TILT_DOWN_START_THRESHOLD) Tilt.UP else Tilt.TRANSITIONING_DOWN
        return State(Tilt.UP, newInternalTiltValue, 0f)
    }

    /**
     * Handles the tilt state logic when the device's internal state is [Tilt.DOWN].
     *
     * It checks if the current tilt [angle] has crossed the [TILT_UP_START_THRESHOLD] to initiate
     * an upward transition.
     *
     * @param angle the current tilt angle in degrees (0-180)
     * @return a new [State] reflecting the potential transition
     */
    private fun handleStateDown(angle: Float): State {
        val newInternalTiltValue =
            if (angle < TILT_UP_START_THRESHOLD) Tilt.DOWN else Tilt.TRANSITIONING_UP
        return State(Tilt.DOWN, newInternalTiltValue, 0f)
    }

    /**
     * Handles the tilt state logic when the device's internal state is [Tilt.TRANSITIONING_DOWN].
     *
     * It determines if the transition has completed (angle <= [TILT_DOWN_COMPLETE_THRESHOLD]), been
     * canceled (angle > [TILT_DOWN_START_THRESHOLD]), or is still in progress. Calculates the
     * progress value for the transition.
     *
     * @param angle the current tilt angle in degrees (0-180)
     * @return a new [State] reflecting the transition's status and progress
     */
    private fun handleStateTransitioningDown(angle: Float): State {
        return if (angle <= TILT_DOWN_COMPLETE_THRESHOLD) {
            // Completed
            State(Tilt.DOWN, Tilt.DOWN, 1.0f)
        } else if (angle > TILT_DOWN_START_THRESHOLD) {
            // Canceled: user tilted back up before completing
            State(Tilt.UP, Tilt.UP, 0.0f)
        } else {
            // In progress
            val range = TILT_DOWN_START_THRESHOLD - TILT_DOWN_COMPLETE_THRESHOLD
            val progress = (TILT_DOWN_START_THRESHOLD - angle) / range
            State(Tilt.UP, Tilt.TRANSITIONING_DOWN, progress.coerceIn(0f, 1f))
        }
    }

    /**
     * Handles the tilt state logic when the device's internal state is [Tilt.TRANSITIONING_UP].
     *
     * It determines if the transition has completed (angle >= [TILT_UP_COMPLETE_THRESHOLD]), been
     * canceled (angle < [TILT_UP_START_THRESHOLD]), or is still in progress. Calculates the
     * progress value for the transition.
     *
     * @param angle the current tilt angle in degrees (0-180)
     * @return a new [State] reflecting the transition's status and progress
     */
    private fun handleStateTransitioningUp(angle: Float): State {
        return if (angle >= TILT_UP_COMPLETE_THRESHOLD) {
            // Completed
            State(Tilt.UP, Tilt.UP, 1.0f)
        } else if (angle < TILT_UP_START_THRESHOLD) {
            // Canceled: user tilted back down before completing
            State(Tilt.DOWN, Tilt.DOWN, 0.0f)
        } else {
            // In progress
            val range = TILT_UP_COMPLETE_THRESHOLD - TILT_UP_START_THRESHOLD
            val progress = (angle - TILT_UP_START_THRESHOLD) / range
            State(Tilt.DOWN, Tilt.TRANSITIONING_UP, progress.coerceIn(0f, 1f))
        }
    }

    /**
     * Calculates the tilt angle in degrees from a device's rotation quaternion.
     *
     * The tilt angle is defined within a specific coordinate system where: - **0 degrees**
     * corresponds to the device looking vertically downwards (-Z axis of device points towards
     * world -Y). - **90 degrees** corresponds to the device looking horizontally straight ahead. -
     * **180 degrees** corresponds to the device looking vertically upwards (-Z axis of device
     * points towards world +Y).
     *
     * This function derives the angle by calculating the pitch from the quaternion and mapping it
     * to the desired 0-to-180-degree range.
     *
     * @param quaternion the rotation of the device pose
     * @return the tilt angle in degrees (0-180), where 90 is horizontal
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
