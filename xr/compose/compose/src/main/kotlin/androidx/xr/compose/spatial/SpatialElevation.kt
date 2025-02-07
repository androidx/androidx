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

package androidx.xr.compose.spatial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * Composable that creates a panel in 3D space when spatialization is enabled.
 *
 * [SpatialElevation] elevates content in-place. It uses the source position and constraints to
 * determine the size and placement of the elevated panel while reserving space for the original
 * element within the layout.
 *
 * In non-spatial environments, the content is rendered normally without elevation.
 *
 * @param spatialElevationLevel the desired elevation level for the panel in spatial environments.
 * @param content the composable content to be displayed within the elevated panel.
 */
@Composable
public fun SpatialElevation(
    spatialElevationLevel: SpatialElevationLevel = SpatialElevationLevel.Level0,
    content: @Composable () -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialElevation(spatialElevationLevel, content)
    } else {
        content()
    }
}

@Composable
private fun LayoutSpatialElevation(
    spatialElevationLevel: SpatialElevationLevel,
    content: @Composable () -> Unit,
) {
    val bufferPadding = 1.dp
    val bufferPaddingPx = with(LocalDensity.current) { bufferPadding.toPx() }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var contentOffset: Offset? by remember { mutableStateOf(null) }

    // Reserve space for the content in the original view.
    with(LocalDensity.current) {
        Spacer(
            Modifier.size(contentSize.width.toDp(), contentSize.height.toDp())
                .onGloballyPositioned { contentOffset = it.positionInRoot() }
        )
    }

    // It is important to use BoxWithConstraints here because the Layout within the ElevatedPanel
    // does
    // not know the constraints of the parent view.
    BoxWithConstraints {
        ElevatedPanel(
            spatialElevationLevel = spatialElevationLevel,
            contentSize = contentSize,
            contentOffset = contentOffset,
        ) {
            // This padding prevents visual aberrations due to stretched panels. The panel is still
            // being stretched in those cases (which will affect input tracking), but it will not be
            // visible to the user.
            // TODO(b/333074376): Remove this padding when the underlying bug is fixed.
            Box(
                Modifier.constrainTo(constraints)
                    .onSizeChanged {
                        check(it.width > bufferPaddingPx * 2 && it.height > bufferPaddingPx * 2) {
                            "Empty composables cannot be placed at a SpatialElevation. You may be trying" +
                                " to use a Popup or Dialog with a SpatialElevation, which is not supported."
                        }
                        contentSize = it
                    }
                    .padding(bufferPadding)
            ) {
                content()
            }
        }
    }
}
