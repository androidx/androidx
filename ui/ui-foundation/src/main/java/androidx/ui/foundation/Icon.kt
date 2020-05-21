/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.VectorPainter
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp

/**
 * Icon component that draws [asset] using [tint], defaulting to [contentColor].
 *
 * @param asset [VectorAsset] to draw inside this Icon
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [asset]
 */
@Composable
fun Icon(
    asset: VectorAsset,
    modifier: Modifier = Modifier,
    tint: Color = contentColor()
) {
    Icon(
        painter = VectorPainter(asset),
        modifier = modifier,
        tint = tint
    )
}

/**
 * Icon component that draws [asset] using [tint], defaulting to [contentColor].
 *
 * @param asset [ImageAsset] to draw inside this Icon
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [asset]
 */
@Composable
fun Icon(
    asset: ImageAsset,
    modifier: Modifier = Modifier,
    tint: Color = contentColor()
) {
    val painter = remember(asset) { ImagePainter(asset) }
    Icon(
        painter = painter,
        modifier = modifier,
        tint = tint
    )
}

/**
 * Icon component that draws a [painter] using [tint], defaulting to [contentColor].
 *
 * @param painter Painter to draw inside this Icon
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [painter]
 */
@Composable
fun Icon(
    painter: Painter,
    modifier: Modifier = Modifier,
    tint: Color = contentColor()
) {
    // TODO: consider allowing developers to override the intrinsic size, and specify their own
    // size that this icon will be forced to take up.
    // TODO: b/149735981 semantics for content description
    Box(
        modifier.defaultSizeFor(painter).paint(painter, colorFilter = ColorFilter.tint(tint)),
        children = emptyContent()
    )
}

private fun Modifier.defaultSizeFor(painter: Painter) =
    this + if (painter.intrinsicSize == Size.UnspecifiedSize) {
        DefaultIconSizeModifier
    } else {
        Modifier
    }

// Default icon size, for icons with no intrinsic size information
private val DefaultIconSizeModifier = Modifier.preferredSize(24.dp)
