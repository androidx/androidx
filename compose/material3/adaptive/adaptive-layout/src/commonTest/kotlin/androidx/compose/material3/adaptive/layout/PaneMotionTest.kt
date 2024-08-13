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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.AnimateBounds
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromLeft
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromLeftDelayed
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromRight
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromRightDelayed
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterWithExpand
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitToLeft
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitToRight
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitWithShrink
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.NoMotion
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Expanded
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Hidden
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.kruth.assertWithMessage
import kotlin.test.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class PaneMotionTest {
    @Test
    fun test_allThreePaneMotions() {
        for (from in ExpectedThreePaneMotions.indices) {
            for (to in ExpectedThreePaneMotions.indices) {
                val fromValue = from.toThreePaneScaffoldValue()
                val toValue = to.toThreePaneScaffoldValue()
                assertWithMessage("From $fromValue to $toValue: ")
                    .that(calculatePaneMotion(fromValue, toValue, MockThreePaneOrder))
                    .isEqualTo(ExpectedThreePaneMotions[from][to])
            }
        }
    }

    @Test
    fun test_allDefaultPaneMotionTransitions() {
        NoMotion.assertTransitions(EnterTransition.None, ExitTransition.None)
        EnterFromLeft.assertTransitions(mockEnterFromLeftTransition, ExitTransition.None)
        EnterFromRight.assertTransitions(mockEnterFromRightTransition, ExitTransition.None)
        EnterFromLeftDelayed.assertTransitions(
            mockEnterFromLeftDelayedTransition,
            ExitTransition.None
        )
        EnterFromRightDelayed.assertTransitions(
            mockEnterFromRightDelayedTransition,
            ExitTransition.None
        )
        ExitToLeft.assertTransitions(EnterTransition.None, mockExitToLeftTransition)
        ExitToRight.assertTransitions(EnterTransition.None, mockExitToRightTransition)
        EnterWithExpand.assertTransitions(mockEnterWithExpandTransition, ExitTransition.None)
        ExitWithShrink.assertTransitions(EnterTransition.None, mockExitWithShrinkTransition)
    }

    private fun DefaultPaneMotion.assertTransitions(
        expectedEnterTransition: EnterTransition,
        expectedExitTransition: ExitTransition
    ) {
        // Can't compare equality directly because of lambda. Check string representation instead
        assertWithMessage("Enter transition of $this: ")
            .that(mockPaneMotionScope.enterTransition.toString())
            .isEqualTo(expectedEnterTransition.toString())
        assertWithMessage("Exit transition of $this: ")
            .that(mockPaneMotionScope.exitTransition.toString())
            .isEqualTo(expectedExitTransition.toString())
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun Int.toThreePaneScaffoldValue(): ThreePaneScaffoldValue {
    return when (this) {
        0 -> ThreePaneScaffoldValue(Hidden, Hidden, Hidden)
        1 -> ThreePaneScaffoldValue(Expanded, Hidden, Hidden)
        2 -> ThreePaneScaffoldValue(Hidden, Expanded, Hidden)
        3 -> ThreePaneScaffoldValue(Hidden, Hidden, Expanded)
        4 -> ThreePaneScaffoldValue(Expanded, Expanded, Hidden)
        5 -> ThreePaneScaffoldValue(Expanded, Hidden, Expanded)
        6 -> ThreePaneScaffoldValue(Hidden, Expanded, Expanded)
        7 -> ThreePaneScaffoldValue(Expanded, Expanded, Expanded)
        else -> throw AssertionError("Unexpected scaffold value: $this")
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockThreePaneOrder =
    ThreePaneScaffoldHorizontalOrder(
        ThreePaneScaffoldRole.Primary,
        ThreePaneScaffoldRole.Secondary,
        ThreePaneScaffoldRole.Tertiary
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ExpectedThreePaneMotions =
    arrayOf(
        // From H, H, H
        arrayOf(
            arrayOf(NoMotion, NoMotion, NoMotion), // To H, H, H
            arrayOf(EnterFromRight, NoMotion, NoMotion), // To V, H, H
            arrayOf(NoMotion, EnterFromRight, NoMotion), // To H, V, H
            arrayOf(NoMotion, NoMotion, EnterFromRight), // To H, H, V
            arrayOf(EnterFromRight, EnterFromRight, NoMotion), // To V, V, H
            arrayOf(EnterFromRight, NoMotion, EnterFromRight), // To V, H, V
            arrayOf(NoMotion, EnterFromRight, EnterFromRight), // To H, V, V
            arrayOf(EnterFromRight, EnterFromRight, EnterFromRight), // To V, V, V
        ),
        // From V, H, H
        arrayOf(
            arrayOf(ExitToRight, NoMotion, NoMotion), // To H, H, H
            arrayOf(AnimateBounds, NoMotion, NoMotion), // To V, H, H
            arrayOf(ExitToLeft, EnterFromRight, NoMotion), // To H, V, H
            arrayOf(ExitToLeft, NoMotion, EnterFromRight), // To H, H, V
            arrayOf(AnimateBounds, EnterFromRight, NoMotion), // To V, V, H
            arrayOf(AnimateBounds, NoMotion, EnterFromRight), // To V, H, V
            arrayOf(ExitToLeft, EnterFromRight, EnterFromRight), // To H, V, V
            arrayOf(AnimateBounds, EnterFromRight, EnterFromRight), // To V, V, V
        ),
        // From H, V, H
        arrayOf(
            arrayOf(NoMotion, ExitToRight, NoMotion), // To H, H, H
            arrayOf(EnterFromLeft, ExitToRight, NoMotion), // To V, H, H
            arrayOf(NoMotion, AnimateBounds, NoMotion), // To H, V, H
            arrayOf(NoMotion, ExitToLeft, EnterFromRight), // To H, H, V
            arrayOf(EnterFromLeft, AnimateBounds, NoMotion), // To V, V, H
            arrayOf(EnterFromLeft, ExitToRight, EnterFromRightDelayed), // To V, H, V
            arrayOf(NoMotion, AnimateBounds, EnterFromRight), // To H, V, V
            arrayOf(EnterFromLeft, AnimateBounds, EnterFromRight), // To V, V, V
        ),
        // From H, H, V
        arrayOf(
            arrayOf(NoMotion, NoMotion, ExitToRight), // To H, H, H
            arrayOf(EnterFromLeft, NoMotion, ExitToRight), // To V, H, H
            arrayOf(NoMotion, EnterFromLeft, ExitToRight), // To H, V, H
            arrayOf(NoMotion, NoMotion, AnimateBounds), // To H, H, V
            arrayOf(EnterFromLeft, EnterFromLeft, ExitToRight), // To V, V, H
            arrayOf(EnterFromLeft, NoMotion, AnimateBounds), // To V, H, V
            arrayOf(NoMotion, EnterFromLeft, AnimateBounds), // To H, V, V
            arrayOf(EnterFromLeft, EnterFromLeft, AnimateBounds), // To V, V, V
        ),
        // From V, V, H
        arrayOf(
            arrayOf(ExitToRight, ExitToRight, NoMotion), // To H, H, H
            arrayOf(AnimateBounds, ExitToRight, NoMotion), // To V, H, H
            arrayOf(ExitToLeft, AnimateBounds, NoMotion), // To H, V, H
            arrayOf(ExitToLeft, ExitToLeft, EnterFromRight), // To H, H, V
            arrayOf(AnimateBounds, AnimateBounds, NoMotion), // To V, V, H
            arrayOf(AnimateBounds, ExitToRight, EnterFromRightDelayed), // To V, H, V
            arrayOf(ExitToLeft, AnimateBounds, EnterFromRight), // To H, V, V
            arrayOf(AnimateBounds, AnimateBounds, EnterFromRight), // To V, V, V
        ),
        // From V, H, V
        arrayOf(
            arrayOf(ExitToRight, NoMotion, ExitToRight), // To H, H, H
            arrayOf(AnimateBounds, NoMotion, ExitToRight), // To V, H, H
            arrayOf(ExitToLeft, EnterFromRightDelayed, ExitToRight), // To H, V, H
            arrayOf(ExitToLeft, NoMotion, AnimateBounds), // To H, H, V
            arrayOf(AnimateBounds, EnterFromRightDelayed, ExitToRight), // To V, V, H
            arrayOf(AnimateBounds, NoMotion, AnimateBounds), // To V, H, V
            arrayOf(ExitToLeft, EnterFromLeftDelayed, AnimateBounds), // To H, V, V
            arrayOf(AnimateBounds, EnterWithExpand, AnimateBounds), // To V, V, V
        ),
        // From H, V, V
        arrayOf(
            arrayOf(NoMotion, ExitToRight, ExitToRight), // To H, H, H
            arrayOf(EnterFromLeft, ExitToRight, ExitToRight), // To V, H, H
            arrayOf(NoMotion, AnimateBounds, ExitToRight), // To H, V, H
            arrayOf(NoMotion, ExitToLeft, AnimateBounds), // To H, H, V
            arrayOf(EnterFromLeft, AnimateBounds, ExitToRight), // To V, V, H
            arrayOf(EnterFromLeftDelayed, ExitToLeft, AnimateBounds), // To V, H, V
            arrayOf(NoMotion, AnimateBounds, AnimateBounds), // To H, V, V
            arrayOf(EnterFromLeft, AnimateBounds, AnimateBounds), // To V, V, V
        ),
        // From V, V, V
        arrayOf(
            arrayOf(ExitToRight, ExitToRight, ExitToRight), // To H, H, H
            arrayOf(AnimateBounds, ExitToRight, ExitToRight), // To V, H, H
            arrayOf(ExitToLeft, AnimateBounds, ExitToRight), // To H, V, H
            arrayOf(ExitToLeft, ExitToLeft, AnimateBounds), // To H, H, V
            arrayOf(AnimateBounds, AnimateBounds, ExitToRight), // To V, V, H
            arrayOf(AnimateBounds, ExitWithShrink, AnimateBounds), // To V, H, V
            arrayOf(ExitToLeft, AnimateBounds, AnimateBounds), // To H, V, V
            arrayOf(AnimateBounds, AnimateBounds, AnimateBounds), // To V, V, V
        ),
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockPaneMotionScope =
    object : PaneMotionScope {
        override val positionAnimationSpec: FiniteAnimationSpec<IntOffset> = tween()
        override val sizeAnimationSpec: FiniteAnimationSpec<IntSize> = spring()
        override val delayedPositionAnimationSpec: FiniteAnimationSpec<IntOffset> = snap()
        override val slideInFromLeftOffset: Int = 1
        override val slideInFromRightOffset: Int = 2
        override val slideOutToLeftOffset: Int = 3
        override val slideOutToRightOffset: Int = 4
        override val motionProgress: () -> Float = { 0.5F }
        override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
            get() = mockLayoutCoordinates

        override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates =
            mockLayoutCoordinates

        val mockLayoutCoordinates =
            object : LayoutCoordinates {
                override val isAttached: Boolean = false
                override val parentCoordinates: LayoutCoordinates? = null
                override val parentLayoutCoordinates: LayoutCoordinates? = null
                override val providedAlignmentLines: Set<AlignmentLine> = emptySet()
                override val size: IntSize = IntSize.Zero

                override fun get(alignmentLine: AlignmentLine): Int = 0

                override fun localBoundingBoxOf(
                    sourceCoordinates: LayoutCoordinates,
                    clipBounds: Boolean
                ): Rect = Rect.Zero

                override fun localPositionOf(
                    sourceCoordinates: LayoutCoordinates,
                    relativeToSource: Offset
                ): Offset = Offset.Zero

                override fun localToRoot(relativeToLocal: Offset): Offset = Offset.Zero

                override fun localToWindow(relativeToLocal: Offset): Offset = Offset.Zero

                override fun windowToLocal(relativeToWindow: Offset): Offset = Offset.Zero
            }
    }

private val mockEnterFromLeftTransition =
    slideInHorizontally(mockPaneMotionScope.positionAnimationSpec) {
        mockPaneMotionScope.slideInFromLeftOffset
    }

private val mockEnterFromRightTransition =
    slideInHorizontally(mockPaneMotionScope.positionAnimationSpec) {
        mockPaneMotionScope.slideInFromRightOffset
    }

private val mockEnterFromLeftDelayedTransition =
    slideInHorizontally(mockPaneMotionScope.delayedPositionAnimationSpec) {
        mockPaneMotionScope.slideInFromLeftOffset
    }

private val mockEnterFromRightDelayedTransition =
    slideInHorizontally(mockPaneMotionScope.delayedPositionAnimationSpec) {
        mockPaneMotionScope.slideInFromLeftOffset
    }

private val mockExitToLeftTransition =
    slideOutHorizontally(mockPaneMotionScope.positionAnimationSpec) {
        mockPaneMotionScope.slideOutToLeftOffset
    }

private val mockExitToRightTransition =
    slideOutHorizontally(mockPaneMotionScope.positionAnimationSpec) {
        mockPaneMotionScope.slideOutToRightOffset
    }

private val mockEnterWithExpandTransition =
    expandHorizontally(mockPaneMotionScope.sizeAnimationSpec, Alignment.CenterHorizontally)

private val mockExitWithShrinkTransition =
    shrinkHorizontally(mockPaneMotionScope.sizeAnimationSpec, Alignment.CenterHorizontally)
