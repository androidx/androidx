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
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tests [BaseWidgetDelegate.setConfigurationResult]. */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateConfigurationResultTest {

    @get:Rule val rule = BaseWidgetDelegateTestRule()

    /** A provider of this app that is not a [GlanceAdaptiveWidgetReceiver]. */
    private class PlainAppWidgetProvider : AppWidgetProvider()

    @Test
    fun setConfigurationResult_withoutAppWidgetIdExtra_throwsAndDoesNotSetResult() {
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE),
                )
                .get()

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "instance_1",
                    options = Bundle().apply { putString("config_key", "config_value") },
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(shadowOf(activity).resultIntent).isNull()
    }

    @Test
    fun setConfigurationResult_withInvalidAppWidgetIdExtra_throwsAndDoesNotSetResult() {
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID,
                        ),
                )
                .get()

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "instance_1",
                    options = Bundle.EMPTY,
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(shadowOf(activity).resultIntent).isNull()
    }

    @Test
    fun setConfigurationResult_withWidgetIdAndOptions_storesThemAndSetsResultOk() {
        rule.setupBoundWidget(701, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 701),
                )
                .get()
        val options = Bundle().apply { putString("meal_window", "lunch") }

        rule
            .delegate()
            .setConfigurationResult(
                activity = activity,
                widgetId = "window_lunch",
                options = options,
            )

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(701)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("window_lunch")
        assertThat(storedOptions.getString("meal_window")).isEqualTo("lunch")
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(
                shadowOf(activity)
                    .resultIntent
                    ?.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
            )
            .isEqualTo(701)
    }

    @Test
    fun setConfigurationResult_withOnlyOptions_storesThemWithGeneratedWidgetIdAndSetsResultOk() {
        rule.setupBoundWidget(702, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 702),
                )
                .get()
        val options = Bundle().apply { putString("meal_window", "dinner") }

        rule
            .delegate()
            .setConfigurationResult(activity = activity, widgetId = null, options = options)

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(702)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isNotEmpty()
        assertThat(storedOptions.getString("meal_window")).isEqualTo("dinner")
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun setConfigurationResult_withBlankWidgetId_throwsAndDoesNotSetResult() {
        rule.setupBoundWidget(703, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 703),
                )
                .get()

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "   ",
                    options = Bundle.EMPTY,
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(eq(703), any())
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun setConfigurationResult_withWidgetIdKeyInOptions_storesWidgetIdArgument() {
        // EXTRA_WIDGET_ID is internal: only the widgetId argument requests an identifier.
        rule.setupBoundWidget(704, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 704),
                )
                .get()
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "from_options")
            }

        rule
            .delegate()
            .setConfigurationResult(
                activity = activity,
                widgetId = "from_argument",
                options = options,
            )

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(704)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("from_argument")
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun setConfigurationResult_withoutWidgetIdAndWithWidgetIdKeyInOptions_ignoresItAndGeneratesWidgetId() {
        rule.setupBoundWidget(705, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 705),
                )
                .get()
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "from_options")
            }

        rule
            .delegate()
            .setConfigurationResult(activity = activity, widgetId = null, options = options)

        val storedWidgetId =
            rule.appWidgetManager
                .getAppWidgetOptions(705)
                .getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(storedWidgetId).isNotEmpty()
        assertThat(storedWidgetId).isNotEqualTo("from_options")
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun setConfigurationResult_onAlreadyConfiguredWidgetWithoutWidgetId_preservesExistingWidgetId() {
        rule.setupBoundWidget(706, TestReceiver::class.java.name, widgetId = "existing_widget_id")
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 706),
                )
                .get()
        val options = Bundle().apply { putString("meal_window", "dinner") }

        rule
            .delegate()
            .setConfigurationResult(activity = activity, widgetId = null, options = options)

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(706)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("existing_widget_id")
        assertThat(storedOptions.getString("meal_window")).isEqualTo("dinner")
    }

    @Test
    fun setConfigurationResult_onAlreadyConfiguredWidgetWithWidgetId_overwritesExistingWidgetId() {
        rule.setupBoundWidget(707, TestReceiver::class.java.name, widgetId = "existing_widget_id")
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 707),
                )
                .get()

        rule
            .delegate()
            .setConfigurationResult(
                activity = activity,
                widgetId = "new_widget_id",
                options = Bundle.EMPTY,
            )

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(707)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("new_widget_id")
    }

    @Test
    fun setConfigurationResult_whenUpdateAppWidgetOptionsThrows_propagatesAndDoesNotSetResult() {
        rule.setupBoundWidget(708, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 708),
                )
                .get()
        val failure = RuntimeException("Simulated IPC write failure")
        doThrow(failure).whenever(rule.appWidgetManager).updateAppWidgetOptions(eq(708), any())

        val thrown =
            assertThrows(RuntimeException::class.java) {
                rule
                    .delegate()
                    .setConfigurationResult(
                        activity = activity,
                        widgetId = "window_lunch",
                        options = Bundle.EMPTY,
                    )
            }

        assertThat(thrown).isSameInstanceAs(failure)
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(shadowOf(activity).resultIntent).isNull()
    }

    @Test
    fun setConfigurationResult_forWidgetOfAnotherApp_throwsBeforeReadingOptions() {
        val otherProvider = ComponentName("com.example.other", "com.example.other.WidgetReceiver")
        bindWidget(711, otherProvider)
        val activity = configurationActivity(711)

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = null,
                    options = Bundle().apply { putString("meal_window", "lunch") },
                )
        }

        verify(rule.appWidgetManager, never()).getAppWidgetOptions(any())
        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertNothingCachedOrSent(711)
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(shadowOf(activity).resultIntent).isNull()
    }

    @Test
    fun setConfigurationResult_forWidgetOfReceiverThatIsNotGlanceAdaptive_throws() {
        bindWidget(712, rule.componentOf(PlainAppWidgetProvider::class.java.name))
        val activity = configurationActivity(712)

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "window_lunch",
                    options = Bundle.EMPTY,
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertNothingCachedOrSent(712)
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun setConfigurationResult_forWidgetOfMissingReceiverClass_throws() {
        bindWidget(713, rule.componentOf("${rule.context.packageName}.RemovedWidgetReceiver"))
        val activity = configurationActivity(713)

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "window_lunch",
                    options = Bundle.EMPTY,
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertNothingCachedOrSent(713)
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun setConfigurationResult_forUnboundAppWidgetId_throws() {
        val activity = configurationActivity(714)

        assertThrows(IllegalArgumentException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "window_lunch",
                    options = Bundle.EMPTY,
                )
        }

        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertNothingCachedOrSent(714)
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun setConfigurationResult_sendsUpdateWithPendingRenderToken() {
        rule.setupBoundWidget(715, TestReceiver::class.java.name)
        val activity = configurationActivity(715)

        rule
            .delegate()
            .setConfigurationResult(
                activity = activity,
                widgetId = "window_lunch",
                options = Bundle.EMPTY,
            )

        // So that the widget is updated, even if the platform sends no options change broadcast.
        val update = sentUpdates().single()
        assertThat(update.component?.className).isEqualTo(TestReceiver::class.java.name)
        assertThat(update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
            .asList()
            .containsExactly(715)
        val token = update.getStringExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN)
        assertThat(WidgetOptionsCache.consumePendingRender(715, token)).isTrue()
        // Nothing library-internal is stored with the options.
        assertThat(
                rule.appWidgetManager
                    .getAppWidgetOptions(715)
                    .containsKey(WidgetOptionsCache.EXTRA_RENDER_TOKEN)
            )
            .isFalse()
    }

    @Test
    fun setConfigurationResult_onWidgetWithFallbackWidgetId_writesNewWidgetIdThroughToCache() {
        rule.setupBoundWidget(716, TestReceiver::class.java.name, widgetId = "fallback_widget_id")
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(
            716,
            rule.appWidgetManager.getAppWidgetOptions(716),
        )
        val activity = configurationActivity(716)

        rule
            .delegate()
            .setConfigurationResult(
                activity = activity,
                widgetId = "window_lunch",
                options = Bundle.EMPTY,
            )

        // Seen by lookups before the options change broadcast is delivered.
        assertThat(WidgetOptionsCache.getCachedWidgetId(716)).isEqualTo("window_lunch")
    }

    @Test
    fun setConfigurationResult_whenStoringFails_doesNotCacheWidgetIdOrSendUpdate() {
        rule.setupBoundWidget(717, TestReceiver::class.java.name)
        val activity = configurationActivity(717)
        doThrow(RuntimeException("Simulated IPC write failure"))
            .whenever(rule.appWidgetManager)
            .updateAppWidgetOptions(eq(717), any())

        assertThrows(RuntimeException::class.java) {
            rule
                .delegate()
                .setConfigurationResult(
                    activity = activity,
                    widgetId = "window_lunch",
                    options = Bundle.EMPTY,
                )
        }

        assertThat(WidgetOptionsCache.getCachedWidgetId(717)).isNull()
        assertThat(WidgetOptionsCache.hasPendingRender(717)).isFalse()
        assertThat(sentUpdates()).isEmpty()
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun setConfigurationResult_thenImmediatePushUpdate_reachesWidgetUnderNewWidgetId() = runTest {
        // No options change broadcast is delivered in between, and a lookup already cached
        // the fallback widgetId.
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        rule.setupBoundWidget(718, TestReceiver::class.java.name, widgetId = "fallback_widget_id")
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(
            718,
            rule.appWidgetManager.getAppWidgetOptions(718),
        )

        rule
            .delegate()
            .setConfigurationResult(
                activity = configurationActivity(718),
                widgetId = "window_lunch",
                options = Bundle.EMPTY,
            )
        rule
            .delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = setOf("window_lunch"),
            )

        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(718))
    }

    @Test
    fun setConfigurationResult_whenReadingExistingOptionsFails_propagatesAndDoesNotSetResult() {
        // With no widgetId requested, the current one is read from the widget's options first.
        rule.setupBoundWidget(719, TestReceiver::class.java.name, widgetId = "existing_widget_id")
        val activity = configurationActivity(719)
        val failure = RuntimeException("Simulated IPC read failure")
        doThrow(failure).whenever(rule.appWidgetManager).getAppWidgetOptions(719)

        val thrown =
            assertThrows(RuntimeException::class.java) {
                rule
                    .delegate()
                    .setConfigurationResult(
                        activity = activity,
                        widgetId = null,
                        options = Bundle.EMPTY,
                    )
            }

        assertThat(thrown).isSameInstanceAs(failure)
        verify(rule.appWidgetManager, never()).updateAppWidgetOptions(eq(719), any())
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    private fun bindWidget(appWidgetId: Int, provider: ComponentName) {
        val info = AppWidgetProviderInfo().apply { this.provider = provider }
        shadowOf(AppWidgetManager.getInstance(rule.context)).addBoundWidget(appWidgetId, info)
    }

    private fun sentUpdates(): List<Intent> =
        shadowOf(rule.context as Application).broadcastIntents.filter {
            it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

    /** Asserts that nothing was cached for [appWidgetId], and that no update was sent. */
    private fun assertNothingCachedOrSent(appWidgetId: Int) {
        assertThat(WidgetOptionsCache.getCachedWidgetId(appWidgetId)).isNull()
        assertThat(WidgetOptionsCache.hasPendingRender(appWidgetId)).isFalse()
        assertThat(sentUpdates()).isEmpty()
    }

    private fun configurationActivity(appWidgetId: Int): Activity =
        Robolectric.buildActivity(
                Activity::class.java,
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            .get()
}
