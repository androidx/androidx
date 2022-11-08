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

/** Concrete entry which contains emoji view data.  */
internal class EmojiViewData(
    categoryIndex: Int,
    idInCategory: Int,
    primary: String,
    secondaries: Array<String>
) :
    ItemViewData(calculateId(TYPE, categoryIndex, idInCategory)) {
    /** The index of category where the emoji view located in.  */
    private val categoryIndex: Int

    /** The id of this emoji view in the category, usually is the position of the emoji.  */
    private val idInCategory: Int

    /** Primary key which is used for labeling and for PRESS action.  */
    val primary: String

    /** Secondary keys which are used for LONG_PRESS action.  */
    val secondaries: Array<String>

    /**
     * Instantiates a EmojiViewData.
     *
     * @param categoryIndex Used to compute the id.
     * @param idInCategory Used to compute the id.
     * @param primary The default base variant of the given emoji (no skin tone or gender modifier)
     * @param secondaries Array of variants associated to primary
     */
    init {
        this.categoryIndex = categoryIndex
        this.idInCategory = idInCategory
        this.primary = primary
        this.secondaries = secondaries
    }

    override val type: Int
        get() = TYPE

    companion object {
        val TYPE: Int = EmojiViewData::class.java.name.hashCode()
    }
}