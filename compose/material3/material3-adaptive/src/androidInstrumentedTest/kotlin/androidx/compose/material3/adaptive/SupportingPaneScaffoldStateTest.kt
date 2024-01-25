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

package androidx.compose.material3.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class SupportingPaneScaffoldStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_mainPaneExpandedByDefault() {
        lateinit var scaffoldState: ThreePaneScaffoldState

        composeRule.setContent {
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldState.scaffoldValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldState.scaffoldValue.secondary).isEqualTo(PaneAdaptedValue.Hidden)
        }
    }

    @Test
    fun dualPaneLayout_mainAndSupportingPaneExpandedByDefault() {
        lateinit var scaffoldState: ThreePaneScaffoldState

        composeRule.setContent {
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = MockDualPaneScaffoldDirective
            )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldState.scaffoldValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldState.scaffoldValue.secondary).isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneLayout_paneDestinationExpanded() {
        lateinit var scaffoldState: ThreePaneScaffoldState

        composeRule.setContent {
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = MockSinglePaneScaffoldDirective,
                currentDestination =
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, null)
            )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldState.scaffoldValue.primary).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldState.scaffoldValue.secondary).isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun dualPaneLayout_paneDestinationExpanded() {
        lateinit var scaffoldState: ThreePaneScaffoldState

        composeRule.setContent {
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                currentDestination =
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, null)
            )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldState.scaffoldValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldState.scaffoldValue.secondary).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldState.scaffoldValue.tertiary).isEqualTo(PaneAdaptedValue.Expanded)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockSinglePaneScaffoldDirective = PaneScaffoldDirective(
    contentPadding = PaddingValues(0.dp),
    maxHorizontalPartitions = 1,
    horizontalPartitionSpacerSize = 0.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = emptyList()
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockDualPaneScaffoldDirective = PaneScaffoldDirective(
    contentPadding = PaddingValues(16.dp),
    maxHorizontalPartitions = 2,
    horizontalPartitionSpacerSize = 16.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 16.dp,
    excludedBounds = emptyList()
)
