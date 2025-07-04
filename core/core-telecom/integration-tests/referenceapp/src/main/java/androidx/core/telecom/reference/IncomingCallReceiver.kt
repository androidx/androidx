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

package androidx.core.telecom.reference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_INCOMING
import androidx.core.telecom.reference.Constants.ACTION_NEW_INCOMING_CALL
import androidx.core.telecom.reference.Constants.EXTRA_IS_VIDEO
import androidx.core.telecom.reference.Constants.EXTRA_REMOTE_USER_NAME
import androidx.core.telecom.reference.Constants.EXTRA_SIMULATED_NUMBER
import androidx.core.telecom.reference.view.loadPhoneNumberPrefix

/**
 * Listens for broadcasts indicating a new incoming (simulated) call and attempts to display a
 * corresponding notification.
 *
 * Example broadcast to start a new incoming call:
 * ```
 * adb shell am broadcast -a androidx.core.telecom.reference.NEW_INCOMING_CALL
 *      --es simulated_number "123" --es name "John Smith" --ez isVideo true
 *      androidx.core.telecom.reference
 * ```
 */
class IncomingCallReceiver : BroadcastReceiver() {
    private lateinit var callNotificationManager: CallNotificationManager

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive with Action: ${intent.action}")
        if (ACTION_NEW_INCOMING_CALL != intent.action) {
            Log.w(TAG, "Received unexpected action: " + intent.action)
            return
        }
        handleIncomingCall(context, intent)
    }

    private fun handleIncomingCall(context: Context, intent: Intent) {
        val incomingNumber = intent.getStringExtra(EXTRA_SIMULATED_NUMBER)
        val remoteName = intent.getStringExtra(EXTRA_REMOTE_USER_NAME)
        val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        if (incomingNumber == null || remoteName == null) {
            Log.e(TAG, "Incoming call intent missing required extras (number or name)")
            return
        }

        val notificationId = getNextNotificationId()
        val attributes = getCallAttributes(context, incomingNumber, remoteName, isVideo)

        val callRepository = (context.applicationContext as? VoipApplication)?.callRepository
        // add the call to Core-Telecom in parallel to creating the call-style notification
        // this allows calling surfaces (e.g. Android Auto, Watch Faces) to answer the call
        callRepository?.maybeConnectService(context.applicationContext)
        callRepository?.onIncomingCallDetected(attributes, notificationId)

        callNotificationManager = CallNotificationManager(context)

        Log.d(TAG, "Generated notification ID [$notificationId] for incoming call.")
        postNotification(callNotificationManager, notificationId, attributes)
    }

    private fun postNotification(
        callNotificationManager: CallNotificationManager,
        notificationId: Int,
        attributes: CallAttributesCompat,
    ) {
        val n = callNotificationManager.buildIncomingCallNotification(notificationId, attributes)
        if (n != null) {
            Log.d(
                TAG,
                "Posting incoming call notification ID=[$notificationId]," +
                    " Number=[${attributes.address}], Name=[${attributes.displayName}]",
            )
            callNotificationManager.immediatelyPostNotification(notificationId, notification = n)
        } else {
            Log.e(TAG, "[$notificationId] Failed to build incoming call notification.")
        }
    }

    private fun getCallAttributes(
        c: Context,
        num: String?,
        name: String?,
        isVideo: Boolean,
    ): CallAttributesCompat {
        val address = Uri.parse(loadPhoneNumberPrefix(c) + num)
        return CallAttributesCompat(
            name.toString(),
            address,
            DIRECTION_INCOMING,
            callType =
                if (isVideo) CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
                else CallAttributesCompat.Companion.CALL_TYPE_AUDIO_CALL,
        )
    }
}
