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

package androidx.compose.ui.samples

import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.annotation.Sampled
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Sampled
@Composable
fun ProgressiveBlurSample() {
    Box(
        Modifier.size(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            // Sharp at the top, increasingly blurred toward the bottom.
            .blur(BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 24.dp))
    )
}

@Sampled
@Composable
fun AnimatedProgressiveBlurSample() {
    var blurred by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(if (blurred) 24.dp else 0.dp, label = "blurRadius")
    Box(
        Modifier.size(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            // blurRadius is read inside the draw-time block, so the animation invalidates only the
            // draw phase.
            .blur {
                radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = blurRadius)
            }
            .clickable { blurred = !blurred }
    )
}

@Sampled
@Composable
fun MultiStopProgressiveBlurSample() {
    Box(
        Modifier.size(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            .blur {
                radius =
                    BlurRadiusSpec.verticalGradient(
                        listOf(
                            BlurStop(fraction = 0f, radius = 0.dp),
                            BlurStop(fraction = 0.5f, radius = 4.dp),
                            BlurStop(fraction = 1f, radius = 24.dp),
                        )
                    )
            }
    )
}

@Sampled
@Composable
fun AngledProgressiveBlurSample() {
    // Blur direction in degrees, CSS convention: 0° fades toward the top, positive angles turn
    // clockwise, so 45° fades toward the top-right.
    val angleDegrees = 45f
    Box(
        Modifier.size(width = 240.dp, height = 120.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            .blur {
                val angle = angleDegrees * PI.toFloat() / 180f
                // Unit direction of increasing blur; y is negated because screen y points down.
                val dirX = sin(angle)
                val dirY = -cos(angle)
                // Half-length of the gradient line that exactly spans the layer at this angle:
                // the layer's corners project onto the line at fractions 0 and 1.
                val width = size.width.value
                val height = size.height.value
                val halfLength = (abs(width * dirX) + abs(height * dirY)) / 2f
                val center = DpOffset((width / 2f).dp, (height / 2f).dp)
                val halfLine = DpOffset((dirX * halfLength).dp, (dirY * halfLength).dp)
                radius =
                    BlurRadiusSpec.linearGradient(
                        start = center - halfLine,
                        end = center + halfLine,
                        startRadius = 0.dp,
                        endRadius = 16.dp,
                    )
            }
    )
}

@Sampled
@Composable
fun RadialProgressiveBlurSample() {
    Box(
        Modifier.size(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            // Sharp at the center, blurred toward the edges (center and extent default to the
            // layer).
            .blur { radius = BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 24.dp) }
    )
}

@Sampled
@Composable
@RequiresApi(33)
fun ShaderProgressiveBlurSample() {
    // A RuntimeShader whose alpha channel is a 0..1 blur intensity over the mask's maxRadius.
    val radiusShader = remember {
        RuntimeShader(
            """
            uniform float2 size;
            half4 main(float2 coord) {
                // alpha is the blur intensity: 0 at the top (sharp) to 1 at the bottom.
                return half4(0.0, 0.0, 0.0, coord.y / max(size.y, 1.0));
            }
            """
                .trimIndent()
        )
    }
    // Hoisted so a single configuration is reused; the block re-runs at each resolve with the
    // current layer size.
    val maskRadius =
        remember(radiusShader) {
            BlurRadiusSpec.shader(maxRadius = 24.dp) { sizePx ->
                radiusShader.setFloatUniform("size", sizePx.width, sizePx.height)
                radiusShader
            }
        }
    Box(
        Modifier.size(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
            .blur { radius = maskRadius }
    )
}
