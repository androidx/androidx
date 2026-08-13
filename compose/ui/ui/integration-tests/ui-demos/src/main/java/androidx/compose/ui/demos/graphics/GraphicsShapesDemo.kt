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

package androidx.compose.ui.demos.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.PolygonShapeGeometry
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.foundation.shape.PolygonShapeGeometry.CornerRounding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.scaledToFit
import androidx.compose.foundation.shape.transformed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BackgroundDark = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryAccent = Color(0xFF8B5CF6)
private val SecondaryAccent = Color(0xFFEC4899)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

enum class DemoTab {
    ShapeEditor,
    MorphSandbox,
    GraphicsLayerMorph,
    LoadingIndicator,
}

@Composable
fun GraphicsShapesDemo() {
    var currentTab by remember { mutableStateOf(DemoTab.ShapeEditor) }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "graphics-shape Sandbox",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Explore responsive PolygonShape and Morphing in Compose",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DemoTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    Box(
                        modifier =
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimaryAccent else Color.Transparent)
                                .clickable { currentTab = tab }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text =
                                when (tab) {
                                    DemoTab.ShapeEditor -> "Editor"
                                    DemoTab.MorphSandbox -> "Morph"
                                    DemoTab.GraphicsLayerMorph -> "Layer"
                                    DemoTab.LoadingIndicator -> "Official Shape APIs"
                                },
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (currentTab) {
                    DemoTab.ShapeEditor -> ShapeEditorContent()
                    DemoTab.MorphSandbox -> MorphSandboxContent()
                    DemoTab.GraphicsLayerMorph -> GraphicsLayerMorphContent()
                    DemoTab.LoadingIndicator -> FoundationLoadingIndicatorDemo()
                }
            }
        }
    }
}

@Composable
private fun ShapeEditorContent() {
    var isStar by remember { mutableStateOf(false) }
    var vertices by remember { mutableFloatStateOf(5f) }
    var innerRadiusRatio by remember { mutableFloatStateOf(0.5f) }
    var roundingPercent by remember { mutableFloatStateOf(20f) }
    var smoothing by remember { mutableFloatStateOf(0f) }
    var showDebug by remember { mutableStateOf(false) }

    // Shape values are value-equal (or memoized on their lambda captures), and remembered
    // across parameter changes.
    val shape =
        remember(isStar, vertices, innerRadiusRatio, roundingPercent, smoothing) {
            if (isStar) {
                PolygonShape.star(
                    numPoints = vertices.toInt(),
                    innerRadiusRatio = innerRadiusRatio,
                    outerRounding =
                        CornerRounding(percent = (roundingPercent.toInt()), smoothing = smoothing),
                )
            } else {
                PolygonShape {
                    polygon(
                        vertices.toInt(),
                        rounding =
                            CornerRounding(percent = roundingPercent.toInt(), smoothing = smoothing),
                    )
                }
            }
        }

    Card(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(260.dp)
                        .background(BackgroundDark, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                    val outline = shape.createOutline(size, layoutDirection, this)

                    if (!showDebug) {
                        drawOutline(
                            outline = outline,
                            brush =
                                Brush.linearGradient(
                                    colors = listOf(PrimaryAccent, SecondaryAccent)
                                ),
                        )
                    } else {
                        drawOutline(outline = outline, color = PrimaryAccent.copy(alpha = 0.1f))
                        drawOutline(
                            outline = outline,
                            color = PrimaryAccent,
                            style = Stroke(width = 4f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isStar,
                    onCheckedChange = { isStar = it },
                    colors =
                        androidx.compose.material.CheckboxDefaults.colors(
                            checkedColor = PrimaryAccent
                        ),
                )
                Text("Star Shape variant", color = TextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(24.dp))
                Checkbox(
                    checked = showDebug,
                    onCheckedChange = { showDebug = it },
                    colors =
                        androidx.compose.material.CheckboxDefaults.colors(
                            checkedColor = PrimaryAccent
                        ),
                )
                Text("Show Wireframe", color = TextPrimary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            SliderWithLabel(
                label = "Vertices: ${vertices.toInt()}",
                value = vertices,
                valueRange = 3f..12f,
                onValueChange = { vertices = it },
            )

            if (isStar) {
                SliderWithLabel(
                    label = "Inner Radius Ratio: ${String.format("%.2f", innerRadiusRatio)}",
                    value = innerRadiusRatio,
                    valueRange = 0.1f..0.9f,
                    onValueChange = { innerRadiusRatio = it },
                )
            }

            SliderWithLabel(
                label = "Rounding: ${roundingPercent.toInt()}%",
                value = roundingPercent,
                valueRange = 0f..100f,
                onValueChange = { roundingPercent = it },
            )

            SliderWithLabel(
                label = "Smoothing (continuity): ${String.format("%.2f", smoothing)}",
                value = smoothing,
                valueRange = 0f..1.0f,
                onValueChange = { smoothing = it },
            )
        }
    }
}

private val SandboxShapeNames =
    listOf("Triangle", "Square", "Circle", "Star (5-Sided)", "Star (8-Sided)", "Hexagon")

private fun sandboxShape(type: Int): PolygonShape =
    when (type) {
        0 -> PolygonShape { polygon(3, rounding = CornerRounding(percent = 10)) }
        1 ->
            PolygonShape.rectangle(
                topStartRounding = CornerRounding(percent = 20),
                topEndRounding = CornerRounding(percent = 20),
                bottomEndRounding = CornerRounding(percent = 20),
                bottomStartRounding = CornerRounding(percent = 20),
            )
        2 -> PolygonShape.circle()
        3 ->
            PolygonShape.star(
                numPoints = 5,
                innerRadiusRatio = 0.4f,
                outerRounding = CornerRounding(percent = 50),
            )
        4 ->
            PolygonShape.star(
                numPoints = 8,
                innerRadiusRatio = 0.6f,
                outerRounding = CornerRounding(percent = 10),
            )
        else -> PolygonShape { polygon(6, rounding = CornerRounding(percent = 15)) }
    }

@Composable
private fun MorphSandboxContent() {
    var startShapeType by remember { mutableIntStateOf(0) }
    var endShapeType by remember { mutableIntStateOf(3) }
    var progress by remember { mutableFloatStateOf(0.5f) }
    var isLooping by remember { mutableStateOf(true) }
    var showDebug by remember { mutableStateOf(false) }

    val startShape = sandboxShape(startShapeType)
    val endShape = sandboxShape(endShapeType)

    val infiniteTransition = rememberInfiniteTransition(label = "MorphTransition")
    val animatedProgress =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "Progress",
        )

    // The lambda reads the state at outline resolution time, so progress animation does not
    // rebuild the morph.
    val morphingShape =
        remember(startShape, endShape) {
            MorphPolygonShape(startShape, endShape) {
                if (isLooping) animatedProgress.value else progress
            }
        }

    Card(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(260.dp)
                        .background(BackgroundDark, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                    val outline = morphingShape.createOutline(size, layoutDirection, this)

                    if (!showDebug) {
                        drawOutline(
                            outline = outline,
                            brush =
                                Brush.linearGradient(
                                    colors = listOf(SecondaryAccent, PrimaryAccent)
                                ),
                        )
                    } else {
                        drawOutline(outline = outline, color = SecondaryAccent.copy(alpha = 0.08f))
                        drawOutline(
                            outline = outline,
                            color = SecondaryAccent,
                            style = Stroke(width = 4f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start Shape", color = TextSecondary, fontSize = 11.sp)
                    ScrollableTabSelector(
                        selected = startShapeType,
                        items = SandboxShapeNames,
                        onSelected = { startShapeType = it },
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("End Shape", color = TextSecondary, fontSize = 11.sp)
                    ScrollableTabSelector(
                        selected = endShapeType,
                        items = SandboxShapeNames,
                        onSelected = { endShapeType = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isLooping,
                    onCheckedChange = { isLooping = it },
                    colors =
                        androidx.compose.material.CheckboxDefaults.colors(
                            checkedColor = PrimaryAccent
                        ),
                )
                Text("Auto-Loop Animation", color = TextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(24.dp))
                Checkbox(
                    checked = showDebug,
                    onCheckedChange = { showDebug = it },
                    colors =
                        androidx.compose.material.CheckboxDefaults.colors(
                            checkedColor = PrimaryAccent
                        ),
                )
                Text("Show Wireframe", color = TextPrimary, fontSize = 13.sp)
            }

            if (!isLooping) {
                Spacer(modifier = Modifier.height(12.dp))
                SliderWithLabel(
                    label = "Morph Progress: ${String.format("%.2f", progress)}",
                    value = progress,
                    valueRange = 0f..1f,
                    onValueChange = { progress = it },
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loop Progress: ${String.format("%.2f", animatedProgress)}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun GraphicsLayerMorphContent() {

    val startShape = remember {
        PolygonShape { polygon(3, rounding = CornerRounding(radius = 12.dp, smoothing = 0.5f)) }
    }
    val endShape = remember {
        PolygonShape.star(
            numPoints = 8,
            innerRadiusRatio = 0.5f,
            outerRounding = CornerRounding(radius = 8.dp, smoothing = 0.5f),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LayerTransition")
    val animatedProgress =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "Progress",
        )

    val morphingShape =
        remember(startShape, endShape) {
            MorphPolygonShape(startShape, endShape) { animatedProgress.value }
        }

    Card(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "graphicsLayer Clipping Morph",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(240.dp)
                        .background(BackgroundDark, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.size(180.dp)
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryAccent, SecondaryAccent)
                                    ),
                                shape = morphingShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Clipped Layout",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextPrimary, fontSize = 12.sp)
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
            colors =
                SliderDefaults.colors(
                    thumbColor = PrimaryAccent,
                    activeTrackColor = PrimaryAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                ),
        )
    }
}

@Composable
private fun ScrollableTabSelector(selected: Int, items: List<String>, onSelected: (Int) -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(BackgroundDark, RoundedCornerShape(8.dp))
                .padding(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selected == index
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent
                        )
                        .clickable { onSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private data class PointNRound(val o: Offset, val r: CornerRounding = CornerRounding.Unrounded)

private val cornerRound15 = CornerRounding(percent = 15)
private val cornerRound50 = CornerRounding(percent = 50)

private val rotateNeg45 = Matrix().apply { rotateZ(-45f) }
private val rotateNeg90 = Matrix().apply { rotateZ(-90f) }

private fun Float.toRadians() = this / 360f * 2 * PI.toFloat()

private fun Offset.angleDegrees() = atan2(y, x) * 180f / PI.toFloat()

private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero) =
    (angle.toRadians()).let { a ->
        val off = this - center
        Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
    }

@Suppress("PrimitiveInCollection")
private fun doRepeat(
    points: List<PointNRound>,
    reps: Int,
    center: Offset,
    mirroring: Boolean,
): List<PointNRound> =
    if (mirroring) {
        buildList {
            val MathAngles = points.fastMap { (it.o - center).angleDegrees() }
            val distances = points.fastMap { (it.o - center).getDistance() }
            val actualReps = reps * 2
            val sectionAngle = 360f / actualReps
            repeat(actualReps) {
                points.indices.forEach { index ->
                    val i = if (it % 2 == 0) index else points.lastIndex - index
                    if (i > 0 || it % 2 == 0) {
                        val a =
                            (sectionAngle * it +
                                    if (it % 2 == 0) MathAngles[i]
                                    else sectionAngle - MathAngles[i] + 2 * MathAngles[0])
                                .toRadians()
                        val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                        add(PointNRound(finalPoint, points[i].r))
                    }
                }
            }
        }
    } else {
        points.size.let { np ->
            (0 until np * reps).map {
                val point = points[it % np].o.rotateDegrees((it / np) * 360f / reps, center)
                PointNRound(point, points[it % np].r)
            }
        }
    }

private fun customPolygonShape(
    pnr: List<PointNRound>,
    reps: Int,
    center: Offset = Offset(0.5f, 0.5f),
    mirroring: Boolean = false,
    startRotation: Matrix? = null,
): PolygonShape {
    val points = doRepeat(pnr, reps, center, mirroring)
    val shape =
        PolygonShape(
            PolygonShapeGeometry(
                vertices = points.fastMap { it.o },
                perVertexRounding = points.fastMap { it.r },
                center = center,
            )
        )
    // A rotation applied after the internal fit can overflow the bounds, so re-fit.
    return if (startRotation != null) {
        shape.transformed(startRotation, contentScale = ContentScale.Fit)
    } else {
        shape
    }
}

object M3Shapes {

    val Oval =
        PolygonShape.circle(numVertices = 10)
            .transformed(
                matrix =
                    Matrix().apply {
                        rotateZ(-45f)
                        scale(1f, 0.64f)
                    },
                contentScale = ContentScale.Fit,
            )

    val Pill =
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
                PointNRound(Offset(1.001f, 0.428f)),
                PointNRound(Offset(1.000f, 0.609f), CornerRounding(1f)),
            ),
            reps = 2,
            mirroring = true,
        )

    val Pentagon =
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
                PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
                PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
            ),
            reps = 1,
            mirroring = true,
        )

    val Sunny =
        PolygonShape.star(numPoints = 8, innerRadiusRatio = 0.8f, outerRounding = cornerRound15)
            .scaledToFit()

    val Cookie4Sided =
        customPolygonShape(
            listOf(
                PointNRound(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
                PointNRound(Offset(0.500f, 0.918f), CornerRounding(0.233f)),
            ),
            reps = 4,
        )

    val Cookie9Sided =
        PolygonShape.star(numPoints = 9, innerRadiusRatio = 0.8f, outerRounding = cornerRound50)
            .transformed(rotateNeg90, contentScale = ContentScale.Fit)

    val SoftBurst =
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
                PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
            ),
            reps = 10,
        )
}

private fun indeterminateIndicatorShapes(): List<PolygonShape> =
    listOf(
        M3Shapes.SoftBurst,
        M3Shapes.Cookie9Sided,
        M3Shapes.Pentagon,
        M3Shapes.Pill,
        M3Shapes.Sunny,
        M3Shapes.Cookie4Sided,
        M3Shapes.Oval,
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun indicatorScaleFactor(): Float {
    val polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
    val bounds = FloatArray(4)
    val maxBounds = FloatArray(4)
    var scaleFactor = 1f
    polygons.forEach { polygon ->
        polygon.calculateBounds(bounds)
        polygon.calculateMaxBounds(maxBounds)
        val scaleX = (bounds[2] - bounds[0]) / (maxBounds[2] - maxBounds[0])
        val scaleY = (bounds[3] - bounds[1]) / (maxBounds[3] - maxBounds[1])
        scaleFactor = min(scaleFactor, max(scaleX, scaleY))
    }
    val activeScale =
        LoadingIndicatorDefaults.IndicatorSize.value /
            min(
                LoadingIndicatorDefaults.ContainerWidth.value,
                LoadingIndicatorDefaults.ContainerHeight.value,
            )
    return scaleFactor * activeScale
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FoundationLoadingIndicatorDemo() {
    val shapes = remember { indeterminateIndicatorShapes() }
    val count = shapes.size
    val totalScale = remember { indicatorScaleFactor() }

    val morphProgress = remember { Animatable(0f) }
    var morphRotationTarget by remember { mutableFloatStateOf(90f) }
    val globalRotation = remember { Animatable(0f) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        launch {
            val morphSpec =
                spring<Float>(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
            while (true) {
                val deferred = async {
                    val result =
                        morphProgress.animateTo(targetValue = 1f, animationSpec = morphSpec)
                    if (result.endReason == AnimationEndReason.Finished) {
                        currentIndex = (currentIndex + 1) % count
                        morphProgress.snapTo(0f)
                        morphRotationTarget = (morphRotationTarget + 90f) % 360f
                    }
                }
                delay(650)
                deferred.await()
            }
        }
        launch {
            globalRotation.animateTo(
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        tween(durationMillis = 4666, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
            )
        }
    }

    val startShape = shapes[currentIndex]
    val endShape = shapes[(currentIndex + 1) % count]
    val morphingShape =
        remember(startShape, endShape) {
            MorphPolygonShape(startShape, endShape) { morphProgress.value.coerceIn(0f, 1f) }
        }

    Card(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LoadingIndicator: Foundation vs. Material 3",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(220.dp)
                        .background(BackgroundDark, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.size(96.dp)
                                .graphicsLayer {
                                    rotationZ =
                                        morphProgress.value * 90f +
                                            morphRotationTarget +
                                            globalRotation.value
                                    scaleX = totalScale
                                    scaleY = totalScale
                                }
                                .clip(morphingShape)
                                .background(PrimaryAccent)
                    )
                    Text(text = "Foundation", color = TextSecondary, fontSize = 11.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LoadingIndicator(modifier = Modifier.size(96.dp), color = PrimaryAccent)
                    Text(text = "Material 3", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
