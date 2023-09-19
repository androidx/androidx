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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.data.connection.OnClickCharacteristic
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class DeviceServiceCharacteristicsAdapter(
    private val deviceConnection: DeviceConnection,
    private val characteristics: List<GattCharacteristic>,
    private val onClickReadCharacteristic: OnClickCharacteristic,
    private val onClickWriteCharacteristic: OnClickCharacteristic
) : RecyclerView.Adapter<DeviceServiceCharacteristicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_service_characteristic, parent, false)
        return ViewHolder(view, onClickReadCharacteristic, onClickWriteCharacteristic)
    }

    override fun getItemCount(): Int {
        return characteristics.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val characteristic = characteristics[position]
        holder.bind(deviceConnection, characteristic)
    }

    inner class ViewHolder(
        itemView: View,
        private val onClickReadCharacteristic: OnClickCharacteristic,
        private val onClickWriteCharacteristic: OnClickCharacteristic
    ) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)
        private val textViewProperties: TextView = itemView.findViewById(R.id.text_view_properties)

        private val layoutValue: LinearLayout = itemView.findViewById(R.id.layout_value)
        private val textViewValue: TextView = itemView.findViewById(R.id.text_view_value)

        private val buttonReadCharacteristic: Button =
            itemView.findViewById(R.id.button_read_characteristic)

        private val buttonWriteCharacteristic: Button =
            itemView.findViewById(R.id.button_write_characteristic)

        private var currentDeviceConnection: DeviceConnection? = null
        private var currentCharacteristic: GattCharacteristic? = null

        init {
            buttonReadCharacteristic.setOnClickListener {
                currentDeviceConnection?.let { deviceConnection ->
                    currentCharacteristic?.let { characteristic ->
                        onClickReadCharacteristic.onClick(deviceConnection, characteristic)
                    }
                }
            }

            buttonWriteCharacteristic.setOnClickListener {
                currentDeviceConnection?.let { deviceConnection ->
                    currentCharacteristic?.let { characteristic ->
                        onClickWriteCharacteristic.onClick(deviceConnection, characteristic)
                    }
                }
            }
        }

        fun bind(deviceConnection: DeviceConnection, characteristic: GattCharacteristic) {
            currentDeviceConnection = deviceConnection
            currentCharacteristic = characteristic

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

            val isReadable = properties.and(GattCharacteristic.PROPERTY_READ) != 0
            val isWriteable = properties.and(GattCharacteristic.PROPERTY_WRITE) != 0 ||
                properties.and(GattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ||
                properties.and(GattCharacteristic.PROPERTY_SIGNED_WRITE) != 0
            buttonReadCharacteristic.isVisible = isReadable
            buttonWriteCharacteristic.isVisible = isWriteable

            val value = deviceConnection.valueFor(characteristic)
            layoutValue.isVisible = value != null
            textViewValue.text = value?.decodeToString()
        }
    }
}
