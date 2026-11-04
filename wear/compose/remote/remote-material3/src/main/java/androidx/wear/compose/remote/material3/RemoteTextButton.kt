/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Wear Material [RemoteTextButton] is a circular, text-only button with transparent background and
 * no border. It offers a single slot for text.
 *
 * The default [RemoteTextButton] has no border and a transparent background for low emphasis
 * actions. For actions that require higher emphasis, consider
 * [RemoteTextButtonDefaults.filledTonalTextButtonColors] or overriding the colors via
 * [RemoteTextButtonColors.copy].
 *
 * [RemoteTextButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of a [RemoteTextButton] with customized colors:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextButtonSimpleSample
 *
 * Example of a filled tonal [RemoteTextButton]:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample
 *
 * Example of a [RemoteTextButton] with corner animation on press:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextButtonWithCornerAnimationSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param shapes Defines the shape for this text button, including a pressed shape when animated.
 *   Defaults to a static shape based on [RemoteTextButtonDefaults.shape], or use
 *   [RemoteTextButtonDefaults.animatedShapes] to animate the corners on press. The press animation
 *   is driven by touch events on the player, and is not shown by players that don't dispatch them.
 * @param colors [RemoteTextButtonColors] that will be used to resolve the background and content
 *   color for this button in different states.
 * @param border Optional [RemoteBorderStroke] that will be used to resolve the border for this
 *   button in different states, or `null` for no border.
 * @param content The content displayed on the text button, expected to be text or image.
 */
@Composable
@RemoteComposable
public fun RemoteTextButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shapes: RemoteTextButtonShapes = RemoteTextButtonDefaults.shapes(),
    colors: RemoteTextButtonColors = RemoteTextButtonDefaults.textButtonColors(),
    border: RemoteBorderStroke? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteRoundButton(
        onClick = onClick,
        modifier = modifier.size(RemoteTextButtonDefaults.DefaultButtonSize),
        backgroundColor = colors.containerColor(enabled = enabled),
        enabled = enabled,
        border = border,
        shape = shapes.shape,
        pressedShape = shapes.pressedShape,
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                RemoteMaterialTheme.typography.labelMedium,
                content,
            ),
    )
}

/** Contains the default values used by [RemoteTextButton]. */
public object RemoteTextButtonDefaults {
    /** Recommended [RemoteShape] for [RemoteTextButton]. */
    public val shape: RemoteRoundedCornerShape = RemoteCircleShape

    /** Recommended pressed [RemoteShape] for [RemoteTextButton]. */
    public val pressedShape: RemoteCornerBasedShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.small

    /**
     * Returns the default [RemoteTextButtonShapes] for a static [RemoteTextButton].
     *
     * @return a [RemoteTextButtonShapes] with the default static [shape]
     */
    @Composable @RemoteComposable public fun shapes(): RemoteTextButtonShapes = shapes(shape)

    /**
     * Returns a [RemoteTextButtonShapes] for a static [RemoteTextButton].
     *
     * @param shape The normal shape of the [RemoteTextButton].
     * @return a [RemoteTextButtonShapes] with the given static [shape]
     */
    @Composable
    @RemoteComposable
    public fun shapes(shape: RemoteShape): RemoteTextButtonShapes =
        RemoteTextButtonShapes(shape = shape)

    /**
     * Returns a [RemoteTextButtonShapes] with an animation between two [RemoteCornerBasedShape]s
     * when pressed.
     *
     * The press animation requires both shapes to be [RemoteRoundedCornerShape]s. It is driven by
     * touch events on the player, and is not shown by players that don't dispatch them.
     *
     * Example of a text button animated when pressed:
     *
     * @sample androidx.wear.compose.remote.material3.samples.RemoteTextButtonWithCornerAnimationSample
     * @param shape The normal shape of the [RemoteTextButton].
     * @param pressedShape The pressed shape of the [RemoteTextButton].
     * @return a [RemoteTextButtonShapes] with the configured resting and pressed shapes
     */
    @Composable
    @RemoteComposable
    public fun animatedShapes(
        shape: RemoteCornerBasedShape = this.shape,
        pressedShape: RemoteCornerBasedShape = this.pressedShape,
    ): RemoteTextButtonShapes = RemoteTextButtonShapes(shape = shape, pressedShape = pressedShape)

    /**
     * Returns a [RemoteTextButtonColors] for a text button - by default, a transparent background
     * with contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * @return a [RemoteTextButtonColors] with the default colors for a [RemoteTextButton]
     */
    @Composable
    @RemoteComposable
    public fun textButtonColors(): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultTextButtonColors

    /**
     * Returns a [RemoteTextButtonColors] for a text button - by default, a transparent background
     * with contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * @param containerColor The background color of this text button when enabled, or `null` to use
     *   the theme default.
     * @param contentColor The content color of this text button when enabled, or `null` to use the
     *   theme default.
     * @param disabledContainerColor The background color of this text button when not enabled, or
     *   `null` to use the theme default.
     * @param disabledContentColor The content color of this text button when not enabled, or `null`
     *   to use the theme default.
     * @return a [RemoteTextButtonColors] with the configured colors for a [RemoteTextButton]
     */
    @Composable
    @RemoteComposable
    public fun textButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteTextButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultTextButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    /**
     * Returns a [RemoteTextButtonColors] for a filled tonal [RemoteTextButton] - by default, a
     * muted colored background with a contrasting content color. If the button is disabled then the
     * colors default to [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of a [RemoteTextButton] with [filledTonalTextButtonColors]:
     *
     * @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample
     */
    @Composable
    @RemoteComposable
    public fun filledTonalTextButtonColors(): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledTonalTextButtonColors

    /**
     * Returns a [RemoteTextButtonColors] for a filled tonal [RemoteTextButton] - by default, a
     * muted colored background with a contrasting content color. If the button is disabled then the
     * colors default to [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of a [RemoteTextButton] with [filledTonalTextButtonColors]:
     *
     * @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample
     * @param containerColor the background color of this text button when enabled, or `null` to use
     *   the default ([RemoteColorScheme.surfaceContainer]).
     * @param contentColor the content color of this text button when enabled, or `null` to use the
     *   default ([RemoteColorScheme.onSurface]).
     * @param disabledContainerColor the background color of this text button when not enabled, or
     *   `null` to use the default ([RemoteColorScheme.onSurface] with 12% opacity).
     * @param disabledContentColor the content color of this text button when not enabled, or `null`
     *   to use the default ([RemoteColorScheme.onSurface] with 38% opacity).
     */
    @Composable
    @RemoteComposable
    public fun filledTonalTextButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteTextButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledTonalTextButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    /** The recommended size for a small button. */
    public val SmallButtonSize: RemoteDp = 48.rdp

    /** The default size applied for buttons. */
    public val DefaultButtonSize: RemoteDp = 52.rdp

    /** The recommended size for a large button. */
    public val LargeButtonSize: RemoteDp = 60.rdp

    /** The recommended text style for a small button. */
    public val smallButtonTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

    /** The default text style applied for buttons. */
    public val defaultButtonTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

    /** The recommended text style for a large button. */
    public val largeButtonTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelLarge

    private val RemoteColorScheme.defaultTextButtonColors: RemoteTextButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteTextButtonColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = onSurface,
                disabledContainerColor = RemoteColor(Color.Transparent),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )

    private val RemoteColorScheme.defaultFilledTonalTextButtonColors: RemoteTextButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteTextButtonColors(
                containerColor = surfaceContainer,
                contentColor = onSurface,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

/**
 * Represents the container and content colors used in a text button in different states.
 *
 * See [RemoteTextButtonDefaults.textButtonColors] and
 * [RemoteTextButtonDefaults.filledTonalTextButtonColors] for the default colors used in a
 * [RemoteTextButton].
 *
 * @param containerColor the background color of this text button when enabled.
 * @param contentColor the content color of this text button when enabled.
 * @param disabledContainerColor the background color of this text button when not enabled.
 * @param disabledContentColor the content color of this text button when not enabled.
 */
@Immutable
public class RemoteTextButtonColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
) {
    @Stable
    internal fun contentColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = contentColor, ifFalse = disabledContentColor)
    }

    @Stable
    internal fun containerColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = containerColor, ifFalse = disabledContainerColor)
    }

    /**
     * Returns a copy of this [RemoteTextButtonColors] optionally overriding some of the values.
     *
     * @param containerColor the background color of this text button when enabled.
     * @param contentColor the content color of this text button when enabled.
     * @param disabledContainerColor the background color of this text button when not enabled.
     * @param disabledContentColor the content color of this text button when not enabled.
     * @return a copy of this [RemoteTextButtonColors] with the specified values overridden
     */
    public fun copy(
        containerColor: RemoteColor = this.containerColor,
        contentColor: RemoteColor = this.contentColor,
        disabledContainerColor: RemoteColor = this.disabledContainerColor,
        disabledContentColor: RemoteColor = this.disabledContentColor,
    ): RemoteTextButtonColors =
        RemoteTextButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
}

/**
 * Represents the shapes used for [RemoteTextButton] in various states.
 *
 * When [pressedShape] differs from [shape], [RemoteTextButton] animates to [pressedShape] while it
 * is pressed. The press animation requires both shapes to be [RemoteRoundedCornerShape]s and the
 * text button to be enabled with a constant value.
 *
 * @param shape the shape of the text button when not pressed.
 * @param pressedShape the shape of the text button when pressed.
 */
@Immutable
public class RemoteTextButtonShapes(
    public val shape: RemoteShape,
    public val pressedShape: RemoteShape = shape,
) {
    /**
     * Returns a copy of this [RemoteTextButtonShapes], optionally overriding some of the values.
     *
     * @param shape the shape of the text button when not pressed.
     * @param pressedShape the shape of the text button when pressed.
     * @return a copy of this [RemoteTextButtonShapes] with the specified values overridden
     */
    public fun copy(
        shape: RemoteShape = this.shape,
        pressedShape: RemoteShape = this.pressedShape,
    ): RemoteTextButtonShapes = RemoteTextButtonShapes(shape = shape, pressedShape = pressedShape)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteTextButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        return result
    }
}
