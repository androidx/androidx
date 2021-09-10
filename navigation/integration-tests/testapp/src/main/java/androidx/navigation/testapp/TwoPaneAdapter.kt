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

package androidx.navigation.testapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TwoPaneAdapter(
    private val dataSet: Array<String>,
    private val onClick: (CharSequence) -> Unit
) : RecyclerView.Adapter<TwoPaneAdapter.ViewHolder>() {

    class ViewHolder(view: View, val onClick: (CharSequence) -> Unit) :
        RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.list_pane_row_item)

        init {
            textView.setOnClickListener {
                onClick(textView.text)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoPaneAdapter
    .ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.list_pane_row_item, parent,
            false
        )
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.textView.text = dataSet[position]
    }

    override fun getItemCount() = dataSet.size
}