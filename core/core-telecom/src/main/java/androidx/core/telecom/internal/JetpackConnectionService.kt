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
package androidx.core.telecom.internal

import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.telecom.CallException
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.utils.Utils
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred

@RequiresApi(api = Build.VERSION_CODES.O)
internal class JetpackConnectionService : ConnectionService() {
    private val TAG = JetpackConnectionService::class.java.simpleName

    /** Wrap all the objects that are associated with a new CallSession request into a class */
    data class PendingConnectionRequest(
        /**
         * requestIdMatcher - is important for matching requests sent to the platform via
         * TelecomManage#placeCall(...,extras) or TelecomManager#addIncomingCall(..., extras) and
         * receiving the same platform request (shortly after) via
         * ConnectionService#onOutgoingConnection*(...,request.extras) and
         * ConnectionService#onIncomingConnection*(...,request.extras). Without this, there is no
         * way to match client CallsManager#addCall requests to Connections the ConnectionService
         * gets from the platform.
         */
        val requestIdMatcher: String,
        val callAttributes: CallAttributesCompat,
        val callChannel: CallChannels,
        val coroutineContext: CoroutineContext,
        val completableDeferred: CompletableDeferred<AddCallResult>?,
        val onAnswer: suspend (callType: Int) -> Unit,
        val onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        val onSetActive: suspend () -> Unit,
        val onSetInactive: suspend () -> Unit,
        val onEvent: suspend (event: String, extras: Bundle) -> Unit,
        val preferredStartingCallEndpoint: CallEndpointCompat? = null,
        val execution: CompletableDeferred<Unit>
    )

    companion object {
        const val REQUEST_ID_MATCHER_KEY = "JetpackConnectionService_requestIdMatcher_key"
        const val KEY_NOT_FOUND = "requestIdMatcher KEY NOT FOUND"
        const val TAG = "JetpackCS"
        const val CONNECTION_CREATION_TIMEOUT: Long = 5000 // time in milli-seconds
        var mPendingConnectionRequests: ArrayList<PendingConnectionRequest> = ArrayList()
    }

    /**
     * Request the Platform create a new Connection with the properties given by
     * [CallAttributesCompat]. This request will have a timeout of [CONNECTION_CREATION_TIMEOUT] and
     * be removed when the result is completed.
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    fun createConnectionRequest(
        telecomManager: TelecomManager,
        pendingConnectionRequest: PendingConnectionRequest,
    ) {
        Log.i(
            TAG,
            "CreationConnectionRequest:" +
                " requestIdMatcher=[${pendingConnectionRequest.requestIdMatcher}]" +
                " phoneAccountHandle=[${pendingConnectionRequest.callAttributes.mHandle}]"
        )

        mPendingConnectionRequests.add(pendingConnectionRequest)

        val extras =
            Utils.getBundleWithPhoneAccountHandle(
                pendingConnectionRequest.callAttributes,
                pendingConnectionRequest.callAttributes.mHandle!!
            )

        val idBundle = Bundle()
        idBundle.putString(REQUEST_ID_MATCHER_KEY, pendingConnectionRequest.requestIdMatcher)

        // Call into the platform to start call
        if (pendingConnectionRequest.callAttributes.isOutgoingCall()) {
            extras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, idBundle)
            telecomManager.placeCall(pendingConnectionRequest.callAttributes.address, extras)
        } else {
            extras.putBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, idBundle)
            telecomManager.addNewIncomingCall(
                pendingConnectionRequest.callAttributes.mHandle,
                extras
            )
        }
    }

    /** Outgoing Connections */
    override fun onCreateOutgoingConnection(
        connectionManagerAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.i(
            TAG,
            "onCreateOutgoingConnection: " +
                "connectionMgrAcct=[$connectionManagerAccount], request=[$request]"
        )
        if (request == null) {
            // if the Platform provides a null request, there is no way to complete the new request
            // for a backwards compat call.  In this event, Core-Telecom needs to return a failed
            // Connection to platform to end the call and ensure Telecom is left in a good state.
            // The application will hit a timeout for the new addCall request and any other
            // CallSessions will be unaffected.
            return Connection.createFailedConnection(
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "ConnectionRequest is null, cannot complete the addCall request"
                )
            )
        }
        return createSelfManagedConnection(request, CallAttributesCompat.DIRECTION_OUTGOING)
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.i(
            TAG,
            "onCreateOutgoingConnectionFailed: " +
                "connectionMgrAcct=[$connectionManagerPhoneAccount], request=[$request]"
        )
        if (request == null) {
            return
        }
        val pendingRequest: PendingConnectionRequest? = getPendingConnectionRequest(request)
        mPendingConnectionRequests.remove(pendingRequest)
        // Immediately complete the CompletableDeferred and throw a CallException out to the client
        // to indicate this addCall request failed!
        completeAddCallRequestWithException(pendingRequest)
    }

    /** Incoming Connections */
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.i(
            TAG,
            "onCreateIncomingConnection: " +
                "connectionManagerPhoneAccount=[$connectionManagerPhoneAccount], request=[$request]"
        )
        if (request == null) {
            // if the Platform provides a null request, there is no way to complete the new request
            // for a backwards compat call.  In this event, Core-Telecom needs to return a failed
            // Connection to platform to end the call and ensure Telecom is left in a good state.
            // The application will hit a timeout for the new addCall request and any other
            // CallSessions will be unaffected.
            return Connection.createFailedConnection(
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "ConnectionRequest is null, cannot complete the addCall request"
                )
            )
        }
        return createSelfManagedConnection(request, CallAttributesCompat.DIRECTION_INCOMING)
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.i(
            TAG,
            "onCreateIncomingConnectionFailed: " +
                "connectionMgrAcct=[$connectionManagerPhoneAccount], request=[$request]"
        )
        if (request == null) {
            return
        }
        val pendingRequest: PendingConnectionRequest? = getPendingConnectionRequest(request)
        mPendingConnectionRequests.remove(pendingRequest)
        // Immediately complete the CompletableDeferred and throw a CallException out to the client
        // to indicate this addCall request failed!
        completeAddCallRequestWithException(pendingRequest)
    }

    /** Helper methods */
    internal fun createSelfManagedConnection(
        request: ConnectionRequest,
        direction: Int
    ): Connection? {
        val targetRequest: PendingConnectionRequest =
            getPendingConnectionRequest(request) ?: return null

        val jetpackConnection =
            CallSessionLegacy(
                ParcelUuid.fromString(UUID.randomUUID().toString()),
                targetRequest.callAttributes,
                targetRequest.callChannel,
                targetRequest.coroutineContext,
                targetRequest.onAnswer,
                targetRequest.onDisconnect,
                targetRequest.onSetActive,
                targetRequest.onSetInactive,
                targetRequest.onEvent,
                targetRequest.preferredStartingCallEndpoint,
                targetRequest.execution
            )

        // set display name
        jetpackConnection.setCallerDisplayName(
            targetRequest.callAttributes.displayName.toString(),
            TelecomManager.PRESENTATION_ALLOWED
        )

        // set address
        jetpackConnection.setAddress(
            targetRequest.callAttributes.address,
            TelecomManager.PRESENTATION_ALLOWED
        )

        // set the extra EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED to true
        val extras = Bundle()
        extras.putBoolean(CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED, true)
        jetpackConnection.putExtras(extras)

        // set the call state for the given direction
        if (direction == CallAttributesCompat.DIRECTION_OUTGOING) {
            jetpackConnection.setDialing()
        } else {
            jetpackConnection.setRinging()
        }

        // set the callType
        if (targetRequest.callAttributes.callType == CallAttributesCompat.CALL_TYPE_VIDEO_CALL) {
            jetpackConnection.setVideoState(VideoProfile.STATE_BIDIRECTIONAL)
        } else {
            jetpackConnection.setVideoState(VideoProfile.STATE_AUDIO_ONLY)
        }

        // set the call capabilities
        // allow video state changes so there are no issues on the platform side. The platform only
        // tracks video state changes and should not deny any video state changes.
        jetpackConnection.setConnectionCapabilities(
            Connection.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL or
                Connection.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or
                Connection.CAPABILITY_CAN_PAUSE_VIDEO
        )

        if (targetRequest.callAttributes.hasSupportsSetInactiveCapability()) {
            jetpackConnection.setConnectionCapabilities(
                jetpackConnection.connectionCapabilities or
                    Connection.CAPABILITY_HOLD or
                    Connection.CAPABILITY_SUPPORT_HOLD
            )
        }

        // Explicitly set voip audio mode on connection side
        jetpackConnection.audioModeIsVoip = true

        targetRequest.completableDeferred?.complete(
            AddCallResult.SuccessCallSessionLegacy(jetpackConnection)
        )

        mPendingConnectionRequests.remove(targetRequest)
        return jetpackConnection
    }

    private fun getPendingConnectionRequest(request: ConnectionRequest): PendingConnectionRequest? {
        if (request.extras == null) {
            Log.w(TAG, "no extras bundle found in the request")
            return null
        }
        val targetId = getPlatformConnectionRequestId(request.extras)
        if (targetId.equals(KEY_NOT_FOUND)) {
            return getFirstPendingRequestFromApp(request) // return the first pending request
            // as it is likely the application is not making multiple calls in parallel
        }
        for (pendingConnectionRequest in mPendingConnectionRequests) {
            Log.i(TAG, "targId=$targetId, currId=${pendingConnectionRequest.requestIdMatcher}")
            if (pendingConnectionRequest.requestIdMatcher.equals(targetId)) {
                return pendingConnectionRequest
            }
        }
        Log.w(TAG, "request did not match any pending request elements")
        return getFirstPendingRequestFromApp(request) // return the first pending request
        // as it is likely the application is not making multiple calls in parallel
    }

    private fun getPlatformConnectionRequestId(extras: Bundle): String {
        if (extras.containsKey(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)) {
            val incomingCallExtras = extras.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
            if (incomingCallExtras == null) {
                Log.w(TAG, "request did not match any pending request elements")
                return KEY_NOT_FOUND
            }
            return incomingCallExtras.getString(REQUEST_ID_MATCHER_KEY, KEY_NOT_FOUND)
        } else {
            return extras.getString(REQUEST_ID_MATCHER_KEY, KEY_NOT_FOUND)
        }
    }

    private fun getFirstPendingRequestFromApp(
        request: ConnectionRequest
    ): PendingConnectionRequest? {
        for (pendingConnectionRequest in mPendingConnectionRequests) {
            if (request.accountHandle.equals(pendingConnectionRequest.callAttributes.mHandle)) {
                return pendingConnectionRequest
            }
        }
        return null
    }

    /** This method should be called when the platform failed to add the ConnectionRequest */
    private fun completeAddCallRequestWithException(pendingRequest: PendingConnectionRequest?) {
        pendingRequest
            ?.completableDeferred
            ?.complete(AddCallResult.Error(CallException.CODE_ERROR_UNKNOWN))
    }
}
