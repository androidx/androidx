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

package androidx.bluetooth.testing

import android.content.Context
import androidx.bluetooth.AdvertiseParams
import androidx.bluetooth.BluetoothLe
import java.util.UUID
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricAdvertiseTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private var bluetoothLe = BluetoothLe(context)

    @Test
    fun advertiseSuccess() = runTest {
        val params = AdvertiseParams()
        launch {
            bluetoothLe.advertise(params) { result ->
                Assert.assertEquals(BluetoothLe.ADVERTISE_STARTED, result)
                cancel()
            }
        }
    }

    /**
     * Tests if [BluetoothLe.advertise] returns error when data is larger than
     * the legacy advertise limit (31 bytes)
     */
    @Test
    fun advertiseTooLargeData() = runTest {
        val parcelUuid = UUID.randomUUID()
        val serviceData = "sampleAdvertiseDataTooLargeToAdvertise".toByteArray(Charsets.UTF_8)

        val advertiseParams = AdvertiseParams(
            serviceData = mapOf(parcelUuid to serviceData)
        )

        launch {
            bluetoothLe.advertise(advertiseParams) { result ->
                Assert.assertEquals(BluetoothLe.ADVERTISE_FAILED_DATA_TOO_LARGE, result)
            }
        }
    }
}
