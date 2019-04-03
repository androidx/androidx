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

import androidx.ui.baseui.Clickable
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Text
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.coerceAtMost
import androidx.ui.core.dp
import androidx.ui.material.borders.BorderStyle
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.material.ripple.BoundedRipple
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.unaryPlus

/**
 * [Button] with flexible user interface. You can provide any content you want as a
 * [children] composable.
 *
 * To make a [Button] clickable, you must provide an [onClick]. Setting [enabled] to false
 * also affects this state.
 * You can specify a [shape] of the surface, it's background [color] and an [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 * To modify these default style values, use [CurrentTextStyleProvider].
 *
 * Example:
 *     <Button onClick={ ... }>
 *         <Padding padding=EdgeInsets(16.dp)>
 *             <Text text=TextSpan(text = "CUSTOM BUTTON") />
 *         </Padding>
 *     </Button>
 *
 * @see Button overload for the default Material Design implementation of [Button] with text.
 *
 * @param onClick Will be called when user clicked on the button.
 * @param enabled Defines the enabled state. The button will not be clickable when it set
 *  to false or when [onClick] is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] from [CurrentShapeAmbient].
 * @param color The background color. When null is provided [MaterialColors.primary] is used.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
@Composable
fun Button(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: ShapeBorder? = null,
    color: Color? = null,
    elevation: Dp = 0.dp,
    @Children children: () -> Unit
) {
    val surfaceColor = +color.orFromTheme { primary }
    val surfaceShape = +shape.orFromTheme { button }
    val textStyle = (+themeTextStyle { button }).copy(
        color = +textColorForBackground(surfaceColor)
    )
    <CurrentTextStyleProvider value=textStyle>
        <Surface shape=surfaceShape color=surfaceColor elevation>
            val clickableChildren = @Composable {
                <Clickable enabled onClick>
                    <children />
                </Clickable>
            }
            if (enabled && onClick != null) {
                <BoundedRipple>
                    <clickableChildren />
                </BoundedRipple>
            } else {
                <clickableChildren />
            }
        </Surface>
    </CurrentTextStyleProvider>
}

/**
 * Material Design implementation of [Button] with [text].
 *
 * [Button] will be clickable if you provide [onClick] and [enabled] set to true.
 * You can specify a [shape] of the surface, it's background [color] and [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 *
 * Example:
 *     <Button
 *         onClick={ ... }
 *         text="TEXT") />
 *
 * @see Button for the flexible implementation with a customizable content.
 *
 * @param text The text to display.
 * @param textStyle The optional text style selector from the [MaterialTypography].
 * @param onClick Will be called when user clicked on the button.
 * @param enabled Defines the enabled state. The button will not be clickable when it set
 *  to false or when [onClick] is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] from [CurrentShapeAmbient].
 * @param color The background color. When null is provided [MaterialColors.primary] is used.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
@Composable
fun Button(
    text: String,
    textStyle: (MaterialTypography.() -> TextStyle)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: ShapeBorder? = null,
    color: Color? = null,
    elevation: Dp = 0.dp
) {
    val surfaceColor = +color.orFromTheme { primary }
    val surfaceShape = +shape.orFromTheme { button }
    val hasBackground = surfaceColor.alpha > 0 || surfaceShape.borderStyle != BorderStyle.None
    val horPaddings = if (hasBackground) ButtonHorPadding else ButtonHorPaddingNoBg
    <Button onClick enabled elevation color=surfaceColor shape=surfaceShape >
        <Layout layoutBlock={ measurables, constraints ->
            val fullHorPaddings = horPaddings.toIntPx() * 2
            val textConstraints = constraints.copy(
                minWidth = (constraints.minWidth - fullHorPaddings).coerceAtLeast(IntPx.Zero),
                maxWidth = (constraints.maxWidth - fullHorPaddings).coerceAtLeast(IntPx.Zero),
                maxHeight = constraints.maxHeight.coerceAtMost(ButtonHeight.toIntPx())
            )

            val placeable = measurables.first().measure(textConstraints)

            val width =
                (placeable.width + fullHorPaddings).coerceAtLeast(ButtonMinWidth.toIntPx())
            val height = ButtonHeight.toIntPx()

            val leftOffset = (width - fullHorPaddings - placeable.width) / 2 + fullHorPaddings / 2
            val topOffset = (height - placeable.height) / 2

            layout(width, height) {
                placeable.place(leftOffset, topOffset)
            }
        }>
            val style = textStyle?.let { +themeTextStyle(it) }
            <Text text=TextSpan(text = text, style = style) />
        </Layout>
    </Button>
}

// Specification for Material Button:
private val ButtonHeight = 36.dp
private val ButtonMinWidth = 64.dp
private val ButtonHorPadding = 16.dp
private val ButtonHorPaddingNoBg = 8.dp
