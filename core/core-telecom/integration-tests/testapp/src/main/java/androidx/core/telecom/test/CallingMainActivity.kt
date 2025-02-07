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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.DisconnectCause
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_INCOMING
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_OUTGOING
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.RaiseHandState
import androidx.core.telecom.test.Constants.Companion.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.Constants.Companion.INCOMING_NAME
import androidx.core.telecom.test.Constants.Companion.INCOMING_URI
import androidx.core.telecom.test.Constants.Companion.OUTGOING_NAME
import androidx.core.telecom.test.Constants.Companion.OUTGOING_URI
import androidx.core.telecom.test.NotificationsUtilities.Companion.IS_ANSWER_ACTION
import androidx.core.telecom.test.NotificationsUtilities.Companion.NOTIFICATION_CHANNEL_ID
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@ExperimentalAppActions
@RequiresApi(34)
class CallingMainActivity : Activity() {
    // Activity
    private val TAG = CallingMainActivity::class.simpleName
    private val mScope = CoroutineScope(Dispatchers.Default)
    private lateinit var mContext: Context
    private var mCurrentCallCount: Int = 0
    // Telecom
    private lateinit var mCallsManager: CallsManager
    // Ongoing Call List
    private var mRecyclerView: RecyclerView? = null
    private var mCallObjects: ArrayList<CallRow> = ArrayList()
    private lateinit var mAdapter: CallListAdapter
    // Pre-Call Endpoint List
    private var mPreCallEndpointsRecyclerView: RecyclerView? = null
    private var mCurrentPreCallEndpoints: ArrayList<CallEndpointCompat> = arrayListOf()
    private lateinit var mPreCallEndpointAdapter: PreCallEndpointsAdapter
    // Notification
    private var mNextNotificationId: Int = 1
    private lateinit var mNotificationManager: NotificationManager
    private val mNotificationActionInfoFlow: MutableStateFlow<NotificationActionInfo> =
        MutableStateFlow(NotificationActionInfo(-1, false))

    /**
     * NotificationActionInfo couples information propagated from the Call-Style notification on
     * which action button was clicked (e.g. answer the call or decline ) *
     */
    data class NotificationActionInfo(val id: Int, val isAnswer: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = applicationContext
        initNotifications(mContext)
        mCallsManager = CallsManager(this)

        val raiseHandCheckBox = findViewById<CheckBox>(R.id.RaiseHandCheckbox)
        val kickParticipantCheckBox = findViewById<CheckBox>(R.id.KickPartCheckbox)
        val participantCheckBox = findViewById<CheckBox>(R.id.ParticipantsCheckbox)

        participantCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                raiseHandCheckBox.isEnabled = false
                raiseHandCheckBox.isChecked = false
                kickParticipantCheckBox.isEnabled = false
                kickParticipantCheckBox.isChecked = false
            } else {
                raiseHandCheckBox.isEnabled = true
                kickParticipantCheckBox.isEnabled = true
            }
        }

        val registerPhoneAccountButton = findViewById<Button>(R.id.registerButton)
        registerPhoneAccountButton.setOnClickListener { mScope.launch { registerPhoneAccount() } }

        val fetchPreCallEndpointsButton = findViewById<Button>(R.id.preCallAudioEndpointsButton)
        fetchPreCallEndpointsButton.setOnClickListener {
            mScope.launch { fetchPreCallEndpoints(findViewById(R.id.cancelFlowButton)) }
        }

        val addOutgoingCallButton = findViewById<Button>(R.id.addOutgoingCall)
        addOutgoingCallButton.setOnClickListener {
            addCallWithAttributes(
                CallAttributesCompat(
                    OUTGOING_NAME,
                    OUTGOING_URI,
                    DIRECTION_OUTGOING,
                    CALL_TYPE_VIDEO_CALL,
                    ALL_CALL_CAPABILITIES,
                    mPreCallEndpointAdapter.mSelectedCallEndpoint
                ),
                participantCheckBox.isChecked,
                raiseHandCheckBox.isChecked,
                kickParticipantCheckBox.isChecked
            )
        }

        val addIncomingCallButton = findViewById<Button>(R.id.addIncomingCall)
        addIncomingCallButton.setOnClickListener {
            addCallWithAttributes(
                CallAttributesCompat(
                    INCOMING_NAME,
                    INCOMING_URI,
                    DIRECTION_INCOMING,
                    CALL_TYPE_VIDEO_CALL,
                    ALL_CALL_CAPABILITIES,
                    mPreCallEndpointAdapter.mSelectedCallEndpoint
                ),
                participantCheckBox.isChecked,
                raiseHandCheckBox.isChecked,
                kickParticipantCheckBox.isChecked
            )
        }

        // setup the adapters which hold the endpoint and call rows
        mAdapter = CallListAdapter(mCallObjects, null, applicationContext)
        mPreCallEndpointAdapter = PreCallEndpointsAdapter(mCurrentPreCallEndpoints)

        // set up the view holders
        mRecyclerView = findViewById(R.id.callListRecyclerView)
        mRecyclerView?.layoutManager = LinearLayoutManager(this)
        mRecyclerView?.adapter = mAdapter
        mPreCallEndpointsRecyclerView = findViewById(R.id.endpointsRecyclerView)
        mPreCallEndpointsRecyclerView?.layoutManager = LinearLayoutManager(this)
        mPreCallEndpointsRecyclerView?.adapter = mPreCallEndpointAdapter
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent: intent=[$intent]")
        maybeHandleNotificationAction(intent)
    }

    private fun maybeHandleNotificationAction(intent: Intent?) {
        if (intent != null) {
            val id = intent.getIntExtra(NotificationsUtilities.NOTIFICATION_ID, -1)
            if (id != -1) {
                val isAnswer = intent.getBooleanExtra(IS_ANSWER_ACTION, false)
                Log.i(TAG, "handleNotification: id=$id, isAnswer=$isAnswer")
                mNotificationActionInfoFlow.value = NotificationActionInfo(id, isAnswer)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (call in mCallObjects) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    call.callObject.mCallControl?.disconnect(DisconnectCause(DisconnectCause.LOCAL))
                } catch (e: Exception) {
                    Log.i(TAG, "onDestroy: exception hit trying to destroy")
                }
            }
        }
        NotificationsUtilities.deleteNotificationChannel(mContext)
    }

    private fun initNotifications(c: Context) {
        NotificationsUtilities.initNotificationChannel(c)
        mNotificationManager = c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @SuppressLint("WrongConstant")
    private fun registerPhoneAccount() {
        var capabilities: @CallsManager.Companion.Capability Int = CallsManager.CAPABILITY_BASELINE

        val videoCallingCheckBox = findViewById<CheckBox>(R.id.VideoCallingCheckBox)
        if (videoCallingCheckBox.isChecked) {
            capabilities = capabilities or CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
        }
        val streamingCheckBox = findViewById<CheckBox>(R.id.streamingCheckBox)
        if (streamingCheckBox.isChecked) {
            capabilities = capabilities or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING
        }
        mCallsManager.registerAppWithTelecom(capabilities)
    }

    private fun addCallWithAttributes(
        attributes: CallAttributesCompat,
        isParticipantsEnabled: Boolean,
        isRaiseHandEnabled: Boolean,
        isKickParticipantEnabled: Boolean
    ) {
        Log.i(TAG, "addCallWithAttributes: attributes=$attributes")
        val callObject = VoipCall(this, attributes, mNextNotificationId++)

        try {
            val handler = CoroutineExceptionHandler { _, exception ->
                Log.i(TAG, "CoroutineExceptionHandler: handling e=$exception")
                NotificationsUtilities.clearNotification(mContext, callObject.notificationId)
            }
            val job =
                mScope.launch(handler) {
                    try {
                        if (isParticipantsEnabled) {
                            addCallWithExtensions(
                                attributes,
                                callObject,
                                isRaiseHandEnabled,
                                isKickParticipantEnabled
                            )
                        } else {
                            addCall(attributes, callObject)
                        }
                    } finally {
                        NotificationsUtilities.clearNotification(
                            mContext,
                            callObject.notificationId
                        )
                        Log.i(TAG, "addCallWithAttributes: finally block")
                    }
                }
            callObject.setJob(job)
        } catch (e: Exception) {
            logException(e, "addCallWithAttributes: catch outer")
            NotificationsUtilities.clearNotification(mContext, callObject.notificationId)
        }
    }

    private suspend fun addCall(attributes: CallAttributesCompat, callObject: VoipCall) {
        mCallsManager.addCall(
            attributes,
            callObject.mOnAnswerLambda,
            callObject.mOnDisconnectLambda,
            callObject.mOnSetActiveLambda,
            callObject.mOnSetInActiveLambda,
        ) {
            postNotification(attributes, callObject)
            mPreCallEndpointAdapter.mSelectedCallEndpoint = null
            // inject client control interface into the VoIP call object
            callObject.onCallStateChanged(
                when (attributes.direction) {
                    DIRECTION_OUTGOING -> "Outgoing"
                    DIRECTION_INCOMING -> "Incoming"
                    else -> "?"
                }
            )
            callObject.setCallId(getCallId().toString())
            callObject.setCallControl(this)

            launch {
                mNotificationActionInfoFlow.collect {
                    if (it.id == callObject.notificationId) {
                        if (it.isAnswer) {
                            answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
                        } else {
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        }
                        handleUpdateToNotification(it, attributes, callObject)
                    }
                }
            }
            // Collect updates
            launch { currentCallEndpoint.collect { callObject.onCallEndpointChanged(it) } }

            launch { availableEndpoints.collect { callObject.onAvailableCallEndpointsChanged(it) } }

            launch { isMuted.collect { callObject.onMuteStateChanged(it) } }
            addCallRow(callObject)
        }
    }

    private fun handleUpdateToNotification(
        it: NotificationActionInfo,
        attributes: CallAttributesCompat,
        callObject: VoipCall
    ) {
        if (it.isAnswer) {
            NotificationsUtilities.updateNotificationToOngoing(
                mContext,
                callObject.notificationId,
                NOTIFICATION_CHANNEL_ID,
                attributes.displayName.toString()
            )
        } else {
            NotificationsUtilities.clearNotification(mContext, callObject.notificationId)
        }
    }

    @OptIn(ExperimentalAppActions::class)
    private suspend fun addCallWithExtensions(
        attributes: CallAttributesCompat,
        callObject: VoipCall,
        isRaiseHandEnabled: Boolean = false,
        isKickParticipantEnabled: Boolean = false
    ) {
        mCallsManager.addCallWithExtensions(
            attributes,
            callObject.mOnAnswerLambda,
            callObject.mOnDisconnectLambda,
            callObject.mOnSetActiveLambda,
            callObject.mOnSetInActiveLambda,
        ) {
            val initLocalCallSilenceValue = false
            callObject.onLocalCallSilenceUpdate(initLocalCallSilenceValue)
            val lcsE =
                addLocalCallSilenceExtension(initLocalCallSilenceValue) {
                    callObject.onLocalCallSilenceUpdate(it)
                }
            callObject.mLocalCallSilenceExtension = lcsE

            val iconExtension = addCallIconExtension(callObject.getIconUri()!!)

            val participants = ParticipantsExtensionManager()
            val participantExtension =
                addParticipantExtension(
                    initialParticipants =
                        participants.participants.value.map { it.toParticipant() }.toSet()
                )
            var raiseHandState: RaiseHandState? = null
            if (isRaiseHandEnabled) {
                raiseHandState =
                    participantExtension.addRaiseHandSupport {
                        participants.onRaisedHandStateChanged(it)
                    }
            }
            if (isKickParticipantEnabled) {
                participantExtension.addKickParticipantSupport {
                    participants.onKickParticipant(it)
                }
            }
            onCall {
                postNotification(attributes, callObject)
                mPreCallEndpointAdapter.mSelectedCallEndpoint = null
                // inject client control interface into the VoIP call object
                callObject.onCallStateChanged(
                    when (attributes.direction) {
                        DIRECTION_OUTGOING -> "Outgoing"
                        DIRECTION_INCOMING -> "Incoming"
                        else -> "?"
                    }
                )
                callObject.setCallId(getCallId().toString())
                callObject.setCallControl(this)
                callObject.setParticipantControl(
                    ParticipantControl(
                        onParticipantAdded = participants::addParticipant,
                        onParticipantRemoved = participants::removeParticipant
                    )
                )

                callObject.mIconExtensionControl =
                    VoipCall.IconControl(onUriChanged = iconExtension::updateCallIconUri)

                addCallRow(callObject)
                launch {
                    mNotificationActionInfoFlow.collect {
                        if (it.id == callObject.notificationId) {
                            if (it.isAnswer) {
                                answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
                            } else {
                                disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            }
                            handleUpdateToNotification(it, attributes, callObject)
                        }
                    }
                }
                // Collect updates
                participants.participants
                    .onEach {
                        participantExtension.updateParticipants(
                            it.map { p -> p.toParticipant() }.toSet()
                        )
                        participantExtension.updateActiveParticipant(
                            it.firstOrNull { p -> p.isActive }?.toParticipant()
                        )
                        raiseHandState?.updateRaisedHands(
                            it.filter { p -> p.isHandRaised }.map { p -> p.toParticipant() }
                        )
                        callObject.onParticipantsChanged(it)
                    }
                    .launchIn(this)

                launch {
                    while (isActive) {
                        delay(1000)
                        participants.changeParticipantStates()
                    }
                }

                launch { currentCallEndpoint.collect { callObject.onCallEndpointChanged(it) } }

                launch {
                    availableEndpoints.collect { callObject.onAvailableCallEndpointsChanged(it) }
                }

                launch { isMuted.collect { callObject.onMuteStateChanged(it) } }
            }
        }
    }

    private fun fetchPreCallEndpoints(cancelFlowButton: Button) {
        val endpointsFlow = mCallsManager.getAvailableStartingCallEndpoints()
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                val endpointsCoroutineScope = this
                Log.i(TAG, "fetchEndpoints: consuming endpoints")
                endpointsFlow.collect {
                    for (endpoint in it) {
                        Log.i(TAG, "fetchEndpoints: endpoint=[$endpoint}")
                    }
                    cancelFlowButton.setOnClickListener {
                        mPreCallEndpointAdapter.mSelectedCallEndpoint = null
                        endpointsCoroutineScope.cancel()
                        updatePreCallEndpoints(null)
                    }
                    updatePreCallEndpoints(it)
                }
                // At this point, the endpointsCoroutineScope has been canceled
                updatePreCallEndpoints(null)
            }
        }
    }

    private fun postNotification(attributes: CallAttributesCompat, voipCall: VoipCall) {
        val notification =
            NotificationsUtilities.createInitialCallStyleNotification(
                mContext,
                voipCall.notificationId,
                NOTIFICATION_CHANNEL_ID,
                attributes.displayName.toString(),
                attributes.direction == DIRECTION_OUTGOING
            )
        mNotificationManager.notify(voipCall.notificationId, notification)
    }

    private fun logException(e: Exception, prefix: String) {
        Log.i(TAG, "$prefix: e=[$e], e.msg=[${e.message}], e.stack:${e.printStackTrace()}")
    }

    private fun addCallRow(callObject: VoipCall) {
        mCallObjects.add(CallRow(++mCurrentCallCount, callObject))
        callObject.setCallAdapter(mAdapter)
        updateCallList()
    }

    private fun updateCallList() {
        runOnUiThread { mAdapter.notifyDataSetChanged() }
    }

    private fun updatePreCallEndpoints(newEndpoints: List<CallEndpointCompat>?) {
        runOnUiThread {
            mCurrentPreCallEndpoints.clear()
            if (newEndpoints != null) {
                mCurrentPreCallEndpoints.addAll(newEndpoints)
            }
            mPreCallEndpointAdapter.notifyDataSetChanged()
        }
    }
}
