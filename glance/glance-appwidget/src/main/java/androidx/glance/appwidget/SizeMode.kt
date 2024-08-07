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
import androidx.glance.appwidget.SizeMode.Exact
import androidx.glance.appwidget.SizeMode.Responsive

/**
 * Modes describing how the [GlanceAppWidget] should handle size specification.
 *
 * Note: Size modes that support multiple sizes ([Exact], [Responsive]) run the composable passed to
 * to [provideContent] concurrently for each size. This has a number of important implications.
 * Since an instance of the content is running for each size, all of the State objects in the
 * content will have an instance for each size.
 *
 * For example, in Exact mode, let's say the AppWidgetHost asks for 2 sizes, portrait and landscape.
 * In the code below, there will end up being two instances of the `count` variable, one for each
 * size:
 * ```
 * provideContent {
 *    var count by remember { mutableStateOf(0) }
 *    Button(
 *        text = "Increment count: $count",
 *        onClick = { count++ }
 *    )
 * }
 * ```
 *
 * If the button is clicked while the widget is displayed in portrait size, the `count` variable is
 * updated for both sizes. This is so that, if the device orientation changes and the host displays
 * the landscape layout, it will be consistent with the state in portrait. This works because
 * lambdas that are at the same place in the composition will be mapped to the same default key in
 * all sizes. So triggering one will trigger the corresponding lambdas in other sizes.
 *
 * This means that lambdas will be called multiple times when they are triggered, which can have
 * unexpected effects if state is not handled correctly. In order to prevent some external action
 * from being triggered multiple times at once, you should conflate requests so that only one is
 * active at a time, e.g. using MutableStateFlow.
 *
 * To prevent this behavior, you can use the [androidx.compose.runtime.key] composable to set
 * different default lambda keys for each size, so that they do not trigger each other:
 * ```
 * provideContent {
 *    key(LocalSize.current) {
 *        var count by remember { mutableStateOf(0) }
 *        Button(
 *            text = "Increment count: $count",
 *            onClick = { count++ }
 *        )
 *    }
 * }
 * ```
 *
 * To disable this behavior on a per-lambda basis, use [androidx.glance.action.action] to set a
 * custom lambda key based on the current size:
 * ```
 * provideContent {
 *    var count by remember { mutableStateOf(0) }
 *    Button(
 *        text = "Increment count: $count",
 *        onClick = action("incrementCount-${LocalSize.current}") { count++ }
 *    )
 * }
 * ```
 *
 * In both of the last two examples, when the button is clicked, only the lambda for the currently
 * visible size will be triggered.
 *
 * Note that the above does not work for effects, which will always be triggered for each size. Use
 * effects to update state variables in the composition, otherwise be sure to handle any duplicate
 * triggering that may occur.
 */
sealed interface SizeMode {
    /**
     * The [GlanceAppWidget] provides a single UI.
     *
     * The [LocalSize] will be the minimum size the App Widget can be, as defined in the App Widget
     * provider info (see [android.appwidget.AppWidgetManager.getAppWidgetInfo]).
     */
    object Single : SizeMode, PreviewSizeMode {
        override fun toString(): String = "SizeMode.Single"
    }

    /**
     * The [GlanceAppWidget] provides a UI for each size the App Widget may be displayed at. The
     * list of sizes is provided by the options bundle (see
     * [android.appwidget.AppWidgetManager.getAppWidgetOptions]).
     *
     * The composable will be run concurrently for each size. In each sub-composition, the
     * [LocalSize] will be the one for which the UI is generated. See the note in [SizeMode] for
     * more info.
     */
    object Exact : SizeMode {
        override fun toString(): String = "SizeMode.Exact"
    }

    /**
     * The [GlanceAppWidget] provides a UI for a fixed set of sizes.
     *
     * On Android 12 and later, the composable will be run concurrently for each size provided and
     * the mapping from size to view will be sent to the system. The framework will then decide
     * which view to display based on the current size of the App Widget (see
     * [android.widget.RemoteViews] for details)
     *
     * Before Android 12, the composable will be run concurrently for each size at which the app
     * widget may be displayed (like for [Exact]). For each size, the best view will be chosen,
     * which is the largest one that fits in the available space, or the smallest one if none fit.
     *
     * See the note in [SizeMode] for more info about handling concurrent runs for multiple sizes.
     *
     * @param sizes List of sizes to use, must not be empty.
     */
    class Responsive(val sizes: Set<DpSize>) : SizeMode, PreviewSizeMode {

        init {
            require(sizes.isNotEmpty()) { "The set of sizes cannot be empty" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Responsive

            if (sizes != other.sizes) return false

            return true
        }

        override fun hashCode(): Int = sizes.hashCode()

        override fun toString(): String = "SizeMode.Responsive(sizes=$sizes)"
    }
}

/** This marker interface determines which [SizeMode]s can be used for preview compositions. */
sealed interface PreviewSizeMode : SizeMode
