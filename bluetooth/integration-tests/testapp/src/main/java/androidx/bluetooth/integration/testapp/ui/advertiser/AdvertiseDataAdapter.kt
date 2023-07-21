/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth.integration.testapp.ui.advertiser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class AdvertiseDataAdapter(var advertiseData: List<String>, private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<AdvertiseDataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_advertiser_data, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun getItemCount(): Int {
        return advertiseData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val advertiseData = advertiseData[position]
        holder.bind(advertiseData)
    }

    inner class ViewHolder(itemView: View, private val onClick: (Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val textViewData: TextView = itemView.findViewById(R.id.text_view_data)
        private val imageButtonClear: ImageButton = itemView.findViewById(R.id.image_button_clear)

        init {
            imageButtonClear.setOnClickListener {
                imageButtonClear.isClickable = false
                onClick(absoluteAdapterPosition)
            }
        }

        fun bind(advertiseData: String) {
            textViewData.text = advertiseData
            imageButtonClear.isClickable = true
        }
    }
}
