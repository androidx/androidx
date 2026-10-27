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

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.ui.RemoteViewsTemplateRenderer
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.WidgetInstanceInfo
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceAdaptiveAppWidgetManagerTest {

    private class TestTemplate : GlanceAdaptiveTemplate(templateId = 1)

    /**
     * Delegate for a surface without AppWidgets, and therefore without dynamic previews, pinning,
     * or configuration activities.
     */
    private class OtherSurfaceDelegate : GlanceAdaptiveWidgetDelegate {
        var callCount = 0

        override suspend fun pushUpdate(
            widgetName: String,
            currentData: GlanceAdaptiveTemplate,
            widgetIds: Set<String>?,
        ) {
            callCount++
        }

        override suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> {
            callCount++
            return emptyList()
        }
    }

    /** Arguments of one [GlanceAdaptiveAppWidgetDelegate.requestPin] call. */
    private data class RequestPinCall(
        val widgetName: String,
        val widgetId: String?,
        val initialData: GlanceAdaptiveTemplate?,
        val options: Bundle,
        val successCallback: PendingIntent?,
    )

    /** Arguments of one [GlanceAdaptiveAppWidgetDelegate.setConfigurationResult] call. */
    private data class SetConfigurationResultCall(
        val activity: Activity,
        val widgetId: String?,
        val options: Bundle,
    )

    /** AppWidget delegate that records the previews, pin requests, and configuration results. */
    private class FakeAppWidgetDelegate : GlanceAdaptiveAppWidgetDelegate {
        val setPreviewCalls = mutableListOf<Pair<String, GlanceAdaptiveTemplate>>()
        val requestPinCalls = mutableListOf<RequestPinCall>()
        val setConfigurationResultCalls = mutableListOf<SetConfigurationResultCall>()

        override suspend fun pushUpdate(
            widgetName: String,
            currentData: GlanceAdaptiveTemplate,
            widgetIds: Set<String>?,
        ) {}

        override suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> =
            emptyList()

        override suspend fun setPreview(widgetName: String, previewData: GlanceAdaptiveTemplate) {
            setPreviewCalls += widgetName to previewData
        }

        override suspend fun requestPin(
            widgetName: String,
            widgetId: String?,
            initialData: GlanceAdaptiveTemplate?,
            options: Bundle,
            successCallback: PendingIntent?,
        ): Boolean {
            requestPinCalls +=
                RequestPinCall(widgetName, widgetId, initialData, options, successCallback)
            return true
        }

        override fun setConfigurationResult(
            activity: Activity,
            widgetId: String?,
            options: Bundle,
        ) {
            setConfigurationResultCalls += SetConfigurationResultCall(activity, widgetId, options)
        }
    }

    @Before
    fun setUp() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @After
    fun tearDown() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @Test
    fun glanceAdaptiveWidgetManager_withContext_createsManagerWithBaseWidgetDelegate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GlanceAdaptiveWidgetManager(context)

        assertThat(manager.delegate).isInstanceOf(BaseWidgetDelegate::class.java)
    }

    @Test
    fun setPreview_withAppWidgetDelegate_forwardsToDelegate() = runTest {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val previewData = TestTemplate()

        manager.setPreview(widgetName = "test_widget", previewData = previewData)

        assertThat(delegate.setPreviewCalls).containsExactly("test_widget" to previewData)
        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun setPreview_withNonAppWidgetDelegate_isNoOpAndLogsWarning() = runTest {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        manager.setPreview(widgetName = "test_widget", previewData = TestTemplate())

        assertThat(delegate.callCount).isEqualTo(0)
        assertThat(fallbackWarnings()).hasSize(1)
    }

    @Test
    fun setPreview_onManagerFromFactory_doesNotLogFallbackWarning() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GlanceAdaptiveWidgetManager(context)

        manager.setPreview(widgetName = "test_widget", previewData = TestTemplate())

        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun requestPin_withAppWidgetDelegate_forwardsToDelegate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val initialData = TestTemplate()
        val options = Bundle().apply { putString("config_key", "config_value") }
        val successCallback =
            PendingIntent.getBroadcast(
                context,
                /* requestCode= */ 0,
                Intent("test.action.PINNED").setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        val result =
            manager.requestPin(
                widgetName = "test_widget",
                widgetId = "instance_1",
                initialData = initialData,
                options = options,
                successCallback = successCallback,
            )

        assertThat(result).isTrue()
        assertThat(delegate.requestPinCalls)
            .containsExactly(
                RequestPinCall("test_widget", "instance_1", initialData, options, successCallback)
            )
        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun requestPin_withOnlyWidgetName_forwardsDefaultsToDelegate() = runTest {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        manager.requestPin(widgetName = "test_widget")

        assertThat(delegate.requestPinCalls)
            .containsExactly(
                RequestPinCall(
                    widgetName = "test_widget",
                    widgetId = null,
                    initialData = null,
                    options = Bundle.EMPTY,
                    successCallback = null,
                )
            )
    }

    @Test
    fun requestPin_withNonAppWidgetDelegate_returnsFalseAndLogsWarning() = runTest {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        val result = manager.requestPin(widgetName = "test_widget")

        assertThat(result).isFalse()
        assertThat(delegate.callCount).isEqualTo(0)
        assertThat(fallbackWarnings()).hasSize(1)
    }

    @Test
    fun setConfigurationResult_withAppWidgetDelegate_forwardsToDelegate() {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val options = Bundle().apply { putString("meal_window", "lunch") }

        manager.setConfigurationResult(
            activity = activity,
            widgetId = "window_lunch",
            options = options,
        )

        assertThat(delegate.setConfigurationResultCalls)
            .containsExactly(SetConfigurationResultCall(activity, "window_lunch", options))
        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun setConfigurationResult_withOnlyActivity_forwardsDefaultsToDelegate() {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        manager.setConfigurationResult(activity = activity)

        assertThat(delegate.setConfigurationResultCalls)
            .containsExactly(
                SetConfigurationResultCall(
                    activity = activity,
                    widgetId = null,
                    options = Bundle.EMPTY,
                )
            )
    }

    @Test
    fun setConfigurationResult_withNonAppWidgetDelegate_throwsIllegalStateException() {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        assertThrows(IllegalStateException::class.java) {
            manager.setConfigurationResult(activity = activity, widgetId = "window_lunch")
        }
        assertThat(delegate.callCount).isEqualTo(0)
    }

    @Test
    fun registerRenderer_withAppWidgetDelegate_overridesRendererForWidget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val remoteViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val manager = GlanceAdaptiveWidgetManager(FakeAppWidgetDelegate())

        manager.registerRenderer<TestTemplate, Unit>(
            widgetName = "track_widget",
            surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
            selector = ArchetypeSelector { _, _ -> },
            renderer = RemoteViewsTemplateRenderer { _, _, _, _ -> remoteViews },
        )

        val output =
            AppWidgetTemplateRegistry.render(
                TestTemplate(),
                AppWidgetSurface.MOBILE_LOCK_SCREEN,
                widgetName = "track_widget",
            )
        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        assertThat((output as RemoteViewsRenderOutput).provider(context))
            .isSameInstanceAs(remoteViews)
        // Other widgets and surfaces are left to the defaults, and TestTemplate has none.
        assertThrows(IllegalArgumentException::class.java) {
            AppWidgetTemplateRegistry.render(
                TestTemplate(),
                AppWidgetSurface.MOBILE_LOCK_SCREEN,
                widgetName = "other_widget",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppWidgetTemplateRegistry.render(
                TestTemplate(),
                AppWidgetSurface.MOBILE_HOME_SCREEN,
                widgetName = "track_widget",
            )
        }
    }

    @Test
    fun registerRenderer_withNonAppWidgetDelegate_throwsIllegalStateException() {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        assertThrows(IllegalStateException::class.java) {
            manager.registerRenderer<TestTemplate, Unit>(
                widgetName = "track_widget",
                surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
                selector = ArchetypeSelector { _, _ -> },
                renderer = RemoteViewsTemplateRenderer { _, _, _, _ -> error("Never rendered") },
            )
        }
        assertThat(delegate.callCount).isEqualTo(0)
        assertThrows(IllegalArgumentException::class.java) {
            AppWidgetTemplateRegistry.render(
                TestTemplate(),
                AppWidgetSurface.MOBILE_HOME_SCREEN,
                widgetName = "track_widget",
            )
        }
    }

    @Test
    fun registerRenderer_withAbstractTemplateType_throwsIllegalArgumentException() {
        val manager = GlanceAdaptiveWidgetManager(FakeAppWidgetDelegate())

        assertThrows(IllegalArgumentException::class.java) {
            manager.registerRenderer<GlanceAdaptiveTemplate, Unit>(
                widgetName = "track_widget",
                surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
                selector = ArchetypeSelector { _, _ -> },
                renderer = RemoteViewsTemplateRenderer { _, _, _, _ -> error("Never rendered") },
            )
        }
    }

    @Test
    fun glanceAdaptiveWidgetManager_doesNotDeclareAppWidgetOnlyMembers() {
        // A member would always win over the extension of the same name, silently shadowing it
        // and making the operation callable without depending on adaptive-appwidget.
        val memberNames = GlanceAdaptiveWidgetManager::class.java.methods.map { it.name }

        assertThat(memberNames)
            .containsNoneOf(
                "setPreview",
                "requestPin",
                "setConfigurationResult",
                "registerRenderer",
            )
    }

    private fun fallbackWarnings() = ShadowLog.getLogsForTag(LOG_TAG).filter { it.type == Log.WARN }

    private companion object {
        const val LOG_TAG = "GlanceAdaptiveAppWidget"
    }
}
