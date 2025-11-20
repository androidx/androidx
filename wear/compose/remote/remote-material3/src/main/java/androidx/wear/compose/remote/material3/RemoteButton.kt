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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.graphics.Paint
import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawWithContentScope
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.remoteComponentHeight
import androidx.compose.remote.creation.compose.layout.remoteComponentWidth
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Base level Wear Material3 [RemoteButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [RemoteButton] components that take specific content such as
 * icons and labels.
 *
 * [RemoteButton] takes the [RemoteButtonDefaults.buttonColors] color scheme by default, with
 * colored background, contrasting content color and no border. This is a high-emphasis button for
 * the primary, most important or most common action on a screen.
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public fun RemoteButton(
    vararg onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = RemoteBoolean(true),
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    shape: Shape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        enabled = enabled,
        colors = colors,
        shape = shape,
        contentPadding = contentPadding,
        border = border,
        borderColor = borderColor,
        labelFont = RemoteMaterialTheme.typography.typography.labelMedium,
        content = content,
    )
}

/**
 * Button with label. This allows to use the token values for individual buttons instead of relying
 * on common values.
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
private fun RemoteButtonImpl(
    vararg onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteButtonColors,
    enabled: RemoteBoolean,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    shape: Shape,
    contentPadding: RemotePaddingValues,
    labelFont: TextStyle,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    // TODO: follow-up with capability API
    val supportClip = false
    val state = LocalRemoteComposeCreationState.current
    val containerModifier =
        RemoteModifier.clickable(*onClick, enabled = enabled.constantValue ?: false)
            .then(
                if (supportClip) {
                    RemoteModifier.background(colors.containerColor(enabled))
                        // TODO: set border when border shape is supported.
                        .clip(shape)
                } else {
                    RemoteModifier
                }
            )
            .padding(
                left = contentPadding.leftPadding.value,
                top = contentPadding.topPadding.value,
                right = contentPadding.rightPadding.value,
                bottom = contentPadding.bottomPadding.value,
            )

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = RemoteArrangement.CenterHorizontally,
        modifier =
            modifier
                .drawWithContent {
                    if (!supportClip) {
                        drawShapedBackground(
                            shape,
                            colors.containerColor(enabled),
                            borderColor = borderColor,
                            borderStrokeWidth = border?.value,
                            state,
                        )
                    }
                    drawContent()
                }
                .then(containerModifier),
        content = content, // TODO: handle labelFont and content color for content
    )
}

/** Contains the default values used by [RemoteButton] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public object RemoteButtonDefaults {
    /** Recommended [RoundedCornerShape] for [RemoteButton]. */
    public val shape: RoundedCornerShape
        @Composable get() = RoundedCornerShape(16.0.dp)

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteButton].
     */
    @Composable
    public fun buttonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonColors

    /** The default minimum height applied for the [RemoteButton]. */
    public val Height: Dp = 52.dp

    /** The default minimum width applied for the [RemoteButton]. */
    public val Width: Dp = 12.dp

    /** The recommended horizontal padding used by [RemoteButton] by default */
    public val ButtonHorizontalPadding: RemoteDp = RemoteDp(14f.rf)

    /** The recommended vertical padding used by [RemoteButton] by default */
    public val ButtonVerticalPadding: RemoteDp = RemoteDp(6f.rf)

    /** The default content padding used by [RemoteButton] */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = ButtonHorizontalPadding, vertical = ButtonVerticalPadding)

    private val RemoteColorScheme.defaultButtonColors: RemoteButtonColors
        @Composable
        get() {
            return RemoteButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                secondaryContentColor = onPrimary.copy(alpha = 0.8f.rf),
                iconColor = onPrimary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }
}

/**
 * Represents the container and content colors used in buttons in different states.
 *
 * @param containerColor The background color of this [RemoteButton] when enabled (overridden by the
 *   containerPainter parameter on Buttons with image backgrounds).
 * @param contentColor The content color of this [RemoteButton] when enabled.
 * @param secondaryContentColor The content color of this [RemoteButton] when enabled.
 * @param iconColor The content color of this [RemoteButton] when enabled.
 * @param disabledContainerColor The background color of this [RemoteButton] when not enabled
 *   (overridden by the disabledContainerPainter parameter on Buttons with image backgrounds)
 * @param disabledContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledSecondaryContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledIconColor The content color of this [RemoteButton] when not enabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public class RemoteButtonColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val secondaryContentColor: RemoteColor,
    public val iconColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
    public val disabledSecondaryContentColor: RemoteColor,
    public val disabledIconColor: RemoteColor,
) {
    @Stable
    internal fun contentColor(enabled: RemoteBoolean = RemoteBoolean(true)): RemoteColor {
        return enabled.select(ifTrue = contentColor, ifFalse = disabledContentColor)
    }

    @Stable
    internal fun containerColor(enabled: RemoteBoolean = RemoteBoolean(true)): RemoteColor {
        return enabled.select(ifTrue = containerColor, ifFalse = disabledContainerColor)
    }
}

/** Draws a colored and shaped background with when clipping is not supported. */
private fun RemoteDrawWithContentScope.drawShapedBackground(
    shape: Shape,
    color: RemoteColor,
    borderColor: RemoteColor?,
    borderStrokeWidth: RemoteFloat?,
    state: RemoteComposeCreationState,
) {
    val paint =
        RemotePaint().apply {
            remoteColor = color
            style = Paint.Style.FILL
        }
    var borderPaint: RemotePaint? = null

    if (borderColor != null && borderStrokeWidth != null) {
        borderPaint =
            RemotePaint().apply {
                remoteColor = borderColor
                strokeWidth = borderStrokeWidth.toFloat()
                style = Paint.Style.STROKE
            }
    }

    val w = remoteComponentWidth(state)
    val h = remoteComponentHeight(state)

    when (shape) {
        is RoundedCornerShape -> {
            @Suppress("DEPRECATION")
            canvas.drawRoundRect(
                0f,
                0f,
                w,
                h,
                shape.bottomEnd.toPx(size, drawContext.density),
                shape.bottomEnd.toPx(size, drawContext.density),
                paint,
            )
            if (borderPaint != null) {
                @Suppress("DEPRECATION")
                canvas.drawRoundRect(
                    paint.strokeWidth,
                    paint.strokeWidth,
                    w - paint.strokeWidth,
                    h - paint.strokeWidth,
                    shape.bottomEnd.toPx(size, drawContext.density),
                    shape.bottomEnd.toPx(size, drawContext.density),
                    borderPaint,
                )
            }
        }
        is CircleShape -> {
            @Suppress("DEPRECATION") canvas.drawCircle(0f, 0f, size.maxDimension / 2f, paint)
            if (borderPaint != null) {
                @Suppress("DEPRECATION")
                canvas.drawCircle(0f, 0f, size.maxDimension / 2f, borderPaint)
            }
        }
    }
}

// TODO(b/451927368): Adds HeightInModifier and WidthInModifier that accept RemoteDp
@Composable
internal fun RemoteModifier.buttonSizeModifier(): RemoteModifier =
    this.heightIn(min = RemoteButtonDefaults.Height).widthIn(min = RemoteButtonDefaults.Width)

internal fun RemoteColor.toDisabledColor(
    disabledAlpha: RemoteFloat = DisabledContentAlpha.rf
): RemoteColor = this.copy(alpha = this.alpha * disabledAlpha)
