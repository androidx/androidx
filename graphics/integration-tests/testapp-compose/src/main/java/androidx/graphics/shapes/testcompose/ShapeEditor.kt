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

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
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
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.plus
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Star
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private val LOG_TAG = "ShapeEditor"
private val DEBUG = false

internal fun debugLog(message: String) {
    if (DEBUG) Log.d(LOG_TAG, message)
}

data class ShapeItem(
    val name: String,
    val shapegen: () -> RoundedPolygon,
    val debugDump: () -> Unit,
    val usesSides: Boolean = true,
    val usesInnerRatio: Boolean = true,
    val usesRoundness: Boolean = true,
    val usesInnerParameters: Boolean = true
)

private val PointZero = PointF(0f, 0f)

class ShapeParameters(
    sides: Int = 5,
    innerRadius: Float = 0.5f,
    roundness: Float = 0f,
    smooth: Float = 0f,
    innerRoundness: Float = roundness,
    innerSmooth: Float = smooth,
    rotation: Float = 0f,
    shapeId: ShapeId = ShapeId.Polygon
) {
    internal val sides = mutableStateOf(sides.toFloat())
    internal val innerRadius = mutableStateOf(innerRadius)
    internal val roundness = mutableStateOf(roundness)
    internal val smooth = mutableStateOf(smooth)
    internal val innerRoundness = mutableStateOf(innerRoundness)
    internal val innerSmooth = mutableStateOf(innerSmooth)
    internal val rotation = mutableStateOf(rotation)

    internal var shapeIx by mutableStateOf(shapeId.ordinal)

    fun copy() = ShapeParameters(
        this.sides.value.roundToInt(),
        this.innerRadius.value,
        this.roundness.value,
        this.smooth.value,
        this.innerRoundness.value,
        this.innerSmooth.value,
        this.rotation.value,
        ShapeId.values()[this.shapeIx]
    )

    enum class ShapeId {
        Star, Polygon, Triangle, Blob, CornerSE
    }

    private fun radialToCartesian(
        radius: Float,
        angleRadians: Float,
        center: PointF = PointZero
    ) = directionVectorPointF(angleRadians) * radius + center

    private fun rotationAsString() =
        if (this.rotation.value != 0f)
            "rotation = ${this.rotation.value}f, "
        else
            ""

    // Primitive shapes we can draw (so far)
    internal val shapes = listOf(
        ShapeItem("Star", shapegen = {
                Star(
                    numVerticesPerRadius = this.sides.value.roundToInt(),
                    innerRadius = this.innerRadius.value,
                    rounding = CornerRounding(this.roundness.value, this.smooth.value),
                    innerRounding = CornerRounding(
                        this.innerRoundness.value,
                        this.innerSmooth.value
                    )
                )
            },
            debugDump = {
                debugLog(
                    "ShapeParameters(sides = ${this.sides.value.roundToInt()}, " +
                        "innerRadius = ${this.innerRadius.value}f, " +
                        "roundness = ${this.roundness.value}f, " +
                        "smooth = ${this.smooth.value}f, " +
                        "innerRoundness = ${this.innerRoundness.value}f, " +
                        "innerSmooth = ${this.innerSmooth.value}f, " +
                        rotationAsString() +
                        "shapeId = ShapeParameters.ShapeId.Star)"
                )
            }
        ),
        ShapeItem("Polygon", shapegen = {
                RoundedPolygon(
                    numVertices = this.sides.value.roundToInt(),
                    rounding = CornerRounding(this.roundness.value, this.smooth.value),
                )
            },
            debugDump = {
                debugLog(
                    "ShapeParameters(sides = ${this.sides.value.roundToInt()}, " +
                        "roundness = ${this.roundness.value}f, " +
                        "smooth = ${this.smooth.value}f, " +
                        rotationAsString() +
                        ")"
                )
            }, usesInnerRatio = false, usesInnerParameters = false
        ),
        ShapeItem(
            "Triangle", shapegen = {
                val points = listOf(
                    radialToCartesian(1f, 270f.toRadians()),
                    radialToCartesian(1f, 30f.toRadians()),
                    radialToCartesian(this.innerRadius.value, 90f.toRadians()),
                    radialToCartesian(1f, 150f.toRadians()),
                )
                RoundedPolygon(
                    points,
                    CornerRounding(this.roundness.value, this.smooth.value),
                    center = PointZero
                )
            },
            debugDump = {
                debugLog(
                    "ShapeParameters(innerRadius = ${this.innerRadius.value}f, " +
                        "smooth = ${this.smooth.value}f, " +
                        rotationAsString() +
                        "shapeId = ShapeParameters.ShapeId.Triangle)"
                )
            },
            usesSides = false, usesInnerParameters = false
        ),
        ShapeItem(
            "Blob", shapegen = {
                val sx = this.innerRadius.value.coerceAtLeast(0.1f)
                val sy = this.roundness.value.coerceAtLeast(0.1f)
                RoundedPolygon(
                    listOf(
                        PointF(-sx, -sy),
                        PointF(sx, -sy),
                        PointF(sx, sy),
                        PointF(-sx, sy),
                    ),
                    rounding = CornerRounding(this.roundness.value, this.smooth.value),
                    center = PointZero
                )
            },
            debugDump = {
                debugLog(
                    "ShapeParameters(roundness = ${this.roundness.value}f, " +
                        "smooth = ${this.smooth.value}f, " +
                        rotationAsString() +
                        "shapeId = ShapeParameters.ShapeId.Blob)"
                )
            },
            usesSides = false, usesInnerParameters = false
        ),
        ShapeItem(
            "CornerSE", shapegen = {
                RoundedPolygon(
                    SquarePoints(),
                    perVertexRounding = listOf(
                        CornerRounding(this.roundness.value, this.smooth.value),
                        CornerRounding(1f),
                        CornerRounding(1f),
                        CornerRounding(1f)
                    ),
                    center = PointZero
                )
            },
            debugDump = {
                debugLog(
                    "ShapeParameters(roundness = ${this.roundness.value}f, " +
                        "smooth = ${this.smooth.value}f, " +
                        rotationAsString() +
                        "shapeId = ShapeParameters.ShapeId.CornerSE)"
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

    fun selectedShape() = derivedStateOf { shapes[shapeIx] }

    fun genShape(autoSize: Boolean = true) = selectedShape().value.shapegen().apply {
        transform(Matrix().apply {
            if (autoSize) {
                // Move the center to the origin.
                center
                postTranslate(-(bounds.left + bounds.right) / 2, -(bounds.top + bounds.bottom) / 2)

                // Scale to the [-1, 1] range
                val scale = 2f / max(bounds.width(), bounds.height())
                postScale(scale, scale)
            }
            // Apply the needed rotation
            postRotate(rotation.value)
        })
    }
}

@Composable
fun ShapeEditor(params: ShapeParameters, onClose: () -> Unit) {
    val shapeParams = params.selectedShape().value
    var debug by remember { mutableStateOf(false) }
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
        MySlider("Sides", 3f, 20f, 1f, params.sides, shapeParams.usesSides)
        MySlider(
            "InnerRadius",
            0.1f,
            0.999f,
            0f,
            params.innerRadius,
            shapeParams.usesInnerRatio
        )
        MySlider("RoundRadius", 0f, 1f, 0f, params.roundness, shapeParams.usesRoundness)
        MySlider("Smoothing", 0f, 1f, 0f, params.smooth)
        MySlider(
            "InnerRoundRadius",
            0f,
            1f,
            0f,
            params.innerRoundness,
            shapeParams.usesInnerParameters
        )
        MySlider("InnerSmoothing", 0f, 1f, 0f, params.innerSmooth, shapeParams.usesInnerParameters)
        MySlider("Rotation", 0f, 360f, 45f, params.rotation)

        PanZoomRotateBox(
            Modifier
                .clipToBounds()
                .weight(1f)
                .border(1.dp, Color.White)
                .padding(2.dp)
        ) {
            PolygonComposableImpl(params.genShape(autoSize = autoSize).also { poly ->
                if (autoSize) {
                    val matrix = calculateMatrix(poly.bounds, 1f, 1f)
                    poly.transform(matrix)
                }
            }, debug = debug)
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
                onClick = { autoSize = !autoSize },
                text = if (autoSize) "AutoSize" else "NoSizing"
            )
            Spacer(Modifier.weight(1f))
            MyTextButton(
                onClick = { params.selectedShape().value.debugDump() },
                text = "Dump to Logcat"
            )
        }
    }
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
    valueHolder: MutableState<Float>,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = Color.White)
        Spacer(Modifier.width(10.dp))
        Slider(
            value = valueHolder.value,
            onValueChange = { valueHolder.value = it },
            valueRange = minValue..maxValue,
            steps = if (step > maxValue - minValue)
                ((maxValue - minValue) / step).roundToInt() - 1
            else
                0,
            enabled = enabled
        )
    }
}

// TODO: remove this when it is integrated into Ktx
operator fun PointF.times(factor: Float): PointF {
    return PointF(this.x * factor, this.y * factor)
}

// Create a new list every time, because mutability is complicated.
private fun SquarePoints() = listOf(
    PointF(1f, 1f),
    PointF(-1f, 1f),
    PointF(-1f, -1f),
    PointF(1f, -1f)
)

internal fun directionVectorPointF(angleRadians: Float) =
    PointF(cos(angleRadians), sin(angleRadians))
