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

import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentScannerBinding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

// TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
import androidx.bluetooth.integration.testapp.experimental.BluetoothLe
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScannerFragment : Fragment() {

    private companion object {
        private const val TAG = "ScannerFragment"
    }

    private lateinit var scannerViewModel: ScannerViewModel

    // TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
    private lateinit var bluetoothLe: BluetoothLe

    private var scannerAdapter: ScannerAdapter? = null

    private val scanScope = CoroutineScope(Dispatchers.Main + Job())
    private var scanJob: Job? = null

    private var isScanning: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonScan?.text = getString(R.string.stop_scanning)
            } else {
                _binding?.buttonScan?.text = getString(R.string.start_scanning)
                scanJob?.cancel()
            }
        }

    private var _binding: FragmentScannerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        scannerViewModel = ViewModelProvider(this)[ScannerViewModel::class.java]

        bluetoothLe = BluetoothLe(requireContext())

        _binding = FragmentScannerBinding.inflate(inflater, container, false)

        scannerAdapter = ScannerAdapter { scanResult -> onClickScanResult(scanResult) }
        binding.recyclerViewScanResults.adapter = scannerAdapter
        binding.recyclerViewScanResults.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        initData()

        binding.buttonScan.setOnClickListener {
            if (scanJob?.isActive == true) {
                isScanning = false
            } else {
                startScan()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isScanning = false
    }

    private fun initData() {
        scannerAdapter?.submitList(scannerViewModel.results)
        scannerAdapter?.notifyItemRangeChanged(0, scannerViewModel.results.size)
    }

    private fun startScan() {
        // TODO(ofy) Migrate to androidx.bluetooth.BluetoothLe once scan API is in place
        val scanSettings = ScanSettings.Builder()
            .build()

        scanJob = scanScope.launch {
            isScanning = true

            bluetoothLe.scan(scanSettings)
                .collect {
                    Log.d(TAG, "ScanResult collected: $it")

                    if (scannerViewModel.addScanResultIfNew(it)) {
                        scannerAdapter?.submitList(scannerViewModel.results)
                        scannerAdapter?.notifyItemInserted(scannerViewModel.results.size)
                    }
                }
        }
    }

    private fun onClickScanResult(scanResult: ScanResult) {
        Log.d(TAG, "onClickScanResult() called with: scanResult = $scanResult")
    }
}
