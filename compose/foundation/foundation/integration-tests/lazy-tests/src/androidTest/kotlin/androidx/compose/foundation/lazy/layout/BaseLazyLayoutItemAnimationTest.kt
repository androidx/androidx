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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import org.junit.Rule

@RequiresApi(Build.VERSION_CODES.O)
open class BaseLazyLayoutItemAnimationTest {
    @get:Rule val rule = createComposeRule()

    @Composable
    protected fun LazyItemScope.Item(
        color: Color,
        width: Int = ItemWidth,
        enter: EnterTransition? = null,
        exit: ExitTransition? = null,
        placementSpec: FiniteAnimationSpec<IntOffset>? = null,
    ) {
        val density = rule.density
        Box(
            Modifier.animateItemWithTransitions(
                    enterTransition = enter,
                    exitTransition = exit,
                    placementSpec = placementSpec,
                )
                .background(color)
                .requiredHeight(with(density) { ItemHeight.toDp() })
                .requiredWidth(with(density) { width.toDp() })
        )
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

    protected class ExpectedItem(
        val color: Color,
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = ItemWidth,
        val height: Int = ItemHeight,
    ) {
        fun contains(position: IntOffset) =
            position.x >= x && position.x < x + width && position.y >= y && position.y < y + height
    }

    protected fun assertItems(
        containerHeight: Int,
        vararg items: ExpectedItem,
        containerWidth: Int = ItemWidth,
    ) {
        rule.onNodeWithTag(ContainerTag).captureToImage().assertPixels(
            IntSize(containerWidth, containerHeight)
        ) { position ->
            (items.lastOrNull { it.contains(position) }?.color ?: Color.White).compositeOver(
                Color.White
            )
        }
    }

    protected companion object {
        const val FrameDuration = 16L
        const val Steps = 4
        const val ItemHeight = 8
        const val DurationMillis = (FrameDuration * Steps).toInt()
        const val ItemWidth = 16
        const val ContainerTag = "container"

        fun interpolate(start: Int, stop: Int, fraction: Float): Int =
            start + ((stop - start) * fraction).roundToInt()

        fun interpolate(start: Float, stop: Float, fraction: Float): Float =
            start + (stop - start) * fraction
    }
}
