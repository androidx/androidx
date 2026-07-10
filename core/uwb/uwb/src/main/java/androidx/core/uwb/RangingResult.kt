/*
 * Copyright (C) 2022 The Android Open Source Project
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

/** Class for UWB ranging result update. */
public sealed class RangingResult {
    public abstract val device: UwbDevice

    /**
     * A ranging result with the device position update.
     *
     * @property device The peer UWB device.
     * @property position Position of the UWB device during Ranging.
     */
    public class RangingResultPosition(
        override val device: UwbDevice,
        public val position: RangingPosition,
    ) : RangingResult()

    /**
     * A ranging result with peer disconnected status update.
     *
     * @property device The peer UWB device that disconnected.
     * @property reason The reason code indicating why the failure occurred.
     */
    public class RangingResultPeerDisconnected(
        override val device: UwbDevice,
        @RangingFailureReason public val reason: Int,
    ) : RangingResult()

    /**
     * A ranging result when a ranging session is initialized with peer device.
     *
     * @property device The peer UWB device.
     */
    public class RangingResultInitialized(override val device: UwbDevice) : RangingResult()

    /**
     * A ranging result indicating failure or suspension of a ranging session.
     *
     * @property device The local UWB device on which the operation failed.
     * @property reason The reason code indicating why the failure occurred.
     */
    public class RangingResultFailure(
        override val device: UwbDevice,
        @RangingFailureReason public val reason: Int,
    ) : RangingResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        RANGING_FAILURE_REASON_UNKNOWN,
        RANGING_FAILURE_REASON_BAD_PARAMETERS,
        RANGING_FAILURE_REASON_FAILED_TO_START,
        RANGING_FAILURE_REASON_STOPPED_BY_PEER,
        RANGING_FAILURE_REASON_STOPPED_BY_LOCAL,
        RANGING_FAILURE_REASON_SYSTEM_POLICY,
        RANGING_FAILURE_REASON_MAX_RR_RETRY_REACHED,
    )
    public annotation class RangingFailureReason

    public companion object {
        /** Unknown failure reason. */
        public const val RANGING_FAILURE_REASON_UNKNOWN: Int = 0

        /** Ranging failed due to bad or invalid parameters. */
        public const val RANGING_FAILURE_REASON_BAD_PARAMETERS: Int = 1

        /** Generic failure to start the ranging session. */
        public const val RANGING_FAILURE_REASON_FAILED_TO_START: Int = 2

        /** The ranging session was stopped by the peer device. */
        public const val RANGING_FAILURE_REASON_STOPPED_BY_PEER: Int = 3

        /** The ranging session was stopped by a local request. */
        public const val RANGING_FAILURE_REASON_STOPPED_BY_LOCAL: Int = 4

        /** Ranging stopped or failed due to system policy. */
        public const val RANGING_FAILURE_REASON_SYSTEM_POLICY: Int = 5

        /** Ranging stopped because the maximum number of ranging round retries was reached. */
        public const val RANGING_FAILURE_REASON_MAX_RR_RETRY_REACHED: Int = 6

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @RangingFailureReason
        internal fun fromId(id: Int): Int {
            return when (id) {
                0 -> RANGING_FAILURE_REASON_UNKNOWN
                1 -> RANGING_FAILURE_REASON_BAD_PARAMETERS
                2 -> RANGING_FAILURE_REASON_FAILED_TO_START
                3 -> RANGING_FAILURE_REASON_STOPPED_BY_PEER
                4 -> RANGING_FAILURE_REASON_STOPPED_BY_LOCAL
                5 -> RANGING_FAILURE_REASON_SYSTEM_POLICY
                6 -> RANGING_FAILURE_REASON_MAX_RR_RETRY_REACHED
                else -> RANGING_FAILURE_REASON_UNKNOWN
            }
        }
    }
}
