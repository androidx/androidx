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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ThreePaneScaffoldScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3_ADAPTIVE)

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard() {
        rule.setContent {
            SampleThreePaneScaffoldStandardMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense() {
        rule.setContent {
            SampleThreePaneScaffoldDenseMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard_medium_size_window() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 700.dp,
            simulatedHeight = 500.dp
        ) {
            SampleThreePaneScaffoldStandardMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard_medium")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense_medium_size_window() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 700.dp,
            simulatedHeight = 500.dp
        ) {
            SampleThreePaneScaffoldDenseMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense_medium")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_standard_expanded_size_window() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            SampleThreePaneScaffoldStandardMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_standard_expanded")
    }

    @Test
    fun threePaneScaffold_listDetailPaneOrder_dense_expanded_size_window() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            SampleThreePaneScaffoldDenseMode()
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_listDetail_dense_expanded")
    }

    @Test
    fun threePaneScaffold_insets_compact_size_window() {
        val mockInsets = WindowInsets(100.dp, 10.dp, 20.dp, 50.dp)
        rule.setContent {
            SampleThreePaneScaffoldWithInsets(mockInsets)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_insets_compact")
    }

    @Test
    fun threePaneScaffold_insets_medium_size_window() {
        val mockInsets = WindowInsets(100.dp, 10.dp, 20.dp, 50.dp)
        rule.setContentWithSimulatedSize(
            simulatedWidth = 700.dp,
            simulatedHeight = 500.dp
        ) {
            SampleThreePaneScaffoldWithInsets(mockInsets)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_insets_medium")
    }

    @Test
    fun threePaneScaffold_insets_expanded_size_window() {
        val mockInsets = WindowInsets(100.dp, 10.dp, 20.dp, 50.dp)
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            SampleThreePaneScaffoldWithInsets(mockInsets)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "threePaneScaffold_insets_expanded")
    }

    @Test
    fun threePaneScaffold_paneExpansion_fixedFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = with(LocalDensity.current) {
                412.dp.roundToPx()
            }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fixedFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_zeroFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = 0
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_zeroFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_overflowFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = with(LocalDensity.current) {
                1024.dp.roundToPx()
            }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_overflowFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_fixedFirstPanePercentage() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPanePercentage = 0.5f
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fixedFirstPanePercentage"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_zeroFirstPanePercentage() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPanePercentage = 0f
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_zeroFirstPanePercentage"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_smallFirstPanePercentage() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPanePercentage = 0.05f
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_smallFirstPanePercentage"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_largeFirstPanePercentage() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPanePercentage = 0.95f
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_largeFirstPanePercentage"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansion_fullFirstPanePercentage() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPanePercentage = 1.0f
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState)
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansion_fullFirstPanePercentage"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_fixedFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = with(LocalDensity.current) {
                412.dp.roundToPx()
            }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) {
                MockDragHandle()
            }
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_fixedFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_zeroFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = 0
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) {
                MockDragHandle()
            }
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_zeroFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionWithDragHandle_overflowFirstPaneWidth() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            val mockPaneExpansionState = PaneExpansionState()
            mockPaneExpansionState.firstPaneWidth = with(LocalDensity.current) {
                1024.dp.roundToPx()
            }
            SampleThreePaneScaffoldWithPaneExpansion(mockPaneExpansionState) {
                MockDragHandle()
            }
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionWithDragHandle_overflowFirstPaneWidth"
            )
    }

    @Test
    fun threePaneScaffold_paneExpansionDragHandle_unspecifiedExpansionState() {
        rule.setContentWithSimulatedSize(
            simulatedWidth = 1024.dp,
            simulatedHeight = 800.dp
        ) {
            SampleThreePaneScaffoldWithPaneExpansion(PaneExpansionState()) {
                MockDragHandle()
            }
        }

        rule.onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "threePaneScaffold_paneExpansionDragHandle_unspecifiedExpansionState"
            )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldStandardMode() {
    val scaffoldDirective = calculateStandardPaneScaffoldDirective(
        currentWindowAdaptiveInfo()
    )
    val scaffoldValue = calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        ThreePaneScaffoldDefaults.adaptStrategies(),
        null
    )
    SampleThreePaneScaffold(
        scaffoldDirective,
        scaffoldValue,
        ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldDenseMode() {
    val scaffoldDirective = calculateDensePaneScaffoldDirective(
        currentWindowAdaptiveInfo()
    )
    val scaffoldValue = calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        ThreePaneScaffoldDefaults.adaptStrategies(),
        null
    )
    SampleThreePaneScaffold(
        scaffoldDirective,
        scaffoldValue,
        ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldWithInsets(
    windowInsets: WindowInsets
) {
    val scaffoldDirective = calculateStandardPaneScaffoldDirective(
        currentWindowAdaptiveInfo()
    )
    val scaffoldValue = calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        ThreePaneScaffoldDefaults.adaptStrategies(),
        null
    )
    SampleThreePaneScaffold(
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = scaffoldValue,
        paneOrder = ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
        windowInsets = windowInsets
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffoldWithPaneExpansion(
    paneExpansionState: PaneExpansionState,
    paneExpansionDragHandle: (@Composable (PaneExpansionState) -> Unit)? = null,
) {
    val scaffoldDirective = calculateStandardPaneScaffoldDirective(
        currentWindowAdaptiveInfo()
    )
    val scaffoldValue = calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        ThreePaneScaffoldDefaults.adaptStrategies(),
        null
    )
    SampleThreePaneScaffold(
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = scaffoldValue,
        paneOrder = ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = paneExpansionDragHandle
    )
}

@Composable
private fun MockDragHandle() {
    Box(
        modifier = Modifier
            .size(
                4.dp, 48.dp
            )
            .graphicsLayer(
                shape = CircleShape,
                clip = true
            )
            .background(MaterialTheme.colorScheme.outline)
    )
}
