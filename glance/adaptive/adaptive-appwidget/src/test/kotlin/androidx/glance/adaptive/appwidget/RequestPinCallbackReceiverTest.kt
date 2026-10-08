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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class RequestPinCallbackReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    @Test
    fun receiver_isDeclaredWithoutBeingExported() {
        val receiverInfo =
            context.packageManager.getReceiverInfo(
                ComponentName(context, RequestPinCallbackReceiver::class.java),
                /* flags= */ 0,
            )

        // Otherwise other apps could change the options of this app's widgets through it.
        assertThat(receiverInfo.exported).isFalse()
    }

    @Test
    fun createSuccessCallback_createsMutableBroadcastToReceiver() {
        val callback =
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = optionsWithWidgetId("instance_1"),
                successCallback = null,
            )

        val shadowCallback = shadowOf(callback)
        assertThat(shadowCallback.isBroadcast).isTrue()
        assertThat(shadowCallback.savedIntent.component)
            .isEqualTo(ComponentName(context, RequestPinCallbackReceiver::class.java))
        // The platform can only add the new appWidgetId to a mutable PendingIntent.
        assertThat(shadowCallback.isImmutable).isFalse()
    }

    @Test
    fun createSuccessCallback_keepsConcurrentRequestsApart() {
        bindWidget(appWidgetId = 701)
        bindWidget(appWidgetId = 702)
        val first =
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = optionsWithWidgetId("first"),
                successCallback = null,
            )
        val second =
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = optionsWithWidgetId("second"),
                successCallback = null,
            )

        deliver(first, appWidgetId = 701)
        deliver(second, appWidgetId = 702)

        assertThat(storedWidgetId(appWidgetId = 701)).isEqualTo("first")
        assertThat(storedWidgetId(appWidgetId = 702)).isEqualTo("second")
    }

    @Test
    fun onReceive_storesRequestedOptionsOnPinnedWidget() {
        bindWidget(appWidgetId = 701)
        val options =
            optionsWithWidgetId("instance_1").apply { putString("config_key", "config_value") }

        deliver(
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = options,
                successCallback = null,
            ),
            appWidgetId = 701,
        )

        assertThat(storedWidgetId(appWidgetId = 701)).isEqualTo("instance_1")
        assertThat(appWidgetManager.getAppWidgetOptions(701).getString("config_key"))
            .isEqualTo("config_value")
    }

    @Test
    fun onReceive_sendsSuccessCallbackWithOnlyThePlatformExtras() {
        bindWidget(appWidgetId = 701)

        deliver(
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = optionsWithWidgetId("instance_1"),
                successCallback = mutableSuccessCallback(),
            ),
            appWidgetId = 701,
        )

        val sentCallback = sentSuccessCallbacks().single()
        assertThat(sentCallback.extras!!.keySet())
            .containsExactly(AppWidgetManager.EXTRA_APPWIDGET_ID)
        assertThat(
                sentCallback.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
            )
            .isEqualTo(701)
    }

    @Test
    fun onReceive_withoutAppWidgetId_stillSendsSuccessCallback() {
        deliver(
            RequestPinCallbackReceiver.createSuccessCallback(
                context = context,
                options = optionsWithWidgetId("instance_1"),
                successCallback = mutableSuccessCallback(),
            ),
            appWidgetId = null,
        )

        assertThat(sentSuccessCallbacks()).hasSize(1)
    }

    /**
     * Delivers [callback] to the receiver the way the platform sends it once the launcher has
     * pinned the widget, adding [appWidgetId] unless it is null.
     */
    private fun deliver(callback: PendingIntent, appWidgetId: Int?) {
        val intent = Intent(shadowOf(callback).savedIntent)
        if (appWidgetId != null) {
            intent.fillIn(
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                /* flags= */ 0,
            )
        }
        RequestPinCallbackReceiver().onReceive(context, intent)
    }

    private fun bindWidget(appWidgetId: Int) {
        val info =
            AppWidgetProviderInfo().apply {
                provider = ComponentName(context.packageName, "TestReceiver")
            }
        shadowOf(appWidgetManager).addBoundWidget(appWidgetId, info)
    }

    private fun storedWidgetId(appWidgetId: Int): String? =
        appWidgetManager
            .getAppWidgetOptions(appWidgetId)
            .getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)

    private fun optionsWithWidgetId(widgetId: String) =
        Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId) }

    /** A mutable success callback, so that it receives the extras added when it is sent. */
    private fun mutableSuccessCallback(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            /* requestCode= */ 0,
            Intent(ACTION_PINNED).setPackage(context.packageName),
            PendingIntent.FLAG_MUTABLE,
        )

    private fun sentSuccessCallbacks(): List<Intent> =
        shadowOf(context as Application).broadcastIntents.filter { it.action == ACTION_PINNED }

    private companion object {
        const val ACTION_PINNED = "androidx.glance.adaptive.appwidget.test.ACTION_PINNED"
    }
}
