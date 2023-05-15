/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.ambient

import android.os.Bundle
import androidx.activity.ComponentActivity

class AmbientLifecycleObserverTestActivity : ComponentActivity() {
    private val callback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(
            ambientDetails: AmbientLifecycleObserver.AmbientDetails
        ) {
            enterAmbientCalled = true
            enterAmbientArgs = ambientDetails
        }

        override fun onUpdateAmbient() {
            updateAmbientCalled = true
        }

        override fun onExitAmbient() {
            exitAmbientCalled = true
        }
    }

    val observer = AmbientLifecycleObserver(this, { r -> r.run() }, callback)

    var enterAmbientCalled = false
    var enterAmbientArgs: AmbientLifecycleObserver.AmbientDetails? = null
    var updateAmbientCalled = false
    var exitAmbientCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(observer)
    }
}