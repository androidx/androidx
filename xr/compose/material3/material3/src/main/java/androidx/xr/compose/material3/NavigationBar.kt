/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarOverride
import androidx.compose.material3.NavigationBarOverrideScope
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.XrNavigationBarOverride.NavigationBar
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape

/**
 * <a href="https://m3.material.io/components/navigation-bar/overview" class="external"
 * target="_blank">Material Design bottom navigation bar</a>.
 *
 * XR-specific Navigation bar that shows a Navigation bar in a bottom-aligned [Orbiter].
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * [NavigationBar] should contain three to five [NavigationBarItem]s, each representing a singular
 * destination.
 *
 * See [NavigationBarItem] for configuration specific to each item, and not the overall
 * [NavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation bar. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param content the content of this navigation bar, typically 3-5 [NavigationBarItem]s
 */
// TODO(brandonjiang): Link to XR-specific NavBar image asset when available
// TODO(brandonjiang): Add a @sample tag and create a new sample project for XR.
@ExperimentalMaterial3XrApi
@Composable
public fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    content: @Composable RowScope.() -> Unit,
) {
    HorizontalOrbiter(LocalNavigationBarOrbiterProperties.current) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            modifier = modifier,
        ) {
            Row(
                // XR-changed: Original NavigationBar uses fillMaxWidth() and windowInsets,
                // which do not produce the desired result in XR.
                modifier =
                    Modifier.width(IntrinsicSize.Min)
                        .heightIn(min = XrNavigationBarTokens.ContainerHeight)
                        .padding(horizontal = XrNavigationBarTokens.HorizontalPadding)
                        .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content,
            )
        }
    }
}

internal object XrNavigationBarTokens {
    /** The [OrbiterOffset] for NavigationBar Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp

    val HorizontalPadding = 8.dp

    val ContainerHeight = 80.0.dp
}

/** [NavigationBarOverride] that uses the XR-specific [NavigationBar]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
internal object XrNavigationBarOverride : NavigationBarOverride {
    @Composable
    override fun NavigationBarOverrideScope.NavigationBar() {
        NavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            content = content,
        )
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by [NavigationBar] if none is specified in
 * [LocalNavigationBarOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultNavigationBarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Bottom,
        offset = XrNavigationBarTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = SpatialRoundedCornerShape(CornerSize(50)),
    )

/** The [HorizontalOrbiterProperties] used by [NavigationBar]. */
@ExperimentalMaterial3XrApi
public val LocalNavigationBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultNavigationBarOrbiterProperties
    }
