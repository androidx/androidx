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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.Alignment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class ThreePaneScaffoldValueTest {
    @Test
    fun calculateWithoutHistory_onePaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies = MockAdaptStrategies,
                currentDestination = null,
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, SecondaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithHistory_onePaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies = MockAdaptStrategies,
                destinationHistory = emptyList(),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, SecondaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithoutHistory_onePaneLayout() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies = MockAdaptStrategies,
                currentDestination =
                    ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PrimaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithHistory_onePaneLayout() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies = MockAdaptStrategies,
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PrimaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithoutHistory_twoPaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies = MockAdaptStrategies,
                currentDestination = null,
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithHistory_twoPaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies = MockAdaptStrategies,
                destinationHistory = emptyList(),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun calculateWithoutHistory_twoPaneLayout() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies = MockAdaptStrategies,
                currentDestination =
                    ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, SecondaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithHistory_twoPaneLayout() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies = MockAdaptStrategies,
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PrimaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithHistory_twoPaneLayout_longHistory() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies = MockAdaptStrategies,
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, SecondaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithReflow_onePaneLayout_reflowCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Tertiary),
                        AdaptStrategy.Hide,
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Secondary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Secondary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Tertiary),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithReflow_onePaneLayout_currentDestinationIsReflowAnchor() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Secondary),
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Secondary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Tertiary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Secondary),
        )
    }

    @Test
    fun calculateWithReflow_onePaneLayout_currentDestinationIsNotReflowAnchor() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Secondary),
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Primary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithReflowAndHistory_onePaneLayout_reflowCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Secondary),
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Tertiary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Secondary),
        )
    }

    @Test
    fun calculateWithReflowAndHistory_onePaneLayout_expandedPaneIsReflowAnchor() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Primary),
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Tertiary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Primary),
        )
    }

    @Test
    fun calculateWithReflowAndHistory_onePaneLayout_expandedPaneIsNotReflowAnchor() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Secondary),
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithReflow_onePaneLayout_reflowPrimaryPaneWhenNoCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Tertiary),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination = null,
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Tertiary),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithReflow_onePaneLayoutWithOneVerticalPartition_neverReflow() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 1,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Tertiary),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination = null,
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithReflow_twoPaneLayout_neverReflow() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Primary),
                        AdaptStrategy.Hide,
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithReflow_threePaneLayout_neverReflow() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 3,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(ThreePaneScaffoldRole.Primary),
                        AdaptStrategy.Hide,
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitate_onePaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination = null,
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithLevitate_onePaneLayout_levitateCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.TopCenter),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Primary),
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Levitated(Alignment.TopCenter),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithLevitate_onePaneLayout_currentDestinationNotLevitated() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.TopCenter),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Secondary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithLevitate_twoPaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination = null,
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitate_twoPaneLayout_levitateCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.BottomEnd),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Primary),
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Levitated(Alignment.BottomEnd),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitate_twoPaneLayout_currentDestinationNotLevitated() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.BottomEnd),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Secondary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitateAndHistory_onePaneLayout_levitateCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.BottomEnd),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                    ),
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Levitated(Alignment.BottomEnd),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitateAndHistory_twoPaneLayout_currentDestinationNotLevitated() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.BottomEnd),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Hide,
                    ),
                destinationHistory =
                    listOf(
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, null),
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null),
                    ),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    @Test
    fun calculateWithLevitateAndReflow_onePaneLayout_levitateCurrentDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.TopCenter),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(reflowUnder = ThreePaneScaffoldRole.Secondary),
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Primary),
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Levitated(Alignment.TopCenter),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Tertiary,
            PaneAdaptedValue.Reflowed(ThreePaneScaffoldRole.Secondary),
        )
    }

    @Test
    fun calculateWithLevitateAndReflow_onePaneLayout_noReflowToLevitatedPane() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                maxVerticalPartitions = 2,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.TopCenter),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(reflowUnder = ThreePaneScaffoldRole.Primary),
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Primary),
            )
        scaffoldState.assertState(
            ThreePaneScaffoldRole.Primary,
            PaneAdaptedValue.Levitated(Alignment.TopCenter),
        )
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    @Test
    fun calculateWithLevitateAndReflow_onePaneLayout_noReflowToNotLevitatedPane() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies =
                    ThreePaneScaffoldAdaptStrategies(
                        AdaptStrategy.Levitate(alignment = Alignment.TopCenter),
                        AdaptStrategy.Hide,
                        AdaptStrategy.Reflow(reflowUnder = ThreePaneScaffoldRole.Primary),
                    ),
                currentDestination =
                    ThreePaneScaffoldDestinationItem<Any>(ThreePaneScaffoldRole.Secondary),
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Hidden)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Hidden)
    }

    private fun ThreePaneScaffoldValue.assertState(
        role: ThreePaneScaffoldRole,
        state: PaneAdaptedValue,
    ) {
        assertThat(this[role]).isEqualTo(state)
    }

    companion object {
        private val PrimaryPaneAdaptStrategy = AdaptStrategy.Hide
        private val SecondaryPaneAdaptStrategy = AdaptStrategy.Hide
        private val TertiaryPaneAdaptStrategy = AdaptStrategy.Hide
        private val PrimaryPaneAdaptedState = PaneAdaptedValue.Hidden
        private val SecondaryPaneAdaptedState = PaneAdaptedValue.Hidden
        private val TertiaryPaneAdaptedState = PaneAdaptedValue.Hidden
        private val MockAdaptStrategies =
            ThreePaneScaffoldAdaptStrategies(
                PrimaryPaneAdaptStrategy,
                SecondaryPaneAdaptStrategy,
                TertiaryPaneAdaptStrategy,
            )
    }
}
