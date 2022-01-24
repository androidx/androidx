/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO(b/197880751): Add url to spec on Material.io.
// TODO(b/193431107): Add sample.
/**
 * Material surface is the central metaphor in material design. Each surface exists at a given
 * elevation, which influences how that piece of surface visually relates to other surfaces and how
 * that surface is modified by tonal variance.
 *
 * If you want to have a [Surface] that handles clicks, consider using another overload.
 *
 * The [Surface] is responsible for:
 *
 * 1) Clipping: Surface clips its children to the shape specified by [shape]
 *
 * 2) Borders: If [shape] has a border, then it will also be drawn.
 *
 * 3) Background: Surface fills the shape specified by [shape] with the [color]. If [color] is
 * [ColorScheme.surface] a color overlay will be applied. The color of the overlay depends on the
 * [tonalElevation] of this Surface, and the [LocalAbsoluteTonalElevation] set by any
 * parent surfaces. This ensures that a Surface never appears to have a lower elevation overlay than
 * its ancestors, by summing the elevation of all previous Surfaces.
 *
 * 4) Content color: Surface uses [contentColor] to specify a preferred color for the content of
 * this surface - this is used by the [Text] and [Icon] components as a default color.
 *
 * If no [contentColor] is set, this surface will try and match its background color to a color
 * defined in the theme [ColorScheme], and return the corresponding content color. For example, if
 * the
 * [color] of this surface is [ColorScheme.surface], [contentColor] will be set to
 * [ColorScheme.onSurface]. If [color] is not part of the theme palette, [contentColor] will keep
 * the same value set above this Surface.
 *
 * To modify these default style values used by text, use [ProvideTextStyle] or explicitly pass a
 * new [TextStyle] to your text.
 *
 * To manually retrieve the content color inside a surface, use [LocalContentColor].
 *
 * 5) Blocking touch propagation behind the surface.
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape as well its shadow.
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching content color for [color], or if [color] is not a color from the
 * theme, this will keep the same value set above this Surface.
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 * in a darker color in light theme and lighter color in dark theme.
 * @param shadowElevation The size of the shadow below the surface. To prevent shadow creep, only
 * apply shadow elevation when absolutely necessary, such as when the surface requires visual
 * separation from a patterned background. Note that It will not affect z index of the Surface.
 * If you want to change the drawing order you can use `Modifier.zIndex`.
 * @param border Optional border to draw on top of the surface
 */
@Composable
@NonRestartableComposable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content,
        clickAndSemanticsModifier =
            Modifier.semantics(mergeDescendants = false) {}.pointerInput(Unit) {}
    )
}

/**
 * Material surface is the central metaphor in material design. Each surface exists at a given
 * elevation, which influences how that piece of surface visually relates to other surfaces and how
 * that surface is modified by tonal variance.
 *
 * This version of [Surface] is responsible for a click handling as well al everything else that a
 * regular Surface does:
 *
 * This clickable [Surface] is responsible for:
 *
 * 1) Clipping: Surface clips its children to the shape specified by [shape]
 *
 * 2) Borders: If [shape] has a border, then it will also be drawn.
 *
 * 3) Background: Surface fills the shape specified by [shape] with the [color]. If [color] is
 * [ColorScheme.surface] a color overlay may be applied. The color of the overlay depends on the
 * [tonalElevation] of this Surface, and the [LocalAbsoluteTonalElevation] set by any
 * parent surfaces. This ensures that a Surface never appears to have a lower elevation overlay than
 * its ancestors, by summing the elevation of all previous Surfaces.
 *
 * 4) Content color: Surface uses [contentColor] to specify a preferred color for the content of
 * this surface - this is used by the [Text] and [Icon] components as a default color. If no
 * [contentColor] is set, this surface will try and match its background color to a color defined in
 * the theme [ColorScheme], and return the corresponding content color. For example, if the [color]
 * of this surface is [ColorScheme.surface], [contentColor] will be set to [ColorScheme.onSurface].
 * If [color] is not part of the theme palette, [contentColor] will keep the same value set above
 * this Surface.
 *
 * 6) Click handling. This version of surface will react to the clicks, calling [onClick] lambda,
 * updating the [interactionSource] when [PressInteraction] occurs, and showing [indication] (if it
 * is not `null) in response to press events. If you don't need click handling, consider using the
 * version that doesn't require [onClick] param.
 *
 * 7) Semantics for clicks. Just like with [Modifier.clickable], clickable version of [Surface] will
 * produce semantics to indicate that it is able to be clicked, with [onClickLabel] (if provided),
 * announced by accessibility services.
 *
 * To modify these default style values used by text, use [ProvideTextStyle] or explicitly pass a
 * new [TextStyle] to your text.
 *
 * To manually retrieve the content color inside a surface, use [LocalContentColor].
 *
 * @param onClick callback to be called when the surface is clicked
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape as well its shadow. A shadow is only displayed if the
 * [tonalElevation] is greater than zero.
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching content color for [color], or if [color] is not a color from the
 * theme, this will keep the same value set above this Surface.
 * @param border Optional border to draw on top of the surface
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 * in a darker color in light theme and lighter color in dark theme.
 * @param shadowElevation The size of the shadow below the surface. Note that It will not affect z index
 * of the Surface. If you want to change the drawing order you can use `Modifier.zIndex`.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Surface. You can create and pass in your own remembered [MutableInteractionSource] if
 * you want to observe [Interaction]s and customize the appearance / behavior of this Surface in
 * different [Interaction]s.
 * @param indication indication to be shown when surface is pressed. By default, indication from
 * [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 * [LocalIndication] to show theme default
 * @param enabled Controls the enabled state of the surface. When `false`, this surface will not be
 * clickable
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to
 * describe the element or do customizations. For example, if the Surface acts as a button, you
 * should pass the [Role.Button]
 */
@Composable
@NonRestartableComposable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.minimumTouchTargetSize(),
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content,
        clickAndSemanticsModifier =
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = onClick
            )
    )
}

@Composable
private fun Surface(
    modifier: Modifier,
    shape: Shape,
    color: Color,
    contentColor: Color,
    border: BorderStroke?,
    tonalElevation: Dp, // This will be used to compute surface tonal colors
    shadowElevation: Dp,
    clickAndSemanticsModifier: Modifier,
    content: @Composable () -> Unit
) {
    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation
    val backgroundColor =
        if (color == MaterialTheme.colorScheme.surface) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteElevation)
        } else {
            color
        }
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation
    ) {
        Box(
            modifier
                .shadow(shadowElevation, shape, clip = false)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .background(color = backgroundColor, shape = shape)
                .clip(shape)
                .then(clickAndSemanticsModifier),
            propagateMinConstraints = true
        ) { content() }
    }
}

/**
 * CompositionLocal containing the current absolute elevation provided by [Surface] components. This
 * absolute elevation is a sum of all the previous elevations. Absolute elevation is only used for
 * calculating surface tonal colors, and is *not* used for drawing the shadow in a [Surface].
 */
// TODO(b/179787782): Add sample after catalog app lands in aosp.
val LocalAbsoluteTonalElevation = compositionLocalOf { 0.dp }
