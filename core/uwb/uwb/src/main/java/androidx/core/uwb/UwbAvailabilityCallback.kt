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

package androidx.core.uwb

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Callback used for UWB availability. */
public interface UwbAvailabilityCallback {
    /**
     * Callback when the UWB state is changed
     *
     * @param isAvailable True if UWB is switched on, false otherwise
     * @param reason The reason of UWB state change. Possible reasons are
     *   [STATE_CHANGE_REASON_UNKNOWN], [STATE_CHANGE_REASON_SYSTEM_POLICY] and
     *   [STATE_CHANGE_REASON_COUNTRY_CODE_ERROR]
     */
    public fun onUwbStateChanged(isAvailable: Boolean, @StateChangeReasonCode reason: Int)

    public companion object {
        /** The state has changed because of an unknown reason */
        public const val STATE_CHANGE_REASON_UNKNOWN: Int = 0

        /** The state has changed because UWB is turned on/off */
        public const val STATE_CHANGE_REASON_SYSTEM_POLICY: Int = 1

        /**
         * The state has changed either because no country code has been configured or due to UWB
         * being unavailable as a result of regulatory constraints.
         */
        public const val STATE_CHANGE_REASON_COUNTRY_CODE_ERROR: Int = 2

        /** UWB state change reason code. */
        @IntDef(
            value =
                [
                    STATE_CHANGE_REASON_UNKNOWN,
                    STATE_CHANGE_REASON_SYSTEM_POLICY,
                    STATE_CHANGE_REASON_COUNTRY_CODE_ERROR,
                ]
        )
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        public annotation class StateChangeReasonCode {}
    }
}
