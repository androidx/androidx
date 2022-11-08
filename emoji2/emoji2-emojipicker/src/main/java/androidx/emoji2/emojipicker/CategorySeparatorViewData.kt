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

/**
 * Separator for each category.
 *
 *
 * CategorySeparatorViewData: A 0-width `Space` at the beginning of each category. The
 * `Space` works an anchor (prevention from unexpected scrolling) when the contents of the
 * RecyclerView is updated.
 */
internal class CategorySeparatorViewData
/**
 * Instantiates a CategorySeparatorViewData.
 *
 * @param categoryIndex Used to compute the id.
 * @param idInCategory Used to compute the id.
 * @param categoryName The category name showing in the text view, e.g. "CUSTOM EMOJIS". If empty,
 * will look up the corresponding category name based on `categoryIndex`
 * in [EmojiPickerBodyAdapter.onBindViewHolder]
 */(
    categoryIndex: Int,
    idInCategory: Int,
    /** The name of this category.  */
    val categoryName: String
) :
    ItemViewData(calculateId(TYPE, categoryIndex, /* idInCategory= */idInCategory)) {

    override val type: Int
        get() = TYPE

    companion object {
        val TYPE = CategorySeparatorViewData::class.java.name.hashCode()
    }
}