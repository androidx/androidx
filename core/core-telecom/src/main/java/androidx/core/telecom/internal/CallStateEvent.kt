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

package androidx.core.telecom.internal

/**
 * CallState should be used internally to track the state the [CallSession] or [CallSessionLegacy]
 * is in.
 */
internal enum class CallStateEvent {
    NEW,
    DIALING,
    RINGING,
    ACTIVE,
    INACTIVE,
    DISCONNECTED,
    GLOBAL_MUTED,
    GLOBAL_UNMUTE;

    fun isCallControlState(): Boolean {
        return isFocusState() || isInactiveState()
    }

    fun isFocusState(): Boolean {
        return this == NEW || this == DIALING || this == RINGING || this == ACTIVE
    }

    fun isInactiveState(): Boolean {
        return this == INACTIVE || this == DISCONNECTED
    }

    fun isGlobalMuteState(): Boolean {
        return this == GLOBAL_UNMUTE || this == GLOBAL_MUTED
    }

    fun isMuted(): Boolean {
        return this == GLOBAL_MUTED
    }
}
