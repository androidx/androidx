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

// TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once Gatt Server API is in place
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.bluetooth.integration.testapp.experimental.BluetoothLe as BluetoothLeExperimental
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.common.setViewEditText
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdvertiserFragment : Fragment() {

    private companion object {
        private const val TAG = "AdvertiserFragment"

        private const val TAB_ADVERTISER_POSITION = 0
    }

    private lateinit var bluetoothLe: BluetoothLe

    // TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
    private lateinit var bluetoothLeExperimental: BluetoothLeExperimental

    private var advertiseDataAdapter: AdvertiseDataAdapter? = null

    private val advertiseScope = CoroutineScope(Dispatchers.Main + Job())
    private var advertiseJob: Job? = null

    private val gattServerScope = CoroutineScope(Dispatchers.Main + Job())
    private var gattServerJob: Job? = null

    private var isAdvertising: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonAdvertise?.text = getString(R.string.stop_advertising)
                _binding?.buttonAdvertise?.backgroundTintList = getColor(R.color.red_500)
            } else {
                _binding?.buttonAdvertise?.text = getString(R.string.start_advertising)
                _binding?.buttonAdvertise?.backgroundTintList = getColor(R.color.indigo_500)
                advertiseJob?.cancel()
                advertiseJob = null
            }
            _binding?.checkBoxIncludeDeviceName?.isEnabled = !value
            _binding?.checkBoxConnectable?.isEnabled = !value
            _binding?.checkBoxDiscoverable?.isEnabled = !value
            _binding?.buttonAddData?.isEnabled = !value
            _binding?.viewRecyclerViewOverlay?.isVisible = value
        }

    private var isGattServerOpen: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonGattServer?.text = getString(R.string.stop_gatt_server)
                _binding?.buttonGattServer?.backgroundTintList = getColor(R.color.red_500)
            } else {
                _binding?.buttonGattServer?.text = getString(R.string.open_gatt_server)
                _binding?.buttonGattServer?.backgroundTintList = getColor(R.color.indigo_500)
                gattServerJob?.cancel()
                gattServerJob = null
            }
        }

    private var showingAdvertiser: Boolean = false
        set(value) {
            field = value
            _binding?.layoutAdvertiser?.isVisible = value
            _binding?.layoutGattServer?.isVisible = !value
        }

    private val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            showingAdvertiser = tab.position == TAB_ADVERTISER_POSITION
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
        }
    }

    private val viewModel: AdvertiserViewModel by viewModels()

    private var _binding: FragmentAdvertiserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bluetoothLe = BluetoothLe(requireContext())

        bluetoothLeExperimental = BluetoothLeExperimental(requireContext())

        _binding = FragmentAdvertiserBinding.inflate(inflater, container, false)

        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        binding.checkBoxIncludeDeviceName.setOnCheckedChangeListener { _, isChecked ->
            viewModel.includeDeviceName = isChecked
        }

        binding.checkBoxConnectable.setOnCheckedChangeListener { _, isChecked ->
            viewModel.connectable = isChecked
        }

        binding.checkBoxDiscoverable.setOnCheckedChangeListener { _, isChecked ->
            viewModel.discoverable = isChecked
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
            viewModel.advertiseData,
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

        binding.buttonGattServer.setOnClickListener {
            if (gattServerJob?.isActive == true) {
                isGattServerOpen = false
            } else {
                openGattServer()
            }
        }

        initData()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isAdvertising = false
    }

    private fun initData() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            binding.textInputEditTextDisplayName.setText(
                (requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                    .adapter.name
            )
        }
        binding.checkBoxIncludeDeviceName.isChecked = viewModel.includeDeviceName
        binding.checkBoxConnectable.isChecked = viewModel.connectable
        binding.checkBoxDiscoverable.isChecked = viewModel.discoverable
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

                val serviceData = Pair(
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

                val manufacturerData = Pair(
                    editText16BitCompanyIdentifierInput.toInt(),
                    editTextDataHexInput.toByteArray()
                )
                viewModel.manufacturerDatas.add(manufacturerData)
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

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun startAdvertise() {
        advertiseJob = advertiseScope.launch {
            isAdvertising = true

            bluetoothLe.advertise(viewModel.advertiseParams)
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

    private fun openGattServer() {
        Log.d(TAG, "openGattServer() called")

        val sampleService =
            BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val sampleCharacteristic = BluetoothGattCharacteristic(
            UUID.randomUUID(),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        sampleService.addCharacteristic(sampleCharacteristic)

        val services = listOf(sampleService)

        gattServerJob = gattServerScope.launch {
            isGattServerOpen = true

            bluetoothLeExperimental.gattServer(services).collect { gattServerCallback ->
                Log.d(TAG, "openGattServer() called with: gattServerCallback = $gattServerCallback")
            }
        }
    }
}
