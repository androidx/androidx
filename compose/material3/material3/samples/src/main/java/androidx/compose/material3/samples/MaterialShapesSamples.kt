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

package androidx.compose.material3.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

// TODO: Consider adding this as an official @sampled code, perhaps with less shapes.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun AllShapes() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ProvideTextStyle(MaterialTheme.typography.labelSmall) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allMaterialShapes().forEach { (name, polygon) ->
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name)
                            Spacer(
                                modifier =
                                    Modifier.requiredSize(56.dp)
                                        .clip(polygon.toShape())
                                        .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun allMaterialShapes(): List<Pair<String, RoundedPolygon>> {
    return listOf(
        "Circle" to MaterialShapes.Circle,
        "Square" to MaterialShapes.Square,
        "Slanted" to MaterialShapes.Slanted,
        "Arch" to MaterialShapes.Arch,
        "Fan" to MaterialShapes.Fan,
        "Arrow" to MaterialShapes.Arrow,
        "SemiCircle" to MaterialShapes.SemiCircle,
        "Oval" to MaterialShapes.Oval,
        "Pill" to MaterialShapes.Pill,
        "Triangle" to MaterialShapes.Triangle,
        "Diamond" to MaterialShapes.Diamond,
        "ClamShell" to MaterialShapes.ClamShell,
        "Pentagon" to MaterialShapes.Pentagon,
        "Gem" to MaterialShapes.Gem,
        "Sunny" to MaterialShapes.Sunny,
        "VerySunny" to MaterialShapes.VerySunny,
        "Cookie4Sided" to MaterialShapes.Cookie4Sided,
        "Cookie6Sided" to MaterialShapes.Cookie6Sided,
        "Cookie7Sided" to MaterialShapes.Cookie7Sided,
        "Cookie9Sided" to MaterialShapes.Cookie9Sided,
        "Cookie12Sided" to MaterialShapes.Cookie12Sided,
        "Ghostish" to MaterialShapes.Ghostish,
        "Clover4Leaf" to MaterialShapes.Clover4Leaf,
        "Clover8Leaf" to MaterialShapes.Clover8Leaf,
        "Burst" to MaterialShapes.Burst,
        "SoftBurst" to MaterialShapes.SoftBurst,
        "Boom" to MaterialShapes.Boom,
        "SoftBoom" to MaterialShapes.SoftBoom,
        "Flower" to MaterialShapes.Flower,
        "Puffy" to MaterialShapes.Puffy,
        "PuffyDiamond" to MaterialShapes.PuffyDiamond,
        "PixelCircle" to MaterialShapes.PixelCircle,
        "PixelTriangle" to MaterialShapes.PixelTriangle,
        "Bun" to MaterialShapes.Bun,
        "Heart" to MaterialShapes.Heart,
    )
}
