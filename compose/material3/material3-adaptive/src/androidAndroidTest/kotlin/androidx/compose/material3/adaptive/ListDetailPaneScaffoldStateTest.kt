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
class ListDetailPaneScaffoldStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_navigateTo_makeFocusPaneExpanded() {
        lateinit var layoutState: ListDetailPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = MockSinglePaneLayoutDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Hidden)
            layoutState.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepFocusPaneExpanded() {
        lateinit var layoutState: ListDetailPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = MockDualPaneLayoutDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            layoutState.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeFocusPaneHidden() {
        lateinit var layoutState: ListDetailPaneScaffoldState
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = MockSinglePaneLayoutDirective
            )
            canNavigateBack = layoutState.canNavigateBack()
        }

        composeRule.runOnIdle {
            layoutState.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(canNavigateBack).isTrue()
            layoutState.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceLayoutValueChange_cannotNavigateBack() {
        lateinit var layoutState: ListDetailPaneScaffoldState

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = MockDualPaneLayoutDirective,
                initialFocusHistory = listOf(
                    ListDetailPaneScaffoldRole.List,
                    ListDetailPaneScaffoldRole.Detail,
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(layoutState.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_notEnforceLayoutValueChange_canNavigateBack() {
        lateinit var layoutState: ListDetailPaneScaffoldState

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = MockDualPaneLayoutDirective,
                initialFocusHistory = listOf(
                    ListDetailPaneScaffoldRole.List,
                    ListDetailPaneScaffoldRole.Detail,
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
        lateinit var layoutState: ListDetailPaneScaffoldState
        val mockCurrentLayoutDirective = mutableStateOf(MockSinglePaneLayoutDirective)

        composeRule.setContent {
            layoutState = rememberListDetailPaneScaffoldState(
                layoutDirectives = mockCurrentLayoutDirective.value,
                initialFocusHistory = listOf(
                    ListDetailPaneScaffoldRole.List,
                    ListDetailPaneScaffoldRole.Detail,
                )
            )
        }
        composeRule.runOnIdle {
            assertThat(layoutState.layoutValue.primary).isEqualTo(PaneAdaptedValue.Expanded)
            // Switches to dual pane
            mockCurrentLayoutDirective.value = MockDualPaneLayoutDirective
        }

        composeRule.runOnIdle {
            assertThat(layoutState.canNavigateBack()).isFalse()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockSinglePaneLayoutDirective = AdaptiveLayoutDirective(
    maxHorizontalPartitions = 1,
    gutterSizes = GutterSizes(0.dp, 0.dp)
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockDualPaneLayoutDirective = AdaptiveLayoutDirective(
    maxHorizontalPartitions = 2,
    gutterSizes = GutterSizes(16.dp, 16.dp)
)
