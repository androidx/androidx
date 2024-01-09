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
    fun singlePaneLayout_navigateTo_makeDestinationPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Hidden)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting, 0)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateTo_keepDestinationPaneExpanded() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Main]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Main]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_hideSupportingWhenNotHistoryAware() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0)
                ),
                scaffoldDirective = MockDualPaneScaffoldDirective,
                isDestinationHistoryAware = false
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, 1)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun dualPaneLayout_navigateToExtra_keepSupportingExpandedWhenHistoryAware() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0)
                ),
                scaffoldDirective = MockDualPaneScaffoldDirective,
                isDestinationHistoryAware = true
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, 1)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(canNavigateBack).isTrue()
        }
    }

    @Test
    fun singlePaneLayout_navigateBack_makeDestinationPaneHidden() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        var canNavigateBack by Delegates.notNull<Boolean>()

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockSinglePaneScaffoldDirective
            )
            canNavigateBack = scaffoldNavigator.canNavigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting, 0)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(canNavigateBack).isTrue()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Supporting]
            ).isEqualTo(PaneAdaptedValue.Hidden)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            assertThat(canNavigateBack).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_withSimplePop_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Main]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            assertThat(scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopLatest)).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Main]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, null),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 1),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                scaffoldNavigator.canNavigateBack(
                    BackNavigationBehavior.PopUntilCurrentDestinationChange
                )
            ).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilCurrentDestinationChange)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
        }
    }

    @Test
    fun dualPaneLayout_enforceCurrentDestinationChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 1),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                scaffoldNavigator.canNavigateBack(
                    BackNavigationBehavior.PopUntilCurrentDestinationChange
                )
            ).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_canNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, null),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 1),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
            assertThat(
                scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
            ).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_canNavigateBack_withOnlyScaffoldValueChange() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 0),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(
                scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
            ).isTrue()
            scaffoldNavigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceContentChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                )
            )
        }

        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            assertThat(
                scaffoldNavigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)
            ).isFalse()
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenHistoryAware_notSkipBackstackEntry() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 1),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                ),
                isDestinationHistoryAware = true
            )
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
        }
    }

    @Test
    fun dualPaneLayout_enforceScaffoldChangeWhenNotHistoryAware_skipBackstackEntry() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = MockDualPaneScaffoldDirective,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Extra, 1),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                ),
                isDestinationHistoryAware = false
            )
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Supporting)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(0)
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Main)
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            scaffoldNavigator.navigateBack()
        }

        composeRule.runOnIdle {
            scaffoldNavigator.scaffoldState.scaffoldValue.assert(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            )
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Extra)
            assertThat(scaffoldNavigator.currentDestination?.content).isEqualTo(1)
        }
    }

    @Test
    fun singlePaneToDualPaneLayout_enforceScaffoldValueChange_cannotNavigateBack() {
        lateinit var scaffoldNavigator: ThreePaneScaffoldNavigator<Int>
        val mockCurrentScaffoldDirective = mutableStateOf(MockSinglePaneScaffoldDirective)

        composeRule.setContent {
            scaffoldNavigator = rememberSupportingPaneScaffoldNavigator(
                scaffoldDirective = mockCurrentScaffoldDirective.value,
                initialDestinationHistory = listOf(
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Supporting, 0),
                    ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main),
                )
            )
        }
        composeRule.runOnIdle {
            assertThat(
                scaffoldNavigator.scaffoldState.scaffoldValue[SupportingPaneScaffoldRole.Main]
            ).isEqualTo(PaneAdaptedValue.Expanded)
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
            // Switches to dual pane
            mockCurrentScaffoldDirective.value = MockDualPaneScaffoldDirective
        }

        composeRule.runOnIdle {
            assertThat(scaffoldNavigator.canNavigateBack()).isFalse()
            assertThat(
                scaffoldNavigator.currentDestination?.pane
            ).isEqualTo(SupportingPaneScaffoldRole.Main)
            assertThat(scaffoldNavigator.currentDestination?.content).isNull()
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ThreePaneScaffoldValue.assert(
    expectedMainPaneAdaptedValue: PaneAdaptedValue,
    expectedSupportingPaneAdaptedValue: PaneAdaptedValue,
    expectedExtraPaneAdaptedValue: PaneAdaptedValue
) {
    assertThat(this[SupportingPaneScaffoldRole.Main]).isEqualTo(expectedMainPaneAdaptedValue)
    assertThat(this[SupportingPaneScaffoldRole.Supporting]).isEqualTo(
        expectedSupportingPaneAdaptedValue
    )
    assertThat(this[SupportingPaneScaffoldRole.Extra]).isEqualTo(expectedExtraPaneAdaptedValue)
}
