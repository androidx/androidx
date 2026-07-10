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
import android.content.IntentFilter
import android.os.Build
import android.os.OutcomeReceiver
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetProviderInfo
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class GlanceWearWidgetManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val componentFullScreen: ComponentName =
        ComponentName(context, TestWidgetService1::class.java)
    private val tileProviderFullScreen: TileProvider = mock {
        on { componentName } doReturn componentFullScreen
        on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_FULLSCREEN
    }
    private val tileInstanceFullScreen: TileInstance = mock {
        on { tileProvider } doReturn tileProviderFullScreen
        on { id } doReturn 1
    }
    private val componentLarge: ComponentName =
        ComponentName(context, TestWidgetService2::class.java)
    private val tileProviderLarge: TileProvider = mock {
        on { componentName } doReturn componentLarge
        on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
    }
    private val tileInstanceLarge: TileInstance = mock {
        on { tileProvider } doReturn tileProviderLarge
        on { id } doReturn 2
    }
    private val tilesManager: TilesManager = mock()
    private val activeWidgetStore: ActiveWidgetStore = ActiveWidgetStore(context)

    private val widgetCache: WearWidgetCache = mock<WearWidgetCache>()
    private val widgetManager: GlanceWearWidgetManager =
        GlanceWearWidgetManager(context, tilesManager, activeWidgetStore, widgetCache)

    @After
    fun tearDown() = runTest { activeWidgetStore.markWidgetAsInactive(componentFullScreen, 1) }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgetsApi34_returnsAllActiveWidgets() = runTest {
        whenever(widgetCache.getServiceToWidgetMapping())
            .thenReturn(
                mapOf(
                    TestWidgetService1::class.java.name to TestWidget1::class.java.canonicalName!!,
                    TestWidgetService2::class.java.name to TestWidget2::class.java.canonicalName!!,
                )
            )

        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute {
                outcomeReceiver.onResult(listOf(tileInstanceFullScreen, tileInstanceLarge))
            }
        }

        val widgets = widgetManager.fetchActiveWidgets()

        assertThat(widgets)
            .containsExactly(
                ActiveWearWidgetHandle(
                    provider = componentFullScreen,
                    instanceId = WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 1),
                    containerType = ContainerInfo.CONTAINER_TYPE_TILE_COMPAT,
                ),
                ActiveWearWidgetHandle(
                    provider = componentLarge,
                    instanceId = WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 2),
                    containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                ),
            )
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
        whenever(widgetCache.getServiceToWidgetMapping())
            .thenReturn(
                mapOf(
                    TestWidgetService1::class.java.name to TestWidget1::class.java.canonicalName!!
                )
            )
        activeWidgetStore.markWidgetAsActive(componentFullScreen, 1)

        val widgets = widgetManager.fetchActiveWidgets()

        verify(tilesManager, never()).getActiveTiles(any(), any())
        assertThat(widgets)
            .containsExactly(
                ActiveWearWidgetHandle(
                    provider = componentFullScreen,
                    instanceId = WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 1),
                    containerType = ContainerInfo.CONTAINER_TYPE_TILE_COMPAT,
                )
            )
    }

    @Test
    fun updateService_updatesWidgetCache() = runTest {
        val service = TestWidgetService1()

        widgetManager.updateServiceMapping(service, service.widget)

        verify(widgetCache).update(any())
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_whenCacheEmpty_regeneratesMapping() = runTest {
        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute {
                outcomeReceiver.onResult(listOf(tileInstanceFullScreen, tileInstanceLarge))
            }
        }
        whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())
        // Mock PackageManager to list our service in queries.
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
        shadowPackageManager.addServiceIfNotPresent(componentFullScreen)
        shadowPackageManager.addIntentFilterForService(componentFullScreen, filter)
        shadowPackageManager.addServiceIfNotPresent(componentLarge)
        shadowPackageManager.addIntentFilterForService(componentLarge, filter)

        val widgets = widgetManager.fetchActiveWidgets(TestWidget1::class)

        assertThat(widgets)
            .containsExactly(
                ActiveWearWidgetHandle(
                    provider = componentFullScreen,
                    instanceId = WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 1),
                    containerType = ContainerInfo.CONTAINER_TYPE_TILE_COMPAT,
                )
            )
        verify(widgetCache).update(any())
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_whenCacheEmpty_withAssociateWithGlanceWearWidget_doesNotInstantiateService() =
        runTest {
            val componentAnnotated = ComponentName(context, AnnotatedWidgetService::class.java)
            val tileProviderAnnotated: TileProvider = mock {
                on { componentName } doReturn componentAnnotated
                on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
            }
            val tileInstanceAnnotated: TileInstance = mock {
                on { tileProvider } doReturn tileProviderAnnotated
                on { id } doReturn 3
            }

            whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
                val executor = invocationOnMock.getArgument<Executor>(0)
                val outcomeReceiver =
                    invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
                executor.execute { outcomeReceiver.onResult(listOf(tileInstanceAnnotated)) }
            }
            whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())

            // Mock PackageManager to list our service in queries.
            val shadowPackageManager = Shadows.shadowOf(context.packageManager)
            val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
            shadowPackageManager.addServiceIfNotPresent(componentAnnotated)
            shadowPackageManager.addIntentFilterForService(componentAnnotated, filter)

            val widgets = widgetManager.fetchActiveWidgets(TestWidget3::class)

            assertThat(widgets)
                .containsExactly(
                    ActiveWearWidgetHandle(
                        provider = componentAnnotated,
                        instanceId =
                            WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 3),
                        containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                    )
                )
            verify(widgetCache).update(any())
        }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_withClassArg_whenClassMismatches_returnsEmpty() = runTest {
        val componentAnnotated = ComponentName(context, AnnotatedWidgetService::class.java)
        val tileProviderAnnotated: TileProvider = mock {
            on { componentName } doReturn componentAnnotated
            on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
        }
        val tileInstanceAnnotated: TileInstance = mock {
            on { tileProvider } doReturn tileProviderAnnotated
            on { id } doReturn 3
        }

        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute { outcomeReceiver.onResult(listOf(tileInstanceAnnotated)) }
        }
        whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())

        // Mock PackageManager to list our service in queries.
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
        shadowPackageManager.addServiceIfNotPresent(componentAnnotated)
        shadowPackageManager.addIntentFilterForService(componentAnnotated, filter)

        // Query with TestWidget1 (a mismatch with the annotation which points to TestWidget3)
        val widgets = widgetManager.fetchActiveWidgets(TestWidget1::class)

        // It should return empty because the actual mapped widget class is TestWidget3
        assertThat(widgets).isEmpty()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_whenFallbackThrowsUninitializedPropertyAccess_ignoresServiceSafely() =
        runTest {
            val componentUninitialized =
                ComponentName(context, UninitializedWidgetService::class.java)
            val tileProviderUninitialized: TileProvider = mock {
                on { componentName } doReturn componentUninitialized
                on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
            }
            val tileInstanceUninitialized: TileInstance = mock {
                on { tileProvider } doReturn tileProviderUninitialized
                on { id } doReturn 4
            }

            whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
                val executor = invocationOnMock.getArgument<Executor>(0)
                val outcomeReceiver =
                    invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
                executor.execute { outcomeReceiver.onResult(listOf(tileInstanceUninitialized)) }
            }
            whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())

            // Mock PackageManager to list our service in queries.
            val shadowPackageManager = Shadows.shadowOf(context.packageManager)
            val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
            shadowPackageManager.addServiceIfNotPresent(componentUninitialized)
            shadowPackageManager.addIntentFilterForService(componentUninitialized, filter)

            // UninitializedWidgetService throws UninitializedPropertyAccessException. By querying
            // for TestWidget1,
            // we trigger mapping recovery, which ignores the broken service safely, causing the
            // result to be empty.
            val widgets = widgetManager.fetchActiveWidgets(TestWidget1::class)
            assertThat(widgets).isEmpty()
        }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_whenClassLoadingThrowsClassNotFound_ignoresServiceSafely() = runTest {
        val componentNonExistent = ComponentName(context, "androidx.glance.wear.NonExistentService")
        val tileProviderNonExistent: TileProvider = mock {
            on { componentName } doReturn componentNonExistent
            on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
        }
        val tileInstanceNonExistent: TileInstance = mock {
            on { tileProvider } doReturn tileProviderNonExistent
            on { id } doReturn 5
        }

        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute { outcomeReceiver.onResult(listOf(tileInstanceNonExistent)) }
        }
        whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())

        // Mock PackageManager to list the non-existent service in queries.
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
        shadowPackageManager.addServiceIfNotPresent(componentNonExistent)
        shadowPackageManager.addIntentFilterForService(componentNonExistent, filter)

        // Querying with TestWidget1 will trigger package recovery, which fails to load the
        // nonexistent class,
        // safely ignoring it, causing the list to be empty.
        val widgets = widgetManager.fetchActiveWidgets(TestWidget1::class)
        assertThat(widgets).isEmpty()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchActiveWidgets_whenFallbackThrowsRuntimeException_ignoresServiceSafely() = runTest {
        val componentCrashing = ComponentName(context, CrashingWidgetService::class.java)
        val tileProviderCrashing: TileProvider = mock {
            on { componentName } doReturn componentCrashing
            on { containerType } doReturn TilesManager.WIDGET_CONTAINER_TYPE_LARGE
        }
        val tileInstanceCrashing: TileInstance = mock {
            on { tileProvider } doReturn tileProviderCrashing
            on { id } doReturn 6
        }

        whenever(tilesManager.getActiveTiles(any(), any())).thenAnswer { invocationOnMock ->
            val executor = invocationOnMock.getArgument<Executor>(0)
            val outcomeReceiver =
                invocationOnMock.getArgument<OutcomeReceiver<List<TileInstance>, Exception>>(1)
            executor.execute { outcomeReceiver.onResult(listOf(tileInstanceCrashing)) }
        }
        whenever(widgetCache.getServiceToWidgetMapping()).thenReturn(emptyMap())

        // Mock PackageManager to list our crashing service in queries.
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val filter = IntentFilter(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
        shadowPackageManager.addServiceIfNotPresent(componentCrashing)
        shadowPackageManager.addIntentFilterForService(componentCrashing, filter)

        // Querying with TestWidget1 will trigger mapping recovery, which catches and ignores
        // constructor level RuntimeExceptions safely
        val widgets = widgetManager.fetchActiveWidgets(TestWidget1::class)
        assertThat(widgets).isEmpty()
    }
}

private open class TestWidget1 : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData = mock()
}

private open class TestWidget2 : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData = mock()
}

private open class TestWidget3 : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData = mock()
}

public class TestWidgetService1 : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = TestWidget1()
}

public class TestWidgetService2 : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = TestWidget2()
}

@AssociateWithGlanceWearWidget(TestWidget3::class)
public class AnnotatedWidgetService : GlanceWearWidgetService() {
    init {
        throw AssertionError("Service should not be instantiated")
    }

    override val widget: GlanceWearWidget
        get() = throw AssertionError("Service should not be instantiated")
}

public class UninitializedWidgetService : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget
        get() =
            throw UninitializedPropertyAccessException(
                "lateinit property widget has not been initialized"
            )
}

public class CrashingWidgetService : GlanceWearWidgetService() {
    init {
        throw RuntimeException("Simulated constructor crash")
    }

    override val widget: GlanceWearWidget
        get() = throw AssertionError("Should not be accessed")
}
