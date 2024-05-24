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

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentAdvertiserBinding
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.common.setViewEditText
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdvertiserFragment : Fragment() {

    private companion object {
        private const val TAG = "AdvertiserFragment"
    }

    private var advertiseDataAdapter: AdvertiseDataAdapter? = null

    private val viewModel: AdvertiserViewModel by viewModels()

    private var _binding: FragmentAdvertiserBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvertiserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.checkBoxIncludeDeviceName.setOnCheckedChangeListener { _, isChecked ->
            viewModel.includeDeviceName = isChecked
        }

        binding.checkBoxConnectable.setOnCheckedChangeListener { _, isChecked ->
            viewModel.connectable = isChecked
        }

        binding.checkBoxDiscoverable.setOnCheckedChangeListener { _, isChecked ->
            viewModel.discoverable = isChecked
        }

        binding.textInputEditTextDuration.doAfterTextChanged {
            val maxDuration: Long = 180_000
            var duration = (it.toString()).toLongOrNull() ?: 0
            if (duration > maxDuration) {
                binding.textInputEditTextDuration.setText(maxDuration.toString())
                duration = maxDuration
            }
            viewModel.durationMillis = duration
        }

        binding.buttonAddData.setOnClickListener {
            with(PopupMenu(requireContext(), binding.buttonAddData)) {
                menu.add(getString(R.string.service_uuid))
                menu.add(getString(R.string.service_data))
                menu.add(getString(R.string.manufacturer_data))
                menu.add(getString(R.string.service_solicitation_uuid))

                setOnMenuItemClickListener { menuItem ->
                    showDialogFor(menuItem.title.toString())
                    true
                }
                show()
            }
        }

        advertiseDataAdapter =
            AdvertiseDataAdapter(viewModel.advertiseData, ::onClickRemoveAdvertiseData)
        binding.recyclerViewAdvertiseData.adapter = advertiseDataAdapter

        binding.buttonAdvertise.setOnClickListener {
            if (viewModel.advertiseJob?.isActive == true) {
                viewModel.advertiseJob?.cancel()
            } else {
                viewModel.startAdvertise()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect(::updateUi)
        }

        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(advertiserUiState: AdvertiserUiState) {
        advertiserUiState.isAdvertising.let { isAdvertising ->
            if (isAdvertising) {
                binding.buttonAdvertise.text = getString(R.string.stop_advertising)
                binding.buttonAdvertise.backgroundTintList = getColor(R.color.red_500)
            } else {
                binding.buttonAdvertise.text = getString(R.string.start_advertising)
                binding.buttonAdvertise.backgroundTintList = getColor(R.color.indigo_500)
            }
            binding.checkBoxIncludeDeviceName.isEnabled = !isAdvertising
            binding.checkBoxConnectable.isEnabled = !isAdvertising
            binding.checkBoxDiscoverable.isEnabled = !isAdvertising
            binding.buttonAddData.isEnabled = !isAdvertising
            binding.viewRecyclerViewOverlay.isVisible = isAdvertising
        }

        advertiserUiState.resultMessage?.let {
            toast(it).show()
            viewModel.clearResultMessage()
        }
    }

    private fun initData() {
        if (
            Build.VERSION.SDK_INT < 31 ||
                (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED)
        ) {
            (requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)
                ?.adapter
                ?.name
                ?.let { binding.textInputEditTextDisplayName.setText(it) }
        }

        binding.checkBoxIncludeDeviceName.isChecked = viewModel.includeDeviceName
        binding.checkBoxConnectable.isChecked = viewModel.connectable
        binding.checkBoxDiscoverable.isChecked = viewModel.discoverable
        binding.textInputEditTextDuration.setText(viewModel.durationMillis.toString())
    }

    private fun showDialogFor(title: String) {
        when (title) {
            getString(R.string.service_uuid) -> showDialogForServiceUuid()
            getString(R.string.service_data) -> showDialogForServiceData()
            getString(R.string.manufacturer_data) -> showDialogForManufacturerData()
            getString(R.string.service_solicitation_uuid) -> showDialogForServiceSolicitationUuid()
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

                viewModel.serviceUuids.add(UUID.fromString(editTextInput))
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

                val serviceData =
                    Pair(
                        UUID.fromString(editTextUuidOrServiceNameInput),
                        editTextDataHexInput.toByteArray()
                    )
                viewModel.serviceDatas.add(serviceData)
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

                val manufacturerData =
                    Pair(
                        editText16BitCompanyIdentifierInput.toInt(),
                        editTextDataHexInput.toByteArray()
                    )
                viewModel.manufacturerDatas.add(manufacturerData)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun showDialogForServiceSolicitationUuid() {
        val editText = EditText(requireActivity())
        editText.hint = getString(R.string.service_solicitation_uuid)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.service_solicitation_uuid))
            .setViewEditText(editText)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val editTextInput = editText.text.toString()

                viewModel.serviceSolicitationUuids.add(UUID.fromString(editTextInput))
                refreshAdvertiseData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshAdvertiseData() {
        advertiseDataAdapter?.advertiseData = viewModel.advertiseData
        advertiseDataAdapter?.notifyDataSetChanged()
    }

    private fun onClickRemoveAdvertiseData(index: Int) {
        viewModel.removeAdvertiseDataAtIndex(index)
        advertiseDataAdapter?.advertiseData = viewModel.advertiseData
        advertiseDataAdapter?.notifyItemRemoved(index)
    }
}
