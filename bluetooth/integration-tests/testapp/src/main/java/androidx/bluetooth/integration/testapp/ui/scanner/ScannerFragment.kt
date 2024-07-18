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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.databinding.FragmentScannerBinding
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.main.MainViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    internal companion object {
        private const val TAG = "ScannerFragment"
    }

    private lateinit var scannerAdapter: ScannerAdapter

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

        scannerAdapter = ScannerAdapter(::onClickScanResult)
        binding.recyclerViewScanResults.adapter = scannerAdapter
        binding.recyclerViewScanResults.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        binding.buttonScan.setOnClickListener {
            if (viewModel.scanJob?.isActive == true) {
                viewModel.scanJob?.cancel()
            } else {
                viewModel.startScan()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect(::updateUi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(scannerUiState: ScannerUiState) {
        scannerAdapter.submitList(scannerUiState.scanResults)

        if (scannerUiState.isScanning) {
            binding.buttonScan.text = getString(R.string.stop_scanning)
            binding.buttonScan.backgroundTintList = getColor(R.color.red_500)
        } else {
            binding.buttonScan.text = getString(R.string.start_scanning)
            binding.buttonScan.backgroundTintList = getColor(R.color.indigo_500)
        }
    }

    private fun onClickScanResult(bluetoothDevice: BluetoothDevice) {
        Log.d(TAG, "onClickScanResult() called with: bluetoothDevice = $bluetoothDevice")

        viewModel.scanJob?.cancel()

        mainViewModel.selectedBluetoothDevice = bluetoothDevice
        mainViewModel.navigateToConnections()
    }
}
