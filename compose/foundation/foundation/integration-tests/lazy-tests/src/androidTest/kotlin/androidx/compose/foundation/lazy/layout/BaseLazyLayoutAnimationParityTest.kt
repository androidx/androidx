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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule

abstract class BaseLazyLayoutAnimationParityTest(private val defaultItemSize: IntSize) {

    @get:Rule val rule = createComposeRule()

    protected var visible by mutableStateOf(false)

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setUpParityTest() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun assertEnterParity(
        enter: EnterTransition,
        itemSize: IntSize = defaultItemSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        frames: Int = TotalFrames,
        maxChannelDifference: Int = 0,
    ) {
        setParityContent(
            initiallyVisible = false,
            enter = enter,
            exit = ExitTransition.None,
            itemSize = itemSize,
            layoutDirection = layoutDirection,
        )

        toggleAndAssertParity(frames, maxChannelDifference)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun assertExitParity(
        exit: ExitTransition,
        itemSize: IntSize = defaultItemSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        frames: Int = TotalFrames,
        maxChannelDifference: Int = 0,
    ) {
        setParityContent(
            initiallyVisible = true,
            enter = EnterTransition.None,
            exit = exit,
            itemSize = itemSize,
            layoutDirection = layoutDirection,
        )

        toggleAndAssertParity(frames, maxChannelDifference)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun setParityContent(
        initiallyVisible: Boolean,
        enter: EnterTransition,
        exit: ExitTransition,
        itemSize: IntSize = defaultItemSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        visible = initiallyVisible
        rule.setContent {
            val isVisible = visible
            val density = rule.density
            val itemWidth = with(density) { itemSize.width.toDp() }
            val itemHeight = with(density) { itemSize.height.toDp() }
            val containerModifier =
                Modifier.requiredSize(itemWidth * ContainerScale, itemHeight * ContainerScale)
                    .clipToBounds()
                    .background(Color.White)
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Column {
                    Box(containerModifier.testTag(LazyTag)) {
                        LazyLayoutUnderTest(
                            visible = isVisible,
                            contentPadding =
                                PaddingValues(horizontal = itemWidth, vertical = itemHeight),
                        ) {
                            ParityItem(
                                itemSize = itemSize,
                                modifier =
                                    Modifier.animateItemWithTransitions(
                                        enterTransition = enter,
                                        exitTransition = exit,
                                        placementSpec = null,
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.requiredHeight(itemHeight * ContainerScale))
                    AnimatedVisibilityContainer(
                        modifier = containerModifier.testTag(AnimatedVisibilityTag),
                        itemSize = itemSize,
                        enter = enter,
                        exit = exit,
                    )
                }
            }
        }
        rule.waitForIdle()
        assertFrameParity(-1)
    }

    @Composable
    protected abstract fun LazyLayoutUnderTest(
        visible: Boolean,
        contentPadding: PaddingValues,
        itemContent: @Composable () -> Unit,
    )

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun toggleAndAssertParity(
        frames: Int,
        maxChannelDifference: Int = 0,
        onFrame: (frame: Int) -> Unit = {},
    ) {
        runFramesAndAssertParity(frames, maxChannelDifference) { frame ->
            if (frame == 0) visible = !visible
            onFrame(frame)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun runFramesAndAssertParity(
        frames: Int,
        maxChannelDifference: Int = 0,
        onFrame: (frame: Int) -> Unit = {},
    ) {
        for (frame in 0..frames) {
            rule.runOnUiThread { onFrame(frame) }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertFrameParity(frame, maxChannelDifference)
        }
    }

    protected fun <T> recordInterruptedFrames(
        interruptFrame: Int,
        frames: Int,
        measure: (tag: String) -> T?,
    ): List<ParityFrame<T>> {
        val recorded = mutableListOf<ParityFrame<T>>()
        for (frame in 0..frames) {
            rule.runOnUiThread { if (frame == 0 || frame == interruptFrame) visible = !visible }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            recorded += ParityFrame(measure(LazyTag), measure(AnimatedVisibilityTag))
        }
        return recorded
    }

    protected fun <T> assertParityUntilInterruption(
        frames: List<ParityFrame<T>>,
        interruptFrame: Int = InterruptFrame,
    ) {
        for (frame in 0..interruptFrame) {
            assertWithMessage("Frame $frame before interruption")
                .that(frames[frame].lazy)
                .isEqualTo(frames[frame].animatedVisibility)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun capture(tag: String): PixelMap =
        rule.onNodeWithTag(tag).captureToImage().toPixelMap()

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun isBlank(tag: String): Boolean {
        val map = capture(tag)
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map[x, y] != Color.White) return false
            }
        }
        return true
    }

    protected class ParityFrame<T>(val lazy: T?, val animatedVisibility: T?)

    @Composable
    private fun AnimatedVisibilityContainer(
        modifier: Modifier,
        itemSize: IntSize,
        enter: EnterTransition,
        exit: ExitTransition,
    ) {
        val density = rule.density
        Box(modifier) {
            Box(
                Modifier.fillMaxSize()
                    .padding(
                        horizontal = with(density) { itemSize.width.toDp() },
                        vertical = with(density) { itemSize.height.toDp() },
                    )
            ) {
                AnimatedVisibility(visible = visible, enter = enter, exit = exit) {
                    ParityItem(itemSize = itemSize)
                }
            }
        }
    }

    @Composable
    private fun ParityItem(itemSize: IntSize, modifier: Modifier = Modifier) {
        val density = rule.density
        Column(
            modifier.requiredSize(
                with(density) { itemSize.width.toDp() },
                with(density) { itemSize.height.toDp() },
            )
        ) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxHeight().aliasedBackground(Color.Red))
                Box(Modifier.weight(1f).fillMaxHeight().aliasedBackground(Color.Green))
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxHeight().aliasedBackground(Color.Blue))
                Box(Modifier.weight(1f).fillMaxHeight().aliasedBackground(Color.Black))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertFrameParity(frame: Int, maxChannelDifference: Int = 0) {
        val lazy = capture(LazyTag)
        val animatedVisibility = capture(AnimatedVisibilityTag)

        assertWithMessage("Frame $frame: captured image sizes differ")
            .that(IntSize(lazy.width, lazy.height))
            .isEqualTo(IntSize(animatedVisibility.width, animatedVisibility.height))

        var mismatches = 0
        var largestDifference = 0
        var firstMismatch: String? = null
        for (y in 0 until lazy.height) {
            for (x in 0 until lazy.width) {
                val lazyColor = lazy[x, y]
                val animatedVisibilityColor = animatedVisibility[x, y]
                val difference = lazyColor.maxChannelDifference(animatedVisibilityColor)
                if (difference > maxChannelDifference) {
                    mismatches++
                    largestDifference = maxOf(largestDifference, difference)
                    if (firstMismatch == null) {
                        firstMismatch =
                            "($x, $y) lazy=$lazyColor animatedVisibility=$animatedVisibilityColor"
                    }
                }
            }
        }

        assertWithMessage(
                "Frame $frame: $mismatches of ${lazy.width * lazy.height} pixels differ by more " +
                    "than $maxChannelDifference (largest difference $largestDifference), " +
                    "first mismatch at $firstMismatch"
            )
            .that(mismatches)
            .isEqualTo(0)
    }

    companion object {
        const val FrameDurationMillis = 16
        const val DurationFrames = 10
        const val InterruptFrame = DurationFrames / 2
        const val DurationMillis = FrameDurationMillis * DurationFrames
        const val TotalFrames = DurationFrames + 4
        const val DelayFrames = 4
        const val DelayedFrames = TotalFrames + DelayFrames
        const val SpringFrames = 60
        const val ContainerScale = 3
        const val ItemKey = 0
        const val LazyTag = "lazy"
        const val AnimatedVisibilityTag = "animatedVisibility"

        fun <T> linearSpec(): FiniteAnimationSpec<T> = tween(DurationMillis, easing = LinearEasing)

        fun <T> easedSpec(): FiniteAnimationSpec<T> =
            tween(DurationMillis, easing = FastOutSlowInEasing)

        fun <T> delayedSpec(): FiniteAnimationSpec<T> =
            tween(
                DurationMillis,
                delayMillis = FrameDurationMillis * DelayFrames,
                easing = LinearEasing,
            )
    }
}

private fun Color.maxChannelDifference(other: Color): Int {
    val a = toArgb()
    val b = other.toArgb()
    return (0 until 32 step 8).maxOf { shift ->
        abs((a shr shift and 0xFF) - (b shr shift and 0xFF))
    }
}

private fun Modifier.aliasedBackground(color: Color) = drawBehind {
    drawIntoCanvas { canvas ->
        canvas.drawRect(
            Rect(Offset.Zero, size),
            Paint().apply {
                this.color = color
                isAntiAlias = false
            },
        )
    }
}
