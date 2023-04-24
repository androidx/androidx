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
import androidx.core.telecom.CallAttributes
import androidx.core.telecom.CallsManager
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // Call Log objects
    private var mRecyclerView: RecyclerView? = null
    private var mCallObjects: ArrayList<CallRow> = ArrayList()
    private var mAdapter: CallListAdapter? = CallListAdapter(mCallObjects)

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mCallsManager = CallsManager(this)
        mCallCount = 0

        val registerPhoneAccountButton = findViewById<Button>(R.id.registerButton)
        registerPhoneAccountButton.setOnClickListener {
            mScope.launch {
                registerPhoneAccount()
            }
        }

        val addOutgoingCallButton = findViewById<Button>(R.id.addOutgoingCall)
        addOutgoingCallButton.setOnClickListener {
            mScope.launch {
                addCallWithAttributes(Utilities.OUTGOING_CALL_ATTRIBUTES)
            }
        }

        val addIncomingCallButton = findViewById<Button>(R.id.addIncomingCall)
        addIncomingCallButton.setOnClickListener {
            mScope.launch {
                addCallWithAttributes(Utilities.INCOMING_CALL_ATTRIBUTES)
            }
        }

        // set up the call list view holder
        mRecyclerView = findViewById(R.id.callListRecyclerView)
        mRecyclerView?.layoutManager = LinearLayoutManager(this)
        mRecyclerView?.adapter = mAdapter
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

    private suspend fun addCallWithAttributes(attributes: CallAttributes) {
        Log.i(TAG, "addCallWithAttributes: attributes=$attributes")
        val callObject = VoipCall()

        CoroutineScope(Dispatchers.IO).launch {
            val coroutineScope = this
            try {
                mCallsManager!!.addCall(attributes) {
                    // set the client callback implementation
                    setCallback(callObject.mCallControlCallbackImpl)

                    // inject client control interface into the VoIP call object
                    callObject.setCallId(getCallId().toString())
                    callObject.setCallControl(this)

                    // Collect updates
                    currentCallEndpoint
                        .onEach { callObject.onCallEndpointChanged(it) }
                        .launchIn(coroutineScope)

                    availableEndpoints
                        .onEach { callObject.onAvailableCallEndpointsChanged(it) }
                        .launchIn(coroutineScope)

                    isMuted
                        .onEach { callObject.onMuteStateChanged(it) }
                        .launchIn(coroutineScope)
                }
                addCallRow(callObject)
            } catch (e: CancellationException) {
                Log.i(TAG, "addCallWithAttributes: cancellationException:$e")
            }
        }
    }

    private fun addCallRow(callObject: VoipCall) {
        mCallObjects.add(CallRow(++mCallCount, callObject))
        callObject.setCallAdapter(mAdapter)
        updateCallList()
    }

    private fun updateCallList() {
        runOnUiThread {
            mAdapter?.notifyDataSetChanged()
        }
    }
}
