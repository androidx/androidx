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
fun rememberPagerState(
    @IntRange(from = 0) initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    @IntRange(from = 1) pageCount: () -> Int
): PagerState {
    return rememberSaveable(saver = DefaultPagerState.Saver) {
            DefaultPagerState(initialPage, initialPageOffsetFraction, pageCount)
        }
        .apply { pageCountState.value = pageCount }
}

/**
 * Creates a default [PagerState] to be used with a Wear Pager
 *
 * @param currentPage The page that should be shown first.
 * @param currentPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount The number of pages this Pager will have.
 */
fun PagerState(
    @IntRange(from = 0) currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    @IntRange(from = 1) pageCount: () -> Int
): PagerState = DefaultPagerState(currentPage, currentPageOffsetFraction, pageCount)

/**
 * The state that can be used to control Wear Pagers.
 *
 * This class extends the Compose Foundation's `PagerState` to provide specialized behavior tailored
 * for Wear OS devices. This allows for future customizations and enhancements specific to Wear OS
 * use cases.
 *
 * @param currentPage The index of the currently active page.
 * @param currentPageOffsetFraction The fractional offset from the start of the current page. Should
 *   be between -0.5 and 0.5, where 0 indicates the start of the current page.
 */
abstract class PagerState(
    @IntRange(from = 0) currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f
) : ComposePagerState(currentPage, currentPageOffsetFraction)

private class DefaultPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int
) : PagerState(currentPage, currentPageOffsetFraction) {
    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()

    companion object {
        /** To keep current page and current page offset saved */
        val Saver: Saver<DefaultPagerState, *> =
            listSaver(
                save = {
                    listOf(
                        it.currentPage,
                        (it.currentPageOffsetFraction).coerceIn(MinPageOffset, MaxPageOffset),
                        it.pageCount
                    )
                },
                restore = {
                    DefaultPagerState(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        updatedPageCount = { it[2] as Int }
                    )
                }
            )
    }
}

internal const val MinPageOffset = -0.5f
internal const val MaxPageOffset = 0.5f
