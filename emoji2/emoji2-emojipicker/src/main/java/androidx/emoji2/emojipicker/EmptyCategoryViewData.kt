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
 * Indicator to show "You haven't used any emojis yet"-like label for empty category.
 *
 * EmptyCategoryViewData: A full-width `Space` at the beginning of each empty category, to
 * show description and indicate that there is no items in the category.
 */
internal class EmptyCategoryViewData
/**
 * Instantiates an EmptyCategoryViewData.
 *
 * @param categoryIndex Used to compute the id.
 * @param idInCategory Used to compute the id.
 * @param description The description showing in the text view, e.g. "You haven't used any emojis
 * yet". If empty, will look up the corresponding description based on `categoryIndex`
 * in [EmojiPickerBodyAdapter.onBindViewHolder].
 */(
    categoryIndex: Int,
    idInCategory: Int,
    /** The description to indicate the category is empty.  */
    val description: String
) :
    ItemViewData(calculateId(TYPE, categoryIndex, idInCategory)) {
    override val type: Int
        get() = TYPE

    companion object {
        val TYPE = EmptyCategoryViewData::class.java.name.hashCode()

        /**
         * Use -1 as categoryIndex and idInCategory and empty string as description for the default
         * instance. The categoryIndex and idInCategory are just used to compute the id for the instance.
         * Make the description empty to look up the corresponding description based on category index in
         * [EmojiPickerBodyAdapter.onBindViewHolder].
         */
        val INSTANCE = EmptyCategoryViewData( /* categoryIndex= */
            -1, /* idInCategory= */-1, /* description= */""
        )
    }
}