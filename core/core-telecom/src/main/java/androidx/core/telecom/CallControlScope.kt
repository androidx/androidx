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

import android.os.ParcelUuid
import kotlinx.coroutines.flow.Flow

/**
 * DSL interface to provide and receive updates about a single call session. The scope should be
 * used to provide updates to the call state and receive updates about a call state.  Example usage:
 * <pre>
 *     // initiate a call and control via the CallControlScope
 *     mCallsManager.addCall(callAttributes) { // This block represents the CallControlScope
 *
 *          // set your implementation of [CallControlCallback]
 *         setCallback(myCallControlCallbackImplementation)
 *
 *         // UI flow sends an update to a call state, relay the update to Telecom
 *         disconnectCallButton.setOnClickListener {
 *             val wasSuccessful = disconnect(reason) // waits for telecom async. response
 *             // update UI
 *         }
 *
 *         // Collect updates
 *         currentCallEndpoint
 *           .onEach { // access the new [CallEndpoint] here }
 *           .launchIn(coroutineScope)
 *     }
 * <pre>
 */
interface CallControlScope {
    /**
     * This method should be the first method called within the [CallControlScope] and your VoIP
     * application should pass in a valid implementation of [CallControlCallback].
     *
     * <p>
     * Failing to call this API may result in your VoIP process being killed or an error to occur.
     */
    @Suppress("ExecutorRegistration")
    fun setCallback(callControlCallback: CallControlCallback)

    /**
     * @return the 128-bit universally unique identifier Telecom assigned to this CallControlScope.
     * This id can be helpful for debugging when dumping the telecom system.
     */
    fun getCallId(): ParcelUuid

    /**
     * Inform Telecom that your app wants to make this call active. This method should be called
     * when either an outgoing call is ready to go active or a held call is ready to go active
     * again. For incoming calls that are ready to be answered, use [answer].
     *
     * Telecom will return true if your app is able to set the call active.  Otherwise false will
     * be returned (ex. another call is active and telecom cannot set this call active until the
     * other call is held or disconnected)
     */
    suspend fun setActive(): Boolean

    /**
     * Inform Telecom that your app wants to make this call inactive. This the same as hold for two
     * call endpoints but can be extended to setting a meeting to inactive.
     *
     * Telecom will return true if your app is able to set the call inactive. Otherwise, false will
     * be returned.
     */
    suspend fun setInactive(): Boolean

    /**
     * Inform Telecom that your app wants to make this incoming call active.  For outgoing calls
     * and calls that have been placed on hold, use [setActive].
     *
     * @param [callType] that call is to be answered as.
     *
     * Telecom will return true if your app is able to answer the call.  Otherwise false will
     * be returned (ex. another call is active and telecom cannot set this call active until the
     * other call is held or disconnected) which means that your app cannot answer this call at
     * this time.
     */
    suspend fun answer(@CallAttributesCompat.Companion.CallType callType: Int): Boolean

    /**
     * Inform Telecom that your app wishes to disconnect the call and remove the call from telecom
     * tracking.
     *
     * @param disconnectCause represents the cause for disconnecting the call.  The only valid
     *                        codes for the [android.telecom.DisconnectCause] passed in are:
     *                        <ul>
     *                        <li>[DisconnectCause#LOCAL]</li>
     *                        <li>[DisconnectCause#REMOTE]</li>
     *                        <li>[DisconnectCause#REJECTED]</li>
     *                        <li>[DisconnectCause#MISSED]</li>
     *                        </ul>
     *
     * Telecom will always return true unless the call has already been disconnected.
     *
     * <p>
     * Note: After the call has been successfully disconnected, calling any [CallControlScope] will
     * result in a false to be returned.
     */
    suspend fun disconnect(disconnectCause: android.telecom.DisconnectCause): Boolean

    /**
     * Request a [CallEndpointCompat] change. Clients should not define their own [CallEndpointCompat] when
     * requesting a change. Instead, the new [endpoint] should be one of the valid [CallEndpointCompat]s
     * provided by [availableEndpoints].
     *
     * @param endpoint The [CallEndpointCompat] to change to.
     *
     * Telecom will return true if your app is able to switch to the requested new endpoint.
     * Otherwise false will be returned.
     */
    suspend fun requestEndpointChange(endpoint: CallEndpointCompat): Boolean

    /**
     * Collect the new [CallEndpointCompat] through which call media flows (i.e. speaker,
     * bluetooth, etc.).
     */
    val currentCallEndpoint: Flow<CallEndpointCompat>

    /**
     * Collect the set of available [CallEndpointCompat]s reported by Telecom.
     */
    val availableEndpoints: Flow<List<CallEndpointCompat>>

    /**
     * Collect the current mute state of the call. This Flow is updated every time the mute state
     * changes.
     */
    val isMuted: Flow<Boolean>
}