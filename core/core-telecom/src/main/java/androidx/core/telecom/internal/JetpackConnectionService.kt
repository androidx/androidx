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
import android.os.ParcelUuid
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.utils.Utils
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(api = Build.VERSION_CODES.O)
internal class JetpackConnectionService : ConnectionService() {
    val TAG: String = JetpackConnectionService::class.java.simpleName.toString()

    /**
     * Wrap all the objects that are associated with a new CallSession request into a class
     */
    data class PendingConnectionRequest(
        val callAttributes: CallAttributesCompat,
        val callChannel: CallChannels,
        val coroutineContext: CoroutineContext,
        val completableDeferred: CompletableDeferred<CallSessionLegacy>
    )

    companion object {
        const val CONNECTION_CREATION_TIMEOUT: Long = 5000 // time in milli-seconds
        var mPendingConnectionRequests: ArrayList<PendingConnectionRequest> = ArrayList()
    }

    /**
     * Request the Platform create a new Connection with the properties given by [CallAttributesCompat].
     * This request will have a timeout of [CONNECTION_CREATION_TIMEOUT] and be removed when the
     * result is completed.
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    fun createConnectionRequest(
        telecomManager: TelecomManager,
        pendingConnectionRequest: PendingConnectionRequest,
    ) {
        // add request to list
        mPendingConnectionRequests.add(pendingConnectionRequest)

        val extras = Utils.getBundleWithPhoneAccountHandle(
            pendingConnectionRequest.callAttributes,
            pendingConnectionRequest.callAttributes.mHandle!!
        )

        // Call into the platform to start call
        if (pendingConnectionRequest.callAttributes.isOutgoingCall()) {
            telecomManager.placeCall(
                pendingConnectionRequest.callAttributes.address,
                extras
            )
        } else {
            telecomManager.addNewIncomingCall(
                pendingConnectionRequest.callAttributes.mHandle,
                extras
            )
        }

        // create a job that times out if the connection cannot be created in x amount of time
        CoroutineScope(pendingConnectionRequest.coroutineContext).launch {
            delay(CONNECTION_CREATION_TIMEOUT)
            if (!pendingConnectionRequest.completableDeferred.isCompleted) {
                Log.i(
                    TAG, "The request to create a connection timed out. Cancelling the" +
                        "request to add the call to Telecom."
                )
                mPendingConnectionRequests.remove(pendingConnectionRequest)
                pendingConnectionRequest.completableDeferred.cancel(
                    CancellationException(CallsManager.CALL_CREATION_FAILURE_MSG)
                )
            }
        }
    }

    /**
     *  Outgoing Connections
     */
    override fun onCreateOutgoingConnection(
        connectionManagerAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection? {
        return createSelfManagedConnection(
            request,
            CallAttributesCompat.DIRECTION_OUTGOING
        )
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        val pendingRequest: PendingConnectionRequest? =
            findTargetPendingConnectionRequest(
                request,
                CallAttributesCompat.DIRECTION_OUTGOING
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
            request,
            CallAttributesCompat.DIRECTION_INCOMING
        )
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        val pendingRequest: PendingConnectionRequest? =
            findTargetPendingConnectionRequest(
                request,
                CallAttributesCompat.DIRECTION_INCOMING
            )
        pendingRequest?.completableDeferred?.cancel()
        mPendingConnectionRequests.remove(pendingRequest)
    }

    private fun createSelfManagedConnection(request: ConnectionRequest, direction: Int):
        Connection? {
        var jetpackConnection: CallSessionLegacy? = null
        val targetRequest: PendingConnectionRequest? =
            findTargetPendingConnectionRequest(request, direction)

        if (targetRequest != null) {
            jetpackConnection = CallSessionLegacy(
                ParcelUuid.fromString(UUID.randomUUID().toString()),
                targetRequest.callChannel,
                targetRequest.coroutineContext
            )
            targetRequest.completableDeferred.complete(jetpackConnection)
            mPendingConnectionRequests.remove(targetRequest)
        }

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
            if (isSameAddress(pendingConnectionRequest.callAttributes, request) &&
                isSameDirection(pendingConnectionRequest.callAttributes, direction) &&
                isSameHandle(pendingConnectionRequest.callAttributes.mHandle, request)
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
