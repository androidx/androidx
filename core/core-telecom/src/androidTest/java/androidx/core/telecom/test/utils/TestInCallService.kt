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

import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.InCallServiceCompat
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** InCallServiceCompat implementation for testing */
@RequiresApi(Build.VERSION_CODES.O)
internal class TestInCallService : InCallServiceCompat() {

    /** Local Binder used by tests in this process to access the Service Directly */
    inner class LocalBinder : Binder() {
        fun getService(): TestInCallService = this@TestInCallService
    }

    private val localBinder = LocalBinder()
    private val mCalls: MutableList<Call> = Collections.synchronizedList(ArrayList<Call>())
    private val mTelecomBoundFlow = MutableStateFlow(false)

    companion object {
        const val LOG_TAG = "TestInCallService"
    }

    override fun onCallAdded(call: Call) {
        if (!mCalls.contains(call)) {
            Log.i(LOG_TAG, "ICS.onCallAdded: added the new call to static call list")
            mCalls.add(call)
        } else {
            Log.w(LOG_TAG, "ICS.onCallAdded: call already exists!")
        }
    }

    override fun onCallRemoved(call: Call?) {
        Log.i(LOG_TAG, String.format("ICS.onCallRemoved: call=[%s]", call))
        mCalls.remove(call)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE) {
            Log.i(LOG_TAG, "InCallService bound from telecom")
            mTelecomBoundFlow.tryEmit(true)
            return super.onBind(intent)
        }
        Log.i(LOG_TAG, "InCallService bound by ${intent?.component}")
        return localBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (intent?.action == SERVICE_INTERFACE) {
            Log.i(LOG_TAG, "InCallService unbound from telecom")
            mTelecomBoundFlow.tryEmit(false)
            return super.onUnbind(intent)
        }
        Log.i(LOG_TAG, "InCallService unbound by ${intent?.component}")
        // Control interface disconnected, so disconnect all calls
        disconnectAllCalls()
        return false
    }

    /** Disconnect all calls and wait until telecom disconnects as a result */
    suspend fun destroyAllCalls() {
        disconnectAllCalls()
        // Wait for the InCallService to unbind from Telecom before the next test.
        runCatching {
                withTimeout(5000) {
                    if (isTelecomBound()) {
                        mTelecomBoundFlow.first { isBound -> !isBound }
                    }
                }
            }
            .onFailure { Log.w(LOG_TAG, "destroyAlLCalls: no unbind detected during destroy") }
    }

    /** Return true if Telecom is bound to this InCallService, false if it is not */
    fun isTelecomBound(): Boolean {
        return mTelecomBoundFlow.value
    }

    /** Disconnect all calls that this InCallService is tracking */
    @Suppress("deprecation")
    private fun disconnectAllCalls() {
        if (mCalls.isEmpty()) return
        Log.i(LOG_TAG, "disconnectAllCalls: Calls.size=[${mCalls.size}]")
        for (call in mCalls) {
            if (call.state != Call.STATE_DISCONNECTED || call.state != Call.STATE_DISCONNECTING) {
                Log.i(LOG_TAG, "destroyAllCalls: disconnecting call=[$call]")
                call.disconnect()
            }
        }
        mCalls.clear()
    }

    fun getLastCall(): Call? {
        return if (mCalls.size == 0) {
            null
        } else {
            mCalls[mCalls.size - 1]
        }
    }

    fun getCallCount(): Int {
        return mCalls.size
    }
}
