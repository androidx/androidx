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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ThreePaneScaffoldScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3_ADAPTIVE)

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard() {
        rule.setContent { SampleThreePaneScaffoldStandardMode() }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense() {
        rule.setContent { SampleThreePaneScaffoldDenseMode() }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard_medium_size_window() {
        rule.setContentWithSimulatedSize(simulatedWidth = 700.dp, simulatedHeight = 500.dp) {
            SampleThreePaneScaffoldStandardMode()
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard_medium")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense_medium_size_window() {
        rule.setContentWithSimulatedSize(simulatedWidth = 700.dp, simulatedHeight = 500.dp) {
            SampleThreePaneScaffoldDenseMode()
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense_medium")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard_expanded_size_window() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            SampleThreePaneScaffoldStandardMode()
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard_expanded")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense_expanded_size_window() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            SampleThreePaneScaffoldDenseMode()
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense_expanded")
    }

    @Test
    fun threePaneScaffold_scaffoldStateTransitionFraction_0percent() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val detailExtraExpanded =
                ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Hidden,
                    tertiary = PaneAdaptedValue.Expanded,
                )
            val listDetailExpanded =
                ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Expanded,
                    tertiary = PaneAdaptedValue.Hidden,
                )
            val scaffoldState = remember { MutableThreePaneScaffoldState(detailExtraExpanded) }
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0f, listDetailExpanded, isPredictiveBackInProgress = true)
            }
            SampleThreePaneScaffoldWithScaffoldState(scaffoldState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_scaffoldStateTransitionFraction_0percent",
            )
    }

    @Test
    fun threePaneScaffold_scaffoldStateTransitionFraction_10percent() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val detailExtraExpanded =
                ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Hidden,
                    tertiary = PaneAdaptedValue.Expanded,
                )
            val listDetailExpanded =
                ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Expanded,
                    tertiary = PaneAdaptedValue.Hidden,
                )
            val scaffoldState = remember { MutableThreePaneScaffoldState(detailExtraExpanded) }
            LaunchedEffect(Unit) {
                scaffoldState.seekTo(0.1f, listDetailExpanded, isPredictiveBackInProgress = true)
            }
            SampleThreePaneScaffoldWithScaffoldState(scaffoldState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_scaffoldStateTransitionFraction_10percent",
            )
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_withReflowedPane() {
        rule.setContent {
            SampleThreePaneScaffoldStandardMode(
                overrideScaffoldValue =
                    ThreePaneScaffoldValue(
                        primary = PaneAdaptedValue.Expanded,
                        secondary = PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Primary),
                        tertiary = PaneAdaptedValue.Hidden,
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_listDetailPaneOrder_withReflowedPane",
            )
    }

    @Test
    fun threePaneScaffold_singlePaneLayout_withLevitatedPane() {
        rule.setContent {
            SampleThreePaneScaffoldStandardMode(
                overrideScaffoldValue =
                    ThreePaneScaffoldValue(
                        primary = PaneAdaptedValue.Expanded,
                        secondary = PaneAdaptedValue.Levitated(alignment = Alignment.Center),
                        tertiary = PaneAdaptedValue.Hidden,
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_singlePaneLayout_withLevitatedPane",
            )
    }

    @Test
    fun threePaneScaffold_dualPaneLayout_withLevitatedPaneWithScrim() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            SampleThreePaneScaffoldStandardMode(
                overrideScaffoldValue =
                    ThreePaneScaffoldValue(
                        primary = PaneAdaptedValue.Expanded,
                        secondary =
                            PaneAdaptedValue.Levitated(
                                alignment = Alignment.Center,
                                scrim = { LevitatedPaneScrim() },
                            ),
                        tertiary = PaneAdaptedValue.Hidden,
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_dualPaneLayout_withLevitatedPaneWithScrim",
            )
    }

    @Test
    fun threePaneScaffold_singlePaneLayout_withLevitatedBottomAligned() {
        rule.setContent {
            SampleThreePaneScaffoldStandardMode(
                overrideScaffoldValue =
                    ThreePaneScaffoldValue(
                        primary = PaneAdaptedValue.Expanded,
                        secondary = PaneAdaptedValue.Levitated(alignment = Alignment.BottomCenter),
                        tertiary = PaneAdaptedValue.Hidden,
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_singlePaneLayout_withLevitatedBottomAligned",
            )
    }

    @Test
    fun threePaneScaffold_dualPaneLayout_withLevitatedPaneStartAligned() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            SampleThreePaneScaffoldStandardMode(
                overrideScaffoldValue =
                    ThreePaneScaffoldValue(
                        primary = PaneAdaptedValue.Expanded,
                        secondary = PaneAdaptedValue.Levitated(alignment = Alignment.CenterStart),
                        tertiary = PaneAdaptedValue.Expanded,
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_dualPaneLayout_withLevitatedPaneStartAligned",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_fixedFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(
                with(LocalDensity.current) { 412.dp.roundToPx() }
            )
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fixedFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_zeroFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(0)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_zeroFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_overflowFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(
                with(LocalDensity.current) { 1024.dp.roundToPx() }
            )
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_overflowFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_fixedFirstPanePercentage() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneProportion(0.5f)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fixedFirstPanePercentage",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_zeroFirstPanePercentage() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneProportion(0f)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_zeroFirstPanePercentage",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_smallFirstPanePercentage() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneProportion(0.05f)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_smallFirstPanePercentage",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_largeFirstPanePercentage() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneProportion(0.95f)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_largeFirstPanePercentage",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_fullFirstPanePercentage() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneProportion(1f)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fullFirstPanePercentage",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_disabledOnSinglePane() {
        rule.setContent {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(
                with(LocalDensity.current) { 412.dp.roundToPx() }
            )
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_disabledOnSinglePane",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_fixedFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(
                with(LocalDensity.current) { 412.dp.roundToPx() }
            )
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_fixedFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_zeroFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(0)
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_zeroFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_overflowFirstPaneWidth() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.setFirstPaneWidth(
                with(LocalDensity.current) { 1024.dp.roundToPx() }
            )
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_overflowFirstPaneWidth",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionDragHandle_unspecifiedExpansionState() {
        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            SampleThreePaneScaffoldWithPaneExpansion(PaneExpansionState()) { MockDragHandle(it) }
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionDragHandle_unspecifiedExpansionState",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_dragging() {
        val mockPaneExpansionState = PaneExpansionState()
        var mockDraggingDp = 0f

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            mockDraggingDp = with(LocalDensity.current) { 200.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle { mockPaneExpansionState.draggableState.dispatchRawDelta(mockDraggingDp) }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_dragging",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_draggingCloseToLeftEdge() {
        val mockPaneExpansionState = PaneExpansionState()
        var mockDraggingDp = 0f

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            mockDraggingDp = with(LocalDensity.current) { -360.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle { mockPaneExpansionState.draggableState.dispatchRawDelta(mockDraggingDp) }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_draggingCloseToLeftEdge",
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_draggingCloseToRightEdge() {
        val mockPaneExpansionState = PaneExpansionState()
        var mockDraggingDp = 0f

        rule.setContentWithSimulatedSize(simulatedWidth = 1024.dp, simulatedHeight = 800.dp) {
            mockDraggingDp = with(LocalDensity.current) { 640.dp.toPx() }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) { MockDragHandle(it) }
        }

        rule.runOnIdle { mockPaneExpansionState.draggableState.dispatchRawDelta(mockDraggingDp) }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_draggingCloseToRightEdge",
            )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldStandardMode(
    overrideScaffoldValue: ThreePaneScaffoldValue? = null
) {
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val scaffoldValue =
        overrideScaffoldValue
            ?: calculateThreePaneScaffoldValue(
                scaffoldDirective.maxHorizontalPartitions,
                ThreePaneScaffoldDefaults.adaptStrategies(),
                null,
            )
    SampleThreePaneScaffold(
        scaffoldDirective,
        scaffoldValue,
        ListDetailPaneScaffoldDefaults.PaneOrder,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldDenseMode() {
    val scaffoldDirective =
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfo())
    val scaffoldValue =
        calculateThreePaneScaffoldValue(
            scaffoldDirective.maxHorizontalPartitions,
            ThreePaneScaffoldDefaults.adaptStrategies(),
            null,
        )
    SampleThreePaneScaffold(
        scaffoldDirective,
        scaffoldValue,
        ListDetailPaneScaffoldDefaults.PaneOrder,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldWithScaffoldState(scaffoldState: ThreePaneScaffoldState) {
    val scaffoldDirective =
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfo())
    SampleThreePaneScaffold(
        scaffoldDirective,
        scaffoldState,
        ListDetailPaneScaffoldDefaults.PaneOrder,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SampleThreePaneScaffoldWithPaneExpansion(
    paneExpansionState: PaneExpansionState,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
) {
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val scaffoldValue =
        calculateThreePaneScaffoldValue(
            scaffoldDirective.maxHorizontalPartitions,
            ThreePaneScaffoldDefaults.adaptStrategies(),
            null,
        )
    SampleThreePaneScaffold(
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = scaffoldValue,
        paneOrder = ListDetailPaneScaffoldDefaults.PaneOrder,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = paneExpansionDragHandle,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldScope.MockDragHandle(state: PaneExpansionState) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier =
            Modifier.paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource,
            ),
        interactionSource = interactionSource,
    )
}
