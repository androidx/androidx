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

package androidx.core.telecom.extensions

/** Internal constants related to Extensions that do not need to be exposed as a public API. */
internal object Extensions {
    internal const val LOG_TAG = "CallsManagerE"

    /**
     * EVENT used by InCallService as part of sendCallEvent to notify the VOIP Application that this
     * InCallService supports jetpack extensions
     */
    internal const val EVENT_JETPACK_CAPABILITY_EXCHANGE =
        "android.telecom.event.CAPABILITY_EXCHANGE"

    /** VERSION used for handling future compatibility in capability exchange. */
    internal const val EXTRA_CAPABILITY_EXCHANGE_VERSION =
        "androidx.core.telecom.extensions.extra.CAPABILITY_EXCHANGE_VERSION"

    /**
     * BINDER used for handling capability exchange between the ICS and VOIP app sides, sent as part
     * of sendCallEvent in the included extras.
     */
    internal const val EXTRA_CAPABILITY_EXCHANGE_BINDER =
        "androidx.core.telecom.extensions.extra.CAPABILITY_EXCHANGE_BINDER"

    /** Represents the [ParticipantExtension] extension */
    internal const val PARTICIPANT = 1
}
