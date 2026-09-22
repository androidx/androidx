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
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
            appWidgetIds: IntArray,
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
    fun onUpdate_whenWidgetIdMissing_generatesAndStoresDefaultWidgetIdInOptions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestReceiver()
        val appWidgetId1 = 10
        val appWidgetId2 = 20
        setupBoundWidget(context, appWidgetId1, TestReceiver::class.java.name)
        setupBoundWidget(context, appWidgetId2, TestReceiver::class.java.name)

        receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId1, appWidgetId2))

        val options1 = appWidgetManager.getAppWidgetOptions(appWidgetId1)
        val options2 = appWidgetManager.getAppWidgetOptions(appWidgetId2)
        val id1 = options1.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        val id2 = options2.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(UUID.fromString(id1)).isNotEqualTo(UUID.fromString(id2))
    }

    @Test
    fun onUpdate_whenWidgetIdAlreadyPresent_preservesExistingWidgetId() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestReceiver()
        val appWidgetId = 30
        setupBoundWidget(context, appWidgetId, TestReceiver::class.java.name)
        appWidgetManager.updateAppWidgetOptions(
            appWidgetId,
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "custom-id-30")
            },
        )

        receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        assertThat(options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("custom-id-30")
    }

    @Test
    fun onUpdate_whenAlreadyCached_doesNotUpdateAppWidgetOptions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestReceiver()
        val appWidgetId = 70
        setupBoundWidget(context, appWidgetId, TestReceiver::class.java.name)

        // First onUpdate generates and persists the ID in options
        receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        val options1 = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val generatedId = options1.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(UUID.fromString(generatedId)).isNotNull()

        // Manually update options in AppWidgetManager with a custom marker
        appWidgetManager.updateAppWidgetOptions(
            appWidgetId,
            Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "marker-id") },
        )

        // Second onUpdate should see cached state and skip re-initializing
        receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))

        // Options in AppWidgetManager remain untouched because re-initialization was skipped
        val options2 = appWidgetManager.getAppWidgetOptions(appWidgetId)
        assertThat(options2.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("marker-id")
    }

    @Test
    fun onAppWidgetOptionsChanged_whenWidgetIdMissing_generatesAndStoresWidgetId() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestReceiver()
        val appWidgetId = 50
        setupBoundWidget(context, appWidgetId, TestReceiver::class.java.name)
        val initialOptions =
            Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120) }

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, initialOptions)

        val storedOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val generatedId = storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(UUID.fromString(generatedId)).isNotNull()
    }

    @Test
    fun onAppWidgetOptionsChanged_whenWidgetIdMissingInNewOptions_preservesExistingCachedWidgetId() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val receiver = TestReceiver()
            val appWidgetId = 60
            setupBoundWidget(context, appWidgetId, TestReceiver::class.java.name)
            appWidgetManager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply {
                    putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "preconfigured-id")
                },
            )

            // Cache the options and ID via onUpdate
            receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))

            // New options arriving from host without EXTRA_WIDGET_ID (e.g. resize)
            val resizeOptions =
                Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150) }
            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                appWidgetId,
                resizeOptions,
            )

            val storedOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
                .isEqualTo("preconfigured-id")
        }

    @Test
    fun onAppWidgetOptionsChanged_onColdStart_whenCustomIdPersistedInAppWidgetManager_preservesCustomId() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val receiver = TestReceiver()
            val appWidgetId = 80
            setupBoundWidget(context, appWidgetId, TestReceiver::class.java.name)
            // Persist a custom widget ID in the platform options
            appWidgetManager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply {
                    putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "cold-start-custom-id")
                },
            )

            // Ensure cache is cold (empty)
            GlanceAdaptiveWidgetReceiver.clearOptionsCache()

            // Host sends options changed (e.g. resize) on cold start without EXTRA_WIDGET_ID
            val resizeOptions =
                Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) }
            receiver.onAppWidgetOptionsChanged(
                context,
                appWidgetManager,
                appWidgetId,
                resizeOptions,
            )

            val storedOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            assertThat(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
                .isEqualTo("cold-start-custom-id")
        }

    @Test
    fun onUpdate_whenConcurrentOptionsChangedOccurs_doesNotOverwriteNewerOptions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        var optionsChangeUpdateCount = 0
        val receiver =
            object : GlanceAdaptiveWidgetReceiver() {
                override val widgetName: String = "test_widget"
                override val coroutineContext = Dispatchers.Unconfined

                override fun onUpdate(
                    context: Context,
                    appWidgetManager: AppWidgetManager,
                    appWidgetIds: IntArray,
                ) {
                    super.onUpdate(context, appWidgetManager, appWidgetIds)
                    optionsChangeUpdateCount++
                }
            }
        val appWidgetId = 90
        setupBoundWidget(context, appWidgetId, receiver.javaClass.name)
        appWidgetManager.updateAppWidgetOptions(
            appWidgetId,
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100)
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "id-90")
            },
        )

        // Simulate onAppWidgetOptionsChanged with new size (minWidth = 300)
        val updatedOptions =
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "id-90")
            }
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, updatedOptions)
        assertThat(optionsChangeUpdateCount).isEqualTo(1)

        // onUpdate should see the up-to-date cache and not overwrite it with older platform options
        receiver.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        assertThat(optionsChangeUpdateCount).isEqualTo(2)

        // A subsequent onAppWidgetOptionsChanged with the same minWidth = 300 should be
        // deduplicated
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, updatedOptions)
        assertThat(optionsChangeUpdateCount).isEqualTo(2)
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
                    appWidgetIds: IntArray,
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
    fun onRestored_migratesWidgetOptionsInCache() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receiver = TestCountingReceiver()
        val oldAppWidgetId1 = 48
        val oldAppWidgetId2 = 49
        val newAppWidgetId1 = 49
        val newAppWidgetId2 = 50
        val options1 = Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100) }
        val options2 = Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) }

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, oldAppWidgetId1, options1)
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, oldAppWidgetId2, options2)
        assertThat(receiver.updateCallCount).isEqualTo(2)

        // Perform restore with overlapping IDs (48 -> 49, 49 -> 50)
        receiver.onRestored(
            context,
            intArrayOf(oldAppWidgetId1, oldAppWidgetId2),
            intArrayOf(newAppWidgetId1, newAppWidgetId2),
        )

        // Options match what was migrated: newId1 (49) has options1, newId2 (50) has options2 ->
        // deduplicated
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, newAppWidgetId1, options1)
        assertThat(receiver.updateCallCount).isEqualTo(2)

        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, newAppWidgetId2, options2)
        assertThat(receiver.updateCallCount).isEqualTo(2)

        // Old ID 48 was removed from cache, so changing its options triggers update
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, oldAppWidgetId1, options1)
        assertThat(receiver.updateCallCount).isEqualTo(3)
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
            appWidgetIds: IntArray,
        ) {
            throw IllegalStateException("Synchronous exception in onUpdate")
        }
    }

    private class CancellationReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "cancellation_widget"

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
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
