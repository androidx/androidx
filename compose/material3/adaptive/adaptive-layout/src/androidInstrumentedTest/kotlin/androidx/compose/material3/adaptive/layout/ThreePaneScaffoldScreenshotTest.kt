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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
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
        scaffoldDirective,
        scaffoldValue,
        ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
        windowInsets
    )
}
