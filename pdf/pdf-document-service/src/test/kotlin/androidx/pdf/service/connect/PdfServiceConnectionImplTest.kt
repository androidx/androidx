/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.service.connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.pdf.service.FakePdfDocumentRemote
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfServiceConnectionImplTest {

    private lateinit var context: Context
    private lateinit var serviceConnection: PdfServiceConnectionImpl
    private val componentName = ComponentName("androidx.pdf.testapp", "PdfDocumentServiceImpl")

    @Before
    fun setUp() {
        context = mock()
        serviceConnection = PdfServiceConnectionImpl(context)
    }

    @Test
    fun initial_state_isDisconnected() {
        assertThat(serviceConnection.isConnected).isFalse()
        assertThat(serviceConnection.documentBinder).isNull()
        assertThat(serviceConnection.needsToReopenDocument).isFalse()
    }

    @Test
    fun onServiceConnected_updatesStateToConnectedAndSetsDocumentBinder() {
        val fakeRemote = FakePdfDocumentRemote()

        serviceConnection.onServiceConnected(componentName, fakeRemote)

        assertThat(serviceConnection.isConnected).isTrue()
        assertThat(serviceConnection.documentBinder).isEqualTo(fakeRemote)
    }

    @Test
    fun disconnect_whenConnected_closesRemoteDocumentAndUnbindsService() {
        val fakeRemote = FakePdfDocumentRemote()
        serviceConnection.onServiceConnected(componentName, fakeRemote)

        assertThat(serviceConnection.isConnected).isTrue()

        serviceConnection.disconnect()

        // Verify that closePdfDocument was called on the remote binder before state reset
        assertThat(fakeRemote.isClosed).isTrue()
        verify(context).unbindService(serviceConnection)
        assertThat(serviceConnection.isConnected).isFalse()
        assertThat(serviceConnection.documentBinder).isNull()
    }

    @Test
    fun disconnect_whenNotConnected_doesNothing() {
        serviceConnection.disconnect()

        verify(context, never()).unbindService(any())
        assertThat(serviceConnection.isConnected).isFalse()
    }

    @Test
    fun onServiceDisconnected_marksNeedsToReopenDocumentAndDisconnects() {
        val fakeRemote = FakePdfDocumentRemote()
        serviceConnection.onServiceConnected(componentName, fakeRemote)

        serviceConnection.onServiceDisconnected(componentName)

        assertThat(serviceConnection.needsToReopenDocument).isTrue()
        assertThat(serviceConnection.isConnected).isFalse()
        assertThat(serviceConnection.documentBinder).isNull()
    }

    @Test
    fun onServiceDisconnected_withActiveJob_setsNeedsToReopenDocumentWithoutImmediateDisconnect() {
        val fakeRemote = FakePdfDocumentRemote()
        serviceConnection.onServiceConnected(componentName, fakeRemote)

        val activeJob = Job()
        serviceConnection.pendingJobs.add(activeJob)

        serviceConnection.onServiceDisconnected(componentName)

        assertThat(serviceConnection.needsToReopenDocument).isTrue()
        assertThat(serviceConnection.isConnected).isFalse()
        // unbindService shouldn't be called when processing is active
        verify(context, never()).unbindService(any())

        activeJob.cancel()
    }

    @Test
    fun connect_bindsServiceAndSuspendsUntilConnected() = runTest {
        whenever(context.bindService(any(), any(), eq(Context.BIND_AUTO_CREATE))).thenReturn(true)
        val testUri = Uri.parse("content://test/sample.pdf")
        val fakeRemote = FakePdfDocumentRemote()

        val connectJob = async { serviceConnection.connect(testUri) }
        runCurrent()

        // Verify bindService was invoked
        val intentCaptor = argumentCaptor<Intent>()
        verify(context)
            .bindService(
                intentCaptor.capture(),
                eq(serviceConnection),
                eq(Context.BIND_AUTO_CREATE),
            )
        assertThat(intentCaptor.firstValue.component?.className).contains("PdfDocumentServiceImpl")

        // Service is not connected yet, so connectJob should still be active
        assertThat(connectJob.isActive).isTrue()

        // Simulate service connection
        serviceConnection.onServiceConnected(componentName, fakeRemote)
        runCurrent()

        // Now connect should complete
        connectJob.await()

        assertThat(serviceConnection.isConnected).isTrue()
        assertThat(serviceConnection.documentBinder).isEqualTo(fakeRemote)
    }
}
