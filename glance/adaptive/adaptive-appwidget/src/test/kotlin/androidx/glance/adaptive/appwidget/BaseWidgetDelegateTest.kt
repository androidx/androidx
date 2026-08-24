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
import android.os.Bundle
import androidx.glance.adaptive.core.TemplateRegistry
import androidx.glance.adaptive.core.templates.AdaptiveGlanceTemplate
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    private lateinit var mockRenderer: (TestTemplate) -> Unit

    @Before
    fun setUp() {
        mockRenderer = mock()
        TemplateRegistry.reset()
        TemplateRegistry.register(TestTemplate::class.java) { template -> mockRenderer(template) }
    }

    @Test
    fun pushUpdate_withoutReceivers_completesWithoutErrors() = runTest {
        val delegate = BaseWidgetDelegate(ApplicationProvider.getApplicationContext())
        delegate.pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        verify(mockRenderer, never()).invoke(any())
    }

    @Test
    fun pushUpdate_withExplicitEmptyWidgetIds_completesWithoutErrors() = runTest {
        val delegate = BaseWidgetDelegate(ApplicationProvider.getApplicationContext())
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = TestTemplate(),
            widgetIds = emptySet(),
        )
        verify(mockRenderer, never()).invoke(any())
    }

    private class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
    }

    private class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"
    }

    @Test
    fun pushUpdate_withWidgetIds_filtersMatchingWidgetIds() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 101, TestReceiver::class.java.name, widgetId = "widget_123")
        setupBoundWidget(context, 102, TestReceiver::class.java.name, widgetId = "widget_456")
        setupBoundWidget(context, 103, TestReceiver::class.java.name, widgetId = "widget_789")
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        // Target widget_123 & widget_456: matches 101 and 102
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = testData,
            widgetIds = setOf("widget_123", "widget_456"),
        )
        verify(mockRenderer).invoke(testData)
        clearInvocations(mockRenderer)

        // Target widget_unknown: no matching widget
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = testData,
            widgetIds = setOf("widget_unknown"),
        )
        verify(mockRenderer, never()).invoke(any())
    }

    @Test
    fun pushUpdate_rendersTemplateAndUpdatesAppWidgetManager() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 201, TestReceiver::class.java.name)
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(mockRenderer).invoke(testData)
    }

    @Test
    fun pushUpdate_withMismatchingReceiver_doesNotRender() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 301, OtherReceiver::class.java.name)
        registerReceiverInManifest(context, OtherReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)
        verify(mockRenderer, never()).invoke(any())
    }

    private fun setupBoundWidget(
        context: Context,
        appWidgetId: Int,
        receiverName: String,
        widgetId: String? = null,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val componentName = ComponentName(context.packageName, receiverName)
        val info = AppWidgetProviderInfo().apply { provider = componentName }
        shadowManager.addBoundWidget(appWidgetId, info)

        if (widgetId != null) {
            appWidgetManager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId) },
            )
        }
    }

    private fun registerReceiverInManifest(context: Context, receiverName: String) {
        val componentName = ComponentName(context.packageName, receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
    }
}
