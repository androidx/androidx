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

package androidx.core.telecom.test

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.extensions.LocalCallSilenceExtension
import androidx.core.telecom.test.NotificationsUtilities.Companion.NOTIFICATION_CHANNEL_ID
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@ExperimentalAppActions
class VoipCall(
    val context: Context,
    val attributes: CallAttributesCompat,
    val notificationId: Int
) {
    private val TAG = VoipCall::class.simpleName
    var mAdapter: CallListAdapter? = null
    var mCallControl: CallControlScope? = null
    var mParticipantControl: ParticipantControl? = null
    var mCurrentState: String = "?"
    var mCurrentEndpoint: CallEndpointCompat? = null
    var mAvailableEndpoints: List<CallEndpointCompat>? = ArrayList()
    var mIsMuted = false
    var mTelecomCallId: String = ""
    var mIsLocallySilence: Boolean = false
    var hasUpdatedToOngoing = false
    var mLocalCallSilenceExtension: LocalCallSilenceExtension? = null
    var mCallJob: Job? = null

    // Call Icon data
    data class IconControl(val onUriChanged: suspend (Uri) -> Unit)

    var mIconExtensionControl: IconControl? = null
    private val _mIconUri = MutableStateFlow<Uri>(Uri.EMPTY)
    val mIconUri = _mIconUri.asStateFlow()
    var mIconBitmap: Bitmap

    init {
        mIconBitmap = CallIconGenerator.generateNextBitmap()
        setIconUri(VoipAppFileProvider.writeCallIconBitMapToFile(context, this)!!)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val mOnSetActiveLambda: suspend () -> Unit = {
        Log.i(TAG, "onSetActive: completing")
        onCallStateChanged("Active")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val mOnSetInActiveLambda: suspend () -> Unit = {
        Log.i(TAG, "onSetInactive: completing")
        onCallStateChanged("Inactive")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val mOnAnswerLambda: suspend (type: Int) -> Unit = {
        Log.i(TAG, "onAnswer: callType=[$it]")
        onCallStateChanged("Answered")
        updateNotificationToOngoing()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val mOnDisconnectLambda: suspend (cause: DisconnectCause) -> Unit = {
        Log.i(TAG, "onDisconnect: disconnectCause=[$it]")
        NotificationsUtilities.clearNotification(context, notificationId)
        onCallStateChanged("Disconnected")
        mCallJob?.cancel("call disconnected")
    }

    fun getIconFileName(): String {
        return "VoipCallIcon_${notificationId}"
    }

    fun getIconBitmap(): Bitmap {
        return mIconBitmap
    }

    fun setIconBitmap(bitmap: Bitmap) {
        mIconBitmap = bitmap
    }

    fun setIconUri(uri: Uri) {
        _mIconUri.value = uri
    }

    fun getIconUri(): Uri? {
        return mIconUri.value
    }

    fun setCallControl(callControl: CallControlScope) {
        mCallControl = callControl
    }

    fun setParticipantControl(participantControl: ParticipantControl) {
        mParticipantControl = participantControl
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun clearNotification() {
        Log.i(TAG, "clearNotification with id=[$notificationId]")
        NotificationsUtilities.clearNotification(context, notificationId)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun updateNotificationToOngoing() {
        if (!hasUpdatedToOngoing) {
            NotificationsUtilities.updateNotificationToOngoing(
                context = context,
                notificationId = notificationId,
                channelId = NOTIFICATION_CHANNEL_ID,
                callerName = attributes.displayName.toString()
            )
        }
        hasUpdatedToOngoing = true
    }

    fun setJob(job: Job) {
        mCallJob = job
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalAppActions::class)
    suspend fun toggleLocalCallSilence() {
        CoroutineScope(coroutineContext).launch {
            // toggle the value for the call
            mIsLocallySilence = !mIsLocallySilence

            mAdapter?.updateLocalCallSilenceIcon(mTelecomCallId, mIsLocallySilence)
            // send update to the ICS
            mLocalCallSilenceExtension?.updateIsLocallySilenced(mIsLocallySilence)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun onLocalCallSilenceUpdate(isSilenced: Boolean) {
        // change the value for the app to match the ics
        mIsLocallySilence = isSilenced
        CoroutineScope(coroutineContext).launch {
            mAdapter?.updateLocalCallSilenceIcon(mTelecomCallId, isSilenced)
        }
    }

    suspend fun disconnect(cause: DisconnectCause) {
        mCallControl?.disconnect(cause)
        mCallJob?.cancel("call disconnected")
    }

    fun setCallAdapter(adapter: CallListAdapter?) {
        mAdapter = adapter
    }

    fun setCallId(callId: String) {
        mTelecomCallId = callId
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onParticipantsChanged(participants: List<ParticipantState>) {
        mAdapter?.updateParticipants(mTelecomCallId, participants)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onCallStateChanged(callState: String) {
        Log.i(TAG, "onCallStateChanged: state=$callState")
        mCurrentState = callState
        mAdapter?.updateCallState(mTelecomCallId, callState)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onCallEndpointChanged(endpoint: CallEndpointCompat) {
        Log.i(TAG, "onCallEndpointChanged: endpoint=$endpoint")
        mCurrentEndpoint = endpoint
        mAdapter?.updateEndpoint(mTelecomCallId, endpoint.name.toString())
    }

    fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpointCompat>) {
        Log.i(TAG, "onAvailableCallEndpointsChanged:")
        for (endpoint in endpoints) {
            Log.i(TAG, "onAvailableCallEndpointsChanged: --> endpoint=$endpoint")
        }
        mAvailableEndpoints = endpoints
    }

    fun onMuteStateChanged(isMuted: Boolean) {
        Log.i(TAG, "onMuteStateChanged: isMuted=$isMuted")
        mIsMuted = isMuted
    }

    fun getEndpointType(type: Int): CallEndpointCompat? {
        for (endpoint in mAvailableEndpoints!!) {
            if (endpoint.type == type) {
                return endpoint
            }
        }
        return null
    }
}
