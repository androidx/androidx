/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.xr.compose.testapp.focuschange

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.composables.FixedSizeFullSpaceLayout
import androidx.xr.compose.testapp.common.composables.TestResult
import androidx.xr.compose.testapp.common.composables.TestResultsDisplay
import androidx.xr.compose.testapp.common.composables.addTestResult
import androidx.xr.scenecore.scene
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun FocusChangeContent(
    activity: ComponentActivity,
    runAutomated: Boolean,
    hasWindowFocus: Boolean,
    isFullSpaceMode: Boolean = false,
) {
    val context = LocalContext.current.applicationContext
    val session = LocalSession.current
    val tag = if (isFullSpaceMode) "FSMFocusChangeActivity" else "HSMFocusChangeActivity"
    val title =
        if (isFullSpaceMode) activity.getString(R.string.fsm_focus_change_test)
        else activity.getString(R.string.hsm_focus_change_test)

    val testResults = remember { mutableStateListOf<TestResult>() }
    var testStatus by remember { mutableStateOf("Running...") }

    // Helper to wait for a target window focus state reactively with timeout
    suspend fun awaitFocusState(expectedFocus: Boolean, timeoutMs: Long = 5000): Boolean {
        return withTimeoutOrNull(timeoutMs.milliseconds) {
            snapshotFlow { hasWindowFocus }.first { it == expectedFocus }
        } != null
    }

    // Helper to verify that focus remains in expected state throughout evaluation duration
    suspend fun verifyFocusUnchanged(expectedFocus: Boolean, durationMs: Long = 1500): Boolean {
        val focusChanged =
            withTimeoutOrNull(durationMs.milliseconds) {
                snapshotFlow { hasWindowFocus }.first { it != expectedFocus }
            }
        return focusChanged == null
    }

    LaunchedEffect(Unit) {
        if (isFullSpaceMode) {
            session?.scene?.requestFullSpace()
            awaitFocusState(expectedFocus = true, timeoutMs = 3000)
        }

        // 1. Low Priority Notification Test (Expect NO focus loss)
        notificationTest(context, NotificationCompat.PRIORITY_LOW)
        val lowPriorityPassed = verifyFocusUnchanged(expectedFocus = true)
        addTestResult(
            testResults,
            tag,
            "Low priority notification did not trigger lose-focus",
            lowPriorityPassed,
        )

        // 2. Default Priority Notification Test (Expect NO focus loss)
        notificationTest(context, NotificationCompat.PRIORITY_DEFAULT)
        val defaultPriorityPassed = verifyFocusUnchanged(expectedFocus = true)
        addTestResult(
            testResults,
            tag,
            "Default priority notification did not trigger lose-focus",
            defaultPriorityPassed,
        )

        // 3. High Priority Notification Test (Expect NO focus loss)
        notificationTest(context, NotificationCompat.PRIORITY_HIGH)
        val highPriorityPassed = verifyFocusUnchanged(expectedFocus = true)
        addTestResult(
            testResults,
            tag,
            "High priority notification did not trigger lose-focus",
            highPriorityPassed,
        )

        // 4. Same App Activity Switch Test (Expect FOCUS LOST, then REGAINED)
        testStatus = "Launching an activity..."
        activity.startActivity(Intent(activity, FocusStealerActivity::class.java))
        val switchFocusLost = awaitFocusState(expectedFocus = false, timeoutMs = 5000)
        addTestResult(testResults, tag, "Activity switch triggered lose-focus", switchFocusLost)

        // Wait for FocusStealerActivity to finish and return focus to main activity
        awaitFocusState(expectedFocus = true, timeoutMs = 5000)

        // 5. Launching 2nd App Test (Expect FOCUS LOST)
        testStatus = "Launching the setting as a 2nd app..."
        val settingAppIntent =
            Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        activity.startActivity(settingAppIntent)
        val secondAppFocusLost = awaitFocusState(expectedFocus = false, timeoutMs = 5000)
        addTestResult(testResults, tag, "Loading 2nd app triggered lose-focus", secondAppFocusLost)

        // Bring main test activity back to front to restore window focus and cleanup
        val reorderIntent =
            Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        activity.startActivity(reorderIntent)
        awaitFocusState(expectedFocus = true, timeoutMs = 3000)

        testStatus = "Finished"

        if (runAutomated) {
            delay(1000.milliseconds)
            activity.finish()
        }
    }

    Subspace {
        FixedSizeFullSpaceLayout(title) {
            TestResultsDisplay(testResults)
            Text(testStatus, fontSize = 30.sp, modifier = Modifier.padding(top = 30.dp))
        }
    }
}
