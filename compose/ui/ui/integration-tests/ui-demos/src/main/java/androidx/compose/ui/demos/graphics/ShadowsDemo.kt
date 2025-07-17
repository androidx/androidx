/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShadowsDemo() {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = "Inner Shadow", fontSize = 16.sp, color = Color.DarkGray)
        InnerShadowDemo()

        Text(text = "Outer Shadow", fontSize = 16.sp, color = Color.DarkGray)
        OuterShadowDemo()

        Text(text = "Neumorphism", fontSize = 16.sp, color = Color.DarkGray)
        NeumorphismDemo()

        Text(text = "Custom Shape Shadow ", fontSize = 16.sp, color = Color.DarkGray)
        CustomShapeShadowDemo()
    }
}

@Composable
private fun InnerShadowDemo() {
    Box(
        Modifier.height(70.dp)
            .width(180.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .innerShadow(
                shape = RoundedCornerShape(12.dp),
                shadow =
                    Shadow(
                        radius = 12.dp,
                        spread = 0.dp,
                        offset = DpOffset(x = 10.dp, y = 10.dp),
                        color = Color.Magenta,
                    ),
            )
    ) {}
}

@Composable
private fun OuterShadowDemo() {
    Box(
        Modifier.height(70.dp)
            .width(180.dp)
            .dropShadow(
                shape = RoundedCornerShape(12.dp),
                shadow =
                    Shadow(
                        radius = 15.dp,
                        spread = 2.dp,
                        offset = DpOffset(x = 8.dp, y = 8.dp),
                        color = Color.Gray.copy(alpha = 0.7f),
                    ),
            )
            .background(Color.White, RoundedCornerShape(12.dp))
    ) {}
}

@Composable
private fun NeumorphismDemo() {
    val buttonShape = RoundedCornerShape(24.dp)
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier.height(100.dp)
                .width(200.dp)
                .background(Color(0xFFE8E8E8), buttonShape)
                .dropShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 12.dp,
                            spread = 0.dp,
                            offset = DpOffset(x = (-8).dp, y = (-8).dp),
                            color = Color.White.copy(0.7f),
                        ),
                )
                .dropShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 12.dp,
                            spread = 0.dp,
                            offset = DpOffset(x = 8.dp, y = 8.dp),
                            color = Color.Black.copy(alpha = 0.2f),
                        ),
                )
        ) {}

        Box(Modifier.height(200.dp).width(200.dp).background(Color(0xFFDFECF4))) {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .align(alignment = Alignment.Center)
                        .dropShadow(
                            shape = buttonShape,
                            shadow =
                                Shadow(
                                    radius = 12.dp,
                                    spread = 0.dp,
                                    offset = DpOffset(x = (-8).dp, y = (-8).dp),
                                    color = Color.White.copy(alpha = 0.8f),
                                ),
                        )
                        .dropShadow(
                            shape = buttonShape,
                            shadow =
                                Shadow(
                                    radius = 12.dp,
                                    spread = 0.dp,
                                    offset = DpOffset(x = 8.dp, y = 8.dp),
                                    color = Color(0x4D6C8DC2),
                                ),
                        )
                        .background(Color(0xFFDFECF4), buttonShape)
            )
        }
    }
}

@Composable
private fun CustomShapeShadowDemo() {
    val starShape =
        object : Shape {

            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                return Outline.Generic(
                    Path().apply {
                        val width = size.width
                        val height = size.height
                        val centerX = width / 2f
                        val centerY = height / 2f
                        val outerRadius = width / 2f * 0.8f
                        val innerRadius = outerRadius / 2.5f

                        val angleIncrement = (Math.PI * 2 / 5).toFloat()
                        moveTo(
                            centerX + outerRadius * kotlin.math.cos(-Math.PI.toFloat() / 2),
                            centerY + outerRadius * kotlin.math.sin(-Math.PI.toFloat() / 2),
                        )

                        for (i in 0 until 5) {
                            val outerX =
                                centerX +
                                    outerRadius *
                                        kotlin.math.cos(-Math.PI.toFloat() / 2 + angleIncrement * i)
                            val outerY =
                                centerY +
                                    outerRadius *
                                        kotlin.math.sin(-Math.PI.toFloat() / 2 + angleIncrement * i)
                            lineTo(outerX, outerY)

                            val innerX =
                                centerX +
                                    innerRadius *
                                        kotlin.math.cos(
                                            -Math.PI.toFloat() / 2 +
                                                angleIncrement * i +
                                                angleIncrement / 2
                                        )
                            val innerY =
                                centerY +
                                    innerRadius *
                                        kotlin.math.sin(
                                            -Math.PI.toFloat() / 2 +
                                                angleIncrement * i +
                                                angleIncrement / 2
                                        )
                            lineTo(innerX, innerY)
                        }
                        close()
                    }
                )
            }
        }

    Box(
        Modifier.size(150.dp)
            .dropShadow(
                shape = starShape,
                shadow =
                    Shadow(
                        radius = 20.dp,
                        spread = 0.dp,
                        offset = DpOffset(x = 10.dp, y = 10.dp),
                        color = Color.Blue.copy(alpha = 0.5f),
                    ),
            )
            .background(Color.Yellow, starShape)
            .innerShadow(
                shape = starShape,
                shadow = Shadow(radius = 10.dp, spread = 5.dp, color = Color.Magenta),
            )
    ) {}
}
