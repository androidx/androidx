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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.bluetooth.AdvertiseResult
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentAdvertiserBinding
import androidx.bluetooth.integration.testapp.ui.common.setViewEditText
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import java.util.UUID

class AdvertiserFragment : Fragment() {

    private companion object {
        private const val TAG = "AdvertiserFragment"
    }

    private lateinit var advertiserViewModel: AdvertiserViewModel

    private lateinit var bluetoothLe: BluetoothLe

    private var advertiseDataAdapter: AdvertiseDataAdapter? = null

    private val advertiseScope = CoroutineScope(Dispatchers.Main + Job())
    private var advertiseJob: Job? = null

    private var isAdvertising: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonAdvertise?.text = getString(R.string.stop_advertising)
            } else {
                _binding?.buttonAdvertise?.text = getString(R.string.start_advertising)
                advertiseJob?.cancel()
                advertiseJob = null
            }
            _binding?.viewOverlay?.isVisible = value
        }

    private var _binding: FragmentAdvertiserBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        advertiserViewModel = ViewModelProvider(this)[AdvertiserViewModel::class.java]

        bluetoothLe = BluetoothLe(requireContext())

        _binding = FragmentAdvertiserBinding.inflate(inflater, container, false)

        initData()

        binding.checkBoxIncludeDeviceName.setOnCheckedChangeListener { _, isChecked ->
            advertiserViewModel.includeDeviceName = isChecked
        }

        binding.checkBoxConnectable.setOnCheckedChangeListener { _, isChecked ->
            advertiserViewModel.connectable = isChecked
        }

        binding.checkBoxDiscoverable.setOnCheckedChangeListener { _, isChecked ->
            advertiserViewModel.discoverable = isChecked
        }

        binding.buttonAddData.setOnClickListener {
            with(PopupMenu(requireContext(), binding.buttonAddData)) {
                menu.add(getString(R.string.service_uuid))
                menu.add(getString(R.string.service_data))
                menu.add(getString(R.string.manufacturer_data))

                setOnMenuItemClickListener { menuItem ->
                    showDialogFor(menuItem.title.toString())
                    true
                }
                show()
            }
        }

        advertiseDataAdapter = AdvertiseDataAdapter(
            advertiserViewModel.advertiseData,
            ::onClickRemoveAdvertiseData
        )
        binding.recyclerViewAdvertiseData.adapter = advertiseDataAdapter

        binding.buttonAdvertise.setOnClickListener {
            if (advertiseJob?.isActive == true) {
                isAdvertising = false
            } else {
                startAdvertise()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isAdvertising = false
    }

    private fun initData() {
        binding.checkBoxIncludeDeviceName.isChecked = advertiserViewModel.includeDeviceName
        binding.checkBoxConnectable.isChecked = advertiserViewModel.connectable
        binding.checkBoxDiscoverable.isChecked = advertiserViewModel.discoverable
    }

    private fun showDialogFor(title: String) {
        when (title) {
            getString(R.string.service_uuid) -> showDialogForServiceUuid()
            getString(R.string.service_data) -> showDialogForServiceData()
            getString(R.string.manufacturer_data) -> showDialogForManufacturerData()
        }
    }

    private fun showDialogForServiceUuid() {
        val editText = EditText(requireActivity())
        editText.hint = getString(R.string.uuid_or_service_name)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.service_uuid))
            .setViewEditText(editText)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val editTextInput = editText.text.toString()

                advertiserViewModel.serviceUuids.add(UUID.fromString(editTextInput))
                refreshAdvertiseData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun showDialogForServiceData() {
        val view = layoutInflater.inflate(R.layout.dialog_service_data, null)
        val editTextUuidOrServiceName =
            view.findViewById<EditText>(R.id.edit_text_uuid_or_service_name)
        val editTextDataHex = view.findViewById<EditText>(R.id.edit_text_data_hex)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.service_data))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val editTextUuidOrServiceNameInput = editTextUuidOrServiceName.text.toString()
                val editTextDataHexInput = editTextDataHex.text.toString()

                val serviceData = Pair(
                    UUID.fromString(editTextUuidOrServiceNameInput),
                    editTextDataHexInput.toByteArray()
                )
                advertiserViewModel.serviceDatas.add(serviceData)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun showDialogForManufacturerData() {
        val view = layoutInflater.inflate(R.layout.dialog_manufacturer_data, null)
        val editText16BitCompanyIdentifier =
            view.findViewById<EditText>(R.id.edit_text_16_bit_company_identifier)
        val editTextDataHex = view.findViewById<EditText>(R.id.edit_text_data_hex)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.manufacturer_data))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val editText16BitCompanyIdentifierInput =
                    editText16BitCompanyIdentifier.text.toString()
                val editTextDataHexInput = editTextDataHex.text.toString()

                val manufacturerData = Pair(
                    editText16BitCompanyIdentifierInput.toInt(),
                    editTextDataHexInput.toByteArray()
                )
                advertiserViewModel.manufacturerDatas.add(manufacturerData)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshAdvertiseData() {
        advertiseDataAdapter?.advertiseData = advertiserViewModel.advertiseData
        advertiseDataAdapter?.notifyDataSetChanged()
    }

    private fun onClickRemoveAdvertiseData(index: Int) {
        advertiserViewModel.removeAdvertiseDataAtIndex(index)
        advertiseDataAdapter?.advertiseData = advertiserViewModel.advertiseData
        advertiseDataAdapter?.notifyItemRemoved(index)
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun startAdvertise() {
        advertiseJob = advertiseScope.launch {
            isAdvertising = true

            bluetoothLe.advertise(advertiserViewModel.advertiseParams)
                .collect {
                    Log.d(TAG, "AdvertiseResult collected: $it")

                    when (it) {
                        AdvertiseResult.ADVERTISE_STARTED -> {
                            toast("ADVERTISE_STARTED").show()
                        }
                        AdvertiseResult.ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                            isAdvertising = false
                            toast("ADVERTISE_FAILED_DATA_TOO_LARGE").show()
                        }
                        AdvertiseResult.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                            isAdvertising = false
                            toast("ADVERTISE_FAILED_FEATURE_UNSUPPORTED").show()
                        }
                        AdvertiseResult.ADVERTISE_FAILED_INTERNAL_ERROR -> {
                            isAdvertising = false
                            toast("ADVERTISE_FAILED_INTERNAL_ERROR").show()
                        }
                        AdvertiseResult.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                            isAdvertising = false
                            toast("ADVERTISE_FAILED_TOO_MANY_ADVERTISERS").show()
                        }
                    }
                }
        }
    }
}
