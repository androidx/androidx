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
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/** RecyclerView adapter for emoji header.  */
internal class EmojiPickerHeaderAdapter(
    context: Context
) : Adapter<ViewHolder>() {
    @DrawableRes
    private val categoryIconIds: IntArray
    private val layoutInflater: LayoutInflater
    private val context: Context

    init {
        this.context = context
        this.categoryIconIds = getEmojiCategoryIconIds(context)
        layoutInflater = LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return object : ViewHolder(
            layoutInflater.inflate(
                R.layout.header_icon_holder, parent,
                /* attachToRoot= */ false
            )
        ) {}
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val headerIconView: ImageView =
            viewHolder.itemView.findViewById(R.id.emoji_picker_header_icon)
        headerIconView.setImageDrawable(context.getDrawable(categoryIconIds[i]))
    }

    override fun getItemCount(): Int {
        return categoryIconIds.size
    }

    @DrawableRes
    private fun getEmojiCategoryIconIds(
        context: Context
    ): IntArray {
        val typedArray: TypedArray =
            context.resources.obtainTypedArray(R.array.emoji_categories_icons)
        @DrawableRes val iconIds = IntArray(typedArray.length())
        for (i in 0 until typedArray.length()) {
            iconIds[i] = typedArray.getResourceId(i, 0)
        }
        typedArray.recycle()
        return iconIds
    }
}