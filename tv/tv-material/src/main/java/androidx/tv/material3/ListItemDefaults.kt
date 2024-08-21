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

package androidx.tv.material3

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.tokens.Elevation

/** Contains the default values used by list items. */
object ListItemDefaults {
    /** The default Icon size used by [ListItem]. */
    val IconSize = 32.dp

    /** The Icon size used by [DenseListItem]. */
    val IconSizeDense = 20.dp

    /** The default elevation used by [ListItem]. */
    val TonalElevation = Elevation.Level0

    /** The default shape for a [ListItem]. */
    private val ListItemShape = RoundedCornerShape(8.dp)

    /** The default border applied to [ListItem] in focused disabled state. */
    private val FocusedDisabledBorder
        @ReadOnlyComposable
        @Composable
        get() =
            Border(border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.border))

    /** The default opacity for the [ListItem] container color in selected state. */
    private const val SelectedContainerColorOpacity = 0.4f

    /** The default content padding [PaddingValues] used by [ListItem] */
    internal val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    /** The default content padding [PaddingValues] used by [DenseListItem] */
    internal val ContentPaddingDense = PaddingValues(horizontal = 12.dp, vertical = 10.dp)

    internal val LeadingContentColor
        @ReadOnlyComposable @Composable get() = LocalContentColor.current.copy(alpha = 0.8f)

    internal const val OverlineContentOpacity = 0.6f
    internal const val SupportingContentOpacity = 0.8f

    internal val LeadingContentEndPadding = 8.dp
    internal val TrailingContentStartPadding = 8.dp

    internal val MinContainerHeight = 48.dp
    internal val MinContainerHeightLeadingContent = 56.dp
    internal val MinContainerHeightTwoLine = 64.dp
    internal val MinContainerHeightThreeLine = 80.dp

    internal val MinDenseContainerHeight = 40.dp
    internal val MinDenseContainerHeightLeadingContent = 40.dp
    internal val MinDenseContainerHeightTwoLine = 56.dp
    internal val MinDenseContainerHeightThreeLine = 72.dp

    /**
     * Creates a [ListItemShape] that represents the default container shapes used in a ListItem
     *
     * @param shape the default shape used when the ListItem is enabled
     * @param focusedShape the shape used when the ListItem is enabled and focused
     * @param pressedShape the shape used when the ListItem is enabled and pressed
     * @param selectedShape the shape used when the ListItem is enabled and selected
     * @param disabledShape the shape used when the ListItem is not enabled
     * @param focusedSelectedShape the shape used when the ListItem is enabled, focused and selected
     * @param focusedDisabledShape the shape used when the ListItem is not enabled and focused
     * @param pressedSelectedShape the shape used when the ListItem is enabled, pressed and selected
     */
    @ReadOnlyComposable
    @Composable
    fun shape(
        shape: Shape = ListItemShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        selectedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedSelectedShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape,
        pressedSelectedShape: Shape = shape
    ) =
        ListItemShape(
            shape = shape,
            focusedShape = focusedShape,
            pressedShape = pressedShape,
            selectedShape = selectedShape,
            disabledShape = disabledShape,
            focusedSelectedShape = focusedSelectedShape,
            focusedDisabledShape = focusedDisabledShape,
            pressedSelectedShape = pressedSelectedShape
        )

    /**
     * Creates a [ListItemColors] that represents the default container & content colors used in a
     * ListItem
     *
     * @param containerColor the default container color used when the ListItem is enabled
     * @param contentColor the default content color used when the ListItem is enabled
     * @param focusedContainerColor the container color used when the ListItem is enabled and
     *   focused
     * @param focusedContentColor the content color used when the ListItem is enabled and focused
     * @param pressedContainerColor the container color used when the ListItem is enabled and
     *   pressed
     * @param pressedContentColor the content color used when the ListItem is enabled and pressed
     * @param selectedContainerColor the container color used when the ListItem is enabled and
     *   selected
     * @param selectedContentColor the content color used when the ListItem is enabled and selected
     * @param disabledContainerColor the container color used when the ListItem is not enabled
     * @param disabledContentColor the content color used when the ListItem is not enabled
     * @param focusedSelectedContainerColor the container color used when the ListItem is enabled,
     *   focused and selected
     * @param focusedSelectedContentColor the content color used when the ListItem is enabled,
     *   focused and selected
     * @param pressedSelectedContainerColor the container color used when the ListItem is enabled,
     *   pressed and selected
     * @param pressedSelectedContentColor the content color used when the ListItem is enabled,
     *   pressed and selected
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor: Color = contentColorFor(focusedContainerColor),
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = contentColorFor(focusedContainerColor),
        selectedContainerColor: Color =
            MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = SelectedContainerColorOpacity
            ),
        selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedSelectedContainerColor: Color = focusedContainerColor,
        focusedSelectedContentColor: Color = focusedContentColor,
        pressedSelectedContainerColor: Color = pressedContainerColor,
        pressedSelectedContentColor: Color = pressedContentColor
    ) =
        ListItemColors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = focusedContentColor,
            pressedContainerColor = pressedContainerColor,
            pressedContentColor = pressedContentColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            focusedSelectedContainerColor = focusedSelectedContainerColor,
            focusedSelectedContentColor = focusedSelectedContentColor,
            pressedSelectedContainerColor = pressedSelectedContainerColor,
            pressedSelectedContentColor = pressedSelectedContentColor
        )

    /**
     * Creates a [ListItemScale] that represents the default scales used in a ListItem. scales are
     * used to modify the size of a composable in different [Interaction] states e.g. 1f (original)
     * in default state, 1.2f (scaled up) in focused state, 0.8f (scaled down) in pressed state,
     * etc.
     *
     * @param scale the scale used when the ListItem is enabled.
     * @param focusedScale the scale used when the ListItem is enabled and focused.
     * @param pressedScale the scale used when the ListItem is enabled and pressed.
     * @param selectedScale the scale used when the ListItem is enabled and selected.
     * @param disabledScale the scale used when the ListItem is not enabled.
     * @param focusedSelectedScale the scale used when the ListItem is enabled, focused and
     *   selected.
     * @param focusedDisabledScale the scale used when the ListItem is not enabled and focused.
     * @param pressedSelectedScale the scale used when the ListItem is enabled, pressed and
     *   selected.
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.05f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) selectedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedSelectedScale: Float = focusedScale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) pressedSelectedScale: Float = scale
    ) =
        ListItemScale(
            scale = scale,
            focusedScale = focusedScale,
            pressedScale = pressedScale,
            selectedScale = selectedScale,
            disabledScale = disabledScale,
            focusedSelectedScale = focusedSelectedScale,
            focusedDisabledScale = focusedDisabledScale,
            pressedSelectedScale = pressedSelectedScale
        )

    /**
     * Creates a [ListItemBorder] that represents the default [Border]s applied on a ListItem in
     * different [Interaction] states
     *
     * @param border the default [Border] used when the ListItem is enabled
     * @param focusedBorder the [Border] used when the ListItem is enabled and focused
     * @param pressedBorder the [Border] used when the ListItem is enabled and pressed
     * @param selectedBorder the [Border] used when the ListItem is enabled and selected
     * @param disabledBorder the [Border] used when the ListItem is not enabled
     * @param focusedSelectedBorder the [Border] used when the ListItem is enabled, focused and
     *   selected
     * @param focusedDisabledBorder the [Border] used when the ListItem is not enabled and focused
     * @param pressedSelectedBorder the [Border] used when the ListItem is enabled, pressed and
     *   selected
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = border,
        pressedBorder: Border = focusedBorder,
        selectedBorder: Border = border,
        disabledBorder: Border = border,
        focusedSelectedBorder: Border = focusedBorder,
        focusedDisabledBorder: Border = FocusedDisabledBorder,
        pressedSelectedBorder: Border = border
    ) =
        ListItemBorder(
            border = border,
            focusedBorder = focusedBorder,
            pressedBorder = pressedBorder,
            selectedBorder = selectedBorder,
            disabledBorder = disabledBorder,
            focusedSelectedBorder = focusedSelectedBorder,
            focusedDisabledBorder = focusedDisabledBorder,
            pressedSelectedBorder = pressedSelectedBorder
        )

    /**
     * Creates a [ListItemGlow] that represents the default [Glow]s used in a ListItem.
     *
     * @param glow the [Glow] used when the ListItem is enabled, and has no other [Interaction]s.
     * @param focusedGlow the [Glow] used when the ListItem is enabled and focused.
     * @param pressedGlow the [Glow] used when the ListItem is enabled and pressed.
     * @param selectedGlow the [Glow] used when the ListItem is enabled and selected.
     * @param focusedSelectedGlow the [Glow] used when the ListItem is enabled, focused and
     *   selected.
     * @param pressedSelectedGlow the [Glow] used when the ListItem is enabled, pressed and
     *   selected.
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow,
        selectedGlow: Glow = glow,
        focusedSelectedGlow: Glow = focusedGlow,
        pressedSelectedGlow: Glow = glow
    ) =
        ListItemGlow(
            glow = glow,
            focusedGlow = focusedGlow,
            pressedGlow = pressedGlow,
            selectedGlow = selectedGlow,
            focusedSelectedGlow = focusedSelectedGlow,
            pressedSelectedGlow = pressedSelectedGlow
        )
}
