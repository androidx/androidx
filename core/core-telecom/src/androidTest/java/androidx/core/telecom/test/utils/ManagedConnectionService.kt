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

package androidx.core.telecom.test.utils

import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred

/**
 * NOTE: This class requires a DUT that has Build.VERSION_CODES.Q or higher in oder to create
 * connections. This is due to the user of [androidx.test.platform.app.InstrumentationRegistry]
 * dependency.
 *
 * [ManagedConnectionService] is a ConnectionService that simulates managed/sim calls. Managed calls
 * have to be simulated because the devices being used for testing are not guaranteed to have sims.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ManagedConnectionService : ConnectionService() {
    val TAG = ManagedConnectionService::class.simpleName;
    data class PendingConnectionRequest(
        val callAttributes: CallAttributesCompat,
        val completableDeferred: CompletableDeferred<ManagedConnection>?
    )

    companion object {
        var mPendingConnectionRequests: ArrayList<PendingConnectionRequest> = ArrayList()
    }

    fun createConnectionRequest(
        telecomManager: TelecomManager,
        phoneAccountHandle: PhoneAccountHandle,
        pendingConnectionRequest: PendingConnectionRequest,
    ) {
        Log.i(TAG, "createConnectionRequest: request=[$pendingConnectionRequest]," +
            " handle=[$phoneAccountHandle]")
        pendingConnectionRequest.callAttributes.mHandle = phoneAccountHandle

        // add request to list
        mPendingConnectionRequests.add(pendingConnectionRequest)

        val extras = Utils.getBundleWithPhoneAccountHandle(
            pendingConnectionRequest.callAttributes,
            pendingConnectionRequest.callAttributes.mHandle!!
        )

        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.adoptShellPermissionIdentity("android.permission.CALL_PHONE")

        // Call into the platform to start call
        if (pendingConnectionRequest.callAttributes.isOutgoingCall()) {
            telecomManager.placeCall(
                pendingConnectionRequest.callAttributes.address, extras
            )
        } else {
            telecomManager.addNewIncomingCall(
                pendingConnectionRequest.callAttributes.mHandle, extras
            )
        }
        uiAutomation.dropShellPermissionIdentity()
    }

    /**
     *  Outgoing Connections
     */
    override fun onCreateOutgoingConnection(
        connectionManagerAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection? {
        return createSelfManagedConnection(
            request, CallAttributesCompat.DIRECTION_OUTGOING
        )
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        Log.i(TAG, "onCreateOutgoingConnectionFailed: request=[$request]")
        val pendingRequest: PendingConnectionRequest? = findTargetPendingConnectionRequest(
            request, CallAttributesCompat.DIRECTION_OUTGOING
        )
        pendingRequest?.completableDeferred?.cancel()

        mPendingConnectionRequests.remove(pendingRequest)
    }

    /**
     *  Incoming Connections
     */
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection? {
        return createSelfManagedConnection(
            request, CallAttributesCompat.DIRECTION_INCOMING
        )
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        Log.i(TAG, "onCreateIncomingConnectionFailed: request=[$request]")
        val pendingRequest: PendingConnectionRequest? = findTargetPendingConnectionRequest(
            request, CallAttributesCompat.DIRECTION_INCOMING
        )
        pendingRequest?.completableDeferred?.cancel()
        mPendingConnectionRequests.remove(pendingRequest)
    }

    internal fun createSelfManagedConnection(
        request: ConnectionRequest,
        direction: Int
    ): Connection? {
        Log.i(TAG, "createSelfManagedConnection: request=[$request], direction=[$direction]")
        val targetRequest: PendingConnectionRequest =
            findTargetPendingConnectionRequest(request, direction) ?: return null

        val jetpackConnection = ManagedConnection()

        // set display name
        jetpackConnection.setCallerDisplayName(
            targetRequest.callAttributes.displayName.toString(), TelecomManager.PRESENTATION_ALLOWED
        )

        // set address
        jetpackConnection.setAddress(
            targetRequest.callAttributes.address, TelecomManager.PRESENTATION_ALLOWED
        )

        // set the call state for the given direction
        if (direction == CallAttributesCompat.DIRECTION_OUTGOING) {
            jetpackConnection.setDialing()
        } else {
            jetpackConnection.setRinging()
        }

        // set the callType
        if (targetRequest.callAttributes.callType == CallAttributesCompat.CALL_TYPE_VIDEO_CALL) {
            jetpackConnection.videoState = VideoProfile.STATE_BIDIRECTIONAL
        } else {
            jetpackConnection.videoState = VideoProfile.STATE_AUDIO_ONLY
        }

        // set the call capabilities
        if (targetRequest.callAttributes.hasSupportsSetInactiveCapability()) {
            jetpackConnection.connectionCapabilities =
                Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD
        }
        Log.i(TAG, "createSelfManagedConnection: targetRequest=[$targetRequest]")
        targetRequest.completableDeferred?.complete(jetpackConnection)
        mPendingConnectionRequests.remove(targetRequest)

        return jetpackConnection
    }

    /**
     *  Helper methods
     */
    private fun findTargetPendingConnectionRequest(
        request: ConnectionRequest,
        direction: Int
    ): PendingConnectionRequest? {
        for (pendingConnectionRequest in mPendingConnectionRequests) {
            if (isSameAddress(pendingConnectionRequest.callAttributes, request) && isSameDirection(
                    pendingConnectionRequest.callAttributes, direction
                ) && isSameHandle(pendingConnectionRequest.callAttributes.mHandle, request)
            ) {
                return pendingConnectionRequest
            }
        }
        return null
    }

    private fun isSameDirection(callAttributes: CallAttributesCompat, direction: Int): Boolean {
        return (callAttributes.direction == direction)
    }

    private fun isSameAddress(
        callAttributes: CallAttributesCompat,
        request: ConnectionRequest
    ): Boolean {
        return request.address?.equals(callAttributes.address) ?: false
    }

    private fun isSameHandle(handle: PhoneAccountHandle?, request: ConnectionRequest): Boolean {
        return request.accountHandle?.equals(handle) ?: false
    }
}
