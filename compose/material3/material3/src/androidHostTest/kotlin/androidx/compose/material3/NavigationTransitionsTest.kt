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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.navigationevent.NavigationEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigationTransitionsTest {

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
    fun slideOffset_centerPivot_returnsZeroHorizontalOffset() {
        val offset =
            calculateSlideOffset(
                fullSize = IntSize(width = 1000, height = 2000),
                currentScale = 0.85f,
                rawYDelta = 0f,
                displayBoundsMargin = 20f,
                pivotFractionX = 0.5f,
            )
        assertThat(offset).isEqualTo(IntOffset.Zero)
    }

    @Test
    fun slideOffset_rightShiftedPivot_returnsPositiveHorizontalOffset() {
        val offset =
            calculateSlideOffset(
                fullSize = IntSize(width = 1000, height = 2000),
                currentScale = 0.85f,
                rawYDelta = 0f,
                displayBoundsMargin = 20f,
                pivotFractionX = 0.9f,
            )

        assertThat(offset).isEqualTo(IntOffset(59, 0))
    }

    @Test
    fun slideOffset_fullScale_returnsZeroHorizontalOffset() {
        val offset =
            calculateSlideOffset(
                fullSize = IntSize(width = 1000, height = 2000),
                currentScale = 1f,
                rawYDelta = 0f,
                displayBoundsMargin = 20f,
                pivotFractionX = 0.9f,
            )
        assertThat(offset).isEqualTo(IntOffset.Zero)
    }

    @Test
    fun horizontalSharedAxisForwardTransform_configuresSlideAndFade() {
        val spec = HorizontalSharedAxisNavigationTransition.forwardTransform()
        val fullSize = IntSize(width = 1000, height = 2000)

        assertThat(spec.targetContentEnter.config.slide?.slideOffset?.invoke(fullSize))
            .isEqualTo(IntOffset(200, 0))
        assertThat(spec.initialContentExit.config.slide?.slideOffset?.invoke(fullSize))
            .isEqualTo(IntOffset(-200, 0))
        assertThat(spec.targetContentEnter.config.fade?.alpha).isEqualTo(0f)
        assertThat(spec.initialContentExit.config.fade?.alpha).isEqualTo(0f)
        assertThat(spec.targetContentZIndex).isEqualTo(0f)
    }

    @Test
    fun horizontalSharedAxisBackwardTransform_configuresSlideFadeScaleAndZIndex() {
        val spec = HorizontalSharedAxisNavigationTransition.backwardTransform()
        val fullSize = IntSize(width = 1000, height = 2000)

        assertThat(spec.targetContentEnter.config.slide?.slideOffset?.invoke(fullSize))
            .isEqualTo(IntOffset(-200, 0))
        assertThat(spec.initialContentExit.config.slide?.slideOffset?.invoke(fullSize))
            .isEqualTo(IntOffset(200, 0))
        assertThat(spec.targetContentEnter.config.fade?.alpha).isEqualTo(0f)
        assertThat(spec.initialContentExit.config.fade?.alpha).isEqualTo(0f)
        assertThat(spec.targetContentEnter.config.fade?.animationSpec)
            .isEqualTo(spec.initialContentExit.config.fade?.animationSpec)
        assertThat(spec.initialContentExit.config.scale?.scale).isEqualTo(1f)
        assertThat(spec.targetContentZIndex).isLessThan(0f)
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
            HorizontalSharedAxisNavigationTransition.predictiveBackTransform(
                backEvent = { backEventState.value },
                density = Density(1f),
                scrimColor = Color.Black.copy(alpha = 0.8f),
            )
        assertThat(mutableTransform).isNotNull()
    }

    @Test
    fun fadeThroughTransform_configuresFadeAndScale() {
        val spec = FadeThroughNavigationTransition.transform()
        assertThat(spec.targetContentEnter.config.fade?.alpha).isEqualTo(0f)
        assertThat(spec.initialContentExit.config.fade?.alpha).isEqualTo(0f)
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
            FadeThroughNavigationTransition.predictiveBackTransform(
                backEvent = { backEventState.value },
                density = Density(1f),
            )
        assertThat(mutableTransform).isNotNull()
    }
}
