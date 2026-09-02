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

package androidx.glance.adaptive.core.ui.selection

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.annotation.RestrictTo

/** Standard surfaces on which Glance Adaptive widgets can be placed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class GlanceSurface {
    /** Mobile Phone home screen launcher surface. */
    MOBILE_HOME_SCREEN,

    /** Mobile Phone lockscreen surface or Keyguard glanceable space. */
    MOBILE_LOCK_SCREEN,

    /** Large screen tablet home launcher. */
    TABLET_HOME_SCREEN,

    /** Wear OS active Tile carousel. */
    WEAR_TILE,

    /** Wear OS watch face complication slot. */
    WEAR_COMPLICATION,

    /** Spatial XR / Augmented Reality glasses display surface. */
    XR_GLASSES,
}

/** Utility for detecting the target [GlanceSurface] from host metadata and options. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object SurfaceDetector {

    /**
     * Resolves the [GlanceSurface] from platform [AppWidgetProviderInfo.widgetCategory] integer
     * flags.
     *
     * @param category The widget category flag from [AppWidgetProviderInfo].
     * @return Corresponding [GlanceSurface].
     */
    public fun fromHostCategory(category: Int): GlanceSurface {
        return when {
            (category and AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) != 0 -> {
                GlanceSurface.MOBILE_LOCK_SCREEN
            }
            else -> GlanceSurface.MOBILE_HOME_SCREEN
        }
    }

    /**
     * Resolves the [GlanceSurface] from runtime options bundle passed during widget updates.
     *
     * @param options The app widget options bundle passed by the host.
     * @return Resolved [GlanceSurface].
     */
    public fun fromAppWidgetOptions(options: Bundle?): GlanceSurface {
        if (options == null) return GlanceSurface.MOBILE_HOME_SCREEN
        val category =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
        return fromHostCategory(category)
    }
}

/**
 * Host placement constraints capturing the physical container size and target display surface.
 *
 * @param dimensions Bounding size in DP.
 * @param surface Host target surface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostConstraints(
    public val dimensions: Dimensions,
    public val surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostConstraints) return false
        return dimensions == other.dimensions && surface == other.surface
    }

    override fun hashCode(): Int = 31 * dimensions.hashCode() + surface.hashCode()

    override fun toString(): String = "HostConstraints(dimensions=$dimensions, surface=$surface)"
}
