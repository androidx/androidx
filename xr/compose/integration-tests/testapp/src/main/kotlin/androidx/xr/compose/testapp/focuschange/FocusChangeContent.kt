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
import kotlinx.coroutines.delay

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

    var lostFocusDetected by remember { mutableStateOf(false) }
    val testResults = remember { mutableStateListOf<TestResult>() }
    var testStatus by remember { mutableStateOf("Running...") }

    // Use an effect to track the state change from the Activity's onWindowFocusChanged
    LaunchedEffect(hasWindowFocus) {
        // If hasWindowFocus becomes false, we've detected a loss of focus.
        if (!hasWindowFocus) {
            lostFocusDetected = true
        }
    }

    LaunchedEffect(Unit) {
        if (isFullSpaceMode) {
            session?.scene?.requestFullSpaceMode()
            delay(2000)
        }

        // Different priority notification test
        lostFocusDetected = false
        notificationTest(context, NotificationCompat.PRIORITY_LOW)
        delay(3000)
        addTestResult(
            testResults,
            tag,
            "Low priority notification did not trigger lose-focus",
            !lostFocusDetected,
        )

        lostFocusDetected = false
        notificationTest(context, NotificationCompat.PRIORITY_DEFAULT)
        delay(3000)
        addTestResult(
            testResults,
            tag,
            "Default priority notification did not trigger lose-focus",
            !lostFocusDetected,
        )

        lostFocusDetected = false
        notificationTest(context, NotificationCompat.PRIORITY_HIGH)
        delay(3000)
        addTestResult(
            testResults,
            tag,
            "High priority notification did not trigger lose-focus",
            !lostFocusDetected,
        )

        // Same app activity switch test
        lostFocusDetected = false
        testStatus = "Launching an activity..."
        val intentFocusStealer = Intent(activity, FocusStealerActivity::class.java)
        activity.startActivity(intentFocusStealer)
        delay(8000)
        addTestResult(testResults, tag, "Activity switch triggered lose-focus", lostFocusDetected)

        // Launching 2nd app test
        lostFocusDetected = false
        testStatus = "Launching the setting as a 2nd app..."
        val settingAppIntent = Intent(Settings.ACTION_SETTINGS)
        settingAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(settingAppIntent)
        delay(10000)
        addTestResult(testResults, tag, "Loading 2nd app triggered lose-focus", lostFocusDetected)

        testStatus = "Finished"

        if (runAutomated) {
            delay(3000)
            activity.finish()
        }
    }

    Subspace {
        FixedSizeFullSpaceLayout(title) {
            TestResultsDisplay(testResults)
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
            Text(testStatus, fontSize = 30.sp, modifier = Modifier.Companion.padding(top = 30.dp))
        }
    }
}
