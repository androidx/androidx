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
import android.widget.Button
import android.widget.TextView
import androidx.bluetooth.integration.testapp.R
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class DeviceServiceCharacteristicsAdapter(
    private val deviceConnection: DeviceConnection,
    private val characteristics: List<BluetoothGattCharacteristic>,
    private val onClickReadCharacteristic: OnClickReadCharacteristic
) : RecyclerView.Adapter<DeviceServiceCharacteristicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_service_characteristic, parent, false)
        return ViewHolder(view, onClickReadCharacteristic)
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
        private val onClickReadCharacteristic: OnClickReadCharacteristic
    ) : RecyclerView.ViewHolder(itemView) {

        private val textViewUuid: TextView = itemView.findViewById(R.id.text_view_uuid)
        private val textViewProperties: TextView = itemView.findViewById(R.id.text_view_properties)

        private val buttonReadCharacteristic: Button =
            itemView.findViewById(R.id.button_read_characteristic)

        private var currentDeviceConnection: DeviceConnection? = null
        private var currentCharacteristic: BluetoothGattCharacteristic? = null

        init {
            buttonReadCharacteristic.setOnClickListener {
                currentDeviceConnection?.let { deviceConnection ->
                    currentCharacteristic?.let { characteristic ->
                        onClickReadCharacteristic.onClick(deviceConnection, characteristic)
                    }
                }
            }
        }

        fun bind(deviceConnection: DeviceConnection, characteristic: BluetoothGattCharacteristic) {
            currentDeviceConnection = deviceConnection
            currentCharacteristic = characteristic

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

            val isNotReadable =
                characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_READ) == 0
            buttonReadCharacteristic.isVisible = isNotReadable.not()
        }
    }
}
