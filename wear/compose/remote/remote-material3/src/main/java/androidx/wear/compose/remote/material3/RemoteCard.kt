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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
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
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.ContentScale

/**
 * Base level Wear Material 3 [RemoteCard] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [RemoteCard] components that take specific content
 * such as icons, images, titles, subtitles and labels.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCardSample
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this component will not
 *   respond to user input
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card. See
 *   [RemoteCardDefaults.cardColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The main slot for a content of this card
 */
@RemoteComposable
@Composable
public fun RemoteCard(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    contentPadding: RemotePaddingValues = RemoteCardDefaults.ContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(value = RemoteCardTokens.ContentTypography, content = content)
    }
}

/**
 * Wear Material 3 [RemoteCard] that offers a single slot to take any content, with a background
 * image.
 *
 * An Image background is a means to reinforce the meaning of information in a Card, e.g. to help to
 * contextualize the information. Cards should have a content color that contrasts with the
 * background image and scrim. This [RemoteCard] takes [containerPainter] for the container image
 * background to be drawn (the [RemoteCardColors] containerColor property is ignored). It is
 * recommended to use [RemoteCardDefaults.containerPainter] to create the painter so that a scrim is
 * drawn on top of the container image, ensuring that any content above the background is legible.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCardWithImageSample
 * @param onClick Will be called when the user clicks the card
 * @param containerPainter The [RemotePainter] to use to draw the container image of the
 *   [RemoteCard], such as returned by [RemoteCardDefaults.containerPainter].
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this component will not
 *   respond to user input
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card. See
 *   [RemoteCardDefaults.cardWithContainerPainterColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The main slot for a content of this card
 */
@RemoteComposable
@Composable
public fun RemoteCard(
    onClick: Action,
    containerPainter: RemotePainter,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardWithContainerPainterColors(),
    contentPadding: RemotePaddingValues = RemoteCardDefaults.CardWithContainerPainterContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        containerPainter = containerPainter,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(value = RemoteCardTokens.ContentTypography, content = content)
    }
}

/**
 * Outlined Wear Material 3 [RemoteCard] that offers a single slot to take any content.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteOutlinedCardSample
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this component will not
 *   respond to user input
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card. See
 *   [RemoteCardDefaults.outlinedCardColors].
 * @param border The border width for the card
 * @param borderColor The color of the border
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The main slot for a content of this card
 */
@RemoteComposable
@Composable
public fun RemoteOutlinedCard(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.outlinedCardColors(),
    border: RemoteDp = RemoteCardDefaults.OutlinedBorderSize,
    borderColor: RemoteColor = RemoteCardDefaults.outlinedCardColors().contentColor,
    contentPadding: RemotePaddingValues = RemoteCardDefaults.ContentPadding,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        border = border,
        borderColor = borderColor,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        ProvideRemoteTextStyle(
            value = RemoteOutlinedCardTokens.ContentTypography,
            content = content,
        )
    }
}

/** Contains the default values used by [RemoteCard] */
public object RemoteCardDefaults {

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * a [RemoteCard], [RemoteAppCard] or [RemoteTitleCard].
     */
    @Composable
    @RemoteComposable
    public fun cardColors(): RemoteCardColors = RemoteMaterialTheme.colorScheme.defaultCardColors

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * a [RemoteCard], [RemoteAppCard] or [RemoteTitleCard].
     *
     * @param containerColor the container color of this [RemoteCard].
     * @param contentColor the content color of this [RemoteCard].
     * @param appNameColor the color used for appName, only applies to [RemoteAppCard].
     * @param timeColor the color used for time, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param titleColor the color used for title, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param subtitleColor the color used for subtitle, applies to [RemoteTitleCard].
     */
    @Composable
    @RemoteComposable
    public fun cardColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * an [RemoteOutlinedCard], outlined [RemoteAppCard] or [RemoteTitleCard].
     */
    @Composable
    @RemoteComposable
    public fun outlinedCardColors(): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultOutlinedCardColors

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * an [RemoteOutlinedCard], outlined [RemoteAppCard] or [RemoteTitleCard].
     *
     * @param contentColor the content color of this [RemoteOutlinedCard].
     * @param appNameColor the color used for appName, only applies to [RemoteAppCard].
     * @param timeColor the color used for time, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param titleColor the color used for title, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param subtitleColor the color used for subtitle, applies to [RemoteTitleCard].
     */
    @Composable
    @RemoteComposable
    public fun outlinedCardColors(
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultOutlinedCardColors.copy(
            containerColor = RemoteColor(Color.Transparent),
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /** The default size of the border for [RemoteOutlinedCard] */
    public val OutlinedBorderSize: RemoteDp = 1.rdp

    /** The default content padding used by [RemoteCard] */
    public val ContentPadding: RemotePaddingValues = RemotePaddingValues(12.rdp)

    /** Additional bottom padding added for TitleCard with an image background */
    public val ImageBottomPadding: RemoteDp = 12.rdp

    /**
     * ContentPadding for use in cards that have an image background in order to show more of the
     * image.
     */
    public val CardWithContainerPainterContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = 12.rdp,
            topPadding = 12.rdp,
            rightPadding = 12.rdp,
            bottomPadding = 12.rdp + ImageBottomPadding,
        )

    /**
     * Creates a [RemotePainter] for the background of a [RemoteCard] or [RemoteTitleCard] that
     * displays an image with a scrim on top to make sure that any content above the background will
     * be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Card, e.g. to
     * help to contextualize the information. Cards should have a content color that contrasts with
     * the background image and scrim.
     *
     * @param image The [RemoteImageBitmap] to use to draw the container background of the
     *   [RemoteCard] or [RemoteTitleCard]
     * @param scrim The [RemoteBrush] to use to paint a scrim over the container image to ensure
     *   that any text drawn over the image is legible
     * @param alpha Opacity of the container image painter and scrim.
     * @param shape Define the container shape.
     * @param contentScale Strategy for scaling the painter if its size does not match the
     *   container.
     */
    @Composable
    @RemoteComposable
    public fun containerPainter(
        image: RemoteImageBitmap,
        scrim: RemoteBrush? = scrimBrush(RemoteSize(image.width, image.height)),
        alpha: RemoteFloat = DefaultAlpha.rf,
        shape: RemoteShape = this.shape,
        contentScale: ContentScale = ContentScale.Crop,
    ): RemotePainter {
        return remoteContainerPainter(image, alpha, shape, contentScale, scrim)
    }

    /**
     * Creates a [RemoteBrush] for the recommended scrim drawn on top of image container
     * backgrounds.
     */
    @Composable
    @RemoteComposable
    public fun scrimBrush(size: RemoteSize): RemoteBrush {
        val color = scrimColor
        return RemoteBrush.linearGradient(
            colors = listOf(color, color),
            start = RemoteOffset.Zero,
            end = RemoteOffset(size.width, size.height),
        )
    }

    /**
     * Returns a scrim color that can be used to draw a scrim on top of an image to ensure that any
     * text drawn over the image is legible.
     */
    public val scrimColor: RemoteColor
        @Composable
        @RemoteComposable
        get() = RemoteMaterialTheme.colorScheme.background.copy(alpha = 0.5f.rf)

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * a [RemoteCard] with image container painter.
     */
    @Composable
    @RemoteComposable
    public fun cardWithContainerPainterColors(): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultCardWithContainerPainterColors

    /**
     * Creates a [RemoteCardColors] that represents the default container and content colors used in
     * a [RemoteCard] or [RemoteTitleCard] with Image set as a background.
     *
     * @param contentColor the content color of this [RemoteCard].
     * @param appNameColor the color used for appName, only applies to [RemoteAppCard].
     * @param timeColor the color used for time, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param titleColor the color used for title, applies to [RemoteAppCard] and [RemoteTitleCard].
     * @param subtitleColor the color used for subtitle, applies to [RemoteTitleCard].
     */
    @Composable
    @RemoteComposable
    public fun cardWithContainerPainterColors(
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors =
        RemoteMaterialTheme.colorScheme.defaultCardWithContainerPainterColors.copy(
            containerColor = RemoteColor(Color.Transparent),
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /** The default size of the app icon/image when used inside a [RemoteAppCard]. */
    public val AppImageSize: RemoteDp = 18.rdp

    /** The default shape of [RemoteCard], which determines its corner radius. */
    public val shape: RemoteShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.large

    /**
     * The default height of [RemoteCard], [RemoteAppCard] and [RemoteTitleCard]. The card will
     * increase its height to accommodate the contents, if necessary.
     */
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

/**
 * Represents Colors used in [RemoteCard]. Unlike other Material 3 components, Cards do not change
 * their color appearance when they are disabled.
 *
 * @param containerColor the background color of this [RemoteCard]
 * @param contentColor the content color of this [RemoteCard].
 * @param appNameColor the color used for appName, only applies to [RemoteAppCard].
 * @param timeColor the color used for time, applies to [RemoteAppCard] and [RemoteTitleCard].
 * @param titleColor the color used for title, applies to [RemoteAppCard] and [RemoteTitleCard].
 * @param subtitleColor the color used for subtitle, applies to [RemoteTitleCard].
 */
public class RemoteCardColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val appNameColor: RemoteColor,
    public val timeColor: RemoteColor,
    public val titleColor: RemoteColor,
    public val subtitleColor: RemoteColor,
) {
    public fun copy(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        appNameColor: RemoteColor? = null,
        timeColor: RemoteColor? = null,
        titleColor: RemoteColor? = null,
        subtitleColor: RemoteColor? = null,
    ): RemoteCardColors =
        RemoteCardColors(
            containerColor = containerColor ?: this.containerColor,
            contentColor = contentColor ?: this.contentColor,
            appNameColor = appNameColor ?: this.appNameColor,
            timeColor = timeColor ?: this.timeColor,
            titleColor = titleColor ?: this.titleColor,
            subtitleColor = subtitleColor ?: this.subtitleColor,
        )
}

@Composable
@RemoteComposable
internal fun RemoteModifier.remoteCardSizeModifier(): RemoteModifier =
    this.fillMaxWidth().heightIn(min = RemoteCardDefaults.Height).wrapContentHeight()

@Composable
@RemoteComposable
internal fun RemoteCardImpl(
    onClick: Action,
    modifier: RemoteModifier,
    colors: RemoteCardColors,
    enabled: RemoteBoolean,
    contentPadding: RemotePaddingValues,
    shape: RemoteShape,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
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
                    borderColor = borderColor,
                    borderStrokeWidth = border,
                )
                drawContent()
            }
            .clip(shape = shape)
            .clickable(action = onClick, enabled = enabled.constantValueOrNull ?: false)
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
    borderColor: RemoteColor? = null,
    borderStrokeWidth: RemoteDp? = null,
) {
    containerPainter?.let { with(it) { onDraw() } }
        ?: drawSolidColorShape(shape, width, height, color)

    // Draw border if specified
    if (borderColor != null && borderStrokeWidth != null) {
        drawBorder(borderColor, borderStrokeWidth, shape)
    }
}

private fun RemoteDrawScope.drawBorder(
    borderColor: RemoteColor,
    borderStrokeWidth: RemoteDp,
    shape: RemoteShape,
) {
    val strokeWidthPx = borderStrokeWidth.toPx()
    val outline =
        if (shape is RemoteCornerBasedShape) {
            shape.createOutline(
                size = RemoteSize(width, height),
                density = remoteDensity,
                layoutDirection = layoutDirection,
                strokeWidth = strokeWidthPx,
            )
        } else {
            shape.createOutline(RemoteSize(width, height), remoteDensity, layoutDirection)
        }
    drawOutline(
        outline,
        RemotePaint {
            color = borderColor
            strokeWidth = strokeWidthPx
            style = PaintingStyle.Stroke
        },
    )
}

private fun RemoteDrawScope.drawSolidColorShape(
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
    color: RemoteColor? = null,
) {
    drawOutline(
        shape.createOutline(RemoteSize(w, h), remoteDensity, layoutDirection),
        RemotePaint {
            style = PaintingStyle.Fill
            color?.let { this.color = it }
        },
    )
}
