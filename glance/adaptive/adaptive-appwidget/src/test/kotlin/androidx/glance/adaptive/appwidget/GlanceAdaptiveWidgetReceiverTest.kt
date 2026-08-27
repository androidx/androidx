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
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceAdaptiveWidgetReceiverTest {

    @Before
    fun setUp() {
        ShadowLog.stream = PrintStream(ByteArrayOutputStream())
    }

    private class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"

        val onUpdateCalled = CompletableDeferred<Boolean>()

        override suspend fun onUpdate(context: Context) {
            onUpdateCalled.complete(true)
        }
    }

    private class DefaultReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "default_widget"
    }

    @Test
    fun widgetName_returnsConfiguredValue() {
        val receiver = TestReceiver()
        assertThat(receiver.widgetName).isEqualTo("test_widget")
    }

    @Test
    fun onUpdate_invokesSuspendOnUpdateWhenAppWidgetUpdateReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetIds = intArrayOf(1, 2, 3)

        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

        receiver.onReceive(context, intent)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    @Test
    fun defaultReceiver_onUpdateDoesNotThrow() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = DefaultReceiver()
        receiver.onUpdate(context)
    }

    @Test
    fun onUpdate_directInvocationTriggersSuspendOnUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = intArrayOf(10, 20)

        receiver.onUpdate(context, appWidgetManager, appWidgetIds)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    @Test
    fun onReceive_invokesSuspendOnUpdateWhenLocaleChangedReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setupBoundWidget(context, 101, TestReceiver::class.java.name)
        val receiver = TestReceiver()

        val intent = Intent(Intent.ACTION_LOCALE_CHANGED)

        receiver.onReceive(context, intent)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    @Test
    fun onReceive_invokesSuspendOnUpdateWhenLocaleChangedWithAppWidgetIdsExtraReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetIds = intArrayOf(1, 2, 3)

        val intent =
            Intent(Intent.ACTION_LOCALE_CHANGED).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

        receiver.onReceive(context, intent)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    private fun setupBoundWidget(context: Context, appWidgetId: Int, receiverName: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val componentName = ComponentName(context, receiverName)
        val info = AppWidgetProviderInfo().apply { provider = componentName }
        shadowManager.addBoundWidget(appWidgetId, info)
    }

    @Test
    fun onReceive_whenNoAppWidgetIds_doesNotInvokeOnUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val intent = Intent(Intent.ACTION_LOCALE_CHANGED)

        receiver.onReceive(context, intent)

        assertThat(receiver.onUpdateCalled.isCompleted).isFalse()
    }

    private class SyncExceptionReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "sync_exception_widget"

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray?,
        ) {
            throw IllegalStateException("Synchronous exception in onUpdate")
        }
    }

    private class CancellationReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "cancellation_widget"

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray?,
        ) {
            throw CancellationException("Simulated cancellation")
        }
    }

    @Test
    fun onReceive_whenExceptionThrownInOnReceive_catchesExceptionAndLogsError() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = SyncExceptionReceiver()
        val intent =
            Intent(Intent.ACTION_LOCALE_CHANGED).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(1))
            }

        receiver.onReceive(context, intent)

        val logs = ShadowLog.getLogsForTag("GlanceAdaptiveReceiver")
        assertThat(logs).hasSize(1)
        assertThat(logs[0].throwable).isInstanceOf(IllegalStateException::class.java)
        assertThat(logs[0].msg).isEqualTo("Error in Glance Adaptive Widget Receiver")
    }

    @Test
    fun onReceive_whenCancellationExceptionThrown_handlesCleanlyWithoutLoggingError() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = CancellationReceiver()
        val intent =
            Intent(Intent.ACTION_LOCALE_CHANGED).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(1))
            }

        receiver.onReceive(context, intent)

        val logs = ShadowLog.getLogsForTag("GlanceAdaptiveReceiver")
        assertThat(logs).isEmpty()
    }
}
