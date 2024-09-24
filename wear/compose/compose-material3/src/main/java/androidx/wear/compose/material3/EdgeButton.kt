/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.wear.compose.materialcore.screenWidthDp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Wear Material3 [EdgeButton] that offers a single slot to take any content.
 *
 * The [EdgeButton] has a special shape designed for the bottom of the screen, as it almost follows
 * the screen's curvature, so it should be allowed to take the full width and touch the bottom of
 * the screen. It has 4 standard sizes, taking 1 line of text for the extra small, 2 for small and
 * medium, and 3 for the large. See the standard values on [ButtonDefaults], and specify it using
 * the buttonSize parameter. Optionally, a single icon can be used instead of the text.
 *
 * This button represents the most important action on the screen, and must take the whole width of
 * the screen as well as being anchored to the screen bottom.
 *
 * [EdgeButton] takes the [ButtonDefaults.buttonColors] color scheme by default, with colored
 * background, contrasting content color and no border. This is a high-emphasis button for the
 * primary, most important or most common action on a screen. Other possible colors for different
 * levels of emphasis are: [FilledTonalButton] which defaults to
 * [ButtonDefaults.filledTonalButtonColors] and [OutlinedButton] which defaults to
 * [ButtonDefaults.outlinedButtonColors]
 *
 * [EdgeButton] is not intended to be used with an image background.
 *
 * Edge button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of an [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonSample
 *
 * For a sample integrating with ScalingLazyColumn, see:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonListSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button. When animating the button to appear/
 *   disappear from the screen, a Modifier.height can be used to change the height of the component,
 *   but that won't change the space available for the content (though it may be scaled)
 * @param preferredHeight Defines the base size of the button, see the 4 standard sizes specified in
 *   [ButtonDefaults]. This is used to determine the size constraints passed on to the content, and
 *   the size of the edge button if no height Modifier is specified. Note that if a height Modifier
 *   is specified for the EdgeButton, that will determine the size of the container, and the content
 *   may be scaled and or have alpha applied to fit. The default is
 *   [ButtonDefaults.EdgeButtonHeightSmall]
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 *   this button in different states. See [ButtonDefaults.buttonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this button in
 *   different states.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content Slot for composable body content displayed on the Button. Either an Icon or Text.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
fun EdgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preferredHeight: Dp = ButtonDefaults.EdgeButtonHeightSmall,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val easing = CubicBezierEasing(0.25f, 0f, 0.75f, 1.0f)

    val density = LocalDensity.current
    val screenWidthDp = screenWidthDp().dp

    val contentShapeHelper =
        remember(preferredHeight) {
            ShapeHelper(density).apply {
                // Compute the inner size using only the screen size and the buttonSize parameter
                val size = with(density) { DpSize(screenWidthDp, preferredHeight).toSize() }
                update(size)
            }
        }

    val containerShapeHelper = remember { ShapeHelper(density) }
    val shape = EdgeButtonShape(containerShapeHelper)

    val containerFadeStartPx = with(LocalDensity.current) { CONTAINER_FADE_START_DP.toPx() }
    val containerFadeEndPx = with(LocalDensity.current) { CONTAINER_FADE_END_DP.toPx() }
    val contentFadeStartPx = with(LocalDensity.current) { CONTENT_FADE_START_DP.toPx() }
    val contentFadeEndPx = with(LocalDensity.current) { CONTENT_FADE_END_DP.toPx() }

    val borderModifier =
        if (border != null) Modifier.border(border = border, shape = shape) else Modifier
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier =
            modifier
                .padding(vertical = VERTICAL_PADDING)
                .layout { measurable, constraints ->
                    // Compute the actual size of the button, and save it for later.
                    // We take the max width available, and the height is determined by the
                    // buttonSize
                    // coerced to the constraints at this point.
                    // We behave similar to .fillMaxWidth().height(buttonSize)
                    val buttonWidthPx =
                        if (constraints.hasBoundedWidth) {
                            constraints.maxWidth
                        } else {
                            screenWidthDp.roundToPx()
                        }
                    val buttonHeightPx = with(density) { preferredHeight.roundToPx() }
                    val size =
                        IntSize(
                            buttonWidthPx,
                            buttonHeightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
                        )
                    containerShapeHelper.update(size.toSize())

                    val placeable =
                        measurable.measure(
                            Constraints(size.width, size.width, size.height, size.height)
                        )
                    layout(size.width, size.height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    // Container fades when button height goes from 18dp to 0dp
                    val height = (containerShapeHelper.lastSize.value?.height ?: 0f)
                    alpha =
                        easing
                            .transform(
                                (height - containerFadeEndPx) /
                                    ((containerFadeStartPx - containerFadeEndPx))
                            )
                            .coerceIn(0f, 1f)
                }
                .then(borderModifier)
                .clip(shape = shape)
                .paint(
                    painter = colors.containerPainter(enabled = enabled),
                    contentScale = ContentScale.Crop
                )
                .graphicsLayer {
                    // Compose the content in an offscreen layer, so we can apply the gradient mask
                    // to
                    // it.
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    val height = (containerShapeHelper.lastSize.value?.height ?: 0f)

                    val alpha =
                        easing
                            .transform(
                                (height - contentFadeEndPx) /
                                    ((contentFadeStartPx - contentFadeEndPx))
                            )
                            .coerceIn(0f, 1f)

                    drawContent()
                    // Draw the gradient.
                    // We use the max dimension (width) as a proxy for screen size.
                    val r = size.maxDimension / 2f
                    val center = Offset(r, size.height - r)
                    drawRect(
                        Brush.radialGradient(
                            0.875f to Color.White.copy(alpha),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = r
                        ),
                        blendMode = BlendMode.Modulate
                    )
                }
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button,
                    indication = ripple(),
                    interactionSource = interactionSource,
                )
                .sizeAndOffset { containerShapeHelper.contentWindow }
                .scaleAndAlignContent()
                // Limit the content size to the expected width for the button size.
                .requiredSizeIn(
                    maxWidth = contentShapeHelper.contentWidthDp(),
                ),
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                MaterialTheme.typography.labelMedium,
                textConfiguration =
                    TextConfiguration(
                        TextAlign.Center,
                        TextOverflow.Ellipsis,
                        maxLines = 3, // TODO(): Change according to buttonHeight
                    ),
                content
            )
    )
}

private fun Modifier.sizeAndOffset(rectFn: (Constraints) -> Rect) =
    layout { measurable, constraints ->
        val rect = rectFn(constraints)
        val placeable =
            measurable.measure(
                Constraints(
                    rect.width.roundToInt(),
                    rect.width.roundToInt(),
                    rect.height.roundToInt(),
                    rect.height.roundToInt()
                )
            )
        val wrapperWidth = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val wrapperHeight = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(wrapperWidth, wrapperHeight) {
            placeable.placeWithLayer(0, 0) {
                translationX = rect.left
                translationY = rect.top
            }
        }
    }

internal class ShapeHelper(private val density: Density) {
    private val extraSmallHeightPx =
        with(density) { ButtonDefaults.EdgeButtonHeightExtraSmall.toPx() }
    private val bottomPaddingPx = with(density) { VERTICAL_PADDING.toPx() }
    private val extraSmallEllipsisHeightPx = with(density) { EXTRA_SMALL_ELLIPSIS_HEIGHT.toPx() }
    private val targetSidePadding = with(density) { TARGET_SIDE_PADDING.toPx() }

    internal val lastSize: MutableState<Size?> = mutableStateOf(null)
    internal var sidePadding: Float = 0f
    internal var finalFadeProgress: Float = 0f
    internal var ellipsisHeight: Float = 0f
    internal var r: Float = 0f

    internal var contentWindow: Rect = Rect(0f, 0f, 0f, 0f)

    fun contentWidthDp() = with(density) { contentWindow.width.toDp() }

    fun update(size: Size) {
        lastSize.value = size

        finalFadeProgress = (1f - size.height / extraSmallHeightPx).coerceAtLeast(0f)
        ellipsisHeight =
            lerp(
                extraSmallEllipsisHeightPx +
                    (size.height - extraSmallHeightPx) * BUTTON_TO_ELLIPSIS_RATIO,
                size.height,
                finalFadeProgress
            )

        val localHalfWidth =
            sqrt(sqr(size.width / 2) - sqr(size.width / 2 - bottomPaddingPx - ellipsisHeight / 2))
        sidePadding = size.width / 2 - localHalfWidth + targetSidePadding

        r = size.height - ellipsisHeight / 2

        contentWindow = Rect(sidePadding, 0f, size.width - sidePadding, size.height).inset(r, 0f)
    }
}

internal fun Rect.size() = Size(width, height)

internal fun Rect.inset(h: Float, v: Float): Rect {
    // Ensure we never have negative sizes.
    val cx = (left + right) / 2
    val cy = (top + bottom) / 2
    val sx2 = (width / 2 - h).coerceAtLeast(0f)
    val sy2 = (height / 2 - v).coerceAtLeast(0f)
    return Rect(cx - sx2, cy - sy2, cx + sx2, cy + sy2)
}

internal class EdgeButtonShape(private val helper: ShapeHelper) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path =
            Path().apply {
                with(helper) {
                    // Top Side - Rounded Rect
                    moveTo(sidePadding, r)
                    quarterEllipsis(Offset(sidePadding + r, r), r, r, 180f)
                    lineTo(size.width - sidePadding - r, 0f)
                    quarterEllipsis(Offset(size.width - sidePadding - r, r), r, r, 270f)

                    // Bottom side - Ellipsis morphing to round rect when very small.
                    val ellipsisRadiusX =
                        lerp((size.width - 2 * sidePadding) / 2, r, finalFadeProgress)
                    val ellipsisRadiusY = ellipsisHeight / 2
                    quarterEllipsis(
                        Offset(
                            size.width - sidePadding - ellipsisRadiusX,
                            size.height - ellipsisRadiusY
                        ),
                        ellipsisRadiusX,
                        ellipsisRadiusY,
                        0f
                    )
                    lineTo(sidePadding + ellipsisRadiusX, size.height)
                    quarterEllipsis(
                        Offset(sidePadding + ellipsisRadiusX, size.height - ellipsisRadiusY),
                        ellipsisRadiusX,
                        ellipsisRadiusY,
                        90f
                    )
                }
            }

        return Outline.Generic(path)
    }
}

private fun Path.quarterEllipsis(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    startAngle: Float
) {
    arcTo(
        Rect(center.x - radiusX, center.y - radiusY, center.x + radiusX, center.y + radiusY),
        startAngle,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )
}

private fun sqr(x: Float) = x * x

// Scales the content if it doesn't fit horizontally, horizontally center if there is room.
// Vertically centers the content if there is room, otherwise aligns to the top.
private fun Modifier.scaleAndAlignContent() = this.then(ScaleAndAlignContentElement())

private class ScaleAndAlignContentElement : ModifierNodeElement<ScaleAndAlignContentNode>() {
    override fun create(): ScaleAndAlignContentNode = ScaleAndAlignContentNode()

    override fun update(node: ScaleAndAlignContentNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "ScaleAndAlignContentElement"
    }

    // All instances are equivalent
    override fun equals(other: Any?) = other is ScaleAndAlignContentElement

    override fun hashCode() = 42
}

private class ScaleAndAlignContentNode : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(Constraints())

        val topPadding = INTERNAL_TOP_PADDING.roundToPx()

        val wrapperWidth = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val wrapperHeight = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)

        val scale = (wrapperWidth.toFloat() / placeable.width.coerceAtLeast(1)).coerceAtMost(1f)

        return layout(wrapperWidth, wrapperHeight) {
            // If there is enough vertical space, we align like 'Center', otherwise like 'TopCenter'
            val position =
                IntOffset(
                    x = (wrapperWidth - placeable.width) / 2, // Always center horizontally
                    y =
                        ((wrapperHeight - placeable.height * scale) / 2)
                            .roundToInt()
                            .coerceAtLeast(topPadding)
                )
            placeable.placeWithLayer(position) {
                scaleX = scale
                scaleY = scale
                translationX
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

// Sizes at which the container will start and end fading away.
private val CONTAINER_FADE_START_DP = 30.dp
private val CONTAINER_FADE_END_DP = 4.dp

// Sizes at which the content will start and end fading away.
private val CONTENT_FADE_START_DP = 38.dp
private val CONTENT_FADE_END_DP = 30.dp

// How tall the ellipsis is for the extra small button.
// Edge buttons are drawn as half a rounded rectangle on top of half an ellipsis.
private val EXTRA_SMALL_ELLIPSIS_HEIGHT = 58.dp

// How much the ellipsis grows as the button height grows
private const val BUTTON_TO_ELLIPSIS_RATIO = 1.42f

// Distance from the leftmost/rightmost point in the button to the edge of the screen, in a
// straight line parallel to the x axis.
private val TARGET_SIDE_PADDING = 20.dp

// Padding around the Edge Button on it's top and bottom.
private val VERTICAL_PADDING = 3.dp

// Padding between the top edge of the button and the content.
private val INTERNAL_TOP_PADDING = 6.dp
