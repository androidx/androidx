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
import androidx.annotation.RestrictTo
import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.util.BundleUtils

/**
 * Information representing an active placed widget instance across host display surfaces.
 *
 * @property widgetName Developer-defined identifier of the widget definition matching the receiver.
 * @property widgetId Target widget instance identifier (developer-defined custom tag or
 *   auto-generated UUID).
 * @property options User configuration options and metadata stored for this widget instance.
 * @property surfacePlacements Map of host surfaces to active placement counts for this instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidgetInstanceInfo
@JvmOverloads
constructor(
    public val widgetName: String,
    public val widgetId: String,
    options: Bundle = Bundle(),
    surfacePlacements: ObjectIntMap<GlanceSurface> = emptyObjectIntMap(),
) {
    public val options: Bundle = Bundle(options)
    public val surfacePlacements: ObjectIntMap<GlanceSurface> =
        if (surfacePlacements is MutableObjectIntMap) {
            MutableObjectIntMap<GlanceSurface>(surfacePlacements.size).apply {
                putAll(surfacePlacements)
            }
        } else {
            surfacePlacements
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WidgetInstanceInfo) return false

        if (widgetName != other.widgetName) return false
        if (widgetId != other.widgetId) return false
        if (!areSurfacePlacementsEqual(surfacePlacements, other.surfacePlacements)) return false
        if (!BundleUtils.areBundlesEqual(options, other.options)) return false

        return true
    }

    private fun areSurfacePlacementsEqual(
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
        result = 31 * result + surfacePlacementsHashCode(surfacePlacements)
        return result
    }

    private fun surfacePlacementsHashCode(map: ObjectIntMap<GlanceSurface>): Int {
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
            "surfacePlacements=$surfacePlacements" +
            ")"
    }
}
