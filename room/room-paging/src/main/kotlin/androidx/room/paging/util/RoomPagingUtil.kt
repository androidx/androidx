/*
 * Copyright 2022 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.paging.util

import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Calculates query limit based on LoadType.
 *
 * Prepend: If requested loadSize is larger than available number of items to prepend, it will
 * query with OFFSET = 0, LIMIT = prevKey
 */
fun getLimit(params: PagingSource.LoadParams<Int>, key: Int): Int {
    return when (params) {
        is PagingSource.LoadParams.Prepend ->
            if (key < params.loadSize) {
                key
            } else {
                params.loadSize
            }
        else -> params.loadSize
    }
}

/**
 * calculates query offset amount based on loadtype
 *
 * Prepend: OFFSET is calculated by counting backwards the number of items that needs to be
 * loaded before [key]. For example, if key = 30 and loadSize = 5, then offset = 25 and items
 * in db position 26-30 are loaded.
 * If requested loadSize is larger than the number of available items to
 * prepend, OFFSET clips to 0 to prevent negative OFFSET.
 *
 * Refresh:
 * If initialKey is supplied through Pager, Paging 3 will now start loading from
 * initialKey with initialKey being the first item.
 * If key is supplied by [getClippedRefreshKey], the key has already been adjusted to load half
 * of the requested items before anchorPosition and the other half after anchorPosition. See
 * comments on [getClippedRefreshKey] for more details.
 * If key (regardless if from initialKey or [getClippedRefreshKey]) is larger than available items,
 * the last page will be loaded by counting backwards the loadSize before last item in
 * database. For example, this can happen if invalidation came from a large number of items
 * dropped. i.e. in items 0 - 100, items 41-80 are dropped. Depending on last
 * viewed item, hypothetically [getClippedRefreshKey] may return key = 60. If loadSize = 10, then items
 * 31-40 will be loaded.
 */
fun getOffset(params: PagingSource.LoadParams<Int>, key: Int, itemCount: Int): Int {
    return when (params) {
        is PagingSource.LoadParams.Prepend ->
            if (key < params.loadSize) {
                0
            } else {
                key - params.loadSize
            }
        is PagingSource.LoadParams.Append -> key
        is PagingSource.LoadParams.Refresh ->
            if (key >= itemCount) {
                maxOf(0, itemCount - params.loadSize)
            } else {
                key
            }
    }
}

/**
 * Returns the key for [PagingSource] for a non-initial REFRESH load.
 *
 * To prevent a negative key, key is clipped to 0 when the number of items available before
 * anchorPosition is less than the requested amount of initialLoadSize / 2.
 */
fun <Value : Any> PagingState<Int, Value>.getClippedRefreshKey(): Int? {
    return when (val anchorPosition = anchorPosition) {
        null -> null
        /**
         *  It is unknown whether anchorPosition represents the item at the top of the screen or item at
         *  the bottom of the screen. To ensure the number of items loaded is enough to fill up the
         *  screen, half of loadSize is loaded before the anchorPosition and the other half is
         *  loaded after the anchorPosition -- anchorPosition becomes the middle item.
         */
        else -> maxOf(0, anchorPosition - (config.initialLoadSize / 2))
    }
}
