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

import androidx.annotation.IntRange

/** Value (immutable) classes for Emoji Picker.*/
internal abstract class ItemViewData(val id: Long) {
    abstract val type: Int

    override fun hashCode(): Int {
        return (id xor (id ushr 32)).toInt()
    }

    companion object {
        fun calculateId(
            type: Int,
            @IntRange(from = 0, to = 256) categoryIndex: Int,
            @IntRange(from = 0) idInCategory: Int
        ): Long {
            return type.toLong() shl 60 or (categoryIndex.toLong() shl 32) or idInCategory.toLong()
        }
    }
}