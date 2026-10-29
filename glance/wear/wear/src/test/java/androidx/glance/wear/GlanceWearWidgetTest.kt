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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.WearWidgetCapture
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class GlanceWearWidgetTest {

    @Test
    fun triggerPullUpdate_withInstanceId_clientRequestsUpdateForInstance() {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient)

        widget.triggerPullUpdate(getApplicationContext(), TEST_COMPONENT, TEST_INSTANCE_ID)

        verify(mockUpdateClient).requestUpdate(any(), eq(TEST_COMPONENT), eq(TEST_INSTANCE_ID))
    }

    @Test
    fun triggerUpdate_debuggableAndDevSettings_sendsBroadcastAndPullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val handle =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
        val context = getApplicationContext<Context>()
        val testComponent = ComponentName(context, TEST_COMPONENT.className)

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        WearWidgetCache(context).update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = true) {
                widget.triggerUpdate(context, TEST_INSTANCE_ID)

                verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(null), eq(TEST_INSTANCE_ID))
                verify(mockUpdateClient)
                    .requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
                verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
            }
        }
    }

    @Test
    fun triggerUpdate_debuggableAndEmulator_sendsBroadcastAndPullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient, activeWidgets = emptyList())
        val context = getApplicationContext<Context>()
        val testComponent = ComponentName(context, TEST_COMPONENT.className)

        WearWidgetCache(context).update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = false) {
                withHardware("ranchu") {
                    widget.triggerUpdate(context, TEST_INSTANCE_ID)

                    verify(mockUpdateClient)
                        .sendUpdateBroadcast(any(), eq(null), eq(TEST_INSTANCE_ID))
                    verify(mockUpdateClient)
                        .requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
                    verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
                }
            }
        }
    }

    @Test
    fun triggerUpdate_debuggableOnly_sendsBroadcastAndPushesUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val widget = TestWidget(mockUpdateClient, mockWidgetCache)
        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        val context = getApplicationContext<Context>()

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = false) {
                widget.triggerUpdate(context, TEST_INSTANCE_ID)

                verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(null), eq(TEST_INSTANCE_ID))
                verify(mockUpdateClient).pushUpdate(eq(context), any(), any())
                verify(mockUpdateClient, never()).requestUpdate(any(), any(), any())
            }
        }
    }

    @Test
    fun triggerUpdate_notDebuggable_doesNotSendUpdateBroadcast() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val widget = TestWidget(mockUpdateClient, mockWidgetCache)
        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        val context = getApplicationContext<Context>()
        context.applicationInfo.flags =
            context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv()

        widget.triggerUpdate(context, TEST_INSTANCE_ID)

        verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), any(), any())
    }

    @Test
    fun triggerUpdate_withInstanceId_pushedUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val widget = TestWidget(mockUpdateClient, mockWidgetCache)
        val context = getApplicationContext<Context>()

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        SystemClock.setCurrentTimeMillis(12345L)

        widget.triggerUpdate(context, TEST_INSTANCE_ID)

        verify(mockUpdateClient)
            .pushUpdate(
                eq(context),
                argThat { instanceId == TEST_INSTANCE_ID },
                argThat {
                    extras.getLong(WearWidgetCapture.CREATION_UPTIME_MILLIS_KEY) == 12345L
                },
            )
    }

    @Test
    @Config(maxSdk = 36)
    fun triggerUpdate_onOlderSdk_pullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val handle =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, activeWidgets = listOf(handle))
        val context = getApplicationContext<Context>()

        widget.triggerUpdate(context, TEST_INSTANCE_ID)

        verify(mockUpdateClient).requestUpdate(any(), eq(TEST_COMPONENT), eq(TEST_INSTANCE_ID))
        verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
    }

    @Test
    @Config(maxSdk = 36)
    fun triggerUpdate_onOlderSdk_invalidId_throws() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient, activeWidgets = listOf())
        val context = getApplicationContext<Context>()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { widget.triggerUpdate(context, TEST_INSTANCE_ID) }
        }
    }

    @Test
    @Config(maxSdk = 36)
    fun triggerUpdateAll_onOlderSdk_pullsUpdateForAll() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val handle1 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val handle2 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = WidgetInstanceId("ns", 456),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, activeWidgets = listOf(handle1, handle2))
        val context = getApplicationContext<Context>()

        widget.triggerUpdateAll(context)

        verify(mockUpdateClient).requestUpdate(any(), eq(TEST_COMPONENT), eq(TEST_INSTANCE_ID))
        verify(mockUpdateClient)
            .requestUpdate(any(), eq(TEST_COMPONENT), eq(WidgetInstanceId("ns", 456)))
        verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
    }

    @Test
    fun triggerUpdateAll_onNewerSdk_pushesUpdateForAll() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val handle1 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val handle2 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = WidgetInstanceId("ns", 456),
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget =
            TestWidget(
                mockUpdateClient,
                mockWidgetCache,
                activeWidgets = listOf(handle1, handle2),
            )
        val context = getApplicationContext<Context>()

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(WidgetInstanceId("ns", 456))))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(WidgetInstanceId("ns", 456)),
                )
            )
            .thenReturn(testWidgetParams(WidgetInstanceId("ns", 456)))

        widget.triggerUpdateAll(context)

        verify(mockUpdateClient)
            .pushUpdate(eq(context), argThat { instanceId == TEST_INSTANCE_ID }, any())
        verify(mockUpdateClient)
            .pushUpdate(
                eq(context),
                argThat { instanceId == WidgetInstanceId("ns", 456) },
                any(),
            )
        verify(mockUpdateClient, never()).requestUpdate(any(), any(), any())
    }

    @Test
    fun triggerUpdate_inDebugEnv_pullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val handle =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
        val context = getApplicationContext<Context>()
        val testComponent = ComponentName(context, TEST_COMPONENT.className)

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        WearWidgetCache(context).update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDevelopmentSettingsEnabled(context) {
            widget.triggerUpdate(context, TEST_INSTANCE_ID)

            verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), any(), any())
            verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
            verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
        }
    }

    @Test
    fun triggerUpdate_inDebugEnv_notActive_pullsUpdateUsingFallbackProvider() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient, activeWidgets = emptyList())
        val context = getApplicationContext<Context>()

        val cache = WearWidgetCache(context)
        val testComponent = ComponentName(context, TEST_COMPONENT.className)
        cache.update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDevelopmentSettingsEnabled(context) {
            widget.triggerUpdate(context, TEST_INSTANCE_ID)

            verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), any(), any())
            verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
            verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
        }
    }

    @Test
    fun triggerUpdateAll_inDebugEnv_noActiveWidgets_pullsUpdateForProvider() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient, activeWidgets = emptyList())
        val context = getApplicationContext<Context>()

        // Populate the real cache
        val cache = WearWidgetCache(context)
        val testComponent = ComponentName(context, TEST_COMPONENT.className)
        cache.update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDevelopmentSettingsEnabled(context) {
            widget.triggerUpdateAll(context)

            verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), any(), any())
            verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(null))
        }
    }

    @Test
    fun triggerUpdateAll_debuggableAndDevSettings_sendsBroadcastAndPullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val handle =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
        val context = getApplicationContext<Context>()

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        val cache = WearWidgetCache(context)
        val testComponent = ComponentName(context, TEST_COMPONENT.className)
        cache.update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = true) {
                widget.triggerUpdateAll(context)

                verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(testComponent), eq(null))
                verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(null))
                verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
            }
        }
    }

    @Test
    fun triggerUpdateAll_debuggableAndEmulator_sendsBroadcastAndPullsUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient, activeWidgets = emptyList())
        val context = getApplicationContext<Context>()

        val cache = WearWidgetCache(context)
        val testComponent = ComponentName(context, TEST_COMPONENT.className)
        cache.update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = false) {
                withHardware("ranchu") {
                    widget.triggerUpdateAll(context)

                    verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(testComponent), eq(null))
                    verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(null))
                    verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
                }
            }
        }
    }

    @Test
    fun triggerUpdateAll_debuggableOnly_sendsBroadcastAndPushesUpdate() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val handle =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget = TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
        val context = getApplicationContext<Context>()

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))
        val cache = WearWidgetCache(context)
        val testComponent = ComponentName(context, TEST_COMPONENT.className)
        cache.update {
            putServiceToWidgetMapping(
                testComponent.className,
                TestWidget::class.java.canonicalName!!,
            )
        }

        withDebuggableBuild(context, debuggable = true) {
            withDevelopmentSettingsEnabled(context, enabled = false) {
                widget.triggerUpdateAll(context)

                verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(testComponent), eq(null))
                verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), eq(null), any())
                verify(mockUpdateClient)
                    .pushUpdate(
                        eq(context),
                        argThat { instanceId == TEST_INSTANCE_ID },
                        any(),
                    )
                verify(mockUpdateClient, never()).requestUpdate(any(), any(), any())
            }
        }
    }

    @Test
    fun triggerUpdate_contentTooLarge_throws() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val widget =
            TestWidget(
                mockUpdateClient,
                mockWidgetCache,
                oversizedInstanceIds = setOf(TEST_INSTANCE_ID),
            )
        val context = getApplicationContext<Context>()

        whenever(mockWidgetCache.getContainerTypeForInstance(eq(TEST_INSTANCE_ID)))
            .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
        whenever(
                mockWidgetCache.getWidgetParams(
                    eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                    eq(TEST_INSTANCE_ID),
                )
            )
            .thenReturn(testWidgetParams(TEST_INSTANCE_ID))

        val exception =
            assertThrows(WearWidgetContentTooLargeException::class.java) {
                runBlocking { widget.triggerUpdate(context, TEST_INSTANCE_ID) }
            }

        assertThat(exception.contentSizeBytes).isEqualTo(OVERSIZED_CONTENT_SIZE_BYTES)
        verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
    }

    @Test
    fun triggerUpdateAll_contentTooLargeForOneInstance_updatesOthersAndThrows() = runTest {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val mockWidgetCache = mock<WearWidgetCache>()
        val oversizedInstanceId = WidgetInstanceId("ns", 456)
        val handle1 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = oversizedInstanceId,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val handle2 =
            ActiveWearWidgetHandle(
                provider = TEST_COMPONENT,
                instanceId = TEST_INSTANCE_ID,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
            )
        val widget =
            TestWidget(
                mockUpdateClient,
                mockWidgetCache,
                activeWidgets = listOf(handle1, handle2),
                oversizedInstanceIds = setOf(oversizedInstanceId),
            )
        val context = getApplicationContext<Context>()

        for (instanceId in listOf(oversizedInstanceId, TEST_INSTANCE_ID)) {
            whenever(mockWidgetCache.getContainerTypeForInstance(eq(instanceId)))
                .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
            whenever(
                    mockWidgetCache.getWidgetParams(
                        eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                        eq(instanceId),
                    )
                )
                .thenReturn(testWidgetParams(instanceId))
        }

        val exception =
            assertThrows(WearWidgetContentTooLargeException::class.java) {
                runBlocking { widget.triggerUpdateAll(context) }
            }

        assertThat(exception.contentSizeBytes).isEqualTo(OVERSIZED_CONTENT_SIZE_BYTES)
        verify(mockUpdateClient)
            .pushUpdate(eq(context), argThat { instanceId == TEST_INSTANCE_ID }, any())
        verify(mockUpdateClient, never())
            .pushUpdate(any(), argThat { instanceId == oversizedInstanceId }, any())
    }

    @Test
    fun triggerUpdateAll_contentTooLargeForMultipleInstances_throwsWithOthersSuppressed() =
        runTest {
            val mockUpdateClient = mock<WidgetUpdateClient>()
            val mockWidgetCache = mock<WearWidgetCache>()
            val oversizedInstanceIds =
                listOf(
                    WidgetInstanceId("ns", 456),
                    WidgetInstanceId("ns", 789),
                    WidgetInstanceId("ns", 1011),
                )
            val handles =
                (oversizedInstanceIds + TEST_INSTANCE_ID).map {
                    ActiveWearWidgetHandle(
                        provider = TEST_COMPONENT,
                        instanceId = it,
                        containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                    )
                }
            val widget =
                TestWidget(
                    mockUpdateClient,
                    mockWidgetCache,
                    activeWidgets = handles,
                    oversizedInstanceIds = oversizedInstanceIds.toSet(),
                )
            val context = getApplicationContext<Context>()

            for (handle in handles) {
                whenever(mockWidgetCache.getContainerTypeForInstance(eq(handle.instanceId)))
                    .thenReturn(ContainerInfo.CONTAINER_TYPE_SMALL)
                whenever(
                        mockWidgetCache.getWidgetParams(
                            eq(ContainerInfo.CONTAINER_TYPE_SMALL),
                            eq(handle.instanceId),
                        )
                    )
                    .thenReturn(testWidgetParams(handle.instanceId))
            }

            val exception =
                assertThrows(WearWidgetContentTooLargeException::class.java) {
                    runBlocking { widget.triggerUpdateAll(context) }
                }

            assertThat(exception.suppressedExceptions).hasSize(oversizedInstanceIds.size - 1)
            for (suppressed in exception.suppressedExceptions) {
                assertThat(suppressed).isInstanceOf(WearWidgetContentTooLargeException::class.java)
            }
            verify(mockUpdateClient)
                .pushUpdate(eq(context), argThat { instanceId == TEST_INSTANCE_ID }, any())
        }

    @Test
    fun provideWidgetDataAsRawContent_customWearWidgetDataTooLarge_throws() = runTest {
        val widget =
            TestWidget(mock<WidgetUpdateClient>(), oversizedInstanceIds = setOf(TEST_INSTANCE_ID))
        val context = getApplicationContext<Context>()

        val exception =
            assertThrows(WearWidgetContentTooLargeException::class.java) {
                runBlocking {
                    widget.provideWidgetDataAsRawContent(
                        context,
                        testWidgetParams(TEST_INSTANCE_ID),
                    )
                }
            }

        assertThat(exception.contentSizeBytes).isEqualTo(OVERSIZED_CONTENT_SIZE_BYTES)
    }

    @Test
    fun provideWidgetDataAsRawContent_inInspectionMode_customWearWidgetDataTooLarge_throws() =
        runTest {
            val widget =
                TestWidget(
                    mock<WidgetUpdateClient>(),
                    oversizedInstanceIds = setOf(TEST_INSTANCE_ID),
                )
            val context = getApplicationContext<Context>()

            assertThrows(WearWidgetContentTooLargeException::class.java) {
                runBlocking {
                    widget.provideWidgetDataAsRawContent(
                        context,
                        testWidgetParams(TEST_INSTANCE_ID),
                        isInspectionMode = true,
                    )
                }
            }
        }

    @Test
    fun provideWidgetDataAsRawContent_withinLimit_returnsContent() = runTest {
        val widget = TestWidget(mock<WidgetUpdateClient>())
        val context = getApplicationContext<Context>()

        val rawContent =
            widget.provideWidgetDataAsRawContent(context, testWidgetParams(TEST_INSTANCE_ID))

        assertThat(rawContent.rcDocument.size)
            .isAtMost(WearWidgetContentTooLargeException.WARNING_CONTENT_SIZE_BYTES)
    }

    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    @Test
    fun provideWidgetDataAsRawContent_inInspectionMode_doesNotChangeFontAxisFlag() = runTest {
        val widget = TestWidget(mock<WidgetUpdateClient>())
        val context = getApplicationContext<Context>()
        val params = testWidgetParams(TEST_INSTANCE_ID)
        val originalValue = RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis
        try {
            RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = true

            widget.provideWidgetDataAsRawContent(context, params, isInspectionMode = true)
            assertThat(RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis).isTrue()

            // Without a renderer installed, the renderer version falls back to the initial one,
            // which doesn't support empty font axes.
            widget.provideWidgetDataAsRawContent(context, params, isInspectionMode = false)
            assertThat(RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis).isFalse()
        } finally {
            RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = originalValue
        }
    }

    private suspend fun withDebuggableBuild(
        context: Context,
        debuggable: Boolean,
        block: suspend () -> Unit,
    ) {
        val originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags =
            if (debuggable) {
                originalFlags or android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
            } else {
                originalFlags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv()
            }
        try {
            block()
        } finally {
            context.applicationInfo.flags = originalFlags
        }
    }

    private suspend fun withDevelopmentSettingsEnabled(
        context: Context,
        enabled: Boolean = true,
        block: suspend () -> Unit,
    ) {
        val contentResolver = context.contentResolver
        val originalValue =
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            if (enabled) 1 else 0,
        )
        try {
            block()
        } finally {
            Settings.Global.putInt(
                contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                originalValue,
            )
        }
    }

    private suspend fun withHardware(hardware: String, block: suspend () -> Unit) {
        val originalHardware = android.os.Build.HARDWARE
        org.robolectric.util.ReflectionHelpers.setStaticField(
            android.os.Build::class.java,
            "HARDWARE",
            hardware,
        )
        try {
            block()
        } finally {
            org.robolectric.util.ReflectionHelpers.setStaticField(
                android.os.Build::class.java,
                "HARDWARE",
                originalHardware,
            )
        }
    }

    private class TestWidget(
        updateClient: WidgetUpdateClient,
        widgetCache: WearWidgetCache? = null,
        private val activeWidgets: List<ActiveWearWidgetHandle> = emptyList(),
        private val oversizedInstanceIds: Set<WidgetInstanceId> = emptySet(),
    ) : GlanceWearWidget(updateClient, widgetCache) {

        override suspend fun provideWidgetData(
            context: Context,
            params: WearWidgetParams,
        ): WearWidgetData =
            if (params.instanceId in oversizedInstanceIds) {
                OversizedWidgetData()
            } else {
                WearWidgetDocument(background = WearWidgetBrush) {}
            }

        override suspend fun findActiveWidgetById(context: Context, instanceId: WidgetInstanceId) =
            activeWidgets.find {
                it.instanceId == instanceId
            }

        override suspend fun fetchActiveWidgets(context: Context) = activeWidgets
    }

    /**
     * A custom [WearWidgetData] implementation, not based on [WearWidgetDocument], whose content is
     * always too large to be sent to the host.
     */
    private class OversizedWidgetData : WearWidgetData {
        override suspend fun captureRawContent(
            context: Context,
            params: WearWidgetParams,
            isInspectionMode: Boolean,
        ): WearWidgetRawContent =
            WearWidgetRawContent(
                rcDocument = ByteArray(OVERSIZED_CONTENT_SIZE_BYTES),
                extras = Bundle(),
            )
    }

    private companion object {
        val TEST_COMPONENT = ComponentName("my.package", "my.package.MyClass")
        val TEST_INSTANCE_ID = WidgetInstanceId("ns", 123)
        const val OVERSIZED_CONTENT_SIZE_BYTES =
            WearWidgetContentTooLargeException.MAX_CONTENT_SIZE_BYTES + 1

        fun testWidgetParams(instanceId: WidgetInstanceId) =
            WearWidgetParams(
                instanceId,
                ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 100f,
                heightDp = 100f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
    }
}
