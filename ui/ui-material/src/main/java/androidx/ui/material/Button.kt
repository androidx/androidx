
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

import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Modifier
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle

/**
 * Styling configuration for a [Button].
 *
 * The three basic Material button styles are provided by [ContainedButtonStyle], intended for high
 * emphasis buttons, [OutlinedButtonStyle], intended for medium emphasis buttons, and
 * [TextButtonStyle], intended for low emphasis buttons.
 *
 * @param color The background color. Use [Color.Transparent] to have no color
 * @param shape Defines the button's shape as well as its shadow
 * @param border Optional border to draw on top of the shape
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button
 * @param paddings The spacing values to apply internally between the container and the content
 * @param textStyle The text style to apply as a default for any children [Text] components
 * @param rippleColor The Ripple color is usually the same color used by the text or iconography in
 * the component. If null is provided the color will be calculated by [RippleTheme.defaultColor].
 */
@Immutable
data class ButtonStyle(
    val color: Color,
    val shape: Shape,
    val border: Border? = null,
    val elevation: Dp = 0.dp,
    val paddings: EdgeInsets = ButtonPaddings,
    val textStyle: TextStyle? = null,
    val rippleColor: Color? = null
)

/**
 * Style used to configure a Button to look like a
 * [Material Contained Button](https://material.io/design/components/buttons.html#contained-button).
 *
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They
 * contain actions that are primary to your app.
 *
 * @sample androidx.ui.material.samples.ContainedButtonSample
 *
 * @see OutlinedButtonStyle
 * @see TextButtonStyle
 *
 * @param color The background color
 * @param shape Defines the button's shape as well as its shadow
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button
 * @param rippleColor The Ripple color is usually the same color used by the text and iconography.
 * If null is provided the color will be calculated by [RippleTheme.defaultColor].
 */
fun ContainedButtonStyle(
    color: Color = MaterialTheme.colors().primary,
    shape: Shape = MaterialTheme.shapes().button,
    elevation: Dp = 2.dp,
    rippleColor: Color? = null
) = ButtonStyle(
    color = color,
    shape = shape,
    elevation = elevation,
    rippleColor = rippleColor
)

/**
 * Style used to configure a Button to look like a
 * [Material Outlined Button](https://material.io/design/components/buttons.html#outlined-button).
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app.
 *
 * Outlined buttons are also a lower emphasis alternative to contained buttons, or a higher emphasis
 * alternative to text buttons.
 *
 * @sample androidx.ui.material.samples.OutlinedButtonSample
 *
 * @see ContainedButtonStyle
 * @see TextButtonStyle
 *
 * @param border Border to draw on top of the button.
 * @param color The background color. Provide [Color.Transparent] to have no color.
 * @param shape Defines the Button's shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 * @param contentColor The color used by text and Ripple.
 */
fun OutlinedButtonStyle(
    border: Border = Border(
        MaterialTheme.colors().onSurface.copy(alpha = OutlinedStrokeOpacity),
        1.dp
    ),
    color: Color = MaterialTheme.colors().surface,
    shape: Shape = MaterialTheme.shapes().button,
    elevation: Dp = 0.dp,
    contentColor: Color? = MaterialTheme.colors().primary
) = ButtonStyle(
    color = color,
    shape = shape,
    border = border,
    elevation = elevation,
    textStyle = TextStyle(color = contentColor),
    rippleColor = contentColor
)

/**
 * Style used to configure a Button to look like a
 * [Material Text Button](https://material.io/design/components/buttons.html#text-button).
 *
 * Text buttons are typically used for less-pronounced actions, including those located in cards and
 * dialogs.
 *
 * @sample androidx.ui.material.samples.TextButtonSample
 *
 * @see ContainedButtonStyle
 * @see OutlinedButtonStyle
 *
 * @param shape Defines the Button's shape.
 * @param contentColor The color used by text and Ripple.
 */
fun TextButtonStyle(
    shape: Shape = MaterialTheme.shapes().button,
    contentColor: Color? = MaterialTheme.colors().primary
) = ButtonStyle(
    color = Color.Transparent,
    shape = shape,
    paddings = TextButtonPaddings,
    textStyle = TextStyle(color = contentColor),
    rippleColor = contentColor
)

/**
 * Material Design implementation of [Button](https://material.io/design/components/buttons.html).
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button will display
 * itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text color will
 * try to match the correlated color for the background color. For example if the background color is set to
 * [ColorPalette.primary] then the text will by default use [ColorPalette.onPrimary].
 *
 * @sample androidx.ui.material.samples.ButtonSample
 *
 * @param modifier Modifier to be applied to the button.
 * @param onClick Will be called when the user clicks the button. The button will be disabled if it is null.
 * @param style Contains the styling parameters for the button.
 */
@Composable
fun Button(
    modifier: Modifier = Modifier.None,
    onClick: (() -> Unit)? = null,
    style: ButtonStyle = ContainedButtonStyle(),
    children: @Composable() () -> Unit
) {
    Surface(
        shape = style.shape,
        color = style.color,
        border = style.border,
        elevation = style.elevation,
        modifier = modifier
    ) {
        Ripple(bounded = true, color = style.rippleColor, enabled = onClick != null) {
            Clickable(onClick = onClick) {
                Container(constraints = ButtonConstraints, padding = style.paddings) {
                    CurrentTextStyleProvider(
                        value = MaterialTheme.typography().button.merge(style.textStyle),
                        children = children
                    )
                }
            }
        }
    }
}

/**
 * Material Design implementation of [Button](https://material.io/design/components/buttons.html) that contains some
 * text.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button will display
 * itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text color will
 * try to match the correlated color for the background color. For example if the background color is set to
 * [ColorPalette.primary] then the text will by default use [ColorPalette.onPrimary].
 *
 * @sample androidx.ui.material.samples.ButtonWithTextSample
 *
 * There is a different overload for this component that takes a lambda of customizable content.
 *
 * @param text The text to display.
 * @param modifier Modifier to be applied to the button.
 * @param onClick Will be called when the user clicks the button. The button will be disabled if it is null.
 * @param style Contains the styling parameters for the button.
 */
@Composable
fun Button(
    text: String,
    modifier: Modifier = Modifier.None,
    onClick: (() -> Unit)? = null,
    style: ButtonStyle = ContainedButtonStyle()
) {
    Button(modifier = modifier, style = style, onClick = onClick) {
        Text(text = text)
    }
}

// Specification for Material Button:
private val ButtonConstraints = DpConstraints(
    minWidth = 64.dp,
    minHeight = 36.dp
)
private val ButtonHorizontalPadding = 16.dp
private val ButtonVerticalPadding = 8.dp
private val ButtonPaddings = EdgeInsets(
    left = ButtonHorizontalPadding,
    top = ButtonVerticalPadding,
    right = ButtonHorizontalPadding,
    bottom = ButtonVerticalPadding
)
private val TextButtonHorizontalPadding = 8.dp
private val TextButtonPaddings = ButtonPaddings.copy(
    left = TextButtonHorizontalPadding,
    right = TextButtonHorizontalPadding
)
private val OutlinedStrokeOpacity = 0.12f