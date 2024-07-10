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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.addParticipantsSupport
import androidx.core.telecom.internal.CallCompat
import androidx.core.telecom.internal.InCallServiceCompat
import androidx.core.telecom.test.utils.TestUtils.printParticipants
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.lifecycleScope
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Delegate service that will point to either an InCallService or InCallServiceCompat based on the
 * way that the Delegate is configured.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class MockInCallServiceDelegate : Service() {

    @OptIn(ExperimentalAppActions::class)
    class InCallServiceWoExtensions(context: Context) : InCallService() {
        init {
            // Icky hack, but since we are using a delegate, we need to attach the Context manually.
            if (baseContext == null) {
                attachBaseContext(context)
            }
        }

        override fun onCallAdded(call: Call?) {
            val callCompat = call?.let { c -> CallCompat.toCallCompat(c) {} }
            if (!mCalls.contains(callCompat)) {
                Log.i(LOG_TAG, "ICS.onCallAdded: added the new call to static call list")
                mCalls.add(callCompat)
            }
        }

        override fun onCallRemoved(call: Call) {
            Log.i(LOG_TAG, String.format("ICS.onCallRemoved: call=[%s]", call))
            mCalls.removeIf { c -> c.toCall() == call }
        }
    }

    @OptIn(ExperimentalAppActions::class)
    class InCallServiceWExtensionsNew(context: Context) : InCallServiceCompat() {
        init {
            // Icky hack, but since we are using a delegate, we need to attach the Context manually.
            if (baseContext == null) {
                attachBaseContext(context)
            }
        }

        override fun onCallAdded(call: Call) {
            val callCompat = call.let { c -> CallCompat.toCallCompat(c) {} }
            if (!mCalls.contains(callCompat)) {
                Log.i(LOG_TAG, "ICSCN.onCallAdded: added the new call to static call list")
                mCalls.add(callCompat)
            }
        }

        override fun onCallRemoved(call: Call?) {
            Log.i(LOG_TAG, String.format("ICSCN.onCallRemoved: call=[%s]", call))
            mCalls.removeIf { c -> c.toCall() == call }
        }
    }

    @ExperimentalAppActions
    class InCallServiceWExtensionsOld(context: Context, val capabilities: Set<Capability>) :
        InCallServiceCompat() {
        init {
            // Icky hack, but since we are using a delegate, we need to attach the Context manually.
            if (baseContext == null) {
                attachBaseContext(context)
            }
        }

        override fun onCreateCallCompat(call: Call): CallCompat {
            Log.i(LOG_TAG, "ICSC.onCreateCallCompat: added the new call to static call list")

            // TODO:: make this a factory
            val callCompat =
                CallCompat.toCallCompat(call) {
                    for (capability in capabilities) {
                        when (capability.featureId) {
                            CallsManager.PARTICIPANT -> {
                                addParticipantsSupport(capability.supportedActions.toSet()) {
                                    Log.i(LOG_TAG, "ICSC.onCreateCallCompat: setup participants")
                                    lifecycleScope.launch {
                                        it.participantsStateFlow.collect { participants ->
                                            printParticipants(participants, "ICS participants")
                                        }
                                    }
                                    lifecycleScope.launch {
                                        it.activeParticipantStateFlow.collect { participant ->
                                            Log.i(LOG_TAG, "ICS active participant: $participant")
                                        }
                                    }
                                    lifecycleScope.launch {
                                        it.raisedHandsStateFlow.collect { participants ->
                                            Log.i(LOG_TAG, "ICS raised hands: $participants")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            mCalls.add(callCompat)
            return callCompat
        }

        override fun onRemoveCallCompat(call: CallCompat) {
            Log.i(LOG_TAG, String.format("ICSC.onRemoveCallCompat: call=[%s]", call))
            mCalls.remove(call)
        }
    }

    companion object {
        const val LOG_TAG = "MockInCallServiceDelegate"
        @OptIn(ExperimentalAppActions::class)
        val mCalls = Collections.synchronizedList(ArrayList<CallCompat>())
        var mIsServiceBound = false
        var mInCallServiceType: InCallServiceType = InCallServiceType.ICS_WITHOUT_EXTENSIONS
        @OptIn(ExperimentalAppActions::class) var mExtensions: Set<Capability> = emptySet()
        val mServiceFlow = MutableStateFlow<InCallService?>(null)

        @OptIn(ExperimentalAppActions::class)
        @Suppress("deprecation")
        suspend fun destroyAllCalls() {
            Log.i(LOG_TAG, "destroyAllCalls: Calls.size=[${mCalls.size}]")
            mIsServiceBound = false
            for (call in mCalls) {
                if (
                    call.toCall().state != Call.STATE_DISCONNECTED ||
                        call.toCall().state != Call.STATE_DISCONNECTING
                ) {
                    Log.i(LOG_TAG, "destroyAllCalls: disconnecting call=[$call]")
                    call.toCall().disconnect()
                }
            }
            mCalls.clear()
            // Wait for the InCallService to unbind from Telecom before the next test.
            if (mServiceFlow.value != null) {
                runCatching { withTimeout(5000) { mServiceFlow.first { it == null } } }
                    .onFailure {
                        Log.w(LOG_TAG, "destroyAlLCalls: no unbind detected during destroy")
                    }
            }
        }

        @ExperimentalAppActions
        fun getLastCall(): CallCompat? {
            return if (mCalls.size == 0) {
                null
            } else {
                mCalls[mCalls.size - 1]
            }
        }

        @OptIn(ExperimentalAppActions::class)
        fun getCallCount(): Int {
            return mCalls.size
        }

        fun setMute(muted: Boolean) {
            getService()?.setMuted(muted)
        }

        fun getService(): InCallService? {
            return mServiceFlow.value
        }

        @ExperimentalAppActions
        fun getServiceWithExtensions(): InCallServiceCompat? {
            if (getService() !is InCallServiceCompat) return null
            return getService() as InCallServiceCompat
        }
    }

    @OptIn(ExperimentalAppActions::class)
    override fun onCreate() {
        Log.i(LOG_TAG, "Delegate service onCreate")
        mServiceFlow.tryEmit(
            when (mInCallServiceType) {
                InCallServiceType.ICS_WITH_EXTENSIONS_OLD -> {
                    InCallServiceWExtensionsOld(applicationContext, mExtensions)
                }
                InCallServiceType.ICS_WITH_EXTENSIONS_NEW -> {
                    InCallServiceWExtensionsNew(applicationContext)
                }
                InCallServiceType.ICS_WITHOUT_EXTENSIONS -> {
                    InCallServiceWoExtensions(applicationContext)
                }
            }
        )
        // Delegate onCreate to the Service
        getService()?.onCreate()
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Delegate service onDestroy")
        getService()?.onDestroy()
        mServiceFlow.tryEmit(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(LOG_TAG, "Delegate service bounded")
        mIsServiceBound = true
        return getService()?.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(LOG_TAG, "Delegate service unbound")
        mIsServiceBound = false
        // Back to default for the next test
        mInCallServiceType = InCallServiceType.ICS_WITHOUT_EXTENSIONS
        val result = getService()?.onUnbind(intent)
        mServiceFlow.tryEmit(null)
        return result ?: true
    }
}
