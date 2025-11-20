/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.ChildButtonTokens
import androidx.wear.compose.material3.tokens.CompactButtonTokens
import androidx.wear.compose.material3.tokens.FilledButtonTokens
import androidx.wear.compose.material3.tokens.FilledTonalButtonTokens
import androidx.wear.compose.material3.tokens.ImageButtonTokens
import androidx.wear.compose.material3.tokens.OutlinedButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens

/**
 * Base level Wear Material3 [Button] that offers a single slot to take any content. Used as the
 * container for more opinionated [Button] components that take specific content such as icons and
 * labels.
 *
 * The [Button] is stadium-shaped by default and its standard height is designed to take 2 lines of
 * text of [Typography.labelMedium] style. With localisation and/or large font sizes, the text can
 * extend to a maximum of 3 lines in which case, the [Button] height adjusts to accommodate the
 * contents.
 *
 * [Button] takes the [ButtonDefaults.buttonColors] color scheme by default, with colored
 * background, contrasting content color and no border. This is a high-emphasis button for the
 * primary, most important or most common action on a screen.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors], [OutlinedButton]
 * which defaults to [ButtonDefaults.outlinedButtonColors] and [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button]:
 *
 * @sample androidx.wear.compose.material3.samples.SimpleButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.buttonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content Slot for composable body content displayed on the Button
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = FilledButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Base level Wear Material3 [Button] that offers parameters for container image backgrounds, with a
 * single slot to take any content.
 *
 * An Image background is a means to reinforce the meaning of information in a Button. Buttons
 * should have a content color that contrasts with the background image and scrim.
 *
 * This [Button] takes [containerPainter] for the image background to be drawn when the button is
 * enabled and [disabledContainerPainter] for the image background when the button is disabled (The
 * [ButtonColors] containerColor and disabledContainerColor properties are ignored). It is
 * recommended to use [ButtonDefaults.containerPainter] to create the painters so that a scrim is
 * drawn on top of the container image, ensuring that any content above the background is legible.
 *
 * The [Button] is Stadium-shaped by default and has a max height designed to take no more than two
 * lines of text of [Typography.labelMedium] style. With localisation and/or large font sizes, the
 * text can extend to a maximum of 3 lines in which case, the [Button] height adjusts to accommodate
 * the contents.
 *
 * [Button] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button] with an image background:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonWithImageSample
 * @param onClick Will be called when the user clicks the button
 * @param containerPainter The [Painter] to use to draw the container image of the [Button], such as
 *   returned by [ButtonDefaults.containerPainter].
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param disabledContainerPainter [Painter] to use to draw the container of the [Button] when not
 *   enabled, such as returned by [ButtonDefaults.containerPainter] with alpha =
 *   [ButtonDefaults.DisabledContainerAlpha].
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states (the containerColor and disabledContainerColor are overridden
 *   by containerPainter and disabledContainerPainter respectively). See
 *   [ButtonDefaults.buttonWithContainerPainterColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content Slot for composable body content displayed on the Button
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun Button(
    onClick: () -> Unit,
    containerPainter: Painter,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    disabledContainerPainter: Painter = ButtonDefaults.disabledContainerPainter(containerPainter),
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = OutlinedButtonTokens.LabelFont.value,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Base level Wear Material3 [FilledTonalButton] that offers a single slot to take any content. Used
 * as the container for more opinionated [FilledTonalButton] components that take specific content
 * such as icons and labels.
 *
 * The [FilledTonalButton] is Stadium-shaped by default and has a max height designed to take no
 * more than two lines of text of [Typography.labelMedium] style. With localisation and/or large
 * font sizes, the text can extend to a maximum of 3 lines in which case, the [FilledTonalButton]
 * height adjusts to accommodate the contents. The [FilledTonalButton] can have an icon or image
 * horizontally parallel to the two lines of text.
 *
 * [FilledTonalButton] takes the [ButtonDefaults.filledTonalButtonColors] color scheme by default,
 * with muted background, contrasting content color and no border. This is a medium-emphasis button
 * for important actions that don't distract from other onscreen elements, such as final or
 * unblocking actions in a flow with less emphasis than [Button].
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [OutlinedButton] which defaults to
 * [ButtonDefaults.outlinedButtonColors] and [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * FilledTonalButton can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [FilledTonalButton]:
 *
 * @sample androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.filledTonalButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content Slot for composable body content displayed on the Button
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = FilledTonalButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Base level Wear Material3 [OutlinedButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [OutlinedButton] components that take specific content such as
 * icons and labels.
 *
 * The [OutlinedButton] is Stadium-shaped by default and has a max height designed to take no more
 * than two lines of text of [Typography.labelMedium] style. With localisation and/or large font
 * sizes, the text can extend to a maximum of 3 lines in which case, the [OutlinedButton] height
 * adjusts to accommodate the contents. The [OutlinedButton] can have an icon or image horizontally
 * parallel to the two lines of text.
 *
 * [OutlinedButton] takes the [ButtonDefaults.outlinedButtonColors] color scheme by default, with a
 * transparent background and a thin border. This is a medium-emphasis button for important,
 * non-primary actions that need attention.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [FilledTonalButton] which defaults to
 * [ButtonDefaults.filledTonalButtonColors], [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * OutlinedButton can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of an [OutlinedButton]:
 *
 * @sample androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content Slot for composable body content displayed on the OutlinedButton
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = OutlinedButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Base level Wear Material3 [ChildButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [ChildButton] components that take specific content such as
 * icons and labels.
 *
 * The [ChildButton] is stadium-shaped by default and its standard height is designed to take 2
 * lines of text of [Typography.labelMedium] style. With localisation and/or large font sizes, the
 * text can extend to a maximum of 3 lines in which case, the [ChildButton] height adjusts to
 * accommodate the contents. The [ChildButton] can have an icon or image horizontally parallel to
 * the two lines of text.
 *
 * [ChildButton] takes the [ButtonDefaults.childButtonColors] color scheme by default, with a
 * transparent background and no border. This is a low-emphasis button for optional or supplementary
 * actions with the least amount of prominence.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [FilledTonalButton] which defaults to
 * [ButtonDefaults.filledTonalButtonColors], [OutlinedButton] which defaults to
 * [ButtonDefaults.outlinedButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * ChildButton can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [ChildButton]:
 *
 * @sample androidx.wear.compose.material3.samples.SimpleChildButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.childButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content Slot for composable body content displayed on the ChildButton
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun ChildButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.childButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = OutlinedButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Wear Material3 [Button] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [Button] is stadium-shaped by default and its standard height is designed to take 2 lines of
 * text of [Typography.labelMedium] style - either a two-line label or both a single line label and
 * a secondary label. With localisation and/or large font sizes, the [Button] height adjusts to
 * accommodate the contents. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [Button] takes the [ButtonDefaults.buttonColors] color scheme by default, with colored
 * background, contrasting content color and no border. This is a high-emphasis button for the
 * primary, most important or most common action on a screen.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors], [OutlinedButton]
 * which defaults to [ButtonDefaults.outlinedButtonColors] and [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * [Button] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button] with an icon and secondary label:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonSample
 *
 * Example of a [Button] with a large icon and adjusted content padding:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonLargeIconSample
 *
 * Example of a [Button] with an extra large icon and adjusted content padding:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonExtraLargeIconSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 *   [ButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.buttonColors]. Defaults to
 *   [ButtonDefaults.buttonColors]
 * @param border Optional [BorderStroke] that will be used to resolve the button border in different
 *   states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = FilledButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = FilledButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = FilledButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * Wear Material3 [Button] that offers parameters for container image backgrounds, with three slots
 * and a specific layout for an icon, label and secondaryLabel. The icon and secondaryLabel are
 * optional. The items are laid out with the icon, if provided, at the start of a row, with a column
 * next containing the two label slots.
 *
 * An image background is a means to reinforce the meaning of information in a Button. Buttons
 * should have a content color that contrasts with the background image and scrim.
 *
 * This [Button] takes [containerPainter] for the container image background to be drawn when the
 * button is enabled and [disabledContainerPainter] for the image background when the button is
 * disabled (the [ButtonColors] containerColor and disabledContainerColor properties are ignored).
 * It is recommended to use [ButtonDefaults.containerPainter] to create the painters so that a scrim
 * is drawn on top of the container image, ensuring that any content above the background is
 * legible.
 *
 * The [Button] is stadium-shaped by default and its standard height is designed to take 2 lines of
 * text of [Typography.labelMedium] style - either a two-line label or both a single line label and
 * a secondary label. With localisation and/or large font sizes, the [Button] height adjusts to
 * accommodate the contents. The label and secondary label should be consistently aligned.
 *
 * [Button] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button] with an image background, an icon and a secondary label:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonWithImageSample
 * @param onClick Will be called when the user clicks the button
 * @param containerPainter The [Painter] to use to draw the container image of the [Button], such as
 *   returned by [ButtonDefaults.containerPainter].
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 *   [ButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param disabledContainerPainter [Painter] to use to draw the container of the [Button] when not
 *   enabled, such as returned by [ButtonDefaults.disabledContainerPainter].
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states (the containerColor and disabledContainerColor are overridden
 *   by containerPainter and disabledContainerPainter respectively). See
 *   [ButtonDefaults.buttonWithContainerPainterColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in different
 *   states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun Button(
    onClick: () -> Unit,
    containerPainter: Painter,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    disabledContainerPainter: Painter = ButtonDefaults.disabledContainerPainter(containerPainter),
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = FilledButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = OutlinedButtonTokens.LabelFont.value,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = FilledButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * Wear Material3 [FilledTonalButton] that offers three slots and a specific layout for an icon,
 * label and secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with
 * the icon, if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [FilledTonalButton] is stadium-shaped by default and its standard height is designed to take
 * 2 lines of text of [Typography.labelMedium] style - either a two-line label or both a single line
 * label and a secondary label. With localisation and/or large font sizes, the [FilledTonalButton]
 * height adjusts to accommodate the contents. The label and secondary label should be consistently
 * aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [FilledTonalButton] takes the [ButtonDefaults.filledTonalButtonColors] color scheme by default,
 * with muted background, contrasting content color and no border. This is a medium-emphasis button
 * for important actions that don't distract from other onscreen elements, such as final or
 * unblocking actions in a flow with less emphasis than [Button].
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [OutlinedButton] which defaults to
 * [ButtonDefaults.outlinedButtonColors] and [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * [FilledTonalButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of a [FilledTonalButton] with an icon and secondary label:
 *
 * @sample androidx.wear.compose.material3.samples.FilledTonalButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 *   [ButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.filledTonalButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in different
 *   states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = FilledButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = FilledTonalButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = FilledButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * Wear Material3 [OutlinedButton] that offers three slots and a specific layout for an icon, label
 * and secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the
 * icon, if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [OutlinedButton] is stadium-shaped by default and its standard height is designed to take 2
 * lines of text of [Typography.labelMedium] style - either a two-line label or both a single line
 * label and a secondary label. With localisation and/or large font sizes, the [OutlinedButton]
 * height adjusts to accommodate the contents. The label and secondary label should be consistently
 * aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [OutlinedButton] takes the [ButtonDefaults.outlinedButtonColors] color scheme by default, with a
 * transparent background and a thin border. This is a medium-emphasis button for important,
 * non-primary actions that need attention.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [FilledTonalButton] which defaults to
 * [ButtonDefaults.filledTonalButtonColors], [ChildButton] which defaults to
 * [ButtonDefaults.childButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * [OutlinedButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of an [OutlinedButton] with an icon and secondary label:
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 *   [ButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in different
 *   states. See [ButtonDefaults.outlinedButtonBorder].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = FilledButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = OutlinedButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = FilledButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * Wear Material3 [ChildButton] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [ChildButton] is stadium-shaped by default and its standard height is designed to take 2
 * lines of text of [Typography.labelMedium] style - either a two-line label or both a single line
 * label and a secondary label. With localisation and/or large font sizes, the [ChildButton] height
 * adjusts to accommodate the contents. The label and secondary label should be consistently
 * aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [ChildButton] takes the [ButtonDefaults.childButtonColors] color scheme by default, with a
 * transparent background and no border. This is a low-emphasis button for optional or supplementary
 * actions with the least amount of prominence.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are: [Button]
 * which defaults to [ButtonDefaults.buttonColors], [FilledTonalButton] which defaults to
 * [ButtonDefaults.filledTonalButtonColors], [OutlinedButton] which defaults to
 * [ButtonDefaults.outlinedButtonColors]. For a background image, see the overload of Button with a
 * containerPainter parameter.
 *
 * [ChildButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [ChildButton] with an icon and secondary label:
 *
 * @sample androidx.wear.compose.material3.samples.ChildButtonSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 *   [ButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.childButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in different
 *   states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
public fun ChildButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.childButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: @Composable RowScope.() -> Unit,
): Unit =
    ButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = FilledButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = ChildButtonTokens.LabelFont.value,
        containerPainter = null,
        disabledContainerPainter = null,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = FilledButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * A Wear Material3 [CompactButton] that offers two slots and a specific layout for an icon and
 * label. Both the icon and label are optional however it is expected that at least one will be
 * provided.
 *
 * The [CompactButton] is Stadium shaped and has a max height designed to take no more than one line
 * of text and/or one icon. The default max height is [ButtonDefaults.CompactButtonHeight]. This
 * includes a visible button height of 32.dp and 8.dp of padding above and below the button in order
 * to meet accessibility guidelines that request a minimum of 48.dp height and width of tappable
 * area.
 *
 * If an icon is provided then the labels should be "start" aligned, e.g. left aligned in
 * left-to-right mode so that the text starts next to the icon.
 *
 * The items are laid out as follows.
 * 1. If a label is provided then the button will be laid out with the optional icon at the start of
 *    a row followed by the label with a default max height of [ButtonDefaults.CompactButtonHeight].
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered with a
 *    default height of [ButtonDefaults.CompactButtonHeight] and the default width of
 *    [ButtonDefaults.IconOnlyCompactButtonWidth]
 *
 * If neither icon nor label is provided then the button will displayed like an icon only button but
 * with no contents or background color.
 *
 * [CompactButton] takes the [ButtonDefaults.buttonColors] color scheme by default, with colored
 * background, contrasting content color and no border. This is a high-emphasis button for the
 * primary, most important or most common action on a screen.
 *
 * Other recommended [ButtonColors] for different levels of emphasis are:
 * [ButtonDefaults.filledTonalButtonColors], [ButtonDefaults.outlinedButtonColors] and
 * [ButtonDefaults.childButtonColors]. Buttons can also take an image background using
 * [ButtonDefaults.buttonWithContainerPainterColors].
 *
 * [CompactButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [CompactButton] with an icon and a label
 *
 * @sample androidx.wear.compose.material3.samples.CompactButtonSample
 *
 * Example of a [CompactButton] with an icon and label and with
 * [ButtonDefaults.filledTonalButtonColors]
 *
 * @sample androidx.wear.compose.material3.samples.FilledTonalCompactButtonSample
 *
 * Example of a [CompactButton] with an icon and label and with
 * [ButtonDefaults.outlinedButtonBorder] and [ButtonDefaults.outlinedButtonColors]. The example
 * includes a [Text] that expands and collapses when the [CompactButton] is clicked.
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedCompactButtonSample
 *
 * Example of a [CompactButton] with onLongClick:
 *
 * @sample androidx.wear.compose.material3.samples.CompactButtonWithOnLongClickSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [ButtonDefaults.ExtraSmallIconSize] when used
 *   with a label or [ButtonDefaults.SmallIconSize] when used as the only content in the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.buttonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param transformation Transformation to be used when button appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param label A slot for providing the button's main label. The contents are expected to be a
 *   single line of text which is "start" aligned if there is an icon preset and "center" aligned if
 *   not.
 */
// TODO(b/261838497) Add Material3 samples and UX guidance links
@Composable
public fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.compactButtonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.CompactButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    label: (@Composable RowScope.() -> Unit)? = null,
) {
    if (label != null) {
        ButtonImpl(
            onClick = onClick,
            modifier =
                modifier
                    .compactButtonModifier()
                    .padding(ButtonDefaults.CompactButtonTapTargetPadding),
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
            secondaryLabelContent = null,
            icon = icon,
            enabled = enabled,
            shape = shape,
            labelFont = CompactButtonTokens.LabelFont.value,
            containerPainter = null,
            disabledContainerPainter = null,
            colors = colors,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            transformation = transformation,
            labelContent =
                provideScopeContent(
                    contentColor = colors.contentColor(enabled),
                    textStyle = CompactButtonTokens.LabelFont.value,
                    textConfiguration =
                        TextConfiguration(
                            textAlign = if (icon != null) TextAlign.Start else TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        ),
                    label,
                ),
        )
    } else {
        // Icon only compact buttons have their own layout with a specific width and center aligned
        // content. We use the base simple single slot Button under the covers.
        ButtonImpl(
            onClick = onClick,
            modifier =
                modifier
                    .compactButtonModifier()
                    .width(ButtonDefaults.IconOnlyCompactButtonWidth)
                    .padding(ButtonDefaults.CompactButtonTapTargetPadding),
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
            enabled = enabled,
            shape = shape,
            labelFont = CompactButtonTokens.LabelFont.value,
            containerPainter = null,
            disabledContainerPainter = null,
            colors = colors,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            transformation = transformation,
        ) {
            // Use a box to fill and center align the icon into the single slot of the
            // Button
            Box(modifier = Modifier.fillMaxSize().wrapContentSize(align = Alignment.Center)) {
                if (icon != null) {
                    icon()
                }
            }
        }
    }
}

/** Contains the default values used by [Button] */
public object ButtonDefaults {
    /** Recommended [RoundedCornerShape] for [Button]. */
    public val shape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerLarge

    /** Recommended [RoundedCornerShape] for [CompactButton]. */
    public val compactButtonShape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerMedium

    /**
     * Creates a [ButtonColors] with a muted background and contrasting content color, the defaults
     * for medium emphasis buttons like [FilledTonalButton]. Use [filledTonalButtonColors] for
     * important actions that don't distract from other onscreen elements, such as final or
     * unblocking actions in a flow with less emphasis than [buttonColors].
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have alpha ([DisabledContainerAlpha]) value applied.
     */
    @Composable
    public fun filledTonalButtonColors(): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalButtonColors

    /**
     * Creates a [ButtonColors] with a muted background and contrasting content color, the defaults
     * for medium emphasis buttons like [FilledTonalButton]. Use [filledTonalButtonColors] for
     * important actions that don't distract from other onscreen elements, such as final or
     * unblocking actions in a flow with less emphasis than [buttonColors].
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have alpha ([DisabledContainerAlpha]) value applied.
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     *   for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The secondary content color of this [Button] when not
     *   enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    public fun filledTonalButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Creates a [ButtonColors] as an alternative to the [filledTonalButtonColors], giving a surface
     * with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have alpha ([DisabledContainerAlpha]) value applied.
     *
     * Example of a [Button] with [filledVariantButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.SimpleFilledVariantButtonSample
     */
    @Composable
    public fun filledVariantButtonColors(): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledVariantButtonColors

    /**
     * Creates a [ButtonColors] as an alternative to the [filledTonalButtonColors], giving a surface
     * with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have alpha ([DisabledContainerAlpha]) value applied.
     *
     * Example of a [Button] with [filledVariantButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledVariantButtonSample
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     *   for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The secondary content color of this [Button] when not
     *   enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    public fun filledVariantButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledVariantButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Creates a [ButtonColors] with a transparent background (typically paired with
     * [ButtonDefaults.outlinedButtonBorder]), the defaults for medium emphasis buttons like
     * [OutlinedButton], for important, non-primary actions that need attention.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have an alpha([DisabledContainerAlpha]) applied.
     */
    @Composable
    public fun outlinedButtonColors(): ButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedButtonColors

    /**
     * Creates a [ButtonColors] with a transparent background (typically paired with
     * [ButtonDefaults.outlinedButtonBorder]), the defaults for medium emphasis buttons like
     * [OutlinedButton], for important, non-primary actions that need attention.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have an alpha([DisabledContainerAlpha]) applied.
     *
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     *   for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The secondary content color of this [Button] when not
     *   enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    public fun outlinedButtonColors(
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedButtonColors.copy(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Creates a [ButtonColors] with transparent background, the defaults for low emphasis buttons
     * like [ChildButton]. Use [childButtonColors] for optional or supplementary actions with the
     * least amount of prominence.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have an alpha([DisabledContainerAlpha]) value applied.
     */
    @Composable
    public fun childButtonColors(): ButtonColors =
        MaterialTheme.colorScheme.defaultChildButtonColors

    /**
     * Creates a [ButtonColors] with transparent background, the defaults for low emphasis buttons
     * like [ChildButton]. Use [childButtonColors] for optional or supplementary actions with the
     * least amount of prominence.
     *
     * If a button is disabled then the content will have an alpha([DisabledContentAlpha]) value
     * applied and container will have an alpha([DisabledContainerAlpha]) value applied.
     *
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     *   for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The secondary content color of this [Button] when not
     *   enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    public fun childButtonColors(
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultChildButtonColors.copy(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /** Creates a [ButtonColors] for the content in a [Button] with an image container painter. */
    @Composable
    public fun buttonWithContainerPainterColors(): ButtonColors =
        MaterialTheme.colorScheme.defaultButtonWithContainerPainterColors

    /**
     * Creates a [ButtonColors] for the content in a [Button] with an image container painter.
     *
     * Example of a Button with an image background:
     *
     * @sample androidx.wear.compose.material3.samples.ButtonWithImageSample
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     *   for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     * @param disabledContentColor The content color of this [Button] when disabled
     * @param disabledSecondaryContentColor The secondary content color of this [Button] when
     *   disabled, used for secondary label content
     * @param disabledIconColor The icon color of this [Button] when disabled, used for icon content
     */
    @Composable
    public fun buttonWithContainerPainterColors(
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultButtonWithContainerPainterColors.copy(
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Creates a [Painter] for the background of a [Button] with container painter, that displays an
     * image with a scrim on top to make sure that any content above the background will be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * Example of a Button with an image background:
     *
     * @sample androidx.wear.compose.material3.samples.ButtonWithImageSample
     * @param image The [Painter] to use to draw the container background of the [Button].
     * @param scrim The [Brush] to use to paint a scrim over the container image to ensure that any
     *   text drawn over the image is legible.
     * @param sizeToIntrinsics When false (the default), fills the available space within the
     *   container. Pass true to retain the size of the image.
     * @param alignment Specifies alignment of the container image painter relative to the
     *   container.
     * @param contentScale Strategy for scaling the painter if its size does not match the
     *   container.
     * @param alpha Opacity of the container image painter and scrim.
     */
    @Composable
    public fun containerPainter(
        image: Painter,
        scrim: Brush? = scrimBrush(),
        sizeToIntrinsics: Boolean = false,
        alignment: Alignment = Alignment.Center,
        contentScale: ContentScale = ContentScale.Fit,
        alpha: Float = DefaultAlpha,
    ): Painter {
        return remember(image, scrim, sizeToIntrinsics, alignment, contentScale, alpha) {
            containerPainter(
                painter = image,
                scrim = scrim,
                sizeToIntrinsics = sizeToIntrinsics,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
            )
        }
    }

    /**
     * Creates a [Painter] for the disabled background of a [Button] with container painter - draws
     * the containerPainter with an alpha applied to achieve a disabled effect.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * @param containerPainter The [Painter] to use to draw the container background of the [Button]
     *   when enabled. An alpha will be applied to this to achieve a disabled effect.
     */
    public fun disabledContainerPainter(containerPainter: Painter): Painter {
        return containerPainter(painter = containerPainter, alpha = DisabledContainerAlpha)
    }

    /** Creates a [Brush] for the recommended scrim drawn on top of image container backgrounds. */
    @Composable
    public fun scrimBrush(): Brush {
        val startColor = scrimGradientStartColor
        val endColor = scrimGradientEndColor
        return remember(startColor, endColor) {
            Brush.linearGradient(colors = listOf(startColor, endColor))
        }
    }

    /**
     * Creates a [BorderStroke], such as for an [OutlinedButton]
     *
     * @param enabled Controls the color of the border based on the enabled/disabled state of the
     *   button
     * @param borderColor The color to use for the border for this outline when enabled
     * @param disabledBorderColor The color to use for the border for this outline when disabled
     * @param borderWidth The width to use for the border for this outline. It is strongly
     *   recommended to use the default width as this outline is a key characteristic of Wear
     *   Material3.
     */
    @Composable
    public fun outlinedButtonBorder(
        enabled: Boolean,
        borderColor: Color = OutlinedButtonTokens.ContainerBorderColor.value,
        disabledBorderColor: Color =
            OutlinedButtonTokens.DisabledContainerBorderColor.value.toDisabledColor(
                disabledAlpha = OutlinedButtonTokens.DisabledContainerBorderOpacity
            ),
        borderWidth: Dp = OutlinedButtonTokens.ContainerBorderWidth,
    ): BorderStroke = BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in a
     * [Button].
     */
    @Composable
    public fun buttonColors(): ButtonColors = MaterialTheme.colorScheme.defaultButtonColors

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in a
     * [Button].
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The content color of this [Button] when enabled
     * @param iconColor The content color of this [Button] when enabled
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Button] when not enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    public fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /** The recommended horizontal padding used by [Button] by default */
    public val ButtonHorizontalPadding: Dp = 14.dp

    /** The recommended start padding to be used with [Button] with a large icon */
    public val ButtonLargeIconStartPadding: Dp = 12.dp

    /** The recommended start padding to be used with [Button] with an extra large icon */
    public val ButtonExtraLargeIconStartPadding: Dp = 8.dp

    /** The recommended vertical padding used by [Button] by default */
    public val ButtonVerticalPadding: Dp = 6.dp

    /** The default content padding used by [Button] */
    public val ContentPadding: PaddingValues =
        PaddingValues(horizontal = ButtonHorizontalPadding, vertical = ButtonVerticalPadding)

    /** The default content padding used by [Button] with a large icon */
    public val ButtonWithLargeIconContentPadding: PaddingValues =
        PaddingValues(
            start = ButtonLargeIconStartPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding,
        )

    /** The default content padding used by [Button] with an extra large icon */
    public val ButtonWithExtraLargeIconContentPadding: PaddingValues =
        PaddingValues(
            start = ButtonExtraLargeIconStartPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding,
        )

    /** The recommended icon size when used in [CompactButton]s containing both icon and text. */
    public val ExtraSmallIconSize: Dp = CompactButtonTokens.IconSize

    /** The recommended icon size when used in [CompactButton]s containing icon-only content. */
    public val SmallIconSize: Dp = CompactButtonTokens.IconOnlyIconSize

    /** The recommended default size for icons when used inside a [Button]. */
    public val IconSize: Dp = FilledButtonTokens.IconSize

    /** The recommended icon size when used in [Button]s for icons such as an app icon */
    public val LargeIconSize: Dp = FilledButtonTokens.IconLargeSize

    /** The recommended icon size when used in [Button]s for icons such as an avatar icon */
    public val ExtraLargeIconSize: Dp = FilledButtonTokens.IconExtraLargeSize

    /**
     * The default height applied for the [Button]. Note that you can override it by applying
     * Modifier.heightIn directly on [Button].
     */
    public val Height: Dp = FilledButtonTokens.ContainerHeight

    public val CompactButtonHorizontalPadding: Dp = 12.dp
    public val CompactButtonVerticalPadding: Dp = 0.dp

    /** The default content padding used by [CompactButton] */
    public val CompactButtonContentPadding: PaddingValues =
        PaddingValues(
            start = CompactButtonHorizontalPadding,
            top = CompactButtonVerticalPadding,
            end = CompactButtonHorizontalPadding,
            bottom = CompactButtonVerticalPadding,
        )

    /**
     * The height applied for the [CompactButton]. This includes a visible button height of 32.dp
     * and 8.dp of padding above and below the button in order to meet accessibility guidelines that
     * request a minimum of 48.dp height and width of tappable area.
     *
     * Note that you can override it by adjusting Modifier.height and Modifier.padding directly on
     * [CompactButton].
     */
    public val CompactButtonHeight: Dp = CompactButtonTokens.ContainerHeight

    /**
     * The default padding to be provided around a [CompactButton] in order to ensure that its
     * tappable area meets minimum UX guidance.
     */
    public val CompactButtonTapTargetPadding: PaddingValues =
        PaddingValues(top = 8.dp, bottom = 8.dp)

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [Button] or [CompactButton].
     */
    public val IconSpacing: Dp = 6.dp

    /** The default alpha applied to the container when the button is disabled. */
    public val DisabledContainerAlpha: Float = ImageButtonTokens.DisabledContainerOpacity

    /**
     * The default gradient start color when drawing a scrim over an image to ensure that the text
     * text drawn over the image is legible.
     */
    public val scrimGradientStartColor: Color
        @Composable
        get() =
            ImageButtonTokens.BackgroundImageGradientColor.value.copy(
                alpha = ImageButtonTokens.GradientStartOpacity
            )

    /**
     * The default gradient end color when drawing a scrim over an image to ensure that the text
     * text drawn over the image is legible.
     */
    public val scrimGradientEndColor: Color
        @Composable
        get() =
            ImageButtonTokens.BackgroundImageGradientColor.value.copy(
                alpha = ImageButtonTokens.GradientEndOpacity
            )

    private val ColorScheme.defaultFilledTonalButtonColors: ButtonColors
        get() {
            return defaultFilledTonalButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledTonalButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalButtonTokens.LabelColor),
                        secondaryContentColor =
                            fromToken(FilledTonalButtonTokens.SecondaryLabelColor),
                        iconColor = fromToken(FilledTonalButtonTokens.IconColor),
                        disabledContainerColor =
                            fromToken(FilledTonalButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTonalButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledTonalButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTonalButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(FilledTonalButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTonalButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(FilledTonalButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTonalButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultFilledTonalButtonColorsCached = it }
        }

    private val ColorScheme.defaultFilledVariantButtonColors: ButtonColors
        get() {
            return defaultFilledVariantButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledButtonTokens.VariantContainerColor),
                        contentColor = fromToken(FilledButtonTokens.VariantLabelColor),
                        secondaryContentColor =
                            fromToken(FilledButtonTokens.VariantSecondaryLabelColor)
                                .copy(alpha = FilledButtonTokens.VariantSecondaryLabelOpacity),
                        iconColor = fromToken(FilledButtonTokens.VariantIconColor),
                        disabledContainerColor =
                            fromToken(FilledButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultFilledVariantButtonColorsCached = it }
        }

    private val ColorScheme.defaultOutlinedButtonColors: ButtonColors
        get() {
            return defaultOutlinedButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedButtonTokens.LabelColor),
                        secondaryContentColor = fromToken(OutlinedButtonTokens.SecondaryLabelColor),
                        iconColor = fromToken(OutlinedButtonTokens.IconColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = OutlinedButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(OutlinedButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = OutlinedButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(OutlinedButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = OutlinedButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultOutlinedButtonColorsCached = it }
        }

    private val ColorScheme.defaultChildButtonColors: ButtonColors
        get() {
            return defaultChildButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(ChildButtonTokens.LabelColor),
                        secondaryContentColor = fromToken(ChildButtonTokens.SecondaryLabelColor),
                        iconColor = fromToken(ChildButtonTokens.IconColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(ChildButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ChildButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(ChildButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ChildButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(ChildButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ChildButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultChildButtonColorsCached = it }
        }

    private val ColorScheme.defaultButtonColors: ButtonColors
        get() {
            return defaultButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledButtonTokens.LabelColor),
                        secondaryContentColor =
                            fromToken(FilledButtonTokens.SecondaryLabelColor)
                                .copy(alpha = FilledButtonTokens.SecondaryLabelOpacity),
                        iconColor = fromToken(FilledButtonTokens.IconColor),
                        disabledContainerColor =
                            fromToken(FilledButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(FilledButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultButtonColorsCached = it }
        }

    private val ColorScheme.defaultButtonWithContainerPainterColors: ButtonColors
        get() {
            return defaultButtonWithContainerPainterColorsCached
                ?: ButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = fromToken(ImageButtonTokens.ContentColor),
                        secondaryContentColor =
                            fromToken(ImageButtonTokens.SecondaryContentColor)
                                .copy(alpha = ImageButtonTokens.SecondaryContentOpacity),
                        iconColor = fromToken(ImageButtonTokens.IconColor),
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor =
                            fromToken(ImageButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ImageButtonTokens.DisabledContentOpacity
                                ),
                        disabledSecondaryContentColor =
                            fromToken(ImageButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ImageButtonTokens.DisabledContentOpacity
                                ),
                        disabledIconColor =
                            fromToken(ImageButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = ImageButtonTokens.DisabledContentOpacity
                                ),
                    )
                    .also { defaultButtonWithContainerPainterColorsCached = it }
        }

    /**
     * The default width applied for the [CompactButton] when it has no label provided. Note that
     * you can override it by applying Modifier.width directly on [CompactButton].
     */
    internal val IconOnlyCompactButtonWidth = CompactButtonTokens.IconOnlyWidth
}

/**
 * Represents the container and content colors used in buttons in different states.
 *
 * @param containerColor The background color of this [Button] when enabled (overridden by the
 *   containerPainter parameter on Buttons with image backgrounds).
 * @param contentColor The content color of this [Button] when enabled.
 * @param secondaryContentColor The content color of this [Button] when enabled.
 * @param iconColor The content color of this [Button] when enabled.
 * @param disabledContainerColor The background color of this [Button] when not enabled (overridden
 *   by the disabledContainerPainter parameter on Buttons with image backgrounds)
 * @param disabledContentColor The content color of this [Button] when not enabled.
 * @param disabledSecondaryContentColor The content color of this [Button] when not enabled.
 * @param disabledIconColor The content color of this [Button] when not enabled.
 */
@Immutable
public class ButtonColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val secondaryContentColor: Color,
    public val iconColor: Color,
    public val disabledContainerColor: Color,
    public val disabledContentColor: Color,
    public val disabledSecondaryContentColor: Color,
    public val disabledIconColor: Color,
) {
    /**
     * Returns a copy of this [ButtonColors], optionally overriding some of the values.
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The content color of this [Button] when enabled
     * @param iconColor The content color of this [Button] when enabled
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Button] when not enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    public fun copy(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        secondaryContentColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledSecondaryContentColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
    ): ButtonColors =
        ButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            secondaryContentColor.takeOrElse { this.secondaryContentColor },
            iconColor.takeOrElse { this.iconColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
            disabledSecondaryContentColor.takeOrElse { this.disabledSecondaryContentColor },
            disabledIconColor.takeOrElse { this.disabledIconColor },
        )

    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color {
        return if (enabled) containerColor else disabledContainerColor
    }

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color {
        return if (enabled) contentColor else disabledContentColor
    }

    /**
     * Represents the secondary content color for this button, depending on [enabled].
     *
     * @param enabled Whether the button is enabled
     */
    @Stable
    internal fun secondaryContentColor(enabled: Boolean): Color {
        return if (enabled) secondaryContentColor else disabledSecondaryContentColor
    }

    /**
     * Represents the icon color for this button, depending on [enabled].
     *
     * @param enabled Whether the button is enabled
     */
    @Stable
    internal fun iconColor(enabled: Boolean): Color {
        return if (enabled) iconColor else disabledIconColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconColor != other.iconColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconColor != other.disabledIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        return result
    }
}

@Composable
private fun Modifier.buttonSizeModifier(): Modifier =
    this.defaultMinSize(minHeight = ButtonDefaults.Height)

@Composable
private fun Modifier.compactButtonModifier(): Modifier =
    this.height(ButtonDefaults.CompactButtonHeight)

/**
 * Button with label. This allows to use the token values for individual buttons instead of relying
 * on common values.
 */
@Composable
private fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    enabled: Boolean,
    shape: Shape,
    labelFont: TextStyle,
    containerPainter: Painter?,
    disabledContainerPainter: Painter?,
    colors: ButtonColors,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    transformation: SurfaceTransformation?,
    content: @Composable RowScope.() -> Unit,
) {
    val painter =
        if (enabled && containerPainter != null) {
            containerPainter
        } else if (!enabled && disabledContainerPainter != null) {
            disabledContainerPainter
        } else {
            ColorPainter(colors.containerColor(enabled = enabled))
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        // Fill the container height but not its width as buttons have fixed size height but we
        // want them to be able to fit their content
        modifier =
            modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .surface(
                    transformation = transformation,
                    painter = painter,
                    shape = shape,
                    border = border,
                )
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick, // NB CombinedClickable calls LongPress haptic
                    onLongClickLabel = onLongClickLabel,
                    role = Role.Button,
                    indication = ripple(),
                    interactionSource = interactionSource,
                )
                .padding(contentPadding),
        content = provideScopeContent(colors.contentColor(enabled = enabled), labelFont, content),
    )
}

/**
 * Button with icon, label and secondary label. This allows to use the token values for individual
 * buttons instead of relying on common values.
 */
@Composable
private fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    secondaryLabelContent: (@Composable RowScope.() -> Unit)?,
    icon: (@Composable BoxScope.() -> Unit)?,
    enabled: Boolean,
    shape: Shape,
    labelFont: TextStyle,
    containerPainter: Painter?,
    disabledContainerPainter: Painter?,
    colors: ButtonColors,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    transformation: SurfaceTransformation?,
    labelContent: @Composable RowScope.() -> Unit,
) {
    ButtonImpl(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        labelFont = labelFont,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                content = provideScopeContent(colors.iconColor(enabled), icon),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        }
        Column {
            Row(content = labelContent)
            if (secondaryLabelContent != null) {
                Spacer(modifier = Modifier.size(1.dp))
                Row(content = secondaryLabelContent)
            }
        }
    }
}
