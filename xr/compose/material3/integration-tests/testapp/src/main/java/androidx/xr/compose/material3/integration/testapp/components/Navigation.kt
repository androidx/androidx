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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3XrApi::class)

package androidx.xr.compose.material3.integration.testapp.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpatialNavigationBar
import androidx.xr.compose.material3.SpatialNavigationRail
import androidx.xr.compose.material3.SpatialShortNavigationBar
import androidx.xr.compose.material3.SpatialWideNavigationRail
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * A navigation bar that uses [SpatialNavigationBar] when spatial UI is enabled, and
 * [androidx.compose.material3.NavigationBar] otherwise.
 */
@Composable
internal fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialNavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            content = content,
        )
    } else {
        androidx.compose.material3.NavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            content = content,
        )
    }
}

/**
 * A short navigation bar that uses [SpatialShortNavigationBar] when spatial UI is enabled, and
 * [androidx.compose.material3.ShortNavigationBar] otherwise.
 */
@Composable
internal fun ShortNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    content: @Composable () -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialShortNavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
    } else {
        androidx.compose.material3.ShortNavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
    }
}

/**
 * A navigation rail that uses [SpatialNavigationRail] when spatial UI is enabled, and
 * [androidx.compose.material3.NavigationRail] otherwise.
 */
@Composable
internal fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialNavigationRail(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            header = header,
            content = content,
        )
    } else {
        androidx.compose.material3.NavigationRail(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            header = header,
            content = content,
        )
    }
}

/**
 * A wide navigation rail that uses [SpatialWideNavigationRail] when spatial UI is enabled, and
 * [androidx.compose.material3.WideNavigationRail] otherwise.
 */
@Composable
internal fun WideNavigationRail(
    modifier: Modifier = Modifier,
    state: WideNavigationRailState = rememberWideNavigationRailState(),
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialWideNavigationRail(
            modifier = modifier,
            state = state,
            colors = colors,
            header = header,
            content = content,
        )
    } else {
        androidx.compose.material3.WideNavigationRail(
            modifier = modifier,
            state = state,
            colors = colors,
            header = header,
            content = content,
        )
    }
}
