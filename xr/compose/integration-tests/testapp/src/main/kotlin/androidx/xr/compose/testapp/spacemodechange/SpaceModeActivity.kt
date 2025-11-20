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

package androidx.xr.compose.testapp.spacemodechange

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.composables.FixedSizeFullSpaceLayout
import androidx.xr.compose.testapp.common.composables.TestResult
import androidx.xr.compose.testapp.common.composables.TestResultsDisplay
import androidx.xr.compose.testapp.common.composables.addTestResult
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import kotlinx.coroutines.delay

/*
 * Ensure switching between HomeSpaceMode and FullSpaceMode triggers appropriate callbacks
 */

class SpaceModeActivity : ComponentActivity() {
    private var runAutomated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runAutomated = (intent.getStringExtra("run") == "automated")
        setContent { SpaceModeContent() }
    }

    @Composable
    fun SpaceModeContent() {
        val tag = "SpaceModeActivity"
        val session =
            checkNotNull(LocalSession.current) {
                "LocalSession.current was null. Session must be available."
            }

        val testResults = remember { mutableStateListOf<TestResult>() }
        var fullSpaceCallbackReceived by remember { mutableStateOf(false) }
        var homeSpaceCallbackReceived by remember { mutableStateOf(false) }
        var testStatus by remember { mutableStateOf("Running..") }

        session.scene.addSpatialCapabilitiesChangedListener { _ ->
            if (session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)) {
                Log.d(tag, "fullSpaceCallback Received")
                fullSpaceCallbackReceived = true
            } else {
                Log.d(tag, "homeSpaceCallback Received")
                homeSpaceCallbackReceived = true
            }
        }

        LaunchedEffect(Unit) {
            delay(1000)
            session.scene.requestFullSpaceMode()
            delay(3000)
            addTestResult(
                testResults,
                tag,
                "FullSpace callback received",
                fullSpaceCallbackReceived,
            )

            delay(1000)
            session.scene.requestHomeSpaceMode()
            delay(3000)
            addTestResult(
                testResults,
                tag,
                "HomeSpace callback received",
                homeSpaceCallbackReceived,
            )

            delay(1000)
            session.scene.requestFullSpaceMode()
            delay(3000)
            testStatus = "Finished"
            if (runAutomated) {
                delay(3000)
                finish()
            }
        }
        Subspace {
            FixedSizeFullSpaceLayout(getString(R.string.space_mode_change_test)) {
                TestResultsDisplay(testResults)
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                Text(
                    testStatus,
                    fontSize = 30.sp,
                    modifier = Modifier.Companion.padding(top = 16.dp),
                )
            }
        }
    }
}
