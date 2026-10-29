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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3XrApi::class)

package androidx.xr.compose.material3.integration.testapp.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpatialTopAppBar
import androidx.xr.compose.material3.SpatialTwoRowsTopAppBar
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * A top app bar that uses [SpatialTopAppBar] when spatial UI is enabled, and
 * [androidx.compose.material3.TopAppBar] otherwise.
 */
@Composable
internal fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        androidx.compose.material3.TopAppBar(
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
 * A two-rows top app bar that uses [SpatialTwoRowsTopAppBar] when spatial UI is enabled, and
 * [androidx.compose.material3.TwoRowsTopAppBar] otherwise.
 */
@Composable
internal fun TwoRowsTopAppBar(
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
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialTwoRowsTopAppBar(
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
    } else {
        androidx.compose.material3.TwoRowsTopAppBar(
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
