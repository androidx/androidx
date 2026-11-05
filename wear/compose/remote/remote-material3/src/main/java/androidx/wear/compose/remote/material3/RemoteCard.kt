/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.wrapContentHeight
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale

/** @sample androidx.wear.compose.remote.material3.samples.RemoteCardSample */
@RemoteComposable
@Composable
public fun RemoteCard(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteCardDefaults.ContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(value = RemoteCardTokens.ContentTypography, content = content)
    }
}

/** @sample androidx.wear.compose.remote.material3.samples.RemoteCardWithImageSample */
@RemoteComposable
@Composable
public fun RemoteCard(
    onClick: RemoteAction,
    containerPainter: RemotePainter,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardWithContainerPainterColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteCardDefaults.CardWithContainerPainterContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        containerPainter = containerPainter,
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(value = RemoteCardTokens.ContentTypography, content = content)
    }
}

/** @sample androidx.wear.compose.remote.material3.samples.RemoteOutlinedCardSample */
@RemoteComposable
@Composable
public fun RemoteOutlinedCard(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.outlinedCardColors(),
    border: RemoteBorderStroke = RemoteCardDefaults.outlinedCardBorder(),
    contentPadding: RemotePaddingValues = RemoteCardDefaults.ContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(
            value = RemoteOutlinedCardTokens.ContentTypography,
            content = content,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteCardDefaults {

    @Composable
    @RemoteComposable
    public fun cardColors(): RemoteCardColors = RemoteMaterialTheme.colorScheme.defaultCardColors

    @Composable
    @RemoteComposable
    public fun cardColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors {
        val default = RemoteMaterialTheme.colorScheme.defaultCardColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            appNameColor = appNameColor ?: default.appNameColor,
            timeColor = timeColor ?: default.timeColor,
            titleColor = titleColor ?: default.titleColor,
            subtitleColor = subtitleColor ?: default.subtitleColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun outlinedCardColors(): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultOutlinedCardColors

    @Composable
    @RemoteComposable
    public fun outlinedCardColors(
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors {
        val default = RemoteMaterialTheme.colorScheme.defaultOutlinedCardColors
        return default.copy(
            containerColor = RemoteColor(Color.Transparent),
            contentColor = contentColor ?: default.contentColor,
            appNameColor = appNameColor ?: default.appNameColor,
            timeColor = timeColor ?: default.timeColor,
            titleColor = titleColor ?: default.titleColor,
            subtitleColor = subtitleColor ?: default.subtitleColor,
        )
    }

    public val outlinedCardBorderColor: RemoteColor
        @Composable @RemoteComposable get() = RemoteMaterialTheme.colorScheme.outline

    public val OutlinedBorderSize: RemoteDp = 1.rdp

    @Composable
    @RemoteComposable
    public fun outlinedCardBorder(
        outlineColor: RemoteColor = outlinedCardBorderColor,
        borderWidth: RemoteDp = OutlinedBorderSize,
    ): RemoteBorderStroke = RemoteBorderStroke(width = borderWidth, color = outlineColor)

    public val ContentPadding: RemotePaddingValues = RemotePaddingValues(12.rdp)

    public val ImageBottomPadding: RemoteDp = 12.rdp

    public val CardWithContainerPainterContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = 12.rdp,
            topPadding = 12.rdp,
            rightPadding = 12.rdp,
            bottomPadding = 12.rdp + ImageBottomPadding,
        )

    @Composable
    @RemoteComposable
    public fun containerPainter(
        image: RemoteImageBitmap,
        scrim: RemoteBrush? = scrimBrush(),
        alpha: RemoteFloat = DefaultAlpha.rf,
        shape: RemoteShape = this.shape,
        contentScale: ContentScale = ContentScale.Crop,
    ): RemotePainter {
        return remoteContainerPainter(image, alpha, shape, contentScale, scrim)
    }

    @Composable
    @RemoteComposable
    public fun scrimBrush(): RemoteBrush {
        val color = scrimColor
        return RemoteBrush.solidColor(color)
    }

    public val scrimColor: RemoteColor
        @Composable
        @RemoteComposable
        get() = RemoteMaterialTheme.colorScheme.background.copy(alpha = 0.5f.rf)

    @Composable
    @RemoteComposable
    public fun cardWithContainerPainterColors(): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultCardWithContainerPainterColors

    @Composable
    @RemoteComposable
    public fun cardWithContainerPainterColors(
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors {
        val default = RemoteMaterialTheme.colorScheme.defaultCardWithContainerPainterColors
        return default.copy(
            containerColor = RemoteColor(Color.Transparent),
            contentColor = contentColor ?: default.contentColor,
            appNameColor = appNameColor ?: default.appNameColor,
            timeColor = timeColor ?: default.timeColor,
            titleColor = titleColor ?: default.titleColor,
            subtitleColor = subtitleColor ?: default.subtitleColor,
        )
    }

    public val AppImageSize: RemoteDp = 18.rdp

    public val shape: RemoteShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.large

    public val Height: RemoteDp = 64.rdp

    public val Width: RemoteDp = 80.rdp

    private val RemoteColorScheme.defaultCardColors: RemoteCardColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteCardColors(
                containerColor = surfaceContainer,
                contentColor = onSurfaceVariant,
                appNameColor = onSurface,
                timeColor = onSurfaceVariant,
                titleColor = onSurface,
                subtitleColor = tertiary,
            )
        }

    private val RemoteColorScheme.defaultOutlinedCardColors: RemoteCardColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteCardColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = onSurfaceVariant,
                appNameColor = onSurface,
                timeColor = onSurface,
                titleColor = onSurface,
                subtitleColor = tertiary,
            )
        }

    private val RemoteColorScheme.defaultCardWithContainerPainterColors: RemoteCardColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteCardColors(
                containerColor = RemoteColor(Color.Transparent),
                contentColor = onBackground,
                appNameColor = onBackground,
                timeColor = onBackground,
                titleColor = onBackground,
                subtitleColor = tertiary,
            )
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteCardColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val appNameColor: RemoteColor,
    public val timeColor: RemoteColor,
    public val titleColor: RemoteColor,
    public val subtitleColor: RemoteColor,
) {
    public fun copy(
        containerColor: RemoteColor = this.containerColor,
        contentColor: RemoteColor = this.contentColor,
        appNameColor: RemoteColor = this.appNameColor,
        timeColor: RemoteColor = this.timeColor,
        titleColor: RemoteColor = this.titleColor,
        subtitleColor: RemoteColor = this.subtitleColor,
    ): RemoteCardColors =
        RemoteCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )
}

@Composable
@RemoteComposable
internal fun RemoteModifier.remoteCardSizeModifier(): RemoteModifier =
    this.fillMaxWidth().heightIn(min = RemoteCardDefaults.Height).wrapContentHeight()

@Composable
@RemoteComposable
internal fun RemoteCardImpl(
    onClick: RemoteAction,
    modifier: RemoteModifier,
    colors: RemoteCardColors,
    enabled: RemoteBoolean,
    contentPadding: RemotePaddingValues,
    shape: RemoteShape,
    border: RemoteBorderStroke? = null,
    containerPainter: RemotePainter? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val containerModifier =
        modifier
            .remoteCardSizeModifier()
            .drawWithContent {
                drawShapedBackground(
                    shape = shape,
                    color = colors.containerColor,
                    containerPainter = containerPainter,
                    border = border,
                )
                drawContent()
            }
            .clip(shape = shape)
            .clickable(onClick = onClick, enabled = enabled.constantValueOrNull ?: false)
            .padding(contentPadding)

    RemoteColumn(modifier = containerModifier) {
        CompositionLocalProvider(
            LocalRemoteContentColor provides colors.contentColor,
            content = content,
        )
    }
}

internal object RemoteCardTokens {
    val ContentTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.bodyLarge
}

private object RemoteOutlinedCardTokens {
    val ContentTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.bodyLarge
}

private fun RemoteDrawScope.drawShapedBackground(
    shape: RemoteShape,
    color: RemoteColor,
    containerPainter: RemotePainter? = null,
    border: RemoteBorderStroke? = null,
) {
    containerPainter?.let { with(it) { onDraw() } }
        ?: drawSolidColorShape(shape, width, height, color)

    // Draw border if specified
    if (border != null) {
        drawBorder(border, shape)
    }
}

private fun RemoteDrawScope.drawBorder(
    border: RemoteBorderStroke,
    shape: RemoteShape,
) {
    val strokeWidthPx = border.width.toPx()
    val outline =
        if (shape is RemoteCornerBasedShape) {
            shape.createOutline(
                size = RemoteSize(width, height),
                layoutDirection = layoutDirection,
                density = remoteDensity,
                strokeWidth = strokeWidthPx,
            )
        } else {
            shape.createOutline(
                size = RemoteSize(width, height),
                layoutDirection = layoutDirection,
                density = remoteDensity,
            )
        }
    val paint = RemotePaint {
        strokeWidth = strokeWidthPx
        style = RemotePaintingStyle.Stroke
    }
    with(border.brush) {
        applyTo(paint, RemoteSize(width, height))
    }
    drawOutline(outline, paint)
}

private fun RemoteDrawScope.drawSolidColorShape(
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
    color: RemoteColor? = null,
) {
    drawOutline(
        shape.createOutline(
            size = RemoteSize(w, h),
            layoutDirection = layoutDirection,
            density = remoteDensity,
        ),
        RemotePaint {
            style = RemotePaintingStyle.Fill
            color?.let { this.color = it }
        },
    )
}
