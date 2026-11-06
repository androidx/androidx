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

package androidx.wear.compose.material3

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AnchorType
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import org.intellij.lang.annotations.Language

/**
 * A [Modifier] that transforms a component to an arc around the border of its circular, parent
 * container. Useful for circular screens. It works by first drawing outside the bounds of the
 * container, then a shader is applied that calculates and draws the curved content. **This only
 * affects the drawing of the content.**
 *
 * This is a performant alternative to [androidx.wear.compose.foundation.CurvedLayout]. The shader
 * implementation warps everything, whereas CurvedLayout with
 * [androidx.wear.compose.foundation.curvedComposable] isn't able to do warping (you can only warp
 * [curvedText] using the warpOffset parameter in
 * [androidx.wear.compose.foundation.CurvedTextStyle]). Use this for simple curved content (such as
 * a single line of text, icons or a row of small composables) when targeting API 33 and higher.
 *
 * Note that Wear Material 3 UX guidance recommends that curved content should not exceed the sweep
 * angle [DrawCurvedToEdgeDefaults.ScrollableContentMaxSweepAngle] on screens with scrollable
 * content such as lists. This limit is enforced by default. For screens without scrollable content,
 * [DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle] may be used instead.
 *
 * If the container is a rectangle, it will be treated as a circle with radius of the height divided
 * by 2.
 *
 * The content is wrapped along the edge of the container and shrinks the further in from the edge
 * it is drawn: a second line of text is visibly smaller than the first, and tall content tapers
 * towards the center. Do not inset the content with padding or spacers on the center-facing side,
 * that only shrinks it; to draw the arc further from the edge, pad the parent container instead.
 * Apply [Modifier.drawCurvedToEdge] before any layout modifiers (such as `Modifier.padding` or
 * `Modifier.offset`) on the same element. Additionally, any modifiers applied after
 * [Modifier.drawCurvedToEdge] are warped as part of the curved content itself.
 *
 * Not recommended for clickable components, as the clickable bounds cannot be modified to match the
 * new morphed shape. Talkback also doesn't highlight the components correctly.
 *
 * Simple example of applying Modifier.drawCurvedToEdge to text:
 *
 * @sample androidx.wear.compose.material3.samples.DrawCurvedToEdgeSample
 *
 * ![DrawCurvedToEdgeSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_DrawCurvedToEdgeSample_CompositeImage.png)
 *
 * Example of drawing the arc further in from the edge by padding the parent container, with
 * overflow handling for a long piece of text:
 *
 * @sample androidx.wear.compose.material3.samples.InsetDrawCurvedToEdgeSample
 *
 * ![InsetDrawCurvedToEdgeSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_InsetDrawCurvedToEdgeSample_CompositeImage.png)
 *
 * Example of applying Modifier.drawCurvedToEdge to a row of Icons:
 *
 * @sample androidx.wear.compose.material3.samples.IconsDrawCurvedToEdgeSample
 *
 * ![IconsDrawCurvedToEdgeSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_IconsDrawCurvedToEdgeSample_CompositeImage.png)
 *
 * Example of applying Modifier.drawCurvedToEdge to mixed content:
 *
 * @sample androidx.wear.compose.material3.samples.MixedContentDrawCurvedToEdgeSample
 *
 * ![MixedContentDrawCurvedToEdgeSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_MixedContentDrawCurvedToEdgeSample_CompositeImage.png)
 *
 * @param maxSweepAngle The maximum sweep angle in degrees,
 *   [DrawCurvedToEdgeDefaults.ScrollableContentMaxSweepAngle] by default. For screens without
 *   scrollable content, [DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle] may be used instead.
 * @param angularDirection Specify the direction of the content. Default is clockwise (so that text
 *   appears the correct way up while at the top of the container).
 * @param anchor The angle at which the content is laid out relative to, in degrees. An angle of 0
 *   corresponds to the right (3 o'clock on a watch), 90 degrees is bottom (6 o'clock), and so on.
 *   Default is 270 degrees (top of the container).
 * @param anchorType Specify how the content is drawn with respect to the anchor. Default is to
 *   center the content on the anchor.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public fun Modifier.drawCurvedToEdge(
    maxSweepAngle: Float = DrawCurvedToEdgeDefaults.ScrollableContentMaxSweepAngle,
    angularDirection: AngularDirection = AngularDirection.Clockwise,
    anchor: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
): Modifier =
    this then
        DrawCurvedToEdgeElement(maxSweepAngle, angularDirection, anchor, anchorType) then
        Modifier.semantics(true) {}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public object DrawCurvedToEdgeDefaults {

    /**
     * The default maximum sweep angle in degrees used by [drawCurvedToEdge]. This threshold is for
     * the [drawCurvedToEdge] displayed on screens with scrollable content, such as lists.
     *
     * This is calculated by keeping the length of the corresponding chord on the circle to be
     * approximately 57% of the screen width.
     */
    public val ScrollableContentMaxSweepAngle: Float = 70f

    /**
     * The recommended maximum sweep angle in degrees used by [drawCurvedToEdge] for screens without
     * scrollable content.
     */
    public val StaticContentMaxSweepAngle: Float = 120f
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class DrawCurvedToEdgeElement(
    val maxSweepAngle: Float,
    val angularDirection: AngularDirection,
    val anchor: Float,
    val anchorType: AnchorType,
) : ModifierNodeElement<DrawCurvedToEdgeNode>() {
    override fun create() =
        DrawCurvedToEdgeNode(maxSweepAngle, angularDirection, anchor, anchorType)

    override fun update(node: DrawCurvedToEdgeNode) {
        node.update(maxSweepAngle, angularDirection, anchor, anchorType)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawCurvedToEdge"
        properties["maxSweepAngle"] = maxSweepAngle
        properties["angularDirection"] = angularDirection
        properties["anchor"] = anchor
        properties["anchorType"] = anchorType
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class DrawCurvedToEdgeNode(
    var maxSweepAngle: Float,
    var angularDirection: AngularDirection,
    var anchor: Float,
    var anchorType: AnchorType,
) : CompositionLocalConsumerModifierNode, LayoutModifierNode, Modifier.Node() {
    override val shouldAutoInvalidate: Boolean
        get() = false

    private val shader = RuntimeShader(CURVED_SHADER)
    private var circleStartNoAnchor = 0f

    fun update(
        maxSweepAngle: Float,
        angularDirection: AngularDirection,
        anchor: Float,
        anchorType: AnchorType,
    ) {
        // maxSweepAngle changes constraints, so remeasure
        if (maxSweepAngle.coerceIn(0f, 360f) != this.maxSweepAngle.coerceIn(0f, 360f)) {
            invalidateMeasurement()
        } else {
            if (angularDirection != this.angularDirection) {
                shader.setIntUniform("angularDirection", angularDirection.type)
            }
            val circleStart = circleStartNoAnchor * anchorType.toRatio()
            shader.setFloatUniform("offset", calculateOffset(circleStart, anchor, angularDirection))
            // Redraw the graphicsLayer with updated shader uniforms
            invalidatePlacement()
        }
        // Update state
        this.maxSweepAngle = maxSweepAngle
        this.angularDirection = angularDirection
        this.anchor = anchor
        this.anchorType = anchorType
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val config = currentValueOf(LocalConfiguration)
        val screenSizePx = config.screenWidthDp.dp.toPx().toInt()
        val maxWidth = min(constraints.maxWidth, screenSizePx)
        val maxHeight = min(constraints.maxHeight, screenSizePx)

        val clampedSweepAngle = maxSweepAngle.coerceIn(0f, 360f)
        val halfMaxSweepAngleRad = clampedSweepAngle.toRadians() / 2f
        // Set the maximum width of the component to the arc length
        val arc = maxHeight.toFloat() * halfMaxSweepAngleRad
        val childConstraints = constraints.copy(minWidth = 0, maxWidth = arc.toInt())

        val placeable = measurable.measure(childConstraints)
        val radius = maxHeight.toFloat() / 2f
        circleStartNoAnchor = if (radius > 0f) placeable.width.toFloat() / radius else 0f
        val contentMid = circleStartNoAnchor * 0.5f
        val circleStart = circleStartNoAnchor * anchorType.toRatio()

        shader.setFloatUniform("centre", radius, radius)
        shader.setFloatUniform("lowerSweepBound", contentMid - halfMaxSweepAngleRad)
        shader.setFloatUniform("upperSweepBound", contentMid + halfMaxSweepAngleRad)
        shader.setFloatUniform("offset", calculateOffset(circleStart, anchor, angularDirection))
        shader.setFloatUniform("contentHeight", placeable.height.toFloat())
        shader.setIntUniform("angularDirection", angularDirection.type)

        // Don't take up full height so that this can be used on a component at the top of a column
        return layout(maxWidth, placeable.height) {
            // Enforce placing at the border of the parent container
            val offsetInParent = coordinates?.positionInParent() ?: Offset.Zero
            val enforcedX = -offsetInParent.x.roundToInt()
            val enforcedY = -offsetInParent.y.roundToInt()
            // This code does not need to take account of RTL, because that will already have been
            // done in the content composables. Therefore, we do not use placeRelative here.
            placeable.placeWithLayer(enforcedX, enforcedY) {
                this.renderEffect =
                    RenderEffect.createRuntimeShaderEffect(shader, "contents")
                        .asComposeRenderEffect()
            }
        }
    }
}

private fun AnchorType.toRatio(): Float =
    when (this) {
        AnchorType.Start -> 0f
        AnchorType.End -> 1f
        else -> 0.5f
    }

private fun calculateOffset(
    circleStart: Float,
    anchor: Float,
    angularDirection: AngularDirection,
): Float {
    val normalizedAnchorRad = (((anchor % 360f) + 360f) % 360f).toRadians()
    return if (angularDirection == AngularDirection.CounterClockwise) {
        circleStart + normalizedAnchorRad
    } else {
        circleStart + 2f * PI.toFloat() - normalizedAnchorRad
    }
}

@Language("AGSL")
private val CURVED_SHADER =
    """
    uniform float2 centre;
    uniform float contentHeight;
    uniform float lowerSweepBound;
    uniform float upperSweepBound;
    uniform float offset;
    uniform int angularDirection;
    uniform shader contents;

    // Look up contents using a log-polar coordinate system to make it circular
    half4 main(float2 fragCoord) {
        const float PI = 3.14159265359;
        const float TWOPI = PI * 2;
        float radius = centre.x;
        if (radius <= 0.0) return half4(0);
        float dist = distance(centre, fragCoord);
        if (dist > radius || dist <= max(0.0, radius - contentHeight)) return half4(0);
        float2 cv = fragCoord - centre;
        // ang is the angle between the x axis line and the point
        float ang = atan(cv.y, cv.x) * (angularDirection == 0 ? 1 : -1);
        // We have to mod x by 2*PI to deal with clipping that happens otherwise
        float x = mod(ang + offset, TWOPI);
        // Only render content if bounds are between the maximum sweep angle
        if (x < lowerSweepBound || x >= upperSweepBound) return half4(0);
        float y = abs(log(dist / radius));
        // Multiply the vector by the radius to get the final coordinate
        float2 final = float2(x, y) * float2(radius);
        if (angularDirection != 0) final.y = contentHeight - final.y;
        return contents.eval(final);
    }
    """
        .trimIndent()
