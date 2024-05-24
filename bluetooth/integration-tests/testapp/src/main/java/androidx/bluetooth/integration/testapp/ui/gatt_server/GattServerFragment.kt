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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattService
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentGattServerBinding
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.common.setViewEditText
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GattServerFragment : Fragment() {

    private companion object {
        private const val TAG = "GattServerFragment"
    }

    private var gattServerServicesAdapter: GattServerServicesAdapter? = null

    private val viewModel: GattServerViewModel by viewModels()

    private var _binding: FragmentGattServerBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGattServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAddService.setOnClickListener { onAddGattService() }

        gattServerServicesAdapter =
            GattServerServicesAdapter(viewModel.gattServerServices, ::onAddGattCharacteristic)
        binding.recyclerViewGattServerServices.adapter = gattServerServicesAdapter

        binding.buttonGattServer.setOnClickListener {
            if (viewModel.gattServerJob?.isActive == true) {
                viewModel.gattServerJob?.cancel()
            } else {
                viewModel.openGattServer()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect(::updateUi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onAddGattService() {
        Log.d(TAG, "onAddGattService() called")

        val editTextUuid = EditText(requireActivity())
        editTextUuid.hint = getString(R.string.service_uuid)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_service))
            .setViewEditText(editTextUuid)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val editTextInput = editTextUuid.text.toString()
                try {
                    val uuid =
                        UUID.fromString(
                            when (editTextInput.length) {
                                4 -> "0000$editTextInput-0000-1000-8000-00805F9B34FB"
                                8 -> "$editTextInput-0000-1000-8000-00805F9B34FB"
                                else -> editTextInput
                            }
                        )
                    val service = GattService(uuid, listOf())
                    viewModel.addGattService(service)
                    gattServerServicesAdapter?.notifyItemInserted(
                        viewModel.gattServerServices.size - 1
                    )
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                    toast(getString(R.string.invalid_uuid)).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun onAddGattCharacteristic(bluetoothGattService: GattService) {
        Log.d(
            TAG,
            "onAddGattCharacteristic() called with: " +
                "bluetoothGattService = $bluetoothGattService"
        )

        val view = layoutInflater.inflate(R.layout.dialog_add_characteristic, null)
        val editTextUuid = view.findViewById<EditText>(R.id.edit_text_uuid)

        val checkBoxPropertiesBroadcast =
            view.findViewById<CheckBox>(R.id.check_box_properties_broadcast)
        val checkBoxPropertiesIndicate =
            view.findViewById<CheckBox>(R.id.check_box_properties_indicate)
        val checkBoxPropertiesNotify = view.findViewById<CheckBox>(R.id.check_box_properties_notify)
        val checkBoxPropertiesRead = view.findViewById<CheckBox>(R.id.check_box_properties_read)
        val checkBoxPropertiesSignedWrite =
            view.findViewById<CheckBox>(R.id.check_box_properties_signed_write)
        val checkBoxPropertiesWrite = view.findViewById<CheckBox>(R.id.check_box_properties_write)
        val checkBoxPropertiesWriteNoResponse =
            view.findViewById<CheckBox>(R.id.check_box_properties_write_no_response)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_characteristic))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val uuidText = editTextUuid.text.toString()

                var properties = 0
                if (checkBoxPropertiesBroadcast.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_BROADCAST
                }
                if (checkBoxPropertiesIndicate.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_INDICATE
                }
                if (checkBoxPropertiesNotify.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_NOTIFY
                }
                if (checkBoxPropertiesRead.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_READ
                }
                if (checkBoxPropertiesSignedWrite.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_SIGNED_WRITE
                }
                if (checkBoxPropertiesWrite.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_WRITE
                }
                if (checkBoxPropertiesWriteNoResponse.isChecked) {
                    properties = properties or GattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                }

                try {
                    val uuid =
                        UUID.fromString(
                            when (uuidText.length) {
                                4 -> "0000$uuidText-0000-1000-8000-00805F9B34FB"
                                8 -> "$uuidText-0000-1000-8000-00805F9B34FB"
                                else -> uuidText
                            }
                        )
                    val sampleCharacteristic = GattCharacteristic(uuid, properties)

                    val index = viewModel.gattServerServices.indexOf(bluetoothGattService)
                    viewModel.addGattCharacteristic(bluetoothGattService, sampleCharacteristic)

                    gattServerServicesAdapter?.notifyItemChanged(index)
                } catch (e: Exception) {
                    toast(getString(R.string.invalid_uuid)).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun updateUi(gattServerUiState: GattServerUiState) {
        val isGattServerOpen = gattServerUiState.isGattServerOpen

        if (isGattServerOpen) {
            binding.buttonGattServer.text = getString(R.string.stop_gatt_server)
            binding.buttonGattServer.backgroundTintList = getColor(R.color.red_500)
        } else {
            binding.buttonGattServer.text = getString(R.string.open_gatt_server)
            binding.buttonGattServer.backgroundTintList = getColor(R.color.indigo_500)
        }

        // TODO(ofy) If we want these to always show, we may have to pass over to mainViewModel
        gattServerUiState.resultMessage?.let {
            toast(it).show()
            viewModel.resultMessageShown()
        }
    }
}
