/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark.target

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

data class Entry(val contents: String)

class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val content: TextView = itemView.findViewById(R.id.content)
}

class EntryAdapter(
    private val entries: List<Entry>,
    @LayoutRes val itemResId: Int = R.layout.recycler_row
) : RecyclerView.Adapter<EntryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(itemResId, parent, false)
        return EntryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.content.text = entry.contents
    }

    override fun getItemCount(): Int = entries.size
}
