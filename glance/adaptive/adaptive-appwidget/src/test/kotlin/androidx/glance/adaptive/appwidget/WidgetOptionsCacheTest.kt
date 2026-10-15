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
import android.os.Bundle
import android.util.SizeF
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetOptionsCacheTest {

    @Before
    fun setUp() {
        WidgetOptionsCache.clear()
    }

    @Test
    fun generateWidgetId_returnsValidUniqueUuids() {
        val id1 = WidgetOptionsCache.generateWidgetId()
        val id2 = WidgetOptionsCache.generateWidgetId()

        assertThat(id1).isNotEmpty()
        assertThat(id2).isNotEmpty()
        assertThat(id1).isNotEqualTo(id2)
        assertThat(UUID.fromString(id1)).isNotNull()
        assertThat(UUID.fromString(id2)).isNotNull()
    }

    @Test
    fun put_contains_and_getCachedWidgetId_storesAndRetrievesState() {
        assertThat(WidgetOptionsCache.contains(10)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(10)).isNull()

        val initial =
            WidgetOptionsState(
                widgetId = "widget_10",
                minWidth = 100,
                minHeight = 50,
                maxWidth = 200,
                maxHeight = 100,
                sizes = null,
            )
        val previous = WidgetOptionsCache.put(10, initial)

        assertThat(previous).isNull()
        assertThat(WidgetOptionsCache.contains(10)).isTrue()
        assertThat(WidgetOptionsCache.getCachedWidgetId(10)).isEqualTo("widget_10")

        val updated = initial.copy(widgetId = "widget_10_updated")
        val replaced = WidgetOptionsCache.put(10, updated)

        assertThat(replaced).isEqualTo(initial)
        assertThat(WidgetOptionsCache.getCachedWidgetId(10)).isEqualTo("widget_10_updated")
    }

    @Test
    fun getCachedWidgetId_withNullOrBlankWidgetId_returnsNull() {
        WidgetOptionsCache.put(
            1,
            WidgetOptionsState(
                widgetId = null,
                minWidth = null,
                minHeight = null,
                maxWidth = null,
                maxHeight = null,
                sizes = null,
            ),
        )
        WidgetOptionsCache.put(
            2,
            WidgetOptionsState(
                widgetId = "   ",
                minWidth = null,
                minHeight = null,
                maxWidth = null,
                maxHeight = null,
                sizes = null,
            ),
        )

        assertThat(WidgetOptionsCache.contains(1)).isTrue()
        assertThat(WidgetOptionsCache.getCachedWidgetId(1)).isNull()
        assertThat(WidgetOptionsCache.contains(2)).isTrue()
        assertThat(WidgetOptionsCache.getCachedWidgetId(2)).isNull()
    }

    @Test
    fun remove_evictsSpecifiedAppWidgetIds() {
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(1, bundleWithWidgetId("w1"))
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(2, bundleWithWidgetId("w2"))
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(3, bundleWithWidgetId("w3"))

        WidgetOptionsCache.remove(intArrayOf(1, 3, 999))

        assertThat(WidgetOptionsCache.contains(1)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(1)).isNull()
        assertThat(WidgetOptionsCache.contains(2)).isTrue()
        assertThat(WidgetOptionsCache.getCachedWidgetId(2)).isEqualTo("w2")
        assertThat(WidgetOptionsCache.contains(3)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(3)).isNull()
    }

    @Test
    fun restore_remapsOverlappingAndMismatchedIdsCorrectly() {
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(1, bundleWithWidgetId("w1"))
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(2, bundleWithWidgetId("w2"))
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(3, bundleWithWidgetId("w3"))

        // Overlapping remap: 1 -> 2, 2 -> 3 (with extra trailing element in oldWidgetIds ignored)
        WidgetOptionsCache.restore(
            oldWidgetIds = intArrayOf(1, 2, 99),
            newWidgetIds = intArrayOf(2, 3),
        )

        assertThat(WidgetOptionsCache.contains(1)).isFalse()
        assertThat(WidgetOptionsCache.getCachedWidgetId(2)).isEqualTo("w1")
        assertThat(WidgetOptionsCache.getCachedWidgetId(3)).isEqualTo("w2")
    }

    @Test
    fun cacheWidgetOptionsIfMissing_whenNotCached_usesProvidedWidgetId() {
        val resolved =
            WidgetOptionsCache.cacheWidgetOptionsIfMissing(42, bundleWithWidgetId("custom_id"))

        assertThat(resolved).isEqualTo("custom_id")
        assertThat(WidgetOptionsCache.getCachedWidgetId(42)).isEqualTo("custom_id")
    }

    @Test
    fun cacheWidgetOptionsIfMissing_whenWidgetIdMissingOrBlank_generatesNonBlankUuid() {
        val emptyBundleResolved = WidgetOptionsCache.cacheWidgetOptionsIfMissing(50, Bundle())
        val blankBundleResolved =
            WidgetOptionsCache.cacheWidgetOptionsIfMissing(51, bundleWithWidgetId("   "))

        assertThat(emptyBundleResolved).isNotEmpty()
        assertThat(UUID.fromString(emptyBundleResolved)).isNotNull()
        assertThat(WidgetOptionsCache.getCachedWidgetId(50)).isEqualTo(emptyBundleResolved)

        assertThat(blankBundleResolved).isNotEmpty()
        assertThat(UUID.fromString(blankBundleResolved)).isNotNull()
        assertThat(WidgetOptionsCache.getCachedWidgetId(51)).isEqualTo(blankBundleResolved)
    }

    @Test
    fun cacheWidgetOptionsIfMissing_whenAlreadyCachedWithValidId_preservesExistingEntry() {
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(60, bundleWithWidgetId("initial_id"))

        val resolved =
            WidgetOptionsCache.cacheWidgetOptionsIfMissing(60, bundleWithWidgetId("new_id"))

        assertThat(resolved).isEqualTo("initial_id")
        assertThat(WidgetOptionsCache.getCachedWidgetId(60)).isEqualTo("initial_id")
    }

    @Test
    fun widgetOptionsStateFrom_parsesDimensionsAndSortsSizesWithoutMutatingInput() {
        val inputSizes = arrayListOf(SizeF(200f, 100f), SizeF(100f, 150f), SizeF(100f, 80f))
        val options =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "widget_abc")
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 240)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)
                putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, inputSizes)
            }

        val state = WidgetOptionsState.from(options)

        assertThat(state.widgetId).isEqualTo("widget_abc")
        assertThat(state.minWidth).isEqualTo(80)
        assertThat(state.minHeight).isEqualTo(60)
        assertThat(state.maxWidth).isEqualTo(240)
        assertThat(state.maxHeight).isEqualTo(180)
        assertThat(state.sizes)
            .containsExactly(SizeF(100f, 80f), SizeF(100f, 150f), SizeF(200f, 100f))
            .inOrder()
        // Verify original list inside the Bundle was not mutated in-place
        assertThat(inputSizes)
            .containsExactly(SizeF(200f, 100f), SizeF(100f, 150f), SizeF(100f, 80f))
            .inOrder()
    }

    @Test
    fun widgetOptionsStateFrom_emptyBundle_returnsNullFields() {
        val state = WidgetOptionsState.from(Bundle())

        assertThat(state)
            .isEqualTo(
                WidgetOptionsState(
                    widgetId = null,
                    minWidth = null,
                    minHeight = null,
                    maxWidth = null,
                    maxHeight = null,
                    sizes = null,
                )
            )
    }

    private fun bundleWithWidgetId(widgetId: String): Bundle =
        Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId) }
}
