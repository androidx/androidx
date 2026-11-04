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
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
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

    /** A second receiver for the widget definition of [MatchingReceiver], which is not allowed. */
    private class DuplicateMatchingReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "matching_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    private class ThrowingConstructorReceiver : GlanceAdaptiveWidgetReceiver() {
        init {
            throw IllegalStateException("Simulated constructor failure")
        }

        override val widgetName: String
            get() = "throwing_widget"

        override suspend fun onUpdate(context: Context) {}
    }

    private class ThrowingWidgetNameReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String
            get() = throw IllegalStateException("Simulated widgetName failure")

        override suspend fun onUpdate(context: Context) {}
    }

    private class NoDefaultConstructorReceiver(override val widgetName: String) :
        GlanceAdaptiveWidgetReceiver() {
        override suspend fun onUpdate(context: Context) {}
    }

    /** A receiver of the same broadcast that is not a GlanceAdaptiveWidgetReceiver. */
    private class PlainReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {}
    }

    private lateinit var context: Context
    private lateinit var repository: WidgetInstanceRepository

    @Before
    fun setUp() {
        WidgetOptionsCache.clear()
        WidgetInstanceRepository.resetForTesting()
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
    fun findAppWidgetIdsForWidgetName_withNoReceivers_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)
        }
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withEmptyWidgetIdsAndUnknownWidgetName_throws() {
        // The widgetName is validated even when nothing is targeted.
        assertThrows(IllegalArgumentException::class.java) {
            repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = emptySet())
        }
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
    fun resolveReceiverComponent_withZeroBoundWidgets_returnsMatchingComponent() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        assertThat(repository.resolveReceiverComponent("matching_widget"))
            .isEqualTo(componentOf(MatchingReceiver::class.java.name))
    }

    @Test
    @Config(sdk = [32])
    fun resolveReceiverComponent_preSdk33_returnsMatchingComponent() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        assertThat(repository.resolveReceiverComponent("matching_widget"))
            .isEqualTo(componentOf(MatchingReceiver::class.java.name))
    }

    @Test
    fun resolveReceiverComponent_withUnmatchedWidgetName_throwsNamingIt() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                repository.resolveReceiverComponent("unmatched_widget")
            }

        assertThat(error).hasMessageThat().contains("unmatched_widget")
    }

    @Test
    fun resolveReceiverComponent_withDuplicateWidgetName_throwsListingReceivers() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        registerReceiverInManifest(DuplicateMatchingReceiver::class.java.name)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                repository.resolveReceiverComponent("matching_widget")
            }

        assertThat(error).hasMessageThat().contains(MatchingReceiver::class.java.name)
        assertThat(error).hasMessageThat().contains(DuplicateMatchingReceiver::class.java.name)
    }

    @Test
    fun resolveReceiverComponent_withDuplicateOfAnotherWidgetName_resolvesRequestedOne() {
        registerReceiverInManifest(OtherReceiver::class.java.name)
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        registerReceiverInManifest(DuplicateMatchingReceiver::class.java.name)

        assertThat(repository.resolveReceiverComponent("other_widget"))
            .isEqualTo(componentOf(OtherReceiver::class.java.name))
    }

    @Test
    fun resolveReceiverComponent_withDisabledReceiver_resolvesIt() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        context.packageManager.setComponentEnabledSetting(
            componentOf(MatchingReceiver::class.java.name),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        assertThat(repository.resolveReceiverComponent("matching_widget"))
            .isEqualTo(componentOf(MatchingReceiver::class.java.name))
    }

    @Test
    fun resolveReceiverComponent_withThrowingConstructor_throwsIllegalStateWithCause() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        registerReceiverInManifest(ThrowingConstructorReceiver::class.java.name)

        val error =
            assertThrows(IllegalStateException::class.java) {
                repository.resolveReceiverComponent("matching_widget")
            }

        assertThat(error).hasMessageThat().contains(ThrowingConstructorReceiver::class.java.name)
        assertThat(error).hasCauseThat().hasMessageThat().isEqualTo("Simulated constructor failure")
    }

    @Test
    fun resolveReceiverComponent_withThrowingWidgetName_throwsIllegalStateWithCause() {
        registerReceiverInManifest(ThrowingWidgetNameReceiver::class.java.name)

        val error =
            assertThrows(IllegalStateException::class.java) {
                repository.resolveReceiverComponent("matching_widget")
            }

        assertThat(error).hasCauseThat().hasMessageThat().isEqualTo("Simulated widgetName failure")
    }

    @Test
    fun resolveReceiverComponent_withoutNoArgConstructor_throwsIllegalState() {
        registerReceiverInManifest(NoDefaultConstructorReceiver::class.java.name)

        assertThrows(IllegalStateException::class.java) {
            repository.resolveReceiverComponent("matching_widget")
        }
    }

    @Test
    fun resolveReceiverComponent_skipsReceiversThatAreNotGlanceAdaptiveWidgetReceivers() {
        registerReceiverInManifest(PlainReceiver::class.java.name)
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        assertThat(repository.resolveReceiverComponent("matching_widget"))
            .isEqualTo(componentOf(MatchingReceiver::class.java.name))
    }

    @Test
    fun resolveReceiverComponent_readsManifestOncePerProcess() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)
        repository.resolveReceiverComponent("matching_widget")

        // Receivers declared later are not seen, even by another repository (every manager
        // creates its own), as the manifest is fixed for the process.
        registerReceiverInManifest(OtherReceiver::class.java.name)

        assertThrows(IllegalArgumentException::class.java) {
            WidgetInstanceRepository(context).resolveReceiverComponent("other_widget")
        }
    }

    private fun componentOf(receiverName: String) = ComponentName(context.packageName, receiverName)

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
