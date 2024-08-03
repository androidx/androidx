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

import android.os.Build.VERSION_CODES
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Provide the ability for [CallsManager] to support extensions on a call.
 *
 * Extensions allow an application to support optional features beyond the scope of call state
 * management and audio routing. These optional features provide the application with the ability to
 * describe additional information about the call, which allows remote surfaces (automotive,
 * watches, etc..) to provide UX related to this additional information. Additionally, remote
 * surfaces can perform actions using a configured extension to notify this application of a remote
 * user request.
 *
 * @see ExtensionInitializationScope
 */
public interface CallsManagerExtensions {
    /**
     * Adds a call with extensions support using [ExtensionInitializationScope], which allows an app
     * to implement optional additional actions that go beyond the scope of a call, such as
     * information about meeting participants and icons.
     *
     * @param callAttributes attributes of the new call (incoming or outgoing, address, etc. )
     * @param onAnswer where callType is the audio/video state the call should be answered as.
     *   Telecom is informing your VoIP application to answer an incoming call and set it to active.
     *   Telecom is requesting this on behalf of an system service (e.g. Automotive service) or a
     *   device (e.g. Wearable).
     * @param onDisconnect where disconnectCause represents the cause for disconnecting the call.
     *   Telecom is informing your VoIP application to disconnect the incoming call. Telecom is
     *   requesting this on behalf of an system service (e.g. Automotive service) or a device (e.g.
     *   Wearable).
     * @param onSetActive Telecom is informing your VoIP application to set the call active. Telecom
     *   is requesting this on behalf of an system service (e.g. Automotive service) or a device
     *   (e.g. Wearable).
     * @param onSetInactive Telecom is informing your VoIP application to set the call inactive.
     *   This is the same as holding a call for two endpoints but can be extended to setting a
     *   meeting inactive. Telecom is requesting this on behalf of an system service (e.g.
     *   Automotive service) or a device (e.g.Wearable). Note: Your app must stop using the
     *   microphone and playing incoming media when returning.
     * @param init The scope used to first initialize Extensions that will be used when the call is
     *   first notified to the platform and UX surfaces. Once the call is set up, the user's
     *   implementation of [ExtensionInitializationScope.onCall] will be called.
     * @see CallsManager.addCall
     */
    @RequiresApi(VERSION_CODES.O)
    @ExperimentalAppActions
    public suspend fun addCallWithExtensions(
        callAttributes: CallAttributesCompat,
        onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
        onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        onSetActive: suspend () -> Unit,
        onSetInactive: suspend () -> Unit,
        init: suspend ExtensionInitializationScope.() -> Unit
    )
}
