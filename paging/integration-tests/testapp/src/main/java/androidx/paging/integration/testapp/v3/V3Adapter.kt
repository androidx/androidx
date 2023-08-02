/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class V3Adapter : PagingDataAdapter<Item, RecyclerView.ViewHolder>(
    diffCallback = Item.DIFF_CALLBACK
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = object : RecyclerView.ViewHolder(TextView(parent.context)) {}
        holder.itemView.minimumHeight = 150
        holder.itemView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            (holder.itemView as TextView).text = item.text
            holder.itemView.setBackgroundColor(item.bgColor)
        } else {
            (holder.itemView as TextView).setText(R.string.loading)
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}
