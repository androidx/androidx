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
import androidx.bluetooth.BluetoothLe
import junit.framework.TestCase.fail
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricScanTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private var bluetoothLe = BluetoothLe(context)
    private companion object {
        private const val TIMEOUT_MS: Long = 2_000
    }

    @Test
    fun scanTest() = runTest {
        try {
            withTimeout(TIMEOUT_MS) {
                bluetoothLe.scan(listOf()).collect {
                    // Should not find any device
                    fail()
                }
            }
            fail()
        } catch (e: TimeoutCancellationException) {
            // expected
        }
    }
}