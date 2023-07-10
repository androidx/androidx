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
import android.widget.Button
import android.widget.TextView
import androidx.bluetooth.GattService
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class GattServerServicesAdapter(
    private val services: List<GattService>,
    private val onClickAddCharacteristic: (GattService) -> Unit
) : RecyclerView.Adapter<GattServerServicesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gatt_server_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount(): Int {
        return services.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)
        private val buttonAddCharacteristic: Button =
            itemView.findViewById(R.id.button_add_characteristic)

        private val recyclerViewServiceCharacteristic: RecyclerView =
            itemView.findViewById(R.id.recycler_view_service_characteristic)

        private var currentGattService: GattService? = null

        init {
            buttonAddCharacteristic.setOnClickListener {
                currentGattService?.let(onClickAddCharacteristic)
            }
        }

        fun bind(gattService: GattService) {
            currentGattService = gattService

            textViewUuid.text = gattService.uuid.toString()

            recyclerViewServiceCharacteristic.adapter = GattServerCharacteristicsAdapter(
                gattService.characteristics
            )
        }
    }
}
