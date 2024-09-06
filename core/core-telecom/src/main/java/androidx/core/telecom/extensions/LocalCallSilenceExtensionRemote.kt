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

import androidx.core.telecom.CallControlResult
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Interface used to allow the remote surface (automotive, watch, etc...) to know if the connected
 * calling application supports the local call silence extension.
 *
 * Local Call Silence means that the call should be silenced at the application layer (local
 * silence) instead of the hardware layer (global silence). Using a local call silence over global
 * silence is advantageous when the application wants to still receive the audio input data while
 * not transmitting audio input data to remote users.
 */
@ExperimentalAppActions
public interface LocalCallSilenceExtensionRemote {

    /**
     * Whether or not the local call silence extension is supported by the calling application.
     *
     * If `true`, then updates about the local call silence state will be notified. If `false`, then
     * the remote doesn't support this extension and the global silence
     * [android.telecom.InCallService.setMuted] should be used instead.
     *
     * Note: Must not be queried until after [CallExtensionScope.onConnected] is called.
     */
    public val isSupported: Boolean

    /**
     * Request the calling application to change the local call silence state.
     *
     * Note: A [CallControlResult.Success] result does not mean that the local call silence state of
     * the user has changed. It only means that the request was received by the remote application
     * and processed.
     *
     * @param isSilenced `true` signals the user wants to locally silence the call.
     * @return Whether or not the remote application received this event. This does not mean that
     *   the operation succeeded, but rather the remote received and processed the event
     *   successfully.
     */
    public suspend fun requestLocalCallSilenceUpdate(isSilenced: Boolean): CallControlResult
}
