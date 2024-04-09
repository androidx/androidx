/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val LOG_TAG = "ShapeEditor"

data class ShapeItem(
    val name: String,
    val shapegen: () -> RoundedPolygon,
    val shapeOutput: () -> String,
    val usesSides: Boolean = true,
    val usesWH: Boolean = false,
    val usesPillStarFactor: Boolean = false,
    val usesInnerRatio: Boolean = true,
    val usesRoundness: Boolean = true,
    val usesInnerParameters: Boolean = true
)

class ShapeParameters(
    sides: Int = 5,
    innerRadius: Float = 0.5f,
    roundness: Float = 0f,
    smooth: Float = 0f,
    innerRoundness: Float = roundness,
    innerSmooth: Float = smooth,
    rotation: Float = 0f,
    width: Float = 4f,
    height: Float = 1f,
    pillStarFactor: Float = .5f,
    shapeId: ShapeId = ShapeId.Polygon
) {
    internal val sides = mutableFloatStateOf(sides.toFloat())
    internal val innerRadius = mutableFloatStateOf(innerRadius)
    internal val roundness = mutableFloatStateOf(roundness)
    internal val smooth = mutableFloatStateOf(smooth)
    internal val innerRoundness = mutableFloatStateOf(innerRoundness)
    internal val innerSmooth = mutableFloatStateOf(innerSmooth)
    internal val rotation = mutableFloatStateOf(rotation)
    internal val width = mutableFloatStateOf(width)
    internal val height = mutableFloatStateOf(height)
    internal val pillStarFactor = mutableFloatStateOf(pillStarFactor)

    internal var shapeIx by mutableIntStateOf(shapeId.ordinal)

    fun copy() = ShapeParameters(
        this.sides.floatValue.roundToInt(),
        this.innerRadius.floatValue,
        this.roundness.floatValue,
        this.smooth.floatValue,
        this.innerRoundness.floatValue,
        this.innerSmooth.floatValue,
        this.rotation.floatValue,
        this.width.floatValue,
        this.height.floatValue,
        this.pillStarFactor.floatValue,
        ShapeId.values()[this.shapeIx]
    )

    enum class ShapeId {
        Pill, PillStar, Star, Polygon, Triangle, Blob, CornerSE, Circle, Rectangle
    }

    private fun radialToCartesian(
        radius: Float,
        angleRadians: Float,
        center: Offset = Offset.Zero
    ) = directionVector(angleRadians) * radius + center

    // Primitive shapes we can draw (so far)
    internal val shapes = listOf(
        ShapeItem(
            "Pill", shapegen = {
                RoundedPolygon.pill(width = this.width.floatValue, height = this.height.floatValue,
                    smoothing = this.smooth.floatValue)
            },
            shapeOutput = {
                shapeDescription(
                    id = "Pill", width = this.width.floatValue, height = this.height.floatValue,
                    code = "RoundedPolygon.pill(width = $this.width.floatValue, " +
                        "height = $this.height.floatValue)"
                )
            },
            usesSides = false, usesInnerParameters = false, usesInnerRatio = false,
            usesRoundness = true, usesWH = true
        ),
        ShapeItem(
            "PillStar", shapegen = {
                RoundedPolygon.pillStar(
                    width = this.width.floatValue, height = this.height.floatValue,
                    numVerticesPerRadius = this.sides.floatValue.roundToInt(),
                    innerRadiusRatio = this.innerRadius.floatValue,
                    rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                    innerRounding = CornerRounding(
                        this.innerRoundness.floatValue,
                        this.innerSmooth.floatValue
                    ),
                    vertexSpacing = this.pillStarFactor.floatValue
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "PillStar",
                    width = this.width.floatValue, height = this.height.floatValue,
                    numVerts = this.sides.floatValue.roundToInt(),
                    innerRadius = this.innerRadius.floatValue,
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    innerRoundness = this.innerRoundness.floatValue,
                    innerSmooth = this.innerSmooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon.pillStar(width = $width, height = $height," +
                        "numVerticesPerRadius = $sides, " +
                        "innerRadius = ${innerRadius}f, " +
                        "rounding = CornerRounding(${roundness}f, ${smooth}f), " +
                        "innerRounding = CornerRounding(${innerRoundness}f, ${innerSmooth}f))"
                )
            },
            usesWH = true, usesPillStarFactor = true
        ),
        ShapeItem(
            "Star", shapegen = {
                RoundedPolygon.star(
                    radius = 2f,
                    numVerticesPerRadius = this.sides.floatValue.roundToInt(),
                    innerRadius = this.innerRadius.floatValue,
                    rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                    innerRounding = CornerRounding(
                        this.innerRoundness.floatValue,
                        this.innerSmooth.floatValue
                    )
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "Star", sides = this.sides.floatValue.roundToInt(),
                    innerRadius = this.innerRadius.floatValue,
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    innerRoundness = this.innerRoundness.floatValue,
                    innerSmooth = this.innerSmooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon.star(" +
                        "radius = 2f, " +
                        "numVerticesPerRadius = ${this.sides.floatValue.roundToInt()}, " +
                        "innerRadius = ${this.innerRadius.floatValue}f, " +
                        "rounding = " +
                        "CornerRounding(${this.roundness.floatValue}f," +
                        "${this.smooth.floatValue}f), " +
                        "innerRounding = CornerRounding(${this.innerRoundness.floatValue}f, " +
                        "${this.innerSmooth.floatValue}f))"
                )
            },
        ),
        ShapeItem(
            "Polygon", shapegen = {
                RoundedPolygon(
                    numVertices = this.sides.floatValue.roundToInt(),
                    rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "Polygon",
                    sides = this.sides.floatValue.roundToInt(),
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon(numVertices = ${this.sides.floatValue.roundToInt()}," +
                        "rounding = CornerRounding(${this.roundness.floatValue}f, " +
                        "${this.smooth.floatValue}f))"
                )
            },
            usesInnerRatio = false, usesInnerParameters = false
        ),
        ShapeItem(
            "Triangle", shapegen = {
                val points = floatArrayOf(
                    radialToCartesian(1f, 270f.toRadians()).x,
                    radialToCartesian(1f, 270f.toRadians()).y,
                    radialToCartesian(1f, 30f.toRadians()).x,
                    radialToCartesian(1f, 30f.toRadians()).y,
                    radialToCartesian(this.innerRadius.floatValue, 90f.toRadians()).x,
                    radialToCartesian(this.innerRadius.floatValue, 90f.toRadians()).y,
                    radialToCartesian(1f, 150f.toRadians()).x,
                    radialToCartesian(1f, 150f.toRadians()).y
                )
                RoundedPolygon(
                    points,
                    CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                    centerX = 0f,
                    centerY = 0f
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "Triangle",
                    innerRadius = this.innerRadius.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "val points = floatArrayOf(" +
                        "    radialToCartesian(1f, 270f.toRadians()).x,\n" +
                        "    radialToCartesian(1f, 270f.toRadians()).y,\n" +
                        "    radialToCartesian(1f, 30f.toRadians()).x,\n" +
                        "    radialToCartesian(1f, 30f.toRadians()).y,\n" +
                        "    radialToCartesian(${this.innerRadius.floatValue}f, " +
                        "90f.toRadians()).x,\n" +
                        "    radialToCartesian(${this.innerRadius.floatValue}f, " +
                        "90f.toRadians()).y,\n" +
                        "    radialToCartesian(1f, 150f.toRadians()).x,\n" +
                        "    radialToCartesian(1f, 150f.toRadians()).y)\n" +
                        "RoundedPolygon(points, CornerRounding(" +
                        "${this.roundness.floatValue}f, ${this.smooth.floatValue}f), " +
                        "centerX = 0f, centerY = 0f)"
                )
            },
            usesSides = false, usesInnerParameters = false
        ),
        ShapeItem(
            "Blob", shapegen = {
                val sx = this.innerRadius.floatValue.coerceAtLeast(0.1f)
                val sy = this.roundness.floatValue.coerceAtLeast(0.1f)
                RoundedPolygon(
                    vertices = floatArrayOf(
                        -sx, -sy,
                        sx, -sy,
                        sx, sy,
                        -sx, sy,
                    ),
                    rounding = CornerRounding(min(sx, sy), this.smooth.floatValue),
                    centerX = 0f, centerY = 0f
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "Blob",
                    innerRadius = this.innerRadius.floatValue,
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "val sx = ${this.innerRadius.floatValue}f.coerceAtLeast(0.1f)\n" +
                        "val sy = ${this.roundness.floatValue}f.coerceAtLeast(.1f)\n" +
                        "val verts = floatArrayOf(-sx, -sy, sx, -sy, sx, sy, -sx, sy)\n" +
                        "RoundedPolygon(verts, rounding = CornerRounding(min(sx, sy), " +
                        "${this.smooth.floatValue}f)," +
                        "centerX = 0f, centerY = 0f)"
                )
            },
            usesSides = false, usesInnerParameters = false
        ),
        ShapeItem(
            "CornerSE", shapegen = {
                RoundedPolygon(
                    squarePoints(),
                    perVertexRounding = listOf(
                        CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                        CornerRounding(1f),
                        CornerRounding(1f),
                        CornerRounding(1f)
                    ),
                    centerX = 0f,
                    centerY = 0f
                )
            }, shapeOutput = {
                shapeDescription(
                    id = "cornerSE",
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon(floatArrayOf(1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f), " +
                        "perVertexRounding = listOf(CornerRounding(" +
                        "${this.roundness.floatValue}f, ${this.smooth.floatValue}f), " +
                        "CornerRounding(1f), CornerRounding(1f),  CornerRounding(1f))," +
                        "centerX = 0f, centerY = 0f)"
                )
            },
            usesSides = false,
            usesInnerRatio = false,
            usesInnerParameters = false
        ),
        ShapeItem(
            "Circle", shapegen = {
                RoundedPolygon.circle(this.sides.floatValue.roundToInt())
            },
            shapeOutput = {
                shapeDescription(
                    id = "Circle",
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon.circle($sides)"
                )
            },
            usesSides = true,
            usesInnerRatio = false,
            usesInnerParameters = false
        ),
        ShapeItem(
            "Rectangle", shapegen = {
                RoundedPolygon.rectangle(
                    width = 4f, height = 2f,
                    rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                )
            },
            shapeOutput = {
                shapeDescription(
                    id = "Rectangle", numVerts = 4,
                    roundness = this.roundness.floatValue,
                    smooth = this.smooth.floatValue,
                    rotation = this.rotation.floatValue,
                    code = "RoundedPolygon.rectangle(width = 4f, height = 2f, " +
                        "rounding = CornerRounding(" +
                        "${this.roundness.floatValue}f, ${this.smooth.floatValue}f))"
                )
            },
            usesSides = false,
            usesInnerRatio = false,
            usesInnerParameters = false
        )

        /*
        TODO: Add quarty. Needs to be able to specify a rounding radius of up to 2f
        ShapeItem("Quarty", { DefaultShapes.quarty(roundness.value, smooth.value) },
        usesSides = false, usesInnerRatio = false),
        */
    )

    fun shapeDescription(
        id: String? = null,
        numVerts: Int? = null,
        sides: Int? = null,
        innerRadius: Float? = null,
        roundness: Float? = null,
        innerRoundness: Float? = null,
        smooth: Float? = null,
        innerSmooth: Float? = null,
        rotation: Float? = null,
        width: Float? = null,
        height: Float? = null,
        pillStarFactor: Float? = null,
        code: String? = null
    ): String {
        var description = "ShapeParameters:\n"
        if (id != null) description += "shapeId = $id, "
        if (numVerts != null) description += "numVertices = $numVerts, "
        if (sides != null) description += "sides = $sides, "
        if (innerRadius != null) description += "innerRadius = $innerRadius, "
        if (roundness != null) description += "roundness = $roundness, "
        if (innerRoundness != null) description += "innerRoundness = $innerRoundness, "
        if (smooth != null) description += "smoothness = $smooth, "
        if (innerSmooth != null) description += "innerSmooth = $innerSmooth, "
        if (rotation != null) description += "rotation = $rotation, "
        if (width != null) description += "width = $width, "
        if (height != null) description += "height = $height, "
        if (pillStarFactor != null) description += "pillStarFactor = $pillStarFactor, "
        if (code != null) {
            description += "\nCode:\n$code"
        }
        return description
    }

    fun selectedShape() = derivedStateOf { shapes[shapeIx] }

    fun genShape(autoSize: Boolean = true) = selectedShape().value.shapegen().let { poly ->
        poly.transformed(Matrix().apply {
            if (autoSize) {
                val bounds = poly.getBounds()
                // Move the center to the origin.
                translate(
                    x = -(bounds.left + bounds.right) / 2,
                    y = -(bounds.top + bounds.bottom) / 2
                )

                // Scale to the [-1, 1] range
                val scale = 2f / max(bounds.width, bounds.height)
                scale(x = scale, y = scale)
            }
            // Apply the needed rotation
            rotateZ(rotation.floatValue)
        })
    }
}

@Composable
fun ShapeEditor(params: ShapeParameters, output: (String) -> Unit, onClose: () -> Unit) {
    val shapeParams = params.selectedShape().value
    var debug by remember { mutableStateOf(false) }
    var stroked by remember { mutableStateOf(false) }
    var autoSize by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Base Shape:", color = Color.White)
            Spacer(Modifier.width(10.dp))
            Button(onClick = { params.shapeIx = (params.shapeIx + 1) % params.shapes.size }) {
                Text(params.selectedShape().value.name)
            }
        }
        if (shapeParams.usesSides) {
            MySlider("Sides", 3f, 20f, 1f, params.sides, shapeParams.usesSides)
        }
        if (shapeParams.usesWH) {
            MySlider("Width", minValue = .1f,
                maxValue = 20f, 1f, params.width, shapeParams.usesWH)
        }
        if (shapeParams.usesWH) {
            MySlider("Height", 1f, maxValue = 20f,
                1f, params.height, shapeParams.usesWH)
        }
        if (shapeParams.usesPillStarFactor) {
            MySlider("Vertex Factor", 0f, maxValue = 1f,
                .05f, params.pillStarFactor, shapeParams.usesPillStarFactor)
        }
        if (shapeParams.usesInnerRatio) {
            MySlider(
                "InnerRadius",
                0.1f,
                0.999f,
                0f,
                params.innerRadius,
                shapeParams.usesInnerRatio
            )
        }
        if (shapeParams.usesRoundness) {
            MySlider("RoundRadius", 0f, 1f, 0f, params.roundness, shapeParams.usesRoundness)
            MySlider("Smoothing", 0f, 1f, 0f, params.smooth)
        }
        if (shapeParams.usesInnerParameters) {
            MySlider(
                "InnerRoundRadius",
                0f,
                1f,
                0f,
                params.innerRoundness,
                shapeParams.usesInnerParameters
            )
            MySlider("InnerSmoothing", 0f, 1f, 0f, params.innerSmooth,
                shapeParams.usesInnerParameters)
        }
        MySlider("Rotation", 0f, 360f, 45f, params.rotation)

        PanZoomRotateBox(
            Modifier
                .clipToBounds()
                .weight(1f)
                .border(1.dp, Color.White)
                .padding(2.dp)
        ) {
            PolygonComposableImpl(params.genShape(autoSize = autoSize).let { poly ->
                if (autoSize) {
                    poly.normalized()
                } else {
                    poly
                }
            }, debug = debug, stroked = stroked)
        }
        Row {
            MyTextButton(
                onClick = onClose,
                text = "Accept"
            )
            // TODO: add cancel!?
            Spacer(Modifier.weight(1f))
            MyTextButton(
                onClick = { debug = !debug },
                text = if (debug) "Beziers" else "Shape"
            )
            Spacer(Modifier.weight(1f))
            MyTextButton(
                onClick = { stroked = !stroked },
                text = if (stroked) "Fill" else "Stroke"
            )
            Spacer(Modifier.weight(1f))
            MyTextButton(
                onClick = { autoSize = !autoSize },
                text = if (autoSize) "AutoSize" else "NoSizing"
            )
            Spacer(Modifier.weight(1f))
            MyTextButton(
                onClick = {
                    val outputString = params.selectedShape().value.shapeOutput() + "\n" +
                        "SVG:\n" + toSvgString(params.selectedShape().value.shapegen())
                    output(outputString)
                },
                text = "Output Details"
            )
        }
    }
}

fun toSvgString(polygon: RoundedPolygon): String {
    var svg = "d=\""
    val cubics = polygon.cubics
    if (cubics.size == 0) return svg.plus("\"")
    svg = svg.plus("M ${cubics[0].anchor0X}, ${cubics[0].anchor0Y}")
    for (c in cubics) {
        svg = svg.plus(
            " C ${c.control0X}, ${c.control0Y}, " +
                "${c.control1X}, ${c.control1Y}, ${c.anchor1X}, ${c.anchor1Y}"
        )
    }
    return svg.plus("\"")
}

@Composable
fun MyTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    // Material defaults are 16 & 8
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
) = Button(onClick = onClick, modifier = modifier, contentPadding = contentPadding) {
    Text(text)
}

@Composable
fun MySlider(
    name: String,
    minValue: Float,
    maxValue: Float,
    step: Float,
    valueHolder: MutableFloatState,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        val text = " %.2f".format(valueHolder.floatValue)
        Text(name + text, color = Color.White)
        Spacer(Modifier.width(10.dp))
        Slider(
            value = valueHolder.floatValue,
            onValueChange = { valueHolder.floatValue = it },
            valueRange = minValue..maxValue,
            steps = if (step > maxValue - minValue)
                ((maxValue - minValue) / step).roundToInt() - 1
            else
                0,
            enabled = enabled
        )
    }
}

private fun squarePoints() = floatArrayOf(1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f)
