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

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.xr.projected.platform.IProjectedDeviceStateListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedDeviceState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ProjectedDeviceLifecycleTest {
    private val mockProjectedService = mock<IProjectedService>()
    private val mockProjectedServiceStub =
        mock<IProjectedService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockProjectedService)
        }
    private val lifecycleOwner = mock<LifecycleOwner>()
    private lateinit var context: Application

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val lifecycleObserver = mock<LifecycleEventObserver>()
    val deviceStateListenerArgumentCaptor = argumentCaptor<IProjectedDeviceStateListener>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(COMPONENT_NAME)
            addOrUpdateService(SERVICE_INFO)
            addIntentFilterForService(COMPONENT_NAME, IntentFilter(ACTION_BIND))
            installPackage(PACKAGE_INFO)
        }
        shadowOf(context)
            .setComponentNameAndServiceForBindService(COMPONENT_NAME, mockProjectedServiceStub)
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
    }

    @After
    fun tearDown() {
        shadowOf(context.packageManager).apply {
            removePackage(COMPONENT_NAME.packageName)
            removeService(COMPONENT_NAME)
        }
        Dispatchers.resetMain()
    }

    @Test
    fun create_returnsProjectedDeviceLifecycleInstance_stateChangesToInitialized() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        testScheduler.advanceUntilIdle()

        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
    }

    @Test
    fun addObserver_registersProjectedDeviceStateListener_stateChangesToCreated() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()

        verify(mockProjectedService).registerProjectedDeviceStateListener(any())
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    fun removeObserver_unregistersProjectedDeviceStateListener_stateChangesToInitialized() =
        runBlocking {
            val projectedDeviceLifecycle =
                ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
            projectedDeviceLifecycle.addObserver(lifecycleObserver)
            testScheduler.advanceUntilIdle()
            verify(mockProjectedService).registerProjectedDeviceStateListener(any())

            projectedDeviceLifecycle.removeObserver(lifecycleObserver)
            testScheduler.advanceUntilIdle()
            verify(mockProjectedService).unregisterProjectedDeviceStateListener(any())
            assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
        }

    @Test
    fun removeObserver_multipleObservers_removesOneObserver_doesNotUnregisterProjectedDeviceStateListener() =
        runBlocking {
            val secondLifecycleObserver = mock<LifecycleEventObserver>()
            val projectedDeviceLifecycle =
                ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
            projectedDeviceLifecycle.addObserver(lifecycleObserver)
            projectedDeviceLifecycle.addObserver(secondLifecycleObserver)
            testScheduler.advanceUntilIdle()
            verify(mockProjectedService).registerProjectedDeviceStateListener(any())

            projectedDeviceLifecycle.removeObserver(secondLifecycleObserver)
            testScheduler.advanceUntilIdle()
            verify(mockProjectedService, never()).unregisterProjectedDeviceStateListener(any())
            assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        }

    @Test
    fun onProjectedDeviceStateChanged_onStartEventReceived_stateChangesToStarted() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()

        verify(mockProjectedService)
            .registerProjectedDeviceStateListener(deviceStateListenerArgumentCaptor.capture())

        deviceStateListenerArgumentCaptor.firstValue.onProjectedDeviceStateChanged(
            ProjectedDeviceState.ACTIVE,
            null,
        )
        testScheduler.advanceUntilIdle()
        verify(lifecycleObserver).onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_START))
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
    }

    @Test
    fun onProjectedDeviceStateChanged_onStopEventReceived_stateChangesToCreated() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()

        verify(mockProjectedService)
            .registerProjectedDeviceStateListener(deviceStateListenerArgumentCaptor.capture())
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        deviceStateListenerArgumentCaptor.firstValue.onProjectedDeviceStateChanged(
            ProjectedDeviceState.ACTIVE,
            null,
        )
        testScheduler.advanceUntilIdle()
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        clearInvocations(lifecycleObserver)

        deviceStateListenerArgumentCaptor.firstValue.onProjectedDeviceStateChanged(
            ProjectedDeviceState.INACTIVE,
            null,
        )
        testScheduler.advanceUntilIdle()
        verify(lifecycleObserver).onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_STOP))
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    fun onProjectedDeviceStateChanged_onDestroyedEventReceived_stateChangesToDestroyed() =
        runBlocking {
            val projectedDeviceLifecycle =
                ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
            projectedDeviceLifecycle.addObserver(lifecycleObserver)
            testScheduler.advanceUntilIdle()

            verify(mockProjectedService)
                .registerProjectedDeviceStateListener(deviceStateListenerArgumentCaptor.capture())
            check(projectedDeviceLifecycle.currentState == Lifecycle.State.CREATED)

            deviceStateListenerArgumentCaptor.firstValue.onProjectedDeviceStateChanged(
                ProjectedDeviceState.DESTROYED,
                /* data= */ null,
            )
            testScheduler.advanceUntilIdle()
            verify(lifecycleObserver)
                .onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_DESTROY))
            assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            verify(mockProjectedService).unregisterProjectedDeviceStateListener(any())
        }

    companion object {
        private const val ACTION_BIND = "androidx.xr.projected.ACTION_BIND"
        private const val SYSTEM_PACKAGE_NAME = "com.system.service"
        private const val SYSTEM_CLASS_NAME = "com.system.service.ProjectedService"
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
