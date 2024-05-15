/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test.VoipAppWithExtensions

import android.app.Service;
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_INCOMING
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_OUTGOING
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.test.ITestAppControl
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAppActions::class)
@RequiresApi(Build.VERSION_CODES.O)
class VoipAppWithExtensionsControl : Service() {
    var mCallsManager: CallsManager? = null
    private var mScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var flowOfParticipants = MutableStateFlow(setOf(Participant()))

    companion object {
        val TAG = VoipAppWithExtensionsControl::class.java.simpleName
        val CLASS_NAME = VoipAppWithExtensionsControl::class.java.canonicalName
    }

    val mBinder: ITestAppControl.Stub = object : ITestAppControl.Stub() {

            var mCurrentCapabilities: MutableList<Capability?> = mutableListOf()

            val mOnSetActiveLambda: suspend () -> Unit = {
                Log.i(TAG, "onSetActive: completing")
            }

            val mOnSetInActiveLambda: suspend () -> Unit = {
                Log.i(TAG, "onSetInactive: completing")
            }

            val mOnAnswerLambda: suspend (type: Int) -> Unit = {
                Log.i(TAG, "onAnswer: callType=[$it]")
            }

            val mOnDisconnectLambda: suspend (cause: DisconnectCause) -> Unit = {
                Log.i(TAG, "onDisconnect: disconnectCause=[$it]")
            }

            override fun addCall(isOutgoing: Boolean): String {
                Log.i(TAG, "VoipAppWithExtensionsControl: addCall: in function")
                var id = ""
                runBlocking {
                    var deferredId = CompletableDeferred<String>()
                    mScope.launch {
                        mCallsManager?.addCall(CallAttributesCompat(
                                "displayName" /* TODO:: make helper */,
                                Uri.parse("123") /* TODO:: make helper */,
                                if (isOutgoing) DIRECTION_OUTGOING else DIRECTION_INCOMING
                            ),
                            mOnAnswerLambda,
                            mOnDisconnectLambda,
                            mOnSetActiveLambda,
                            mOnSetInActiveLambda
                        ) {
                            deferredId.complete(this.getCallId().toString())

                            launch {
                                flowOfParticipants.collect {
                                   TestUtils.printParticipants(it, "VoIP")
                                }
                            }
                        }
                    }
                    deferredId.await()
                    id = deferredId.getCompleted()
                }
                return id
            }

            override fun setVoipCapabilities(capabilities: List<Capability>?) {
                if (mCallsManager != null && capabilities != null) {
                    mCallsManager!!.setVoipCapabilities(capabilities.toMutableList())
                    mCurrentCapabilities = capabilities.toMutableList()
                }
            }

            override fun getVoipCapabilities(): List<Capability?> {
               return mCurrentCapabilities.toList()
            }

            override fun updateParticipants(setOfParticipants: List<Participant>) {
                flowOfParticipants.value = setOfParticipants.toSet()
            }
        }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.component?.className.equals(CLASS_NAME)) {
            mCallsManager = CallsManager(applicationContext)
            return mBinder
        }
        return null
    }
}
