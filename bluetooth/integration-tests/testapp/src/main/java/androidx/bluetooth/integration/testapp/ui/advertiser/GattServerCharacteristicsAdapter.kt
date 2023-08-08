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
import android.widget.TextView
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class GattServerCharacteristicsAdapter(
    private val characteristics: List<GattCharacteristic>
) : RecyclerView.Adapter<GattServerCharacteristicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gatt_server_characteristic, parent, false)
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
        private val textViewProperties: TextView = itemView.findViewById(R.id.text_view_properties)

        fun bind(characteristic: GattCharacteristic) {
            textViewUuid.text = characteristic.uuid.toString()

            val properties = characteristic.properties
            val context = itemView.context
            val propertiesList = mutableListOf<String>()

            if (properties.and(GattCharacteristic.PROPERTY_BROADCAST) != 0) {
                propertiesList.add(context.getString(R.string.broadcast))
            }
            if (properties.and(GattCharacteristic.PROPERTY_INDICATE) != 0) {
                propertiesList.add(context.getString(R.string.indicate))
            }
            if (properties.and(GattCharacteristic.PROPERTY_NOTIFY) != 0) {
                propertiesList.add(context.getString(R.string.notify))
            }
            if (properties.and(GattCharacteristic.PROPERTY_READ) != 0) {
                propertiesList.add(context.getString(R.string.read))
            }
            if (properties.and(GattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) {
                propertiesList.add(context.getString(R.string.signed_write))
            }
            if (properties.and(GattCharacteristic.PROPERTY_WRITE) != 0) {
                propertiesList.add(context.getString(R.string.write))
            }
            if (properties.and(GattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                propertiesList.add(context.getString(R.string.write_no_response))
            }
            textViewProperties.text = propertiesList.joinToString()
        }
    }
}
