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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.graphics.shapes.testcompose

import android.content.Intent
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlin.math.min
import kotlinx.coroutines.launch

@Composable
fun PolygonComposable(
    polygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    stroked: Boolean = false
) = PolygonComposableImpl(polygon, modifier, stroked = stroked)

@Composable
private fun MorphComposable(
    morph: Morph,
    progress: Float,
    modifier: Modifier = Modifier,
    isDebug: Boolean = false,
    stroked: Boolean = false
) = MorphComposableImpl(morph, modifier, isDebug, progress, stroked = stroked)

@Composable
private fun MorphComposableImpl(
    morph: Morph,
    modifier: Modifier = Modifier,
    isDebug: Boolean = false,
    progress: Float,
    stroked: Boolean = false
) {
    Box(
        modifier.fillMaxSize().drawWithContent {
            drawContent()
            val path = morph.toPath(progress)
            fitToViewport(path, morph.getBounds(), size)
            if (isDebug) {
                val scale = min(size.width, size.height)
                drawPath(path, Color.Green, style = Stroke(2f))
                morph.forEachCubic(progress) { cubic ->
                    cubic.transform { x, y -> TransformResult(x * scale, y * scale) }
                    debugDraw(cubic)
                }
            } else {
                val style = if (stroked) Stroke(size.width / 10f) else Fill
                drawPath(path, Color.White, style = style)
            }
        }
    )
}

@Composable
internal fun PolygonComposableImpl(
    polygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    debug: Boolean = false,
    stroked: Boolean = false
) {
    @Suppress("PrimitiveInCollection")
    val sizedShapes = remember(polygon) { mutableMapOf<Size, List<Cubic>>() }
    Box(
        modifier.fillMaxSize().drawWithContent {
            // TODO: Can we use drawWithCache to simplify this?
            drawContent()
            val scale = min(size.width, size.height)
            if (debug) {
                val shape = sizedShapes.getOrPut(size) { polygon.cubics.scaled(scale) }
                // Draw bounding boxes
                val bounds = FloatArray(4)
                polygon.calculateBounds(bounds = bounds)
                drawRect(
                    Color.Green,
                    topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                    size = Size(scale * (bounds[2] - bounds[0]), scale * (bounds[3] - bounds[1])),
                    style = Stroke(2f)
                )
                polygon.calculateBounds(bounds = bounds, false)
                drawRect(
                    Color.Yellow,
                    topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                    size = Size(scale * (bounds[2] - bounds[0]), scale * (bounds[3] - bounds[1])),
                    style = Stroke(2f)
                )
                polygon.calculateMaxBounds(bounds = bounds)
                drawRect(
                    Color.Magenta,
                    topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                    size = Size(scale * (bounds[2] - bounds[0]), scale * (bounds[3] - bounds[1])),
                    style = Stroke(2f)
                )

                // Center of shape
                drawCircle(
                    Color.White,
                    radius = 2f,
                    center = Offset(polygon.centerX * scale, polygon.centerY * scale),
                    style = Stroke(2f)
                )

                shape.forEach { cubic -> debugDraw(cubic) }
            } else {
                val scaledPath = polygon.toPath()
                val matrix = Matrix()
                matrix.scale(scale, scale)
                scaledPath.transform(matrix)
                val style = if (stroked) Stroke(size.width / 10f) else Fill
                drawPath(scaledPath, Color.White, style = style)
            }
        }
    )
}

fun outputShapeInfo(activity: FragmentActivity, info: String) {
    // Output to logcat and also share intent
    println(info)
    val sendIntent: Intent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, info)
            type = "text/plain"
        }
    val shareIntent = Intent.createChooser(sendIntent, null)
    activity.startActivity(shareIntent)
}

@Composable
fun MainScreen(activity: MainActivity) {
    var editing by remember { mutableStateOf<ShapeParameters?>(null) }
    var selectedShape = remember { mutableIntStateOf(0) }
    val shapes = remember { materialShapes() }
    editing?.let {
        ShapeEditor(it, output = { outputString -> outputShapeInfo(activity, outputString) }) {
            editing = null
        }
    } ?: MorphScreen(shapes, selectedShape) { editing = shapes[selectedShape.intValue] }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorphScreen(
    shapeParams: List<ShapeParameters>,
    selectedShape: MutableIntState,
    onEditClicked: () -> Unit
) {
    val shapes = remember {
        shapeParams.map { sp -> sp.genShape().let { poly -> poly.normalized() } }
    }
    var currShape by remember { mutableIntStateOf(selectedShape.intValue) }
    var showControls by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    var debug by remember { mutableStateOf(false) }
    var stroked by remember { mutableStateOf(false) }

    val morphed by remember {
        derivedStateOf {
            // NOTE: We need to access this variable to ensure we recalculate the morph !
            debugLog("Re-computing morph / $debug")
            Morph(shapes[currShape], shapes[selectedShape.intValue])
        }
    }

    val scope = rememberCoroutineScope()
    val clickFn: (Int) -> Unit = remember {
        { shapeIx ->
            scope.launch {
                currShape = selectedShape.intValue
                selectedShape.intValue = shapeIx
                doAnimation(progress)
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 7) {
            shapes.forEachIndexed { shapeIx, shape ->
                val borderAlpha =
                    ((if (shapeIx == selectedShape.intValue) progress.value else 0f) +
                            (if (shapeIx == currShape) 1 - progress.value else 0f))
                        .coerceIn(0f, 1f)
                Box(
                    Modifier.weight(1f)
                        .aspectRatio(1f)
                        .padding(horizontal = 5.dp)
                        .border(3.dp, Color.Red.copy(alpha = borderAlpha))
                ) {
                    // draw shape
                    PolygonComposable(
                        shape,
                        Modifier.clickable { clickFn(shapeIx) },
                        stroked = stroked
                    )
                }
            }
        }
        if (showControls) {

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(
                    onClick = onEditClicked,
                    enabled = !shapeParams[selectedShape.intValue].isCustom
                ) {
                    Text("Edit")
                }
                Button(onClick = { debug = !debug }) { Text(if (debug) "Debug" else "Shape") }
                Button(onClick = { stroked = !stroked }) { Text(if (stroked) "Fill" else "Stroke") }
            }
            Slider(
                value = progress.value.coerceIn(0f, 1f),
                onValueChange = { scope.launch { progress.snapTo(it) } }
            )
        }
        Box {
            MorphComposable(
                morphed,
                progress.value,
                Modifier.fillMaxSize().clickable(
                    indication = null, // Eliminate the ripple effect.
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    scope.launch { doAnimation(progress) }
                },
                debug,
                stroked
            )
            Button(onClick = { showControls = !showControls }, Modifier.align(Alignment.TopEnd)) {
                Text("Controls")
            }
            Text(
                shapeParams[selectedShape.intValue].name,
                Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.5f)),
                color = Color.White
            )
        }
    }
}

private suspend fun doAnimation(progress: Animatable<Float, AnimationVector1D>) {
    progress.snapTo(0f)
    progress.animateTo(1f, animationSpec = spring(0.8f, 360f))
}

internal const val DEBUG = false

internal inline fun debugLog(message: String) {
    if (DEBUG) {
        println(message)
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(parent = null) { MaterialTheme { MainScreen(this) } }
    }
}
