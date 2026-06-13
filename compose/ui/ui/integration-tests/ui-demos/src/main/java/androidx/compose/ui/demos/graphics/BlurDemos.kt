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

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BlurDemos() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        BlurDemosContent()
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Progressive blur requires Android 13 (API 33) or above.",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun BlurDemosContent() {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xFF0F172A))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            text = "Progressive Blur",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )

        InteractiveLinearBlurDemo()

        TactileRadialBlurDemo()

        GlassmorphicCardDemo()

        CustomShaderMaskDemo()
    }
}

/** Checkerboard backdrop making transparency visible. */
private fun Modifier.checkerboard(): Modifier = drawBehind {
    val cell = 8.dp.toPx()
    var iy = 0
    var y = 0f
    while (y < size.height) {
        var ix = 0
        var x = 0f
        while (x < size.width) {
            drawRect(
                if ((ix + iy) % 2 == 0) Color(0xFF64748B) else Color(0xFF334155),
                topLeft = Offset(x, y),
                size = Size(cell, cell),
            )
            x += cell
            ix++
        }
        y += cell
        iy++
    }
}

@Composable
private fun InteractiveLinearBlurDemo() {
    val sliderState = rememberSliderState(value = 24f, trackRange = 0f..50f)
    var isHorizontal by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "1. Linear Gradient Blur",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    "Smoothly transitions from sharp to blurred. Adjust orientation and max radius.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Box(
                modifier =
                    Modifier.width(260.dp).height(260.dp).blur {
                        // isHorizontal and sliderState.value are read inside the block: changing
                        // them re-applies layer properties without recomposition or re-recording.
                        radius =
                            if (isHorizontal) {
                                BlurRadiusSpec.horizontalGradient(
                                    startRadius = 0.dp,
                                    endRadius = sliderState.value.dp,
                                )
                            } else {
                                BlurRadiusSpec.verticalGradient(
                                    startRadius = 0.dp,
                                    endRadius = sliderState.value.dp,
                                )
                            }
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    },
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE53935), Color(0xFFFDD835))
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Organic Peppers",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Content text overlay that gets blurred with the image
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors =
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(16.dp)
                ) {
                    Text(
                        "Organic Red Peppers",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Farm fresh ingredients from local suppliers",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { isHorizontal = !isHorizontal },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                ) {
                    Text(if (isHorizontal) "Switch to Vertical" else "Switch to Horizontal")
                }

                Text(
                    text = "Max Blur: ${sliderState.value.toInt()} dp",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Slider(state = sliderState, modifier = Modifier.fillMaxWidth())
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun TactileRadialBlurDemo() {
    var touchOffset by remember { mutableStateOf(Offset.Unspecified) }

    // Fallback orbital animation when not touched
    val transition = rememberInfiniteTransition()
    val angle by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
        )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "2. Tactile Radial Focus Blur",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Drag your finger across the image to move the clear focus lens.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(260.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { touchOffset = it },
                                onDrag = { change, _ -> touchOffset = change.position },
                                onDragEnd = { touchOffset = Offset.Unspecified },
                                onDragCancel = { touchOffset = Offset.Unspecified },
                            )
                        }
                        .blur {
                            val center =
                                if (touchOffset.isSpecified) {
                                    // Pointer positions arrive in pixels; the gradient geometry
                                    // is Dp.
                                    DpOffset(touchOffset.x.toDp(), touchOffset.y.toDp())
                                } else {
                                    val orbitRadius = minOf(size.width, size.height) * 0.25f
                                    DpOffset(
                                        x = size.width / 2 + orbitRadius * cos(angle),
                                        y = size.height / 2 + orbitRadius * sin(angle),
                                    )
                                }
                            radius =
                                BlurRadiusSpec.radialGradient(
                                    center = center,
                                    fallOffRadius = 300.dp,
                                    startRadius = 0.dp,
                                    endRadius = 24.dp,
                                )
                        }
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2E3192), Color(0xFF1BFFFF))
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Mountain Vista",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // High detail HUD overlay to showcase sharpness contrast
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        text = "FOCUS DETECTED\nAUTO-APERTURE F/1.8",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.border(
                                    1.dp,
                                    Color.Cyan.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp),
                                )
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun GlassmorphicCardDemo() {
    val transition = rememberInfiniteTransition()
    val animatedOffset by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "3. Glassmorphic Multi-Stop Blur",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    "Frosted glass card with non-linear multi-stop progressive blur on shifting background.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Animated background
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Brush.sweepGradient(
                                colors =
                                    listOf(
                                        Color(0xFFEC4899), // Pink
                                        Color(0xFF8B5CF6), // Purple
                                        Color(0xFF3B82F6), // Blue
                                        Color(0xFF06B6D4), // Cyan
                                        Color(0xFFEC4899), // Pink
                                    ),
                                center = Offset(animatedOffset * 2f, 140f),
                            )
                        ),
                contentAlignment = Alignment.Center,
            ) {
                // Shorter grid lines to show behind glass card
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(5) {
                        Spacer(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }

                // Frosted progressive blur card
                Box(
                    modifier =
                        Modifier.size(width = 280.dp, height = 180.dp)
                            .blur {
                                radius =
                                    BlurRadiusSpec.linearGradient(
                                        start = DpOffset(0.dp, 0.dp),
                                        end = DpOffset(0.dp, size.height),
                                        stops =
                                            listOf(
                                                BlurStop(0.0f, 0.dp),
                                                BlurStop(0.4f, 8.dp),
                                                BlurStop(1.0f, 28.dp),
                                            ),
                                    )
                                edgeTreatment = BlurredEdgeTreatment(RoundedCornerShape(24.dp))
                            }
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(
                                width = 1.dp,
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.05f),
                                            )
                                    ),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PREMIUM CARD",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            text = "Progressive frosted glass creates authentic depth.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun CustomShaderMaskDemo() {
    val transition = rememberInfiniteTransition()
    val animatedTime by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
        )

    val customShader = remember {
        RuntimeShader(
            """
            uniform float2 size;
            uniform float time;

            half4 main(float2 coords) {
                // Diagonal wave mask
                float normalizedX = coords.x / size.x;
                float normalizedY = coords.y / size.y;
                float wave = sin((normalizedX + normalizedY) * 6.0 + time) * 0.5 + 0.5;
                // Output alpha channel is the 0..1 blur intensity over the mask's maxRadius
                return half4(0.0, 0.0, 0.0, wave);
            }
            """
                .trimIndent()
        )
    }

    val customBlurRadius =
        remember(customShader) { BlurRadiusSpec.shader(maxRadius = 24.dp) { customShader } }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "4. Dynamic Custom AGSL Shader Mask",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    "A custom AGSL shader calculates a moving wave. The output alpha channel scales the local blur radius.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Box(
                modifier =
                    Modifier.fillMaxWidth().height(260.dp).blur {
                        val sizePx = size.toSize()
                        customShader.setFloatUniform("size", sizePx.width, sizePx.height)
                        customShader.setFloatUniform("time", animatedTime)
                        radius = customBlurRadius
                    },
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE53935), Color(0xFFFDD835))
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Organic Peppers",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors =
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(16.dp)
                ) {
                    Text(
                        "AGSL Wave Mask",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Real-time procedural math blur modulation",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
