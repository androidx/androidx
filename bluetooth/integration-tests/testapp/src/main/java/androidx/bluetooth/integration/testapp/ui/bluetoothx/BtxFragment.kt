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

package androidx.bluetooth.integration.testapp.ui.bluetoothx

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.bluetooth.core.BluetoothManager
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentBtxBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class BtxFragment : Fragment() {

    companion object {
        const val TAG = "BtxFragment"
    }

    private var _binding: FragmentBtxBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBtxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonPrevious.setOnClickListener {
            findNavController().navigate(R.id.action_BtxFragment_to_FwkFragment)
        }

        binding.buttonScan.setOnClickListener {
            scan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scan() {
        Log.d(TAG, "scan() called")

        val bluetoothManager = BluetoothManager(requireContext())

        @Suppress("UNUSED_VARIABLE")
        // TODO(ofy) Use below
        val bluetoothAdapter = bluetoothManager.getAdapter()

        // TODO(ofy) Convert to BluetoothX classes
//        val bleScanner = bluetoothAdapter?.bluetoothLeScanner
//
//        val scanSettings = ScanSettings.Builder()
//            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//            .build()
//
//        bleScanner?.startScan(null, scanSettings, scanCallback)
//
//        Toast.makeText(context, getString(R.string.scan_start_message), Toast.LENGTH_LONG)
//            .show()
    }
}
