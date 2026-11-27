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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.DurationMillis
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.delayedSpec
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.easedSpec
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutAnimationParityTest.Companion.linearSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LazyLayoutChangeSizeAnimationParityTest : BaseLazyLayoutAnimationParityTest(DefaultItemSize) {

    @Test
    fun expandIn_default() {
        assertEnterParity(expandIn(LinearSpec))
    }

    @Test
    fun expandVertically_default() {
        assertEnterParity(expandVertically(LinearSpec))
    }

    @Test
    fun expandHorizontally_default() {
        assertEnterParity(expandHorizontally(LinearSpec))
    }

    @Test
    fun expandIn_fromTopStart() {
        assertEnterParity(expandIn(LinearSpec, expandFrom = Alignment.TopStart))
    }

    @Test
    fun expandIn_fromCenter() {
        assertEnterParity(expandIn(LinearSpec, expandFrom = Alignment.Center))
    }

    @Test
    fun expandIn_fromBottomCenter() {
        assertEnterParity(expandIn(LinearSpec, expandFrom = Alignment.BottomCenter))
    }

    @Test
    fun expandVertically_fromTop() {
        assertEnterParity(expandVertically(LinearSpec, expandFrom = Alignment.Top))
    }

    @Test
    fun expandVertically_fromCenter() {
        assertEnterParity(expandVertically(LinearSpec, expandFrom = Alignment.CenterVertically))
    }

    @Test
    fun expandHorizontally_fromStart() {
        assertEnterParity(expandHorizontally(LinearSpec, expandFrom = Alignment.Start))
    }

    @Test
    fun expandHorizontally_fromCenter() {
        assertEnterParity(expandHorizontally(LinearSpec, expandFrom = Alignment.CenterHorizontally))
    }

    @Test
    fun expandIn_partialInitialSize() {
        assertEnterParity(expandIn(LinearSpec) { IntSize(it.width / 2, it.height / 4) })
    }

    @Test
    fun expandVertically_partialInitialHeight() {
        assertEnterParity(expandVertically(LinearSpec) { it / 3 })
    }

    @Test
    fun expandHorizontally_partialInitialWidth() {
        assertEnterParity(expandHorizontally(LinearSpec) { it / 3 })
    }

    @Test
    fun expandIn_initialSizeIsFullSize() {
        assertEnterParity(expandIn(LinearSpec) { it })
    }

    @Test
    fun expandIn_withoutClip() {
        assertEnterParity(expandIn(LinearSpec, clip = false))
    }

    @Test
    fun expandIn_withoutClip_fromTopStart() {
        assertEnterParity(expandIn(LinearSpec, expandFrom = Alignment.TopStart, clip = false))
    }

    @Test
    fun expandIn_oddItemSize() {
        assertEnterParity(expandIn(LinearSpec), itemSize = OddItemSize)
    }

    @Test
    fun expandIn_fromCenter_oddItemSize() {
        assertEnterParity(
            expandIn(LinearSpec, expandFrom = Alignment.Center),
            itemSize = OddItemSize,
        )
    }

    @Test
    fun expandIn_nonLinearEasing() {
        assertEnterParity(expandIn(EasedSpec))
    }

    @Test
    fun expandHorizontally_springSpec() {
        assertEnterParity(expandHorizontally(SpringSpec), frames = SpringFrames)
    }

    @Test
    fun expandIn_delayedTween() {
        assertEnterParity(expandIn(DelayedSpec), frames = DelayedFrames)
    }

    @Test
    fun expandIn_keyframes() {
        assertEnterParity(expandIn(KeyframesSpec))
    }

    @Test
    fun expandIn_zeroDuration() {
        assertEnterParity(expandIn(tween(0)))
    }

    @Test
    fun expandIn_snapSpec() {
        assertEnterParity(expandIn(snap()))
    }

    @Test
    fun expandIn_rtl() {
        assertEnterParity(
            expandIn(LinearSpec, expandFrom = Alignment.TopStart),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun expandHorizontally_fromStart_rtl() {
        assertEnterParity(
            expandHorizontally(LinearSpec, expandFrom = Alignment.Start),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun expandHorizontally_fromEnd_rtl() {
        assertEnterParity(
            expandHorizontally(LinearSpec, expandFrom = Alignment.End),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun expandVertically_pushesTrailingItem() {
        assertEnterParity(expandVertically(LinearSpec), withTrailingItem = true)
    }

    @Test
    fun expandVertically_fromTop_pushesTrailingItem() {
        assertEnterParity(
            expandVertically(LinearSpec, expandFrom = Alignment.Top),
            withTrailingItem = true,
        )
    }

    @Test
    fun expandVertically_partialInitialHeight_pushesTrailingItem() {
        assertEnterParity(expandVertically(LinearSpec) { it / 2 }, withTrailingItem = true)
    }

    @Test
    fun expandVertically_withoutClip_overlapsTrailingItem() {
        assertEnterParity(expandVertically(LinearSpec, clip = false), withTrailingItem = true)
    }

    @Test
    fun expandVertically_springSpec_pushesTrailingItem() {
        assertEnterParity(
            expandVertically(SpringSpec),
            frames = SpringFrames,
            withTrailingItem = true,
        )
    }

    @Test
    fun expandHorizontally_doesNotMoveTrailingItem() {
        assertEnterParity(expandHorizontally(LinearSpec), withTrailingItem = true)
    }

    @Test
    fun expandInCombinedWithFadeIn() {
        assertEnterParity(
            fadeIn(FloatLinearSpec) + expandIn(LinearSpec),
            maxChannelDifference = FadeChannelTolerance,
        )
    }

    @Test
    fun expandInCombinedWithSlideIn_slideIsClippedToExpandedBounds() {
        assertEnterParity(
            slideInHorizontally(IntOffsetLinearSpec) { -it } +
                expandIn(LinearSpec, expandFrom = Alignment.TopStart)
        )
    }

    @Test
    fun expandVerticallyCombinedWithSlideInVertically() {
        assertEnterParity(
            slideInVertically(IntOffsetLinearSpec) { -it / 2 } + expandVertically(LinearSpec),
            withTrailingItem = true,
        )
    }

    @Test
    fun noAnimation_initiallyVisible() {
        setParityContent(
            initiallyVisible = true,
            enter = expandIn(LinearSpec),
            exit = fadeOut(FloatLinearSpec),
            withTrailingItem = true,
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun noAnimation_initiallyHidden() {
        setParityContent(
            initiallyVisible = false,
            enter = expandIn(LinearSpec),
            exit = fadeOut(FloatLinearSpec),
            withTrailingItem = true,
        )

        runFramesAndAssertParity(TotalFrames)
    }

    @Test
    fun enterThenExit_fullCycle() {
        setParityContent(
            initiallyVisible = false,
            enter = expandIn(LinearSpec),
            exit = fadeOut(FloatLinearSpec),
        )

        toggleAndAssertParity(TotalFrames)
        toggleAndAssertParity(TotalFrames)
    }

    @Test
    fun expandVertically_itemGrowsWhileExpanding_expansionIsRetargeted() {
        assertExpandParityWhenItemIsResized(
            expandVertically(LinearSpec),
            initialSize = IntSize(DefaultItemSize.width, DefaultItemSize.height / 2),
            newSize = DefaultItemSize,
        )
    }

    @Test
    fun expandVertically_itemShrinksWhileExpanding_expansionIsRetargeted() {
        assertExpandParityWhenItemIsResized(
            expandVertically(LinearSpec),
            initialSize = DefaultItemSize,
            newSize = IntSize(DefaultItemSize.width, DefaultItemSize.height / 4),
        )
    }

    @Test
    fun expandIn_itemGrowsOnBothAxesWhileExpanding_expansionIsRetargeted() {
        assertExpandParityWhenItemIsResized(
            expandIn(LinearSpec),
            initialSize = IntSize(DefaultItemSize.width / 2, DefaultItemSize.height / 2),
            newSize = DefaultItemSize,
        )
    }

    @Test
    fun expandVertically_itemShrinksWhileExpanding_trailingItemFollows() {
        assertExpandParityWhenItemIsResized(
            expandVertically(LinearSpec),
            initialSize = DefaultItemSize,
            newSize = IntSize(DefaultItemSize.width, DefaultItemSize.height / 4),
            withTrailingItem = true,
        )
    }

    @Test
    fun expandVertically_itemResizedTwiceWhileExpanding_expansionIsRetargetedTwice() {
        setParityContent(
            initiallyVisible = false,
            enter = expandVertically(LinearSpec),
            exit = ExitTransition.None,
        )

        toggleAndAssertParity(TotalFrames * 3) { frame ->
            if (frame == InterruptFrame) {
                resizedItemSize = IntSize(DefaultItemSize.width, DefaultItemSize.height / 4)
            }
            if (frame == InterruptFrame + 2) resizedItemSize = DefaultItemSize
        }
    }

    @Test
    fun expandVertically_itemResizedAfterExpanding_takesItsNewSizeAtOnce() {
        assertExpandParityWhenItemIsResized(
            expandVertically(LinearSpec),
            initialSize = IntSize(DefaultItemSize.width, DefaultItemSize.height / 2),
            newSize = DefaultItemSize,
            resizeFrame = TotalFrames,
        )
    }

    private fun assertExpandParityWhenItemIsResized(
        enter: EnterTransition,
        initialSize: IntSize,
        newSize: IntSize,
        resizeFrame: Int = InterruptFrame,
        frames: Int = TotalFrames * 3,
        withTrailingItem: Boolean = false,
    ) {
        resizedItemSize = initialSize
        setParityContent(
            initiallyVisible = false,
            enter = enter,
            exit = ExitTransition.None,
            withTrailingItem = withTrailingItem,
        )

        toggleAndAssertParity(frames) { frame ->
            if (frame == resizeFrame) resizedItemSize = newSize
        }
    }

    @Composable
    override fun LazyLayoutUnderTest(
        visible: Boolean,
        contentPadding: PaddingValues,
        trailingItemContent: (@Composable () -> Unit)?,
        itemContent: @Composable () -> Unit,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            if (trailingItemContent != null) item(key = LeadingItemKey) {}
            if (visible) item(key = ItemKey) { itemContent() }
            if (trailingItemContent != null) item(key = TrailingItemKey) { trailingItemContent() }
        }
    }
}

private const val LeadingItemKey = -1
private val DefaultItemSize = IntSize(20, 12)
private val OddItemSize = IntSize(21, 13)
private const val FadeChannelTolerance = 2
private val LinearSpec: FiniteAnimationSpec<IntSize> = linearSpec()
private val FloatLinearSpec: FiniteAnimationSpec<Float> = linearSpec()
private val IntOffsetLinearSpec: FiniteAnimationSpec<IntOffset> = linearSpec()
private val EasedSpec: FiniteAnimationSpec<IntSize> = easedSpec()
private val DelayedSpec: FiniteAnimationSpec<IntSize> = delayedSpec()
private val SpringSpec: FiniteAnimationSpec<IntSize> =
    spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )
private val KeyframesSpec: FiniteAnimationSpec<IntSize> = keyframes {
    durationMillis = DurationMillis
    IntSize(4, 2) at DurationMillis / 4
    IntSize(16, 4) at DurationMillis / 2
}
