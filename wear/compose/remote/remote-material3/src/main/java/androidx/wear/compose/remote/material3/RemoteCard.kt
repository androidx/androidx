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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle

/**
 * Base level Wear Material 3 [RemoteCard] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [RemoteCard] components that take specific content
 * such as icons, images, titles, subtitles and labels.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCardSample
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card. See
 *   [RemoteCardDefaults.cardColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The main slot for a content of this card
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteCard(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    contentPadding: RemoteDp = RemoteCardDefaults.ContentPadding,
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
 * Outlined Wear Material 3 [RemoteCard] that offers a single slot to take any content.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteOutlinedCardSample
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card. See
 *   [RemoteCardDefaults.outlinedCardColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The main slot for a content of this card
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    contentPadding: RemoteDp = RemoteCardDefaults.ContentPadding,
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    public val ContentPadding: RemoteDp = 12.rdp

    /** The default size of the app icon/image when used inside a [RemoteAppCard]. */
    public val AppImageSize: RemoteDp = 18.rdp // From CardTokens.AppImageSize

    /** The default shape of [RemoteCard], which determines its corner radius. */
    public val shape: RemoteShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.large

    /**
     * The default height of [RemoteCard], [RemoteAppCard] and [RemoteTitleCard]. The card will
     * increase its height to accommodate the contents, if necessary.
     */
    public val Height: RemoteDp = 64.rdp // From CardTokens.ContainerMinHeight
    public val Width: RemoteDp = 80.rdp // From CardTokens.ContainerMinHeight

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    this.heightIn(min = RemoteCardDefaults.Height).fillMaxWidth()

@Composable
@RemoteComposable
internal fun RemoteCardImpl(
    onClick: Action,
    modifier: RemoteModifier,
    colors: RemoteCardColors,
    enabled: RemoteBoolean,
    contentPadding: RemoteDp,
    shape: RemoteShape,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val containerModifier =
        modifier
            .remoteCardSizeModifier()
            .clickable(
                actions = buildList { add(onClick) },
                enabled = enabled.constantValueOrNull ?: false,
            )
            .drawWithContent {
                drawShapedBackground(
                    shape = shape,
                    color = colors.containerColor,
                    borderColor = borderColor,
                    borderStrokeWidth = border?.value,
                )
                drawContent()
            }
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
    borderColor: RemoteColor?,
    borderStrokeWidth: RemoteFloat?,
) {
    drawSolidColorShape(shape, width, height, color)

    // Draw border if specified
    if (borderColor != null && borderStrokeWidth != null) {
        drawBorder(borderColor, borderStrokeWidth, shape, width, height)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawBorder(
    borderColor: RemoteColor,
    borderStrokeWidth: RemoteFloat,
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
) {
    with(shape.createOutline(RemoteSize(w, h), remoteDensity, layoutDirection)) {
        drawOutline(
            RemotePaint {
                color = borderColor
                strokeWidth = borderStrokeWidth
                style = PaintingStyle.Stroke
            }
        )
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawSolidColorShape(
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
    color: RemoteColor? = null,
) {
    with(shape.createOutline(RemoteSize(w, h), remoteDensity, layoutDirection)) {
        drawOutline(
            RemotePaint {
                style = PaintingStyle.Fill
                color?.let { this.color = it }
            }
        )
    }
}
