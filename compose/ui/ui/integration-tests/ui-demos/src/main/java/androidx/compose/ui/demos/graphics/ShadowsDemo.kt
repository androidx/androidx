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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Neumphormism {
    val DarkShadow =
        Shadow(
            radius = 12.dp,
            spread = 0.dp,
            offset = DpOffset(x = 4.dp, y = 4.dp),
            color = Color(0x4D6C8DC2),
        )

    val LightShadow =
        Shadow(
            radius = 12.dp,
            spread = 0.dp,
            offset = DpOffset(x = (-4).dp, y = (-4).dp),
            color = Color.White.copy(alpha = 0.8f),
        )
}

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
        Text(text = "Demos", fontSize = 16.sp, color = Color.DarkGray)
        Demos()
        Text(text = "Neumorphism", fontSize = 16.sp, color = Color.DarkGray)
        NeumorphismDemo()
        Text(text = "3D Buttons", fontSize = 16.sp, color = Color.DarkGray)
        KeyboardButtons()
    }
}

@Composable
private fun Demos() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "InnerShadows", fontSize = 12.sp, color = Color.DarkGray)
        InnerShadows()
        Text(text = "DropShadows", fontSize = 12.sp, color = Color.DarkGray)
        DropShadows()
    }
}

@Composable
private fun InnerShadows() {
    Row(
        Modifier.height(150.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier =
                Modifier.size(height = 100.dp, width = 70.dp)
                    .innerShadow(
                        shape = RectangleShape,
                        shadow = Shadow(radius = 6.dp, spread = 0.dp, color = Color.Red),
                    )
        ) {}
        InnerShadowWithOffset()
        InnerShadowWithGradient()
        InnerShadowWithGradientAndOffset()
        InnerShadowWithArbitraryShape()

        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun InnerShadowWithOffset() {
    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .innerShadow(
                    shape = RectangleShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            color = Color.Red,
                        ),
                )
    ) {}
}

@Composable
private fun InnerShadowWithGradient() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )

    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .innerShadow(
                    shape = RectangleShape,
                    shadow = Shadow(radius = 6.dp, spread = 0.dp, brush = sweepGradientBrush),
                )
    ) {}
}

@Composable
private fun InnerShadowWithGradientAndOffset() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )

    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .innerShadow(
                    shape = RectangleShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            brush = sweepGradientBrush,
                        ),
                )
    ) {}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InnerShadowWithArbitraryShape() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )
    val cookieShape = MaterialShapes.Cookie7Sided.toShape()

    Box(
        modifier =
            Modifier.size(100.dp)
                .innerShadow(
                    shape = cookieShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            brush = sweepGradientBrush,
                        ),
                )
    ) {}
}

@Composable
private fun DropShadows() {
    Row(
        Modifier.height(150.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier =
                Modifier.size(height = 100.dp, width = 70.dp)
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(radius = 10.dp, spread = 0.dp, color = Color.Red),
                    )
                    .background(Color.White, RectangleShape)
        ) {}
        DropShadowWithOffset()
        DropShadowWithGradient()
        DropShadowWithGradientAndOffset()
        DropShadowWithArbitraryShape()
        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun DropShadowWithOffset() {
    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .dropShadow(
                    shape = RectangleShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            color = Color.Red,
                        ),
                )
                .background(Color.White, RectangleShape)
    ) {}
}

@Composable
private fun DropShadowWithGradient() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )

    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .dropShadow(
                    shape = RectangleShape,
                    shadow = Shadow(radius = 6.dp, spread = 0.dp, brush = sweepGradientBrush),
                )
                .background(Color.White, RectangleShape)
    ) {}
}

@Composable
private fun DropShadowWithGradientAndOffset() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )

    Box(
        modifier =
            Modifier.size(height = 100.dp, width = 70.dp)
                .dropShadow(
                    shape = RectangleShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            brush = sweepGradientBrush,
                        ),
                )
                .background(Color.White, RectangleShape)
    ) {}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DropShadowWithArbitraryShape() {
    val sweepGradientBrush =
        Brush.sweepGradient(
            colors =
                listOf(
                    Color(0xFFFF8C00),
                    Color(0xFFFF2D55),
                    Color(0xFFD400FF),
                    Color(0xFF4A00E0),
                    Color(0xFF4A00E0),
                    Color(0xFFD400FF),
                    Color(0xFFFF2D55),
                    Color(0xFFFF8C00),
                )
        )
    val cookieShape = MaterialShapes.Cookie7Sided.toShape()

    Box(
        modifier =
            Modifier.size(100.dp)
                .dropShadow(
                    shape = cookieShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(10.dp, 10.dp),
                            brush = sweepGradientBrush,
                        ),
                )
                .background(Color.White, cookieShape)
    ) {}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NeumorphismDemo() {
    val buttonShape = RoundedCornerShape(24.dp)
    val triangleShape = MaterialShapes.Triangle.toShape()
    val cookieShape = MaterialShapes.Cookie7Sided.toShape()
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        NeumorphButtons(
            buttonShape = buttonShape,
            triangleShape = triangleShape,
            cookieShape = cookieShape,
        )

        NeumorphButtonPressed(
            buttonShape = buttonShape,
            triangleShape = triangleShape,
            cookieShape = cookieShape,
        )

        NeumorphButtonLined(
            buttonShape = buttonShape,
            triangleShape = triangleShape,
            cookieShape = cookieShape,
        )
    }
}

@Composable
private fun NeumorphButtonLined(buttonShape: Shape, triangleShape: Shape, cookieShape: Shape) {
    Row(
        Modifier.height(100.dp)
            .background(Color(0xFFDFECF4))
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = buttonShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = buttonShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), buttonShape)
                    .innerShadow(shape = buttonShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = buttonShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = triangleShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = triangleShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), triangleShape)
                    .innerShadow(shape = triangleShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = triangleShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = CircleShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = CircleShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), CircleShape)
                    .innerShadow(shape = CircleShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = CircleShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = cookieShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = cookieShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), cookieShape)
                    .innerShadow(shape = cookieShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = cookieShape, shadow = Neumphormism.DarkShadow)
        )

        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun NeumorphButtonPressed(buttonShape: Shape, triangleShape: Shape, cookieShape: Shape) {
    Row(
        Modifier.height(100.dp)
            .background(Color(0xFFDFECF4))
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier =
                Modifier.size(60.dp)
                    .background(Color(0xFFDFECF4), triangleShape)
                    .innerShadow(shape = buttonShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = buttonShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .background(Color(0xFFDFECF4), triangleShape)
                    .innerShadow(shape = triangleShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = triangleShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .background(Color(0xFFDFECF4), triangleShape)
                    .innerShadow(shape = CircleShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = CircleShape, shadow = Neumphormism.DarkShadow)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .background(Color(0xFFDFECF4), triangleShape)
                    .innerShadow(shape = cookieShape, shadow = Neumphormism.LightShadow)
                    .innerShadow(shape = cookieShape, shadow = Neumphormism.DarkShadow)
        )

        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun NeumorphButtons(buttonShape: Shape, triangleShape: Shape, cookieShape: Shape) {
    Row(
        Modifier.height(100.dp)
            .background(Color(0xFFDFECF4))
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = buttonShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = buttonShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), buttonShape)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = triangleShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = triangleShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), triangleShape)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = CircleShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = CircleShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), CircleShape)
        )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .dropShadow(shape = cookieShape, shadow = Neumphormism.LightShadow)
                    .dropShadow(shape = cookieShape, shadow = Neumphormism.DarkShadow)
                    .background(Color(0xFFDFECF4), cookieShape)
        )

        Spacer(modifier = Modifier.width(24.dp))
    }
}

object KeyboardButtonColors {
    val Background = Color(0xFFFA9DAC)
    val Surface = Color(0xFFFF8288)
    val LightShadow = Color(0xFFD56C71)
    val DarkShadow = Color(0xFF6C6C6C)
    val TextColor = Color(0xFFFDEBC1)
}

@Composable
private fun KeyboardButtons(modifier: Modifier = Modifier) {
    val buttonShape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier.fillMaxWidth().background(KeyboardButtonColors.Background).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.dropShadow(
                        shape = buttonShape,
                        shadow =
                            Shadow(
                                radius = 0.dp,
                                spread = 0.5.dp,
                                offset = DpOffset(0.6.dp, 0.6.dp),
                                color = KeyboardButtonColors.LightShadow,
                                alpha = 0.4f,
                            ),
                    )
                    .dropShadow(
                        shape = buttonShape,
                        shadow =
                            Shadow(
                                radius = 20.dp,
                                spread = 0.dp,
                                offset = DpOffset(12.dp, 12.dp),
                                color = KeyboardButtonColors.DarkShadow,
                                alpha = 0.5f,
                            ),
                    )
                    .clip(buttonShape)
                    .background(KeyboardButtonColors.Surface)
                    .innerShadow(
                        shape = buttonShape,
                        shadow =
                            Shadow(
                                radius = 0.2.dp,
                                spread = 0.05.dp,
                                offset = DpOffset(0.dp, 0.5.dp),
                                color = Color.White.copy(alpha = 0.4f),
                            ),
                    )
                    .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeumorphicKey {
                    Text(
                        text = "ESC",
                        color = KeyboardButtonColors.TextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                NeumorphicKey {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up Arrow",
                        tint = KeyboardButtonColors.TextColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                NeumorphicKey {
                    Text(
                        text = "↵",
                        color = KeyboardButtonColors.TextColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NeumorphicKey(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val buttonShape = RoundedCornerShape(4.dp)

    Box(
        modifier =
            modifier
                .size(48.dp)
                .dropShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 8.dp,
                            spread = 0.dp,
                            offset = DpOffset(3.dp, 3.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                        ),
                )
                .dropShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 0.dp,
                            spread = 1.dp,
                            offset = DpOffset(0.6.dp, 0.6.dp),
                            color = KeyboardButtonColors.DarkShadow,
                            alpha = 0.4f,
                        ),
                )
                .clip(buttonShape)
                .background(KeyboardButtonColors.Surface)
                .innerShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 6.dp,
                            spread = 0.dp,
                            offset = DpOffset(4.dp, 4.dp),
                            color = Color.Black.copy(alpha = 0.05f),
                        ),
                )
                .innerShadow(
                    shape = buttonShape,
                    shadow =
                        Shadow(
                            radius = 0.2.dp,
                            spread = 0.05.dp,
                            offset = DpOffset(0.dp, 0.5.dp),
                            color = Color.White.copy(alpha = 0.4f),
                        ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
