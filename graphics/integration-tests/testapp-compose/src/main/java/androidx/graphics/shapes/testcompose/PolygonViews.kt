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

package androidx.graphics.shapes.testcompose

import androidx.collection.MutableFloatObjectMap
import androidx.collection.mutableFloatObjectMapOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShapesGallery(
    shapes: List<RoundedPolygon>,
    selectedMainShape: Int,
    selectedOtherShape: Int,
    modifier: Modifier = Modifier,
    onNewClicked: (Int) -> Unit,
) {
    FlowRow(modifier, maxItemsInEachRow = 7) {
        shapes.forEachIndexed { newShapeIndex, shape ->
            val alpha =
                when (newShapeIndex) {
                    selectedMainShape -> 1f
                    selectedOtherShape -> 0.65f
                    else -> 0.3f
                }
            PolygonView(
                shape,
                Modifier.padding(2.dp).height(60.dp).weight(1f).clickable {
                    onNewClicked(newShapeIndex)
                },
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha),
                center = true,
            )
        }
    }
}

@Composable
internal fun PolygonView(
    polygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    debug: Boolean = false,
    stroked: Boolean = false,
    center: Boolean = false,
) {
    Size
    val sizedShapes: MutableFloatObjectMap<List<Cubic>> =
        remember(polygon) { mutableFloatObjectMapOf() }
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier.then(if (center) Modifier.aspectRatio(1f) else Modifier)
                .fillMaxSize()
                .align(Alignment.Center)
                .drawWithContent {
                    drawContent()
                    val scale = size.minDimension
                    if (debug) {
                        val shape = sizedShapes.getOrPut(scale) { polygon.cubics.scaled(scale) }
                        // Draw bounding boxes
                        val bounds = FloatArray(4)
                        polygon.calculateBounds(bounds = bounds)
                        drawRect(
                            scheme.secondary,
                            topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                            size =
                                Size(
                                    scale * (bounds[2] - bounds[0]),
                                    scale * (bounds[3] - bounds[1]),
                                ),
                            style = Stroke(2f),
                        )
                        polygon.calculateBounds(bounds = bounds, false)
                        drawRect(
                            scheme.tertiary,
                            topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                            size =
                                Size(
                                    scale * (bounds[2] - bounds[0]),
                                    scale * (bounds[3] - bounds[1]),
                                ),
                            style = Stroke(2f),
                        )
                        polygon.calculateMaxBounds(bounds = bounds)
                        drawRect(
                            scheme.inversePrimary,
                            topLeft = Offset(scale * bounds[0], scale * bounds[1]),
                            size =
                                Size(
                                    scale * (bounds[2] - bounds[0]),
                                    scale * (bounds[3] - bounds[1]),
                                ),
                            style = Stroke(2f),
                        )

                        // Center of shape
                        drawCircle(
                            fillColor,
                            radius = 2f,
                            center = Offset(polygon.centerX * scale, polygon.centerY * scale),
                            style = Stroke(2f),
                        )

                        shape.forEach { cubic -> debugDrawCubic(cubic, scheme) }
                    } else {
                        val scaledPath = polygon.toPath()
                        val matrix = Matrix()
                        matrix.scale(scale, scale)
                        scaledPath.transform(matrix)
                        val style = if (stroked) Stroke(size.width / 10f) else Fill
                        drawPath(scaledPath, fillColor, style = style)
                    }
                }
        )
    }
}

@Composable
internal fun PolygonFeatureView(
    polygon: RoundedPolygon,
    customFeaturesOverlayState: MutableState<List<FeatureType>>,
    featureColorScheme: FeatureColorScheme,
    modifier: Modifier = Modifier,
) {
    @Suppress("PrimitiveInCollection") val scheme = MaterialTheme.colorScheme
    val model = remember { PanZoomRotateBoxState() }
    var scale by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(scale) {
        // Zoom in to see the shape
        scale.let { if (it != 0f) model.zoom.value = it }
    }

    Box(modifier.fillMaxWidth().clip(RectangleShape)) {
        Box(Modifier.aspectRatio(1f).fillMaxSize().align(Alignment.Center)) {
            PanZoomRotateBox(
                state = model,
                allowRotation = false,
                showInteraction = false,
                modifier = Modifier.padding(15.dp),
            ) {
                Box(
                    Modifier.fillMaxSize().drawWithContent {
                        drawContent()
                        scale = size.minDimension
                        val paths = polygon.features.map { it.toPath() }

                        // Draw outline. Color features according to their type
                        val style = Stroke(5f / scale)
                        paths.forEachIndexed { index, path ->
                            drawPath(
                                path,
                                featureToColor(polygon.features[index], featureColorScheme),
                                style = style,
                            )
                        }

                        polygon.features.forEach {
                            // Separate features by Spacer points.
                            drawCircle(
                                scheme.background,
                                radius = 15f / scale,
                                center = it.cubics.first().anchor0(),
                            )
                        }
                    }
                )
            }

            // Interactive points to show features and make their type changeable.
            polygon.features.forEachIndexed { index, feature ->
                FeatureRepresentativePoint(
                    modifier = Modifier.padding(15.dp),
                    feature,
                    featureColorScheme,
                    scheme.background,
                    model,
                ) {
                    customFeaturesOverlayState.value =
                        customFeaturesOverlayState.value.mapIndexed { copyIndex, type ->
                            if (copyIndex == index) toggleFeatureType(feature.toFeatureType())
                            else type
                        }
                }
            }

            // We are adding the button manually as the reset behavior is different in our custom
            // model
            if (model.offset.value != Offset.Zero || model.zoom.value != scale) {
                Button(
                    onClick = {
                        model.offset.value = Offset.Zero
                        model.zoom.value = scale
                    }
                ) {
                    Text("Reset View", textAlign = TextAlign.Center)
                }
            } else {
                Icon(Icons.Default.Pinch, "Zoom in", Modifier.alpha(0.2f))
            }
        }
    }
}

internal data class FeatureColorScheme(
    val edgeColor: Color,
    val convexColor: Color,
    val concaveColor: Color,
)
