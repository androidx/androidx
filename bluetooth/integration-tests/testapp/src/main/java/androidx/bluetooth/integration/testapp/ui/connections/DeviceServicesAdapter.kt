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

package androidx.bluetooth.integration.testapp.ui.connections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.bluetooth.GattService
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.data.connection.OnCharacteristicActionClick
import androidx.recyclerview.widget.RecyclerView

class DeviceServicesAdapter(
    private val deviceConnection: DeviceConnection,
    private val onCharacteristicActionClick: OnCharacteristicActionClick,
) : RecyclerView.Adapter<DeviceServicesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceConnection, deviceConnection.services[position])
    }

    override fun getItemCount(): Int {
        return deviceConnection.services.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)

        private val recyclerViewServiceCharacteristic: RecyclerView =
            itemView.findViewById(R.id.recycler_view_service_characteristic)

        fun bind(deviceConnection: DeviceConnection, service: GattService) {
            textViewUuid.text = service.uuid.toString()

            recyclerViewServiceCharacteristic.adapter =
                DeviceServiceCharacteristicsAdapter(
                    deviceConnection,
                    service.characteristics,
                    onCharacteristicActionClick
                )
        }
    }
}
