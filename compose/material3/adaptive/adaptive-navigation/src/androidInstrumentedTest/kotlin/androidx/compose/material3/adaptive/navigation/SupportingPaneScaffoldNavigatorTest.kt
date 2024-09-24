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
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class SupportingPaneScaffoldNavigatorTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun singlePaneLayout_navigateTo_makeDestinationPaneExpanded() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockSinglePaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Hidden)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting, 0)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepDestinationPaneExpanded() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockDualPaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_hideSupportingWhenNotHistoryAware() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            )
                        ),
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    isDestinationHistoryAware = false
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, 1)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_keepSupportingExpandedWhenHistoryAware() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            )
                        ),
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    isDestinationHistoryAware = true
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, 1)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeDestinationPaneHidden() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator<Int>(
                    scaffoldDirective = MockSinglePaneScaffoldDirective
                )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        scope.runBlockingOnIdle {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting, 0)
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_withSimplePop_canNavigateBack() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            assertThat(scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopLatest)).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_canNavigateBack() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, null),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                1
                            ),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
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
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 1),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
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
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, null),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                1
                            ),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_canNavigateBack_withOnlyScaffoldValueChange() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 0),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                        )
                )
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            assertThat(
                    scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
                )
                .isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenHistoryAware_notSkipBackstackEntry() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 1),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                        ),
                    isDestinationHistoryAware = true
                )
        }

        scope.runBlockingOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        scope.runBlockingOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenNotHistoryAware_skipBackstackEntry() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = MockDualPaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 1),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                        ),
                    isDestinationHistoryAware = false
                )
        }

        scope.runBlockingOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        scope.runBlockingOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            )
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isEqualTo(1)
        }
    }

    @Test
    fun singlePaneLayout_previousScaffoldValue_popLatest() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockSinglePaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Hidden)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            SupportingPaneScaffoldRole.Supporting]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            SupportingPaneScaffoldRole.Main]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)

            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            SupportingPaneScaffoldRole.Supporting]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(BackNavigationBehavior.PopLatest)[
                            SupportingPaneScaffoldRole.Main]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)
        }
    }

    @Test
    fun singlePaneLayout_previousScaffoldValue_popUntilScaffoldValueChange() {
        lateinit var scope: CoroutineScope
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scope = rememberCoroutineScope()
            scaffoldNavigator =
                rememberListDetailPaneScaffoldNavigator(
                    scaffoldDirective = MockSinglePaneScaffoldDirective,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                1
                            ),
                        )
                )
        }

        scope.runBlockingOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Hidden)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(
                            BackNavigationBehavior.PopUntilScaffoldValueChange
                        )[SupportingPaneScaffoldRole.Supporting]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(
                            BackNavigationBehavior.PopUntilScaffoldValueChange
                        )[SupportingPaneScaffoldRole.Main]
                )
                .isEqualTo(PaneAdaptedValue.Expanded)

            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting])
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)

            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(
                            BackNavigationBehavior.PopUntilScaffoldValueChange
                        )[SupportingPaneScaffoldRole.Supporting]
                )
                .isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                    scaffoldNavigator
                        .peekPreviousScaffoldValue(
                            BackNavigationBehavior.PopUntilScaffoldValueChange
                        )[SupportingPaneScaffoldRole.Main]
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
                rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = mockCurrentScaffoldDirective.value,
                    initialDestinationHistory =
                        listOf(
                            ThreePaneScaffoldDestinationItem(
                                SupportingPaneScaffoldRole.Supporting,
                                0
                            ),
                            ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                        )
                )
        }
        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Main])
                .isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
            // Switches to dual pane
            mockCurrentScaffoldDirective.value = MockDualPaneScaffoldDirective
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
            assertThat(scaffoldNavigator.currentDestination?.pane)
                .isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.contentKey).isNull()
        }
    }

    private fun CoroutineScope.runBlockingOnIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = composeRule.runOnIdle { launch(block = block) }
        runBlocking { job.join() }
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
    expectedMainPaneAdaptedValue: PaneAdaptedValue,
    expectedSupportingPaneAdaptedValue: PaneAdaptedValue,
    expectedExtraPaneAdaptedValue: PaneAdaptedValue
) {
    assertThat(this[SupportingPaneScaffoldRole.Main]).isEqualTo(expectedMainPaneAdaptedValue)
    assertThat(this[SupportingPaneScaffoldRole.Supporting])
        .isEqualTo(expectedSupportingPaneAdaptedValue)
    assertThat(this[SupportingPaneScaffoldRole.Extra]).isEqualTo(expectedExtraPaneAdaptedValue)
}
