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
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Collections

@RequiresApi(Build.VERSION_CODES.M)
class MockInCallService : InCallService() {
    companion object {
        val LOG_TAG = "MockInCallService"
        val mCalls = Collections.synchronizedList(ArrayList<Call>())
        var mIsServiceBound = false
        var mService: MockInCallService? = null

        @Suppress("deprecation")
        fun destroyAllCalls() {
            Log.i(LOG_TAG, "destroyAllCalls: Calls.size=[${mCalls.size}]")
            mIsServiceBound = false
            for (call in mCalls) {
                if (call.state != Call.STATE_DISCONNECTED ||
                    call.state != Call.STATE_DISCONNECTING
                ) {
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

        fun setMute(muted: Boolean) {
            mService?.setMuted(muted)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(LOG_TAG, "Service bounded")
        mIsServiceBound = true
        if (mService == null) {
            mService = this
        }
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(LOG_TAG, "Service has been unbound")
        mIsServiceBound = false
        mService = null
        return super.onUnbind(intent)
    }

    override fun onCallAdded(call: Call) {
        Log.i(LOG_TAG, String.format("onCallAdded: call=[%s]", call))
        super.onCallAdded(call)

        if (!mCalls.contains(call)) {
            Log.i(LOG_TAG, "onCallAdded: added the new call to static call list")
            mCalls.add(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        Log.i(LOG_TAG, String.format("onCallRemoved: call=[%s]", call))
        super.onCallRemoved(call)
        mCalls.remove(call)
    }
}
