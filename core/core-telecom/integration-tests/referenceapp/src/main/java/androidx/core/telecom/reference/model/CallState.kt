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

package androidx.core.telecom.reference.model

/**
 * Represents the various possible states a phone call can transition through during its lifecycle,
 * from initiation to termination.
 *
 * This enum covers both outgoing and incoming call scenarios.
 */
enum class CallState {
    /**
     * State for an outgoing call immediately after initiation, before it is connected. The device
     * is attempting to reach the recipient.
     */
    DIALING,

    /**
     * State for an incoming call that has been received by the device but not yet answered. The
     * device is alerting the user (e.g., ringing).
     */
    RINGING,

    /**
     * State representing a call that is currently connected and ongoing. Communication is
     * established between the parties.
     */
    ACTIVE,

    /**
     * State representing a call that is currently on hold (set to inactive). The connection is
     * maintained, but audio transmission is typically paused.
     */
    INACTIVE,

    /**
     * State representing a call that has ended, either normally or due to an error. The connection
     * is terminated.
     */
    DISCONNECTED,

    /**
     * An unknown or initial state, used when the call's state cannot be determined or does not fit
     * into the other defined categories.
     */
    UNKNOWN,
}
