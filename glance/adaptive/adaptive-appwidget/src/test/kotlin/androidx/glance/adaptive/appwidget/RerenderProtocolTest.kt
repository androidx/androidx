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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests the re-render protocol for library writes of a placement's options from outside
 * [GlanceAdaptiveWidgetReceiver]: [updateOptionsAndRerender], the widgetId kept by
 * [GlanceAdaptiveWidgetReceiver.onAppWidgetOptionsChanged] while a write is pending, and the token
 * check of the explicit update in [GlanceAdaptiveWidgetReceiver.onReceive].
 *
 * Robolectric delivers neither [AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED] nor the explicit
 * update to the receiver by itself, so each test delivers them explicitly, in the exact order under
 * test.
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class RerenderProtocolTest {

    private class CountingReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
        var updateCount: Int = 0

        override suspend fun onUpdate(context: Context) {}

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            super.onUpdate(context, appWidgetManager, appWidgetIds)
            updateCount++
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val provider = ComponentName(context, CountingReceiver::class.java)
    private val receiver = CountingReceiver()

    @Before
    fun setUp() {
        WidgetOptionsCache.clear()
        WidgetInstanceRepository.resetForTesting()
        val info = AppWidgetProviderInfo().apply { provider = this@RerenderProtocolTest.provider }
        shadowOf(appWidgetManager).addBoundWidget(APP_WIDGET_ID, info)
    }

    @Test
    fun updateOptionsAndRerender_storesOptionsCachesWidgetIdAndSendsUpdateWithToken() {
        writeForRerender("requested", extra = "config")

        val stored = storedOptions()
        assertThat(stored.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("requested")
        assertThat(stored.getString(EXTRA_CONFIG)).isEqualTo("config")
        assertThat(stored.containsKey(WidgetOptionsCache.EXTRA_RENDER_TOKEN)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")

        val update = sentUpdates().single()
        assertThat(update.action).isEqualTo(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        assertThat(update.component).isEqualTo(provider)
        assertThat(update.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
            .asList()
            .containsExactly(APP_WIDGET_ID)
        assertThat(update.flags and Intent.FLAG_RECEIVER_FOREGROUND).isNotEqualTo(0)
        val token = update.getStringExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN)
        assertThat(WidgetOptionsCache.consumePendingRender(APP_WIDGET_ID, token)).isTrue()
    }

    @Test
    fun updateWithToken_updatesPlacementOnceAndClearsMarker() {
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        deliver(sentUpdates().single())

        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
    }

    @Test
    fun optionsChangeBroadcastOfWrite_beforeUpdate_doesNotUpdateAgain() {
        // Android versions that still send the options change broadcast to the provider writing
        // its own options.
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        deliverStoredOptions()
        assertThat(receiver.updateCount).isEqualTo(0)

        deliver(sentUpdates().single())
        assertThat(receiver.updateCount).isEqualTo(1)
    }

    @Test
    fun optionsChangeBroadcastOfWrite_withColdCacheAndExistingHostSizes_doesNotUpdateAgain() {
        // Host has already set dimensions in AppWidgetManager before the library write, while
        // WidgetOptionsCache is still cold (e.g. initial setConfigurationResult).
        val hostOptionsWithSizes =
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            }
        appWidgetManager.updateAppWidgetOptions(APP_WIDGET_ID, hostOptionsWithSizes)

        writeForRerender("requested", extra = "config")

        // The broadcast of the write carries the host's sizes merged with the written options, and
        // matches the cache populated from getAppWidgetOptions before the write.
        deliver(Bundle(hostOptionsWithSizes).apply { putAll(storedOptions()) })
        assertThat(receiver.updateCount).isEqualTo(0)

        deliver(sentUpdates().single())
        assertThat(receiver.updateCount).isEqualTo(1)
    }

    @Test
    fun optionsChangeBroadcastOfWrite_afterUpdate_doesNotUpdateAgain() {
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        deliver(sentUpdates().single())
        deliverStoredOptions()

        assertThat(receiver.updateCount).isEqualTo(1)
    }

    @Test
    fun staleBroadcastDeliveredAfterWrite_keepsRequestedWidgetIdAndDoesNotUpdate() {
        // A pinned widget is updated, and so gets a fallback widgetId, before the pin callback
        // stores the requested one. The broadcast of that fallback widgetId write is delivered
        // after the requested write.
        registerReceiverInManifest()
        placeWithWidgetId("fallback")
        val staleFallbackBroadcast = storedOptions()
        writeForRerender("requested")

        deliver(staleFallbackBroadcast)

        assertThat(receiver.updateCount).isEqualTo(0)
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
        assertThat(
                WidgetInstanceRepository(context, appWidgetManager)
                    .findAppWidgetIdsForWidgetName("test_widget", setOf("requested"))
            )
            .containsKey(provider)

        deliver(sentUpdates().single())

        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
    }

    @Test
    fun staleBroadcastWithoutWidgetIdAfterWrite_doesNotOverwriteStoredOptions() {
        placeWithWidgetId("fallback")
        writeForRerender("requested", extra = "config")

        // An options broadcast queued at initial placement before any EXTRA_WIDGET_ID was set.
        deliver(Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) })

        assertThat(storedOptions().getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("requested")
        assertThat(storedOptions().getString(EXTRA_CONFIG)).isEqualTo("config")
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
    }

    @Test
    fun staleBroadcastWithNewSizeDeliveredAfterWrite_updatesButKeepsRequestedWidgetId() {
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        // A resize queued before the write: its size is new, but its widgetId is stale.
        deliver(
            widgetIdOptions("fallback").apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)
            }
        )

        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isTrue()
    }

    @Test
    fun lookupsBetweenWriteAndUpdate_seeNewWidgetIdAndDoNotSuppressUpdate() {
        registerReceiverInManifest()
        placeWithWidgetId("fallback")
        writeForRerender("requested")
        val repository = WidgetInstanceRepository(context, appWidgetManager)

        // pushUpdate filtering before the update is delivered sees the new widgetId only.
        assertThat(repository.findAppWidgetIdsForWidgetName("test_widget", setOf("requested")))
            .containsKey(provider)
        assertThat(repository.findAppWidgetIdsForWidgetName("test_widget", setOf("fallback")))
            .isEmpty()
        deliver(sentUpdates().single())

        assertThat(receiver.updateCount).isEqualTo(1)
    }

    @Test
    fun updateOfOlderWrite_updatesButDoesNotClearMarkerOfNewerWrite() {
        placeWithWidgetId("fallback")
        writeForRerender("first")
        val firstBroadcast = storedOptions()
        writeForRerender("second")
        val (firstUpdate, secondUpdate) = sentUpdates()

        deliver(firstUpdate)
        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isTrue()

        // The newer widgetId is still protected from the stale broadcast of the first write.
        deliver(firstBroadcast)
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("second")

        deliver(secondUpdate)
        assertThat(receiver.updateCount).isEqualTo(2)
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isFalse()
    }

    @Test
    fun updateWithoutToken_updatesButDoesNotClearMarker() {
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        // An update from the platform, such as a periodic one.
        deliver(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(APP_WIDGET_ID))
        )

        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isTrue()
    }

    @Test
    fun processDeath_updateStillUpdates() {
        placeWithWidgetId("fallback")
        writeForRerender("requested")

        // A restarted process has an empty cache and no marker.
        WidgetOptionsCache.clear()
        deliver(sentUpdates().single())

        assertThat(receiver.updateCount).isEqualTo(1)
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("requested")
    }

    @Test
    fun fallbackWidgetIdWrite_cachesWidgetIdSetsNoMarkerAndDoesNotLoop() {
        // No widgetId yet: the receiver writes a fallback one and updates the placement once.
        deliver(Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120) })
        assertThat(receiver.updateCount).isEqualTo(1)
        val fallbackWidgetId =
            storedOptions().getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
        assertThat(fallbackWidgetId).isNotNull()
        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo(fallbackWidgetId)
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isFalse()
        assertThat(sentUpdates()).isEmpty()

        // The broadcast of that fallback write does not update the placement again.
        deliverStoredOptions()

        assertThat(receiver.updateCount).isEqualTo(1)
    }

    @Test
    fun updateOptionsAndRerender_whenWriteFails_propagatesWithoutCachingOrSending() {
        placeWithWidgetId("fallback")
        val failingManager = spy(appWidgetManager)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(failingManager)
            .updateAppWidgetOptions(eq(APP_WIDGET_ID), any())

        assertThrows(RuntimeException::class.java) {
            failingManager.updateOptionsAndRerender(
                context,
                provider,
                APP_WIDGET_ID,
                widgetIdOptions("requested"),
            )
        }

        assertThat(WidgetOptionsCache.getCachedWidgetId(APP_WIDGET_ID)).isEqualTo("fallback")
        assertThat(WidgetOptionsCache.hasPendingRender(APP_WIDGET_ID)).isFalse()
        assertThat(sentUpdates()).isEmpty()
    }

    @Test
    fun remove_dropsMarker() {
        val token = WidgetOptionsCache.markPendingRender(APP_WIDGET_ID, "requested")

        WidgetOptionsCache.remove(intArrayOf(APP_WIDGET_ID))

        assertThat(WidgetOptionsCache.consumePendingRender(APP_WIDGET_ID, token)).isFalse()
    }

    @Test
    fun restore_movesCachedStateAndDropsMarker() {
        val token = WidgetOptionsCache.markPendingRender(APP_WIDGET_ID, "requested")

        WidgetOptionsCache.restore(intArrayOf(APP_WIDGET_ID), intArrayOf(RESTORED_APP_WIDGET_ID))

        assertThat(WidgetOptionsCache.getCachedWidgetId(RESTORED_APP_WIDGET_ID))
            .isEqualTo("requested")
        assertThat(WidgetOptionsCache.consumePendingRender(APP_WIDGET_ID, token)).isFalse()
        assertThat(WidgetOptionsCache.hasPendingRender(RESTORED_APP_WIDGET_ID)).isFalse()
    }

    private fun registerReceiverInManifest() {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(provider)
        shadowPackageManager.addIntentFilterForReceiver(
            provider,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
    }

    /** Places the widget with [widgetId] already handled by the receiver, as after `onUpdate`. */
    private fun placeWithWidgetId(widgetId: String) {
        appWidgetManager.updateAppWidgetOptions(APP_WIDGET_ID, widgetIdOptions(widgetId))
        WidgetOptionsCache.put(APP_WIDGET_ID, WidgetOptionsState.from(storedOptions()))
    }

    /** Writes [widgetId] and an [extra] option with the protocol, as a library caller does. */
    private fun writeForRerender(widgetId: String, extra: String? = null) {
        val options = widgetIdOptions(widgetId).apply { putString(EXTRA_CONFIG, extra) }
        appWidgetManager.updateOptionsAndRerender(context, provider, APP_WIDGET_ID, options)
    }

    private fun widgetIdOptions(widgetId: String) =
        Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId) }

    private fun storedOptions(): Bundle =
        Bundle(appWidgetManager.getAppWidgetOptions(APP_WIDGET_ID))

    private fun sentUpdates(): List<Intent> =
        shadowOf(context as Application).broadcastIntents.filter {
            it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

    private fun deliverStoredOptions() = deliver(storedOptions())

    private fun deliver(options: Bundle) {
        receiver.onAppWidgetOptionsChanged(context, appWidgetManager, APP_WIDGET_ID, options)
    }

    private fun deliver(update: Intent) {
        receiver.onReceive(context, update)
    }

    private companion object {
        const val APP_WIDGET_ID = 501
        const val RESTORED_APP_WIDGET_ID = 601
        const val EXTRA_CONFIG = "androidx.glance.adaptive.appwidget.test.EXTRA_CONFIG"
    }
}
