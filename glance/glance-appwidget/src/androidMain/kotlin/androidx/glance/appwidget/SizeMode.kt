/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalSize

/**
 * Modes describing how the [GlanceAppWidget] should handle size specification.
 */
public sealed interface SizeMode {
    /**
     * The [GlanceAppWidget] provides a single UI.
     *
     * The [LocalSize] will be the minimum size the App Widget can be, as defined in
     * the App Widget provider info (see [android.appwidget.AppWidgetManager.getAppWidgetInfo]).
     */
    public object Single : SizeMode {
        public override fun toString(): String = "SizeMode.Single"
    }

    /**
     * The [GlanceAppWidget] provides a UI for each size the App Widget may be displayed at. The
     * list of sizes is provided by the options bundle (see
     * [android.appwidget.AppWidgetManager.getAppWidgetOptions]).
     *
     * The composable will be called for each size. During that call, the [LocalSize] will be the
     * one for which the UI is generated.
     */
    public object Exact : SizeMode {
        public override fun toString(): String = "SizeMode.Exact"
    }

    /**
     * The [GlanceAppWidget] provides a UI for a fixed set of sizes.
     *
     * On Android 12 and later, the composable will be called once per size provided and the
     * mapping from size to view will be sent to the system. The framework will then decide which
     * view to display based on the current size of the App Widget (see
     * [android.widget.RemoteViews] for details)
     *
     * Before Android 12, the composable will be called for each size at which the app widget may be
     * displayed (like for [Exact]). For each size, the best view will be chosen, which is the
     * largest one that fits in the available space, or the smallest one if none fit.
     *
     * @param sizes List of sizes to use, must not be empty.
     */
    public class Responsive(val sizes: Set<DpSize>) : SizeMode {

        init {
            require(sizes.isNotEmpty()) { "The set of sizes cannot be empty" }
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Responsive

            if (sizes != other.sizes) return false

            return true
        }

        public override fun hashCode(): Int = sizes.hashCode()

        public override fun toString(): String = "SizeMode.Responsive(sizes=$sizes)"
    }
}
