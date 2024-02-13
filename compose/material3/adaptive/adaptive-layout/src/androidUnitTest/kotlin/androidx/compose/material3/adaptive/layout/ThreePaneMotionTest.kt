/*
 * Copyright 2024 The Android Open Source Project
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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class ThreePaneMotionTest {
    @Test
    fun noPane_noMotion() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotion.NoMotion)
    }

    @Test
    fun singlePane_firstToSecond_movesLeft() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToLeftMotion)
    }

    @Test
    fun singlePane_firstToThird_movesLeft() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToLeftMotion)
    }

    @Test
    fun singlePane_secondToThird_movesLeft() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToLeftMotion)
    }

    @Test
    fun singlePane_secondToFirst_movesRight() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToRightMotion)
    }

    @Test
    fun singlePane_thirdToFirst_movesRight() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToRightMotion)
    }

    @Test
    fun singlePane_thirdToSecond_movesRight() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToRightMotion)
    }

    @Test
    fun dualPane_hidesFirstShowsThird_movesLeft() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToLeftMotion)
    }

    @Test
    fun dualPane_hidesThirdShowsFirst_movesRight() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.movePanesToRightMotion)
    }

    @Test
    fun dualPane_hidesSecondShowsThird_switchRightTwoPanes() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.switchRightTwoPanesMotion)
    }

    @Test
    fun dualPane_hidesThirdShowsSecond_switchRightTwoPanes() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.switchRightTwoPanesMotion)
    }

    @Test
    fun dualPane_hidesFirstShowsSecond_switchLeftTwoPanes() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.switchLeftTwoPanesMotion)
    }

    @Test
    fun dualPane_hidesSecondShowsFirst_switchLeftTwoPanes() {
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotionDefaults.switchLeftTwoPanesMotion)
    }

    @Test
    fun changeNumberOfPanes_noMotion() {
        // TODO(conradchen): Update this when we support motions in this case
        val motions = calculateThreePaneMotion(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded
            ),
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            ),
            PaneOrder
        )
        assertThat(motions).isEqualTo(ThreePaneMotion.NoMotion)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneOrder = ThreePaneScaffoldDefaults.SupportingPaneLayoutPaneOrder
