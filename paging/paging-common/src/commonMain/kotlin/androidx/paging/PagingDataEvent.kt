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
    ) : PagingDataEvent<T>() {

        override fun equals(other: Any?): Boolean {
            return other is Prepend<*> &&
                inserted == other.inserted &&
                newPlaceholdersBefore == other.newPlaceholdersBefore &&
                oldPlaceholdersBefore == other.oldPlaceholdersBefore
        }

        override fun hashCode(): Int {
            return inserted.hashCode() + newPlaceholdersBefore.hashCode() +
                oldPlaceholdersBefore.hashCode()
        }

        override fun toString(): String {
            return """PagingDataEvent.Prepend loaded ${inserted.size} items (
                    |   first item: ${inserted.firstOrNull()}
                    |   last item: ${inserted.lastOrNull()}
                    |   newPlaceholdersBefore: $newPlaceholdersBefore
                    |   oldPlaceholdersBefore: $oldPlaceholdersBefore
                    |)
                    |""".trimMargin()
        }
    }

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
    ) : PagingDataEvent<T>() {
        override fun equals(other: Any?): Boolean {
            return other is Append<*> &&
                startIndex == other.startIndex &&
                inserted == other.inserted &&
                newPlaceholdersAfter == other.newPlaceholdersAfter &&
                oldPlaceholdersAfter == other.oldPlaceholdersAfter
        }

        override fun hashCode(): Int {
            return startIndex.hashCode() + inserted.hashCode() + newPlaceholdersAfter.hashCode() +
                oldPlaceholdersAfter.hashCode()
        }

        override fun toString(): String {
            return """PagingDataEvent.Append loaded ${inserted.size} items (
                    |   startIndex: $startIndex
                    |   first item: ${inserted.firstOrNull()}
                    |   last item: ${inserted.lastOrNull()}
                    |   newPlaceholdersBefore: $newPlaceholdersAfter
                    |   oldPlaceholdersBefore: $oldPlaceholdersAfter
                    |)
                    |""".trimMargin()
        }
    }

    /**
     * A refresh load event
     *
     * @param [newList] A [PlaceholderPaddedList] that contains the metadata of the new list
     * that is presented upon this refresh event
     * @param [previousList] A [PlaceholderPaddedList] that contains the metadata of the list
     * presented prior to this refresh load event
     */
    public class Refresh<T : Any> @RestrictTo(LIBRARY_GROUP) constructor(
        val newList: PlaceholderPaddedList<T>,
        val previousList: PlaceholderPaddedList<T>,
    ) : PagingDataEvent<T>() {
        override fun equals(other: Any?): Boolean {
            return other is Refresh<*> &&
                newList.placeholdersBefore == other.newList.placeholdersBefore &&
                newList.placeholdersAfter == other.newList.placeholdersAfter &&
                newList.size == other.newList.size &&
                newList.dataCount == other.newList.dataCount &&
                previousList.placeholdersBefore == other.previousList.placeholdersBefore &&
                previousList.placeholdersAfter == other.previousList.placeholdersAfter &&
                previousList.size == other.previousList.size &&
                previousList.dataCount == other.previousList.dataCount
        }

        override fun hashCode(): Int {
            return newList.hashCode() + previousList.hashCode()
        }

        override fun toString(): String {
            return """PagingDataEvent.Refresh loaded newList
                    |   newList (
                    |       placeholdersBefore: ${newList.placeholdersBefore}
                    |       placeholdersAfter: ${newList.placeholdersAfter}
                    |       size: ${newList.size}
                    |       dataCount: ${newList.dataCount}
                    |   )
                    |   previousList (
                    |       placeholdersBefore: ${previousList.placeholdersBefore}
                    |       placeholdersAfter: ${previousList.placeholdersAfter}
                    |       size: ${previousList.size}
                    |       dataCount: ${previousList.dataCount}
                    |   )
                    |""".trimMargin()
        }
    }

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
    ) : PagingDataEvent<T>() {

        override fun equals(other: Any?): Boolean {
            return other is DropPrepend<*> &&
                dropCount == other.dropCount &&
                newPlaceholdersBefore == other.newPlaceholdersBefore &&
                oldPlaceholdersBefore == other.oldPlaceholdersBefore
        }

        override fun hashCode(): Int {
            return dropCount.hashCode() + newPlaceholdersBefore.hashCode() +
                oldPlaceholdersBefore.hashCode()
        }

        override fun toString(): String {
            return """PagingDataEvent.DropPrepend dropped $dropCount items (
                    |   dropCount: $dropCount
                    |   newPlaceholdersBefore: $newPlaceholdersBefore
                    |   oldPlaceholdersBefore: $oldPlaceholdersBefore
                    |)
                    |""".trimMargin()
        }
    }

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
    ) : PagingDataEvent<T>() {

        override fun equals(other: Any?): Boolean {
            return other is DropAppend<*> &&
                startIndex == other.startIndex &&
                dropCount == other.dropCount &&
                newPlaceholdersAfter == other.newPlaceholdersAfter &&
                oldPlaceholdersAfter == other.oldPlaceholdersAfter
        }

        override fun hashCode(): Int {
            return startIndex.hashCode() + dropCount.hashCode() + newPlaceholdersAfter.hashCode() +
                oldPlaceholdersAfter.hashCode()
        }

        override fun toString(): String {
            return """PagingDataEvent.DropAppend dropped $dropCount items (
                    |   startIndex: $startIndex
                    |   dropCount: $dropCount
                    |   newPlaceholdersBefore: $newPlaceholdersAfter
                    |   oldPlaceholdersBefore: $oldPlaceholdersAfter
                    |)
                    |""".trimMargin()
        }
    }
}
