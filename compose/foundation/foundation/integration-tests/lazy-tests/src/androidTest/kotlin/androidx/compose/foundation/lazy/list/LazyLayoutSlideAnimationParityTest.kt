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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.DurationMillis
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.delayedSpec
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.easedSpec
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.linearSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.lerp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyLayoutSlideAnimationParityTest : BaseLazyLayoutAnimationParityTest(DefaultItemSize) {

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

        val offsets = recordInterruptedOffsets()

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

        val offsets = recordInterruptedOffsets()

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

    @Composable
    override fun LazyLayoutUnderTest(
        visible: Boolean,
        contentPadding: PaddingValues,
        itemContent: @Composable () -> Unit,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            if (visible) item(key = ItemKey) { itemContent() }
        }
    }

    private fun recordInterruptedOffsets() =
        recordInterruptedFrames(InterruptFrame, InterruptFrame + TotalFrames, ::itemOffset)

    private fun itemOffset(tag: String): IntOffset? {
        val map = capture(tag)
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map[x, y] == Color.Red) {
                    return IntOffset(x - DefaultItemSize.width, y - DefaultItemSize.height)
                }
            }
        }
        return null
    }
}

private val DefaultItemSize = IntSize(20, 12)
private val OddItemSize = IntSize(21, 13)
private val LinearSpec: FiniteAnimationSpec<IntOffset> = linearSpec()
private val EasedSpec: FiniteAnimationSpec<IntOffset> = easedSpec()
private val DelayedSpec: FiniteAnimationSpec<IntOffset> = delayedSpec()
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
