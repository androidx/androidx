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

package androidx.core.telecom.test.utils

import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.internal.utils.Utils
import java.util.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class VoipConnectionService : ConnectionService() {
    data class VoipPendingConnectionRequest(
        val callAttributes: CallAttributesCompat,
        val completableDeferred: CompletableDeferred<AutoVoipConnection>?,
    )

    companion object {
        const val TAG = "VoipConnectionService"
        const val PHONE_ACCOUNT_HANDLE_ID = "AA_VOIP_TEST_APP_PHONE_ACCOUNT_HANDLE"
        const val PACKAGE_NAME = "androidx.core.telecom.test"
        val DEFAULT_ADDRESS: Uri = Uri.fromParts("tel", "1234567890", null)
        val SINGLETON_PHONE_ACCOUNT_HANDLE =
            PhoneAccountHandle(
                ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.utils.VoipConnectionService"),
                PHONE_ACCOUNT_HANDLE_ID,
            )

        var mPendingConnectionRequests: ArrayList<VoipPendingConnectionRequest> = ArrayList()
    }

    fun createConnectionRequest(
        telecomManager: TelecomManager,
        phoneAccountHandle: PhoneAccountHandle,
        pendingConnectionRequest: VoipPendingConnectionRequest,
    ) {
        Log.i(
            TAG,
            "createConnectionRequest: request=[$pendingConnectionRequest]," +
                " handle=[$phoneAccountHandle]",
        )
        pendingConnectionRequest.callAttributes.mHandle = phoneAccountHandle

        // add request to list
        mPendingConnectionRequests.add(pendingConnectionRequest)

        val extras =
            Utils.getBundleWithPhoneAccountHandle(
                pendingConnectionRequest.callAttributes,
                pendingConnectionRequest.callAttributes.mHandle!!,
            )

        // Call into the platform to start call
        if (pendingConnectionRequest.callAttributes.isOutgoingCall()) {
            telecomManager.placeCall(pendingConnectionRequest.callAttributes.address, extras)
        } else {
            telecomManager.addNewIncomingCall(
                pendingConnectionRequest.callAttributes.mHandle,
                extras,
            )
        }
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ): Connection? {
        Log.i(TAG, "onCreateOutgoingConnection")
        val targetRequest: VoipPendingConnectionRequest =
            findTargetPendingConnectionRequest(request, CallAttributesCompat.DIRECTION_OUTGOING)
                ?: return null

        val ongoingConnection = AutoVoipConnection(applicationContext)

        val address = request.address ?: DEFAULT_ADDRESS
        ongoingConnection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)

        CoroutineScope(Dispatchers.Default).launch {
            delay(2500)
            startDialing(ongoingConnection)
        }

        targetRequest.completableDeferred?.complete(ongoingConnection)
        mPendingConnectionRequests.remove(targetRequest)

        return ongoingConnection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ) {
        Log.i(TAG, "onCreateOutgoingConnectionFailed")
        val pendingRequest: VoipPendingConnectionRequest? =
            findTargetPendingConnectionRequest(request, CallAttributesCompat.DIRECTION_OUTGOING)
        pendingRequest?.completableDeferred?.cancel()
        mPendingConnectionRequests.remove(pendingRequest)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ): Connection? {
        Log.i(TAG, "onCreateIncomingConnection")
        val ongoingConnection = AutoVoipConnection(applicationContext)
        val targetRequest: VoipPendingConnectionRequest =
            findTargetPendingConnectionRequest(request, CallAttributesCompat.DIRECTION_OUTGOING)
                ?: return null

        val address = request.address ?: DEFAULT_ADDRESS
        ongoingConnection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)

        ongoingConnection.setRinging()
        targetRequest.completableDeferred?.complete(ongoingConnection)
        mPendingConnectionRequests.remove(targetRequest)

        return ongoingConnection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ) {
        Log.i(TAG, "onCreateIncomingConnectionFailed")
        val pendingRequest: VoipPendingConnectionRequest? =
            findTargetPendingConnectionRequest(request, CallAttributesCompat.DIRECTION_INCOMING)
        pendingRequest?.completableDeferred?.cancel()
        mPendingConnectionRequests.remove(pendingRequest)
    }

    private fun startDialing(ongoingConnection: AutoVoipConnection) {
        if (ongoingConnection.state == Connection.STATE_NEW) {
            ongoingConnection.setDialing()
            ongoingConnection.updateSpeakerAndParticipants("", Random().nextInt(5))
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(3000)
            becomeActive(ongoingConnection)
        }
    }

    private fun becomeActive(ongoingConnection: AutoVoipConnection) {
        if (ongoingConnection.state == Connection.STATE_DIALING) {
            ongoingConnection.setActive()
            ongoingConnection.updateSpeakerAndParticipants("John", Random().nextInt(5))
        }
    }

    /** Helper methods */
    private fun findTargetPendingConnectionRequest(
        request: ConnectionRequest,
        direction: Int,
    ): VoipPendingConnectionRequest? {
        for (pendingConnectionRequest in mPendingConnectionRequests) {
            if (
                isSameAddress(pendingConnectionRequest.callAttributes, request) &&
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
        request: ConnectionRequest,
    ): Boolean {
        return request.address?.equals(callAttributes.address) == true
    }

    private fun isSameHandle(handle: PhoneAccountHandle?, request: ConnectionRequest): Boolean {
        return request.accountHandle?.equals(handle) == true
    }
}
