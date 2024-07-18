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
import android.media.AudioManager.AudioRecordingCallback
import android.media.AudioRecord
import android.os.Bundle
import android.telecom.DisconnectCause
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(34)
class CallingMainActivity : Activity() {
    // Activity
    private val TAG = CallingMainActivity::class.simpleName
    private val mScope = CoroutineScope(Dispatchers.Default)
    private var mCallCount: Int = 0

    // Telecom
    private var mCallsManager: CallsManager? = null

    // Audio Record
    private var mAudioRecord: AudioRecord? = null
    private var mAudioRecordingCallback: AudioRecordingCallback? = null

    // Call Log objects
    private var mRecyclerView: RecyclerView? = null
    private var mCallObjects: ArrayList<CallRow> = ArrayList()
    private lateinit var mAdapter: CallListAdapter

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
                startAudioRecording()
                addCallWithAttributes(Utilities.OUTGOING_CALL_ATTRIBUTES)
            }
        }

        val addIncomingCallButton = findViewById<Button>(R.id.addIncomingCall)
        addIncomingCallButton.setOnClickListener {
            mScope.launch {
                startAudioRecording()
                addCallWithAttributes(Utilities.INCOMING_CALL_ATTRIBUTES)
            }
        }

        // Set up AudioRecord
        mAudioRecord = Utilities.createAudioRecord(applicationContext, this)
        mAdapter = CallListAdapter(mCallObjects, mAudioRecord)

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

        // Clean up AudioRecord
        mAudioRecord?.release()
        mAudioRecord = null
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

    private suspend fun addCallWithAttributes(attributes: CallAttributesCompat) {
        Log.i(TAG, "addCallWithAttributes: attributes=$attributes")
        val callObject = VoipCall()

        try {
            val handler = CoroutineExceptionHandler { _, exception ->
                Log.i(TAG, "CoroutineExceptionHandler: handling e=$exception")
            }

            CoroutineScope(Dispatchers.IO).launch(handler) {
                try {
                    mCallsManager!!.addCall(
                        attributes,
                        callObject.mOnAnswerLambda,
                        callObject.mOnDisconnectLambda,
                        callObject.mOnSetActiveLambda,
                        callObject.mOnSetInActiveLambda
                    ) {
                        // inject client control interface into the VoIP call object
                        callObject.setCallId(getCallId().toString())
                        callObject.setCallControl(this)

                        // Collect updates
                        launch {
                            currentCallEndpoint.collect {
                                callObject.onCallEndpointChanged(it)
                            }
                        }

                        launch {
                            availableEndpoints.collect {
                                callObject.onAvailableCallEndpointsChanged(it)
                            }
                        }

                        launch {
                            isMuted.collect {
                                callObject.onMuteStateChanged(it)
                            }
                        }
                        addCallRow(callObject)
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

    private fun logException(e: Exception, prefix: String) {
        Log.i(TAG, "$prefix: e=[$e], e.msg=[${e.message}], e.stack:${e.printStackTrace()}")
    }

    private fun addCallRow(callObject: VoipCall) {
        mCallObjects.add(CallRow(++mCallCount, callObject))
        callObject.setCallAdapter(mAdapter)
        updateCallList()
    }

    private fun updateCallList() {
        runOnUiThread {
            mAdapter.notifyDataSetChanged()
        }
    }

    private fun startAudioRecording() {
        mAudioRecordingCallback = Utilities.TelecomAudioRecordingCallback(mAudioRecord!!)
        mAudioRecord?.registerAudioRecordingCallback(
            Executors.newSingleThreadExecutor(), mAudioRecordingCallback!!)
        mAdapter.mAudioRecordingCallback = mAudioRecordingCallback
        mAudioRecord?.startRecording()
    }
}
