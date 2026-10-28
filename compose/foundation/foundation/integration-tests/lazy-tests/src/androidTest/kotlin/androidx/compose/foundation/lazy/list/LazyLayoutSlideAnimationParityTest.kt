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

package androidx.compose.foundation.lazy.list

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.lerp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyLayoutSlideAnimationParityTest {

    @get:Rule val rule = createComposeRule()

    private var visible by mutableStateOf(false)

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @Test
    fun slideInHorizontally_default() {
        assertEnterParity(slideInHorizontally(LinearSpec))
    }

    @Test
    fun slideOutHorizontally_default() {
        assertExitParity(slideOutHorizontally(LinearSpec))
    }

    @Test
    fun slideInVertically_default() {
        assertEnterParity(slideInVertically(LinearSpec))
    }

    @Test
    fun slideOutVertically_default() {
        assertExitParity(slideOutVertically(LinearSpec))
    }

    @Test
    fun slideIn_fromStart() {
        assertEnterParity(slideInHorizontally(LinearSpec) { -it })
    }

    @Test
    fun slideIn_fromEnd() {
        assertEnterParity(slideInHorizontally(LinearSpec) { it })
    }

    @Test
    fun slideIn_fromTop() {
        assertEnterParity(slideInVertically(LinearSpec) { -it })
    }

    @Test
    fun slideIn_fromBottom() {
        assertEnterParity(slideInVertically(LinearSpec) { it })
    }

    @Test
    fun slideOut_toStart() {
        assertExitParity(slideOutHorizontally(LinearSpec) { -it })
    }

    @Test
    fun slideOut_toEnd() {
        assertExitParity(slideOutHorizontally(LinearSpec) { it })
    }

    @Test
    fun slideOut_toTop() {
        assertExitParity(slideOutVertically(LinearSpec) { -it })
    }

    @Test
    fun slideOut_toBottom() {
        assertExitParity(slideOutVertically(LinearSpec) { it })
    }

    @Test
    fun slideIn_diagonal() {
        assertEnterParity(slideIn(LinearSpec) { IntOffset(-it.width, -it.height) })
    }

    @Test
    fun slideOut_diagonal() {
        assertExitParity(slideOut(LinearSpec) { IntOffset(it.width, it.height) })
    }

    @Test
    fun slideIn_partialOffset() {
        assertEnterParity(slideIn(LinearSpec) { IntOffset(it.width / 4, -it.height / 3) })
    }

    @Test
    fun slideOut_partialOffset() {
        assertExitParity(slideOut(LinearSpec) { IntOffset(-it.width / 3, it.height / 4) })
    }

    @Test
    fun slideIn_zeroOffset() {
        assertEnterParity(slideIn(LinearSpec) { IntOffset.Zero })
    }

    @Test
    fun slideOut_zeroOffset() {
        assertExitParity(slideOut(LinearSpec) { IntOffset.Zero })
    }

    @Test
    fun slideIn_offsetLargerThanContainer() {
        assertEnterParity(slideIn(LinearSpec) { IntOffset(it.width * 5, it.height * 5) })
    }

    @Test
    fun slideOut_offsetLargerThanContainer() {
        assertExitParity(slideOut(LinearSpec) { IntOffset(-it.width * 5, -it.height * 5) })
    }

    @Test
    fun slideInHorizontally_oddItemSize() {
        assertEnterParity(slideInHorizontally(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun slideOutHorizontally_oddItemSize() {
        assertExitParity(slideOutHorizontally(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun slideInVertically_oddItemSize() {
        assertEnterParity(slideInVertically(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun slideOutVertically_oddItemSize() {
        assertExitParity(slideOutVertically(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun slideIn_nonLinearEasing() {
        assertEnterParity(slideInHorizontally(EasedSpec) { -it })
    }

    @Test
    fun slideOut_nonLinearEasing() {
        assertExitParity(slideOutHorizontally(EasedSpec) { it })
    }

    @Test
    fun slideIn_springSpec() {
        assertEnterParity(slideInVertically(SpringSpec) { -it }, frames = SpringFrames)
    }

    @Test
    fun slideOut_springSpec() {
        assertExitParity(slideOutVertically(SpringSpec) { it }, frames = SpringFrames)
    }

    @Test
    fun slideIn_delayedTween() {
        assertEnterParity(slideInHorizontally(DelayedSpec) { -it }, frames = DelayedFrames)
    }

    @Test
    fun slideOut_delayedTween() {
        assertExitParity(slideOutHorizontally(DelayedSpec) { it }, frames = DelayedFrames)
    }

    @Test
    fun slideIn_keyframes() {
        assertEnterParity(slideInHorizontally(KeyframesSpec) { -it })
    }

    @Test
    fun slideOut_keyframes() {
        assertExitParity(slideOutHorizontally(KeyframesSpec) { it })
    }

    @Test
    fun slideIn_zeroDuration() {
        assertEnterParity(slideInHorizontally(tween(0)) { -it })
    }

    @Test
    fun slideOut_zeroDuration() {
        assertExitParity(slideOutHorizontally(tween(0)) { it })
    }

    @Test
    fun slideIn_snapSpec() {
        assertEnterParity(slideInHorizontally(snap()) { -it })
    }

    @Test
    fun slideOut_snapSpec() {
        assertExitParity(slideOutHorizontally(snap()) { it })
    }

    @Test
    fun slideInHorizontally_rtl() {
        assertEnterParity(slideInHorizontally(LinearSpec), layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun slideOutHorizontally_rtl() {
        assertExitParity(slideOutHorizontally(LinearSpec), layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun slideIn_diagonal_rtl() {
        assertEnterParity(
            slideIn(LinearSpec) { IntOffset(-it.width, it.height) },
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun slideOut_diagonal_rtl() {
        assertExitParity(
            slideOut(LinearSpec) { IntOffset(it.width, -it.height) },
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun noAnimation_initiallyVisible() {
        setParityContent(
            initiallyVisible = true,
            enter = slideInHorizontally(LinearSpec),
            exit = slideOutHorizontally(LinearSpec),
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun noAnimation_initiallyHidden() {
        setParityContent(
            initiallyVisible = false,
            enter = slideInHorizontally(LinearSpec),
            exit = slideOutHorizontally(LinearSpec),
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun enterThenExit_fullCycle() {
        setParityContent(
            initiallyVisible = false,
            enter = slideInHorizontally(LinearSpec) { -it },
            exit = slideOutVertically(LinearSpec) { it },
        )

        toggleAndAssertParity(TotalFrames)
        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun exitThenEnter_fullCycle() {
        setParityContent(
            initiallyVisible = true,
            enter = slideInVertically(LinearSpec) { -it },
            exit = slideOutHorizontally(LinearSpec) { it },
        )

        toggleAndAssertParity(TotalFrames)
        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun enterInterruptedByExit_lazyKeepsExitSpec_animatedVisibilitySettlesSooner() {
        setParityContent(
            initiallyVisible = false,
            enter = slideInHorizontally(LinearSpec) { -it },
            exit = slideOutHorizontally(LinearSpec) { -it },
        )

        val offsets = recordInterruptedOffsets(InterruptFrame, InterruptFrame + TotalFrames)

        assertParityUntilInterruption(offsets)
        val start = offsets[InterruptFrame].lazy!!
        val target = IntOffset(-DefaultItemSize.width, 0)
        for (step in 1 until DurationFrames) {
            assertWithMessage("Lazy offset $step frames after interruption")
                .that(offsets[InterruptFrame + step].lazy)
                .isEqualTo(lerp(start, target, step / DurationFrames.toFloat()))
        }
        assertWithMessage("Lazy item should be removed once the exit spec finishes")
            .that(offsets[InterruptFrame + DurationFrames].lazy)
            .isNull()
        val animatedVisibilityRemovedFrame =
            (InterruptFrame + 1..InterruptFrame + DurationFrames).first {
                offsets[it].animatedVisibility == null
            }
        assertWithMessage("AnimatedVisibility should remove its content before the lazy item")
            .that(animatedVisibilityRemovedFrame)
            .isLessThan(InterruptFrame + DurationFrames)
    }

    @Test
    fun exitInterruptedByEnter_lazyKeepsEnterSpec_animatedVisibilityKeepsMomentum() {
        setParityContent(
            initiallyVisible = true,
            enter = slideInHorizontally(LinearSpec) { it },
            exit = slideOutHorizontally(LinearSpec) { it },
        )

        val offsets = recordInterruptedOffsets(InterruptFrame, InterruptFrame + TotalFrames)

        assertParityUntilInterruption(offsets)
        val start = offsets[InterruptFrame].lazy!!
        for (step in 1..DurationFrames) {
            assertWithMessage("Lazy offset $step frames after interruption")
                .that(offsets[InterruptFrame + step].lazy)
                .isEqualTo(lerp(start, IntOffset.Zero, step / DurationFrames.toFloat()))
        }
        assertWithMessage("AnimatedVisibility should keep moving outwards after interruption")
            .that(offsets[InterruptFrame + 1].animatedVisibility!!.x)
            .isGreaterThan(start.x)
        assertWithMessage("AnimatedVisibility should still be settling when the lazy item rests")
            .that(offsets[InterruptFrame + DurationFrames].animatedVisibility)
            .isNotEqualTo(IntOffset.Zero)
    }

    @Test
    fun enterInterruptedByExit_onFirstFrame() {
        setParityContent(
            initiallyVisible = false,
            enter = slideInHorizontally(LinearSpec) { -it },
            exit = slideOutHorizontally(LinearSpec) { -it },
        )

        toggleAndAssertParity(TotalFrames) { frame -> if (frame == 1) visible = !visible }
    }

    @Test
    fun enter_noTransitions() {
        assertEnterParity(EnterTransition.None)
    }

    @Test
    fun exit_noTransitions_lazyItemIsRemovedOneFrameBeforeAnimatedVisibility() {
        setParityContent(
            initiallyVisible = true,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        )

        rule.runOnUiThread { visible = false }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        assertWithMessage("Lazy item should be removed on the first frame")
            .that(isBlank(LazyTag))
            .isTrue()
        assertWithMessage("AnimatedVisibility content should still be shown on the first frame")
            .that(isBlank(AnimatedVisibilityTag))
            .isFalse()

        runFramesAndAssertParity(TotalFrames)
    }

    private class FrameOffsets(val lazy: IntOffset?, val animatedVisibility: IntOffset?)

    private fun recordInterruptedOffsets(interruptFrame: Int, frames: Int): List<FrameOffsets> {
        val offsets = mutableListOf<FrameOffsets>()
        for (frame in 0..frames) {
            rule.runOnUiThread { if (frame == 0 || frame == interruptFrame) visible = !visible }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            offsets += FrameOffsets(itemOffset(LazyTag), itemOffset(AnimatedVisibilityTag))
        }
        return offsets
    }

    private fun assertParityUntilInterruption(offsets: List<FrameOffsets>) {
        for (frame in 0..InterruptFrame) {
            assertWithMessage("Frame $frame before interruption")
                .that(offsets[frame].lazy)
                .isEqualTo(offsets[frame].animatedVisibility)
        }
    }

    private fun itemOffset(tag: String): IntOffset? {
        val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map[x, y] == Color.Red) {
                    return IntOffset(x - DefaultItemSize.width, y - DefaultItemSize.height)
                }
            }
        }
        return null
    }

    private fun isBlank(tag: String): Boolean {
        val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map[x, y] != Color.White) return false
            }
        }
        return true
    }

    private fun assertEnterParity(
        enter: EnterTransition,
        itemSize: IntSize = DefaultItemSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        frames: Int = TotalFrames,
    ) {
        setParityContent(
            initiallyVisible = false,
            enter = enter,
            exit = ExitTransition.None,
            itemSize = itemSize,
            layoutDirection = layoutDirection,
        )

        toggleAndAssertParity(frames)
    }

    private fun assertExitParity(
        exit: ExitTransition,
        itemSize: IntSize = DefaultItemSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        frames: Int = TotalFrames,
    ) {
        setParityContent(
            initiallyVisible = true,
            enter = EnterTransition.None,
            exit = exit,
            itemSize = itemSize,
            layoutDirection = layoutDirection,
        )

        toggleAndAssertParity(frames)
    }

    private fun setParityContent(
        initiallyVisible: Boolean,
        enter: EnterTransition,
        exit: ExitTransition,
        itemSize: IntSize = DefaultItemSize,
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(horizontal = itemWidth, vertical = itemHeight),
                        ) {
                            if (isVisible) {
                                item(key = ItemKey) {
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
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Green))
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
            }
        }
    }

    private fun toggleAndAssertParity(frames: Int, onFrame: (frame: Int) -> Unit = {}) {
        runFramesAndAssertParity(frames) { frame ->
            if (frame == 0) visible = !visible
            onFrame(frame)
        }
    }

    private fun runFramesAndAssertParity(frames: Int, onFrame: (frame: Int) -> Unit = {}) {
        for (frame in 0..frames) {
            rule.runOnUiThread { onFrame(frame) }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            assertFrameParity(frame)
        }
    }

    private fun assertFrameParity(frame: Int) {
        val lazy = rule.onNodeWithTag(LazyTag).captureToImage().toPixelMap()
        val animatedVisibility =
            rule.onNodeWithTag(AnimatedVisibilityTag).captureToImage().toPixelMap()

        assertWithMessage("Frame $frame: captured image sizes differ")
            .that(IntSize(lazy.width, lazy.height))
            .isEqualTo(IntSize(animatedVisibility.width, animatedVisibility.height))

        var mismatches = 0
        var firstMismatch: String? = null
        for (y in 0 until lazy.height) {
            for (x in 0 until lazy.width) {
                val lazyColor = lazy[x, y]
                val animatedVisibilityColor = animatedVisibility[x, y]
                if (lazyColor != animatedVisibilityColor) {
                    mismatches++
                    if (firstMismatch == null) {
                        firstMismatch =
                            "($x, $y) lazy=$lazyColor animatedVisibility=$animatedVisibilityColor"
                    }
                }
            }
        }

        assertWithMessage(
                "Frame $frame: $mismatches of ${lazy.width * lazy.height} pixels differ, " +
                    "first mismatch at $firstMismatch"
            )
            .that(mismatches)
            .isEqualTo(0)
    }
}

private const val FrameDurationMillis = 16
private const val DurationFrames = 10
private const val InterruptFrame = DurationFrames / 2
private const val DurationMillis = FrameDurationMillis * DurationFrames
private const val TotalFrames = DurationFrames + 4
private const val DelayFrames = 4
private const val DelayedFrames = TotalFrames + DelayFrames
private const val SpringFrames = 60
private const val ContainerScale = 3
private const val ItemKey = 0
private const val LazyTag = "lazy"
private const val AnimatedVisibilityTag = "animatedVisibility"
private val DefaultItemSize = IntSize(20, 12)
private val OddItemSize = IntSize(21, 13)
private val LinearSpec: FiniteAnimationSpec<IntOffset> =
    tween(DurationMillis, easing = LinearEasing)
private val EasedSpec: FiniteAnimationSpec<IntOffset> =
    tween(DurationMillis, easing = FastOutSlowInEasing)
private val DelayedSpec: FiniteAnimationSpec<IntOffset> =
    tween(
        DurationMillis,
        delayMillis = FrameDurationMillis * DelayFrames,
        easing = LinearEasing,
    )
private val SpringSpec: FiniteAnimationSpec<IntOffset> =
    spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
private val KeyframesSpec: FiniteAnimationSpec<IntOffset> = keyframes {
    durationMillis = DurationMillis
    IntOffset(0, 0) at DurationMillis / 4
    IntOffset(5, 5) at DurationMillis / 2
}
