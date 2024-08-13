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

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ThreePaneMotionTest {
    @Test
    fun noPane_noMotion() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(ThreePaneMotion.NoMotion)
    }

    @Test
    fun singlePane_firstToSecond_movesLeft() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToLeftMotion(SpacerSize))
    }

    @Test
    fun singlePane_firstToThird_movesLeft() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToLeftMotion(SpacerSize))
    }

    @Test
    fun singlePane_secondToThird_movesLeft() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToLeftMotion(SpacerSize))
    }

    @Test
    fun singlePane_secondToFirst_movesRight() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToRightMotion(SpacerSize))
    }

    @Test
    fun singlePane_thirdToFirst_movesRight() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToRightMotion(SpacerSize))
    }

    @Test
    fun singlePane_thirdToSecond_movesRight() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToRightMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesFirstShowsThird_movesLeft() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToLeftMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesThirdShowsFirst_movesRight() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(MovePanesToRightMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesSecondShowsThird_switchRightTwoPanes() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(SwitchRightTwoPanesMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesThirdShowsSecond_switchRightTwoPanes() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(SwitchRightTwoPanesMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesFirstShowsSecond_switchLeftTwoPanes() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(SwitchLeftTwoPanesMotion(SpacerSize))
    }

    @Test
    fun dualPane_hidesSecondShowsFirst_switchLeftTwoPanes() {
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(SwitchLeftTwoPanesMotion(SpacerSize))
    }

    @Test
    fun changeNumberOfPanes_noMotion() {
        // TODO(conradchen): Update this when we support motions in this case
        val motions =
            calculateThreePaneMotion(
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
                PaneOrder,
                SpacerSize
            )
        assertThat(motions).isEqualTo(ThreePaneMotion.NoMotion)
    }

    @Test
    fun delayedSpring_identicalWithOriginPlusDelay() {
        val delayedRatio = 0.5f

        val originalSpec =
            spring(dampingRatio = 0.7f, stiffness = 500f, visibilityThreshold = 0.1f)
                .vectorize(Float.VectorConverter)

        val delayedSpec =
            DelayedSpringSpec(
                    dampingRatio = 0.7f,
                    stiffness = 500f,
                    visibilityThreshold = 0.1f,
                    delayedRatio = delayedRatio,
                )
                .vectorize(Float.VectorConverter)

        val originalDurationNanos = originalSpec.getDurationNanos()
        val delayedNanos = (originalDurationNanos * delayedRatio).toLong()

        fun assertValuesAt(playTimeNanos: Long) {
            assertValuesAreEqual(
                originalSpec.getValueFromNanos(playTimeNanos),
                delayedSpec.getValueFromNanos(playTimeNanos + delayedNanos)
            )
        }

        assertValuesAt(0)
        assertValuesAt((originalDurationNanos * 0.2).toLong())
        assertValuesAt((originalDurationNanos * 0.35).toLong())
        assertValuesAt((originalDurationNanos * 0.6).toLong())
        assertValuesAt((originalDurationNanos * 0.85).toLong())
        assertValuesAt(originalDurationNanos)
    }

    private fun VectorizedAnimationSpec<AnimationVector1D>.getDurationNanos(): Long =
        getDurationNanos(InitialValue, TargetValue, InitialVelocity)

    private fun VectorizedAnimationSpec<AnimationVector1D>.getValueFromNanos(
        playTimeNanos: Long
    ): Float = getValueFromNanos(playTimeNanos, InitialValue, TargetValue, InitialVelocity).value

    private fun assertValuesAreEqual(value1: Float, value2: Float) {
        assertEquals(value1, value2, Tolerance)
    }

    companion object {
        private val InitialValue = AnimationVector1D(0f)
        private val TargetValue = AnimationVector1D(1f)
        private val InitialVelocity = AnimationVector1D(0f)
        private const val Tolerance = 0.001f
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneOrder = SupportingPaneScaffoldDefaults.PaneOrder
internal const val SpacerSize = 123
