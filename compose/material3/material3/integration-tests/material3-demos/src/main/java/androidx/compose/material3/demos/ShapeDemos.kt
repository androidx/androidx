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

package androidx.compose.material3.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarms
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ShapeDemo() {
    val shapes = MaterialTheme.shapes
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Button(onClick = {}, shape = RectangleShape) { Text("None") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.extraSmall) { Text("Extra  Small") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.small) { Text("Small") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.medium) { Text("Medium") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.large) { Text("Large") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.largeIncreased) { Text("Large Increased") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.extraLarge) { Text("Extra Large") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.extraLargeIncreased) { Text("Extra Large Increased") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = shapes.extraExtraLarge) { Text("Extra Extra Large") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, shape = CircleShape) { Text("Full") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialShapeDemo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = MaterialShapes.Clover4Leaf.toShape()

        // A Material Button with a shape that is provided from the MaterialShapes
        Button(
            onClick = { /* on-click*/ },
            modifier = Modifier.requiredSize(48.dp),
            shape = shape,
            border = BorderStroke(width = 2.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(
                Icons.Outlined.AccessibilityNew,
                modifier = Modifier.requiredSize(24.dp),
                contentDescription = "Localized description",
            )
        }

        // A basic Box with a shape that is provided from the MaterialShapes
        Box(
            modifier =
                Modifier.requiredSize(64.dp)
                    .border(BorderStroke(width = 2.dp, Color.Red), shape)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AccessibilityNew,
                contentDescription = "Localized description",
                modifier = Modifier.requiredSize(36.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Path drawing on Canvas with a shape that is provided from the MaterialShapes
        val shapePath = MaterialShapes.Clover4Leaf.toPath()
        val workPath = Path()
        val color = MaterialTheme.colorScheme.outline
        val borderWidth = with(LocalDensity.current) { 2.dp.toPx() }
        Canvas(Modifier.requiredSize(64.dp)) {
            // The path is normalized, so we need to scale it to the size of the canvas.
            workPath.rewind()
            workPath.addPath(shapePath)
            workPath.transform(matrix = Matrix().apply { scale(size.width, size.height) })
            drawPath(workPath, color = color, style = Stroke(width = borderWidth))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialShapeMorphDemo() {
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedProgress =
        animateFloatAsState(
            targetValue = if (isPressed) 1f else 0f,
            label = "progress",
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        )
    val morphShape = remember {
        object : Shape {
            private val path = Path()
            private val matrix = Matrix()

            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                matrix.reset()
                matrix.scale(size.width, size.height)
                morph.toPath(animatedProgress.value, path)
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }
    Button(
        onClick = { /* on-click*/ },
        modifier = Modifier.requiredSize(48.dp),
        shape = morphShape,
        interactionSource = interactionSource
    ) {
        Icon(
            Icons.Outlined.AccessAlarms,
            modifier = Modifier.requiredSize(24.dp),
            contentDescription = "Localized description",
        )
    }
}
