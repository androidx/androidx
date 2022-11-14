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

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager

/** Body view contains all emojis.  */
internal class EmojiPickerBodyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    init {
        val layoutManager = GridLayoutManager(
            getContext(),
            EmojiPickerConstants.DEFAULT_BODY_COLUMNS,
            LinearLayoutManager.VERTICAL,
            /* reverseLayout = */ false
        )
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val adapter = adapter ?: return 1
                val viewType = adapter.getItemViewType(position)
                // The following viewTypes occupy entire row.
                return if (
                    viewType == CategorySeparatorViewData.TYPE ||
                    viewType == EmptyCategoryViewData.TYPE
                ) EmojiPickerConstants.DEFAULT_BODY_COLUMNS else 1
            }
        }
        setLayoutManager(layoutManager)
    }
}