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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_FULLSCREEN
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_LARGE
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_SMALL
import androidx.glance.wear.core.WearWidgetEvent
import androidx.glance.wear.core.WearWidgetEventBatch
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WearWidgetVisibleEvent
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearWidgetProviderImplTest {
    private val testWidget = TestGlanceWearWidget()
    private val fakeExecutionCallback =
        object : IExecutionCallback.Stub() {
            var isSuccess = false
            var isFailure = false

            override fun onSuccess() {
                isSuccess = true
            }

            override fun onError(errorCode: Int, message: String) {
                isFailure = true
            }

            override fun getInterfaceVersion(): Int = VERSION
        }

    private val testName = ComponentName("package.name", "class.name")
    private val context: Context = getApplicationContext()
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)
    private val contentChannel = Channel<WearWidgetRawContent>()

    @Test
    fun onWidgetRequest_callsProvideWidgetData() = runTest {
        val widgetParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("namespace", 17),
                containerType = CONTAINER_TYPE_LARGE,
                widthDp = 200f,
                heightDp = 200f,
                horizontalPaddingDp = 8f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val channelWidgetCallback = ChannelWidgetCallback(this, contentChannel)
        val provider = WearWidgetProviderImpl(context, testName, mainScope, testWidget)

        provider.onWidgetRequest(widgetParams.toParcel(), channelWidgetCallback)
        contentChannel.receive()

        assertThat(testWidget.lastRequestedInstanceId).isEqualTo(widgetParams.instanceId)
        assertThat(testWidget.lastRequestedContainerType).isEqualTo(CONTAINER_TYPE_LARGE)
    }

    @Test
    fun onWidgetRequest_remapsFullscreenToLarge() = runTest {
        val widgetParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("namespace", 17),
                containerType = CONTAINER_TYPE_FULLSCREEN,
                widthDp = 200f,
                heightDp = 200f,
                horizontalPaddingDp = 8f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )
        val channelWidgetCallback = ChannelWidgetCallback(this, contentChannel)
        val provider = WearWidgetProviderImpl(context, testName, mainScope, testWidget)

        provider.onWidgetRequest(widgetParams.toParcel(), channelWidgetCallback)
        contentChannel.receive()

        assertThat(testWidget.lastRequestedInstanceId).isEqualTo(widgetParams.instanceId)
        assertThat(testWidget.lastRequestedContainerType).isEqualTo(CONTAINER_TYPE_LARGE)
    }

    @Test
    fun onWidgetRequest_capturesRemoteComposable() = runBlocking {
        val widgetParams =
            WearWidgetParams(
                instanceId = WidgetInstanceId("namespace", 17),
                containerType = CONTAINER_TYPE_LARGE,
                widthDp = 200f,
                heightDp = 200f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        testWidget.content = { RemoteText("Testing ...") }
        val expectedRcDocumentHierarchy =
            """
            ROOT [-2:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
              BOX [-3:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
                MODIFIERS
                  ROUNDED_CLIP_RECT = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
                  BACKGROUND = [0.0, 0.0, 0.0, 0.0] color [0.0, 0.0, 0.0, 0.0] shape [0]
                BOX [-5:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
                  DATA_TEXT<42> = "Testing ..."
                  MODIFIERS
                    PADDING = [0.0, 0.0, 0.0, 0.0]
                  TEXT_LAYOUT [-7:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE (42:"null")
                    MODIFIERS
            """
                .trimIndent()
        val provider = WearWidgetProviderImpl(context, testName, mainScope, testWidget)
        val channelWidgetCallback = ChannelWidgetCallback(this, contentChannel)

        provider.onWidgetRequest(widgetParams.toParcel(), channelWidgetCallback)
        val receivedRawContent = contentChannel.receive()

        assertThat(testWidget.lastRequestedInstanceId).isEqualTo(widgetParams.instanceId)
        assertThat(
                RemoteDocument(receivedRawContent.rcDocument)
                    .document
                    .displayHierarchy()
                    .trimIndent()
            )
            .isEqualTo(expectedRcDocumentHierarchy)
    }

    @Test
    fun onAdded_callsWidgetAndCallback() = runTest {
        val handle =
            ActiveWearWidgetHandle(
                testName,
                WidgetInstanceId("namespace", 12),
                CONTAINER_TYPE_LARGE,
            )
        val provider = WearWidgetProviderImpl(context, testName, this, testWidget)

        provider.onAdded(handle.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.addedHandle).isEqualTo(handle)
        assertThat(fakeExecutionCallback.isSuccess).isTrue()
    }

    @Test
    fun onAdded_throws_callsCallbackOnError() = runTest {
        val handle =
            ActiveWearWidgetHandle(
                testName,
                WidgetInstanceId("namespace", 12),
                CONTAINER_TYPE_LARGE,
            )
        val exceptionHandlerScope =
            CoroutineScope(
                coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }
            )
        val provider = WearWidgetProviderImpl(context, testName, exceptionHandlerScope, testWidget)
        testWidget.enableFailureMode = true

        provider.onAdded(handle.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.addedHandle).isEqualTo(handle)
        assertThat(fakeExecutionCallback.isFailure).isTrue()
    }

    @Test
    fun onRemoved_callsWidgetAndCallback() = runTest {
        val handle =
            ActiveWearWidgetHandle(
                testName,
                WidgetInstanceId("namespace", 12),
                CONTAINER_TYPE_SMALL,
            )
        val provider = WearWidgetProviderImpl(context, testName, this, testWidget)

        provider.onRemoved(handle.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.removedHandle).isEqualTo(handle)
        assertThat(fakeExecutionCallback.isSuccess).isTrue()
    }

    @Test
    fun onRemoved_throws_callsCallbackOnError() = runTest {
        val handle =
            ActiveWearWidgetHandle(
                testName,
                WidgetInstanceId("namespace", 12),
                CONTAINER_TYPE_LARGE,
            )
        val exceptionHandlerScope =
            CoroutineScope(
                coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }
            )
        val provider = WearWidgetProviderImpl(context, testName, exceptionHandlerScope, testWidget)
        testWidget.enableFailureMode = true

        provider.onRemoved(handle.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.removedHandle).isEqualTo(handle)
        assertThat(fakeExecutionCallback.isFailure).isTrue()
    }

    @Test
    fun onEvents_callsWidgetAndCallback() = runTest {
        val exceptionHandlerScope =
            CoroutineScope(
                coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }
            )
        val provider = WearWidgetProviderImpl(context, testName, exceptionHandlerScope, testWidget)

        val events =
            listOf(
                WearWidgetVisibleEvent(
                    instanceId = WidgetInstanceId(namespace = "ns", id = 1),
                    startTime = Instant.ofEpochMilli(1000),
                    duration = Duration.ofSeconds(2),
                ),
                WearWidgetVisibleEvent(
                    instanceId = WidgetInstanceId(namespace = "ns", id = 2),
                    startTime = Instant.ofEpochMilli(2000),
                    duration = Duration.ofSeconds(3),
                ),
            )
        val eventBatch = WearWidgetEventBatch(events)
        provider.onEvents(eventBatch.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.events).containsExactlyElementsIn(events)
        assertThat(fakeExecutionCallback.isSuccess).isTrue()
    }

    @Test
    fun onEvents_widgetThrows_callbackCalled() = runTest {
        val exceptionHandlerScope =
            CoroutineScope(
                coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }
            )
        val provider = WearWidgetProviderImpl(context, testName, exceptionHandlerScope, testWidget)
        testWidget.enableFailureMode = true

        val events =
            listOf(
                WearWidgetVisibleEvent(
                    instanceId = WidgetInstanceId(namespace = "ns", id = 1),
                    startTime = Instant.ofEpochMilli(1000),
                    duration = Duration.ofSeconds(2),
                )
            )
        val eventBatch = WearWidgetEventBatch(events)
        provider.onEvents(eventBatch.toParcel(), fakeExecutionCallback)
        advanceUntilIdle()

        assertThat(testWidget.events).containsExactlyElementsIn(events)
        assertThat(fakeExecutionCallback.isFailure).isTrue()
    }

    private class TestGlanceWearWidget : GlanceWearWidget() {
        var lastRequestedInstanceId: WidgetInstanceId? = null
        var lastRequestedContainerType: Int? = null
        var addedHandle: ActiveWearWidgetHandle? = null
        var removedHandle: ActiveWearWidgetHandle? = null
        var enableFailureMode = false
        var content = @Composable { RemoteText("WearWidgetProviderImplTest") }
        var events: List<WearWidgetEvent>? = null

        override suspend fun provideWidgetData(
            context: Context,
            params: WearWidgetParams,
        ): WearWidgetData {
            lastRequestedInstanceId = params.instanceId
            lastRequestedContainerType = params.containerType
            if (enableFailureMode) {
                throw Exception("Test exception")
            }
            return WearWidgetDocument(backgroundColor = Color.Transparent) { content() }
        }

        override suspend fun onAdded(context: Context, widgetHandle: ActiveWearWidgetHandle) {
            addedHandle = widgetHandle
            if (enableFailureMode) {
                throw Exception("Test exception")
            }
        }

        override suspend fun onRemoved(context: Context, widgetHandle: ActiveWearWidgetHandle) {
            removedHandle = widgetHandle
            if (enableFailureMode) {
                throw Exception("Test exception")
            }
        }

        override suspend fun onEvents(context: Context, events: List<WearWidgetEvent>) {
            this@TestGlanceWearWidget.events = events
            if (enableFailureMode) {
                throw Exception("Test exception")
            }
        }
    }

    private class ChannelWidgetCallback(
        val scope: CoroutineScope,
        val channel: Channel<WearWidgetRawContent>,
    ) : IWearWidgetCallback.Stub() {
        override fun updateWidgetContent(contentParcel: WearWidgetRawContentParcel?) {
            if (contentParcel != null) {
                scope.launch { channel.send(WearWidgetRawContent.fromParcel(contentParcel)) }
            } else {
                channel.close(IllegalStateException("null contentParcel"))
            }
        }

        override fun getInterfaceVersion(): Int = VERSION
    }
}
