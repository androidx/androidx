/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import androidx.annotation.IntRange

/**
 * Snapshot of data being presented by a
 * [AsyncPagingDataDiffer][androidx.pagingAsyncPagingDataDiffer] or a
 * [PagingDataAdapter][androidx.paging.PagingDataAdapter].
 */
public class ItemSnapshotList<T>(
    /**
     * Number of placeholders before the presented [items], 0 if
     * [enablePlaceholders][androidx.paging.PagingConfig.enablePlaceholders] is `false`.
     */
    @IntRange(from = 0)
    public val placeholdersBefore: Int,
    /**
     * Number of placeholders after the presented [items], 0 if
     * [enablePlaceholders][androidx.paging.PagingConfig.enablePlaceholders] is `false`.
     */
    @IntRange(from = 0)
    public val placeholdersAfter: Int,
    /**
     * The presented data, excluding placeholders.
     */
    public val items: List<T>
) : AbstractList<T?>() {

    /**
     * Size of [ItemSnapshotList] including placeholders.
     *
     * To get the size excluding placeholders, use [List.size] on [items] directly.
     *
     * @see items
     */
    public override val size: Int
        get() = placeholdersBefore + items.size + placeholdersAfter

    /**
     * Returns the item at [index], where [index] includes the position of placeholders. If [index]
     * points to the position of a placeholder, `null` is returned.
     *
     * To get the size using an index excluding placeholders, use [List.size] on [items] directly.
     *
     * @throws IndexOutOfBoundsException if [index] < 0 or [index] > [size].
     */
    override fun get(index: Int): T? {
        return when (index) {
            in 0 until placeholdersBefore -> null
            in placeholdersBefore until (placeholdersBefore + items.size) -> {
                items[index - placeholdersBefore]
            }
            in (placeholdersBefore + items.size) until size -> null
            else -> throw IndexOutOfBoundsException(
                "Illegal attempt to access index $index in ItemSnapshotList of size $size"
            )
        }
    }
}
