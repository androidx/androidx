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
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.applicationInfo.flags =
            context.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        GlanceAdaptiveWidgetReceiver.clearOptionsCache()
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

    private class TestCountingReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
        var updateCallCount: Int = 0

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray?,
        ) {
            updateCallCount++
        }
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
    fun onAppWidgetOptionsChanged_whenOptionsChanged_invokesOnUpdateForTargetWidgetId() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var updatedAppWidgetIds: IntArray? = null
        val receiver =
            object : GlanceAdaptiveWidgetReceiver() {
                override val widgetName: String = "test_widget"
                val onUpdateCalled = CompletableDeferred<Boolean>()

                override fun onUpdate(
                    context: Context,
                    appWidgetManager: AppWidgetManager,
                    appWidgetIds: IntArray?,
                ) {
                    updatedAppWidgetIds = appWidgetIds
                    super.onUpdate(context, appWidgetManager, appWidgetIds)
                }

                override suspend fun onUpdate(context: Context) {
                    onUpdateCalled.complete(true)
                }
            }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val targetAppWidgetId = 42
        val options =
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            }

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, targetAppWidgetId, options)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
        assertThat(updatedAppWidgetIds).isEqualTo(intArrayOf(targetAppWidgetId))
    }

    @Test
    fun onAppWidgetOptionsChanged_whenOptionsUnchanged_deduplicatesAndDoesNotInvokeOnUpdate() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val receiver = TestCountingReceiver()
            val targetAppWidgetId = 43
            val options =
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
                }

            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                targetAppWidgetId,
                options,
            )
            assertThat(receiver.updateCallCount).isEqualTo(1)

            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                targetAppWidgetId,
                options,
            )
            assertThat(receiver.updateCallCount).isEqualTo(1)
        }

    @Test
    fun onAppWidgetOptionsChanged_whenAppWidgetSizesChanged_invokesOnUpdate() = runTest {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@runTest
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestCountingReceiver()
        val targetAppWidgetId = 44
        val initialOptions =
            Bundle().apply {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(100f, 100f)),
                )
            }
        val updatedOptions =
            Bundle().apply {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(100f, 100f), SizeF(200f, 200f)),
                )
            }

        receiver.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            targetAppWidgetId,
            initialOptions,
        )
        assertThat(receiver.updateCallCount).isEqualTo(1)

        receiver.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            targetAppWidgetId,
            updatedOptions,
        )
        assertThat(receiver.updateCallCount).isEqualTo(2)
    }

    @Test
    fun onAppWidgetOptionsChanged_whenAppWidgetSizesReordered_deduplicatesAndDoesNotInvokeOnUpdate() =
        runTest {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@runTest
            val context = ApplicationProvider.getApplicationContext<Context>()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val receiver = TestCountingReceiver()
            val targetAppWidgetId = 46
            val initialOptions =
                Bundle().apply {
                    putParcelableArrayList(
                        AppWidgetManager.OPTION_APPWIDGET_SIZES,
                        arrayListOf(SizeF(100f, 100f), SizeF(200f, 200f)),
                    )
                }
            val reorderedOptions =
                Bundle().apply {
                    putParcelableArrayList(
                        AppWidgetManager.OPTION_APPWIDGET_SIZES,
                        arrayListOf(SizeF(200f, 200f), SizeF(100f, 100f)),
                    )
                }

            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                targetAppWidgetId,
                initialOptions,
            )
            assertThat(receiver.updateCallCount).isEqualTo(1)

            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                targetAppWidgetId,
                reorderedOptions,
            )
            assertThat(receiver.updateCallCount).isEqualTo(1)
        }

    @Test
    fun onDeleted_removesWidgetOptionsFromCache() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestCountingReceiver()
        val targetAppWidgetId = 45
        val options = Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100) }

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, targetAppWidgetId, options)
        assertThat(receiver.updateCallCount).isEqualTo(1)

        receiver.onDeleted(context, intArrayOf(targetAppWidgetId))

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, targetAppWidgetId, options)
        assertThat(receiver.updateCallCount).isEqualTo(2)
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

    @Test
    fun onReceive_invokesSuspendOnUpdateWhenDebugUpdateReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setupBoundWidget(context, 102, TestReceiver::class.java.name)
        val receiver = TestReceiver()

        val intent = Intent(GlanceAdaptiveWidgetReceiver.ACTION_DEBUG_UPDATE)

        receiver.onReceive(context, intent)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    @Test
    fun onReceive_whenDebugUpdateReceivedAndNotDebuggable_doesNotInvokeOnUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Temporarily clear FLAG_DEBUGGABLE for this test; setUp() resets it to true for subsequent
        // tests.
        context.applicationInfo.flags =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        setupBoundWidget(context, 102, TestReceiver::class.java.name)
        val receiver = TestReceiver()

        val intent = Intent(GlanceAdaptiveWidgetReceiver.ACTION_DEBUG_UPDATE)

        receiver.onReceive(context, intent)

        assertThat(receiver.onUpdateCalled.isCompleted).isFalse()
    }

    @Test
    fun onReceive_invokesSuspendOnUpdateWhenDebugUpdateWithAppWidgetIdsExtraReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetIds = intArrayOf(1, 2, 3)

        val intent =
            Intent(GlanceAdaptiveWidgetReceiver.ACTION_DEBUG_UPDATE).apply {
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
