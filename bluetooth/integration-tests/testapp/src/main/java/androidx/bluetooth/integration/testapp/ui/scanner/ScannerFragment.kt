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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentScannerBinding
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.main.MainViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    internal companion object {
        private const val TAG = "ScannerFragment"
    }

    @Inject
    lateinit var bluetoothLe: BluetoothLe

    private val scanScope = CoroutineScope(Dispatchers.Main + Job())
    private var scanJob: Job? = null

    private var isScanning: Boolean = false
        set(value) {
            field = value
            if (value) {
                _binding?.buttonScan?.text = getString(R.string.stop_scanning)
                _binding?.buttonScan?.backgroundTintList = getColor(R.color.red_500)
            } else {
                _binding?.buttonScan?.text = getString(R.string.start_scanning)
                _binding?.buttonScan?.backgroundTintList = getColor(R.color.indigo_500)
                scanJob?.cancel()
                scanJob = null
            }
        }

    private val viewModel by viewModels<ScannerViewModel>()

    private val mainViewModel by activityViewModels<MainViewModel>()

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scannerAdapter = ScannerAdapter(::onClickScanResult)
        binding.recyclerViewScanResults.adapter = scannerAdapter
        binding.recyclerViewScanResults.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        binding.buttonScan.setOnClickListener {
            if (scanJob?.isActive == true) {
                isScanning = false
            } else {
                startScan()
            }
        }

        viewModel.scanResults
            .observe(viewLifecycleOwner) { scannerAdapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isScanning = false
        _binding = null
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        Log.d(TAG, "startScan() called")

        scanJob = scanScope.launch {
            Log.d(TAG, "bluetoothLe.scan() called")
            isScanning = true

            try {
                bluetoothLe.scan()
                    .collect {
                        Log.d(TAG, "bluetoothLe.scan() collected: ScanResult = $it")

                        viewModel.addScanResultIfNew(it)
                    }
            } catch (exception: Exception) {
                isScanning = false

                if (exception is CancellationException) {
                    Log.e(TAG, "bluetoothLe.scan() CancellationException", exception)
                }
            }
        }
    }

    private fun onClickScanResult(bluetoothDevice: BluetoothDevice) {
        Log.d(TAG, "onClickScanResult() called with: bluetoothDevice = $bluetoothDevice")

        isScanning = false

        mainViewModel.selectedBluetoothDevice = bluetoothDevice
        mainViewModel.navigateToConnections()
    }
}
