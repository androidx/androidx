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

// TODO(ofy) Migrate to androidx.bluetooth.BluetoothGattCharacteristic once in place
import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.bluetooth.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class DeviceServiceCharacteristicsAdapter(
    private val characteristics: List<BluetoothGattCharacteristic>
) :
    RecyclerView.Adapter<DeviceServiceCharacteristicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_service, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return characteristics.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val characteristic = characteristics[position]
        holder.bind(characteristic)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)
        private val textViewProperties: TextView = itemView.findViewById(R.id.text_view_properties)

        fun bind(characteristic: BluetoothGattCharacteristic) {
            textViewUuid.text = characteristic.uuid.toString()
            /*
                TODO(ofy) Display property type correctly
                int	PROPERTY_BROADCAST
                int	PROPERTY_EXTENDED_PROPS
                int	PROPERTY_INDICATE
                int	PROPERTY_NOTIFY
                int	PROPERTY_READ
                int	PROPERTY_SIGNED_WRITE
                int	PROPERTY_WRITE
                int	PROPERTY_WRITE_NO_RESPONSE

                textViewProperties.text = characteristic.properties
             */
        }
    }
}
