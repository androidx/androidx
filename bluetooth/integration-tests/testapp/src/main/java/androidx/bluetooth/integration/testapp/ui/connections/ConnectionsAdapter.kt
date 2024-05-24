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
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.data.connection.OnCharacteristicActionClick
import androidx.bluetooth.integration.testapp.data.connection.Status
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConnectionsAdapter(
    private val deviceConnections: Set<DeviceConnection>,
    private val onClickReconnect: (DeviceConnection) -> Unit,
    private val onClickDisconnect: (DeviceConnection) -> Unit
) : RecyclerView.Adapter<ConnectionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_connection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceConnections.elementAt(position))
    }

    override fun getItemCount(): Int {
        return deviceConnections.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewDeviceConnectionStatus: TextView =
            itemView.findViewById(R.id.text_view_device_connection_status)
        private val progressIndicatorDeviceConnection: ProgressBar =
            itemView.findViewById(R.id.progress_indicator_device_connection)

        private val buttonReconnect: Button = itemView.findViewById(R.id.button_reconnect)
        private val buttonDisconnect: Button = itemView.findViewById(R.id.button_disconnect)

        private val recyclerViewDeviceServices: RecyclerView =
            itemView.findViewById(R.id.recycler_view_device_services)

        private val onCharacteristicActionClick =
            object : OnCharacteristicActionClick {
                override fun onClick(
                    deviceConnection: DeviceConnection,
                    characteristic: GattCharacteristic,
                    action: @OnCharacteristicActionClick.Action Int
                ) {
                    deviceConnection.onCharacteristicActionClick?.onClick(
                        deviceConnection,
                        characteristic,
                        action
                    )
                }
            }

        init {
            buttonReconnect.setOnClickListener {
                deviceConnections.elementAt(bindingAdapterPosition).let(onClickReconnect)
            }
            buttonDisconnect.setOnClickListener {
                deviceConnections.elementAt(bindingAdapterPosition).let(onClickDisconnect)
            }
        }

        fun bind(deviceConnection: DeviceConnection) {
            recyclerViewDeviceServices.adapter =
                DeviceServicesAdapter(deviceConnection, onCharacteristicActionClick)
            val context = itemView.context
            recyclerViewDeviceServices.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            )

            progressIndicatorDeviceConnection.isVisible = false
            buttonReconnect.isVisible = false
            buttonDisconnect.isVisible = false

            when (deviceConnection.status) {
                Status.DISCONNECTED -> {
                    textViewDeviceConnectionStatus.text = context.getString(R.string.disconnected)
                    textViewDeviceConnectionStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.green_500)
                    )
                    buttonReconnect.isVisible = true
                }
                Status.CONNECTING -> {
                    progressIndicatorDeviceConnection.isVisible = true
                    textViewDeviceConnectionStatus.text = context.getString(R.string.connecting)
                    textViewDeviceConnectionStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.indigo_500)
                    )
                }
                Status.CONNECTED -> {
                    textViewDeviceConnectionStatus.text = context.getString(R.string.connected)
                    textViewDeviceConnectionStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.indigo_500)
                    )
                    buttonDisconnect.isVisible = true
                }
            }
        }
    }
}
