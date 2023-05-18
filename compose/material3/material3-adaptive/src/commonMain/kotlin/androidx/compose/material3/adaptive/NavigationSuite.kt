/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId

/**
 * The Navigation Suite wraps the provided content and places the adequate provided navigation
 * component on the screen according to the current NavigationLayoutType.
 *
 * TODO: Add the navigationLayoutType param
 * @param modifier the [Modifier] to be applied to the navigation suite
 * @param navigationComponent the navigation component to be displayed
 * @param containerColor the color used for the background of the navigation suite. Use
 * [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside the navigation suite. Defaults to
 * either the matching content color for [containerColor], or to the current LocalContentColor if
 * [containerColor] is not a color from the theme.
 * @param content the content of your screen
 *
 * TODO: Remove "internal".
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun NavigationSuite(
    modifier: Modifier = Modifier,
    navigationComponent: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        NavigationSuiteLayout(
            navigationComponent = navigationComponent,
            content = content
        )
    }
}

/**
 * Layout for a [NavigationSuite]'s content.
 *
 * TODO: Add the navigationLayoutType param.
 * @param navigationComponent the navigation component of the [NavigationSuite]
 * @param content the main body of the [NavigationSuite]
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun NavigationSuiteLayout(
    navigationComponent: @Composable () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Layout(
        content = {
            Box(modifier = Modifier.layoutId("navigation")) { navigationComponent() }
            Box(modifier = Modifier.layoutId("content")) { content() }
        }
    ) { _, constraints ->
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth

        layout(layoutWidth, layoutHeight) {
            // TODO: Add the placement logic based on the NavigationLayoutType.
        }
    }
}

/**
 * A feature that describes characteristics of the navigation component of the [NavigationSuite].
 *
 * TODO: Remove "internal".
 */
@ExperimentalMaterial3AdaptiveApi
internal interface NavigationSuiteFeature {
    /**
     * Represents the orientation of the navigation component of the [NavigationSuite].
     */
    enum class Orientation {
        /**
         * The navigation component of the [NavigationSuite] is horizontal, such as the
         * Navigation Bar.
         */
        Horizontal,
        /**
         * The navigation component of the [NavigationSuite] is vertical, such as the
         * Navigation Rail or Navigation Drawer.
         */
        Vertical
    }
}

/**
 * Class that describes the different navigation layout types of the [NavigationSuite].
 *
 * The [NavigationLayoutType] informs the [NavigationSuite] of what navigation component to expect
 * and how to properly place it on the screen in relation to the [NavigationSuite]'s content.
 *
 * @param description the description of the [NavigationLayoutType]
 * @param alignment the [Alignment] of the [NavigationLayoutType] that helps inform how the
 * navigation component will be positioned on the screen. The current supported alignments are:
 * [Alignment.TopStart], [Alignment.BottomStart], and [Alignment.TopEnd]
 * @param orientation the [NavigationSuiteFeature.Orientation] of the [NavigationLayoutType] that
 * helps inform how the navigation component will be positioned on the screen in relation to the
 * content
 *
 * TODO: Make class open instead of internal.
 */
@ExperimentalMaterial3AdaptiveApi
internal class NavigationLayoutType constructor(
    private val description: String,
    // TODO: Make this an internal open val.
    internal val alignment: Alignment,
    // TODO: Make this an internal open val.
    internal val orientation: NavigationSuiteFeature.Orientation
) {
    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a
         * [androidx.compose.material3.NavigationBar] and properly place it on the screen.
         *
         * @see androidx.compose.material3.NavigationBar
         */
        @JvmField
        val NavigationBar =
            NavigationLayoutType(
                description = "NavigationBar",
                alignment = Alignment.BottomStart,
                orientation = NavigationSuiteFeature.Orientation.Horizontal,
            )
        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a
         * [androidx.compose.material3.NavigationRail] and properly place it on the screen.
         *
         * @see androidx.compose.material3.NavigationRail
         */
        @JvmField
        val NavigationRail =
            NavigationLayoutType(
                description = "NavigationRail",
                alignment = Alignment.TopStart,
                orientation = NavigationSuiteFeature.Orientation.Vertical,
            )
        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a
         * [androidx.compose.material3.PermanentDrawerSheet] and properly place it on the screen.
         *
         * @see androidx.compose.material3.PermanentDrawerSheet
         */
        @JvmField
        val NavigationDrawer =
            NavigationLayoutType(
                description = "NavigationDrawer",
                alignment = Alignment.TopStart,
                orientation = NavigationSuiteFeature.Orientation.Vertical,
            )
    }
}