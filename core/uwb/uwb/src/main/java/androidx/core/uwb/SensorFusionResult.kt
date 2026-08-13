/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.uwb

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** An asynchronous update from a sensor fusion session. */
public sealed interface SensorFusionResult {
    public val device: UwbDevice

    /**
     * An estimate of the peer device's position produced from the sensor fusion algorithm.
     *
     * @property device The peer UWB device.
     * @property distance The line-of-sight distance in meters of the ranging device.
     * @property azimuth The azimuth angle in degrees of the ranging device, or null if not
     *   available. The range is [-90, 90].
     * @property elevation The elevation angle in degrees of the ranging device, or null if not
     *   available. The range is [-90, 90].
     * @property elapsedRealtimeNanos The elapsed realtime in nanos from when the system booted up
     *   to this position estimate.
     */
    public sealed interface Estimate : SensorFusionResult {
        override val device: UwbDevice
        public val distance: RangingMeasurement
        public val azimuth: RangingMeasurement?
        public val elevation: RangingMeasurement?
        public val elapsedRealtimeNanos: Long
    }

    /**
     * An [Estimate] backed by both odometry data and range data.
     *
     * @property azimuth The azimuth angle in degrees of the ranging device. The range is [-90, 90].
     */
    public class PreciseEstimate(
        public override val device: UwbDevice,
        public override val distance: RangingMeasurement,
        public override val azimuth: RangingMeasurement,
        public override val elevation: RangingMeasurement?,
        public override val elapsedRealtimeNanos: Long,
    ) : Estimate

    /**
     * An [Estimate] backed by odometry data only because raw UWB range has failed. Continuous
     * drifting will lead to inaccurate results over time.
     *
     * @property azimuth The azimuth angle in degrees of the ranging device. The range is [-90, 90].
     */
    public class DriftingEstimate(
        public override val device: UwbDevice,
        public override val distance: RangingMeasurement,
        public override val azimuth: RangingMeasurement,
        public override val elevation: RangingMeasurement?,
        public override val elapsedRealtimeNanos: Long,
    ) : Estimate

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ESTIMATE_FAILURE_REASON_INSUFFICIENT_LIGHT,
        ESTIMATE_FAILURE_REASON_EXCESSIVE_MOTION,
        ESTIMATE_FAILURE_REASON_INSUFFICIENT_FEATURES,
        ESTIMATE_FAILURE_REASON_CAMERA_UNAVAILABLE,
        ESTIMATE_FAILURE_REASON_NOT_AVAILABLE,
        ESTIMATE_FAILURE_REASON_PEER_MOVING,
        ESTIMATE_FAILURE_REASON_INCONCLUSIVE_RESULT,
        ESTIMATE_FAILURE_REASON_BAD_STATE,
    )
    public annotation class EstimateFailureReason

    /**
     * An [Estimate] backed by raw UWB range only because the odometry sensor has failed. The
     * estimate will not have AoA.
     */
    public class ImpreciseEstimate(
        public override val device: UwbDevice,
        public override val distance: RangingMeasurement,
        public override val elapsedRealtimeNanos: Long,
        @get:EstimateFailureReason public val reason: Int,
    ) : Estimate {
        @Deprecated(
            message = "ImpreciseEstimate never has azimuth.",
            level = DeprecationLevel.HIDDEN,
        )
        public override val azimuth: RangingMeasurement? = null

        @Deprecated(
            message = "ImpreciseEstimate never has elevation.",
            level = DeprecationLevel.HIDDEN,
        )
        public override val elevation: RangingMeasurement? = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        FALLBACK_REASON_NOT_AVAILABLE,
        FALLBACK_REASON_HARDWARE_AOA_PRECEDENCE,
        FALLBACK_REASON_ARCORE_CAMERA_NOT_AVAILABLE,
        FALLBACK_REASON_ARCORE_MISSING_GL_CONTEXT,
        FALLBACK_REASON_ARCORE_SDK_TOO_OLD,
        FALLBACK_REASON_ARCORE_DEVICE_NOT_COMPATIBLE,
    )
    public annotation class SensorFusionFallbackReason

    /**
     * Sensor fusion failed to start and the session will fall back to UWB ranging.
     *
     * @property device The peer UWB device.
     * @property reason Reason code indicating why sensor fusion failed to start
     */
    public class SensorFusionFallback(
        override val device: UwbDevice,
        @get:SensorFusionFallbackReason public val reason: Int,
    ) : SensorFusionResult

    public companion object {
        /**
         * Failed because AoA is transiently unavailable for an expected reason, such as during
         * initialization. This should resolve itself soon.
         */
        public const val ESTIMATE_FAILURE_REASON_NOT_AVAILABLE: Int = 1

        /**
         * ARCore failed due to poor lighting conditions. Ask the user to move to a more brightly
         * lit area.
         */
        public const val ESTIMATE_FAILURE_REASON_INSUFFICIENT_LIGHT: Int = 2

        /** ARCore failed due to excessive motion. Ask the user to move the device more slowly. */
        public const val ESTIMATE_FAILURE_REASON_EXCESSIVE_MOTION: Int = 3

        /**
         * ARCore failed due to insufficient visual features. Ask the user to move to a different
         * area and to avoid blank walls and surfaces without detail.
         */
        public const val ESTIMATE_FAILURE_REASON_INSUFFICIENT_FEATURES: Int = 4

        /**
         * ARCore failed because the camera is in use by another application. Odometry will resume
         * once this app regains priority, or once all apps with higher priority have stopped using
         * the camera.
         */
        public const val ESTIMATE_FAILURE_REASON_CAMERA_UNAVAILABLE: Int = 5

        /**
         * Failed because excessive peer motion was detected. The algorithm works best when the peer
         * is stationary.
         */
        public const val ESTIMATE_FAILURE_REASON_PEER_MOVING: Int = 6

        /**
         * Failed because the algorithm produced a bimodal or otherwise inconclusive result. Ask the
         * user to move in a different direction than was done previously to help the algorithm
         * converge.
         */
        public const val ESTIMATE_FAILURE_REASON_INCONCLUSIVE_RESULT: Int = 7

        /**
         * Failed because the sensor fusion algorithm reached a bad state. No user action is likely
         * to fix this, consider restarting the session.
         */
        public const val ESTIMATE_FAILURE_REASON_BAD_STATE: Int = 8

        /** Sensor fusion is not available on this device. */
        public const val FALLBACK_REASON_NOT_AVAILABLE: Int = 0

        /**
         * This device supports hardware AoA which will be used instead of sensor fusion.
         *
         * To use sensor fusion even when hardware AoA is supported, configure the session with
         * [RangingParameters.isAoaDisabled] set to `true`.
         */
        public const val FALLBACK_REASON_HARDWARE_AOA_PRECEDENCE: Int = 1

        /** ARCore failed to start because the camera is in use by another app or process. */
        public const val FALLBACK_REASON_ARCORE_CAMERA_NOT_AVAILABLE: Int = 2

        /**
         * The ARCore SDK that the client was built with is too old and is no longer supported by
         * the installed ARCore APK.
         */
        public const val FALLBACK_REASON_ARCORE_SDK_TOO_OLD: Int = 3

        /**
         * The device is not compatible with ARCore. If encountered after completing the
         * [installation steps](https://developers.google.com/ar/develop/java/enable-arcore), this
         * usually indicates that ARCore has been side-loaded onto an incompatible device.
         */
        public const val FALLBACK_REASON_ARCORE_DEVICE_NOT_COMPATIBLE: Int = 4

        /**
         * ARCore failed to start because the necessary [android.opengl.EGLContext] is not available
         * on the polling thread.
         *
         * **NOTE:** This should never occur unless there has been an internal failure. The ARCore
         * session and necessary context object is managed within the UWB stack.
         */
        public const val FALLBACK_REASON_ARCORE_MISSING_GL_CONTEXT: Int = 5
    }
}
