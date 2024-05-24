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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattClientScope
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.databinding.FragmentConnectionsBinding
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.bluetooth.integration.testapp.ui.main.MainViewModel
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConnectionsFragment : Fragment() {

    internal companion object {
        private const val TAG = "ConnectionsFragment"

        internal const val MANUAL_DISCONNECT = "MANUAL_DISCONNECT"
    }

    private val viewModel by viewModels<ConnectionsViewModel>()

    private val mainViewModel by activityViewModels<MainViewModel>()

    private var _binding: FragmentConnectionsBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter =
            ConnectionsAdapter(viewModel.deviceConnections, ::onClickReconnect, ::onClickDisconnect)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                setCustomViewTab(tab, viewModel.deviceConnections.elementAt(position))
            }
            .attach()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect(::updateUi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setCustomViewTab(tab: Tab, deviceConnection: DeviceConnection) {
        tab.setCustomView(R.layout.tab_item_device)

        val bluetoothDevice = deviceConnection.bluetoothDevice
        val deviceId = bluetoothDevice.id.toString()
        val deviceName = bluetoothDevice.name

        val customView = tab.customView
        customView?.findViewById<TextView>(R.id.text_view_device_id)?.text = deviceId
        val textViewName = customView?.findViewById<TextView>(R.id.text_view_name)
        textViewName?.text = deviceName
        textViewName?.isVisible = deviceName.isNullOrEmpty().not()
        customView?.findViewById<Button>(R.id.image_button_remove)?.setOnClickListener {
            viewModel.removeDeviceConnection(deviceConnection)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUi(connectionsUiState: ConnectionsUiState) {
        binding.viewPager.adapter?.notifyDataSetChanged()

        mainViewModel.selectedBluetoothDevice?.let { selectedBluetoothDevice ->
            mainViewModel.selectedBluetoothDevice = null
            onClickConnect(selectedBluetoothDevice)
        }

        connectionsUiState.showDialogForWrite?.let {
            showDialogForWrite(it)
            viewModel.writeDialogShown()
        }

        connectionsUiState.resultMessage?.let {
            toast(it).show()
            viewModel.resultMessageShown()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onClickConnect(bluetoothDevice: BluetoothDevice) {
        val index = viewModel.addDeviceConnectionIfNew(bluetoothDevice)
        binding.viewPager.adapter?.notifyDataSetChanged()

        val deviceTabIndex =
            if (index == ConnectionsViewModel.NEW_DEVICE) {
                binding.tabLayout.tabCount - 1
            } else {
                index
            }

        binding.viewPager.setCurrentItem(deviceTabIndex, false)

        viewModel.connect(viewModel.deviceConnections.elementAt(deviceTabIndex))
    }

    private fun onClickReconnect(deviceConnection: DeviceConnection) {
        viewModel.connect(deviceConnection)
    }

    private fun onClickDisconnect(deviceConnection: DeviceConnection) {
        viewModel.disconnect(deviceConnection)
    }

    private fun showDialogForWrite(
        gattCharacteristicPair: Pair<GattClientScope, GattCharacteristic>
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_write_characteristic, null)
        val editTextValue = view.findViewById<EditText>(R.id.edit_text_value)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.write))
            .setView(view)
            .setPositiveButton(getString(R.string.write)) { _, _ ->
                val editTextValueString = editTextValue.text.toString()

                viewModel.writeCharacteristic(
                    gattCharacteristicPair.first,
                    gattCharacteristicPair.second,
                    editTextValueString
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }
}
