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
import android.content.IntentFilter
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetInstanceRepositoryTest {

    private class MatchingReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "matching_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    private class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    private lateinit var context: Context
    private lateinit var repository: WidgetInstanceRepository

    @Before
    fun setUp() {
        WidgetOptionsCache.clear()
        context = ApplicationProvider.getApplicationContext()
        repository = WidgetInstanceRepository(context)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withEmptyWidgetIds_returnsEmptyMap() {
        setupBoundWidget(101, MatchingReceiver::class.java.name)
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val result =
            repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = emptySet())

        assertThat(result).isEmpty()
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withNoReceivers_returnsEmptyMap() {
        val result = repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)

        assertThat(result).isEmpty()
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withNullWidgetIds_returnsAllMatchingAppWidgetIds() {
        setupBoundWidget(201, MatchingReceiver::class.java.name)
        setupBoundWidget(202, MatchingReceiver::class.java.name)
        setupBoundWidget(203, OtherReceiver::class.java.name)

        registerReceiverInManifest(MatchingReceiver::class.java.name)
        registerReceiverInManifest(OtherReceiver::class.java.name)

        val result = repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(201, 202)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withWidgetIds_filtersAppWidgetIdsByOptionsExtra() {
        setupBoundWidget(301, MatchingReceiver::class.java.name, widgetId = "instance_1")
        setupBoundWidget(302, MatchingReceiver::class.java.name, widgetId = "instance_2")
        setupBoundWidget(303, MatchingReceiver::class.java.name, widgetId = "instance_3")

        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val result =
            repository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("instance_1", "instance_3"),
            )

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(301, 303)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withBlankStoredWidgetId_doesNotMatch() {
        setupBoundWidget(311, MatchingReceiver::class.java.name, widgetId = "   ")
        setupBoundWidget(312, MatchingReceiver::class.java.name, widgetId = "instance_1")

        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val result =
            repository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("   ", "instance_1"),
            )

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(312)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withWarmCache_avoidsGetAppWidgetOptionsIpc() {
        val spyManager = spy(AppWidgetManager.getInstance(context))
        val spyRepository = WidgetInstanceRepository(context, spyManager)
        setupBoundWidget(401, MatchingReceiver::class.java.name, widgetId = "cached_instance")
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        WidgetOptionsCache.cacheWidgetOptionsIfMissing(
            401,
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "cached_instance")
            },
        )
        clearInvocations(spyManager)

        val result =
            spyRepository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("cached_instance"),
            )

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result[componentName]?.toList()).containsExactly(401)
        verify(spyManager, never()).getAppWidgetOptions(401)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_onCacheMiss_fallsBackToManagerAndCachesForSubsequentLookups() {
        val spyManager = spy(AppWidgetManager.getInstance(context))
        val spyRepository = WidgetInstanceRepository(context, spyManager)
        setupBoundWidget(402, MatchingReceiver::class.java.name, widgetId = "miss_instance")
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        clearInvocations(spyManager)

        // First lookup: cache miss falls back to AppWidgetManager.getAppWidgetOptions once
        val firstResult =
            spyRepository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("miss_instance"),
            )

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(firstResult[componentName]?.toList()).containsExactly(402)
        assertThat(WidgetOptionsCache.getCachedWidgetId(402)).isEqualTo("miss_instance")
        verify(spyManager, times(1)).getAppWidgetOptions(402)

        // Second lookup: warm cache serves without calling getAppWidgetOptions again
        val secondResult =
            spyRepository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("miss_instance"),
            )

        assertThat(secondResult[componentName]?.toList()).containsExactly(402)
        verify(spyManager, times(1)).getAppWidgetOptions(402)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_afterClearAndOnDeleted_evictsCachedEntryCleanly() {
        val spyManager = spy(AppWidgetManager.getInstance(context))
        val spyRepository = WidgetInstanceRepository(context, spyManager)
        setupBoundWidget(403, MatchingReceiver::class.java.name, widgetId = "evict_instance")
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        // Populate cache via initial lookup
        spyRepository.findAppWidgetIdsForWidgetName(
            "matching_widget",
            widgetIds = setOf("evict_instance"),
        )
        assertThat(WidgetOptionsCache.getCachedWidgetId(403)).isEqualTo("evict_instance")

        // Verify WidgetOptionsCache.clear() evicts cached entry cleanly
        WidgetOptionsCache.clear()
        assertThat(WidgetOptionsCache.getCachedWidgetId(403)).isNull()

        // Re-populate cache via lookup and verify receiver.onDeleted() evicts cached entry cleanly
        clearInvocations(spyManager)
        spyRepository.findAppWidgetIdsForWidgetName(
            "matching_widget",
            widgetIds = setOf("evict_instance"),
        )
        assertThat(WidgetOptionsCache.getCachedWidgetId(403)).isEqualTo("evict_instance")
        verify(spyManager, times(1)).getAppWidgetOptions(403)

        MatchingReceiver().onDeleted(context, intArrayOf(403))
        assertThat(WidgetOptionsCache.getCachedWidgetId(403)).isNull()

        // Subsequent lookup after onDeleted must query AppWidgetManager again
        clearInvocations(spyManager)
        val resultAfterDelete =
            spyRepository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("evict_instance"),
            )
        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(resultAfterDelete[componentName]?.toList()).containsExactly(403)
        verify(spyManager, times(1)).getAppWidgetOptions(403)
    }

    @Test
    fun findReceiverComponentsForWidgetName_withZeroBoundWidgets_returnsMatchingComponent() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val components = repository.findReceiverComponentsForWidgetName("matching_widget")

        val expectedComponent =
            ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(components).containsExactly(expectedComponent)
    }

    @Test
    @Config(sdk = [32])
    fun findReceiverComponentsForWidgetName_preSdk33_returnsMatchingComponent() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val components = repository.findReceiverComponentsForWidgetName("matching_widget")

        val expectedComponent =
            ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(components).containsExactly(expectedComponent)
    }

    @Test
    fun findReceiverComponentsForWidgetName_withUnmatchedWidgetName_returnsEmpty() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val components = repository.findReceiverComponentsForWidgetName("unmatched_widget")

        assertThat(components).isEmpty()
    }

    private fun setupBoundWidget(appWidgetId: Int, receiverName: String, widgetId: String? = null) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val componentName = ComponentName(context.packageName, receiverName)
        val info = AppWidgetProviderInfo().apply { provider = componentName }
        shadowManager.addBoundWidget(appWidgetId, info)

        if (widgetId != null) {
            appWidgetManager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply {
                    putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId)
                },
            )
        }
    }

    private fun registerReceiverInManifest(receiverName: String) {
        val componentName = ComponentName(context.packageName, receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
    }
}
