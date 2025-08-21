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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FeatureEditor(params: ShapeParameters) {
    val scheme = MaterialTheme.colorScheme
    val colors =
        FeatureColorScheme(
            edgeColor = Color.Blue,
            concaveColor = Color.Red,
            convexColor = Color.Green,
        )
    val polygon = params.genShape().normalized()

    Column(
        Modifier.padding(12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Tap on dot to change its feature type",
            textAlign = TextAlign.Center,
            color = scheme.primary.copy(0.6f),
        )

        PolygonFeatureView(polygon, params.customFeaturesOverlay, colors)

        ColorLegend(colors)

        DoubleLabelledSlider("Split At", 0.25f, 1f, 0.125f, params.splitProgress)
    }
}

@Composable
fun ParametricEditor(params: ShapeParameters) {
    val shapeParams = params.selectedShape().value

    Column(
        Modifier.padding(12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!params.isCustom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Base Shape:")
                Spacer(Modifier.width(10.dp))
                Button(onClick = { params.shapeIx = (params.shapeIx + 1) % params.shapes.size }) {
                    Text(params.selectedShape().value.name)
                }
            }
        } else {
            Text(
                "This shape can only parametrically edit its rotation.",
                textAlign = TextAlign.Center,
            )
        }

        PanZoomRotateBox(modifier = Modifier.clip(RectangleShape).aspectRatio(1f)) {
            PolygonView(params.genShape().normalized(), modifier = Modifier, center = true)
        }

        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
            if (!params.isCustom) {
                if (shapeParams.usesSides) {
                    DoubleLabelledSlider("Sides", 3f, 20f, 1f, params.sides)
                }
                if (shapeParams.usesWidthAndHeight) {
                    DoubleLabelledSlider("Width", minValue = .1f, maxValue = 20f, 1f, params.width)
                }
                if (shapeParams.usesWidthAndHeight) {
                    DoubleLabelledSlider("Height", 1f, maxValue = 20f, 1f, params.height)
                }
                if (shapeParams.usesPillStarFactor) {
                    DoubleLabelledSlider("Points", 0f, maxValue = 1f, .05f, params.pillStarFactor)
                }
                if (shapeParams.usesInnerRatio) {
                    DoubleLabelledSlider("Inner Radius", .1f, 0.999f, 0f, params.innerRadius)
                }
                if (shapeParams.usesRoundness) {
                    DoubleLabelledSlider("Round", 0f, 1f, 0f, params.roundness)
                    DoubleLabelledSlider("Smooth", 0f, 1f, 0f, params.smooth)
                }
                if (shapeParams.usesInnerParameters) {
                    DoubleLabelledSlider("Inner Round", 0f, 1f, 0f, params.innerRoundness)
                    DoubleLabelledSlider("Inner Smooth", 0f, 1f, 0f, params.innerSmooth)
                }
            }
            DoubleLabelledSlider("Rotation", 0f, 360f, 1f, params.rotation)
        }
    }
}

@Composable
internal fun ColorLegend(scheme: FeatureColorScheme) {
    val colors = listOf("None", "Outward", "Inward")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Corner Indentation")
        Row(
            modifier =
                Modifier.border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f)),
                        shape = RoundedCornerShape(100),
                    )
                    .padding(10.dp)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendChip(colors[0], scheme.edgeColor)
            LegendChip(colors[1], scheme.convexColor)
            LegendChip(colors[2], scheme.concaveColor)
        }
    }
}

@Composable
fun LegendChip(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DoubleLabelledSlider(
    name: String,
    minValue: Float,
    maxValue: Float,
    step: Float,
    valueHolder: MutableFloatState,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().height(50.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val value = " %.2f".format(valueHolder.floatValue)
        Text(name, Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Slider(
            value = valueHolder.floatValue,
            onValueChange = { valueHolder.floatValue = it },
            modifier = Modifier.fillMaxWidth(0.6f),
            valueRange = minValue..maxValue,
            steps =
                if (step == 0f) 0
                else if (step > maxValue - minValue) ((maxValue - minValue) / step).roundToInt() - 1
                else ((maxValue - minValue) / step).roundToInt() - 1,
            enabled = enabled,
        )
        Spacer(Modifier.width(10.dp))
        Text(value, Modifier.width(60.dp))
    }
}
