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
package androidx.xr.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.IInterface
import android.os.RemoteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.platform.IEngagementModeCallback
import androidx.xr.projected.platform.IEngagementModeService
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Test class for [EngagementModeClient]. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
class EngagementModeClientTest {
    private val mMockContext = mock<Context>()
    private val mMockConsumer = mock<Consumer<Int>>()
    private val mMockBinder = mock<IBinder>()
    private val mMockService = mock<IEngagementModeService>()
    private val mMockPackageManager = mock<PackageManager>()
    private val mMockResolveInfo = mock<ResolveInfo>()
    private val mMockHandler = mock<Handler>()
    private val mServiceConnectionCaptor = argumentCaptor<ServiceConnection>()
    private val mIntentCaptor = argumentCaptor<Intent>()
    private val mCallbackCaptor = argumentCaptor<IEngagementModeCallback>()
    private val mReconnectRunnableCaptor = argumentCaptor<Runnable>()
    private var mClient: EngagementModeClient? = null
    private val mDirectExecutor = Executor { obj: Runnable? -> obj!!.run() }

    @Before
    @Throws(PackageManager.NameNotFoundException::class)
    fun setUp() {
        whenever(mMockContext.applicationContext).thenReturn(mMockContext)
        whenever(mMockContext.getPackageManager()).thenReturn(mMockPackageManager)
        whenever<IInterface?>(
                mMockBinder.queryLocalInterface(IEngagementModeService::class.java.getName())
            )
            .thenReturn(mMockService)
        mMockResolveInfo.serviceInfo = ServiceInfo()
        mMockResolveInfo.serviceInfo.packageName = FAKE_PACKAGE_NAME
        whenever<ResolveInfo?>(mMockPackageManager.resolveService(any<Intent>(), any<Int>()))
            .thenReturn(mMockResolveInfo)
        // Default to a valid system app. Individual tests can override this.
        mockSystemAppVerification(true)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun testClient_startsUnbound_hasNullValue() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        assertThat(mClient!!.getEngagementModeFlags()).isNull()
        verify(mMockConsumer, never()).accept(any<Int>())
    }

    @Test
    fun testClient_bindsToServiceIfSystemApp() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        verify(mMockContext)
            .bindService(
                mIntentCaptor!!.capture(),
                mServiceConnectionCaptor!!.capture(),
                eq(Context.BIND_AUTO_CREATE),
            )
        val intent = mIntentCaptor.firstValue
        assertThat(intent.getAction()).isEqualTo(SERVICE_ACTION)
        assertThat(intent.getPackage()).isEqualTo(FAKE_PACKAGE_NAME)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun testClient_doesNotBindIfProviderNotSystemApp() {
        // Service is NOT a system app
        mockSystemAppVerification(false)
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        // bindService is not called
        verify(mMockContext, never()).bindService(any(), any(), any<Int>())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    @Throws(RemoteException::class)
    fun testClient_receivesUpdatesFromCallback() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        verify(mMockContext).bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>())
        val connection = mServiceConnectionCaptor.firstValue
        // Connect to service
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Verify callback is registered
        verify<IEngagementModeService>(mMockService).registerCallback(mCallbackCaptor!!.capture())
        val callback = mCallbackCaptor.firstValue
        // Simulate initial state from service
        val initialFlags = EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON
        callback.onEngagementModeChanged(initialFlags)
        // Client gets the initial state and the update callback is called
        // Expect AUDIO_ON to always be set to true
        var expectedFlags = initialFlags or EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON
        assertThat(mClient!!.getEngagementModeFlags()).isEqualTo(expectedFlags)
        verify(mMockConsumer).accept(expectedFlags)

        // Simulate engagement mode update from service
        val newFlags = 0
        callback.onEngagementModeChanged(newFlags)

        // Client is updated and the update callback is called again
        // Expect AUDIO_ON to always be set to true
        expectedFlags = newFlags or EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON
        assertThat(mClient!!.getEngagementModeFlags()).isEqualTo(expectedFlags)
        verify(mMockConsumer).accept(expectedFlags)
    }

    @Test
    fun testClient_addCallback_callsWithCurrentState() {
        // Simulate that bindService succeeds and returns true
        whenever(mMockContext.bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>()))
            .thenReturn(true)
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        val anotherMockConsumer = mock<Consumer<Int>>()
        // Add the first listener.
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        val connection = mServiceConnectionCaptor.firstValue
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Verify callback is registered
        verify<IEngagementModeService>(mMockService).registerCallback(mCallbackCaptor!!.capture())
        val callback = mCallbackCaptor.firstValue
        // Simulate initial state from service
        val initialFlags = EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON
        callback.onEngagementModeChanged(initialFlags)
        // Client gets the initial state and the update callback is called
        // Expect AUDIO_ON to always be set to true
        var expectedFlags = initialFlags or EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON
        assertThat(mClient!!.getEngagementModeFlags()).isEqualTo(expectedFlags)
        verify(mMockConsumer).accept(expectedFlags)

        // Add the second listener.
        mClient!!.addUpdateCallback(mDirectExecutor, anotherMockConsumer)
        // Client gets the initial state and the update callback is called
        verify(anotherMockConsumer).accept(expectedFlags)

        // Simulate engagement mode update from service
        val newFlags = 0
        callback.onEngagementModeChanged(newFlags)

        // Client is updated and the both callbacks are called again
        // Expect AUDIO_ON to always be set to true
        expectedFlags = newFlags or EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON
        assertThat(mClient!!.getEngagementModeFlags()).isEqualTo(expectedFlags)
        verify(mMockConsumer).accept(expectedFlags)
        verify(anotherMockConsumer).accept(expectedFlags)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    @Throws(RemoteException::class)
    fun testClient_resetsFlagsOnDisconnect() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        verify(mMockContext).bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>())
        val connection = mServiceConnectionCaptor.firstValue
        // Connect to service
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Verify callback is registered
        verify<IEngagementModeService>(mMockService).registerCallback(mCallbackCaptor!!.capture())
        val callback = mCallbackCaptor.firstValue
        // Simulate initial state from service
        val initialFlags = EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON
        callback.onEngagementModeChanged(initialFlags)
        // Client gets the initial state and the update callback is called
        // Expect AUDIO_ON to always be set to true
        val expectedFlags = initialFlags or EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON
        assertThat(mClient!!.getEngagementModeFlags()).isEqualTo(expectedFlags)
        verify(mMockConsumer).accept(expectedFlags)

        // Disconnect from service
        connection.onServiceDisconnected(ComponentName(FAKE_PACKAGE_NAME, "Test"))

        // Verify that the flags are reset to the default value, but the callback isn't called.
        assertThat(mClient!!.getEngagementModeFlags()).isNull()
    }

    @Test
    fun testClient_schedulesReconnectOnDisconnect_exponentialBackoff() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        verify(mMockContext).bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>())
        val connection = mServiceConnectionCaptor.firstValue
        // Connect to service
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Disconnect from service
        connection.onServiceDisconnected(ComponentName(FAKE_PACKAGE_NAME, "Test"))
        // Verify that a reconnect is scheduled with the initial delay.
        verify<Handler>(mMockHandler)
            .postDelayed(
                mReconnectRunnableCaptor!!.capture(),
                eq(EngagementModeClient.INITIAL_RECONNECT_DELAY_MS),
            )
        // Verify that running the captured runnable triggers a reconnect.
        val reconnectRunnable = mReconnectRunnableCaptor.firstValue
        reconnectRunnable.run()
        // Verify bindService is called again (total of 2 times)
        verify(mMockContext, times(2)).bindService(any(), any(), any<Int>())
        // Disconnect again
        connection.onServiceDisconnected(ComponentName(FAKE_PACKAGE_NAME, "Test"))
        // Verify that a reconnect is scheduled with the next delay in the backoff.
        verify(mMockHandler)
            .postDelayed(any<Runnable>(), eq(EngagementModeClient.INITIAL_RECONNECT_DELAY_MS * 10))
        // Connect to service again
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Disconnect again
        connection.onServiceDisconnected(ComponentName(FAKE_PACKAGE_NAME, "Test"))
        // Verify that the reconnect delay is reset to the initial delay.
        verify<Handler>(mMockHandler, times(2))
            .postDelayed(any<Runnable>(), eq(EngagementModeClient.INITIAL_RECONNECT_DELAY_MS))
    }

    @Test
    fun testClient_addCallback_bindsOnlyOnce() {
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        // Add a listener. This should trigger binding.
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)

        // Add another listener. This should not trigger binding again.
        mClient!!.addUpdateCallback(mDirectExecutor, Consumer { i: Int -> })

        // Verify bindService is only called once
        verify<Context>(mMockContext).bindService(any(), any(), any<Int>())
    }

    @Test
    fun testClient_removeLastCallback_unbindsService() {
        // Simulate that bindService succeeds and returns true
        whenever(mMockContext.bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>()))
            .thenReturn(true)
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        // Add callback, which connects and simulates connection succeeding
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        val connection = mServiceConnectionCaptor.firstValue
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)

        // Remove callback
        mClient!!.removeUpdateCallback(mMockConsumer)

        // Verify unbindService is called and handler callbacks are cancelled
        verify(mMockContext).unbindService(eq(connection))
        verify(mMockHandler).removeCallbacksAndMessages(null)
    }

    @Test
    fun testClient_removeCallback_doesNotUnbindWithActiveListeners() {
        // Simulate that bindService succeeds and returns true
        whenever(mMockContext.bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>()))
            .thenReturn(true)
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        val anotherConsumer = Consumer { i: Int -> }
        // Add two listeners
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        mClient!!.addUpdateCallback(mDirectExecutor, anotherConsumer)
        val connection = mServiceConnectionCaptor.firstValue
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)

        // Remove first listener
        mClient!!.removeUpdateCallback(mMockConsumer)

        // Verify unbindService is NOT called, and handler is NOT cancelled
        verify(mMockContext, never()).unbindService(any())
        verify(mMockHandler, never()).removeCallbacksAndMessages(null)
        // Remove second (last) listener
        mClient!!.removeUpdateCallback(anotherConsumer)
        // Verify unbindService IS called, and handler IS cancelled
        verify(mMockContext).unbindService(eq(connection))
        verify(mMockHandler).removeCallbacksAndMessages(null)
    }

    @Test
    fun testClient_doesNotReconnectOnServiceDisconnect_ifNoListeners() {
        whenever(mMockContext.bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>()))
            .thenReturn(true)
        mClient = EngagementModeClient(mMockContext, mMockHandler)
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)
        val connection = mServiceConnectionCaptor.firstValue
        // Connect to service
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)
        // Remove listener before service disconnects
        mClient!!.removeUpdateCallback(mMockConsumer)

        // Disconnect from service
        connection.onServiceDisconnected(ComponentName(FAKE_PACKAGE_NAME, "Test"))

        // Verify that a reconnect is NOT scheduled.
        verify(mMockHandler, never()).postDelayed(any<Runnable>(), any<Long>())
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun mockSystemAppVerification(isSystemApp: Boolean) {
        val packageInfo = PackageInfo()
        packageInfo.applicationInfo = ApplicationInfo()
        if (isSystemApp) {
            packageInfo.applicationInfo!!.flags =
                packageInfo.applicationInfo!!.flags or ApplicationInfo.FLAG_SYSTEM
        }
        whenever<PackageInfo?>(mMockPackageManager.getPackageInfo(FAKE_PACKAGE_NAME, 0))
            .thenReturn(packageInfo)
    }

    @Test
    fun testClient_unbindsOnConnect_ifListenersRemovedDuringBind() {
        // Simulate that bindService succeeds and returns true
        whenever(mMockContext.bindService(any(), mServiceConnectionCaptor!!.capture(), any<Int>()))
            .thenReturn(true)
        mClient = EngagementModeClient(mMockContext, mMockHandler)

        // Add the listener, which triggers the bind
        mClient!!.addUpdateCallback(mDirectExecutor, mMockConsumer)

        // Capture the connection
        val connection = mServiceConnectionCaptor.firstValue

        // Remove the listener before the service connects
        mClient!!.removeUpdateCallback(mMockConsumer)

        // Simulate the service connecting (this is the race)
        connection.onServiceConnected(ComponentName(FAKE_PACKAGE_NAME, "Test"), mMockBinder)

        // Verify that the client should immediately unbind to prevent a leak.
        verify(mMockContext).unbindService(eq(connection))
    }

    companion object {
        private const val FAKE_PACKAGE_NAME = "test.package.name"
        private const val SERVICE_ACTION = "androidx.xr.projected.ACTION_ENGAGEMENT_BIND"
    }
}
