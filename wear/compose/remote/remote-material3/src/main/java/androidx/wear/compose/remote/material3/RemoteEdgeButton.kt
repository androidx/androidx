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
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.TextConfiguration

/**
 * Wear Material 3 [RemoteEdgeButton] that offers a single slot to take any content.
 *
 * The [RemoteEdgeButton] has a special shape designed to anchor to the bottom edge of a round
 * screen.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonSample
 *
 * Example of a [RemoteEdgeButton] with an icon:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonIconSample
 *
 * Example of a [RemoteEdgeButton] with filled tonal colors:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonFilledTonalSample
 *
 * Example of a [RemoteEdgeButton] with multi-line text:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonMultiLineSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param buttonSize The size of the button, defaults to [RemoteEdgeButtonSize.Small].
 * @param enabled Controls the enabled state of the button. When false, this component will not
 *   respond to user input.
 * @param colors [RemoteButtonColors] that will be used to resolve the colors used for this button.
 *   See [RemoteEdgeButtonDefaults.buttonColors].
 * @param border The border stroke width for the button.
 * @param borderColor The color of the border.
 * @param shape Defines the button's shape. Defaults to [RemoteEdgeButtonDefaults.shape].
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param content The content displayed inside the button.
 */
@Composable
@RemoteComposable
public fun RemoteEdgeButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small,
    enabled: RemoteBoolean = true.rb,
    colors: RemoteButtonColors = RemoteEdgeButtonDefaults.buttonColors(),
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteEdgeButtonDefaults.shape(buttonSize),
    contentPadding: RemotePaddingValues = RemoteEdgeButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteBox(
        modifier =
            modifier
                .fillMaxWidth()
                .height(buttonSize.maximumHeight)
                .semantics(mergeDescendants = true) { role = Role.Button }
                .clip(shape = shape)
                .clickable(onClick, enabled = enabled.constantValueOrNull ?: false)
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = colors.containerColor(enabled),
                        enabled = enabled,
                        containerPainter = null,
                        disabledContainerPainter = null,
                        borderColor = borderColor,
                        borderStrokeWidth = border,
                    )
                    drawContent()
                },
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteRow(
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Center,
            modifier = RemoteModifier.fillMaxWidth().padding(contentPadding),
            content =
                provideScopeContent(
                    contentColor = colors.contentColor(enabled = enabled),
                    textStyle = RemoteMaterialTheme.typography.labelMedium,
                    textConfiguration =
                        TextConfiguration(
                            // TODO: Center alignment for multi-line text is fixed in a follow-up
                            // CL in remote-player-core.
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = buttonSize.maxLines(),
                        ),
                    content = content,
                ),
        )
    }
}

/**
 * Size of the [RemoteEdgeButton].
 *
 * @param maximumHeight The maximum height of the button.
 */
@Immutable
public class RemoteEdgeButtonSize internal constructor(public val maximumHeight: RemoteDp) {
    internal fun maxLines(): Int =
        when (this) {
            ExtraSmall -> 1
            Small -> 2
            Medium -> 2
            else -> 3
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteEdgeButtonSize) return false
        return maximumHeight == other.maximumHeight
    }

    override fun hashCode(): Int = maximumHeight.hashCode()

    public companion object {
        /** The Size to be applied for an extra small [RemoteEdgeButton]. */
        public val ExtraSmall: RemoteEdgeButtonSize = RemoteEdgeButtonSize(46.rdp)

        /** The Size to be applied for a small [RemoteEdgeButton]. */
        public val Small: RemoteEdgeButtonSize = RemoteEdgeButtonSize(56.rdp)

        /** The Size to be applied for a medium [RemoteEdgeButton]. */
        public val Medium: RemoteEdgeButtonSize = RemoteEdgeButtonSize(70.rdp)

        /** The Size to be applied for a large [RemoteEdgeButton]. */
        public val Large: RemoteEdgeButtonSize = RemoteEdgeButtonSize(96.rdp)
    }
}

/** Contains the default values used by [RemoteEdgeButton]. */
public object RemoteEdgeButtonDefaults {
    /** The recommended icon size when used with [RemoteEdgeButtonSize.ExtraSmall]. */
    public val ExtraSmallIconSize: RemoteDp = 24.rdp

    /** The recommended icon size when used with [RemoteEdgeButtonSize.Small]. */
    public val SmallIconSize: RemoteDp = 32.rdp

    /** The recommended icon size when used with [RemoteEdgeButtonSize.Medium]. */
    public val MediumIconSize: RemoteDp = 32.rdp

    /** The recommended icon size when used with [RemoteEdgeButtonSize.Large]. */
    public val LargeIconSize: RemoteDp = 36.rdp

    /**
     * Recommended icon size for a given edge button size.
     *
     * @param edgeButtonSize The size of the edge button
     */
    public fun iconSizeFor(edgeButtonSize: RemoteEdgeButtonSize): RemoteDp =
        when (edgeButtonSize) {
            RemoteEdgeButtonSize.ExtraSmall -> ExtraSmallIconSize
            RemoteEdgeButtonSize.Small -> SmallIconSize
            RemoteEdgeButtonSize.Medium -> MediumIconSize
            RemoteEdgeButtonSize.Large -> LargeIconSize
            else -> MediumIconSize
        }

    /** The default shape for [RemoteEdgeButton]. */
    public val shape: RemoteShape = RemoteEdgeButtonShape(RemoteEdgeButtonSize.Small)

    /** The recommended shape for [RemoteEdgeButton] of a given size. */
    public fun shape(buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small): RemoteShape =
        RemoteEdgeButtonShape(buttonSize)

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteEdgeButton].
     */
    @Composable public fun buttonColors(): RemoteButtonColors = RemoteButtonDefaults.buttonColors()

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteEdgeButton].
     */
    @Composable
    public fun buttonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors =
        RemoteButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /** Creates a [RemoteButtonColors] with filled tonal colors for [RemoteEdgeButton]. */
    @Composable
    public fun filledTonalButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.filledTonalButtonColors()

    /** Creates a [RemoteButtonColors] with filled tonal colors for [RemoteEdgeButton]. */
    @Composable
    public fun filledTonalButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors =
        RemoteButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Creates a [RemoteButtonColors] with higher chroma container colors for [RemoteEdgeButton].
     */
    @Composable
    public fun filledVariantButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.filledVariantButtonColors()

    /**
     * Creates a [RemoteButtonColors] with higher chroma container colors for [RemoteEdgeButton].
     */
    @Composable
    public fun filledVariantButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors =
        RemoteButtonDefaults.filledVariantButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /** Creates a [RemoteButtonColors] with outlined colors for [RemoteEdgeButton]. */
    @Composable
    public fun outlinedButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.outlinedButtonColors()

    /** Creates a [RemoteButtonColors] with outlined colors for [RemoteEdgeButton]. */
    @Composable
    public fun outlinedButtonColors(
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors =
        RemoteButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )

    /** Padding around the Edge Button on its top and bottom. */
    public val VerticalPadding: RemoteDp = 3.rdp

    /** The recommended top content padding for [RemoteEdgeButton]. */
    public val TopContentPadding: RemoteDp = 6.rdp

    /** The recommended bottom content padding for [RemoteEdgeButton]. */
    public val BottomContentPadding: RemoteDp = 8.rdp

    /** The recommended horizontal content padding for [RemoteEdgeButton]. */
    public val HorizontalContentPadding: RemoteDp = 14.rdp

    /** The default content padding used by [RemoteEdgeButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = HorizontalContentPadding,
            topPadding = TopContentPadding,
            rightPadding = HorizontalContentPadding,
            bottomPadding = BottomContentPadding,
        )
}

/**
 * Shape for [RemoteEdgeButton] that fits the bottom edge of a round screen.
 *
 * @param buttonSize The size of the button to determine its shape contour.
 */
public class RemoteEdgeButtonShape(
    public val buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small
) : RemoteShape {
    @Suppress("RestrictedApiAndroidX")
    override fun createOutline(
        size: RemoteSize,
        density: RemoteDensity,
        layoutDirection: LayoutDirection,
    ): RemoteOutline {
        return createOutline(
            size = size,
            density = density,
            layoutDirection = layoutDirection,
            strokeWidth = 0f.rf,
        )
    }

    /**
     * Creates a [RemoteOutline] for this shape, optionally configured for drawing a stroked border.
     *
     * @param size the outer size of the component boundary
     * @param density the remote density to apply to the shape
     * @param layoutDirection the current layout direction
     * @param strokeWidth the stroke width of the border (0 if drawing a solid background fill).
     *   When positive, the outline is inset by `strokeWidth / 2` to keep the centered stroke within
     *   component bounds.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("RestrictedApiAndroidX")
    public fun createOutline(
        size: RemoteSize,
        density: RemoteDensity,
        layoutDirection: LayoutDirection,
        strokeWidth: RemoteFloat,
    ): RemoteOutline {
        val densityFactor = density.density
        val w = size.width
        val h = size.height

        val bottomPaddingPx = 3f.rf * densityFactor
        val extraSmallHeightPx = 46f.rf * densityFactor
        val extraSmallEllipsisHeightPx = 58f.rf * densityFactor
        val buttonToEllipsisRatio = 1.42f.rf

        val ellipsisHeight =
            extraSmallEllipsisHeightPx + (h - extraSmallHeightPx) * buttonToEllipsisRatio
        val screenRadius = max(w / 2f.rf - bottomPaddingPx, 1f.rf)
        val ellipsisRadiusY = ellipsisHeight / 2f.rf
        val ellipsisRadiusX = min(sqrt(ellipsisRadiusY * screenRadius), screenRadius)
        val circleRadius = max(h - ellipsisRadiusY, 0f.rf)

        val halfStroke = strokeWidth / 2f.rf
        val topY = halfStroke
        val bottomY = h - halfStroke
        val insetEllipsisRadiusX = max(ellipsisRadiusX - halfStroke, 0f.rf)
        val insetCircleRadius = max(circleRadius - halfStroke, 0f.rf)

        val circleCenterRightX = w / 2f.rf + insetEllipsisRadiusX - insetCircleRadius
        val circleCenterLeftX = w / 2f.rf - insetEllipsisRadiusX + insetCircleRadius
        val circularArcWeight = 0.70710678f.rf

        return RemoteOutline.Generic {
            moveTo(circleCenterRightX, topY)

            // Top-right corner
            conicTo(
                x1 = w / 2f.rf + insetEllipsisRadiusX,
                y1 = topY,
                x2 = w / 2f.rf + insetEllipsisRadiusX,
                y2 = topY + insetCircleRadius,
                weight = circularArcWeight,
            )

            // Bottom-right quadrant of ellipse
            conicTo(
                x1 = w / 2f.rf + insetEllipsisRadiusX,
                y1 = bottomY,
                x2 = w / 2f.rf,
                y2 = bottomY,
                weight = circularArcWeight,
            )

            // Bottom-left quadrant of ellipse
            conicTo(
                x1 = w / 2f.rf - insetEllipsisRadiusX,
                y1 = bottomY,
                x2 = w / 2f.rf - insetEllipsisRadiusX,
                y2 = topY + insetCircleRadius,
                weight = circularArcWeight,
            )

            // Top-left corner
            conicTo(
                x1 = w / 2f.rf - insetEllipsisRadiusX,
                y1 = topY,
                x2 = circleCenterLeftX,
                y2 = topY,
                weight = circularArcWeight,
            )

            close()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteEdgeButtonShape) return false
        return buttonSize == other.buttonSize
    }

    override fun hashCode(): Int = buttonSize.hashCode()
}
