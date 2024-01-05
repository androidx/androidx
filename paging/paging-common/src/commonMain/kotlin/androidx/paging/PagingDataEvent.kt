/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Events captured from a [PagingData] that was submitted to the [PagingDataPresenter]
 */
@RestrictTo(LIBRARY_GROUP)
public sealed class PagingDataEvent<T : Any> {
    /**
     * A prepend load event
     *
     * @param [inserted] The list of newly prepended items.
     * @param [newPlaceholdersBefore] The count of null items leading the list of loaded data when
     * new data has been prepended.
     * @param [oldPlaceholdersBefore] The count of null items leading the list of loaded data prior
     * to new data being prepended
     */
    public class Prepend<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val inserted: List<T>,
        val newPlaceholdersBefore: Int,
        val oldPlaceholdersBefore: Int,
    ) : PagingDataEvent<T>()

    /**
     * An append load event
     *
     * @param [startIndex] The first index where this append is applied. If placeholders are
     * enabled, represents the index of the first placeholder replaced with a real item.
     * Otherwise, it represents the index of where the first new item is inserted.
     * @param [inserted] The list of newly appended items.
     * @param [newPlaceholdersAfter] The count of null items trailing the list of loaded data when
     * new data has been appended.
     * @param [oldPlaceholdersAfter] The count of null items trailing the list of loaded data prior
     * to new data being appended.
     */
    public class Append<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val startIndex: Int,
        val inserted: List<T>,
        val newPlaceholdersAfter: Int,
        val oldPlaceholdersAfter: Int,
    ) : PagingDataEvent<T>()

    /**
     * A refresh load event
     *
     * @param [newList] A [NullPaddedList] that contains the metadata of the new list
     * that is presented upon this refresh event
     * @param [previousList] A [NullPaddedList] that contains the metadata of the list
     * presented prior to this refresh load event
     */
    public class Refresh<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val newList: NullPaddedList<T>,
        val previousList: NullPaddedList<T>,
    ) : PagingDataEvent<T>()

    /**
     * A drop event from the front of the list
     *
     * @param [dropCount] A count of items dropped from loaded items
     * @param [newPlaceholdersBefore] The count of null items leading the list of loaded data after
     * items were dropped.
     * @param [oldPlaceholdersBefore] The count of null items leading the list of loaded data prior
     * to items being dropped.
     */
    public class DropPrepend<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val dropCount: Int,
        val newPlaceholdersBefore: Int,
        val oldPlaceholdersBefore: Int,
    ) : PagingDataEvent<T>()

    /**
     * A drop event from the end of the list
     *
     * @param [startIndex] The first index where this drop is applied. If placeholders are
     * enabled, represents the index of the first dropped item that was replaced with a placeholder.
     * Otherwise, it represents the index of where the first real item was dropped.
     * @param [dropCount] A count of items dropped from loaded items
     * @param [newPlaceholdersAfter] The count of null items trailing the list of loaded data after
     * items were dropped.
     * @param [oldPlaceholdersAfter] The count of null items trailing the list of loaded data prior
     * to items being dropped.
     */
    public class DropAppend<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val startIndex: Int,
        val dropCount: Int,
        val newPlaceholdersAfter: Int,
        val oldPlaceholdersAfter: Int,
    ) : PagingDataEvent<T>()
}
