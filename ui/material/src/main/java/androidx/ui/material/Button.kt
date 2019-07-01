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

package androidx.ui.material

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.border.Border
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.painting.TextStyle

/**
 * [Button] with flexible user interface. You can provide any content you want as a
 * [children] composable.
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 * You can specify a [shape] of the surface, it's background [color] and an [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 * To modify these default style values use [CurrentTextStyleProvider].
 *
 * Example:
 *     Button(onClick = { ... }) {
 *         Padding(padding = EdgeInsets(16.dp)) {
 *             Text(text=TextSpan(text="CUSTOM BUTTON"))
 *         }
 *     }
 *
 * @see Button overload for the default Material Design implementation of [Button] with text.
 *
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] value from the theme.
 * @param color The background color. [MaterialColors.primary] is used when null
 *  is provided. Provide [Color.Transparent] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
@Composable
fun Button(
    onClick: (() -> Unit)? = null,
    shape: Shape = +themeShape { button },
    color: Color = +themeColor { primary },
    border: Border? = null,
    elevation: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    val textStyle = +themeTextStyle { button }
    Surface(shape = shape, color = color, border = border, elevation = elevation) {
        CurrentTextStyleProvider(value = textStyle) {
            val clickableChildren = @Composable {
                Clickable(onClick = onClick) {
                    children()
                }
            }
            if (onClick != null) {
                Ripple(bounded = true) {
                    clickableChildren()
                }
            } else {
                clickableChildren()
            }
        }
    }
}

/**
 * Material Design implementation of [Button] with [text].
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 * You can specify a [shape] of the surface, it's background [color] and [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 *
 * Example:
 *     Button(
 *         onClick = { ... },
 *         text="TEXT"))
 *
 * @see Button for the flexible implementation with a customizable content.
 * @see TransparentButton for the version with no background.
 *
 * @param text The text to display.
 * @param textStyle The optional text style to apply for the text.
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] value from the theme.
 * @param color The background color. [MaterialColors.primary] is used when null
 *  is provided. Use [TransparentButton] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
@Composable
fun Button(
    text: String,
    textStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = +themeShape { button },
    color: Color = +themeColor { primary },
    border: Border? = null,
    elevation: Dp = 0.dp
) {
    val hasBackground = color.alpha > 0 || border != null
    val horPaddings = if (hasBackground) ButtonHorPadding else ButtonHorPaddingNoBg
    Button(
        onClick = onClick,
        elevation = elevation,
        color = color,
        border = border,
        shape = shape
    ) {
        val constraints = DpConstraints
            .tightConstraintsForHeight(ButtonHeight)
            .copy(minWidth = ButtonMinWidth)
        Container(
            padding = EdgeInsets(left = horPaddings, right = horPaddings),
            constraints = constraints) {
            Text(text = text, style = textStyle)
        }
    }
}

/**
 * Material Design implementation of [Button] with [text] and no background.
 * This will also apply [MaterialColors.primary] as a text color by default, but
 * you can override this with [textStyle].
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 * You can specify a [shape] of the surface, it's background [color] and [elevation].
 *
 * @param text The text to display.
 * @param textStyle The optional text style to apply for the text.
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] from [CurrentShapeAmbient].
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
@Composable
fun TransparentButton(
    text: String,
    textStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = +themeShape { button },
    border: Border? = null,
    elevation: Dp = 0.dp
) {
    val finalTextStyle = TextStyle(color = +themeColor { primary }).merge(textStyle)
    Button(
        text = text,
        onClick = onClick,
        shape = shape,
        elevation = elevation,
        textStyle = finalTextStyle,
        border = border,
        color = Color.Transparent)
}

// Specification for Material Button:
private val ButtonHeight = 36.dp
private val ButtonMinWidth = 64.dp
private val ButtonHorPadding = 16.dp
private val ButtonHorPaddingNoBg = 8.dp
