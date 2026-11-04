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
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.renderer.GlanceTemplateRenderer
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf

/**
 * Shared setup, test receivers and helpers for the [BaseWidgetDelegate] tests, which are split per
 * operation. Each test class declares it as a rule:
 * ```
 * @get:Rule val rule = BaseWidgetDelegateTestRule()
 * ```
 *
 * [appWidgetManager] is a spy that records only the calls of the delegate under test, so that tests
 * can verify them. Test setup and checks of the platform state go through the real
 * [AppWidgetManager.getInstance] instead, so they never count as calls of the delegate.
 *
 * How individual compositions are grouped, sized and rendered is covered by
 * [GlanceRemoteViewsComposerTest], and the telemetry extras by [WidgetTelemetryHandlerTest].
 */
class BaseWidgetDelegateTestRule : ExternalResource() {

    class TestTemplate(templateId: Int = 1) : GlanceAdaptiveTemplate(templateId = templateId)

    class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    /** A second receiver for the widget definition of [TestReceiver], which apps should avoid. */
    class DuplicateTestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    val context: Context = ApplicationProvider.getApplicationContext()
    lateinit var appWidgetManager: AppWidgetManager
        private set

    lateinit var mockRenderer: (TestTemplate, GlanceSurface) -> Unit
        private set

    override fun before() {
        mockRenderer = mock()
        appWidgetManager = spy(AppWidgetManager.getInstance(context))
        WidgetOptionsCache.clear()
        GlanceRemoteViewsComposer.resetForTesting()
        AppWidgetTemplateRegistry.resetForTesting()
        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            TemplateRenderer(
                ArchetypeSelector { _, surface -> surface },
                GlanceTemplateRenderer { template, _, surface ->
                    { mockRenderer(template, surface) }
                },
            ),
        )
    }

    internal fun delegate() =
        BaseWidgetDelegate(context = context, appWidgetManager = appWidgetManager)

    /** The appWidgetIds of every [AppWidgetManager.updateAppWidget] call, in call order. */
    fun capturedUpdatedIds(): List<List<Int>> {
        val idsCaptor = argumentCaptor<IntArray>()
        verify(appWidgetManager, atLeastOnce())
            .updateAppWidget(idsCaptor.capture(), any<RemoteViews>())
        return idsCaptor.allValues.map { it.toList() }
    }

    fun componentOf(receiverName: String) = ComponentName(context.packageName, receiverName)

    fun setupBoundWidget(
        appWidgetId: Int,
        receiverName: String,
        widgetId: String? = null,
        hostCategory: Int? = null,
        extraOptions: Bundle? = null,
    ) {
        val info = AppWidgetProviderInfo().apply { provider = componentOf(receiverName) }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(appWidgetId, info)

        val bundle = extraOptions?.let { Bundle(it) } ?: Bundle()
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

    fun registerReceiverInManifest(
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
