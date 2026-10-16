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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpatialHorizontalFloatingToolbar
import androidx.xr.compose.material3.SpatialVerticalFloatingToolbar
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * A horizontal floating toolbar that uses [SpatialHorizontalFloatingToolbar] when spatial UI is
 * enabled, and [androidx.compose.material3.HorizontalFloatingToolbar] otherwise.
 */
@Composable
internal fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialHorizontalFloatingToolbar(
            expanded = expanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    } else {
        androidx.compose.material3.HorizontalFloatingToolbar(
            expanded = expanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}

/**
 * A horizontal floating toolbar with a FAB that uses [SpatialHorizontalFloatingToolbar] when
 * spatial UI is enabled, and [androidx.compose.material3.HorizontalFloatingToolbar] otherwise.
 */
@Composable
internal fun HorizontalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    floatingActionButtonPosition: FloatingToolbarHorizontalFabPosition =
        FloatingToolbarHorizontalFabPosition.End,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialHorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    } else {
        androidx.compose.material3.HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}

/**
 * A vertical floating toolbar that uses [SpatialVerticalFloatingToolbar] when spatial UI is
 * enabled, and [androidx.compose.material3.VerticalFloatingToolbar] otherwise.
 */
@Composable
internal fun VerticalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    leadingContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialVerticalFloatingToolbar(
            expanded = expanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    } else {
        androidx.compose.material3.VerticalFloatingToolbar(
            expanded = expanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}

/**
 * A vertical floating toolbar with a FAB that uses [SpatialVerticalFloatingToolbar] when spatial UI
 * is enabled, and [androidx.compose.material3.VerticalFloatingToolbar] otherwise.
 */
@Composable
internal fun VerticalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    floatingActionButtonPosition: FloatingToolbarVerticalFabPosition =
        FloatingToolbarVerticalFabPosition.Bottom,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialVerticalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    } else {
        androidx.compose.material3.VerticalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}
