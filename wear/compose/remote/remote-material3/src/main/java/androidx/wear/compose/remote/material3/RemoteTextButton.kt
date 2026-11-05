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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.RemoteAction
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

/**
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextButtonSimpleSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample
 */
@Composable
@RemoteComposable
public fun RemoteTextButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteTextButtonDefaults.shape,
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
        shape = shape,
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                RemoteMaterialTheme.typography.labelMedium,
                content,
            ),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTextButtonDefaults {
    public val shape: RemoteRoundedCornerShape = RemoteCircleShape

    @Composable
    @RemoteComposable
    public fun textButtonColors(): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultTextButtonColors

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

    /** @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample */
    @Composable
    @RemoteComposable
    public fun filledTonalTextButtonColors(): RemoteTextButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledTonalTextButtonColors

    /** @sample androidx.wear.compose.remote.material3.samples.RemoteFilledTonalTextButtonSample */
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

    public val SmallButtonSize: RemoteDp = 48.rdp

    public val DefaultButtonSize: RemoteDp = 52.rdp

    public val LargeButtonSize: RemoteDp = 60.rdp

    public val smallButtonTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

    public val defaultButtonTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
