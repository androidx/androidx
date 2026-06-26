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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalWideNavigationRailOverride
import androidx.compose.material3.ModalWideNavigationRailOverrideScope
import androidx.compose.material3.Surface
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailOverride
import androidx.compose.material3.WideNavigationRailOverrideScope
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape

/**
 * XR-specific Material design wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The wide navigation rail should be used to display multiple [WideNavigationRailItem]s, each
 * representing a singular app destination, and, optionally, a header containing a menu button, a
 * [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and a
 * text label.
 *
 * The [WideNavigationRail] is collapsed by default, but it also supports being expanded via a
 * [WideNavigationRailState]. When collapsed, the rail should display three to seven navigation
 * items.
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [WideNavigationRail] component.
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param state the [WideNavigationRailState] of this wide navigation rail
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param content the content of this wide navigation rail, typically [WideNavigationRailItem]s
 */
// TODO(brandonjiang): Link to XR-specific WideNavRail image asset when available
// TODO(brandonjiang): Add a @sample tag and create a new sample project for XR.
@ExperimentalMaterial3ExpressiveApi
@ExperimentalMaterial3XrApi
@Composable
public fun WideNavigationRail(
    modifier: Modifier = Modifier,
    state: WideNavigationRailState = rememberWideNavigationRailState(),
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val orbiterProperties =
        LocalWideNavigationRailOrbiterProperties.current.copy(
            shape = SpatialRoundedCornerShape(CornerSize(percent = 0))
        )
    VerticalOrbiter(orbiterProperties) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(XrNavigationRailTokens.VerticalPadding),
        ) {
            header?.let { it() }
            Surface(
                shape = CircleShape,
                color = colors.containerColor,
                contentColor = colors.contentColor,
                modifier = modifier,
            ) {
                Column(
                    modifier =
                        Modifier.padding(vertical = XrNavigationRailTokens.VerticalPadding)
                            .getContainerWidth(state)
                            .selectableGroup(),
                    verticalArrangement =
                        Arrangement.spacedBy(XrNavigationRailTokens.VerticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    content()
                }
            }
        }
    }
}

private fun Modifier.getContainerWidth(state: WideNavigationRailState): Modifier {
    return if (state.targetValue == WideNavigationRailValue.Expanded) {
        this.widthIn(
            min = XrWideNavigationRailTokens.ExpandedContainerMinWidth,
            max = XrWideNavigationRailTokens.ExpandedContainerMaxWidth,
        )
    } else {
        this.width(XrNavigationRailTokens.ContainerWidth)
    }
}

private object XrWideNavigationRailTokens {
    val ExpandedContainerMinWidth = 220.0.dp
    val ExpandedContainerMaxWidth = 360.0.dp
}

/** [WideNavigationRailOverride] that uses the XR-specific [WideNavigationRail]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class, ExperimentalMaterial3ExpressiveApi::class)
internal object XrWideNavigationRailOverride : WideNavigationRailOverride {
    @Composable
    override fun WideNavigationRailOverrideScope.WideNavigationRail() {
        WideNavigationRail(
            modifier = modifier,
            state = state,
            colors = colors,
            header = header,
            content = content,
        )
    }
}

/** [ModalWideNavigationRailOverride] that uses the XR-specific [WideNavigationRail]. */
// TODO(b/407769444): implement modal version of WideNavRail
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class, ExperimentalMaterial3ExpressiveApi::class)
internal object XrModalWideNavigationRailOverride : ModalWideNavigationRailOverride {
    @Composable
    override fun ModalWideNavigationRailOverrideScope.ModalWideNavigationRail() {
        WideNavigationRail(
            modifier = modifier,
            state = state,
            colors = colors,
            header = header,
            content = content,
        )
    }
}

/**
 * The default [VerticalOrbiterProperties] used by [WideNavigationRail] if none is specified in
 * [LocalWideNavigationRailOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultWideNavigationRailOrbiterProperties: VerticalOrbiterProperties =
    VerticalOrbiterProperties(
        position = ContentEdge.Vertical.Start,
        offset = XrNavigationRailTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterVertically,
        shape = SpatialRoundedCornerShape(CornerSize(50)),
    )

/** The [VerticalOrbiterProperties] used by [WideNavigationRail]. */
@ExperimentalMaterial3XrApi
public val LocalWideNavigationRailOrbiterProperties:
    ProvidableCompositionLocal<VerticalOrbiterProperties> =
    compositionLocalOf {
        DefaultWideNavigationRailOrbiterProperties
    }
