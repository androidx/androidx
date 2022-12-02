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

internal enum class ItemType {
    CATEGORY_TITLE,
    PLACEHOLDER_TEXT,
    EMOJI,
    PLACEHOLDER_EMOJI,
}

/**
 * Represents an item within the body RecyclerView.
 */
internal sealed class ItemViewData(itemType: ItemType, val occupyEntireRow: Boolean = false) {
    val viewType = itemType.ordinal
}

/**
 * Title of each category.
 */
internal class CategoryTitle(val title: String) :
    ItemViewData(ItemType.CATEGORY_TITLE, occupyEntireRow = true)

/**
 * Text to display when the category contains no items.
 */
internal class PlaceholderText(val text: String) :
    ItemViewData(ItemType.PLACEHOLDER_TEXT, occupyEntireRow = true)

/**
 * Represents an emoji.
 */
internal class EmojiViewData(
    var emoji: String,
    val updateToSticky: Boolean = true
) : ItemViewData(ItemType.EMOJI)

internal object PlaceholderEmoji : ItemViewData(ItemType.PLACEHOLDER_EMOJI)

internal object Extensions {
    internal fun Int.toItemType() = ItemType.values()[this]
}
