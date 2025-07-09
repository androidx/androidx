/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.content.Context
import androidx.pdf.service.connect.PdfSandboxHandleImpl
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PdfSandboxHandleImplTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun test_bindsServiceAndUpdatesState() = runTest {
        val connectionState = MutableStateFlow(false)
        val handle =
            PdfSandboxHandleImpl(
                context,
                onConnected = { connectionState.value = true },
                onDisconnected = { connectionState.value = false },
            )

        assertFalse(connectionState.value) // Initial state

        handle.connect()

        // Wait for connection
        // This relies on onConnected() being called from onServiceConnected
        val connected = connectionState.first { it }

        assertTrue(connected)

        // Clean up
        handle.close()
        assertFalse(connectionState.value)
    }

    @Test
    fun test_initMultiplePdfSession() = runTest {
        // Connection 1
        val connectionState1 = MutableStateFlow(false)
        val handle1 =
            PdfSandboxHandleImpl(
                context,
                onConnected = { connectionState1.value = true },
                onDisconnected = { connectionState1.value = false },
            )

        val conn1StartTime = System.currentTimeMillis()
        handle1.connect()

        // Suspend until connection is established
        connectionState1.first { isConnected -> isConnected }
        val conn1EndTime = System.currentTimeMillis()
        val conn1Time = conn1EndTime - conn1StartTime
        println("Connection 1 time: $conn1Time ms")

        // Connection 2
        val connectionState2 = MutableStateFlow(false)
        val handle2 =
            PdfSandboxHandleImpl(
                context,
                onConnected = { connectionState2.value = true },
                onDisconnected = { connectionState2.value = false },
            )

        val conn2StartTime = System.currentTimeMillis()
        handle2.connect()

        // Suspend until connection is established
        connectionState2.first { isConnected -> isConnected }
        val conn2EndTime = System.currentTimeMillis()
        val conn2Time = conn2EndTime - conn2StartTime
        println("Connection 2 time: $conn2Time ms")

        // Note: conn2Time is expected to be significantly less than conn1Time in isolation.
        // However, when running the full test suite, the service might already be active from
        // previous tests, causing conn1Time and conn2Time to be similar, which can make
        // direct comparison flaky.

        handle1.close()
        // Assert only connection from handle1 is disActive; while handle2 is still connected
        assertFalse(connectionState1.value)
        assertTrue(connectionState2.value)

        // clean up
        handle2.close()
        assertFalse(connectionState2.value)
    }

    @Test
    fun test_multipleCloseOnSameHandle() = runTest {
        val connectionState = MutableStateFlow(false)
        val handle =
            PdfSandboxHandleImpl(
                context,
                onConnected = { connectionState.value = true },
                onDisconnected = { connectionState.value = false },
            )

        handle.connect()

        // Assert a connection actually established
        connectionState.first { it }

        handle.close()
        // Assert a disconnection on calling `close`
        assertFalse(connectionState.value)

        // Assert calling close again will not throw exception
        handle.close()
        assertFalse(connectionState.value)
    }

    @Test
    fun test_callingCloseFromWorkerThread() = runTest {
        val connectionState = MutableStateFlow(false)
        val handle =
            PdfSandboxHandleImpl(
                context,
                onConnected = { connectionState.value = true },
                onDisconnected = { connectionState.value = false },
            )

        handle.connect()

        // Assert a connection actually established
        connectionState.first { it }

        withContext(Dispatchers.IO) {
            handle.close()
            assertFalse(connectionState.value)
        }
    }

    @Test
    fun test_close_withoutConnect_skipsUnbind() {
        var unbindAttempted = false
        val handle = PdfSandboxHandleImpl(context, onDisconnected = { unbindAttempted = true })

        // Directly call close on handle; without calling connect
        handle.close()

        assertTrue("Unbind not attempted when service not bound", !unbindAttempted)
    }
}
