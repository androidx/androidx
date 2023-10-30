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

import androidx.bluetooth.ScanResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScannerViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"
    }

    val scanResults: LiveData<List<ScanResult>>
        get() = _scanResults
    private val _scanResults = MutableLiveData<List<ScanResult>>()
    private val _scanResultsMap = mutableMapOf<String, ScanResult>()

    fun addScanResultIfNew(scanResult: ScanResult) {
        val deviceAddress = scanResult.deviceAddress.address

        if (_scanResultsMap.containsKey(deviceAddress).not()) {
            _scanResultsMap[deviceAddress] = scanResult
            _scanResults.value = _scanResultsMap.values.toList()
        }
    }
}
