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

package androidx.glance.wear.core

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_LARGE
import androidx.glance.wear.parcel.ActiveWearWidgetHandleParcel
import androidx.glance.wear.parcel.IExecutionCallback
import androidx.glance.wear.parcel.IWearWidgetCallback
import androidx.glance.wear.parcel.IWearWidgetProvider
import androidx.glance.wear.parcel.WearWidgetEventBatchParcel
import androidx.glance.wear.parcel.WearWidgetRequestParcel
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WearWidgetProviderClientTest {
    private lateinit var context: Context
    private val service = FakeWearWidgetProvider()
    private val shadowApp by lazy { shadowOf(context as Application) }

    @Before
    fun setUp() {
        context = getApplicationContext()
        shadowApp.setComponentNameAndServiceForBindService(COMPONENT_NAME, service.asBinder())
    }

    @Test
    fun sendAddEvent() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        launch { client.sendAddEvent(instanceId, containerType) }
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()

        val handle = ActiveWearWidgetHandle.fromParcel(service.addedHandleParcel!!, COMPONENT_NAME)
        assertThat(handle.provider).isEqualTo(COMPONENT_NAME)
        assertThat(handle.instanceId).isEqualTo(instanceId)
        assertThat(handle.containerType).isEqualTo(containerType)
        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    @Test
    fun sendRemoveEvent() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        launch { client.sendRemoveEvent(instanceId, containerType) }
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()

        val handle =
            ActiveWearWidgetHandle.fromParcel(service.removedHandleParcel!!, COMPONENT_NAME)
        assertThat(handle.provider).isEqualTo(COMPONENT_NAME)
        assertThat(handle.instanceId).isEqualTo(instanceId)
        assertThat(handle.containerType).isEqualTo(containerType)
        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    @Test
    fun sendAddEvent_cancelled() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        val job = launch { client.sendAddEvent(instanceId, containerType) }
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        job.cancel()
        advanceUntilIdle()

        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    @Test
    fun sendRemoveEvent_cancelled() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        val job = launch { client.sendRemoveEvent(instanceId, containerType) }
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        job.cancel()
        advanceUntilIdle()

        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    @Test
    fun sendAddEventAsync() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        val future = client.sendAddEventAsync(instanceId, containerType, Runnable::run)
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()
        future.get()

        val handle = ActiveWearWidgetHandle.fromParcel(service.addedHandleParcel!!, COMPONENT_NAME)
        assertThat(handle.provider).isEqualTo(COMPONENT_NAME)
        assertThat(handle.instanceId).isEqualTo(instanceId)
        assertThat(handle.containerType).isEqualTo(containerType)
        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    @Test
    fun sendRemoveEventAsync() = runTest {
        val client = WearWidgetProviderClient(context, COMPONENT_NAME)
        val instanceId = WidgetInstanceId(ID_NAMESPACE, 123)
        val containerType = CONTAINER_TYPE_LARGE

        assertThat(shadowApp.boundServiceConnections).isEmpty()

        val future = client.sendRemoveEventAsync(instanceId, containerType, Runnable::run)
        // Actual binding runs on the main thread.
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()
        future.get()

        val handle =
            ActiveWearWidgetHandle.fromParcel(service.removedHandleParcel!!, COMPONENT_NAME)
        assertThat(handle.provider).isEqualTo(COMPONENT_NAME)
        assertThat(handle.instanceId).isEqualTo(instanceId)
        assertThat(handle.containerType).isEqualTo(containerType)
        assertThat(shadowApp.unboundServiceConnections).hasSize(1)
        assertThat(shadowApp.boundServiceConnections).isEmpty()
    }

    private class FakeWearWidgetProvider : IWearWidgetProvider.Stub() {
        var addedHandleParcel: ActiveWearWidgetHandleParcel? = null
        var removedHandleParcel: ActiveWearWidgetHandleParcel? = null

        override fun getApiVersion(): Int = 1

        override fun onWidgetRequest(
            requestParcel: WearWidgetRequestParcel?,
            callback: IWearWidgetCallback?,
        ) {}

        override fun onActivated(
            handleParcel: ActiveWearWidgetHandleParcel?,
            callback: IExecutionCallback?,
        ) {}

        override fun onDeactivated(
            handleParcel: ActiveWearWidgetHandleParcel?,
            callback: IExecutionCallback?,
        ) {}

        override fun onAdded(
            handleParcel: ActiveWearWidgetHandleParcel?,
            callback: IExecutionCallback?,
        ) {
            addedHandleParcel = handleParcel
            callback?.onSuccess()
        }

        override fun onRemoved(
            handleParcel: ActiveWearWidgetHandleParcel?,
            callback: IExecutionCallback?,
        ) {
            removedHandleParcel = handleParcel
            callback?.onSuccess()
        }

        override fun onEvents(
            eventBatchParcel: WearWidgetEventBatchParcel?,
            callback: IExecutionCallback?,
        ) {
            callback?.onSuccess()
        }

        override fun getInterfaceVersion(): Int = VERSION
    }

    private companion object {
        val COMPONENT_NAME = ComponentName("my.package", "androidx.glance.wear.MyTestService")
        private const val ID_NAMESPACE = "ns"
    }
}
