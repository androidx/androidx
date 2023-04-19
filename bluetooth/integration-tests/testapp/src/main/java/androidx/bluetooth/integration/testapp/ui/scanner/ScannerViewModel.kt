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
import androidx.lifecycle.ViewModel

class ScannerViewModel : ViewModel() {

    private companion object {
        private const val TAG = "ScannerViewModel"
    }

    internal val results: List<ScanResult> get() = _results.values.toList()

    private val _results = mutableMapOf<String, ScanResult>()

    fun addScanResultIfNew(scanResult: ScanResult): Boolean {
        val deviceAddress = scanResult.device.address

        if (_results.containsKey(deviceAddress)) return false
        _results[deviceAddress] = scanResult
        return true
    }
}
