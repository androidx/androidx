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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import org.intellij.lang.annotations.Language

/**
 * A [Modifier] that curves a component to the border of the parent container around the center,
 * always treating the container as a circle. Useful for circular screens. It draws outside the
 * bounds of the container, then a shader is applied that calculates and draws the curved content.
 *
 * If the container is a rectangle, it will be treated as a circle with radius of the height divided
 * by 2.
 *
 * Content towards the center of the container will be smaller due to the method of calculating the
 * curve. This is most apparent in text that runs over one line.
 *
 * There are currently some limitations: clickable components will have incorrect clickable bounds,
 * and talkback doesn't highlight components correctly.
 *
 * Simple example of applying Modifier.curvedToEdge to text:
 *
 * @sample androidx.wear.compose.material3.samples.CurveToEdgeSample
 *
 * Example of applying Modifier.curvedToEdge to a long piece of text, with overflow handling:
 *
 * @sample androidx.wear.compose.material3.samples.LongCurveToEdgeSample
 * @param maxSweepAngle The maximum sweep angle in degrees,
 *   [CurvedTextDefaults.ScrollableContentMaxSweepAngle] by default. For screens without scrollable
 *   content, [CurvedTextDefaults.StaticContentMaxSweepAngle] may be used instead.
 * @param angularDirection Specify the direction of the content. Default is clockwise (so that text
 *   appears the correct way up while at the top of the container)
 * @param anchor The angle at which the content is laid out relative to, in degrees. An angle of 0
 *   corresponds to the right (3 o'clock on a watch), 90 degrees is bottom (6 o'clock), and so on.
 *   Default is 270 degrees (top of the container).
 * @param anchorType Specify how the content is drawn with respect to the anchor. Default is to
 *   center the content on the anchor.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public fun Modifier.curveToEdge(
    maxSweepAngle: Float = CurvedTextDefaults.ScrollableContentMaxSweepAngle,
    angularDirection: AngularDirection = AngularDirection.Clockwise,
    anchor: Float = 270f,
    anchorType: AngularAnchorType = AngularAnchorType.Center,
): Modifier =
    this then
        CurveElement(maxSweepAngle, angularDirection, anchor, anchorType) then
        Modifier.semantics(true) {}

/** Specifies how components will be laid down with respect to the anchor. */
@JvmInline
public value class AngularAnchorType internal constructor(internal val ratio: Float) {
    public companion object {
        /** Anchors the start of the content to the anchor angle. */
        public val Start: AngularAnchorType = AngularAnchorType(0f)
        /** Anchors the center of the content to the anchor angle. */
        public val Center: AngularAnchorType = AngularAnchorType(1f)
        /** Anchors the end of the content to the anchor angle. */
        public val End: AngularAnchorType = AngularAnchorType(2f)
    }

    override fun toString(): String {
        return when (this) {
            Start -> "AngularAnchorType.Start"
            Center -> "AngularAnchorType.Center"
            End -> "AngularAnchorType.End"
            else -> "unknown"
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class CurveElement(
    val maxSweepAngle: Float,
    val angularDirection: AngularDirection,
    val anchor: Float,
    val anchorType: AngularAnchorType,
) : ModifierNodeElement<CurveModifierNode>() {
    override fun create() = CurveModifierNode(maxSweepAngle, angularDirection, anchor, anchorType)

    override fun update(node: CurveModifierNode) {
        node.update(maxSweepAngle, angularDirection, anchor, anchorType)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "curveToEdge"
        properties["maxSweepAngle"] = maxSweepAngle
        properties["angularDirection"] = angularDirection
        properties["anchor"] = anchor
        properties["anchorType"] = anchorType
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class CurveModifierNode(
    var maxSweepAngle: Float,
    var angularDirection: AngularDirection,
    var anchor: Float,
    var anchorType: AngularAnchorType,
) : CompositionLocalConsumerModifierNode, LayoutModifierNode, Modifier.Node() {
    override val shouldAutoInvalidate: Boolean
        get() = false

    private val shader = RuntimeShader(CURVED_TEXT)
    private var circleStartNoAnchor = 0f

    fun update(
        maxSweepAngle: Float,
        angularDirection: AngularDirection,
        anchor: Float,
        anchorType: AngularAnchorType,
    ) {
        // maxSweepAngle changes constraints, so remeasure
        if (maxSweepAngle != this.maxSweepAngle) {
            invalidateMeasurement()
        } else {
            // anchorType changes all but the centre and contentHeight uniforms
            if (anchorType != this.anchorType || anchor != this.anchor) {
                // Only offset uniform needs to be changed on anchor change
                val circleStart = circleStartNoAnchor * anchorType.ratio
                shader.setFloatUniform(
                    "offset",
                    (circleStart + 2f * PI - anchor.toRadians()).toFloat(),
                )
                if (anchorType != this.anchorType) {
                    val halfMaxSweepAngle = maxSweepAngle.toRadians() / 2f
                    shader.setFloatUniform("lowerSweepBound", circleStart - halfMaxSweepAngle)
                    shader.setFloatUniform("upperSweepBound", circleStart + halfMaxSweepAngle)
                }
            }
            // Only direction uniform needs to be changed on direction change
            if (angularDirection != this.angularDirection)
                shader.setIntUniform("angularDirection", angularDirection.type)
            // Only redraw the graphicsLayer; reapply the shader since it has new uniforms
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
        val maxWidth = min(constraints.maxWidth, config.screenWidthDp.dp.toPx().toInt())
        val maxHeight = min(constraints.maxHeight, config.screenHeightDp.dp.toPx().toInt())

        val halfMaxSweepAngleRad = maxSweepAngle * PI / 360
        // Set the maximum width of the component to the arc length
        val arc = maxHeight.toFloat() * halfMaxSweepAngleRad
        val childConstraints = constraints.copy(minWidth = 0, maxWidth = arc.toInt())

        val placeable = measurable.measure(childConstraints)
        val radius = maxHeight.toFloat() / 2f
        circleStartNoAnchor = (placeable.width.toFloat() / radius) / 2f
        val circleStart = circleStartNoAnchor * anchorType.ratio
        val yOffset = (radius * (1 - cos(2 * halfMaxSweepAngleRad))).toFloat()
        val centreY =
            if (angularDirection == AngularDirection.CounterClockwise) radius - yOffset.toInt()
            else radius

        shader.setFloatUniform("centre", radius, centreY)
        shader.setFloatUniform("lowerSweepBound", circleStart - halfMaxSweepAngleRad.toFloat())
        shader.setFloatUniform("upperSweepBound", circleStart + halfMaxSweepAngleRad.toFloat())
        shader.setFloatUniform("offset", (circleStart + 2f * PI - anchor.toRadians()).toFloat())
        shader.setFloatUniform("contentHeight", placeable.height.toFloat())
        shader.setIntUniform("angularDirection", angularDirection.type)

        // Don't take up full height so that this can be used on a component at the top of a column
        return layout(maxWidth, placeable.height) {
            // RTL should already be sorted; all we do is curve therefore do not use placeRelative
            placeable.placeWithLayer(
                0,
                if (angularDirection == AngularDirection.CounterClockwise) yOffset.toInt() else 0,
            ) {
                this.renderEffect =
                    RenderEffect.createRuntimeShaderEffect(shader, "contents")
                        .asComposeRenderEffect()
            }
        }
    }
}

@Language("AGSL")
private val CURVED_TEXT =
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
        float dist = distance(centre, fragCoord);
        if (dist > radius || dist < radius - contentHeight) return half4(0);
        float2 cv = fragCoord - centre;
        // ang is the angle between the x axis line and the point
        float ang = atan(cv.y, cv.x);
        if (angularDirection != 0) ang = -ang - PI;
        // We have to mod x by 2*PI to deal with clipping that happens otherwise
        float x = mod(ang + offset.x, TWOPI);
        // Only render text if bounds are between the maximum sweep angle
        if (x <= lowerSweepBound || x >= upperSweepBound) return half4(0);
        float y = abs(log(dist / radius));
        // Multiply the vector by the radius to get the final coordinate
        float2 final = float2(x, y) * float2(radius);
        if (angularDirection != 0) final.y = contentHeight - final.y;
        return contents.eval(final);
    }
    """
        .trimIndent()
