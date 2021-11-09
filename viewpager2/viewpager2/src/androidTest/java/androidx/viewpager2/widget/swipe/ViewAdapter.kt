/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

open class ViewAdapter(private val items: List<String>) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        object : ViewHolder(PageView.inflatePage(LayoutInflater.from(parent.context), parent)) {}

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        PageView.setPageText(holder.itemView, items[position])
        PageView.setPageColor(holder.itemView, position)
    }

    override fun getItemCount(): Int = items.size

    /** easy way of dynamically overriding [getItemId] */
    var positionToItemId: (Int) -> Long = { position -> super.getItemId(position) }
    override fun getItemId(position: Int): Long = positionToItemId(position)
}
