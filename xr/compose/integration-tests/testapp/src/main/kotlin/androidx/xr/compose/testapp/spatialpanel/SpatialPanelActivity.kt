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
package androidx.xr.compose.testapp.spatialpanel

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.onGloballyPositioned
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.composables.BasicLayout
import androidx.xr.compose.testapp.common.composables.FixedSizeFullSpaceLayout
import androidx.xr.compose.testapp.common.composables.TestResult
import androidx.xr.compose.testapp.common.composables.TestResultsDisplay
import androidx.xr.compose.testapp.common.composables.addTestResult
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import kotlinx.coroutines.delay

/*
 * This test checks how an activity in Full Space Mode reacts to a reconfiguration event.
 * This can be caused by a settings change, app resize or other factors.
 * In particular we want to test whether items lose their position in a recreation event
 * - Move panel and verify the delta in its X coordinate.
 * - Verify the Y coordinate did not change.
 * - Trigger activity reconfiguration and verify the X coordinate delta is preserved.
 * - Verify the Y coordinate did not change (just as a control).
 */

class SpatialPanelActivity : ComponentActivity() {
    private var runAutomated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runAutomated = (intent.getStringExtra("run") == "automated")
        setContent { CaseSpatialPanel() }
    }

    @Composable
    fun CaseSpatialPanel() {
        val tag = "SpatialPanelActivity"

        var panelOffset by remember { mutableStateOf(Offset(0f, 0f)) }
        val session =
            checkNotNull(LocalSession.current) {
                "LocalSession.current was null. Session must be available."
            }

        val testResults = remember { mutableStateListOf<TestResult>() }
        var testResult: Boolean

        var x1 by rememberSaveable { mutableStateOf<Float?>(null) }
        var y1 by rememberSaveable { mutableStateOf<Float?>(null) }
        var x2 by rememberSaveable { mutableStateOf<Float?>(null) }
        var y2 by rememberSaveable { mutableStateOf<Float?>(null) }
        var x3 by rememberSaveable { mutableStateOf<Float?>(null) }
        var y3 by rememberSaveable { mutableStateOf<Float?>(null) }
        var creationLoopCount by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            creationLoopCount++
            if (creationLoopCount == 1) {
                delay(1000)
                panelOffset = Offset(panelOffset.x + 100f, panelOffset.y)
                delay(1000)

                testResult = (x2 != x1)
                addTestResult(testResults, tag, "Panel X-coordinate changed", testResult)

                testResult = (y2 == y1)
                addTestResult(testResults, tag, "Panel Y-coordinate did not change", testResult)

                recreate() // Simulate having an OS settings change.
            } else {
                // A recreation event has occurred if we're in this code block.
                delay(1000)
                // TODO: b/430264066 requestFullSpaceMode() is failing after activity.recreate()
                try {
                    session.scene.requestFullSpaceMode()
                    delay(1000)
                    testResult =
                        session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)
                    addTestResult(
                        testResults,
                        tag,
                        "App is in FSM after reconfiguration",
                        testResult,
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error requesting full space mode: ${e.message}", e)
                    addTestResult(testResults, tag, "App is in FSM after reconfiguration", false)
                }

                testResult = (x1 == x3)
                addTestResult(
                    testResults,
                    tag,
                    "X coordinate resets to original position",
                    testResult,
                )

                testResult = (y2 == y3)
                addTestResult(
                    testResults,
                    tag,
                    "Y coordinate maintains its unaltered position",
                    testResult,
                )

                if (runAutomated) {
                    delay(3000)
                    finish()
                }
            }
        }

        Subspace {
            SpatialColumn {
                FixedSizeFullSpaceLayout(getString(R.string.spatial_panel_test)) {
                    TestResultsDisplay(testResults)
                }
                SpatialPanel(
                    resizePolicy = ResizePolicy(),
                    modifier =
                        SubspaceModifier.width(1200.dp)
                            .height(200.dp)
                            .offset(panelOffset.x.dp, panelOffset.y.dp)
                            .movable()
                            .onGloballyPositioned {
                                val newX = it?.poseInRoot?.translation?.x
                                val newY = it?.poseInRoot?.translation?.y
                                // Save the initial values in x1 and y1.
                                // Any altered position before the recreation event will be saved
                                // in x2 and y2.
                                // The coordinates after the recreation event will be in x3 and y3.
                                if (x1 == null && y1 == null) {
                                    x1 = newX
                                    y1 = newY
                                } else if (
                                    creationLoopCount == 1 && (newX ?: 0f) > 0f && x2 == null
                                ) {
                                    x2 = newX
                                    y2 = newY
                                } else if (creationLoopCount > 1) {
                                    x3 = newX
                                    y3 = newY
                                }
                            },
                ) {
                    Box(
                        modifier =
                            Modifier.background(Color.White)
                                .fillMaxSize()
                                .padding(top = 40.dp, start = 40.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Text(
                            text =
                                "This panel will move automatically and should reset " +
                                    "after the app recreation occurs",
                            fontSize = 30.sp,
                        )
                    }
                }
            }
        }

        // Show results in HSM in case there are problems with FSM
        BasicLayout("Spatial Panel Recomposition") { TestResultsDisplay(testResults) }
    }
}
