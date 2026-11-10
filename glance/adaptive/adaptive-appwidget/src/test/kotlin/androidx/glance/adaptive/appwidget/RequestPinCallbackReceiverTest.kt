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
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doCallRealMethod
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBroadcastPendingResult
import org.robolectric.shadows.ShadowLog

/**
 * Tests [RequestPinCallbackReceiver]. Retries run on [Dispatchers.Unconfined] without delay, so
 * every test completes within [RequestPinCallbackReceiver.onReceive].
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class RequestPinCallbackReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = spy(AppWidgetManager.getInstance(context))

    @Before
    fun setUp() {
        WidgetOptionsCache.clear()
    }

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
    fun createCallback_createsMutableBroadcastToReceiver() {
        val callback = RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("id"))

        val shadowCallback = shadowOf(callback)
        assertThat(shadowCallback.isBroadcast).isTrue()
        assertThat(shadowCallback.savedIntent.component)
            .isEqualTo(ComponentName(context, RequestPinCallbackReceiver::class.java))
        // The platform can only add the new appWidgetId to a mutable PendingIntent.
        assertThat(shadowCallback.isImmutable).isFalse()
    }

    @Test
    fun createCallback_keepsConcurrentRequestsApart() {
        bindWidget(appWidgetId = 701)
        bindWidget(appWidgetId = 702)
        val first = RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("first"))
        val second = RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("second"))

        deliver(first, appWidgetId = 701)
        deliver(second, appWidgetId = 702)

        assertThat(storedOptions(appWidgetId = 701).widgetId).isEqualTo("first")
        assertThat(storedOptions(appWidgetId = 702).widgetId).isEqualTo("second")
    }

    @Test
    fun onReceive_storesRequestedOptionsForRerender() {
        bindWidget(appWidgetId = 711)
        val options = widgetIdOptions("instance_1").apply { putString("config_key", "value") }

        deliver(RequestPinCallbackReceiver.createCallback(context, options), appWidgetId = 711)

        val stored = storedOptions(appWidgetId = 711)
        assertThat(stored.widgetId).isEqualTo("instance_1")
        assertThat(stored.getString("config_key")).isEqualTo("value")
        // Lookups see the requested widgetId immediately, and the widget is updated by an
        // explicit update carrying the pending token.
        assertThat(WidgetOptionsCache.getCachedWidgetId(711)).isEqualTo("instance_1")
        val update = sentUpdates().single()
        assertThat(update.component).isEqualTo(ComponentName(context.packageName, "TestReceiver"))
        assertThat(update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
            .asList()
            .containsExactly(711)
        assertThat(WidgetOptionsCache.consumePendingRender(711, update.token)).isTrue()
        assertThat(errorLogs()).isEmpty()
    }

    @Test
    fun onReceive_withoutAppWidgetId_logsErrorAndStoresNothing() {
        deliver(
            RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
            appWidgetId = null,
        )

        verify(appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertThat(errorLogs()).hasSize(1)
    }

    @Test
    fun onReceive_whenWriteFailsTwiceThenSucceeds_storesOptionsAndSendsOneUpdate() {
        bindWidget(appWidgetId = 721)
        val failure = RuntimeException("Simulated IPC failure")
        doThrow(failure)
            .doThrow(failure)
            .doCallRealMethod()
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(721), any())

        deliver(
            RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
            appWidgetId = 721,
        )

        verify(appWidgetManager, times(RequestPinCallbackReceiver.MAX_ATTEMPTS))
            .updateAppWidgetOptions(eq(721), any())
        assertThat(storedOptions(appWidgetId = 721).widgetId).isEqualTo("instance_1")
        assertThat(WidgetOptionsCache.getCachedWidgetId(721)).isEqualTo("instance_1")
        assertThat(WidgetOptionsCache.consumePendingRender(721, sentUpdates().single().token))
            .isTrue()
        assertThat(errorLogs()).isEmpty()
    }

    @Test
    fun onReceive_whenWriteFailsOnceThenSucceeds_storesOptionsAndSendsUpdate() {
        bindWidget(appWidgetId = 722)
        doThrow(RuntimeException("Simulated IPC failure"))
            .doCallRealMethod()
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(722), any())
        val options = widgetIdOptions("instance_1").apply { putString("config_key", "value") }

        deliver(RequestPinCallbackReceiver.createCallback(context, options), appWidgetId = 722)

        verify(appWidgetManager, times(2)).updateAppWidgetOptions(eq(722), any())
        val stored = storedOptions(appWidgetId = 722)
        assertThat(stored.widgetId).isEqualTo("instance_1")
        assertThat(stored.getString("config_key")).isEqualTo("value")
        assertThat(WidgetOptionsCache.getCachedWidgetId(722)).isEqualTo("instance_1")
        assertThat(sentUpdates()).hasSize(1)
        assertThat(errorLogs()).isEmpty()
    }

    @Test
    fun onReceive_whenEveryAttemptFails_logsErrorAndKeepsFallbackWidgetId() {
        bindWidget(appWidgetId = 731)
        placeWithFallbackWidgetId(appWidgetId = 731)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(731), any())

        deliver(
            RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
            appWidgetId = 731,
        )

        verify(appWidgetManager, times(RequestPinCallbackReceiver.MAX_ATTEMPTS))
            .updateAppWidgetOptions(eq(731), any())
        val error = errorLogs().single()
        assertThat(error.msg).contains("731")
        assertThat(error.msg).contains("instance_1")
        // The widget keeps its fallback widgetId, as when the callback never arrives.
        assertThat(storedOptions(appWidgetId = 731).widgetId).isEqualTo("fallback")
        assertThat(WidgetOptionsCache.getCachedWidgetId(731)).isEqualTo("fallback")
        assertThat(WidgetOptionsCache.hasPendingRender(731)).isFalse()
        assertThat(sentUpdates()).isEmpty()
    }

    @Test
    fun onReceive_whenWidgetRemovedBeforeRetry_stopsWithoutUpdate() {
        bindWidget(appWidgetId = 741)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(741), any())
        // Still bound when the callback arrives, removed before the retry.
        doCallRealMethod().doReturn(null).whenever(appWidgetManager).getAppWidgetInfo(741)

        deliver(
            RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
            appWidgetId = 741,
        )

        verify(appWidgetManager, times(1)).updateAppWidgetOptions(eq(741), any())
        assertThat(errorLogs()).isEmpty()
        assertThat(WidgetOptionsCache.hasPendingRender(741)).isFalse()
        assertThat(sentUpdates()).isEmpty()
    }

    @Test
    fun onReceive_whenWidgetRemovedBeforeCallback_storesNothingAndSetsNoMarker() {
        // appWidgetId 742 is not bound.
        deliver(
            RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
            appWidgetId = 742,
        )

        verify(appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertThat(errorLogs()).isEmpty()
        assertThat(WidgetOptionsCache.hasPendingRender(742)).isFalse()
    }

    @Test
    fun onReceive_withoutOptions_logsErrorAndStoresNothing() {
        bindWidget(appWidgetId = 751)

        RequestPinCallbackReceiver(
                appWidgetManagerProvider = { appWidgetManager },
                coroutineContext = Dispatchers.Unconfined,
                retryDelayMillis = 0,
            )
            .onReceive(
                context,
                Intent(context, RequestPinCallbackReceiver::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 751),
            )

        verify(appWidgetManager, never()).updateAppWidgetOptions(any(), any())
        assertThat(errorLogs()).hasSize(1)
        assertThat(WidgetOptionsCache.hasPendingRender(751)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun onReceive_afterRetrySucceeds_finishesBroadcast() {
        bindWidget(appWidgetId = 761)
        doThrow(RuntimeException("Simulated IPC failure"))
            .doCallRealMethod()
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(761), any())

        val receiver =
            deliverAsBroadcast(
                RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
                appWidgetId = 761,
            )

        assertThat(storedOptions(appWidgetId = 761).widgetId).isEqualTo("instance_1")
        assertThat(receiver.isBroadcastFinished()).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun onReceive_afterEveryAttemptFails_finishesBroadcast() {
        bindWidget(appWidgetId = 762)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(762), any())

        val receiver =
            deliverAsBroadcast(
                RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
                appWidgetId = 762,
            )

        assertThat(errorLogs()).hasSize(1)
        assertThat(receiver.isBroadcastFinished()).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun onReceive_whenRetryIsCancelled_stopsRetryingAndFinishesBroadcast() {
        bindWidget(appWidgetId = 763)
        doThrow(RuntimeException("Simulated IPC failure"))
            .doThrow(CancellationException("Simulated cancellation"))
            .whenever(appWidgetManager)
            .updateAppWidgetOptions(eq(763), any())

        val receiver =
            deliverAsBroadcast(
                RequestPinCallbackReceiver.createCallback(context, widgetIdOptions("instance_1")),
                appWidgetId = 763,
            )

        // Cancellation is not a failure: no further attempt, and nothing is logged.
        verify(appWidgetManager, times(2)).updateAppWidgetOptions(eq(763), any())
        assertThat(errorLogs()).isEmpty()
        assertThat(receiver.isBroadcastFinished()).isTrue()
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
        RequestPinCallbackReceiver(
                appWidgetManagerProvider = { appWidgetManager },
                coroutineContext = Dispatchers.Unconfined,
                retryDelayMillis = 0,
            )
            .onReceive(context, intent)
    }

    /**
     * Delivers [callback] like [deliver], but through a real broadcast to a registered receiver, so
     * that the receiver gets a PendingResult to finish.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    private fun deliverAsBroadcast(
        callback: PendingIntent,
        appWidgetId: Int,
    ): RequestPinCallbackReceiver {
        val receiver =
            RequestPinCallbackReceiver(
                appWidgetManagerProvider = { appWidgetManager },
                coroutineContext = Dispatchers.Unconfined,
                retryDelayMillis = 0,
            )
        context.registerReceiver(
            receiver,
            IntentFilter(ACTION_TEST_PIN_CALLBACK),
            Context.RECEIVER_NOT_EXPORTED,
        )
        context.sendBroadcast(
            Intent(ACTION_TEST_PIN_CALLBACK)
                .setPackage(context.packageName)
                .putExtras(Intent(shadowOf(callback).savedIntent))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        shadowOf(Looper.getMainLooper()).idle()
        return receiver
    }

    private fun RequestPinCallbackReceiver.isBroadcastFinished(): Boolean =
        Shadow.extract<ShadowBroadcastPendingResult>(shadowOf(this).originalPendingResult)
            .future
            .isDone

    /** Stores and caches a fallback widgetId, as after the first update of the widget. */
    private fun placeWithFallbackWidgetId(appWidgetId: Int) {
        AppWidgetManager.getInstance(context)
            .updateAppWidgetOptions(appWidgetId, widgetIdOptions("fallback"))
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(appWidgetId, storedOptions(appWidgetId))
    }

    private fun bindWidget(appWidgetId: Int) {
        val info =
            AppWidgetProviderInfo().apply {
                provider = ComponentName(context.packageName, "TestReceiver")
            }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(appWidgetId, info)
    }

    private fun storedOptions(appWidgetId: Int): Bundle =
        AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)

    private val Bundle.widgetId: String?
        get() = getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)

    private val Intent.token: String?
        get() = getStringExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN)

    private fun sentUpdates(): List<Intent> =
        shadowOf(context as Application).broadcastIntents.filter {
            it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

    private fun widgetIdOptions(widgetId: String) =
        Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId) }

    private fun errorLogs() =
        ShadowLog.getLogsForTag("RequestPinCallback").filter { it.type == Log.ERROR }

    private companion object {
        const val ACTION_TEST_PIN_CALLBACK =
            "androidx.glance.adaptive.appwidget.test.ACTION_TEST_PIN_CALLBACK"
    }
}
