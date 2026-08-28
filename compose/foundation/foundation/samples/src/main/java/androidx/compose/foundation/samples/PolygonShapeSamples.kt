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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.PolygonShapeGeometry
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.toPolygonShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun PolygonShapeSample() {
    // A regular hexagon sized to the container's smaller dimension with an exact 16.dp corner
    // rounding resolved against the current density.
    val hexagon = remember {
        PolygonShape {
            polygon(
                numVertices = 6,
                radius = size.minDimension / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
                rounding = CornerRounding(radius = 16.dp),
            )
        }
    }
    Box(Modifier.size(96.dp).clip(hexagon).background(Color(0xFF3F51B5)))
}

@Sampled
@Composable
fun DirectionalPolygonShapeSample() {
    // A pointed tag whose arrow tip adapts to the layout size and flips horizontally in RTL.
    val tag = remember {
        PolygonShape {
            val isLtr = layoutDirection == LayoutDirection.Ltr
            val tipX = if (isLtr) size.width else 0f
            val baseX = if (isLtr) 0f else size.width
            val indentX = if (isLtr) size.width * 0.2f else size.width * 0.8f
            polygon(
                vertices =
                    listOf(
                        Offset(baseX, 0f),
                        Offset(tipX, size.height / 2f),
                        Offset(baseX, size.height),
                        Offset(indentX, size.height / 2f),
                    ),
                rounding = CornerRounding(radius = 6.dp),
            )
        }
    }
    Box(Modifier.size(width = 120.dp, height = 64.dp).clip(tag).background(Color(0xFF4CAF50)))
}

@Sampled
@Composable
fun CustomPolygonShapeSample() {
    // A diamond built from pixel coordinates derived from the resolved layout size, with an
    // exact 8dp corner rounding.
    val diamond = remember {
        PolygonShape {
            polygon(
                vertices =
                    listOf(
                        Offset(size.width / 2f, 0f),
                        Offset(size.width, size.height / 2f),
                        Offset(size.width / 2f, size.height),
                        Offset(0f, size.height / 2f),
                    ),
                rounding = CornerRounding(radius = 8.dp),
            )
        }
    }
    Box(Modifier.size(96.dp).clip(diamond).background(Color(0xFFFF5722)))
}

@Sampled
@Composable
fun PolygonShapeWithRoundingPercentSample() {
    // A pentagon whose corners round to 20% of its generating radius, so the rounding scales
    // proportionally with the shape at any size.
    val pentagon = remember {
        PolygonShape.regularPolygon(numVertices = 5, rounding = CornerRounding(percent = 20))
    }
    Box(Modifier.size(96.dp).clip(pentagon).background(Color(0xFF009688)))
}

@Sampled
@Composable
fun UnitSpacePolygonShapeSample() {
    // A kite authored in the unit square [0..1] with per-vertex rounding in the same coordinate
    // space. The shape scales and centers into any container preserving its aspect ratio.
    val kite = remember {
        PolygonShape(
            PolygonShapeGeometry(
                vertices =
                    listOf(Offset(0.5f, 0f), Offset(1f, 0.4f), Offset(0.5f, 1f), Offset(0f, 0.4f)),
                perVertexRounding =
                    listOf(
                        CornerRounding(0.1f),
                        CornerRounding(0.15f),
                        CornerRounding(0.05f),
                        CornerRounding(0.15f),
                    ),
            )
        )
    }

    Box(Modifier.size(width = 120.dp, height = 84.dp).clip(kite).background(Color(0xFF673AB7)))
}

@Sampled
@Composable
fun StarPolygonShapeSample() {
    // An eight-point star with smoothly rounded outer points.
    val star =
        PolygonShape.star(
            numPoints = 8,
            innerRadiusRatio = 0.6f,
            outerRounding = CornerRounding(percent = 15, smoothing = 0.5f),
        )
    Box(Modifier.size(96.dp).clip(star).background(Color(0xFFFFC107)))
}

@Sampled
@Composable
fun PillStarPolygonShapeSample() {
    // An elongated star outline distributed along a pill container, creating a decorative badge.
    val pillStar = remember {
        PolygonShape.pillStar(
            numPoints = 8,
            innerRadiusRatio = 0.6f,
            outerRounding = CornerRounding(percent = 20, smoothing = 0.5f),
        )
    }
    Box(Modifier.size(width = 140.dp, height = 64.dp).clip(pillStar).background(Color(0xFFE91E63)))
}

@Sampled
@Composable
fun MorphPolygonShapeSample() {
    // A badge that morphs between a rounded hexagon and a star as it is toggled. No remember
    // keys are needed: the endpoints are constant and the current progress is read through the
    // lambda each time the outline is resolved.
    var selected by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (selected) 1f else 0f)
    val shape = remember {
        MorphPolygonShape(
            start =
                PolygonShape { polygon(numVertices = 6, rounding = CornerRounding(percent = 20)) },
            end =
                PolygonShape.star(
                    numPoints = 6,
                    innerRadiusRatio = 0.6f,
                    outerRounding = CornerRounding(percent = 20),
                ),
            progress = { progress },
        )
    }
    Box(
        Modifier.size(96.dp).clip(shape).background(Color(0xFF4CAF50)).clickable {
            selected = !selected
        }
    )
}

@Sampled
@Composable
fun RoundedCornerShapeToPolygonShapeSample() {
    // Converting a corner-based shape to a PolygonShape lets it morph with any other polygon
    // shape; here a rounded rectangle relaxes into a circle.
    var expanded by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (expanded) 1f else 0f)
    val shape = remember {
        MorphPolygonShape(
            start = RoundedCornerShape(12.dp).toPolygonShape(),
            end = PolygonShape.circle(),
            progress = { progress },
        )
    }
    Box(
        Modifier.size(96.dp).clip(shape).background(Color(0xFF03A9F4)).clickable {
            expanded = !expanded
        }
    )
}
