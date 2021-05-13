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
package androidx.wear.compose.material

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The most basic chip that provides a single content slot, used as the building block for other
 * chips.
 */
@Composable
fun Chip(
    onClick: () -> Unit,
    colors: ChipColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    role: Role? = Role.Button,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(ChipDefaults.Height),
        color = Color.Transparent,
        shape = shape,
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .paint(
                    painter = colors.background(enabled = enabled).value,
                )

        val contentBoxModifier = Modifier
            .clickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                onClick = onClick,
                role = role,
                indication = indication,
                interactionSource = interactionSource,
            )
            .padding(contentPadding)

        Box(
            modifier = painterModifier
        ) { }
        Box(
            modifier = contentBoxModifier
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.contentColor(enabled = enabled).value,
                content = content
            )
        }
    }
}

/**
 * Represents the background and content colors used in a chip in different states.
 *
 * See [ChipDefaults.primaryChipColors] for the default colors used in a primary styled [Chip].
 * See [ChipDefaults.secondaryChipColors] for the default colors used in a secondary styled [Chip].
 */
@Stable
interface ChipColors {
    @Composable
    fun background(enabled: Boolean): State<Painter>

    /**
     * Represents the content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>

    /**
     * Represents the secondary content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun secondaryContentColor(enabled: Boolean): State<Color>

    /**
     * Represents the icon tint color for this chip, depending on [enabled].
     */
    @Composable
    fun iconTintColor(enabled: Boolean): State<Color>
}

/**
 * Contains the default values used by [Chip]
 */
public object ChipDefaults {

    @Composable
    public fun primaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

    @Composable
    public fun secondaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

    private val ChipHorizontalPadding = 16.dp
    private val ChipVerticalPadding = 6.dp

    /**
     * The default content padding used by [Chip]
     */
    public val ContentPadding = PaddingValues(
        start = ChipHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipHorizontalPadding,
        bottom = ChipVerticalPadding
    )

    /**
     * The default height applied for the [Chip].
     * Note that you can override it by applying Modifier.heightIn directly on [Chip].
     */
    internal val Height = 52.dp

    /**
     * The default size of the icon when used inside a [Chip].
     */
    internal val IconSize = 24.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside a [Chip].
     */
    internal val IconSpacing = 8.dp

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * a [Chip].
     *
     * @param backgroundColor the background color of this [Chip] when enabled
     * @param contentColor the content color of this [Chip] when enabled
     * @param secondaryContentColor the content color of this [Chip] when enabled
     * @param iconTintColor the content color of this [Chip] when enabled
     * @param disabledBackgroundColor the background color of this [Chip] when not enabled
     * @param disabledContentColor the content color of this [Chip] when not enabled
     * @param disabledSecondaryContentColor the content color of this [Chip] when not enabled
     * @param disabledIconTintColor the content color of this [Chip] when not enabled
     */
    @Composable
    fun chipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor,
        disabledBackgroundColor: Color = backgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledContentColor: Color = contentColor.copy(alpha = ContentAlpha.disabled),
        disabledSecondaryContentColor: Color =
            secondaryContentColor.copy(alpha = ContentAlpha.disabled),
        disabledIconTintColor: Color = disabledContentColor,
    ): ChipColors = DefaultChipColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        secondaryContentColor = secondaryContentColor,
        iconTintColor = iconTintColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        disabledSecondaryContentColor = disabledSecondaryContentColor,
        disabledIconTintColor = disabledIconTintColor,
    )
}

/**
 * Default [ChipColors] implementation.
 */
@Immutable
private class DefaultChipColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val iconTintColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledIconTintColor: Color,
) : ChipColors {
    @Composable
    override fun background(enabled: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) ColorPainter(backgroundColor) else ColorPainter(disabledBackgroundColor)
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) contentColor else disabledContentColor
        )
    }

    @Composable
    override fun secondaryContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) secondaryContentColor else disabledSecondaryContentColor
        )
    }

    @Composable
    override fun iconTintColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) iconTintColor else disabledIconTintColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultChipColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconTintColor != other.iconTintColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconTintColor != other.disabledIconTintColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconTintColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconTintColor.hashCode()
        return result
    }
}
