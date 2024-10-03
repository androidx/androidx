/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ThreePaneScaffoldTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun threePaneScaffold_allPanesHidden_noVisiblePanes() {
        val testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_oneExpandedPane_onlyExpandedPanesAreVisible() {
        val testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_twoExpandedPanes_onlyExpandedPanesAreVisible() {
        val testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertExists()
    }

    @Test
    fun threePaneScaffold_threeExpandedPanes_onlyExpandedPanesAreVisible() {
        val testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertExists()
    }

    @Test
    fun threePaneScaffold_scaffoldValueChangeWithSinglePane_expandedPanesAreChanged() {
        var testScaffoldValue by
            mutableStateOf(
                ThreePaneScaffoldValue(
                    PaneAdaptedValue.Expanded,
                    PaneAdaptedValue.Hidden,
                    PaneAdaptedValue.Hidden
                )
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()

        testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )

        rule.waitForIdle()

        rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_scaffoldValueChangeWithDualPane_expandedPanesAreChanged() {
        var testScaffoldValue by
            mutableStateOf(
                ThreePaneScaffoldValue(
                    PaneAdaptedValue.Expanded,
                    PaneAdaptedValue.Hidden,
                    PaneAdaptedValue.Expanded
                )
            )
        rule.setContent { SampleThreePaneScaffold(scaffoldValue = testScaffoldValue) }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertExists()

        testScaffoldValue =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )

        rule.waitForIdle()

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_slowDraggingAndSettling() {
        var mockDraggingPx = 0f
        var expectedSettledOffsetPx = 0
        lateinit var mockPaneExpansionState: PaneExpansionState
        lateinit var scope: CoroutineScope

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            scope = rememberCoroutineScope()
            mockPaneExpansionState = rememberPaneExpansionState(anchors = MockPaneExpansionAnchors)
            mockDraggingPx = with(LocalDensity.current) { 200.dp.toPx() }
            expectedSettledOffsetPx =
                with(LocalDensity.current) { MockPaneExpansionMiddleAnchor.toPx().toInt() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle {
            mockPaneExpansionState.dispatchRawDelta(mockDraggingPx)
            scope.launch { mockPaneExpansionState.settleToAnchorIfNeeded(0F) }
        }

        rule.runOnIdle {
            assertThat(mockPaneExpansionState.currentMeasuredDraggingOffset)
                .isEqualTo(expectedSettledOffsetPx)
        }
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_fastDraggingAndSettling() {
        var mockDraggingPx = 0f
        lateinit var mockPaneExpansionState: PaneExpansionState
        lateinit var scope: CoroutineScope

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            scope = rememberCoroutineScope()
            mockPaneExpansionState = rememberPaneExpansionState(anchors = MockPaneExpansionAnchors)
            mockDraggingPx = with(LocalDensity.current) { 200.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle {
            mockPaneExpansionState.dispatchRawDelta(mockDraggingPx)
            scope.launch { mockPaneExpansionState.settleToAnchorIfNeeded(400F) }
        }

        rule.runOnIdle {
            assertThat(mockPaneExpansionState.currentMeasuredDraggingOffset)
                .isEqualTo(mockPaneExpansionState.maxExpansionWidth)
        }
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_flingOverAnchorAndSettling() {
        var mockDraggingPx = 0f
        lateinit var mockPaneExpansionState: PaneExpansionState
        lateinit var scope: CoroutineScope

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            scope = rememberCoroutineScope()
            mockPaneExpansionState = rememberPaneExpansionState(anchors = MockPaneExpansionAnchors)
            mockDraggingPx = with(LocalDensity.current) { 100.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle {
            mockPaneExpansionState.dispatchRawDelta(mockDraggingPx)
            scope.launch { mockPaneExpansionState.settleToAnchorIfNeeded(800F) }
        }

        rule.runOnIdle {
            assertThat(mockPaneExpansionState.currentMeasuredDraggingOffset)
                .isEqualTo(mockPaneExpansionState.maxExpansionWidth)
        }
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_draggingAndSettlingCloseToLeftEdge() {
        var mockDraggingDp = 0f
        lateinit var mockPaneExpansionState: PaneExpansionState
        lateinit var scope: CoroutineScope

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            scope = rememberCoroutineScope()
            mockPaneExpansionState = rememberPaneExpansionState(anchors = MockPaneExpansionAnchors)
            mockDraggingDp = with(LocalDensity.current) { -360.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle {
            mockPaneExpansionState.dispatchRawDelta(mockDraggingDp)
            scope.launch { mockPaneExpansionState.settleToAnchorIfNeeded(-200F) }
        }

        rule.runOnIdle {
            assertThat(mockPaneExpansionState.currentMeasuredDraggingOffset).isEqualTo(0)
        }
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_draggingAndSettlingCloseToRightEdge() {
        var mockDraggingDp = 0f
        lateinit var mockPaneExpansionState: PaneExpansionState
        lateinit var scope: CoroutineScope

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            scope = rememberCoroutineScope()
            mockPaneExpansionState = rememberPaneExpansionState(anchors = MockPaneExpansionAnchors)
            mockDraggingDp = with(LocalDensity.current) { 640.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle {
            mockPaneExpansionState.dispatchRawDelta(mockDraggingDp)
            scope.launch { mockPaneExpansionState.settleToAnchorIfNeeded(200F) }
        }

        rule.runOnIdle {
            assertThat(mockPaneExpansionState.currentMeasuredDraggingOffset)
                .isEqualTo(mockPaneExpansionState.maxExpansionWidth)
        }
    }
}

private val MockScaffoldDirective = PaneScaffoldDirective.Default

internal const val ThreePaneScaffoldTestTag = "SampleThreePaneScaffold"

private val MockPaneExpansionMiddleAnchor = 400.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockPaneExpansionAnchors =
    listOf(
        PaneExpansionAnchor.Proportion(0f),
        PaneExpansionAnchor.Offset(MockPaneExpansionMiddleAnchor),
        PaneExpansionAnchor.Proportion(1f),
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffold(scaffoldValue: ThreePaneScaffoldValue) {
    SampleThreePaneScaffold(
        MockScaffoldDirective,
        scaffoldValue,
        ListDetailPaneScaffoldDefaults.PaneOrder
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SampleThreePaneScaffold(
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState = PaneExpansionState(),
) {
    ThreePaneScaffold(
        modifier = Modifier.fillMaxSize().testTag(ThreePaneScaffoldTestTag),
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = scaffoldValue,
        paneOrder = paneOrder,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = paneExpansionDragHandle,
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
