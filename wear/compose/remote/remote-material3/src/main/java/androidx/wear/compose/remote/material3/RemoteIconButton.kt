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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Wear Material [RemoteIconButton] is a circular, icon-only button with transparent background and
 * no border. It offers a single slot to take icon or image content.
 *
 * The default [RemoteIconButton] has no border and a transparent background for low emphasis
 * actions. For actions that require higher emphasis, consider overriding the colors by
 * [RemoteIconButtonColors.copy]
 *
 * [RemoteIconButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of an [RemoteIconButton] with shape animation of rounded corners on press:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIconButtonSimpleSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param shape Defines the shape for this button. Defaults to a static shape based on
 *   [RemoteIconButtonDefaults.shape]
 * @param colors [RemoteIconButtonColors] that will be used to resolve the background and content
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
public fun RemoteIconButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteIconButtonColors = RemoteIconButtonDefaults.iconButtonColors(),
    enabled: RemoteBoolean = true.rb,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteIconButtonDefaults.shape,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteRoundButton(
        onClick = onClick,
        modifier = modifier.size(RemoteIconButtonDefaults.DefaultButtonSize),
        backgroundColor = colors.containerColor(enabled = enabled),
        enabled = enabled,
        border = border,
        borderColor = borderColor,
        shape = shape,
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                LocalRemoteTypography.current.labelMedium,
                content,
            ),
    )
}

/** Contains the default values used by [RemoteIconButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteIconButtonDefaults {
    /** Recommended [RemoteShape] for [RemoteIconButton]. */
    @Suppress("RestrictedApiAndroidX")
    public val shape: RemoteRoundedCornerShape
        get() = RemoteCircleShape

    /** Recommended alpha to apply to an IconButton with Image content with disabled */
    public val DisabledImageOpacity: RemoteFloat = DisabledContentAlpha.rf

    /**
     * Returns a [iconButtonColors] for a text button - by default, a transparent background with
     * contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    public fun iconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultIconButtonColors

    /**
     * Returns a [RemoteIconButtonColors] for a text button - by default, a transparent background
     * with contrasting content color. If the button is disabled then the colors default to
     * [RemoteColorScheme.onSurface] with suitable alpha values applied.
     *
     * @param containerColor the background color of this text button when enabled
     * @param contentColor the content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    public fun iconButtonColors(
        containerColor: RemoteColor = RemoteColor(Color.Transparent),
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor = RemoteColor(Color.Transparent),
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Recommended icon size for a given icon button size.
     *
     * Ensures that the minimum recommended icon size is applied.
     *
     * Examples: for size [LargeButtonSize], returns [LargeIconSize], for size
     * [ExtraSmallButtonSize] returns [SmallIconSize].
     *
     * @param buttonSize The size of the icon button
     */
    @SuppressLint("RestrictedApiAndroidX")
    public fun iconSizeFor(buttonSize: RemoteDp): RemoteDp =
        buttonSize.value
            .gt(LargeButtonSize.value)
            .select(
                ifTrue = LargeIconSize.value,
                ifFalse = max(SmallIconSize.value, buttonSize.value / 2f),
            )
            .asRemoteDp()

    /**
     * The recommended size of an icon when used inside an icon button with size [SmallButtonSize]
     * or [ExtraSmallButtonSize]. Use [iconSizeFor] to easily determine the icon size.
     */
    public val SmallIconSize: RemoteDp = 24.rdp

    /**
     * The default size of an icon when used inside an icon button of size DefaultButtonSize. Use
     * [iconSizeFor] to easily determine the icon size.
     */
    public val DefaultIconSize: RemoteDp = 26.rdp

    /**
     * The size of an icon when used inside an icon button with size [LargeButtonSize]. Use
     * [iconSizeFor] to easily determine the icon size.
     */
    public val LargeIconSize: RemoteDp = 32.rdp

    /** The recommended size for a small button. */
    public val SmallButtonSize: RemoteDp = 48.rdp

    /** The default size applied for buttons. */
    public val DefaultButtonSize: RemoteDp = 52.rdp

    /** The recommended size for a large button. */
    public val LargeButtonSize: RemoteDp = 60.rdp

    /** The recommended background size of an extra small, compact button. */
    public val ExtraSmallButtonSize: RemoteDp = 32.rdp

    private val RemoteColorScheme.defaultIconButtonColors: RemoteIconButtonColors
        @Composable
        get() =
            RemoteIconButtonColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = onSurface,
                disabledContainerColor = RemoteColor(Color.Transparent),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

/**
 * Represents the container and content colors used in a text button in different states.
 *
 * @param containerColor the background color of this text button when enabled.
 * @param contentColor the content color of this text button when enabled.
 * @param disabledContainerColor the background color of this text button when not enabled.
 * @param disabledContentColor the content color of this text button when not enabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteIconButtonColors(
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
     * Returns a copy of this RemoteIconButtonColors optionally overriding some of the values.
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
    ): RemoteIconButtonColors =
        RemoteIconButtonColors(
            containerColor = containerColor ?: this.containerColor,
            contentColor = contentColor ?: this.contentColor,
            disabledContainerColor = disabledContainerColor ?: this.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: this.disabledContentColor,
        )
}
