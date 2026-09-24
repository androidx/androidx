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

package androidx.glance.adaptive.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests the orchestration performed by [BaseWidgetDelegate]: resolving target widgets through the
 * repository, handing them to [GlanceRemoteViewsComposer] and delivering the result to
 * [AppWidgetManager].
 *
 * How individual compositions are grouped, sized and rendered is covered by
 * [GlanceRemoteViewsComposerTest].
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    private class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
    }

    private class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var mockRenderer: (TestTemplate, GlanceSurface) -> Unit

    @Before
    fun setUp() {
        mockRenderer = mock()
        appWidgetManager = spy(AppWidgetManager.getInstance(context))
        GlanceRemoteViewsComposer.resetForTesting()
        AppWidgetTemplateRegistry.resetForTesting()
        AppWidgetTemplateRegistry.register(
            TestTemplate::class.java,
            TemplateRenderer { template, constraints ->
                { mockRenderer(template, constraints.surface) }
            },
        )
    }

    private fun delegate() =
        BaseWidgetDelegate(context = context, appWidgetManager = appWidgetManager)

    // region pushUpdate

    @Test
    fun pushUpdate_withoutReceivers_doesNotComposeOrUpdate() = runTest {
        delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        verify(mockRenderer, never()).invoke(any(), any())
        verify(appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_withExplicitEmptyWidgetIds_doesNotComposeOrUpdate() = runTest {
        setupBoundWidget(201, TestReceiver::class.java.name)
        registerReceiverInManifest(TestReceiver::class.java.name)

        delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = emptySet(),
            )

        verify(mockRenderer, never()).invoke(any(), any())
        verify(appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_withMismatchingReceiver_doesNotComposeOrUpdate() = runTest {
        setupBoundWidget(301, OtherReceiver::class.java.name)
        registerReceiverInManifest(OtherReceiver::class.java.name)

        delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        verify(mockRenderer, never()).invoke(any(), any())
        verify(appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_rendersTemplateAndUpdatesAppWidgetManager() = runTest {
        setupBoundWidget(201, TestReceiver::class.java.name)
        registerReceiverInManifest(TestReceiver::class.java.name)
        val testData = TestTemplate()

        delegate().pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(mockRenderer).invoke(eq(testData), any())
        assertThat(capturedUpdatedIds()).containsExactly(listOf(201))
    }

    @Test
    fun pushUpdate_withWidgetIds_updatesOnlyMatchingWidgets() = runTest {
        setupBoundWidget(101, TestReceiver::class.java.name, widgetId = "widget_123")
        setupBoundWidget(102, TestReceiver::class.java.name, widgetId = "widget_456")
        setupBoundWidget(103, TestReceiver::class.java.name, widgetId = "widget_789")
        registerReceiverInManifest(TestReceiver::class.java.name)

        delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = setOf("widget_123", "widget_456"),
            )

        assertThat(capturedUpdatedIds().flatten()).containsExactly(101, 102)
    }

    @Test
    fun pushUpdate_withUnknownWidgetIds_doesNotComposeOrUpdate() = runTest {
        setupBoundWidget(101, TestReceiver::class.java.name, widgetId = "widget_123")
        registerReceiverInManifest(TestReceiver::class.java.name)

        delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = setOf("widget_unknown"),
            )

        verify(mockRenderer, never()).invoke(any(), any())
        verify(appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_updatesEachRenderTargetWithItsOwnWidgets() = runTest {
        setupBoundWidget(
            401,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        setupBoundWidget(
            402,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        registerReceiverInManifest(TestReceiver::class.java.name)

        delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        // Different surfaces compose separately, and each result goes only to its own instances.
        assertThat(capturedUpdatedIds()).containsExactly(listOf(401), listOf(402))
    }

    @Test
    fun pushUpdate_whenUpdateFailsForOneTarget_stillUpdatesRemainingTargets() = runTest {
        setupBoundWidget(
            401,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        setupBoundWidget(
            402,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever target is delivered first, then behave normally.
        doThrow(IllegalStateException("Simulated host failure"))
            .doCallRealMethod()
            .whenever(appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        assertThat(capturedUpdatedIds()).containsExactly(listOf(401), listOf(402))
    }

    // endregion

    // region setPreview

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_sdk35_withMatchingReceiver_rendersAndCallsSetWidgetPreview() = runTest {
        registerReceiverInManifest(TestReceiver::class.java.name)
        val testData = TestTemplate()

        delegate().setPreview(widgetName = "test_widget", previewData = testData)

        // A home screen provider also gets a keyguard picker entry by default.
        verify(mockRenderer, times(2)).invoke(eq(testData), any())
        verify(appWidgetManager)
            .setWidgetPreview(
                eq(componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                any(),
            )
        verify(appWidgetManager)
            .setWidgetPreview(
                eq(componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                any(),
            )
    }

    @Test
    @Config(sdk = [34])
    fun setPreview_preSdk35_gracefullyNoOps() = runTest {
        registerReceiverInManifest(TestReceiver::class.java.name)

        delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())

        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_withoutMatchingReceivers_doesNotComposeOrSetPreview() = runTest {
        delegate().setPreview(widgetName = "unregistered_widget", previewData = TestTemplate())

        verify(mockRenderer, never()).invoke(any(), any())
        verify(appWidgetManager, never())
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenSetWidgetPreviewFailsForOneEntry_stillSetsRemainingPreviews() = runTest {
        registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever picker entry is delivered first, then behave normally.
        doThrow(IllegalStateException("Simulated host failure"))
            .doCallRealMethod()
            .whenever(appWidgetManager)
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

        delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())

        verify(appWidgetManager)
            .setWidgetPreview(
                eq(componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                any(),
            )
        verify(appWidgetManager)
            .setWidgetPreview(
                eq(componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                any(),
            )
    }

    // endregion

    /** The appWidgetIds of every [AppWidgetManager.updateAppWidget] call, in call order. */
    private fun capturedUpdatedIds(): List<List<Int>> {
        val idsCaptor = argumentCaptor<IntArray>()
        verify(appWidgetManager, atLeastOnce())
            .updateAppWidget(idsCaptor.capture(), any<RemoteViews>())
        return idsCaptor.allValues.map { it.toList() }
    }

    private fun componentOf(receiverName: String) = ComponentName(context.packageName, receiverName)

    private fun setupBoundWidget(
        appWidgetId: Int,
        receiverName: String,
        widgetId: String? = null,
        hostCategory: Int? = null,
    ) {
        val info = AppWidgetProviderInfo().apply { provider = componentOf(receiverName) }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(appWidgetId, info)

        val bundle = Bundle()
        if (widgetId != null) {
            bundle.putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId)
        }
        if (hostCategory != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, hostCategory)
        }
        if (!bundle.isEmpty) {
            AppWidgetManager.getInstance(context).updateAppWidgetOptions(appWidgetId, bundle)
        }
    }

    private fun registerReceiverInManifest(
        receiverName: String,
        widgetCategory: Int = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
    ) {
        val componentName = componentOf(receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
        val info =
            AppWidgetProviderInfo().apply {
                provider = componentName
                this.widgetCategory = widgetCategory
            }
        val shadowManager = shadowOf(AppWidgetManager.getInstance(context))
        shadowManager.addInstalledProvider(info)
        shadowManager.addInstalledProvidersForProfile(null, info)
    }
}
