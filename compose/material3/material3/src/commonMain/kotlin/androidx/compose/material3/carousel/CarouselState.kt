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

package androidx.compose.material3.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * The state that can be used to control all types of carousels.
 *
 * @param currentItem the current item to be scrolled to.
 * @param currentItemOffsetFraction the current item offset as a fraction of the item size.
 * @param itemCount the number of items this Carousel will have.
 */
// TODO: b/321997456 - Remove lint suppression once version checks are added in lint or library
// moves to beta
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterial3Api
internal class CarouselState(
    currentItem: Int = 0,
    currentItemOffsetFraction: Float = 0F,
    itemCount: () -> Int
) : ScrollableState {
    var itemCountState = mutableStateOf(itemCount)

    internal var pagerState: PagerState = PagerState(currentItem, currentItemOffsetFraction,
        itemCountState.value)

    override val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress

    override fun dispatchRawDelta(delta: Float): Float {
        return pagerState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        pagerState.scroll(scrollPriority, block)
    }

    companion object {
        /**
         * To keep current item and item offset saved
         */
        val Saver: Saver<CarouselState, *> = listSaver(
            save = {
                listOf(
                    it.pagerState.currentPage,
                    it.pagerState.currentPageOffsetFraction,
                    it.pagerState.pageCount,
                )
            },
            restore = {
                CarouselState(
                    currentItem = it[0] as Int,
                    currentItemOffsetFraction = it[1] as Float,
                    itemCount = { it[2] as Int },
                )
            }
        )
    }
}

/**
 * Creates a [CarouselState] that is remembered across compositions.
 *
 * @param initialItem The initial item that should be scrolled to.
 * @param itemCount The number of items this Carousel will have.
 */
@ExperimentalMaterial3Api
@Composable
internal fun rememberCarouselState(
    initialItem: Int = 0,
    itemCount: () -> Int,
): CarouselState {
    return rememberSaveable(saver = CarouselState.Saver) {
        CarouselState(
            currentItem = initialItem,
            currentItemOffsetFraction = 0F,
            itemCount = itemCount
        )
    }.apply {
        itemCountState.value = itemCount
    }
}
