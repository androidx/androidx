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
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.remoteComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.TextConfiguration

/**
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonIconSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonFilledTonalSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteEdgeButtonMultiLineSample
 */
@Composable
@RemoteComposable
public fun RemoteEdgeButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small,
    enabled: RemoteBoolean = true.rb,
    colors: RemoteButtonColors = RemoteEdgeButtonDefaults.buttonColors(),
    border: RemoteBorderStroke? = null,
    shape: RemoteShape = RemoteEdgeButtonDefaults.shape(buttonSize),
    contentPadding: RemotePaddingValues = RemoteEdgeButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteBox(
        modifier =
            modifier
                .fillMaxWidth()
                .height(buttonSize.maximumHeight)
                .semantics(mergeDescendants = true) { role = RemoteRole.Button }
                .clip(shape = shape)
                .clickable(onClick, enabled = enabled.constantValueOrNull ?: false)
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = colors.containerColor(enabled),
                        enabled = enabled,
                        containerPainter = null,
                        disabledContainerPainter = null,
                        border = border,
                    )
                    drawContent()
                },
        contentAlignment = RemoteAlignment.Center,
    ) {
        val buttonWidthDp = remoteComponent().width.toRemoteDp()
        val contentWidth =
            calculateEdgeButtonContentWidth(
                actualButtonWidth = buttonWidthDp,
                buttonHeight = buttonSize.maximumHeight,
            )

        RemoteRow(
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Center,
            modifier = RemoteModifier.widthIn(max = contentWidth).padding(contentPadding),
            content =
                provideScopeContent(
                    contentColor = colors.contentColor(enabled = enabled),
                    textStyle = RemoteMaterialTheme.typography.labelMedium,
                    textConfiguration =
                        TextConfiguration(
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = buttonSize.maxLines(),
                        ),
                    content = content,
                ),
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        public val ExtraSmall: RemoteEdgeButtonSize = RemoteEdgeButtonSize(46.rdp)

        public val Small: RemoteEdgeButtonSize = RemoteEdgeButtonSize(56.rdp)

        public val Medium: RemoteEdgeButtonSize = RemoteEdgeButtonSize(70.rdp)

        public val Large: RemoteEdgeButtonSize = RemoteEdgeButtonSize(96.rdp)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteEdgeButtonDefaults {
    public val ExtraSmallIconSize: RemoteDp = 24.rdp

    public val SmallIconSize: RemoteDp = 32.rdp

    public val MediumIconSize: RemoteDp = 32.rdp

    public val LargeIconSize: RemoteDp = 36.rdp

    public fun iconSizeFor(edgeButtonSize: RemoteEdgeButtonSize): RemoteDp =
        when (edgeButtonSize) {
            RemoteEdgeButtonSize.ExtraSmall -> ExtraSmallIconSize
            RemoteEdgeButtonSize.Small -> SmallIconSize
            RemoteEdgeButtonSize.Medium -> MediumIconSize
            RemoteEdgeButtonSize.Large -> LargeIconSize
            else -> MediumIconSize
        }

    public fun shape(buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small): RemoteShape =
        RemoteEdgeButtonShape(buttonSize)

    @Composable
    @RemoteComposable
    public fun buttonColors(): RemoteButtonColors = RemoteButtonDefaults.buttonColors()

    @Composable
    @RemoteComposable
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

    @Composable
    @RemoteComposable
    public fun filledTonalButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.filledTonalButtonColors()

    @Composable
    @RemoteComposable
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

    @Composable
    @RemoteComposable
    public fun filledVariantButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.filledVariantButtonColors()

    @Composable
    @RemoteComposable
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

    @Composable
    @RemoteComposable
    public fun outlinedButtonColors(): RemoteButtonColors =
        RemoteButtonDefaults.outlinedButtonColors()

    @Composable
    @RemoteComposable
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

    public val VerticalPadding: RemoteDp = 3.rdp

    public val TopContentPadding: RemoteDp = 6.rdp

    public val BottomContentPadding: RemoteDp = 8.rdp

    public val HorizontalContentPadding: RemoteDp = 0.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = HorizontalContentPadding,
            topPadding = TopContentPadding,
            rightPadding = HorizontalContentPadding,
            bottomPadding = BottomContentPadding,
        )
}

// How tall the ellipse is for the extra small button.
private val ExtraSmallEllipseHeight: RemoteDp = 58.rdp

// How much the ellipse grows as the button height grows.
private val ButtonToEllipseRatio: RemoteFloat = 1.42f.rf

// Distance from the leftmost/rightmost point in the button to the edge of the screen.
private val TargetSidePadding: RemoteDp = 20.rdp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteEdgeButtonShape(
    public val buttonSize: RemoteEdgeButtonSize = RemoteEdgeButtonSize.Small
) : RemoteShape {
    override fun createOutline(
        size: RemoteSize,
        layoutDirection: RemoteLayoutDirection,
        density: RemoteDensity,
    ): RemoteOutline {
        return createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = density,
            strokeWidth = 0f.rf,
        )
    }

    /**
     * Creates a [RemoteOutline] for this shape, optionally configured for drawing a stroked border.
     *
     * @param size the outer size of the component boundary
     * @param layoutDirection the current layout direction
     * @param density the remote density to apply to the shape
     * @param strokeWidth the stroke width of the border (0 if drawing a solid background fill).
     *   When positive, the outline is inset by `strokeWidth / 2` to keep the centered stroke within
     *   component bounds.
     * @return a [RemoteOutline] for this shape
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createOutline(
        size: RemoteSize,
        layoutDirection: RemoteLayoutDirection,
        density: RemoteDensity,
        strokeWidth: RemoteFloat,
    ): RemoteOutline {
        val w = size.width
        val h = size.height

        val bottomPaddingPx = RemoteEdgeButtonDefaults.VerticalPadding.toPx(density)
        val extraSmallHeightPx = RemoteEdgeButtonSize.ExtraSmall.maximumHeight.toPx(density)
        val extraSmallEllipseHeightPx = ExtraSmallEllipseHeight.toPx(density)
        val buttonToEllipseRatio = ButtonToEllipseRatio

        val finalFadeProgress = clamp(1f.rf - h / extraSmallHeightPx, 0f.rf, 1f.rf)
        val ellipseHeight =
            (extraSmallEllipseHeightPx + (h - extraSmallHeightPx) * buttonToEllipseRatio) *
                (1f.rf - finalFadeProgress) + h * finalFadeProgress
        val screenRadius = max(w / 2f.rf - bottomPaddingPx, 1f.rf)
        val ellipseRadiusY = max(ellipseHeight / 2f.rf, 0f.rf)
        val ellipseRadiusX = min(sqrt(max(ellipseRadiusY * screenRadius, 0f.rf)), screenRadius)
        val circleRadius = max(h - ellipseRadiusY, 0f.rf)

        val halfStroke = strokeWidth / 2f.rf
        val topY = halfStroke
        val bottomY = h - halfStroke
        val insetEllipseRadiusX = max(ellipseRadiusX - halfStroke, 0f.rf)
        val insetCircleRadius = max(circleRadius - halfStroke, 0f.rf)

        val circleCenterRightX = w / 2f.rf + insetEllipseRadiusX - insetCircleRadius
        val circleCenterLeftX = w / 2f.rf - insetEllipseRadiusX + insetCircleRadius
        val circularArcWeight = 0.70710678f.rf

        return RemoteOutline.Generic {
            moveTo(circleCenterRightX, topY)

            // Top-right corner
            conicTo(
                x1 = w / 2f.rf + insetEllipseRadiusX,
                y1 = topY,
                x2 = w / 2f.rf + insetEllipseRadiusX,
                y2 = topY + insetCircleRadius,
                weight = circularArcWeight,
            )

            // Bottom-right quadrant of ellipse
            conicTo(
                x1 = w / 2f.rf + insetEllipseRadiusX,
                y1 = bottomY,
                x2 = w / 2f.rf,
                y2 = bottomY,
                weight = circularArcWeight,
            )

            // Bottom-left quadrant of ellipse
            conicTo(
                x1 = w / 2f.rf - insetEllipseRadiusX,
                y1 = bottomY,
                x2 = w / 2f.rf - insetEllipseRadiusX,
                y2 = topY + insetCircleRadius,
                weight = circularArcWeight,
            )

            // Top-left corner
            conicTo(
                x1 = w / 2f.rf - insetEllipseRadiusX,
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

/**
 * Calculates the available content width for [RemoteEdgeButton] matching EdgeButton's ShapeHelper.
 *
 * @return the available content width in [RemoteDp]
 */
internal fun calculateEdgeButtonContentWidth(
    actualButtonWidth: RemoteDp,
    buttonHeight: RemoteDp,
): RemoteDp {
    // Calculation operates in DP space using public RemoteDp arithmetic so constant heights fold
    // at creation time.
    val extraSmallHeight = RemoteEdgeButtonSize.ExtraSmall.maximumHeight
    val bottomPadding = RemoteEdgeButtonDefaults.VerticalPadding
    val extraSmallEllipseHeight = ExtraSmallEllipseHeight
    val buttonToEllipseRatio = ButtonToEllipseRatio
    val targetSidePadding = TargetSidePadding

    val finalFadeProgress = clamp(1f.rf - buttonHeight / extraSmallHeight, 0f.rf, 1f.rf)
    val ellipseHeight =
        (extraSmallEllipseHeight + (buttonHeight - extraSmallHeight) * buttonToEllipseRatio) *
            (1f.rf - finalFadeProgress) + buttonHeight * finalFadeProgress

    val screenRadius = actualButtonWidth / 2f.rf
    val diff = max(screenRadius - bottomPadding - ellipseHeight / 2f.rf, 0.rdp)
    val screenRadiusScalar = screenRadius / 1.rdp
    val diffScalar = diff / 1.rdp
    val localHalfWidth =
        sqrt(max(screenRadiusScalar * screenRadiusScalar - diffScalar * diffScalar, 0f.rf))
            .asRemoteDp()
    val r = buttonHeight - ellipseHeight / 2f.rf

    return max(localHalfWidth - targetSidePadding - r, 0.rdp) * 2f.rf
}
