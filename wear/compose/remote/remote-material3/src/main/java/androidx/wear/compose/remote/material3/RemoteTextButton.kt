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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
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
import androidx.wear.compose.material3.TextButtonColors
import androidx.wear.compose.material3.TextButtonDefaults

/**
 * Wear Material [RemoteTextButton] is a circular, text-only button with transparent background and
 * no border. It offers a single slot for text.
 *
 * The default [RemoteTextButton] has no border and a transparent background for low emphasis
 * actions. For actions that require higher emphasis, consider overriding the colors by
 * [RemoteTextButtonColors.copy]
 *
 * [RemoteTextButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of an [RemoteTextButton] with shape animation of rounded corners on press:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextButtonSimpleSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param shape Defines the shape for this button. Defaults to a static shape based on
 *   [RemoteTextButtonDefaults.shape]
 * @param colors [RemoteTextButtonColors] that will be used to resolve the background and content
 *   color for this button in different states.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param content The content displayed on the text button, expected to be text or image.
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public fun RemoteTextButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteTextButtonColors = RemoteTextButtonDefaults.textButtonColors(),
    enabled: RemoteBoolean = true.rb,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteTextButtonDefaults.shape,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteRoundButton(
        onClick = onClick,
        modifier = modifier.size(RemoteTextButtonDefaults.DefaultButtonSize),
        backgroundColor = colors.containerColor(enabled = enabled),
        enabled = enabled,
        border = border,
        borderColor = borderColor,
        shape = shape,
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                RemoteMaterialTheme.typography.labelMedium,
                content,
            ),
    )
}

/** Contains the default values used by [RemoteTextButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTextButtonDefaults {
    /** Recommended [RemoteShape] for [RemoteTextButton]. */
    @Suppress("RestrictedApiAndroidX")
    public val shape: RemoteRoundedCornerShape
        get() = RemoteCircleShape

    /**
     * Returns a [TextButtonColors] for a text button - by default, a transparent background with
     * contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    public fun textButtonColors(): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultTextButtonColors

    /**
     * Returns a [RemoteTextButtonColors] for a text button - by default, a transparent background
     * with contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * @param containerColor the background color of this text button when enabled
     * @param contentColor the content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    public fun textButtonColors(
        containerColor: RemoteColor = RemoteColor(Color.Transparent),
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor = RemoteColor(Color.Transparent),
        disabledContentColor: RemoteColor? = null,
    ): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /** The recommended size for a small button. */
    public val SmallButtonSize: RemoteDp = 48.rdp

    /** The default size applied for buttons. */
    public val DefaultButtonSize: RemoteDp = 52.rdp

    /** The recommended size for a large button. */
    public val LargeButtonSize: RemoteDp = 60.rdp

    /** The recommended text style for a small button. */
    public val smallButtonTextStyle: RemoteTextStyle
        @Composable get() = RemoteMaterialTheme.typography.labelMedium

    /** The default text style applied for buttons. */
    public val defaultButtonTextStyle: RemoteTextStyle
        @Composable get() = RemoteMaterialTheme.typography.labelMedium

    /** The recommended text style for a large button. */
    public val largeButtonTextStyle: RemoteTextStyle
        @Composable get() = RemoteMaterialTheme.typography.labelLarge

    private val RemoteColorScheme.defaultTextButtonColors: RemoteTextButtonColors
        @Composable
        get() =
            RemoteTextButtonColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = onSurface,
                disabledContainerColor = RemoteColor(Color.Transparent),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

/**
 * Represents the container and content colors used in a text button in different states.
 *
 * See [TextButtonDefaults.filledTextButtonColors],
 * [TextButtonDefaults.filledTonalTextButtonColors], [TextButtonDefaults.textButtonColors] and
 * [TextButtonDefaults.outlinedTextButtonColors] for [TextButtonColors] with different levels of
 * emphasis.
 *
 * @param containerColor the background color of this text button when enabled.
 * @param contentColor the content color of this text button when enabled.
 * @param disabledContainerColor the background color of this text button when not enabled.
 * @param disabledContentColor the content color of this text button when not enabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteTextButtonColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
) {
    @Stable
    @SuppressLint("RestrictedApiAndroidX")
    internal fun contentColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = contentColor, ifFalse = disabledContentColor)
    }

    @Stable
    @SuppressLint("RestrictedApiAndroidX")
    internal fun containerColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = containerColor, ifFalse = disabledContainerColor)
    }

    /**
     * Returns a copy of this RemoteTextButtonColors optionally overriding some of the values.
     *
     * @param containerColor the background color of this text button when enabled.
     * @param contentColor the content color of this text button when enabled.
     * @param disabledContainerColor the background color of this text button when not enabled.
     * @param disabledContentColor the content color of this text button when not enabled.
     */
    public fun copy(
        containerColor: RemoteColor? = this.containerColor,
        contentColor: RemoteColor? = this.contentColor,
        disabledContainerColor: RemoteColor? = this.disabledContainerColor,
        disabledContentColor: RemoteColor? = this.disabledContentColor,
    ): RemoteTextButtonColors =
        RemoteTextButtonColors(
            containerColor = containerColor ?: this.containerColor,
            contentColor = contentColor ?: this.contentColor,
            disabledContainerColor = disabledContainerColor ?: this.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: this.disabledContentColor,
        )
}
