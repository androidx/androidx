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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.material

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.InnerPadding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.semantics.Semantics
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

/**
 * Material Design implementation of a
 * [Material Contained Button](https://material.io/design/components/buttons.html#contained-button).
 *
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They
 * contain actions that are primary to your app.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [ColorPalette.primary] then the text will by default use
 * [ColorPalette.onPrimary].
 *
 * @sample androidx.ui.material.samples.ButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param disabledElevation The elevation used when [enabled] is false
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param disabledBackgroundColor The background color used when [enabled] is false
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param padding The spacing values to apply internally between the container and the content
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 2.dp,
    disabledElevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: Border? = null,
    backgroundColor: Color = MaterialTheme.colors.primary,
    disabledBackgroundColor: Color = Button.defaultDisabledBackgroundColor,
    contentColor: Color = contentColorFor(backgroundColor),
    disabledContentColor: Color = Button.defaultDisabledContentColor,
    padding: InnerPadding = Button.DefaultInnerPadding,
    text: @Composable () -> Unit
) {
    // Since we're adding layouts in between the clickable layer and the content, we need to
    // merge all descendants, or we'll get multiple nodes
    Semantics(container = true, mergeAllDescendants = true) {
        Surface(
            shape = shape,
            color = if (enabled) backgroundColor else disabledBackgroundColor,
            contentColor = if (enabled) contentColor else disabledContentColor,
            border = border,
            elevation = if (enabled) elevation else disabledElevation,
            modifier = modifier
        ) {
            Box(
                ButtonConstraints
                    .clickable(onClick = onClick, enabled = enabled),
                paddingStart = padding.start,
                paddingTop = padding.top,
                paddingEnd = padding.end,
                paddingBottom = padding.bottom,
                gravity = ContentGravity.Center
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.button,
                    children = text
                )
            }
        }
    }
}

/**
 * Material Design implementation of a
 * [Material Outlined Button](https://material.io/design/components/buttons.html#outlined-button).
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app.
 *
 * Outlined buttons are also a lower emphasis alternative to contained buttons, or a higher emphasis
 * alternative to text buttons.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [ColorPalette.primary] then the text will by default use
 * [ColorPalette.onPrimary].
 *
 * @sample androidx.ui.material.samples.OutlinedButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param padding The spacing values to apply internally between the container and the content
 */
@Composable
inline fun OutlinedButton(
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: Border? = Border(
        1.dp, MaterialTheme.colors.onSurface.copy(alpha = OutlinedStrokeOpacity)
    ),
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    disabledContentColor: Color = Button.defaultDisabledContentColor,
    padding: InnerPadding = Button.DefaultInnerPadding,
    noinline text: @Composable () -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    elevation = elevation,
    disabledElevation = 0.dp,
    shape = shape,
    border = border,
    backgroundColor = backgroundColor,
    disabledBackgroundColor = backgroundColor,
    contentColor = contentColor,
    disabledContentColor = disabledContentColor,
    padding = padding,
    text = text
)

/**
 * Material Design implementation of a
 * [Material Text Button](https://material.io/design/components/buttons.html#text-button).
 *
 * Text buttons are typically used for less-pronounced actions, including those located in cards and
 * dialogs.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [ColorPalette.primary] then the text will by default use
 * [ColorPalette.onPrimary].
 *
 * @sample androidx.ui.material.samples.TextButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param padding The spacing values to apply internally between the container and the content
 */
@Composable
inline fun TextButton(
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: Border? = null,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colors.primary,
    disabledContentColor: Color = Button.defaultDisabledContentColor,
    padding: InnerPadding = TextButton.DefaultInnerPadding,
    noinline text: @Composable () -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    elevation = elevation,
    disabledElevation = 0.dp,
    shape = shape,
    border = border,
    backgroundColor = backgroundColor,
    disabledBackgroundColor = backgroundColor,
    contentColor = contentColor,
    disabledContentColor = disabledContentColor,
    padding = padding,
    text = text
)

// Specification for Material Button:
private val ButtonConstraints = Modifier.preferredSizeIn(minWidth = 64.dp, minHeight = 36.dp)

/**
 * Contains the default values used by [Button]
 */
object Button {
    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default inner padding used by [Button]
     */
    val DefaultInnerPadding = InnerPadding(
        start = ButtonHorizontalPadding,
        top = ButtonVerticalPadding,
        end = ButtonHorizontalPadding,
        bottom = ButtonVerticalPadding
    )

    /**
     * The default disabled background color used by Contained [Button]s
     */
    @Composable
    val defaultDisabledBackgroundColor
        get(): Color = with(MaterialTheme.colors) {
            // we have to composite it over surface here as if we provide a transparent background for
            // Surface and non-zero elevation the artifacts from casting the shadow will be visible
            // below the background.
            onSurface.copy(alpha = 0.12f).compositeOver(surface)
        }

    /**
     * The default disabled content color used by all types of [Button]s
     */
    @Composable
    val defaultDisabledContentColor
        get(): Color = with(MaterialTheme.colors) {
            EmphasisAmbient.current.disabled.applyEmphasis(onSurface)
        }
}

/**
 * Contains the default values used by [TextButton]
 */
object TextButton {
    private val TextButtonHorizontalPadding = 8.dp

    /**
     * The default inner padding used by [TextButton]
     */
    val DefaultInnerPadding = Button.DefaultInnerPadding.copy(
        start = TextButtonHorizontalPadding,
        end = TextButtonHorizontalPadding
    )
}

@PublishedApi
internal const val OutlinedStrokeOpacity = 0.12f
