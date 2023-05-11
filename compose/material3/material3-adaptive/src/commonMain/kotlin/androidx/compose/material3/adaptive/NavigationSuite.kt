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

/* TODO: Add NavigationLayoutType class and NavigationSuiteFeature. */