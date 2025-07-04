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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailOverride
import androidx.compose.material3.NavigationRailOverrideScope
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
import androidx.xr.compose.material3.XrNavigationRailOverride.NavigationRail
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape

/**
 * <a href="https://m3.material.io/components/navigation-rail/overview" class="external"
 * target="_blank">Material Design bottom navigation rail</a>.
 *
 * XR-specific Navigation rail that shows a Navigation rail in a start-aligned [Orbiter].
 *
 * Navigation rails provide access to primary destinations in apps when using tablet and desktop
 * screens.
 *
 * The navigation rail should be used to display three to seven app destinations and, optionally, a
 * [FloatingActionButton] or a logo header. Each destination is typically represented by an icon and
 * an optional text label.
 *
 * [NavigationRail] should contain multiple [NavigationRailItem]s, each representing a singular
 * destination.
 *
 * See [NavigationRailItem] for configuration specific to each item, and not the overall
 * NavigationRail component.
 *
 * @param modifier the [Modifier] to be applied to this navigation rail
 * @param containerColor the color used for the background of this navigation rail. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param content the content of this navigation rail, typically 3-7 [NavigationRailItem]s
 */
// TODO(kmost): Link to XR-specific NavRail image asset when available
// TODO(kmost): Add a @sample tag and create a new sample project for XR.
@ExperimentalMaterial3XrApi
@Composable
public fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val orbiterProperties = LocalNavigationRailOrbiterProperties.current
    VerticalOrbiter(orbiterProperties) {
        Surface(color = containerColor, contentColor = contentColor, modifier = modifier) {
            Column(
                // XR-changed: Original NavigationRail uses fillMaxHeight() and windowInsets,
                // which do not produce the desired result in XR.
                Modifier.widthIn(min = XrNavigationRailTokens.ContainerWidth)
                    .padding(vertical = XrNavigationRailTokens.VerticalPadding)
                    .selectableGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(XrNavigationRailTokens.VerticalPadding),
                content = content,
            )
        }
    }
    // Header goes inside a separate top-aligned Orbiter without an outline shape, as this is
    // generally a FAB.
    if (header != null) {
        VerticalOrbiter(orbiterProperties.copy(alignment = Alignment.Top)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(XrNavigationRailTokens.VerticalPadding),
                content = header,
            )
        }
    }
}

private object XrNavigationRailTokens {
    /** The [OrbiterOffset] for NavigationRail Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp

    /**
     * Vertical padding between the contents of the [NavigationRail] and its top/bottom, and
     * internally between items.
     *
     * XR-changed value to match desired UX.
     */
    val VerticalPadding: Dp = 20.dp

    val ContainerWidth = 96.0.dp
}

/** [NavigationRailOverride] that uses the XR-specific [NavigationRail]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
internal object XrNavigationRailOverride : NavigationRailOverride {
    @Composable
    override fun NavigationRailOverrideScope.NavigationRail() {
        NavigationRail(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            header = header,
            content = content,
        )
    }
}

/**
 * The default [VerticalOrbiterProperties] used by [NavigationRail] if none is specified in
 * [LocalNavigationRailOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultNavigationRailOrbiterProperties: VerticalOrbiterProperties =
    VerticalOrbiterProperties(
        position = ContentEdge.Vertical.Start,
        offset = XrNavigationRailTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterVertically,
        shape = SpatialRoundedCornerShape(CornerSize(50)),
    )

/** The [VerticalOrbiterProperties] used by [NavigationRail]. */
@ExperimentalMaterial3XrApi
public val LocalNavigationRailOrbiterProperties:
    ProvidableCompositionLocal<VerticalOrbiterProperties> =
    compositionLocalOf {
        DefaultNavigationRailOrbiterProperties
    }
