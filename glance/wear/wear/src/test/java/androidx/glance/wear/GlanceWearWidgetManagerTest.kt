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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.wear.services.tiles.TileInstance
import com.google.wear.services.tiles.TileProvider
import com.google.wear.services.tiles.TilesManager
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class GlanceWearWidgetManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val component1: ComponentName = ComponentName(context, TestWidgetService1::class.java)
    private val tileProvider1: TileProvider = mock { on { componentName } doReturn component1 }
    private val tileInstance1: TileInstance = mock {
        on { tileProvider } doReturn tileProvider1
        on { id } doReturn 1
    }
    private val component2: ComponentName = ComponentName(context, TestWidgetService2::class.java)
    private val tileProvider2: TileProvider = mock { on { componentName } doReturn component2 }
    private val tileInstance2: TileInstance = mock {
        on { tileProvider } doReturn tileProvider2
        on { id } doReturn 2
    }
    private val tilesManager: TilesManager = mock()
    private val activeWidgetStore: ActiveWidgetStore = ActiveWidgetStore(context)

    private val widgetManager: GlanceWearWidgetManager =
        GlanceWearWidgetManager(tilesManager, activeWidgetStore)

    @After
    fun tearDown() {
        activeWidgetStore.markWidgetAsInactive(component1, 1)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgetsApi34_returnsAllActiveWidgets() = runTest {
        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute { outcomeReceiver.onResult(listOf(tileInstance1, tileInstance2)) }
        }

        val widgets = widgetManager.fetchActiveWidgets()

        assertThat(widgets.size).isEqualTo(2)
        assertThat(widgets[0].instanceId.id).isEqualTo(1)
        assertThat(widgets[0].instanceId.namespace)
            .isEqualTo(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE)
        assertThat(widgets[0].provider).isEqualTo(component1)
        assertThat(widgets[0].containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_FULLSCREEN)
        assertThat(widgets[1].instanceId.id).isEqualTo(2)
        assertThat(widgets[1].instanceId.namespace)
            .isEqualTo(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE)
        assertThat(widgets[1].provider).isEqualTo(component2)
        assertThat(widgets[1].containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_FULLSCREEN)
    }

    @Test(expected = RuntimeException::class)
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgetsApi34_ifError_throwsException() = runTest {
        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute { outcomeReceiver.onError(RuntimeException()) }
        }

        val unused = widgetManager.fetchActiveWidgets()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun fetchActiveWidgetsApi33_returnsAllActiveWidgets() = runTest {
        activeWidgetStore.markWidgetAsActive(component1, 1)

        val widgets = widgetManager.fetchActiveWidgets()

        verify(tilesManager, never()).getActiveTiles(any(), any())
        assertThat(widgets.size).isEqualTo(1)
        assertThat(widgets[0].instanceId.id).isEqualTo(1)
        assertThat(widgets[0].instanceId.namespace)
            .isEqualTo(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE)
        assertThat(widgets[0].provider).isEqualTo(component1)
        assertThat(widgets[0].containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_FULLSCREEN)
    }

    private class TestWidgetService1 : GlanceWearWidgetService() {
        override val widget: GlanceWearWidget = mock()
    }

    private class TestWidgetService2 : GlanceWearWidgetService() {
        override val widget: GlanceWearWidget = mock()
    }
}
