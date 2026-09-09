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

package androidx.xr.glimmer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Renders a clickable icon marker (a dot [IconMarkerDefaults.DotIcon] by default) for anchoring to
 * real-world objects. For cases where a custom icon is required, provide an [Icon] to [content]
 * slot.
 *
 * This variant renders a clickable icon enclosed in an interactive surface.
 *
 * @sample androidx.xr.glimmer.samples.IconMarkerSample
 * @sample androidx.xr.glimmer.samples.IconMarkerWithCustomIconSample
 * @param onClick called when this marker is clicked.
 * @param contentDescription text used by accessibility services to describe a real-world object
 *   this marker is anchored to. This text should be localized. It will be merged with content
 *   description of the [content] so the latter one should be omitted.
 * @param modifier the [Modifier] to be applied to this layout
 * @param size the [IconMarkerSize] variant of the marker
 * @param color background [Color] of the interactive surface
 * @param contentColor [Color] applied to [content]
 * @param contentPadding internal padding between the surface and [content]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting interactions for this marker. Note that if `null` is provided, interactions will still
 *   happen internally
 * @param content the marker content to display inside the surface. By default, this is a dot
 *   [IconMarkerDefaults.DotIcon], but can be customized with an [Icon]. Size of the icon depends on
 *   the provided [size].
 */
@Composable
public fun IconMarker(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: IconMarkerSize = IconMarkerSize.Medium,
    color: Color = IconMarkerDefaults.color(),
    contentColor: Color = IconMarkerDefaults.contentColor(color),
    contentPadding: PaddingValues = IconMarkerDefaults.contentPadding(size),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit = { IconMarkerDefaults.DotIcon() },
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                }
                .surface(
                    color = color,
                    contentColor = contentColor,
                    interactionSource = interactionSource,
                )
                .clickable(
                    role = Role.Button,
                    interactionSource = interactionSource,
                    onClick = onClick,
                )
                .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalIconSize provides IconMarkerDefaults.iconSize(size),
            content = content,
        )
    }
}

/**
 * Renders a non-interactive icon marker (a dot [IconMarkerDefaults.DotIcon] by default) for
 * anchoring to real-world objects. For cases where a custom icon is required, provide an [Icon] to
 * [content] slot.
 *
 * Unlike the clickable [IconMarker], this variant renders only the marker [content] without an
 * interactive background surface or click handling.
 *
 * @param contentDescription text used by accessibility services to describe a real-world object
 *   this marker is anchored to. This text should be localized. It will be merged with content
 *   description of the [content] so the latter one should be omitted.
 * @param modifier the [Modifier] to be applied to this layout
 * @param size the [IconMarkerSize] variant of the marker
 * @param contentColor [Color] applied to [content]
 * @param contentPadding internal padding applied around [content]
 * @param content the marker content to display inside the surface. By default, this is a dot
 *   [IconMarkerDefaults.DotIcon], but can be customized with an [Icon]. Size of the icon depends on
 *   the provided [size].
 */
@Composable
public fun IconMarker(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: IconMarkerSize = IconMarkerSize.Medium,
    contentColor: Color = IconMarkerDefaults.contentColor(),
    contentPadding: PaddingValues = IconMarkerDefaults.contentPadding(size),
    content: @Composable () -> Unit = { IconMarkerDefaults.DotIcon() },
) {
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                }
                .contentColorProvider(contentColor)
                .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalIconSize provides IconMarkerDefaults.iconSize(size),
            content = content,
        )
    }
}

/** Size variant for icon markers. */
@Immutable
@JvmInline
public value class IconMarkerSize internal constructor(private val value: Int) {
    public companion object {
        /** Small size variant. */
        public val Small: IconMarkerSize = IconMarkerSize(1)

        /** Medium size variant. This is the default size. */
        public val Medium: IconMarkerSize = IconMarkerSize(2)
    }

    override fun toString(): String {
        return when (this) {
            Small -> "IconMarkerSize.Small"
            Medium -> "IconMarkerSize.Medium"
            else -> "IconMarkerSize.Unknown"
        }
    }
}

/** Contains default values used by [IconMarker]. */
public object IconMarkerDefaults {

    /**
     * Default background surface color for clickable [IconMarker].
     *
     * Calculates the background [Color] for a surface derived from the provided [color]. Adjusts
     * the provided [color] so that it is suitable for use as a surface background.
     *
     * @param color the base [Color] of the surface
     * @return the surface background [Color], adjusted to improve content contrast
     * @see SurfaceDefaults.color
     */
    @Composable
    public fun color(color: Color = GlimmerTheme.colors.surface): Color {
        return SurfaceDefaults.color(color)
    }

    /**
     * Default content color for [IconMarker]. It defines the color of icon in [Icon].
     *
     * Calculates the preferred content color for [color]. This will return either [Color.White] or
     * [Color.Black], depending on the luminance of the provided color.
     *
     * @param color the base background [Color] of the surface
     * @return the content [Color], adjusted to improve content contrast
     * @see calculateContentColor
     */
    @Composable
    public fun contentColor(color: Color = GlimmerTheme.colors.surface): Color {
        return calculateContentColor(color)
    }

    /** Resolves padding values between marker and surface around it based on [IconMarkerSize]. */
    @Composable
    public fun contentPadding(size: IconMarkerSize): PaddingValues {
        val allDp =
            when (size) {
                IconMarkerSize.Small -> GlimmerTheme.componentSpacingValues.small
                IconMarkerSize.Medium -> GlimmerTheme.componentSpacingValues.medium
                else -> throw IllegalArgumentException("Unknown size $size.")
            }
        return PaddingValues(allDp)
    }

    /** Resolves marker's icon size based on [IconMarkerSize]. */
    @Composable
    internal fun iconSize(size: IconMarkerSize): Dp =
        when (size) {
            IconMarkerSize.Small -> GlimmerTheme.iconSizes.small
            IconMarkerSize.Medium -> GlimmerTheme.iconSizes.medium
            else -> throw IllegalArgumentException("Unknown size $size.")
        }

    /**
     * Default marker icon for [IconMarker], rendering a circle marker. To provide content
     * description use [IconMarker] API instead.
     */
    @Composable
    public fun DotIcon(modifier: Modifier = Modifier) {
        Icon(painter = DotPainter, contentDescription = null, modifier = modifier)
    }

    private val DotPainter: Painter =
        object : Painter() {
            override fun DrawScope.onDraw() {
                val radius = size.minDimension / 2
                drawCircle(color = Color.White, radius = radius * 0.85f)
            }

            override val intrinsicSize: Size = Size.Unspecified
        }
}
