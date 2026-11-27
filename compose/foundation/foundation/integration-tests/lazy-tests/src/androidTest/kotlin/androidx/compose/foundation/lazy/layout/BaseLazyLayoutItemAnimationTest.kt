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

package androidx.compose.foundation.lazy.layout

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
open class BaseLazyLayoutItemAnimationTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {
    @Composable
    protected fun LazyItemScope.Item(
        color: Color,
        crossAxisSize: Int = ItemCrossAxisSize,
        enter: EnterTransition? = null,
        exit: ExitTransition? = null,
        placementSpec: FiniteAnimationSpec<IntOffset>? = null,
        modifier: Modifier = Modifier,
    ) {
        val density = rule.density
        Box(
            modifier
                .animateItemWithTransitions(
                    enterTransition = enter,
                    exitTransition = exit,
                    placementSpec = placementSpec,
                )
                .background(color)
                .requiredMainAxisSize(with(density) { ItemMainAxisSize.toDp() })
                .requiredCrossAxisSize(with(density) { crossAxisSize.toDp() })
        )
    }

    protected fun slideInMainAxis(
        animationSpec: FiniteAnimationSpec<IntOffset>,
        initialOffset: (mainAxisSize: Int) -> Int = { -it / 2 },
    ): EnterTransition =
        if (vertical) {
            slideInVertically(animationSpec, initialOffset)
        } else {
            slideInHorizontally(animationSpec, initialOffset)
        }

    protected fun slideInCrossAxis(
        animationSpec: FiniteAnimationSpec<IntOffset>,
        initialOffset: (crossAxisSize: Int) -> Int = { -it / 2 },
    ): EnterTransition =
        if (vertical) {
            slideInHorizontally(animationSpec, initialOffset)
        } else {
            slideInVertically(animationSpec, initialOffset)
        }

    protected fun expandMainAxis(
        animationSpec: FiniteAnimationSpec<IntSize>,
        fromEnd: Boolean = true,
        clip: Boolean = true,
        initialSize: (fullSize: Int) -> Int = { 0 },
    ): EnterTransition =
        if (vertical) {
            expandVertically(
                animationSpec,
                expandFrom = if (fromEnd) Alignment.Bottom else Alignment.Top,
                clip = clip,
                initialHeight = initialSize,
            )
        } else {
            expandHorizontally(
                animationSpec,
                expandFrom = if (fromEnd) Alignment.End else Alignment.Start,
                clip = clip,
                initialWidth = initialSize,
            )
        }

    protected fun slideOutCrossAxis(
        animationSpec: FiniteAnimationSpec<IntOffset>,
        targetOffset: (crossAxisSize: Int) -> Int = { -it / 2 },
    ): ExitTransition =
        if (vertical) {
            slideOutHorizontally(animationSpec, targetOffset)
        } else {
            slideOutVertically(animationSpec, targetOffset)
        }

    protected fun onAnimationFrame(onFrame: (fraction: Float) -> Unit) {
        onAnimationStep { step -> onFrame(step / Steps.toFloat()) }
    }

    protected fun onAnimationStep(steps: Int = Steps, onFrame: (step: Int) -> Unit) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        for (step in 0..steps) {
            onFrame(step)
            if (step < steps) rule.mainClock.advanceTimeBy(FrameDuration)
        }
    }

    protected inner class ExpectedItem(
        val color: Color,
        val mainAxis: Int = 0,
        val crossAxis: Int = 0,
        val mainAxisSize: Int = ItemMainAxisSize,
        val crossAxisSize: Int = ItemCrossAxisSize,
    ) {
        fun contains(position: IntOffset): Boolean =
            position.mainAxis >= mainAxis &&
                position.mainAxis < mainAxis + mainAxisSize &&
                position.crossAxis >= crossAxis &&
                position.crossAxis < crossAxis + crossAxisSize
    }

    protected fun assertItems(
        containerMainAxisSize: Int,
        vararg items: ExpectedItem,
        containerCrossAxisSize: Int = ItemCrossAxisSize,
    ) {
        rule.onNodeWithTag(ContainerTag).captureToImage().assertPixels(
            axisSize(crossAxis = containerCrossAxisSize, mainAxis = containerMainAxisSize)
        ) { position ->
            (items.lastOrNull { it.contains(position) }?.color ?: Color.White).compositeOver(
                Color.White
            )
        }
    }

    protected companion object {
        const val FrameDuration = 16L
        const val Steps = 4
        const val ItemMainAxisSize = 8
        const val DurationMillis = (FrameDuration * Steps).toInt()
        const val ItemCrossAxisSize = 16
        const val ContainerTag = "container"

        fun interpolate(start: Int, stop: Int, fraction: Float): Int =
            start + ((stop - start) * fraction).roundToInt()

        fun interpolate(start: Float, stop: Float, fraction: Float): Float =
            start + (stop - start) * fraction
    }
}
