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
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
    fun requestPin_withOnlyKeyguardReceiver_returnsFalse() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(
            TestReceiver::class.java.name,
            widgetCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )

        assertThat(requestPin()).isFalse()
        verify(rule.appWidgetManager, never()).requestPinAppWidget(any(), anyOrNull(), anyOrNull())
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
    fun requestPin_withSuccessCallback_passesItThrough() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val successCallback =
            PendingIntent.getBroadcast(
                rule.context,
                /* requestCode= */ 0,
                Intent("test.action.PINNED").setPackage(rule.context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        assertThat(requestPin(successCallback = successCallback)).isTrue()

        verify(rule.appWidgetManager).requestPinAppWidget(any(), anyOrNull(), eq(successCallback))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun requestPin_withBlankWidgetIdAndNoOptions_passesSuccessCallbackThrough() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val successCallback = mutableSuccessCallback()

        assertThat(requestPin(widgetId = " ", successCallback = successCallback)).isTrue()

        verify(rule.appWidgetManager).requestPinAppWidget(any(), anyOrNull(), eq(successCallback))
    }

    @Test
    fun requestPin_withWidgetIdAndOptions_storesThemOnPinnedWidgetAndSendsSuccessCallback() =
        runTest {
            enablePinning()
            rule.registerReceiverInManifest(TestReceiver::class.java.name)
            val options = Bundle().apply { putString("config_key", "config_value") }

            val result =
                requestPin(
                    widgetId = "instance_1",
                    options = options,
                    successCallback = mutableSuccessCallback(),
                )
            // Delivers the success callback, which the platform sends once the widget is pinned.
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(result).isTrue()
            val appWidgetId = pinnedAppWidgetId()
            val storedOptions =
                AppWidgetManager.getInstance(rule.context).getAppWidgetOptions(appWidgetId)
            assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
                .isEqualTo("instance_1")
            assertThat(storedOptions.getString("config_key")).isEqualTo("config_value")
            val sentCallback =
                shadowOf(rule.context as Application).broadcastIntents.single {
                    it.action == ACTION_PINNED
                }
            assertThat(
                    sentCallback.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
                )
                .isEqualTo(appWidgetId)
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
        // So that the receiver sees an identifier change, and updates the widget with the options.
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isNotEmpty()
        assertThat(storedOptions.getString("config_key")).isEqualTo("config_value")
    }

    @Test
    fun requestPin_withWidgetIdOnlyInOptions_storesIt() = runTest {
        enablePinning()
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "from_options")
            }

        assertThat(requestPin(options = options)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()

        val storedOptions =
            AppWidgetManager.getInstance(rule.context).getAppWidgetOptions(pinnedAppWidgetId())
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("from_options")
    }

    @Test
    fun requestPin_withWidgetIdInArgumentAndOptions_storesArgument() = runTest {
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
        successCallback: PendingIntent? = null,
    ): Boolean =
        rule
            .delegate()
            .requestPin(
                widgetName = widgetName,
                widgetId = widgetId,
                initialData = initialData,
                options = options,
                successCallback = successCallback,
            )

    /** The appWidgetId of the single widget pinned through [enablePinning]. */
    private fun pinnedAppWidgetId(): Int =
        AppWidgetManager.getInstance(rule.context)
            .getAppWidgetIds(rule.componentOf(TestReceiver::class.java.name))
            .single()

    /** A mutable success callback, so that it receives the extras added when it is sent. */
    private fun mutableSuccessCallback(): PendingIntent =
        PendingIntent.getBroadcast(
            rule.context,
            /* requestCode= */ 0,
            Intent(ACTION_PINNED).setPackage(rule.context.packageName),
            PendingIntent.FLAG_MUTABLE,
        )

    private companion object {
        const val ACTION_PINNED = "androidx.glance.adaptive.appwidget.test.ACTION_PINNED"
    }
}
