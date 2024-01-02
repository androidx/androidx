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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.properties.Delegates
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class SupportingPaneScaffoldNavigatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_navigateTo_makeFocusPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.secondary
            ).isEqualTo(PaneAdaptedValue.Hidden)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.secondary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepFocusPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.primary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.primary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeFocusPaneHidden() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.secondary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.secondary
            ).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_notEnforceScaffoldValueChange_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.primary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.canNavigateBack(false)).isTrue()
            scaffoldNavigator.navigateBack(false)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.primary
            ).isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneToDualPaneLayout_enforceScaffoldValueChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator
        val mockCurrentScaffoldDirective = mutableStateOf(MockSinglePaneScaffoldDirective)

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = mockCurrentScaffoldDirective.value,
                initialDestinationHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }
        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue.primary
            ).isEqualTo(PaneAdaptedValue.Expanded)
            // Switches to dual pane
            mockCurrentScaffoldDirective.value = MockDualPaneScaffoldDirective
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
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
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = emptyList()
)
