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
import androidx.ui.core.Modifier
import androidx.ui.core.asModifier
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp

// TODO: b/149030271 remove when we have a VectorPainter, so developers can use just the Painter
// overload
/**
 * Icon component that draws [icon] using [tint], defaulting to [contentColor].
 *
 * @param icon Vector icon to draw inside this icon component
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [icon]
 */
@Composable
fun Icon(
    icon: VectorAsset,
    modifier: Modifier = Modifier.None,
    tint: Color = contentColor()
) {
    // TODO: consider allowing developers to override the intrinsic size, and specify their own
    // size that this icon will be forced to take up.
    // TODO: b/149735981 semantics for content description
    Box(modifier + LayoutWidth(icon.defaultWidth) + LayoutHeight(icon.defaultHeight)) {
        DrawVector(vectorImage = icon, tintColor = tint)
    }
}

/**
 * Icon component that draws [icon] using [tint], defaulting to [contentColor].
 *
 * @param icon Painter to draw inside this icon component
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [icon]
 */
@Composable
fun Icon(
    icon: Painter,
    modifier: Modifier = Modifier.None,
    tint: Color = contentColor()
) {
    // TODO: consider allowing developers to override the intrinsic size, and specify their own
    // size that this icon will be forced to take up.
    val iconModifier = icon.asModifier(
        colorFilter = ColorFilter(color = tint, blendMode = BlendMode.srcIn)
    )

    val layoutModifier = if (icon.intrinsicSize == PxSize.UnspecifiedSize) {
        DefaultIconSizeModifier
    } else {
        Modifier.None
    }

    // TODO: b/149735981 semantics for content description
    Box(
        modifier = modifier + layoutModifier + iconModifier,
        children = emptyContent()
    )
}

// Default icon size, for icons with no intrinsic size information
private val DefaultIconSizeModifier = LayoutSize(24.dp)
