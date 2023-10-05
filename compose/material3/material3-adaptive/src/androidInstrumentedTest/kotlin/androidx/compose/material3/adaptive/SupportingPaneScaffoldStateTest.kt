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
class SupportingPaneScaffoldStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_navigateTo_makeFocusPaneExpanded() {
        lateinit var layoutState: SupportingPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.secondary).isEqualTo(PaneAdaptedValue.Hidden)
            layoutState.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.secondary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepFocusPaneExpanded() {
        lateinit var layoutState: SupportingPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = MockDualPaneScaffoldDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            layoutState.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeFocusPaneHidden() {
        lateinit var layoutState: SupportingPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            layoutState.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.secondary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
            layoutState.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.secondary).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceLayoutValueChange_cannotNavigateBack() {
        lateinit var layoutState: SupportingPaneScaffoldState

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialFocusHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(layoutState.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_notEnforceLayoutValueChange_canNavigateBack() {
        lateinit var layoutState: SupportingPaneScaffoldState

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialFocusHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(layoutState.canNavigateBack(false)).isTrue()
            layoutState.navigateBack(false)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneToDualPaneLayout_enforceLayoutValueChange_cannotNavigateBack() {
        lateinit var layoutState: SupportingPaneScaffoldState
        val mockCurrentScaffoldDirective = mutableStateOf(MockSinglePaneScaffoldDirective)

        composeRule.setContent {
            layoutState = rememberSupportingPaneScaffoldState(
                scaffoldDirective = mockCurrentScaffoldDirective.value,
                initialFocusHistory = listOf(
                    SupportingPaneScaffoldRole.Supporting,
                    SupportingPaneScaffoldRole.Main,
                )
            )
        }
        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            // Switches to dual pane
            mockCurrentScaffoldDirective.value = MockDualPaneScaffoldDirective
        }

        composeRule.runOnIdle {
            assertThat(layoutState.canNavigateBack()).isFalse()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockSinglePaneScaffoldDirective = PaneScaffoldDirective(
    maxHorizontalPartitions = 1,
    gutterSizes = GutterSizes(0.dp, 0.dp),
    maxVerticalPartitions = 1,
    excludedBounds = emptyList()
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockDualPaneScaffoldDirective = PaneScaffoldDirective(
    maxHorizontalPartitions = 2,
    gutterSizes = GutterSizes(16.dp, 16.dp),
    maxVerticalPartitions = 1,
    excludedBounds = emptyList()
)
