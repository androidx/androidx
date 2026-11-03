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
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.util.BundleUtils

/**
 * Information representing an active placed widget instance across host display surfaces.
 *
 * Each widget definition is identified by [widgetName] (shared across all instances of a
 * `GlanceAdaptiveWidgetReceiver`, e.g. `"weather_forecast"`), while [widgetId] identifies the
 * logical widget instance configuration (either a developer-defined custom tag, e.g.
 * `"bucharest_weather"`, or an auto-generated UUID).
 *
 * Multiple placements on the same surface or across different host surfaces (e.g. two placements on
 * the home screen, or one on the home screen and one on the lock screen) can share the same
 * [widgetId]. [placementsPerSurface] maps each active [GlanceSurface] to the number of placements
 * currently showing this [widgetId] on that surface.
 *
 * @property widgetName Identifier of the widget definition.
 * @property widgetId Identifier for this logical widget instance, which may be shared across
 *   multiple placements.
 * @property options User configuration options and metadata stored for this widget instance.
 * @property placementsPerSurface Map of host [GlanceSurface] to active placement count.
 */
public class WidgetInstanceInfo(
    public val widgetName: String,
    public val widgetId: String,
    options: Bundle = Bundle(),
    placementsPerSurface: ObjectIntMap<GlanceSurface> = emptyObjectIntMap(),
) {
    public val options: Bundle = Bundle(options)
    public val placementsPerSurface: ObjectIntMap<GlanceSurface> =
        if (placementsPerSurface is MutableObjectIntMap) {
            MutableObjectIntMap<GlanceSurface>(placementsPerSurface.size).apply {
                putAll(placementsPerSurface)
            }
        } else {
            placementsPerSurface
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WidgetInstanceInfo) return false

        if (widgetName != other.widgetName) return false
        if (widgetId != other.widgetId) return false
        if (!arePlacementsEqual(placementsPerSurface, other.placementsPerSurface)) return false
        if (!BundleUtils.areBundlesEqual(options, other.options)) return false

        return true
    }

    private fun arePlacementsEqual(
        a: ObjectIntMap<GlanceSurface>,
        b: ObjectIntMap<GlanceSurface>,
    ): Boolean {
        if (a === b) return true
        if (a.size != b.size) return false
        a.forEach { key, value ->
            if (!b.contains(key) || b.getOrDefault(key, Int.MIN_VALUE) != value) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = widgetName.hashCode()
        result = 31 * result + widgetId.hashCode()
        result = 31 * result + BundleUtils.bundleHashCode(options)
        result = 31 * result + placementsHashCode(placementsPerSurface)
        return result
    }

    private fun placementsHashCode(map: ObjectIntMap<GlanceSurface>): Int {
        var hash = 0
        map.forEach { key, value ->
            hash += (key.hashCode() xor value)
        }
        return hash
    }

    override fun toString(): String {
        return "WidgetInstanceInfo(" +
            "widgetName='$widgetName', " +
            "widgetId='$widgetId', " +
            "options=$options, " +
            "placementsPerSurface=$placementsPerSurface" +
            ")"
    }
}
