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
import android.provider.Settings
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        withForceAtLeast37 {
            val mockUpdateClient = mock<WidgetUpdateClient>()
            val mockWidgetCache = mock<WearWidgetCache>()
            val handle =
                ActiveWearWidgetHandle(
                    provider = TEST_COMPONENT,
                    instanceId = TEST_INSTANCE_ID,
                    containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                )
            val widget =
                TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
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
    fun triggerUpdate_debuggableAndEmulator_sendsBroadcastAndPullsUpdate() = runTest {
        withForceAtLeast37 {
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
    }

    @Test
    fun triggerUpdate_debuggableOnly_sendsBroadcastAndPushesUpdate() = runTest {
        withForceAtLeast37 {
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

                    verify(mockUpdateClient)
                        .sendUpdateBroadcast(any(), eq(null), eq(TEST_INSTANCE_ID))
                    verify(mockUpdateClient).pushUpdate(eq(context), any(), any())
                    verify(mockUpdateClient, never()).requestUpdate(any(), any(), any())
                }
            }
        }
    }

    @Test
    fun triggerUpdate_notDebuggable_doesNotSendUpdateBroadcast() = runTest {
        withForceAtLeast37 {
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
    }

    @Test
    fun triggerUpdate_withInstanceId_pushedUpdate() = runTest {
        withForceAtLeast37 {
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

            widget.triggerUpdate(context, TEST_INSTANCE_ID)

            verify(mockUpdateClient).pushUpdate(eq(context), any(), any())
        }
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
        withForceAtLeast37 {
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
    }

    @Test
    fun triggerUpdate_inDebugEnv_pullsUpdate() = runTest {
        withForceAtLeast37 {
            val mockUpdateClient = mock<WidgetUpdateClient>()
            val mockWidgetCache = mock<WearWidgetCache>()
            val handle =
                ActiveWearWidgetHandle(
                    provider = TEST_COMPONENT,
                    instanceId = TEST_INSTANCE_ID,
                    containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                )
            val widget =
                TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
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
                verify(mockUpdateClient)
                    .requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
                verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
            }
        }
    }

    @Test
    fun triggerUpdate_inDebugEnv_notActive_pullsUpdateUsingFallbackProvider() = runTest {
        withForceAtLeast37 {
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
                verify(mockUpdateClient)
                    .requestUpdate(any(), eq(testComponent), eq(TEST_INSTANCE_ID))
                verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
            }
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
        withForceAtLeast37 {
            val mockUpdateClient = mock<WidgetUpdateClient>()
            val mockWidgetCache = mock<WearWidgetCache>()
            val handle =
                ActiveWearWidgetHandle(
                    provider = TEST_COMPONENT,
                    instanceId = TEST_INSTANCE_ID,
                    containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                )
            val widget =
                TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
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
    }

    @Test
    fun triggerUpdateAll_debuggableAndEmulator_sendsBroadcastAndPullsUpdate() = runTest {
        withForceAtLeast37 {
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

                        verify(mockUpdateClient)
                            .sendUpdateBroadcast(any(), eq(testComponent), eq(null))
                        verify(mockUpdateClient).requestUpdate(any(), eq(testComponent), eq(null))
                        verify(mockUpdateClient, never()).pushUpdate(any(), any(), any())
                    }
                }
            }
        }
    }

    @Test
    fun triggerUpdateAll_debuggableOnly_sendsBroadcastAndPushesUpdate() = runTest {
        withForceAtLeast37 {
            val mockUpdateClient = mock<WidgetUpdateClient>()
            val mockWidgetCache = mock<WearWidgetCache>()
            val handle =
                ActiveWearWidgetHandle(
                    provider = TEST_COMPONENT,
                    instanceId = TEST_INSTANCE_ID,
                    containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                )
            val widget =
                TestWidget(mockUpdateClient, mockWidgetCache, activeWidgets = listOf(handle))
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
    ) : GlanceWearWidget(updateClient, widgetCache) {

        override suspend fun provideWidgetData(context: Context, params: WearWidgetParams) =
            WearWidgetDocument(background = WearWidgetBrush) {}

        override suspend fun findActiveWidgetById(context: Context, instanceId: WidgetInstanceId) =
            activeWidgets.find {
                it.instanceId == instanceId
            }

        override suspend fun fetchActiveWidgets(context: Context) = activeWidgets
    }

    private companion object {
        val TEST_COMPONENT = ComponentName("my.package", "my.package.MyClass")
        val TEST_INSTANCE_ID = WidgetInstanceId("ns", 123)

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

        suspend fun withForceAtLeast37(block: suspend () -> Unit) {
            GlanceWearWidget.forceIsAtLeast37ForTesting = true
            try {
                block.invoke()
            } finally {
                GlanceWearWidget.forceIsAtLeast37ForTesting = null
            }
        }
    }
}
