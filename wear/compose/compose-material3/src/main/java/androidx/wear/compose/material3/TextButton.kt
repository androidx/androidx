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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Wear Material [TextButton] is a circular, text-only button with transparent background and
 * no border. It offers a single slot for text.
 *
 * Set the size of the [TextButton] with [Modifier.touchTargetAwareSize]
 * to ensure that the recommended minimum touch target size is available.
 *
 * The recommended [TextButton] sizes are [TextButtonDefaults.DefaultButtonSize],
 * [TextButtonDefaults.LargeButtonSize], [TextButtonDefaults.SmallButtonSize] and
 * [TextButtonDefaults.ExtraSmallButtonSize].
 *
 * The default [TextButton] has no border and a transparent background for low emphasis actions.
 * For actions that require high emphasis, set [colors] to
 * [TextButtonDefaults.filledTextButtonColors].
 * For a medium-emphasis, outlined [TextButton], set [border] to
 * [ButtonDefaults.outlinedButtonBorder].
 * For a middle ground between outlined and filled, set [colors] to
 * [TextButtonDefaults.filledTonalTextButtonColors].
 *
 * [TextButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * Example of a [TextButton]:
 * @sample androidx.wear.compose.material3.samples.TextButtonSample
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the text button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 Theme.
 * @param colors [TextButtonColors] that will be used to resolve the background and content color
 * for this button in different states.
 * @param border Optional [BorderStroke] that will be used to resolve the text button border in
 * different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the text button, expected to be text or image.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = TextButtonDefaults.shape,
    colors: TextButtonColors = TextButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Button(
        onClick = onClick,
        modifier.minimumInteractiveComponentSize(),
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shape = shape,
        border = { rememberUpdatedState(border) },
        buttonSize = TextButtonDefaults.DefaultButtonSize,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.buttonMedium,
            content
        )
    )
}

/**
 * Contains the default values used by [TextButton].
 */
object TextButtonDefaults {
    /**
     * Recommended [Shape] for [TextButton].
     */
    val shape = CircleShape

    /**
     * Creates a [TextButtonColors] with the colors for a filled [TextButton]- by default, a
     * colored background with a contrasting content color.
     * If the text button is disabled then the colors will default to [ColorScheme.onSurface] with
     * suitable alpha values applied.
     *
     * Example of [TextButton] with [filledTextButtonColors]:
     * @sample androidx.wear.compose.material3.samples.FilledTextButtonSample
     *
     * @param containerColor The background color of this text button when enabled
     * @param contentColor The content color of this text button when enabled
     */
    @Composable
    fun filledTextButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    ): TextButtonColors {
        return textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [TextButtonColors] with the colors for a filled, tonal [TextButton]- by default,
     * a muted colored background with a contrasting content color.
     * If the text button is disabled then the colors will default to [ColorScheme.onSurface] with
     * suitable alpha values applied.
     *
     * Example of [TextButton] with [filledTonalTextButtonColors]:
     * @sample androidx.wear.compose.material3.samples.FilledTonalTextButtonSample
     *
     * @param containerColor The background color of this text button when enabled
     * @param contentColor The content color of this text button when enabled
     */
    @Composable
    fun filledTonalTextButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
    ): TextButtonColors {
        return textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [TextButtonColors] with the colors for an outlined [TextButton]- by default,
     * a transparent background with contrasting content color. If the button is disabled,
     * then the colors will default to [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of [TextButton] with [outlinedTextButtonColors] and
     * [ButtonDefaults.outlinedButtonBorder]:
     * @sample androidx.wear.compose.material3.samples.OutlinedTextButtonSample
     *
     * @param contentColor The content color of this text button when enabled
     */
    @Composable
    fun outlinedTextButtonColors(
        contentColor: Color = MaterialTheme.colorScheme.onBackground,
    ): TextButtonColors {
        return textButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [TextButtonColors] for a text button - by default, a transparent
     * background with contrasting content color. If the button is disabled
     * then the colors default to [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * @param containerColor the background color of this text button when enabled
     * @param contentColor the content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.toDisabledColor(
            disabledAlpha = DisabledBorderAndContainerAlpha
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.toDisabledColor()
    ): TextButtonColors = TextButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * The recommended background size of an extra small, compact button.
     * It is recommended to apply this size using [Modifier.touchTargetAwareSize].
     */
    val ExtraSmallButtonSize = 32.dp

    /**
     * The recommended size for a small button.
     * It is recommended to apply this size using [Modifier.touchTargetAwareSize].
     */
    val SmallButtonSize = 48.dp

    /**
     * The default size applied for buttons.
     * It is recommended to apply this size using [Modifier.touchTargetAwareSize].
     */
    val DefaultButtonSize = 52.dp

    /**
     * The recommended size for a large button.
     * It is recommended to apply this size using [Modifier.touchTargetAwareSize].
     */
    val LargeButtonSize = 60.dp
}

/**
 * Represents the container and content colors used in a text button in different states.
 *
 * See [TextButtonDefaults.filledTextButtonColors],
 * [TextButtonDefaults.filledTonalTextButtonColors], [TextButtonDefaults.textButtonColors] and
 * [TextButtonDefaults.outlinedTextButtonColors] for [TextButtonColors] with different levels
 * of emphasis.
 *
 * @param containerColor the background color of this text button when enabled.
 * @param contentColor the content color of this text button when enabled.
 * @param disabledContainerColor the background color of this text button when not enabled.
 * @param disabledContentColor the content color of this text button when not enabled.
 */
@Immutable
class TextButtonColors constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Represents the container color for this text button, depending on [enabled].
     *
     * @param enabled whether the text button is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    /**
     * Represents the content color for this text button, depending on [enabled].
     *
     * @param enabled whether the text button is enabled
     */
    @Composable
    internal fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()

        return result
    }
}
