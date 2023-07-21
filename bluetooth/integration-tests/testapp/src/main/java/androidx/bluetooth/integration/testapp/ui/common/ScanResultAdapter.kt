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

package androidx.bluetooth.integration.testapp.ui.common

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter(private val onClick: (ScanResult) -> Unit) :
    ListAdapter<ScanResult, ScanResultAdapter.ScanResultViewHolder>(ScanResultDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.scan_result_item, parent, false)
        return ScanResultViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        val scanResult = getItem(position)
        holder.bind(scanResult)
    }

    class ScanResultViewHolder(itemView: View, val onClick: (ScanResult) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val scanResultTextView: TextView = itemView.findViewById(R.id.text_scan_result)
        private var currentScanResult: ScanResult? = null

        init {
            itemView.setOnClickListener {
                currentScanResult?.let {
                    onClick(it)
                }
            }
        }

        fun bind(scanResult: ScanResult) {
            currentScanResult = scanResult

            scanResultTextView.text = scanResult.toString()
        }
    }
}

object ScanResultDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
    override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem.device == newItem.device
    }
}
