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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import androidx.xr.compose.testapp.common.composables.BasicLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Create new activity within JXR and verify that all basic lifecycle events and sequence are observed.
 * Parent activity will display the results
 */

class OpenCloseActivity : ComponentActivity() {
    private var runAutomated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        runAutomated = (intent.getStringExtra("run") == "automated")
        super.onCreate(savedInstanceState)

        setContent { OpenCloseScreen(this) }
    }

    @Composable
    fun OpenCloseScreen(activity: OpenCloseActivity) {
        val childLifecycleEvents = remember { mutableStateOf<List<String>>(emptyList()) }
        val hasChildFinished = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == RESULT_OK) {
                    hasChildFinished.value = true
                }
            }

        LaunchedEffect(hasChildFinished.value) {
            if (hasChildFinished.value) {
                delay(3000) // wait until the child activity fully destroyed
                childLifecycleEvents.value =
                    LifecycleDataStore.readLifecycleEvents(activity, "OpenCloseChildActivity")
                if (runAutomated) {
                    delay(3000)
                    finish()
                }
            } else {
                scope.launch {
                    val intent = Intent(activity, OpenCloseChildActivity::class.java)
                    launcher.launch(intent)
                }
            }
        }

        BasicLayout("Open Close Activity") {
            if (hasChildFinished.value && childLifecycleEvents.value.isNotEmpty()) {
                LifecycleEventComparator(
                    activityName = activity.localClassName,
                    expectedEvents = ExpectedLifecycleEvents.OPEN_CLOSE,
                    actualEvents = childLifecycleEvents.value,
                )
            } else {
                Text("Opening new activity...", fontSize = 30.sp)
            }
        }
    }
}
