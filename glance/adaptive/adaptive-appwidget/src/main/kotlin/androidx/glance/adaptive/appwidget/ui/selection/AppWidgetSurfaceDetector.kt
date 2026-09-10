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

package androidx.glance.adaptive.appwidget.ui.selection

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.annotation.RestrictTo

/** Utility for detecting the target [AppWidgetGlanceSurface] from host metadata and options. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AppWidgetSurfaceDetector {

    /**
     * Resolves the [AppWidgetGlanceSurface] from platform [AppWidgetProviderInfo.widgetCategory]
     * integer flags.
     *
     * @param category The widget category flag from [AppWidgetProviderInfo].
     * @return Corresponding [AppWidgetGlanceSurface].
     */
    public fun fromHostCategory(category: Int): AppWidgetGlanceSurface {
        return when {
            (category and AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) != 0 &&
                (category and AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD) == 0 -> {
                AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN
            }
            else -> AppWidgetGlanceSurface.MOBILE_HOME_SCREEN
        }
    }

    /**
     * Resolves the [AppWidgetGlanceSurface] from runtime options bundle passed during widget
     * updates.
     *
     * @param options The app widget options bundle passed by the host.
     * @return Resolved [AppWidgetGlanceSurface].
     */
    public fun fromAppWidgetOptions(options: Bundle?): AppWidgetGlanceSurface {
        if (options == null) return AppWidgetGlanceSurface.MOBILE_HOME_SCREEN
        val category =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
        return fromHostCategory(category)
    }

    /**
     * Resolves all applicable target [AppWidgetGlanceSurface]s and corresponding category bitflags
     * for a provider's declared [widgetCategory] bitmask.
     *
     * A provider can declare multiple categories (e.g. both home screen and keyguard). In addition,
     * home screen widgets are offered on the keyguard by default unless explicitly excluded with
     * [AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD]. Keyguard previews are generated only if
     * not_keyguard is false and either keyguard or home screen is supported.
     */
    public fun resolvePreviewCategories(
        widgetCategory: Int
    ): List<Pair<AppWidgetGlanceSurface, Int>> {
        val categories =
            widgetCategory.takeIf { it != 0 } ?: AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
        val results = mutableListOf<Pair<AppWidgetGlanceSurface, Int>>()
        val isNotKeyguard = (categories and AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD) != 0
        val isHomeScreen =
            (categories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0 ||
                categories == AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
        val isKeyguard = (categories and AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) != 0

        if ((categories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0) {
            results.add(
                AppWidgetGlanceSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
            )
        }
        if ((categories and AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX) != 0) {
            results.add(
                AppWidgetGlanceSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
            )
        }
        if (!isNotKeyguard && (isKeyguard || isHomeScreen)) {
            results.add(
                AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )
        }
        if (results.isEmpty()) {
            val fallbackCategory =
                if (isNotKeyguard) {
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
                } else {
                    categories
                }
            results.add(fromHostCategory(categories) to fallbackCategory)
        }
        return results
    }
}
