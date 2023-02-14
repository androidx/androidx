/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.bluetooth.integration.testapp.ui.framework

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentFwkBinding
import androidx.bluetooth.integration.testapp.ui.common.ScanResultAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class FwkFragment : Fragment() {

    companion object {
        const val TAG = "FwkFragment"
        val ServiceUUID: ParcelUuid = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    }

    private var scanResultAdapter: ScanResultAdapter? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "onScanResult() called with: callbackType = $callbackType, result = $result")

            fwkViewModel.scanResults[result.device.address] = result
            scanResultAdapter?.submitList(fwkViewModel.scanResults.values.toMutableList())
            scanResultAdapter?.notifyItemInserted(fwkViewModel.scanResults.size)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(TAG, "onBatchScanResults() called with: results = $results")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed() called with: errorCode = $errorCode")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.d(TAG, "onStartFailure() called with: errorCode = $errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "onStartSuccess() called")
        }
    }

    private var isScanning = false

    private lateinit var fwkViewModel: FwkViewModel

    private var _binding: FragmentFwkBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(
            TAG, "onCreateView() called with: inflater = $inflater, " +
                "container = $container, savedInstanceState = $savedInstanceState"
        )
        fwkViewModel = ViewModelProvider(this)[FwkViewModel::class.java]

        _binding = FragmentFwkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanResultAdapter = ScanResultAdapter { scanResult -> scanResultOnClick(scanResult) }
        binding.recyclerView.adapter = scanResultAdapter

        binding.buttonScan.setOnClickListener {
            if (isScanning) stopScan()
            else startScan()
        }

        binding.switchAdvertise.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startAdvertise()
            else stopAdvertise()
        }

        binding.switchGattServer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) openGattServer()
            else closeGattServer()
        }
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        stopScan()
        bluetoothGattServer?.close()
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun startScan() {
        Log.d(TAG, "startScan() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter = bluetoothManager?.adapter

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(null, scanSettings, scanCallback)

        isScanning = true
        binding.buttonScan.text = getString(R.string.stop_scanning)

        Toast.makeText(context, getString(R.string.scan_start_message), Toast.LENGTH_SHORT)
            .show()
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun stopScan() {
        Log.d(TAG, "stopScan() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter = bluetoothManager?.adapter

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner

        bleScanner?.stopScan(scanCallback)

        isScanning = false
        _binding?.buttonScan?.text = getString(R.string.scan_using_fwk)
    }

    private fun scanResultOnClick(scanResult: ScanResult) {
        Log.d(TAG, "scanResultOnClick() called with: scanResult = $scanResult")
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun startAdvertise() {
        Log.d(TAG, "startAdvertise() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter = bluetoothManager?.adapter

        val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTimeout(0)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ServiceUUID)
            .setIncludeDeviceName(true)
            .build()

        bleAdvertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)

        Toast.makeText(context, getString(R.string.advertise_start_message), Toast.LENGTH_SHORT)
            .show()
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun stopAdvertise() {
        Log.d(TAG, "stopAdvertise() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter = bluetoothManager?.adapter

        val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        bleAdvertiser?.stopAdvertising(advertiseCallback)
    }

    private var bluetoothGattServer: BluetoothGattServer? = null

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun openGattServer() {
        Log.d(TAG, "openGattServer() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        bluetoothGattServer = bluetoothManager?.openGattServer(
            requireContext(),
            object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(
                    device: BluetoothDevice?,
                    status: Int,
                    newState: Int
                ) {
                    Log.d(
                        TAG,
                        "onConnectionStateChange() called with: device = $device" +
                            ", status = $status, newState = $newState"
                    )
                }

                override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                    Log.d(TAG, "onServiceAdded() called with: status = $status, service = $service")
                }

                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    Log.d(
                        TAG,
                        "onCharacteristicReadRequest() called with: device = $device" +
                            ", requestId = $requestId, offset = $offset" +
                            ", characteristic = $characteristic"
                    )
                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    Log.d(
                        TAG,
                        "onCharacteristicWriteRequest() called with: device = $device" +
                            ", requestId = $requestId, characteristic = $characteristic" +
                            ", preparedWrite = $preparedWrite, responseNeeded = $responseNeeded" +
                            ", offset = $offset, value = $value"
                    )
                }

                override fun onDescriptorReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    descriptor: BluetoothGattDescriptor?
                ) {
                    Log.d(
                        TAG,
                        "onDescriptorReadRequest() called with: device = $device" +
                            ", requestId = $requestId, offset = $offset, descriptor = $descriptor"
                    )
                }

                override fun onDescriptorWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    descriptor: BluetoothGattDescriptor?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    Log.d(
                        TAG,
                        "onDescriptorWriteRequest() called with: device = $device" +
                            ", requestId = $requestId, descriptor = $descriptor" +
                            ", preparedWrite = $preparedWrite, responseNeeded = $responseNeeded" +
                            ", offset = $offset, value = $value"
                    )
                }

                override fun onExecuteWrite(
                    device: BluetoothDevice?,
                    requestId: Int,
                    execute: Boolean
                ) {
                    Log.d(
                        TAG,
                        "onExecuteWrite() called with: device = $device, requestId = $requestId" +
                            ", execute = $execute"
                    )
                }

                override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
                    Log.d(
                        TAG,
                        "onNotificationSent() called with: device = $device, status = $status"
                    )
                }

                override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                    Log.d(TAG, "onMtuChanged() called with: device = $device, mtu = $mtu")
                }

                override fun onPhyUpdate(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    Log.d(
                        TAG, "onPhyUpdate() called with: device = $device, txPhy = $txPhy" +
                            ", rxPhy = $rxPhy, status = $status"
                    )
                }

                override fun onPhyRead(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    Log.d(
                        TAG,
                        "onPhyRead() called with: device = $device, txPhy = $txPhy" +
                            ", rxPhy = $rxPhy, status = $status"
                    )
                }
            })
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun closeGattServer() {
        Log.d(TAG, "closeGattServer() called")

        bluetoothGattServer?.close()
    }
}
