/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.extensions.voip

import android.os.ParcelUuid
import androidx.annotation.RestrictTo
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Singleton that encapsulates the CallControlScope extensions which have been added in as part of
 * supporting capabilities for VOIP app actions. Each delegate has a key (session id) that points
 * to the extension properties/functions available to the call session.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal class CallControlScopeExtensionSingleton private constructor(
    // Delegates to handle setting the CallControlScope extension properties for any given
    // instance.
    internal val PARTICIPANT_DELEGATE: MutableMap<ParcelUuid,
        VoipParticipantExtensionManager.ParticipantApiDelegate> = hashMapOf(),
//    internal val CALL_DETAILS_DELEGATE: MutableMap<ParcelUuid,
//        VoipCallDetailsExtensionManager.CallDetailsApiDelegate> = hashMapOf()
) {
    companion object {
        // Private volatile instance variable to hold the singleton instance to ensure read/write
        // ops are atomic.
        @Volatile
        private var INSTANCE: CallControlScopeExtensionSingleton? = null

        fun getInstance(): CallControlScopeExtensionSingleton {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = CallControlScopeExtensionSingleton()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}
