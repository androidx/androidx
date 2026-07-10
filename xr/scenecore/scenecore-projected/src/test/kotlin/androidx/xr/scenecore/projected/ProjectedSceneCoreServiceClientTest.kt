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

package androidx.xr.scenecore.projected

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

private const val DEFAULT_TEST_PACKAGE_NAME = "com.example.xr.service"
private const val DEFAULT_TEST_CLASS_NAME = "ProjectedService"

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedSceneCoreServiceClientTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var client: ProjectedSceneCoreServiceClient

    @Before
    fun setUp() {
        context = mock()
        packageManager = mock()
        whenever(context.packageManager).thenReturn(packageManager)
        client = ProjectedSceneCoreServiceClient()
    }

    private fun setupMockServiceResolution(
        packageName: String = DEFAULT_TEST_PACKAGE_NAME,
        className: String = DEFAULT_TEST_CLASS_NAME,
        isSystemApp: Boolean = true,
    ) {
        val appInfo =
            ApplicationInfo().apply { flags = if (isSystemApp) ApplicationInfo.FLAG_SYSTEM else 0 }
        val resolveInfo =
            ResolveInfo().apply {
                serviceInfo =
                    ServiceInfo().apply {
                        this.packageName = packageName
                        this.name = className
                        this.applicationInfo = appInfo
                    }
            }

        // Mock queryIntentServices
        whenever(packageManager.queryIntentServices(any(), anyInt()))
            .thenReturn(listOf(resolveInfo))

        // Mock getApplicationInfo which is called inside the filter loop
        whenever(packageManager.getApplicationInfo(eq(packageName), anyInt())).thenReturn(appInfo)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_bindsToCorrectIntent() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        // Launch in a coroutine because bindService suspends waiting for connection
        val job =
            launch(Dispatchers.Default) {
                try {
                    client.bindService(context)
                } catch (e: Exception) {
                    // Ignore timeout/cancellation for this specific assertion
                }
            }

        // Capture the intent passed to bindService
        val intentCaptor = argumentCaptor<Intent>()
        verify(context, timeout(1000)).bindService(intentCaptor.capture(), any(), anyInt())

        val intent = intentCaptor.firstValue
        assertThat(intent.action).isEqualTo(ProjectedSceneCoreServiceClient.ACTION_SCENE_CORE_BIND)
        assertThat(intent.component?.packageName).isEqualTo(DEFAULT_TEST_PACKAGE_NAME)
        assertThat(intent.component?.className).isEqualTo(DEFAULT_TEST_CLASS_NAME)

        job.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_returnsService_whenConnectionSucceeds() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        val deferred = async(Dispatchers.Default) { client.bindService(context) }

        // Verify bind was called and capture the connection
        val connectionCaptor = argumentCaptor<ServiceConnection>()
        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())

        // Simulate the system calling onServiceConnected
        connectionCaptor.firstValue.onServiceConnected(mock(), mock())

        val service = deferred.await()
        assertThat(service).isNotNull()
        assertThat(client.service).isEqualTo(service)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_throwsIllegalStateException_whenBindingDied() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        // Launch the bind
        launch(Dispatchers.Default) {
            val exception =
                assertFailsWith(IllegalStateException::class) { client.bindService(context) }
            assertThat(exception).hasMessageThat().contains("Binding died")
        }

        // Trigger onBindingDied
        val connectionCaptor = argumentCaptor<ServiceConnection>()
        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())
        connectionCaptor.firstValue.onBindingDied(mock())

        // Verify cleanup happened
        verify(context, timeout(1000)).unbindService(connectionCaptor.firstValue)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_throwsIllegalStateException_whenNullBinding() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        launch(Dispatchers.Default) {
            val exception =
                assertFailsWith(IllegalStateException::class) { client.bindService(context) }
            assertThat(exception).hasMessageThat().contains("Service returned null binding")
        }

        // Trigger onNullBinding
        val connectionCaptor = argumentCaptor<ServiceConnection>()
        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())
        connectionCaptor.firstValue.onNullBinding(mock())

        verify(context, timeout(1000)).unbindService(connectionCaptor.firstValue)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onServiceDisconnected_clearsService_butDoesNotUnbind() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        // Connect successfully
        val deferred = async(Dispatchers.Default) { client.bindService(context) }

        val connectionCaptor = argumentCaptor<ServiceConnection>()
        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())

        connectionCaptor.firstValue.onServiceConnected(mock(), mock())
        deferred.await()
        assertThat(client.service).isNotNull()

        // Trigger disconnect
        connectionCaptor.firstValue.onServiceDisconnected(mock())

        // Assert service is cleared
        assertThat(client.service).isNull()

        // Assert unbindService was NOT called (system handles reconnection)
        verify(context, never()).unbindService(any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_unbinds_whenCoroutineCancelled() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)
        val job = launch(Dispatchers.Default) { client.bindService(context) }

        // Wait for bind to be called
        val connectionCaptor = argumentCaptor<ServiceConnection>()
        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())

        // Cancel the coroutine before connection is established
        job.cancel()
        job.join()

        // Verify cleanup occurred
        verify(context, timeout(1000)).unbindService(connectionCaptor.firstValue)
    }

    @Test
    fun bindService_throwsIllegalStateException_whenBindReturnsFalse() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(false)

        launch(Dispatchers.Default) {
            val exception =
                assertFailsWith(IllegalStateException::class) { client.bindService(context) }
            assertThat(exception).hasMessageThat().contains("bindService returned false")
        }
    }

    @Test
    fun bindService_throwsIllegalStateException_whenNoSystemAppFound() = runTest {
        // Mock resolution to return empty list
        whenever(packageManager.queryIntentServices(any(), anyInt())).thenReturn(emptyList())

        launch(Dispatchers.Default) {
            val exception =
                assertFailsWith(IllegalStateException::class) { client.bindService(context) }
            assertThat(exception).hasMessageThat().contains("System doesn't include a service")
        }
    }

    @Test
    fun bindService_throwsSecurityException_whenContextThrows() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt()))
            .thenThrow(SecurityException("Not allowed"))

        launch(Dispatchers.Default) {
            assertFailsWith(SecurityException::class) { client.bindService(context) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bindService_returnsExistingService_ifAlreadyConnected() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        val connectionCaptor = argumentCaptor<ServiceConnection>()
        val deferred1 = async(Dispatchers.Default) { client.bindService(context) }

        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())
        connectionCaptor.firstValue.onServiceConnected(mock(), mock())
        val service1 = deferred1.await()

        // Call bindService again
        val deferred2 = async(Dispatchers.Default) { client.bindService(context) }
        val service2 = deferred2.await()

        // Assert that bindService was NOT called a second time
        verify(context, times(1)).bindService(any(), any(), anyInt())
        assertThat(service2).isNotNull()
        assertThat(service2).isEqualTo(service1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun unbindService_unbindsFromContext_andClearsState() = runTest {
        setupMockServiceResolution()
        whenever(context.bindService(any(), any(), anyInt())).thenReturn(true)

        val connectionCaptor = argumentCaptor<ServiceConnection>()
        val deferred = async(Dispatchers.Default) { client.bindService(context) }

        verify(context, timeout(1000)).bindService(any(), connectionCaptor.capture(), anyInt())
        connectionCaptor.firstValue.onServiceConnected(mock(), mock())
        deferred.await()

        // Act
        client.unbindService()

        // Assert
        verify(context).unbindService(connectionCaptor.firstValue)
        assertThat(client.service).isNull()
    }

    @Test
    fun unbindService_doesNothing_ifNotBound() {
        client.unbindService()
        verify(context, never()).unbindService(any())
    }

    @Test
    fun bindService_throwsException_whenCalledFromMainThread() = runTest {
        val exception =
            assertFailsWith(IllegalStateException::class) { client.bindService(context) }
        assertThat(exception).hasMessageThat().contains("cannot be bound from the main thread")
    }
}
