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

package androidx.emoji2.emojipicker

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange

/**
 * A group of items in RecyclerView for emoji picker body.
 * [titleItem] comes first.
 * [contentItems] comes after [titleItem].
 * [emptyPlaceholderItem] will be served after [titleItem] only if [contentItems] is empty.
 * [maxContentItemCount], if provided, will truncate [contentItems] to certain size.
 *
 * [categoryIconId] is the corresponding category icon in emoji picker header.
 */
internal class ItemGroup(
    @DrawableRes internal val categoryIconId: Int,
    internal val titleItem: CategoryTitle,
    private val contentItems: List<EmojiViewData>,
    private val maxContentItemCount: Int? = null,
    private val emptyPlaceholderItem: PlaceholderText? = null
) {

    val size: Int
        get() = 1 /* title */ + when {
            contentItems.isEmpty() -> if (emptyPlaceholderItem != null) 1 else 0
            maxContentItemCount != null && contentItems.size > maxContentItemCount ->
                maxContentItemCount
            else -> contentItems.size
        }

    operator fun get(index: Int): ItemViewData {
        if (index == 0) return titleItem
        val contentIndex = index - 1
        if (contentIndex < contentItems.size) return contentItems[contentIndex]
        if (contentIndex == 0 && emptyPlaceholderItem != null) return emptyPlaceholderItem
        throw IndexOutOfBoundsException()
    }

    fun getAll(): List<ItemViewData> = IntRange(0, size - 1).map { get(it) }
}

/**
 * A view of concatenated list of [ItemGroup].
 */
internal class EmojiPickerItems(
    private val groups: List<ItemGroup>,
) : Iterable<ItemViewData> {
    val size: Int get() = groups.sumOf { it.size }

    init {
        check(groups.isNotEmpty()) { "Initialized with empty categorized sources" }
    }

    fun getBodyItem(@IntRange(from = 0) absolutePosition: Int): ItemViewData {
        var localPosition = absolutePosition
        for (group in groups) {
            if (localPosition < group.size) return group[localPosition]
            else localPosition -= group.size
        }
        throw IndexOutOfBoundsException()
    }

    val numGroups: Int get() = groups.size

    @DrawableRes
    fun getHeaderIconId(@IntRange(from = 0) index: Int): Int = groups[index].categoryIconId

    fun getHeaderIconDescription(@IntRange(from = 0) index: Int): String =
        groups[index].titleItem.title

    fun groupIndexByItemPosition(@IntRange(from = 0) absolutePosition: Int): Int {
        var localPosition = absolutePosition
        var index = 0
        for (group in groups) {
            if (localPosition < group.size) return index
            else {
                localPosition -= group.size
                index++
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun firstItemPositionByGroupIndex(@IntRange(from = 0) groupIndex: Int): Int =
        groups.take(groupIndex).sumOf { it.size }

    fun groupRange(group: ItemGroup): kotlin.ranges.IntRange {
        check(groups.contains(group))
        val index = groups.indexOf(group)
        return firstItemPositionByGroupIndex(index).let { it until it + group.size }
    }

    override fun iterator(): Iterator<ItemViewData> = groups.flatMap { it.getAll() }.iterator()
}
