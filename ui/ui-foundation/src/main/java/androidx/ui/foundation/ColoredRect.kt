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
import androidx.ui.layout.Container
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
 * @sample androidx.ui.foundation.samples.ColoredRectBrushSample
 *
 * @param brush brush to paint rect with
 * @param width width of this rect, by default it will match incoming layout constraints
 * @param height height of this rect, by default it will match incoming layout constraints
 */
@Composable
fun ColoredRect(
    brush: Brush,
    modifier: Modifier = Modifier.None,
    width: Dp? = null,
    height: Dp? = null
) {
    Container(
        modifier = modifier + DrawBackground(brush), width = width, height = height, expanded = true
    ) {}
}

/**
 * Component that represents a rectangle painted with a solid color.
 *
 * @sample androidx.ui.foundation.samples.ColoredRectColorSample
 *
 * @param color color to paint rect with
 * @param width width of this rect, by default it will match parent's constraints
 * @param height height of this rect, by default it will match parent's constraints
 */
@Composable
fun ColoredRect(
    color: Color,
    modifier: Modifier = Modifier.None,
    width: Dp? = null,
    height: Dp? = null
) {
    Container(
        modifier = modifier + DrawBackground(color), width = width, height = height, expanded = true
    ) {}
}
