/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.navigationevent.NavigationEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MaterialTransitionsTest {

    @Test
    fun dampedVerticalOffset_zeroDelta_returnsZero() {
        val offset =
            calculateDampedVerticalOffset(
                rawYDelta = 0f,
                containerHeight = 1000f,
                currentScale = 0.9f,
                displayBoundsMargin = 20f,
            )
        assertThat(offset).isEqualTo(0f)
    }

    @Test
    fun dampedVerticalOffset_positiveDelta_returnsPositiveOffsetWithinBounds() {
        val containerHeight = 1000f
        val currentScale = 0.85f
        val displayBoundsMargin = 20f
        val maxPossibleDeltaY =
            (containerHeight - (containerHeight * currentScale)) / 2f - displayBoundsMargin

        val offset =
            calculateDampedVerticalOffset(
                rawYDelta = 200f,
                containerHeight = containerHeight,
                currentScale = currentScale,
                displayBoundsMargin = displayBoundsMargin,
            )

        assertThat(offset).isGreaterThan(0f)
        assertThat(offset).isAtMost(maxPossibleDeltaY)
    }

    @Test
    fun dampedVerticalOffset_negativeDelta_returnsNegativeOffsetWithinBounds() {
        val containerHeight = 1000f
        val currentScale = 0.85f
        val displayBoundsMargin = 20f
        val maxPossibleDeltaY =
            (containerHeight - (containerHeight * currentScale)) / 2f - displayBoundsMargin

        val offset =
            calculateDampedVerticalOffset(
                rawYDelta = -200f,
                containerHeight = containerHeight,
                currentScale = currentScale,
                displayBoundsMargin = displayBoundsMargin,
            )

        assertThat(offset).isLessThan(0f)
        assertThat(-offset).isAtMost(maxPossibleDeltaY)
    }

    @Test
    fun dampedVerticalOffset_zeroContainerHeight_returnsZero() {
        val offset =
            calculateDampedVerticalOffset(
                rawYDelta = 100f,
                containerHeight = 0f,
                currentScale = 0.9f,
                displayBoundsMargin = 20f,
            )
        assertThat(offset).isEqualTo(0f)
    }

    @Test
    fun horizontalSharedAxisForwardTransform_returnsNonNullSpec() {
        val spec = MaterialTransitions.HorizontalSharedAxis.forwardTransform()
        assertThat(spec).isNotNull()
    }

    @Test
    fun horizontalSharedAxisBackwardTransform_returnsNonNullSpec() {
        val spec = MaterialTransitions.HorizontalSharedAxis.backwardTransform()
        assertThat(spec).isNotNull()
    }

    @Test
    fun horizontalSharedAxisPredictiveBackTransform_createsValidMutableTransform() {
        val backEventState =
            mutableStateOf(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 100f,
                    touchY = 300f,
                )
            )

        val mutableTransform =
            MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                backEvent = { backEventState.value },
                density = Density(1f),
                scrimColor = Color.Black.copy(alpha = 0.8f),
            )
        assertThat(mutableTransform).isNotNull()
    }

    @Test
    fun fadeThroughTransform_returnsNonNullSpec() {
        val spec = MaterialTransitions.FadeThrough.transform()
        assertThat(spec).isNotNull()
        assertThat(spec.targetContentEnter.config.scale?.scale).isEqualTo(0.8f)
        assertThat(spec.initialContentExit.config.scale?.scale).isEqualTo(1.05f)
    }

    @Test
    fun fadeThroughPredictiveBackTransform_createsValidMutableTransform() {
        val backEventState =
            mutableStateOf(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 100f,
                    touchY = 300f,
                )
            )

        val mutableTransform =
            MaterialTransitions.FadeThrough.predictiveBackTransform(
                backEvent = { backEventState.value },
                density = Density(1f),
            )
        assertThat(mutableTransform).isNotNull()
    }
}
