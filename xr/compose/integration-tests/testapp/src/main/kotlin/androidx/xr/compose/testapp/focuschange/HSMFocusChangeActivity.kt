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

package androidx.xr.compose.testapp.focuschange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/*
 * Testing onWindowFocusChanged fires when it's supposed to in Home Space Mode
 * - Does not trigger on low priority notification.
 * - Does not trigger on default priority notification.
 * - Does not trigger on high priority notification.
 * - Same-app activity switch triggers lose focus.
 * - Launching 2nd app triggers lose focus.
 */

class HSMFocusChangeActivity : ComponentActivity() {
    private var runAutomated: Boolean = false
    private var hasWindowFocus by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runAutomated = (intent.getStringExtra("run") == "automated")
        setContent { FocusChangeContent(this, runAutomated, hasWindowFocus) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus
    }
}
