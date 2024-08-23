/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ThreePaneScaffoldMotionScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3_ADAPTIVE)

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress0() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress0")
    }

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress10() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.1f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress10")
    }

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress15() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.15f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress15")
    }

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress20() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.2f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress20")
    }

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress50() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.5f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress50")
    }

    @Test
    fun singlePaneLayout_defaultPaneMotion_progress100() {
        rule.setContent {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueSinglePane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(1f, MockTargetScaffoldValueSinglePane) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "singlePaneLayout_defaultPaneMotion_progress100")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress0() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0f, MockTargetScaffoldValuePaneSwitching) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress0")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress10() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.1f, MockTargetScaffoldValuePaneSwitching)
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress10")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress15() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.15f, MockTargetScaffoldValuePaneSwitching)
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress15")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress20() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.2f, MockTargetScaffoldValuePaneSwitching)
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress20")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress50() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.5f, MockTargetScaffoldValuePaneSwitching)
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress50")
    }

    @Test
    fun dualPaneLayout_defaultPaneSwitching_progress100() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(1f, MockTargetScaffoldValuePaneSwitching) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneSwitching_progress100")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress0() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0f, MockTargetScaffoldValuePaneShifting) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress0")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress10() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.1f, MockTargetScaffoldValuePaneShifting) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress10")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress15() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.15f, MockTargetScaffoldValuePaneShifting)
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress15")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress20() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.2f, MockTargetScaffoldValuePaneShifting) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress20")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress50() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(0.5f, MockTargetScaffoldValuePaneShifting) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress50")
    }

    @Test
    fun dualPaneLayout_defaultPaneShifting_progress100() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val scaffoldState = ThreePaneScaffoldState(MockOriginalScaffoldValueDualPane)
            SampleThreePaneScaffold(scaffoldState)
            LaunchedEffect(Unit) { scaffoldState.seekTo(1f, MockTargetScaffoldValuePaneShifting) }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "dualPaneLayout_defaultPaneShifting_progress100")
    }

    companion object {
        val MockOriginalScaffoldValueSinglePane =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
            )

        val MockTargetScaffoldValueSinglePane =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
            )

        val MockOriginalScaffoldValueDualPane =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
            )

        val MockTargetScaffoldValuePaneSwitching =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
            )

        val MockTargetScaffoldValuePaneShifting =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
            )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SampleThreePaneScaffold(
    scaffoldState: ThreePaneScaffoldState,
) {
    ThreePaneScaffold(
        modifier = Modifier.fillMaxSize().testTag(ThreePaneScaffoldTestTag),
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
        scaffoldState = scaffoldState,
        paneOrder = SupportingPaneScaffoldDefaults.PaneOrder,
        secondaryPane = {
            AnimatedPane(modifier = Modifier.testTag(tag = "SecondaryPane")) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.secondary
                ) {}
            }
        },
        tertiaryPane = {
            AnimatedPane(modifier = Modifier.testTag(tag = "TertiaryPane")) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary
                ) {}
            }
        }
    ) {
        AnimatedPane(modifier = Modifier.testTag(tag = "PrimaryPane")) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {}
        }
    }
}
