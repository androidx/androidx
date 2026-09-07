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

package androidx.glance.adaptive.core

import android.os.Bundle
import androidx.collection.MutableObjectIntMap
import androidx.collection.objectIntMapOf
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetInstanceInfoTest {

    @Test
    fun defaultValues_areExpected() {
        val info = WidgetInstanceInfo(widgetName = "test_widget", widgetId = "instance_123")

        assertThat(info.widgetName).isEqualTo("test_widget")
        assertThat(info.widgetId).isEqualTo("instance_123")
        assertThat(info.options.isEmpty).isTrue()
        assertThat(info.surfacePlacements.isEmpty()).isTrue()
        assertThat(info.surfacePlacements.size).isEqualTo(0)
    }

    @Test
    fun customValues_areRetained() {
        val bundle =
            Bundle().apply {
                putString("city", "Bucharest")
                putInt("temp", 25)
            }
        val placements =
            objectIntMapOf(GlanceSurface.MOBILE_HOME_SCREEN, 2, GlanceSurface.MOBILE_LOCK_SCREEN, 1)
        val info =
            WidgetInstanceInfo(
                widgetName = "weather_widget",
                widgetId = "weather_bucharest",
                options = bundle,
                surfacePlacements = placements,
            )

        assertThat(info.widgetName).isEqualTo("weather_widget")
        assertThat(info.widgetId).isEqualTo("weather_bucharest")
        assertThat(info.options.getString("city")).isEqualTo("Bucharest")
        assertThat(info.options.getInt("temp")).isEqualTo(25)
        assertThat(info.surfacePlacements).isEqualTo(placements)
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val bundle1 =
            Bundle().apply {
                putString("key", "val")
                putIntArray("arr", intArrayOf(1, 2, 3))
                putShortArray("shortArr", shortArrayOf(10, 20))
                putCharArray("charArr", charArrayOf('a', 'b'))
            }
        val bundle2 =
            Bundle().apply {
                putString("key", "val")
                putIntArray("arr", intArrayOf(1, 2, 3))
                putShortArray("shortArr", shortArrayOf(10, 20))
                putCharArray("charArr", charArrayOf('a', 'b'))
            }
        val placements1 = objectIntMapOf(GlanceSurface.MOBILE_HOME_SCREEN, 1)
        val placements2 = objectIntMapOf(GlanceSurface.MOBILE_HOME_SCREEN, 1)

        val info1 = WidgetInstanceInfo("name", "id", bundle1, placements1)
        val info2 = WidgetInstanceInfo("name", "id", bundle2, placements2)

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())

        // Different widgetName
        val diffName = WidgetInstanceInfo("other_name", "id", bundle1, placements1)
        assertThat(info1).isNotEqualTo(diffName)

        // Different widgetId
        val diffId = WidgetInstanceInfo("name", "other_id", bundle1, placements1)
        assertThat(info1).isNotEqualTo(diffId)

        // Different options
        val diffBundle = Bundle().apply { putString("key", "different") }
        val diffOptions = WidgetInstanceInfo("name", "id", diffBundle, placements1)
        assertThat(info1).isNotEqualTo(diffOptions)

        val diffShortBundle =
            Bundle().apply {
                putString("key", "val")
                putIntArray("arr", intArrayOf(1, 2, 3))
                putShortArray("shortArr", shortArrayOf(99, 99))
                putCharArray("charArr", charArrayOf('a', 'b'))
            }
        assertThat(info1)
            .isNotEqualTo(WidgetInstanceInfo("name", "id", diffShortBundle, placements1))

        val diffCharBundle =
            Bundle().apply {
                putString("key", "val")
                putIntArray("arr", intArrayOf(1, 2, 3))
                putShortArray("shortArr", shortArrayOf(10, 20))
                putCharArray("charArr", charArrayOf('x', 'y'))
            }
        assertThat(info1)
            .isNotEqualTo(WidgetInstanceInfo("name", "id", diffCharBundle, placements1))

        // Different surfacePlacements
        val diffPlacements =
            WidgetInstanceInfo(
                "name",
                "id",
                bundle1,
                objectIntMapOf(GlanceSurface.MOBILE_LOCK_SCREEN, 1),
            )
        assertThat(info1).isNotEqualTo(diffPlacements)
    }

    @Test
    fun equalsAndHashCode_handlesNestedBundlesAndLists() {
        val inner1 = Bundle().apply { putString("inner", "value") }
        val inner2 = Bundle().apply { putString("inner", "value") }
        val bundle1 =
            Bundle().apply {
                putBundle("nested", inner1)
                putParcelableArrayList("list", arrayListOf(inner1))
            }
        val bundle2 =
            Bundle().apply {
                putBundle("nested", inner2)
                putParcelableArrayList("list", arrayListOf(inner2))
            }

        val info1 = WidgetInstanceInfo("name", "id", bundle1)
        val info2 = WidgetInstanceInfo("name", "id", bundle2)

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())

        val diffInner = Bundle().apply { putString("inner", "different") }
        val diffBundle =
            Bundle().apply {
                putBundle("nested", inner1)
                putParcelableArrayList("list", arrayListOf(diffInner))
            }
        assertThat(info1).isNotEqualTo(WidgetInstanceInfo("name", "id", diffBundle))
    }

    @Test
    fun equalsAndHashCode_handlesNestedBundlesInArrays() {
        val inner1 = Bundle().apply { putString("inner", "value") }
        val inner2 = Bundle().apply { putString("inner", "value") }
        val bundle1 = Bundle().apply { putParcelableArray("array", arrayOf(inner1)) }
        val bundle2 = Bundle().apply { putParcelableArray("array", arrayOf(inner2)) }

        val info1 = WidgetInstanceInfo("name", "id", bundle1)
        val info2 = WidgetInstanceInfo("name", "id", bundle2)

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())

        val diffInner = Bundle().apply { putString("inner", "different") }
        val diffBundle = Bundle().apply { putParcelableArray("array", arrayOf(diffInner)) }
        assertThat(info1).isNotEqualTo(WidgetInstanceInfo("name", "id", diffBundle))
    }

    @Test
    fun defensiveCopy_optionsBundleIsNotAffectedByExternalMutation() {
        val originalBundle = Bundle().apply { putString("key", "initial") }
        val info =
            WidgetInstanceInfo(widgetName = "widget", widgetId = "id", options = originalBundle)

        originalBundle.putString("key", "mutated")
        assertThat(info.options.getString("key")).isEqualTo("initial")
    }

    @Test
    fun defensiveCopy_mutablePlacementsMapIsNotAffectedByExternalMutation() {
        val mutablePlacements =
            MutableObjectIntMap<GlanceSurface>().apply { put(GlanceSurface.MOBILE_HOME_SCREEN, 1) }
        val info =
            WidgetInstanceInfo(
                widgetName = "widget",
                widgetId = "id",
                surfacePlacements = mutablePlacements,
            )

        mutablePlacements.put(GlanceSurface.MOBILE_HOME_SCREEN, 99)
        assertThat(info.surfacePlacements.getOrDefault(GlanceSurface.MOBILE_HOME_SCREEN, 0))
            .isEqualTo(1)
    }

    @Test
    fun toString_containsClassAndFieldData() {
        val info =
            WidgetInstanceInfo(
                widgetName = "sample_widget",
                widgetId = "sample_id",
                surfacePlacements = objectIntMapOf(GlanceSurface.MOBILE_HOME_SCREEN, 3),
            )
        val str = info.toString()
        assertThat(str).contains("WidgetInstanceInfo")
        assertThat(str).contains("widgetName='sample_widget'")
        assertThat(str).contains("widgetId='sample_id'")
        assertThat(str).contains("MOBILE_HOME_SCREEN=3")
    }
}
