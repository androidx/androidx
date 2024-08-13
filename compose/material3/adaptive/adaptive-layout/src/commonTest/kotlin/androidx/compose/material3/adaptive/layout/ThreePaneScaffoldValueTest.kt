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
import androidx.kruth.assertThat
import kotlin.test.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ThreePaneScaffoldValueTest {
    @Test
    fun calculateWithoutHistory_onePaneLayout_noDestination() {
        val scaffoldState =
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = 1,
                adaptStrategies = MockAdaptStrategies,
                currentDestination = null
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
                destinationHistory = emptyList()
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
                    ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null)
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
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null)
                    )
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
                currentDestination = null
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
                destinationHistory = emptyList()
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
                    ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null)
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
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Secondary, null)
                    )
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
                        ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Tertiary, null)
                    )
            )
        scaffoldState.assertState(ThreePaneScaffoldRole.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRole.Secondary, SecondaryPaneAdaptedState)
        scaffoldState.assertState(ThreePaneScaffoldRole.Tertiary, PaneAdaptedValue.Expanded)
    }

    private fun ThreePaneScaffoldValue.assertState(
        role: ThreePaneScaffoldRole,
        state: PaneAdaptedValue
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
                TertiaryPaneAdaptStrategy
            )
    }
}
