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
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
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

/** @sample androidx.wear.compose.remote.material3.samples.RemoteIconButtonSimpleSample */
@Composable
@RemoteComposable
public fun RemoteIconButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteIconButtonDefaults.shape,
    colors: RemoteIconButtonColors = RemoteIconButtonDefaults.iconButtonColors(),
    border: RemoteBorderStroke? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteRoundButton(
        onClick = onClick,
        modifier =
            modifier.defaultMinSize(
                minWidth = RemoteIconButtonDefaults.DefaultButtonSize,
                minHeight = RemoteIconButtonDefaults.DefaultButtonSize,
            ),
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
public object RemoteIconButtonDefaults {
    public val shape: RemoteRoundedCornerShape = RemoteCircleShape

    public val DisabledImageOpacity: RemoteFloat = DisabledContentAlpha.rf

    @Composable
    @RemoteComposable
    public fun filledIconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledIconButtonColors

    @Composable
    @RemoteComposable
    public fun filledIconButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledIconButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun filledTonalIconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledTonalIconButtonColors

    @Composable
    @RemoteComposable
    public fun filledTonalIconButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledTonalIconButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun filledVariantIconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledVariantIconButtonColors

    @Composable
    @RemoteComposable
    public fun filledVariantIconButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledVariantIconButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun outlinedIconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultOutlinedIconButtonColors

    @Composable
    @RemoteComposable
    public fun outlinedIconButtonColors(
        contentColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultOutlinedIconButtonColors
        return default.copy(
            contentColor = contentColor ?: default.contentColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun iconButtonColors(): RemoteIconButtonColors =
        RemoteMaterialTheme.colorScheme.defaultIconButtonColors

    @Composable
    @RemoteComposable
    public fun iconButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
    ): RemoteIconButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultIconButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
        )
    }

    public fun iconSizeFor(buttonSize: RemoteDp): RemoteDp =
        buttonSize
            .isGreaterThanOrEqualTo(LargeButtonSize)
            .select(ifTrue = LargeIconSize, ifFalse = max(SmallIconSize, buttonSize / 2f.rf))

    public val SmallIconSize: RemoteDp = 24.rdp

    public val DefaultIconSize: RemoteDp = 26.rdp

    public val LargeIconSize: RemoteDp = 32.rdp

    public val SmallButtonSize: RemoteDp = 48.rdp

    public val DefaultButtonSize: RemoteDp = 52.rdp

    public val LargeButtonSize: RemoteDp = 60.rdp

    public val ExtraSmallButtonSize: RemoteDp = 32.rdp

    internal val RemoteColorScheme.defaultFilledIconButtonColors: RemoteIconButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteIconButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )

    internal val RemoteColorScheme.defaultFilledVariantIconButtonColors: RemoteIconButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteIconButtonColors(
                containerColor = primaryContainer,
                contentColor = onPrimaryContainer,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )

    internal val RemoteColorScheme.defaultFilledTonalIconButtonColors: RemoteIconButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteIconButtonColors(
                containerColor = surfaceContainer,
                contentColor = primary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )

    internal val RemoteColorScheme.defaultOutlinedIconButtonColors: RemoteIconButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteIconButtonColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = primary,
                disabledContainerColor = RemoteColor(Color.Transparent),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )

    internal val RemoteColorScheme.defaultIconButtonColors: RemoteIconButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteIconButtonColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = primary,
                disabledContainerColor = RemoteColor(Color.Transparent),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteIconButtonColors(
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
    ): RemoteIconButtonColors =
        RemoteIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
}
