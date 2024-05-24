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

import android.os.Build
import androidx.bluetooth.AdvertiseException
import androidx.bluetooth.AdvertiseImpl
import androidx.bluetooth.AdvertiseParams
import androidx.bluetooth.BluetoothLe
import java.util.UUID
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricAdvertiseTest {

    private val bluetoothLe = BluetoothLe(RuntimeEnvironment.getApplication())

    @Before
    fun setUp() {
        // TODO: Workaround for Robolectric doesn't support startAdvertisingSet.
        //       Remove this once it's supported.
        if (Build.VERSION.SDK_INT >= 26) {
            bluetoothLe.advertiseImpl = AdvertiseImplForTesting()
        }
    }

    @Test
    fun advertiseSuccess() = runTest {
        assumeTrue(
            "Can only run on API Level 23 or newer because of reasons",
            Build.VERSION.SDK_INT < 26
        )
        val params = AdvertiseParams()

        launch {
            val result = bluetoothLe.advertise(params).first()

            assertEquals(BluetoothLe.ADVERTISE_STARTED, result)
        }
    }

    /**
     * Tests if [BluetoothLe.advertise] returns error when data is larger than the legacy advertise
     * limit (31 bytes)
     */
    @Test
    fun advertiseTooLargeData() = runTest {
        val parcelUuid = UUID.randomUUID()
        val serviceData = "sampleAdvertiseDataTooLargeToAdvertise".toByteArray(Charsets.UTF_8)

        val advertiseParams = AdvertiseParams(serviceData = mapOf(parcelUuid to serviceData))

        launch {
            try {
                val result = bluetoothLe.advertise(advertiseParams).first()

                if (Build.VERSION.SDK_INT >= 26) {
                    assertEquals(BluetoothLe.ADVERTISE_STARTED, result)
                }
            } catch (throwable: Throwable) {
                if (Build.VERSION.SDK_INT < 26) {
                    assertTrue(throwable is AdvertiseException)
                }
            }
        }
    }
}

class AdvertiseImplForTesting : AdvertiseImpl {
    override fun advertise(
        advertiseParams: AdvertiseParams,
    ): Flow<@BluetoothLe.AdvertiseResult Int> = flowOf(BluetoothLe.ADVERTISE_STARTED)
}
