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

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import java.util.Collections

/** Standard platform AppWidget surfaces on which Glance Adaptive widgets can be placed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppWidgetSurface private constructor(tag: String) : GlanceSurface(tag) {
    public companion object {
        /** Mobile Phone home screen launcher surface. */
        @JvmField public val MOBILE_HOME_SCREEN: AppWidgetSurface = AppWidgetSurface("home_screen")

        /** Mobile Phone lockscreen surface or Keyguard glanceable surface. */
        @JvmField public val MOBILE_LOCK_SCREEN: AppWidgetSurface = AppWidgetSurface("keyguard")

        internal val ALL: List<AppWidgetSurface> =
            Collections.unmodifiableList(listOf(MOBILE_HOME_SCREEN, MOBILE_LOCK_SCREEN))
    }
}
