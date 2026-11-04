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
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import com.google.common.truth.Truth.assertThat
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
    fun setConfigurationResult_withBlankWidgetId_generatesWidgetIdAndSetsResultOk() {
        rule.setupBoundWidget(703, TestReceiver::class.java.name)
        val activity =
            Robolectric.buildActivity(
                    Activity::class.java,
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 703),
                )
                .get()

        rule
            .delegate()
            .setConfigurationResult(activity = activity, widgetId = "   ", options = Bundle.EMPTY)

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(703)
        val storedWidgetId = storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(storedWidgetId).isNotEmpty()
        assertThat(storedWidgetId).isNotEqualTo("   ")
        assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun setConfigurationResult_withWidgetIdOnlyInOptions_storesIt() {
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
            .setConfigurationResult(activity = activity, widgetId = null, options = options)

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(704)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("from_options")
    }

    @Test
    fun setConfigurationResult_withWidgetIdInArgumentAndOptions_storesArgument() {
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
            .setConfigurationResult(
                activity = activity,
                widgetId = "from_argument",
                options = options,
            )

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(705)
        assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("from_argument")
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
}
