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

package androidx.wear.compose.foundation.pager

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.pager.PagerLayoutInfo as ComposePagerLayoutInfo
import androidx.compose.foundation.pager.PagerState as ComposePagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates and remember a [PagerState] to be used with a Wear Pager
 *
 * @param initialPage The page that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount The number of pages this Pager will have.
 */
@Composable
public fun rememberPagerState(
    @IntRange(from = 0) initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    @IntRange(from = 1) pageCount: () -> Int,
): PagerState {
    return rememberSaveable(saver = PagerState.Saver) {
            PagerState(initialPage, initialPageOffsetFraction, pageCount)
        }
        .apply { pagerState.pageCountState.value = pageCount }
}

/**
 * The state that can be used in conjunction with Wear [HorizontalPager] and [VerticalPager].
 *
 * @param currentPage The index of the current active page.
 * @param currentPageOffsetFraction The fractional offset from the start of the current page. Should
 *   be between -0.5 and 0.5, where 0 indicates the start of the initial page.
 * @param pageCount The number of pages in this Pager.
 */
public class PagerState(
    @IntRange(from = 0) currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    @IntRange(from = 1) pageCount: () -> Int,
) : ScrollableState {
    internal var pagerState = PagerStateImpl(currentPage, currentPageOffsetFraction, pageCount)

    /** The current page displayed by the pager. */
    public val currentPage: Int
        get() = pagerState.currentPage

    /**
     * The fractional offset from the start of the current page, in the range [-0.5,0.5], where 0
     * indicates the start of the current page
     */
    public val currentPageOffsetFraction: Float
        // @FrequentlyChangingValue
        get() = pagerState.currentPageOffsetFraction

    /** The total number of pages present in this pager. */
    public val pageCount: Int
        get() = pagerState.pageCount

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     */
    public val settledPage: Int
        get() = pagerState.settledPage

    /**
     * The page this pager intends to settle to. During fling or animated scroll (from
     * [animateScrollToPage]) this will represent the page this pager intends to settle to. When no
     * scroll is ongoing, this will be equal to [currentPage].
     */
    public val targetPage: Int
        get() = pagerState.targetPage

    /**
     * A [PagerLayoutInfo] that contains useful information about the Pager's last layout pass. For
     * instance, you can query the page size.
     *
     * This property is observable and is updated after every scroll or remeasure. If you use it in
     * the composable function it will be recomposed on every change causing potential performance
     * issues including infinity recomposition loop. Therefore, avoid using it in the composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow".
     */
    public val layoutInfo: PagerLayoutInfo
        get() = PagerLayoutInfoImpl(pagerState.layoutInfo)

    override val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress

    override fun dispatchRawDelta(delta: Float): Float {
        return pagerState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        pagerState.scroll(scrollPriority, block)
    }

    /**
     * Scroll (jump immediately) to a given [page].
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     */
    public suspend fun scrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
    ): Unit = pagerState.scrollToPage(page, pageOffsetFraction)

    /**
     * Scroll animate to a given [page]. If the [page] is too far away from [currentPage] we will
     * not compose all pages in the way. We will pre-jump to a nearer page, compose and animate the
     * rest of the pages until [page].
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     *   default animation.
     */
    public suspend fun animateScrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        pagerState.animateScrollToPage(page, pageOffsetFraction, animationSpec)
    }

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    public val interactionSource: InteractionSource
        get() = pagerState.interactionSource

    public companion object {
        /** To keep current page and page offset saved */
        public val Saver: Saver<PagerState, *> =
            listSaver(
                save = {
                    listOf(
                        it.pagerState.currentPage,
                        it.pagerState.currentPageOffsetFraction,
                        it.pagerState.pageCount,
                    )
                },
                restore = {
                    PagerState(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        pageCount = { it[2] as Int },
                    )
                },
            )
    }
}

internal class PagerStateImpl(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
) : ComposePagerState(currentPage, currentPageOffsetFraction) {
    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()

    companion object {
        /** To keep current page and current page offset saved */
        val Saver: Saver<PagerStateImpl, *> =
            listSaver(
                save = {
                    listOf(
                        it.currentPage,
                        (it.currentPageOffsetFraction).coerceIn(MinPageOffset, MaxPageOffset),
                        it.pageCount,
                    )
                },
                restore = {
                    PagerStateImpl(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        updatedPageCount = { it[2] as Int },
                    )
                },
            )
    }
}

internal class PagerLayoutInfoImpl(val layoutInfo: ComposePagerLayoutInfo) : PagerLayoutInfo {
    override val pageSize = layoutInfo.pageSize
    override val orientation = layoutInfo.orientation

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is PagerLayoutInfoImpl) return false

        if (pageSize != other.pageSize) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageSize.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }
}

internal const val MinPageOffset = -0.5f
internal const val MaxPageOffset = 0.5f
