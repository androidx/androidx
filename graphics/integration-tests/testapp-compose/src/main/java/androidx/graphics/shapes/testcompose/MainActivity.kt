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
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.launch

@Composable
fun PolygonComposable(polygon: RoundedPolygon, modifier: Modifier = Modifier) =
    PolygonComposableImpl(polygon, modifier)

@Composable
private fun MorphComposable(
    sizedMorph: SizedMorph,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    isDebug: Boolean = false
) =
    MorphComposableImpl(sizedMorph, modifier, isDebug) {
        sizedMorph.morph.progress = progress()
    }

internal fun calculateMatrix(bounds: RectF, width: Float, height: Float): Matrix {
    val originalWidth = bounds.right - bounds.left
    val originalHeight = bounds.bottom - bounds.top
    val scale = min(width / originalWidth, height / originalHeight)
    val newLeft = bounds.left - (width / scale - originalWidth) / 2
    val newTop = bounds.top - (height / scale - originalHeight) / 2
    val matrix = Matrix()
    matrix.setTranslate(-newLeft, -newTop)
    matrix.postScale(scale, scale)
    return matrix
}

internal fun PointF.transform(
    matrix: Matrix,
    dst: PointF = PointF(),
    floatArray: FloatArray = FloatArray(2)
): PointF {
    floatArray[0] = x
    floatArray[1] = y
    matrix.mapPoints(floatArray)
    dst.x = floatArray[0]
    dst.y = floatArray[1]
    return dst
}

private val TheBounds = RectF(0f, 0f, 1f, 1f)

private class SizedMorph(val morph: Morph) {
    var width = 1f
    var height = 1f

    fun resizeMaybe(newWidth: Float, newHeight: Float) {
        if (abs(width - newWidth) > 1e-4 || abs(height - newHeight) > 1e-4) {
            val matrix = calculateMatrix(RectF(0f, 0f, width, height), newWidth, newHeight)
            morph.transform(matrix)
            width = newWidth
            height = newHeight
        }
    }
}

@Composable
private fun MorphComposableImpl(
    sizedMorph: SizedMorph,
    modifier: Modifier = Modifier,
    isDebug: Boolean = false,
    prep: ContentDrawScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                prep()
                drawContent()
                sizedMorph.resizeMaybe(size.width, size.height)
                if (isDebug) {
                    debugDraw(sizedMorph.morph)
                } else {
                    drawPath(sizedMorph.morph.asPath().asComposePath(), Color.White)
                }
            })
}

@Composable
internal fun PolygonComposableImpl(
    shape: RoundedPolygon,
    modifier: Modifier = Modifier,
    debug: Boolean = false
) {
    val sizedPolygonCache = remember(shape) {
        mutableMapOf<Size, RoundedPolygon>()
    }
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val sizedPolygon = sizedPolygonCache.getOrPut(size) {
                    val matrix = calculateMatrix(TheBounds, size.width, size.height)
                    RoundedPolygon(shape).apply { transform(matrix) }
                }
                if (debug) {
                    debugDraw(sizedPolygon.toCubicShape())
                } else {
                    drawPath(sizedPolygon.toPath().asComposePath(), Color.White)
                }
            })
}

@Composable
fun MainScreen() {
    var editing by remember { mutableStateOf<ShapeParameters?>(null) }
    var selectedShape = remember { mutableStateOf(0) }
    val shapes = remember {
        listOf(
            // LINE 1
            // Circle
            ShapeParameters(
                sides = 8,
                roundness = 1f,
                shapeId = ShapeParameters.ShapeId.Circle
            ),
            //
            ShapeParameters(
                sides = 12,
                innerRadius = .928f,
                roundness = .1f,
                shapeId = ShapeParameters.ShapeId.Star
            ),
            // Clover
            ShapeParameters(
                sides = 4,
                innerRadius = .352f,
                roundness = .32f,
                rotation = 45f,
                shapeId = ShapeParameters.ShapeId.Star
            ),
            // Alice
            ShapeParameters(
                innerRadius = 0.1f,
                roundness = 0.22f,
                shapeId = ShapeParameters.ShapeId.Triangle
            ),
            // Wiggle Star
            ShapeParameters(
                sides = 8,
                innerRadius = .784f,
                roundness = .16f,
                shapeId = ShapeParameters.ShapeId.Star
            ),

            // LINE 2
            // Wovel
            ShapeParameters(
                sides = 15,
                innerRadius = .892f,
                roundness = 1f,
                shapeId = ShapeParameters.ShapeId.Star
            ),
            // BlobR
            ShapeParameters(
                innerRadius = .19f,
                roundness = 0.86f,
                rotation = -45f,
                shapeId = ShapeParameters.ShapeId.Blob
            ),
            // BlobL
            ShapeParameters(
                innerRadius = .19f,
                roundness = 0.86f,
                rotation = 45f,
                shapeId = ShapeParameters.ShapeId.Blob
            ),
            // Scallop
            ShapeParameters(
                sides = 12,
                innerRadius = .928f,
                roundness = .928f,
                shapeId = ShapeParameters.ShapeId.Star
            ),
            // More
            ShapeParameters(
                sides = 3,
                roundness = .2f,
                rotation = 30f,
                shapeId = ShapeParameters.ShapeId.Polygon
            ),

            // LINE 3
            // CornerSE
            ShapeParameters(roundness = .4f, shapeId = ShapeParameters.ShapeId.CornerSE),

            // Non - material shapes:
            // Rectangle
            ShapeParameters(
                sides = 4,
                shapeId = ShapeParameters.ShapeId.Rectangle
            ),

            // Pentagon
            ShapeParameters(
                sides = 5,
                rotation = -360f / 20f,
                shapeId = ShapeParameters.ShapeId.Polygon
            ),

            // 5-Sided Star
            ShapeParameters(
                sides = 5,
                rotation = -360f / 20,
                innerRadius = .3f,
                shapeId = ShapeParameters.ShapeId.Star
            ),

            // Round Rect
            ShapeParameters(
                sides = 4,
                roundness = .5f,
                smooth = 1f,
                shapeId = ShapeParameters.ShapeId.Rectangle
            ),
        )
    }
    editing?.let {
        ShapeEditor(it) { editing = null }
    } ?: MorphScreen(shapes, selectedShape) { editing = shapes[selectedShape.value] }
}

@Composable
fun MorphScreen(
    shapeParams: List<ShapeParameters>,
    selectedShape: MutableState<Int>,
    onEditClicked: () -> Unit
) {
    val shapes = remember {
        shapeParams.map { sp ->
            sp.genShape().also { poly ->
                val matrix = calculateMatrix(poly.bounds, 1f, 1f)
                poly.transform(matrix)
            }
        }
    }

    var currShape by remember { mutableStateOf(selectedShape.value) }
    val progress = remember { Animatable(0f) }

    var debug by remember { mutableStateOf(false) }

    val morphed by remember {
        derivedStateOf {
            // NOTE: We need to access this variable to ensure we recalculate the morph !
            debugLog("Re-computing morph / $debug")
            SizedMorph(
                Morph(
                    shapes[currShape],
                    shapes[selectedShape.value]
                )
            )
        }
    }

    val scope = rememberCoroutineScope()
    val clickFn: (Int) -> Unit = remember {
            { shapeIx ->
            scope.launch {
                currShape = selectedShape.value
                selectedShape.value = shapeIx
                doAnimation(progress)
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        repeat(3) { rowIx ->
            Row(Modifier.fillMaxWidth()) {
                repeat(5) { columnIx ->
                    val shapeIx = rowIx * 5 + columnIx
                    val borderAlpha = (
                        (if (shapeIx == selectedShape.value) progress.value else 0f) +
                        (if (shapeIx == currShape) 1 - progress.value else 0f)
                    ).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 5.dp)
                            .border(
                                3.dp,
                                Color.Red.copy(alpha = borderAlpha)
                            )
                    ) {
                        // draw shape
                        val shape = shapes[shapeIx]
                        PolygonComposable(shape, Modifier.clickable { clickFn(shapeIx) })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { debug = !debug }) {
                Text(if (debug) "Debug" else "Shape")
            }
            Button(onClick = onEditClicked) {
                Text("Edit")
            }
        }
        Slider(value = progress.value.coerceIn(0f, 1f), onValueChange = {
            scope.launch { progress.snapTo(it) }
        })
        MorphComposable(morphed, { progress.value },
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null, // Eliminate the ripple effect.
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    scope.launch { doAnimation(progress) }
                }, debug)
    }
}

private suspend fun doAnimation(progress: Animatable<Float, AnimationVector1D>) {
    progress.snapTo(0f)
    progress.animateTo(
        1f,
        animationSpec =
             spring(0.6f, 50f)
    )
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(parent = null) {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}
