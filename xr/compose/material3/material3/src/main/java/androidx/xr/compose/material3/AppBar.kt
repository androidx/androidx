/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.material3

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.tokens.XrTokens
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.OrbiterEdgeOffsetType

/**
 * XR-specific SingleRowTopAppBar that displays a single-row top app bar inside a top-aligned
 * `Orbiter`.
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon optional navigation icon to be displayed at the start of the top app bar
 * @param actions optional actions to be displayed at the end of the top app bar
 * @param windowInsets a window insets of the top app bar
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 * @param scrollBehavior a [TopAppBarScrollBehavior]
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3XrApi
@Composable
public fun SingleRowTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    HorizontalOrbiter(LocalSingleRowTopAppBarOrbiterProperties.current) {
        TopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by XR `TopAppBar` if none is specified in
 * [LocalSingleRowTopAppBarOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultSingleRowTopAppBarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Top,
        offset = XrSingleRowTopAppBarTokens.OrbiterOffset,
        offsetType = OrbiterEdgeOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = XrTokens.ContainerShape,
    )

/** The [HorizontalOrbiterProperties] used by XR [TopAppBar]. */
@ExperimentalMaterial3XrApi
public val LocalSingleRowTopAppBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultSingleRowTopAppBarOrbiterProperties
    }

/**
 * XR-specific TwoRowsTopAppBar that displays a two-rows top app bar inside a top-aligned `Orbiter`.
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param subtitle optional subtitle to be displayed in the top app bar
 * @param navigationIcon optional navigation icon to be displayed at the start of the top app bar
 * @param actions optional actions to be displayed at the end of the top app bar
 * @param titleHorizontalAlignment the horizontal alignment of the title
 * @param collapsedHeight the collapsed height of the top app bar
 * @param expandedHeight the expanded height of the top app bar
 * @param windowInsets a window insets of the top app bar
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 * @param scrollBehavior a [TopAppBarScrollBehavior]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3XrApi
@Composable
public fun TwoRowsTopAppBar(
    title: @Composable (expanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable (expanded: Boolean) -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = Dp.Unspecified,
    expandedHeight: Dp = Dp.Unspecified,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    HorizontalOrbiter(LocalTwoRowsTopAppBarOrbiterProperties.current) {
        TwoRowsTopAppBar(
            title = title,
            modifier = modifier,
            subtitle = subtitle,
            navigationIcon = navigationIcon,
            actions = actions,
            titleHorizontalAlignment = titleHorizontalAlignment,
            collapsedHeight = collapsedHeight,
            expandedHeight = expandedHeight,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by XR [TopAppBar] if none is specified in
 * [LocalTwoRowsTopAppBarOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultTwoRowsTopAppBarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Top,
        offset = XrTwoRowsTopAppBarTokens.OrbiterOffset,
        offsetType = OrbiterEdgeOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = XrTokens.ContainerShape,
    )

/** The [HorizontalOrbiterProperties] used by XR [TopAppBar]. */
@ExperimentalMaterial3XrApi
public val LocalTwoRowsTopAppBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultTwoRowsTopAppBarOrbiterProperties
    }

private object XrSingleRowTopAppBarTokens {
    /** The [OrbiterOffset] for SingleRowTopAppBar Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp
}

private object XrTwoRowsTopAppBarTokens {
    /** The [OrbiterOffset] for TwoRowsTopAppBar Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp
}
