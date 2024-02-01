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

package androidx.bluetooth.integration.testapp.ui.main

import androidx.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class MainViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "MainViewModel"
    }

    var selectedBluetoothDevice: BluetoothDevice? = null

    val navigateToConnections: LiveData<Int>
        get() = _navigateToConnections
    private val _navigateToConnections = MutableLiveData<Int>()

    fun navigateToConnections() {
        _navigateToConnections.value = Random.nextInt()
    }
}
