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
import androidx.core.telecom.test.Utilities.Companion.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.Utilities.Companion.INCOMING_NAME
import androidx.core.telecom.test.Utilities.Companion.INCOMING_URI
import androidx.core.telecom.test.Utilities.Companion.OUTGOING_NAME
import androidx.core.telecom.test.Utilities.Companion.OUTGOING_URI
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@RequiresApi(34)
class CallingMainActivity : Activity() {
    // Activity
    private val TAG = CallingMainActivity::class.simpleName
    private val mScope = CoroutineScope(Dispatchers.Default)
    private var mCallCount: Int = 0

    // Telecom
    private var mCallsManager: CallsManager? = null

    // Ongoing Call List
    private var mRecyclerView: RecyclerView? = null
    private var mCallObjects: ArrayList<CallRow> = ArrayList()
    private lateinit var mAdapter: CallListAdapter

    // Pre-Call Endpoint List
    private var mPreCallEndpointsRecyclerView: RecyclerView? = null
    private var mCurrentPreCallEndpoints: ArrayList<CallEndpointCompat> = arrayListOf()
    private lateinit var mPreCallEndpointAdapter: PreCallEndpointsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mCallsManager = CallsManager(this)
        mCallCount = 0

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
            mScope.launch {
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
        }

        val addIncomingCallButton = findViewById<Button>(R.id.addIncomingCall)
        addIncomingCallButton.setOnClickListener {
            mScope.launch {
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
        }

        // setup the adapters which hold the endpoint and call rows
        mAdapter = CallListAdapter(mCallObjects, null)
        mPreCallEndpointAdapter = PreCallEndpointsAdapter(mCurrentPreCallEndpoints)

        // set up the view holders
        mRecyclerView = findViewById(R.id.callListRecyclerView)
        mRecyclerView?.layoutManager = LinearLayoutManager(this)
        mRecyclerView?.adapter = mAdapter
        mPreCallEndpointsRecyclerView = findViewById(R.id.endpointsRecyclerView)
        mPreCallEndpointsRecyclerView?.layoutManager = LinearLayoutManager(this)
        mPreCallEndpointsRecyclerView?.adapter = mPreCallEndpointAdapter
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
        mCallsManager?.registerAppWithTelecom(capabilities)
    }

    private suspend fun addCallWithAttributes(
        attributes: CallAttributesCompat,
        isParticipantsEnabled: Boolean,
        isRaiseHandEnabled: Boolean,
        isKickParticipantEnabled: Boolean
    ) {
        Log.i(TAG, "addCallWithAttributes: attributes=$attributes")
        val callObject = VoipCall()

        try {
            val handler = CoroutineExceptionHandler { _, exception ->
                Log.i(TAG, "CoroutineExceptionHandler: handling e=$exception")
            }
            CoroutineScope(Dispatchers.Default).launch(handler) {
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
                } catch (e: Exception) {
                    logException(e, "addCallWithAttributes: catch inner")
                } finally {
                    Log.i(TAG, "addCallWithAttributes: finally block")
                }
            }
        } catch (e: Exception) {
            logException(e, "addCallWithAttributes: catch outer")
        }
    }

    private suspend fun addCall(attributes: CallAttributesCompat, callObject: VoipCall) {
        mCallsManager!!.addCall(
            attributes,
            callObject.mOnAnswerLambda,
            callObject.mOnDisconnectLambda,
            callObject.mOnSetActiveLambda,
            callObject.mOnSetInActiveLambda,
        ) {
            mPreCallEndpointAdapter.mSelectedCallEndpoint = null
            // inject client control interface into the VoIP call object
            callObject.setCallId(getCallId().toString())
            callObject.setCallControl(this)

            // Collect updates
            launch { currentCallEndpoint.collect { callObject.onCallEndpointChanged(it) } }

            launch { availableEndpoints.collect { callObject.onAvailableCallEndpointsChanged(it) } }

            launch { isMuted.collect { callObject.onMuteStateChanged(it) } }
            addCallRow(callObject)
        }
    }

    @OptIn(ExperimentalAppActions::class)
    private suspend fun addCallWithExtensions(
        attributes: CallAttributesCompat,
        callObject: VoipCall,
        isRaiseHandEnabled: Boolean = false,
        isKickParticipantEnabled: Boolean = false
    ) {
        mCallsManager!!.addCallWithExtensions(
            attributes,
            callObject.mOnAnswerLambda,
            callObject.mOnDisconnectLambda,
            callObject.mOnSetActiveLambda,
            callObject.mOnSetInActiveLambda,
        ) {
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
                mPreCallEndpointAdapter.mSelectedCallEndpoint = null
                // inject client control interface into the VoIP call object
                callObject.setCallId(getCallId().toString())
                callObject.setCallControl(this)
                callObject.setParticipantControl(
                    ParticipantControl(
                        onParticipantAdded = participants::addParticipant,
                        onParticipantRemoved = participants::removeParticipant
                    )
                )
                addCallRow(callObject)

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
                    while (true) {
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
        val endpointsFlow = mCallsManager!!.getAvailableStartingCallEndpoints()
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

    private fun logException(e: Exception, prefix: String) {
        Log.i(TAG, "$prefix: e=[$e], e.msg=[${e.message}], e.stack:${e.printStackTrace()}")
    }

    private fun addCallRow(callObject: VoipCall) {
        mCallObjects.add(CallRow(++mCallCount, callObject))
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
