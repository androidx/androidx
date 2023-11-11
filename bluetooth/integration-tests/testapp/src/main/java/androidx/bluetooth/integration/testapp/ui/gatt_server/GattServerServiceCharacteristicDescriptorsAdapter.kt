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

package androidx.bluetooth.integration.testapp.ui.gatt_server

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class GattServerServiceCharacteristicDescriptorsAdapter(
    // TODO Wait for (b/310337673)
    // private val characteristics: List<GattDescriptor>
    // TODO Remove below after (b/310337673)
    private val characteristics: List<GattCharacteristic>
) : RecyclerView.Adapter<GattServerServiceCharacteristicDescriptorsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gatt_server_characteristic_descriptor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(characteristics[position])
    }

    override fun getItemCount(): Int {
        return characteristics.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)

        fun bind(characteristic: GattCharacteristic) {
            textViewUuid.text = characteristic.uuid.toString()
        }
    }
}
