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

package androidx.core.telecom.testdialerapp

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class InCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        // Register the call with our manager
        CallManager.registerCall(call)

        // Launch the In-Call UI
        val intent = Intent(this, InCallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)

        // Unregister the call when it's removed (e.g., call ended)
        CallManager.unregisterCall()
    }
}
