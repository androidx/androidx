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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class ThreePaneScaffoldValueTest {
    @Test
    fun test_onePaneLayoutNoFocus() {
        val scaffoldState = calculateThreePaneScaffoldValueInternal(
            maxHorizontalPartitions = 1,
            adaptStrategies = MockAdaptStrategies
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRoleInternal.Secondary,
            SecondaryPaneAdaptedState
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun test_onePaneLayoutWithFocus() {
        val scaffoldState = calculateThreePaneScaffoldValueInternal(
            maxHorizontalPartitions = 1,
            adaptStrategies = MockAdaptStrategies,
            currentDestination = ThreePaneScaffoldRoleInternal.Secondary
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Primary, PrimaryPaneAdaptedState)
        scaffoldState.assertState(
            ThreePaneScaffoldRoleInternal.Secondary,
            PaneAdaptedValue.Expanded)
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun test_twoPaneLayoutNoFocus() {
        val scaffoldState = calculateThreePaneScaffoldValueInternal(
            maxHorizontalPartitions = 2,
            adaptStrategies = MockAdaptStrategies
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRoleInternal.Secondary, PaneAdaptedValue.Expanded
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Tertiary, TertiaryPaneAdaptedState)
    }

    @Test
    fun test_twoPaneLayoutWithFocus() {
        val scaffoldState = calculateThreePaneScaffoldValueInternal(
            maxHorizontalPartitions = 2,
            adaptStrategies = MockAdaptStrategies,
            currentDestination = ThreePaneScaffoldRoleInternal.Tertiary
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Primary, PaneAdaptedValue.Expanded)
        scaffoldState.assertState(
            ThreePaneScaffoldRoleInternal.Secondary, SecondaryPaneAdaptedState
        )
        scaffoldState.assertState(ThreePaneScaffoldRoleInternal.Tertiary, PaneAdaptedValue.Expanded)
    }

    private fun ThreePaneScaffoldValue.assertState(
        role: ThreePaneScaffoldRoleInternal,
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
        private val MockAdaptStrategies = ThreePaneScaffoldAdaptStrategies(
            PrimaryPaneAdaptStrategy,
            SecondaryPaneAdaptStrategy,
            TertiaryPaneAdaptStrategy
        )
    }
}
