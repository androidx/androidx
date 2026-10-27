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
import android.os.Bundle
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests [WidgetTelemetryHandler]: which extras it writes into the options bundle, when it skips the
 * write, and how failures stay contained to one [RenderTarget] or one widget.
 *
 * That [BaseWidgetDelegate.pushUpdate] feeds it the composer's render targets, and that the
 * reported archetype matches the rendered one, is covered by [BaseWidgetDelegateTest].
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetTelemetryHandlerTest {

    private class TestTemplate(templateId: Int = 1) :
        GlanceAdaptiveTemplate(templateId = templateId)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var handler: WidgetTelemetryHandler

    @Before
    fun setUp() {
        appWidgetManager = spy(AppWidgetManager.getInstance(context))
        handler = WidgetTelemetryHandler(appWidgetManager)
        WidgetOptionsCache.clear()
    }

    @Test
    fun ensureTelemetryOptions_writesExtrasUnderPlatformContractKeys() {
        bindWidget(701)

        handler.ensureTelemetryOptions(
            mapOf(HOME_SCREEN_TARGET to listOf(701)),
            TestTemplate(templateId = 7),
        )

        // Platform AppWidgetServiceImpl reads these keys by literal value, so assert the strings
        // directly.
        val options = appWidgetManager.getAppWidgetOptions(701)
        assertThat(options.getBoolean("android.appwidget.option.ADAPTIVE_WIDGET_ENABLED")).isTrue()
        assertThat(options.getInt("android.appwidget.option.ADAPTIVE_WIDGET_TEMPLATE_TYPE"))
            .isEqualTo(7)
        assertThat(options.getInt("android.appwidget.option.ADAPTIVE_WIDGET_SDK_VERSION_CODE"))
            .isEqualTo(WidgetTelemetryHandler.XFF_SDK_VERSION_CODE)
        assertThat(
                options.containsKey("android.appwidget.option.ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS")
            )
            .isTrue()
        assertThat(options.getBoolean("android.appwidget.option.ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS"))
            .isFalse()
    }

    @Test
    fun ensureTelemetryOptions_sendsOnlyTelemetryKeys_andPreservesExistingOptions() {
        bindWidget(
            702,
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "instance_custom_id")
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
                )
                putString("custom_developer_key", "custom_value")
            },
        )

        handler.ensureTelemetryOptions(mapOf(LOCK_SCREEN_TARGET to listOf(702)), TestTemplate())

        // updateAppWidgetOptions merges with Bundle.putAll(), so any host-owned key sent here
        // would overwrite a concurrent host update.
        val delta = argumentCaptor<Bundle>()
        verify(appWidgetManager).updateAppWidgetOptions(eq(702), delta.capture())
        assertThat(delta.firstValue.keySet())
            .containsExactly(
                WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_ENABLED,
                WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE,
                WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE,
                WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS,
            )
        val options = appWidgetManager.getAppWidgetOptions(702)
        assertThat(options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("instance_custom_id")
        assertThat(options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY))
            .isEqualTo(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
        assertThat(options.getString("custom_developer_key")).isEqualTo("custom_value")
        assertThat(options.getInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE))
            .isEqualTo(1)
    }

    @Test
    fun ensureTelemetryOptions_whenExtrasAlreadyMatch_skipsUpdateAppWidgetOptions() {
        bindWidget(
            703,
            Bundle().apply {
                putBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_ENABLED, true)
                putInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE, 1)
                putInt(
                    WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE,
                    WidgetTelemetryHandler.XFF_SDK_VERSION_CODE,
                )
                putBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS, false)
            },
        )

        handler.ensureTelemetryOptions(mapOf(HOME_SCREEN_TARGET to listOf(703)), TestTemplate())

        verify(appWidgetManager, never()).updateAppWidgetOptions(eq(703), any())
    }

    @Test
    fun ensureTelemetryOptions_whenTemplateTypeChanges_updatesOptions() {
        bindWidget(
            704,
            Bundle().apply {
                putBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_ENABLED, true)
                putInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE, 1)
                putInt(
                    WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE,
                    WidgetTelemetryHandler.XFF_SDK_VERSION_CODE,
                )
                putBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS, false)
            },
        )

        handler.ensureTelemetryOptions(
            mapOf(HOME_SCREEN_TARGET to listOf(704)),
            TestTemplate(templateId = 2),
        )

        assertThat(
                appWidgetManager
                    .getAppWidgetOptions(704)
                    .getInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE)
            )
            .isEqualTo(2)
    }

    @Test
    fun parseSdkVersionCode_parsesSemanticVersionStrings() {
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("1.0.0-alpha01")).isEqualTo(10_000)
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("1.2.3")).isEqualTo(10_203)
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("2.5.0-beta02")).isEqualTo(20_500)
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("0.0.0")).isEqualTo(0)
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("1.100.0")).isNull()
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("1.0.100")).isNull()
        assertThat(WidgetTelemetryHandler.parseSdkVersionCode("invalid")).isNull()
    }

    @Test
    fun ensureTelemetryOptions_whenOptionsReadFailsForOneWidget_stillWritesOtherWidgets() {
        bindWidget(707)
        bindWidget(708)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(appWidgetManager)
            .getAppWidgetOptions(eq(707))

        handler.ensureTelemetryOptions(
            mapOf(HOME_SCREEN_TARGET to listOf(707, 708)),
            TestTemplate(),
        )

        verify(appWidgetManager, never()).updateAppWidgetOptions(eq(707), any())
        assertThat(
                appWidgetManager
                    .getAppWidgetOptions(708)
                    .getBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_ENABLED)
            )
            .isTrue()
    }

    @Test
    fun ensureTelemetryOptions_preCachesReceiverOptions_soOptionsChangedDoesNotTriggerOnUpdate() {
        bindWidget(
            709,
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            },
        )

        handler.ensureTelemetryOptions(mapOf(HOME_SCREEN_TARGET to listOf(709)), TestTemplate())

        // Simulate system_server delivering ACTION_APPWIDGET_OPTIONS_CHANGED with the merged
        // bundle, which now also carries the generated EXTRA_WIDGET_ID.
        val mergedOptions = appWidgetManager.getAppWidgetOptions(709)
        assertThat(mergedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isNotEmpty()
        var onUpdateCalled = false
        val receiver =
            object : GlanceAdaptiveWidgetReceiver() {
                override val widgetName: String = "test_widget"

                override suspend fun onUpdate(context: Context) {}

                override fun onUpdate(
                    context: Context,
                    appWidgetManager: AppWidgetManager,
                    appWidgetIds: IntArray,
                ) {
                    onUpdateCalled = true
                }
            }
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, 709, mergedOptions)

        assertThat(onUpdateCalled).isFalse()
    }

    /** Binds [appWidgetId] with [options] already stored, as a host would have left them. */
    private fun bindWidget(appWidgetId: Int, options: Bundle = Bundle()) {
        val realManager = AppWidgetManager.getInstance(context)
        val info =
            AppWidgetProviderInfo().apply {
                provider = ComponentName(context.packageName, TEST_PROVIDER_CLASS)
            }
        shadowOf(realManager).addBoundWidget(appWidgetId, info)
        if (!options.isEmpty) {
            // Seeded through the real instance so the spy records only the handler's calls.
            realManager.updateAppWidgetOptions(appWidgetId, options)
        }
    }

    private companion object {
        const val TEST_PROVIDER_CLASS = "androidx.glance.adaptive.appwidget.TestProvider"

        val HOME_SCREEN_TARGET = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(0, 0))
        val LOCK_SCREEN_TARGET = RenderTarget(AppWidgetSurface.MOBILE_LOCK_SCREEN, Dimensions(0, 0))
    }
}
