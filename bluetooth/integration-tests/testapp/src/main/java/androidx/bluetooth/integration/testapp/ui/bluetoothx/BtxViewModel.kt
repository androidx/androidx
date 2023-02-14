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

package androidx.bluetooth.integration.testapp.ui.bluetoothx

import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.ViewModel

class BtxViewModel : ViewModel() {

    companion object {
        const val TAG = "BtxViewModel"
    }

    val scanResults = mutableMapOf<String, ScanResult>()

    init {
        Log.d(TAG, "init called")
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared() called")
    }
}
