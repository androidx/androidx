/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample.infolog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.window.sample.R

class InfoLogAdapter : RecyclerView.Adapter<InfoLogVH>() {

    private var id = 0
    private val items = mutableListOf<InfoLog>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoLogVH {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_holder_info_log, parent, false)
        return InfoLogVH(root)
    }

    override fun onBindViewHolder(holder: InfoLogVH, position: Int) {
        val item = items[position]
        holder.titleView.text = "ID: ${item.id} Title: ${item.title}"
        holder.detailView.text = "Detail: ${item.detail}"
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun append(title: String, message: String) {
        append(InfoLog(title, message, id))
        ++id
    }

    private fun append(item: InfoLog) {
        items.add(0, item)
    }
}