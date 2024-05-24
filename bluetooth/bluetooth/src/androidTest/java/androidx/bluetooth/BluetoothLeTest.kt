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

package androidx.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test cases for [BluetoothLe] */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class BluetoothLeTest {

    companion object {
        private const val TAG = "BluetoothLeTest"
    }

    @Rule
    @JvmField
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 31)
            GrantPermissionRule.grant(android.Manifest.permission.BLUETOOTH_ADVERTISE)
        else GrantPermissionRule.grant(android.Manifest.permission.BLUETOOTH)

    private lateinit var bluetoothLe: BluetoothLe

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        Assume.assumeNotNull(bluetoothAdapter)
        Assume.assumeTrue(bluetoothAdapter.isEnabled)
        Assume.assumeTrue(bluetoothAdapter.bluetoothLeAdvertiser != null)

        bluetoothLe = BluetoothLe(context)
    }

    @Test
    fun advertise() = runTest {
        val advertiseParams = AdvertiseParams()

        val result = bluetoothLe.advertise(advertiseParams).first()

        assertEquals(BluetoothLe.ADVERTISE_STARTED, result)
    }

    @Test
    fun advertiseDataTooLarge() = runTest {
        val parcelUuid = UUID.randomUUID()
        val serviceData = "sampleAdvertiseDataTooBig".toByteArray(Charsets.UTF_8)

        val advertiseParams = AdvertiseParams(serviceData = mapOf(parcelUuid to serviceData))

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

    @Test
    fun advertise1000Millis() = runTest {
        val advertiseParams = AdvertiseParams(durationMillis = 1000)

        val advertiseJob = bluetoothLe.advertise(advertiseParams).launchIn(this)
        assertTrue(advertiseJob.isActive)

        delay(1100)
        assertFalse(advertiseJob.isActive)
    }
}
