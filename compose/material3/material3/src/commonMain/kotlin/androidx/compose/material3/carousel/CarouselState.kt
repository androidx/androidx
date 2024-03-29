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

import androidx.annotation.FloatRange
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * The state that can be used to control all types of carousels.
 *
 * @param currentItem the current item to be scrolled to.
 * @param currentItemOffsetFraction the offset of the current item as a fraction of the item's size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the current item from the
 * snapped position.
 * @param itemCount the number of items this Carousel will have.
 */
@ExperimentalMaterial3Api
class CarouselState(
    currentItem: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentItemOffsetFraction: Float = 0f,
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

    @ExperimentalMaterial3Api
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
fun rememberCarouselState(
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

/**
 * Interface to hold information about a Carousel item and its size.
 *
 * Example of CarouselItemInfo usage:
 * @sample androidx.compose.material3.samples.FadingHorizontalMultiBrowseCarouselSample
 */
@ExperimentalMaterial3Api
sealed interface CarouselItemInfo {

    /** The size of the carousel item in the main axis */
    val size: Float

    /**
     * The minimum size in the main axis of the carousel item, eg. the size of the item when it
     * scrolls off the sides of the carousel
     */
    val minSize: Float

    /**
     * The maximum size in the main axis of the carousel item, eg. the size of the item when it is
     * at a focal position
     */
    val maxSize: Float
}

/**
 * Gets the start offset of the carousel item from its full size. This offset can be used to pin any
 * carousel item content to the left side of the item (or right if RTL).
 */
@OptIn(ExperimentalMaterial3Api::class)
fun CarouselItemInfo.startOffset(): Float {
    return (maxSize - size) / 2f
}

/**
 * Gets the end offset of the carousel item from its full size. This offset can be used to pin any
 * carousel item content to the right side of the item (or left if RTL).
 */
@OptIn(ExperimentalMaterial3Api::class)
fun CarouselItemInfo.endOffset(): Float {
    return maxSize - (maxSize - size) / 2f
}

@OptIn(ExperimentalMaterial3Api::class)
internal class CarouselItemInfoImpl : CarouselItemInfo {

    val sizeState = mutableFloatStateOf(0f)
    override val size: Float
        get() = sizeState.floatValue

    val minSizeState = mutableFloatStateOf(0f)
    override val minSize: Float
        get() = minSizeState.floatValue

    val maxSizeState = mutableFloatStateOf(0f)
    override val maxSize: Float
        get() = maxSizeState.floatValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarouselItemInfoImpl) return false

        if (sizeState != other.sizeState) return false
        if (minSizeState != other.minSizeState) return false
        if (maxSizeState != other.maxSizeState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizeState.hashCode()
        result = 31 * result + minSizeState.hashCode()
        result = 31 * result + maxSizeState.hashCode()
        return result
    }
}
