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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class FwkFragment : Fragment() {

    companion object {
        const val TAG = "FwkFragment"
        val ServiceUUID: ParcelUuid = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d(TAG, "onScanResult() called with: callbackType = $callbackType, result = $result")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
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

    private var _binding: FragmentFwkBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFwkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonNext.setOnClickListener {
            findNavController().navigate(R.id.action_FwkFragment_to_BtxFragment)
        }

        binding.buttonScan.setOnClickListener {
            scan()
        }

        binding.switchAdvertise.setOnClickListener {
            if (binding.switchAdvertise.isChecked) startAdvertise()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    private fun scan() {
        Log.d(TAG, "scan() called")

        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter = bluetoothManager?.adapter

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(null, scanSettings, scanCallback)

        Toast.makeText(context, getString(R.string.scan_start_message), Toast.LENGTH_LONG)
            .show()
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

        Toast.makeText(context, getString(R.string.advertise_start_message), Toast.LENGTH_LONG)
            .show()
    }
}
