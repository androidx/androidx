
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
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle

/**
 * Styling params for the [Button].
 *
 * @param color The background color. Provide [Color.Transparent] to have no color.
 * @param shape Defines the Button's shape as well its shadow.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 * @param paddings The paddings to apply for the Button's container.
 * @param textStyle The text style to apply for the children [Text] components.
 */
@Immutable
data class ButtonStyle(
    val color: Color,
    val shape: Shape,
    val border: Border? = null,
    val elevation: Dp = 0.dp,
    val paddings: EdgeInsets = ButtonPaddings,
    val textStyle: TextStyle? = null
)

/**
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They
 * contain actions that are primary to your app.
 *
 * @sample androidx.ui.material.samples.ContainedButtonSample
 *
 * @see OutlinedButtonStyle
 * @see TextButtonStyle
 *
 * @param color The background color.
 * @param shape Defines the Button's shape and shadow.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
fun ContainedButtonStyle(
    color: Color = +themeColor { primary },
    shape: Shape = +themeShape { button },
    elevation: Dp = 0.dp // TODO update to 2.dp when DrawShadow will be ready
) = ButtonStyle(
    color = color,
    shape = shape,
    elevation = elevation
)

/**
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but aren’t
 * the primary action in an app.
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
 */
fun OutlinedButtonStyle(
    border: Border = Border(+themeColor { onSurface.copy(alpha = OutlinedStrokeOpacity) }, 1.dp),
    color: Color = +themeColor { surface },
    shape: Shape = +themeShape { button },
    elevation: Dp = 0.dp
) = ButtonStyle(
    color = color,
    shape = shape,
    border = border,
    elevation = elevation,
    textStyle = TextStyle(color = +themeColor { primary })
)

/**
 * Text buttons are typically used for less-pronounced actions, including those located in cards and
 * dialogs.
 *
 * @sample androidx.ui.material.samples.TextButtonSample
 *
 * @see ContainedButtonStyle
 * @see OutlinedButtonStyle
 *
 * @param shape Defines the Button's shape.
 */
fun TextButtonStyle(
    shape: Shape = +themeShape { button }
) = ButtonStyle(
    color = Color.Transparent,
    shape = shape,
    paddings = TextButtonPaddings,
    textStyle = TextStyle(color = +themeColor { primary })
)

/**
 * Material Design implementation of [Button].
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 *
 * @sample androidx.ui.material.samples.ButtonSample
 *
 * @see BaseButton for the flexible variant that does not provide an internal padded container.
 *
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param style Contains the styling params for the Button.
 */
@Composable
fun Button(
    onClick: (() -> Unit)? = null,
    style: ButtonStyle = ContainedButtonStyle(),
    children: @Composable() () -> Unit
) {
    BaseButton(
        onClick = onClick,
        color = style.color,
        shape = style.shape,
        border = style.border,
        elevation = style.elevation,
        textStyle = +themeTextStyle { button.merge(style.textStyle) }
    ) {
        Container(constraints = ButtonConstraints, padding = style.paddings, children = children)
    }
}

/**
 * Material Design implementation of [Button] with [text].
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button].
 *
 * @sample androidx.ui.material.samples.ButtonWithTextSample
 *
 * @see Button for the version with a slot for a text so you can customize the text style.
 *
 * @param text The text to display.
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param style Contains the styling params for the Button.
 */
@Composable
fun Button(
    text: String,
    onClick: (() -> Unit)? = null,
    style: ButtonStyle = ContainedButtonStyle()
) {
    Button(style = style, onClick = onClick) {
        Text(text = text)
    }
}

/**
 * [Button] with flexible user interface. You can provide any content as a [children] lambda.
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 *
 * @sample androidx.ui.material.samples.BaseButtonSample
 *
 * @see Button overload for the default Material Design implementation of [Button] with text.
 *
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param color The background color. Provide [Color.Transparent] to have no color.
 * @param shape Defines the Button's shape as well its shadow.
 * @param border Optional border to draw on top of the button.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 * @param textStyle The text style to apply for the children [Text] components.
 */
@Composable
fun BaseButton(
    onClick: (() -> Unit)? = null,
    color: Color = +themeColor { primary },
    shape: Shape = RectangleShape,
    border: Border? = null,
    elevation: Dp = 0.dp,
    textStyle: TextStyle = +themeTextStyle { button },
    children: @Composable() () -> Unit
) {
    Surface(shape, color, border, elevation) {
        CurrentTextStyleProvider(value = textStyle) {
            if (onClick != null) {
                Ripple(bounded = true) {
                    Clickable(onClick = onClick, children = children)
                }
            } else {
                Clickable(children = children)
            }
        }
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