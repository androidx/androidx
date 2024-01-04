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

package androidx.bluetooth.integration.testapp.ui.scanner

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.ScanResult
import androidx.bluetooth.integration.testapp.R
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ScannerAdapter(
    private val onClick: (BluetoothDevice) -> Unit
) : ListAdapter<ScanResult, ScannerAdapter.ViewHolder>(ScannerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = getItem(position)
        holder.bind(scanResult.device)
    }

    inner class ViewHolder(
        itemView: View,
        private val onClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textViewDeviceName: TextView = itemView.findViewById(R.id.text_view_device_name)
        private val textViewDeviceAddress: TextView =
            itemView.findViewById(R.id.text_view_device_address)
        private val buttonConnect: Button = itemView.findViewById(R.id.button_connect)

        private var currentBluetoothDevice: BluetoothDevice? = null

        init {
            buttonConnect.setOnClickListener {
                currentBluetoothDevice?.let(onClick)
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(bluetoothDevice: BluetoothDevice) {
            currentBluetoothDevice = bluetoothDevice
            textViewDeviceAddress.text = bluetoothDevice.id.toString()
            textViewDeviceName.text = bluetoothDevice.name
            textViewDeviceName.isVisible = bluetoothDevice.name.isNullOrEmpty().not()
        }
    }
}

object ScannerDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
    override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem.device == newItem.device
    }
}
