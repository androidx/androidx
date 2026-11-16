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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyLayoutScaleAnimationParityTest : BaseLazyLayoutAnimationParityTest(DefaultItemSize) {

    @Test
    fun scaleIn_default() {
        assertEnterParity(scaleIn(LinearSpec))
    }

    @Test
    fun scaleOut_default() {
        assertExitParity(scaleOut(LinearSpec))
    }

    @Test
    fun scaleIn_partialInitialScale() {
        assertEnterParity(scaleIn(LinearSpec, initialScale = 0.5f))
    }

    @Test
    fun scaleOut_partialTargetScale() {
        assertExitParity(scaleOut(LinearSpec, targetScale = 0.5f))
    }

    @Test
    fun scaleIn_initialScaleLargerThanOne() {
        assertEnterParity(scaleIn(LinearSpec, initialScale = 2f))
    }

    @Test
    fun scaleOut_targetScaleLargerThanOne() {
        assertExitParity(scaleOut(LinearSpec, targetScale = 2f))
    }

    @Test
    fun scaleIn_initialScaleLargerThanContainer() {
        assertEnterParity(scaleIn(LinearSpec, initialScale = 5f))
    }

    @Test
    fun scaleOut_targetScaleLargerThanContainer() {
        assertExitParity(scaleOut(LinearSpec, targetScale = 5f))
    }

    @Test
    fun scaleIn_initialScaleOne() {
        assertEnterParity(scaleIn(LinearSpec, initialScale = 1f))
    }

    @Test
    fun scaleOut_targetScaleOne() {
        assertExitParity(scaleOut(LinearSpec, targetScale = 1f))
    }

    @Test
    fun scaleIn_fromTopStart() {
        assertEnterParity(scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)))
    }

    @Test
    fun scaleIn_fromBottomEnd() {
        assertEnterParity(scaleIn(LinearSpec, transformOrigin = TransformOrigin(1f, 1f)))
    }

    @Test
    fun scaleOut_toTopStart() {
        assertExitParity(scaleOut(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)))
    }

    @Test
    fun scaleOut_toBottomEnd() {
        assertExitParity(scaleOut(LinearSpec, transformOrigin = TransformOrigin(1f, 1f)))
    }

    @Test
    fun scaleIn_partialTransformOrigin() {
        assertEnterParity(scaleIn(LinearSpec, transformOrigin = TransformOrigin(0.25f, 0.75f)))
    }

    @Test
    fun scaleOut_partialTransformOrigin() {
        assertExitParity(scaleOut(LinearSpec, transformOrigin = TransformOrigin(0.75f, 0.25f)))
    }

    @Test
    fun scaleIn_transformOriginOutsideOfItem() {
        assertEnterParity(scaleIn(LinearSpec, transformOrigin = TransformOrigin(-0.5f, 1.5f)))
    }

    @Test
    fun scaleOut_transformOriginOutsideOfItem() {
        assertExitParity(scaleOut(LinearSpec, transformOrigin = TransformOrigin(1.5f, -0.5f)))
    }

    @Test
    fun scaleIn_oddItemSize() {
        assertEnterParity(scaleIn(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun scaleOut_oddItemSize() {
        assertExitParity(scaleOut(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun scaleIn_nonLinearEasing() {
        assertEnterParity(scaleIn(EasedSpec))
    }

    @Test
    fun scaleOut_nonLinearEasing() {
        assertExitParity(scaleOut(EasedSpec))
    }

    @Test
    fun scaleIn_springSpec() {
        assertEnterParity(
            scaleIn(SpringSpec),
            frames = SpringFrames,
        )
    }

    @Test
    fun scaleOut_springSpec() {
        assertExitParity(
            scaleOut(SpringSpec),
            frames = SpringFrames,
        )
    }

    @Test
    fun scaleIn_delayedTween() {
        assertEnterParity(scaleIn(DelayedSpec), frames = DelayedFrames)
    }

    @Test
    fun scaleOut_delayedTween() {
        assertExitParity(scaleOut(DelayedSpec), frames = DelayedFrames)
    }

    @Test
    fun scaleIn_keyframes() {
        assertEnterParity(scaleIn(KeyframesSpec))
    }

    @Test
    fun scaleOut_keyframes() {
        assertExitParity(scaleOut(KeyframesSpec))
    }

    @Test
    fun scaleIn_zeroDuration() {
        assertEnterParity(scaleIn(tween(0)))
    }

    @Test
    fun scaleOut_zeroDuration() {
        assertExitParity(scaleOut(tween(0)))
    }

    @Test
    fun scaleIn_snapSpec() {
        assertEnterParity(scaleIn(snap()))
    }

    @Test
    fun scaleOut_snapSpec() {
        assertExitParity(scaleOut(snap()))
    }

    @Test
    fun scaleIn_rtl() {
        assertEnterParity(
            scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun scaleOut_rtl() {
        assertExitParity(
            scaleOut(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun scaleInCombinedWithFadeIn() {
        assertEnterParity(
            fadeIn(LinearSpec) + scaleIn(LinearSpec),
            itemSize = WholePixelItemSize,
            maxChannelDifference = FadeWithScaleChannelTolerance,
        )
    }

    @Test
    fun scaleOutCombinedWithFadeOut() {
        assertExitParity(
            fadeOut(LinearSpec) + scaleOut(LinearSpec),
            itemSize = WholePixelItemSize,
            maxChannelDifference = FadeWithScaleChannelTolerance,
        )
    }

    @Test
    fun scaleInCombinedWithSlideIn() {
        assertEnterParity(
            slideInHorizontally(IntOffsetLinearSpec) { -it } +
                scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f))
        )
    }

    @Test
    fun scaleOutCombinedWithSlideOut() {
        assertExitParity(
            slideOutVertically(IntOffsetLinearSpec) { it } +
                scaleOut(LinearSpec, transformOrigin = TransformOrigin(1f, 1f))
        )
    }

    @Test
    fun noAnimation_initiallyVisible() {
        setParityContent(
            initiallyVisible = true,
            enter = scaleIn(LinearSpec),
            exit = scaleOut(LinearSpec),
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun noAnimation_initiallyHidden() {
        setParityContent(
            initiallyVisible = false,
            enter = scaleIn(LinearSpec),
            exit = scaleOut(LinearSpec),
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun enterThenExit_fullCycle() {
        setParityContent(
            initiallyVisible = false,
            enter = scaleIn(LinearSpec, initialScale = 0.25f),
            exit = scaleOut(LinearSpec, targetScale = 1.5f),
        )

        toggleAndAssertParity(TotalFrames)
        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun exitThenEnter_fullCycle() {
        setParityContent(
            initiallyVisible = true,
            enter = scaleIn(LinearSpec, initialScale = 1.5f),
            exit = scaleOut(LinearSpec, targetScale = 0.25f),
        )

        toggleAndAssertParity(TotalFrames)
        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun enter_usesEnterTransformOrigin_whenExitHasADifferentOne() {
        setParityContent(
            initiallyVisible = false,
            enter = scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            exit = scaleOut(LinearSpec, transformOrigin = TransformOrigin(1f, 1f)),
        )

        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun exit_usesExitTransformOrigin_whenEnterHasADifferentOne() {
        setParityContent(
            initiallyVisible = true,
            enter = scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            exit = scaleOut(LinearSpec, transformOrigin = TransformOrigin(1f, 1f)),
        )

        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun enterInterruptedByExit_lazyKeepsExitSpec() {
        setParityContent(
            initiallyVisible = false,
            enter = scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            exit = scaleOut(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
        )

        val widths = recordInterruptedWidths()

        assertParityUntilInterruption(widths)
        val start = widths[InterruptFrame].lazy!!.toFloat()
        for (step in 1 until DurationFrames) {
            val expected = lerp(start, 0f, step / DurationFrames.toFloat())
            assertWithMessage("Lazy width $step frames after interruption")
                .that(abs((widths[InterruptFrame + step].lazy ?: 0) - expected))
                .isAtMost(WidthTolerance)
        }
        assertWithMessage("Lazy item should be removed once the exit spec finishes")
            .that(widths[InterruptFrame + DurationFrames].lazy)
            .isNull()
    }

    @Test
    fun exitInterruptedByEnter_lazyKeepsEnterSpec_animatedVisibilityKeepsMomentum() {
        setParityContent(
            initiallyVisible = true,
            enter = scaleIn(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
            exit = scaleOut(LinearSpec, transformOrigin = TransformOrigin(0f, 0f)),
        )

        val widths = recordInterruptedWidths()

        assertParityUntilInterruption(widths)
        val start = widths[InterruptFrame].lazy!!
        val itemWidth = DefaultItemSize.width.toFloat()
        for (step in 1..DurationFrames) {
            val expected = lerp(start.toFloat(), itemWidth, step / DurationFrames.toFloat())
            assertWithMessage("Lazy width $step frames after interruption")
                .that(abs(widths[InterruptFrame + step].lazy!! - expected))
                .isAtMost(WidthTolerance)
        }
        assertWithMessage("AnimatedVisibility should keep shrinking after interruption")
            .that(widths[InterruptFrame + 1].animatedVisibility!!)
            .isLessThan(start)
        assertWithMessage("AnimatedVisibility should still be settling when the lazy item rests")
            .that(widths[InterruptFrame + DurationFrames].animatedVisibility)
            .isNotEqualTo(DefaultItemSize.width)
    }

    @Test
    fun enterInterruptedByExit_onFirstFrame() {
        setParityContent(
            initiallyVisible = false,
            enter = scaleIn(LinearSpec),
            exit = scaleOut(LinearSpec),
        )

        toggleAndAssertParity(TotalFrames) { frame -> if (frame == 1) visible = !visible }
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

    private fun recordInterruptedWidths() =
        recordInterruptedFrames(InterruptFrame, InterruptFrame + TotalFrames, ::itemWidth)

    private fun itemWidth(tag: String): Int? {
        val map = capture(tag)
        var left = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map[x, y] != Color.White) {
                    left = minOf(left, x)
                    right = maxOf(right, x)
                }
            }
        }
        return if (left > right) null else right - left + 1
    }
}

private const val WidthTolerance = 2f
private val DefaultItemSize = IntSize(40, 24)
private val OddItemSize = IntSize(41, 23)
private val WholePixelItemSize = IntSize(40, 20)
private const val FadeWithScaleChannelTolerance = 2
private val LinearSpec: FiniteAnimationSpec<Float> = linearSpec()
private val IntOffsetLinearSpec: FiniteAnimationSpec<IntOffset> = linearSpec()
private val EasedSpec: FiniteAnimationSpec<Float> = easedSpec()
private val DelayedSpec: FiniteAnimationSpec<Float> = delayedSpec()
private val SpringSpec: FiniteAnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
private val KeyframesSpec: FiniteAnimationSpec<Float> = keyframes {
    durationMillis = DurationMillis
    0.5f at DurationMillis / 4
    0.25f at DurationMillis / 2
}
