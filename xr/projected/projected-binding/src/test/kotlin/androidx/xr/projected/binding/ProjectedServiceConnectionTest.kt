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

package androidx.xr.projected.binding

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.os.DeadObjectException
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.xr.projected.platform.IProjectedService
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ProjectedServiceConnectionTest {

    private lateinit var context: Application
    private lateinit var testScope: TestScope
    private lateinit var projectedServiceConnection: ProjectedServiceConnection

    @Before
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(dispatcher)
        projectedServiceConnection = ProjectedServiceConnection(context, ACTION_BIND)
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(COMPONENT_NAME)
            addOrUpdateService(SERVICE_INFO)
            addIntentFilterForService(COMPONENT_NAME, IntentFilter(ACTION_BIND.intentActionString))
            installPackage(PACKAGE_INFO)
        }
        shadowOf(context)
            .setComponentNameAndServiceForBindService(COMPONENT_NAME, mock(IBinder::class.java))
    }

    @Test
    fun connect_bindsAndConnects_returnsInstance() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)

        val service = projectedServiceConnection.connect()

        assertThat(service).isInstanceOf(IProjectedService::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
    }

    @Test
    fun connect_doesNotBind_throwsIllegalStateException() = testScope.runTest {
        shadowOf(context).declareComponentUnbindable(COMPONENT_NAME)

        assertFailsWith<IllegalStateException> { projectedServiceConnection.connect() }

        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
    }

    @Test
    fun connect_calledSecondTimeWithoutDisconnect_throwsIllegalStateException() =
        testScope.runTest {
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            check(projectedServiceConnection.isServiceConnected.value)

            assertFailsWith<IllegalStateException> { projectedServiceConnection.connect() }
        }

    @Test
    fun connect_calledSecondTimeWithDisconnect_returnsInstance() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        projectedServiceConnection.connect()
        projectedServiceConnection.disconnect()
        check(!projectedServiceConnection.isServiceConnected.value)

        val service = projectedServiceConnection.connect()

        assertThat(service).isInstanceOf(IProjectedService::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
    }

    @Test
    fun connect_linksToDeathBeforeCompleting_disconnectUnlinksRegisteredRecipient() =
        testScope.runTest {
            var linkedRecipient: IBinder.DeathRecipient? = null
            var unlinkedRecipient: IBinder.DeathRecipient? = null
            val binder =
                mock<IBinder> {
                    on { linkToDeath(any(), eq(0)) } doAnswer
                        { invocation ->
                            linkedRecipient = invocation.getArgument(0)
                            null
                        }
                    on { unlinkToDeath(any(), eq(0)) } doAnswer
                        { invocation ->
                            val recipient = invocation.getArgument<IBinder.DeathRecipient>(0)
                            if (recipient != linkedRecipient) {
                                throw NoSuchElementException("Death link does not exist")
                            }
                            unlinkedRecipient = recipient
                            true
                        }
                }
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, binder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            checkNotNull(linkedRecipient)
            check(projectedServiceConnection.isServiceConnected.value)

            projectedServiceConnection.disconnect()

            assertThat(unlinkedRecipient).isSameInstanceAs(linkedRecipient)
            assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
        }

    @Test
    fun connect_linkToDeathThrowsRemoteException_throwsIllegalStateExceptionAndCleansUp() =
        testScope.runTest {
            val deadBinder =
                mock<IBinder> {
                    on { linkToDeath(any(), eq(0)) } doThrow DeadObjectException("Binder died")
                }
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, deadBinder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)

            assertFailsWith<IllegalStateException> { projectedServiceConnection.connect() }

            assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
            assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
        }

    @Test
    fun connect_afterLinkToDeathFailure_returnsInstance() = testScope.runTest {
        val deadBinder =
            mock<IBinder> {
                on { linkToDeath(any(), eq(0)) } doThrow DeadObjectException("Binder died")
            }
        shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, deadBinder)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        runCatching { projectedServiceConnection.connect() }
        check(!projectedServiceConnection.isServiceConnected.value)
        shadowOf(context)
            .setComponentNameAndServiceForBindService(COMPONENT_NAME, mock(IBinder::class.java))

        val service = projectedServiceConnection.connect()

        assertThat(service).isInstanceOf(IProjectedService::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
    }

    @Test
    fun connect_timesOut_unbindsService() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)

        assertFailsWith<TimeoutCancellationException> { projectedServiceConnection.connect() }

        assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
    }

    @Test
    fun onServiceConnected_afterTimeout_isIgnored() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)
        runCatching { projectedServiceConnection.connect() }
        val staleConnection = shadowOf(context).unboundServiceConnections.single()

        staleConnection.onServiceConnected(COMPONENT_NAME, mock(IBinder::class.java))

        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
    }

    @Test
    fun connect_afterTimeout_returnsInstance() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)
        runCatching { projectedServiceConnection.connect() }
        check(shadowOf(context).unboundServiceConnections.size == 1)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)

        val service = projectedServiceConnection.connect()

        assertThat(service).isInstanceOf(IProjectedService::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
    }

    @Test
    fun connect_serviceDies_disconnects() = runTest {
        lateinit var deathRecipient: IBinder.DeathRecipient
        val binder =
            mock<IBinder> {
                on { linkToDeath(any(), any()) } doAnswer
                    { invocation ->
                        deathRecipient = invocation.getArgument(0)
                        null
                    }
            }
        shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, binder)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        projectedServiceConnection.connect()

        deathRecipient.binderDied()
        projectedServiceConnection.disconnect()

        assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
        verify(binder, times(1)).unlinkToDeath(eq(deathRecipient), eq(0))
    }

    @Test
    fun onServiceDisconnected_updatesIsServiceConnectedToFalse() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        projectedServiceConnection.connect()
        check(projectedServiceConnection.isServiceConnected.value)
        val activeConnection = shadowOf(context).boundServiceConnections.single()

        activeConnection.onServiceDisconnected(COMPONENT_NAME)

        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
    }

    @Test
    fun disconnect_unlinkToDeathThrowsNoSuchElementException_doesNotThrow() = testScope.runTest {
        val binder =
            mock<IBinder> {
                on { unlinkToDeath(any(), eq(0)) } doThrow
                    NoSuchElementException("Death link does not exist")
            }
        shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, binder)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
        projectedServiceConnection.connect()

        projectedServiceConnection.disconnect()

        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
        assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
    }

    @Test
    fun disconnect_whileConnectPending_cancelsPendingConnectAndUnbinds() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)
        var connectException: Throwable? = null
        val connectJob = backgroundScope.launch {
            try {
                projectedServiceConnection.connect()
            } catch (e: Throwable) {
                connectException = e
            }
        }
        check(connectJob.isActive)

        projectedServiceConnection.disconnect()

        assertThat(connectJob.isCompleted).isTrue()
        assertThat(connectException).isInstanceOf(CancellationException::class.java)
        assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
    }

    @Test
    fun connect_afterDisconnectWhileConnectPending_returnsInstance() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)
        val connectJob = backgroundScope.launch { projectedServiceConnection.connect() }
        projectedServiceConnection.disconnect()
        check(connectJob.isCancelled)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)

        val service = projectedServiceConnection.connect()

        assertThat(service).isInstanceOf(IProjectedService::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
    }

    @Test
    fun connect_staleDeathRecipientFromPreviousConnection_doesNotDisconnectNewConnection() =
        testScope.runTest {
            lateinit var firstDeathRecipient: IBinder.DeathRecipient
            val firstBinder =
                mock<IBinder> {
                    on { linkToDeath(any(), eq(0)) } doAnswer
                        { invocation ->
                            firstDeathRecipient = invocation.getArgument(0)
                            null
                        }
                }
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, firstBinder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            projectedServiceConnection.disconnect()
            val secondBinder = mock<IBinder>()
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, secondBinder)
            projectedServiceConnection.connect()
            check(projectedServiceConnection.isServiceConnected.value)

            firstDeathRecipient.binderDied()

            assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
            assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
        }

    @Test
    fun connect_nullServiceBinder_throwsIllegalStateExceptionAndCleansUp() = testScope.runTest {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(false)
        var connectException: Throwable? = null
        val connectJob = backgroundScope.launch {
            try {
                projectedServiceConnection.connect()
            } catch (e: Throwable) {
                connectException = e
            }
        }
        val activeConnection = shadowOf(context).boundServiceConnections.single()

        activeConnection.onServiceConnected(COMPONENT_NAME, null)

        assertThat(connectJob.isCompleted).isTrue()
        assertThat(connectException).isInstanceOf(IllegalStateException::class.java)
        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
        assertThat(shadowOf(context).unboundServiceConnections).hasSize(1)
    }

    @Test
    fun onServiceConnected_afterOnServiceDisconnected_unlinksPreviousBinderAndReconnects() =
        testScope.runTest {
            lateinit var firstDeathRecipient: IBinder.DeathRecipient
            val firstBinder =
                mock<IBinder> {
                    on { linkToDeath(any(), eq(0)) } doAnswer
                        { invocation ->
                            firstDeathRecipient = invocation.getArgument(0)
                            null
                        }
                }
            val secondBinder = mock<IBinder>()
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, firstBinder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            val activeConnection = shadowOf(context).boundServiceConnections.single()
            activeConnection.onServiceDisconnected(COMPONENT_NAME)
            check(!projectedServiceConnection.isServiceConnected.value)

            activeConnection.onServiceConnected(COMPONENT_NAME, secondBinder)

            assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
            verify(firstBinder, times(1)).unlinkToDeath(eq(firstDeathRecipient), eq(0))
            verify(secondBinder, times(1)).linkToDeath(any(), eq(0))
        }

    @Test
    fun binderDied_fromPreviousBinderAfterServiceRestart_doesNotDisconnectRestartedService() =
        testScope.runTest {
            lateinit var firstDeathRecipient: IBinder.DeathRecipient
            val firstBinder =
                mock<IBinder> {
                    on { linkToDeath(any(), eq(0)) } doAnswer
                        { invocation ->
                            firstDeathRecipient = invocation.getArgument(0)
                            null
                        }
                }
            val secondBinder = mock<IBinder>()
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, firstBinder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            val activeConnection = shadowOf(context).boundServiceConnections.single()
            activeConnection.onServiceDisconnected(COMPONENT_NAME)
            activeConnection.onServiceConnected(COMPONENT_NAME, secondBinder)
            check(projectedServiceConnection.isServiceConnected.value)

            firstDeathRecipient.binderDied()

            assertThat(projectedServiceConnection.isServiceConnected.value).isTrue()
            assertThat(shadowOf(context).unboundServiceConnections).isEmpty()
        }

    @Test
    fun disconnect_afterServiceRestartOnSameConnection_unlinksRestartedBinder() =
        testScope.runTest {
            val firstBinder = mock<IBinder>()
            val secondBinder = mock<IBinder>()
            shadowOf(context).setComponentNameAndServiceForBindService(COMPONENT_NAME, firstBinder)
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            projectedServiceConnection.connect()
            val activeConnection = shadowOf(context).boundServiceConnections.single()
            activeConnection.onServiceDisconnected(COMPONENT_NAME)
            activeConnection.onServiceConnected(COMPONENT_NAME, secondBinder)
            check(projectedServiceConnection.isServiceConnected.value)

            projectedServiceConnection.disconnect()

            verify(secondBinder, times(1)).unlinkToDeath(any(), eq(0))
            assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
        }

    @Test
    fun disconnect_unbinds() = testScope.runTest {
        backgroundScope.launch { projectedServiceConnection.connect() }

        projectedServiceConnection.disconnect()

        assertThat(shadowOf(context).unboundServiceConnections).isNotEmpty()
    }

    @Test
    fun disconnect_notConnected_doesNotThrow() {
        check(!projectedServiceConnection.isServiceConnected.value)

        projectedServiceConnection.disconnect()

        assertThat(projectedServiceConnection.isServiceConnected.value).isFalse()
    }

    companion object {
        private const val SYSTEM_PACKAGE_NAME = "com.system.service"
        private const val SYSTEM_CLASS_NAME = "com.system.service.ProjectedService"
        private val ACTION_BIND = ProjectedServiceConnection.ProjectedIntentAction.ACTION_BIND
        private val COMPONENT_NAME = ComponentName(SYSTEM_PACKAGE_NAME, SYSTEM_CLASS_NAME)
        private val SERVICE_INFO =
            ServiceInfo().apply {
                packageName = SYSTEM_PACKAGE_NAME
                name = SYSTEM_CLASS_NAME
            }
        private val PACKAGE_INFO =
            PackageInfo().apply {
                packageName = SYSTEM_PACKAGE_NAME
                services = arrayOf(SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
    }
}
