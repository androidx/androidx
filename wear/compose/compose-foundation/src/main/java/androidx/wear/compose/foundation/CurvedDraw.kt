/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI

/**
 * Specified a solid background for a curved element.
 *
 * @param color The color to use to paint the background.
 * @param cap How to start and end the background.
 */
public fun CurvedModifier.background(
    color: Color,
    cap: StrokeCap = StrokeCap.Butt,
) = background(cap) { SolidColor(color) }

/**
 * Specifies a radial gradient background for a curved element.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedBackground
 *
 * @param colorStops Colors and their offset in the gradient area.
 * Note that the offsets should be in ascending order. 0 means the outer curve and
 * 1 means the inner curve of the curved element.
 * @param cap How to start and end the background.
 */
public fun CurvedModifier.radialGradientBackground(
    vararg colorStops: Pair<Float, Color>,
    cap: StrokeCap = StrokeCap.Butt
) = background(cap) { layoutInfo ->
    val radiusRatio = layoutInfo.innerRadius / layoutInfo.outerRadius
    @Suppress("ListIterator")
    Brush.radialGradient(
        *(colorStops.map { (step, color) ->
            1f - step * (1f - radiusRatio) to color
        }.reversed().toTypedArray()),
        center = layoutInfo.centerOffset,
        radius = layoutInfo.outerRadius
    )
}

/**
 * Specifies a radial gradient background for a curved element.
 *
 * @param colors Colors in the gradient area. Gradient goes from the outer curve to the
 * inner curve of the curved element.
 * @param cap How to start and end the background.
 */
public fun CurvedModifier.radialGradientBackground(
    colors: List<Color>,
    cap: StrokeCap = StrokeCap.Butt
) = radialGradientBackground(*colorsToColorStops(colors), cap = cap)

/**
 * Specifies a sweep gradient background for a curved element.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedBackground
 *
 * @param colorStops Colors and their offset in the gradient area.
 * Note that the offsets should be in ascending order. 0 means where the curved element starts
 * laying out, 1 means the end
 * @param cap How to start and end the background.
 */
public fun CurvedModifier.angularGradientBackground(
    vararg colorStops: Pair<Float, Color>,
    cap: StrokeCap = StrokeCap.Butt
) = background(cap) { layoutInfo ->
    @Suppress("ListIterator")
    val actualStops = colorStops.map { (step, color) ->
        (layoutInfo.startAngleRadians + layoutInfo.sweepRadians * step) /
            (2 * PI).toFloat() to color
    }.sortedBy { it.first }
    Brush.sweepGradient(*(actualStops.toTypedArray()))
}

/**
 * Specifies a sweep gradient background for a curved element.
 *
 * @param colors Colors in the gradient area. Gradient goes in the clockwise direction.
 * @param cap How to start and end the background.
 */
public fun CurvedModifier.angularGradientBackground(
    colors: List<Color>,
    cap: StrokeCap = StrokeCap.Butt
) = angularGradientBackground(*colorsToColorStops(colors), cap = cap)

private fun colorsToColorStops(colors: List<Color>): Array<Pair<Float, Color>> =
    Array(colors.size) {
        it.toFloat() / (colors.size - 1) to colors[it]
    }

internal fun CurvedModifier.background(
    cap: StrokeCap = StrokeCap.Butt,
    brushProvider: (CurvedLayoutInfo) -> Brush
) = drawBefore {
    with(it) {
        val radius = outerRadius - thickness / 2
        drawArc(
            brushProvider(it),
            startAngleRadians.toDegrees(),
            sweepRadians.toDegrees(),
            useCenter = false,
            topLeft = centerOffset - Offset(radius, radius),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(thickness, cap = cap)
        )
    }
}

internal class DrawWrapper(
    child: CurvedChild,
    val customDraw: DrawScope.(CurvedLayoutInfo) -> Unit,
    val drawBefore: Boolean
) : BaseCurvedChildWrapper(child) {

    private var parentOuterRadius: Float = 0f
    private var parentThickness: Float = 0f

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        this.parentThickness = parentThickness
        this.parentOuterRadius = parentOuterRadius
        return wrapped.radialPosition(
            parentOuterRadius,
            parentThickness,
        )
    }

    private lateinit var outerLayoutInfo: CurvedLayoutInfo

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        /* We want the background to fill the space that our parent assigned us (outerLayoutInfo),
         * as opposed to the size of or wrapped child (layoutInfo).
         */
        outerLayoutInfo = CurvedLayoutInfo(
            sweepRadians = parentSweepRadians,
            outerRadius = parentOuterRadius,
            thickness = parentThickness,
            centerOffset = centerOffset,
            measureRadius = parentOuterRadius - parentThickness / 2f,
            startAngleRadians = parentStartAngleRadians
        )
        return wrapped.angularPosition(
            parentStartAngleRadians,
            parentSweepRadians,
            centerOffset
        )
    }

    override fun DrawScope.draw() {
        if (drawBefore) customDraw(outerLayoutInfo)
        with(wrapped) {
            draw()
        }
        if (!drawBefore) customDraw(outerLayoutInfo)
    }
}

internal fun CurvedModifier.drawAfter(draw: DrawScope.(CurvedLayoutInfo) -> Unit) =
    this.then { child -> DrawWrapper(child, draw, drawBefore = false) }

internal fun CurvedModifier.drawBefore(draw: DrawScope.(CurvedLayoutInfo) -> Unit) =
    this.then { child -> DrawWrapper(child, draw, drawBefore = true) }
