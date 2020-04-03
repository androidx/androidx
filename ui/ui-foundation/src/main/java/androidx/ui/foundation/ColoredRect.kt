/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.ui.core.Modifier
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.Dp

/**
 * Component that represents a rectangle painted with the specified [Brush].
 *
 * If width and/or height are not specified, this component will expand
 * to the corresponding max constraints received from the parent
 * if these are finite, or to the min constraints otherwise.
 * Note that even if width and height are specified, these will not be satisfied
 * if the component's incoming layout constraints do not allow that.
 *
 * @param brush brush to paint rect with
 * @param width width of this rect, by default it will match incoming layout constraints
 * @param height height of this rect, by default it will match incoming layout constraints
 */
@Deprecated(
    "Use Box(Modifier.preferredSize(width, height).drawBackground(brush)) instead",
    replaceWith = ReplaceWith(
        "Box(modifier.preferredSize(width, height)\n.drawBackground(brush))",
        "androidx.ui.foundation.preferredSize",
        "androidx.ui.foundation.drawBackground"
    )
)
@Composable
fun ColoredRect(
    brush: Brush,
    modifier: Modifier = Modifier.None,
    width: Dp? = null,
    height: Dp? = null
) {
    val widthModifier =
        if (width != null) Modifier.preferredWidth(width) else Modifier.fillMaxWidth()
    val heightModifier =
        if (height != null) Modifier.preferredHeight(height) else Modifier.fillMaxHeight()
    Box(modifier + widthModifier + heightModifier + Modifier.drawBackground(brush))
}

/**
 * Component that represents a rectangle painted with a solid color.
 *
 * @param color color to paint rect with
 * @param width width of this rect, by default it will match parent's constraints
 * @param height height of this rect, by default it will match parent's constraints
 */
@Deprecated(
    "Use Box(Modifier.preferredSize(width, height).drawBackground(color)) instead",
    replaceWith = ReplaceWith(
        "Box(modifier.preferredSize(width, height)\n.drawBackground(color))",
        "androidx.ui.foundation.preferredSize",
        "androidx.ui.foundation.drawBackground"
    )
)
@Composable
fun ColoredRect(
    color: Color,
    modifier: Modifier = Modifier.None,
    width: Dp? = null,
    height: Dp? = null
) {
    val widthModifier =
        if (width != null) Modifier.preferredWidth(width) else Modifier.fillMaxWidth()
    val heightModifier =
        if (height != null) Modifier.preferredHeight(height) else Modifier.fillMaxHeight()
    Box(modifier + widthModifier + heightModifier + Modifier.drawBackground(color))
}
