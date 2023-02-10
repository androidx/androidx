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

package androidx.core.telecom

/**
 * CallControlCallback relays call updates (that require a response) from the Telecom framework out
 * to the application. This can include operations which the app must implement on a Call due to the
 * presence of other calls on the device, requests relayed from a Bluetooth device, or from another
 * calling surface.
 *
 * <p>
 * All CallControlCallbacks are transactional, meaning that a client must
 * complete the suspend fun with a [Boolean] response in order to complete the
 * CallControlCallback. If the operation has been completed, the [suspend fun] should return
 * true. Otherwise, the suspend fun should be returned with a false to represent the
 * CallControlCallback cannot be completed on the client side.
 *
 * <p>
 * Note: Each CallEventCallback has a timeout of 5000 milliseconds. Failing to complete the
 * suspend fun before the timeout will result in a failed transaction.
 */
interface CallControlCallback {
    /**
     * Telecom is informing the client to set the call active.
     *
     * @return true if your VoIP application can set the call (that corresponds to this
     * CallControlCallback) to active. Otherwise, return false.
     */
    suspend fun onSetActive(): Boolean

    /**
     * Telecom is informing the client to set the call inactive. This is the same as holding a call
     * for two endpoints but can be extended to setting a meeting inactive.
     *
     * @return true if your app VoIP application has move the call to an inactive state. Your app
     * must stop using the microphone and playing incoming media when returning.
     */
    suspend fun onSetInactive(): Boolean

    /**
     * Telecom is informing the client to answer an incoming call and set it to active.
     *
     * @param callType that call is requesting to be answered as.
     *
     * @return true if your VoIP application can set the call (that corresponds to this
     * CallControlCallback) to active. Otherwise, return false.
     */
    suspend fun onAnswer(@CallAttributes.Companion.CallType callType: Int): Boolean

    /**
     * Telecom is informing the client to disconnect the call
     *
     * @param disconnectCause represents the cause for disconnecting the call.
     *
     * @return true when your VoIP application has disconnected the call. Otherwise, return false.
     */
    suspend fun onDisconnect(disconnectCause: android.telecom.DisconnectCause): Boolean
}