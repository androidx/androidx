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

package androidx.xr.compose.testapp.lifecycle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

open class BaseLifecycleTestActivity() : ComponentActivity() {
    protected var runAutomated: Boolean = false
    private var activityName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        activityName = this.localClassName.replace("activities.", "")
        super.onCreate(savedInstanceState)
        runAutomated = (intent.getStringExtra("run") == "automated")
        Log.i(TAG, "[$activityName] Started")
        Log.i(TAG, "[$activityName] Running Automated: $runAutomated")
        Log.d(TAG, "[$activityName] Lifecycle Event: onCreate")
        LifecycleDataStore.addLifecycleEvent(this, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "[$activityName] Lifecycle Event: onStart")
        LifecycleDataStore.addLifecycleEvent(this, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[$activityName] Lifecycle Event: onResume")
        LifecycleDataStore.addLifecycleEvent(this, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[$activityName] Lifecycle Event: onPause")
        LifecycleDataStore.addLifecycleEvent(this, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "[$activityName] Lifecycle Event: onStop")
        LifecycleDataStore.addLifecycleEvent(this, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "[$activityName] Lifecycle Event: onDestroy")
        LifecycleDataStore.addLifecycleEvent(this, "onDestroy")
        Log.i(TAG, "[$activityName] Ended Success")
    }
}
