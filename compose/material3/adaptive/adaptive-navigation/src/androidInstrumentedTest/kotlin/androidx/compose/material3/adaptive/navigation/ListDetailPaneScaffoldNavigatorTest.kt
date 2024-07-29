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

package androidx.compose.material3.adaptive.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
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
class ListDetailPaneScaffoldNavigatorTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_navigateTo_makeDestinationPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockSinglePaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Hidden)
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepDestinationPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockDualPaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_hideListWhenNotHistoryAware() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    isDestinationHistoryAware = false
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra, 0)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_keepListExpandedWhenHistoryAware() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    isDestinationHistoryAware = true
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra, 0)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeDestinationPaneHidden() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockSinglePaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle { scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0) }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldValueChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_withSimplePop_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopLatest)).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 1),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                    scaffoldNavigator.canNavigateBack(
                        BackNavigationBehavior.PopUntilCurrentDestinationChange
                    )
                )
                .isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilCurrentDestinationChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 1),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                    scaffoldNavigator.canNavigateBack(
                        BackNavigationBehavior.PopUntilCurrentDestinationChange
                    )
                )
                .isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 1),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_canNavigateBack_withOnlyScaffoldValueChange() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Extra, 0),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenHistoryAware_notSkipBackstackEntry() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Extra, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                        ),
                    isDestinationHistoryAware = true
                )
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0)
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenNotHistoryAware_skipBackstackEntry() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Extra, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                        ),
                    isDestinationHistoryAware = false
                )
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.List)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 0)
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun singlePaneLayout_previousScaffoldValue_popLatest() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockSinglePaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Hidden)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            ListDetailPaneScaffoldRole.Detail]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            ListDetailPaneScaffoldRole.List]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)

            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Expanded)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            ListDetailPaneScaffoldRole.Detail]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            ListDetailPaneScaffoldRole.List]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneLayout_previousScaffoldValue_popUntilScaffoldValueChange() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockSinglePaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 1),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Hidden)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopUntilScaffoldValueChange)[
                            ListDetailPaneScaffoldRole.Detail]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopUntilScaffoldValueChange)[
                            ListDetailPaneScaffoldRole.List]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)

            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List])
                .isEqualTo(PaneAdaptedValue.Expanded)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopUntilScaffoldValueChange)[
                            ListDetailPaneScaffoldRole.Detail]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopUntilScaffoldValueChange)[
                            ListDetailPaneScaffoldRole.List]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneToDualPaneLayout_enforceScaffoldValueChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        val mockCurrentScaffoldDirective = mutableStateOf(MockSinglePaneScaffoldDirective)

        composeRule.setContent {
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = mockCurrentScaffoldDirective.value,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
                            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, 0),
                        )
                )
        }
        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            // Switches to dual pane
            mockCurrentScaffoldDirective.value = MockDualPaneScaffoldDirective
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(ListDetailPaneScaffoldRole.Detail)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }
}

private val MockSinglePaneScaffoldDirective = PaneScaffoldDirective.Default

private val MockDualPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 16.dp,
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ThreePaneScaffoldValue.assert(
    expectedDetailPaneAdaptedValue: PaneAdaptedValue,
    expectedListPaneAdaptedValue: PaneAdaptedValue,
    expectedExtraPaneAdaptedValue: PaneAdaptedValue
) {
    assertThat(this[ListDetailPaneScaffoldRole.Detail]).isEqualTo(expectedDetailPaneAdaptedValue)
    assertThat(this[ListDetailPaneScaffoldRole.List]).isEqualTo(expectedListPaneAdaptedValue)
    assertThat(this[ListDetailPaneScaffoldRole.Extra]).isEqualTo(expectedExtraPaneAdaptedValue)
}
