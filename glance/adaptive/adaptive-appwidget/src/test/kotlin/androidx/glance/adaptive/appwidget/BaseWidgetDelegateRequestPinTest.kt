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

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.DuplicateTestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestTemplate
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tests [BaseWidgetDelegate.requestPin]. */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateRequestPinTest {

    @get:Rule val rule = BaseWidgetDelegateTestRule()

    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun requestPin_preSdk26_returnsFalse() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        assertThat(requestPin()).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_whenLauncherDoesNotSupportPinning_returnsFalse() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        assertThat(requestPin()).isFalse()
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withoutMatchingReceiver_throws() = runTest {
        enablePinning()

        val error = runCatching {
            requestPin(widgetName = "unregistered_widget")
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withOnlyKeyguardReceiver_throws() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(
            TestReceiver::class.java.name,
            widgetCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )

        val error = runCatching { requestPin() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun requestPin_preSdk26_withOnlyKeyguardReceiver_stillThrows() = runTest {
        rule.registerReceiverInManifest(
            TestReceiver::class.java.name,
            widgetCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )

        val error = runCatching { requestPin() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_whenComposingInitialDataFails_propagatesWithoutRequestingPin() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        whenever(rule.mockRenderer.invoke(any(), eq(AppWidgetSurface.MOBILE_HOME_SCREEN)))
            .thenThrow(IllegalStateException("Simulated renderer failure"))

        val error = runCatching { requestPin(initialData = TestTemplate()) }.exceptionOrNull()

        assertThat(error).isNotNull()
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_whenRequestPinAppWidgetThrows_propagatesUnchanged() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(IllegalStateException("Simulated missing foreground activity"))
            .whenever(rule.appWidgetManager)
            .requestPinAppWidget(any(), anyOrNull(), anyOrNull())

        val error = runCatching { requestPin() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Simulated missing foreground activity")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withSeveralMatchingReceivers_throwsListingThem() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        rule.registerReceiverInManifest(DuplicateTestReceiver::class.java.name)

        val error = runCatching { requestPin() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains(DuplicateTestReceiver::class.java.name)
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withMatchingReceiver_requestsPinWithoutPreview() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        assertThat(requestPin()).isTrue()

        verify(rule.mockRenderer, never()).invoke(any(), any())
        val extrasCaptor = argumentCaptor<Bundle>()
        verify(rule.appWidgetManager)
            .requestPinAppWidget(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                extrasCaptor.capture(),
                isNull(),
            )
        assertThat(extrasCaptor.firstValue.isEmpty).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withInitialData_attachesHomeScreenPreview() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val initialData = TestTemplate()

        assertThat(requestPin(initialData = initialData)).isTrue()

        verify(rule.mockRenderer).invoke(eq(initialData), eq(AppWidgetSurface.MOBILE_HOME_SCREEN))
        val extrasCaptor = argumentCaptor<Bundle>()
        verify(rule.appWidgetManager)
            .requestPinAppWidget(any(), extrasCaptor.capture(), anyOrNull())
        assertThat(extrasCaptor.firstValue.keySet())
            .containsExactly(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withBlankWidgetId_throwsBeforeRequestingPin() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val error = runCatching { requestPin(widgetId = "") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun requestPin_withWidgetIdAndOptions_storesThemOnPinnedWidgetForRerender() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val options = Bundle().apply { putString("config_key", "config_value") }

        val result = requestPin(widgetId = "instance_1", options = options)
        // Delivers the callback, which the platform sends once the widget is pinned.
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(result).isTrue()
        val appWidgetId = pinnedAppWidgetId()
        val storedOptions =
            AppWidgetManager.getInstance(rule.context).getAppWidgetOptions(appWidgetId)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("instance_1")
        assertThat(storedOptions.getString("config_key")).isEqualTo("config_value")
        // Stored with the re-render protocol: lookups see the widgetId immediately, and an
        // explicit update carrying the pending token updates the widget.
        assertThat(WidgetOptionsCache.getCachedWidgetId(appWidgetId)).isEqualTo("instance_1")
        val update =
            shadowOf(rule.context as Application).broadcastIntents.single {
                it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        val token = update.getStringExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN)
        assertThat(WidgetOptionsCache.consumePendingRender(appWidgetId, token)).isTrue()
    }

    @Test
    fun requestPin_withOnlyOptions_storesThemWithGeneratedWidgetId() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val options = Bundle().apply { putString("config_key", "config_value") }

        assertThat(requestPin(options = options)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()

        val storedOptions =
            AppWidgetManager.getInstance(rule.context).getAppWidgetOptions(pinnedAppWidgetId())
        // Generated at request time, so that the callback stores an explicit final widgetId.
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isNotEmpty()
        assertThat(storedOptions.getString("config_key")).isEqualTo("config_value")
    }

    @Test
    fun requestPin_withWidgetIdKeyInOptions_ignoresItAndGeneratesWidgetId() = runTest {
        // EXTRA_WIDGET_ID is internal: only the widgetId argument requests an identifier.
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "from_options")
            }

        assertThat(requestPin(options = options)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()

        val storedWidgetId =
            AppWidgetManager.getInstance(rule.context)
                .getAppWidgetOptions(pinnedAppWidgetId())
                .getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(storedWidgetId).isNotEmpty()
        assertThat(storedWidgetId).isNotEqualTo("from_options")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withWidgetIdArgumentAndWidgetIdKeyInOptions_storesWidgetIdArgument() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "from_options")
            }

        assertThat(requestPin(widgetId = "from_argument", options = options)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()

        val storedOptions =
            AppWidgetManager.getInstance(rule.context).getAppWidgetOptions(pinnedAppWidgetId())
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("from_argument")
    }

    /** Makes the launcher accept every pin request, as soon as it is made. */
    private fun enablePinning() {
        shadowOf(AppWidgetManager.getInstance(rule.context)).setRequestPinAppWidgetSupported(true)
    }

    private suspend fun requestPin(
        widgetName: String = "test_widget",
        widgetId: String? = null,
        initialData: GlanceAdaptiveTemplate? = null,
        options: Bundle = Bundle.EMPTY,
    ): Boolean =
        rule
            .delegate()
            .requestPin(
                widgetName = widgetName,
                widgetId = widgetId,
                initialData = initialData,
                options = options,
            )

    /** The appWidgetId of the single widget pinned through [enablePinning]. */
    private fun pinnedAppWidgetId(): Int =
        AppWidgetManager.getInstance(rule.context)
            .getAppWidgetIds(rule.componentOf(TestReceiver::class.java.name))
            .single()
}
