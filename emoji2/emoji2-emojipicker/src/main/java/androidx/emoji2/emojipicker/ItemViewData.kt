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
}

/** Represents an item within the body RecyclerView. */
internal sealed class ItemViewData(val itemType: ItemType) {
    val viewType = itemType.ordinal
}

/** Title of each category. */
internal data class CategoryTitle(val title: String) : ItemViewData(ItemType.CATEGORY_TITLE)

/** Text to display when the category contains no items. */
internal data class PlaceholderText(val text: String) : ItemViewData(ItemType.PLACEHOLDER_TEXT)

/** Represents an emoji. */
internal data class EmojiViewData(
    var emoji: String,
    val updateToSticky: Boolean = true,
    // Needed to ensure uniqueness since we enabled stable Id.
    val dataIndex: Int = 0
) : ItemViewData(ItemType.EMOJI)

internal object Extensions {
    internal fun Int.toItemType() = ItemType.values()[this]
}
