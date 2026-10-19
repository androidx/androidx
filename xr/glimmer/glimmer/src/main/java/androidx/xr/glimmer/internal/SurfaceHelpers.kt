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

package androidx.xr.glimmer.internal

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.surface
import org.intellij.lang.annotations.Language

/** Enter animation for focus highlight and depth effect */
internal val SurfaceAnimationFocusedEnter =
    tween<Float>(durationMillis = 800, easing = LinearOutSlowInEasing)

/** Exit animation for focus highlight and depth effect */
internal val SurfaceAnimationFocusedExit =
    tween<Float>(durationMillis = 500, easing = LinearOutSlowInEasing)

internal val SurfaceAnimationAmbientEnterExit =
    tween<Float>(durationMillis = 2000, easing = LinearEasing)
internal const val SurfaceAnimationAmbientInitialDelayMillis = 1800L
internal const val SurfaceAnimationAmbientDelayMillis = 4000L

internal val SurfaceAnimationPressedEnter = spring<Float>(dampingRatio = 0.84f, stiffness = 8000f)
internal val SurfaceAnimationPressedExit = spring<Float>(dampingRatio = 0.85f, stiffness = 50f)
internal const val SurfaceAnimationPressedMinimumDurationMillis = 300L

internal const val SurfaceFocusedColorTone = 34f
internal val SurfaceDisabledOverlayColor = Color(0x8F191919)

/** Returns the overlay color that should be used when a press is at [pressedProgress]. */
internal fun surfacePressedOverlayColor(pressedProgress: Float) =
    Color.White.copy(alpha = 0.16f * pressedProgress)

internal object BlurShader {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @JvmStatic
    fun configureShader(
        shader: RuntimeShader?,
        isVertical: Boolean,
        width: Float,
        height: Float,
        cornerRadii: FloatArray,
        focusProgress: Float,
        ambientProgress: Float,
        pressedProgress: Float,
        idleBlurRadiusStartPx: Float,
        idleBlurRadiusEndPx: Float,
        focusedBlurRadiusStartPx: Float,
        focusedBlurRadiusEndPx: Float,
        ambientBlurRadiusMaxStartPx: Float,
        ambientBlurRadiusMaxEndPx: Float,
    ): RuntimeShader {
        val runtimeShader = shader ?: RuntimeShader(getBlurShader(isVertical))
        runtimeShader.setFloatUniform(UniformResolution, width, height)
        runtimeShader.setFloatUniform(UniformCornerRadii, cornerRadii)
        runtimeShader.setFloatUniform(UniformFocusProgress, focusProgress)
        runtimeShader.setFloatUniform(UniformAmbientProgress, ambientProgress)
        runtimeShader.setFloatUniform(UniformPressedProgress, pressedProgress)
        runtimeShader.setFloatUniform(UniformIdleStartRadius, idleBlurRadiusStartPx)
        runtimeShader.setFloatUniform(UniformIdleEndRadius, idleBlurRadiusEndPx)
        runtimeShader.setFloatUniform(UniformFocusedStartRadius, focusedBlurRadiusStartPx)
        runtimeShader.setFloatUniform(UniformFocusedEndRadius, focusedBlurRadiusEndPx)
        runtimeShader.setFloatUniform(UniformAmbientMaxStartRadius, ambientBlurRadiusMaxStartPx)
        runtimeShader.setFloatUniform(UniformAmbientMaxEndRadius, ambientBlurRadiusMaxEndPx)
        return runtimeShader
    }

    /** String keys map directly to uniform parameter names inside the AGSL runtime shaders. */
    const val UniformContent = "uContent"

    /** Blur radii at the start / end of the blur gradient while the surface is idle (unfocused). */
    internal val IdleBlurRadiusStart = 2.dp
    internal val IdleBlurRadiusEnd = 8.dp

    /** Blur radii at the start / end of the blur gradient while the surface is focused. */
    internal val FocusedBlurRadiusStart = 1.dp
    internal val FocusedBlurRadiusEnd = 3.dp

    /** Maximum blur radii reached at the peak of the ambient motion */
    internal val AmbientBlurRadiusMaxStart = 5.3.dp
    internal val AmbientBlurRadiusMaxEnd = 15.9.dp
}

internal object BorderShader {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @JvmStatic
    fun configureShader(
        shader: RuntimeShader?,
        size: Size,
        focusProgress: Float,
        ambientProgress: Float,
        focusedBorderColor0: Color,
        focusedBorderColor1: Color,
        focusedBorderColor2: Color,
        focusedBorderColor3: Color,
    ): RuntimeShader {
        val runtimeShader = shader ?: RuntimeShader(BorderShaderAgsl)
        runtimeShader.setFloatUniform(UniformResolution, size.width, size.height)
        runtimeShader.setFloatUniform(UniformFocusProgress, focusProgress)
        runtimeShader.setFloatUniform(UniformAmbientProgress, ambientProgress)

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor0,
            focusedBorderColor0.red,
            focusedBorderColor0.green,
            focusedBorderColor0.blue,
            focusedBorderColor0.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor1,
            focusedBorderColor1.red,
            focusedBorderColor1.green,
            focusedBorderColor1.blue,
            focusedBorderColor1.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor2,
            focusedBorderColor2.red,
            focusedBorderColor2.green,
            focusedBorderColor2.blue,
            focusedBorderColor2.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor3,
            focusedBorderColor3.red,
            focusedBorderColor3.green,
            focusedBorderColor3.blue,
            focusedBorderColor3.alpha,
        )

        return runtimeShader
    }

    /** Default border width for a [surface]. */
    internal val DefaultSurfaceBorderWidth = 1.5.dp

    /** Focused border width for a [surface]. */
    internal val FocusedSurfaceBorderWidth = 2.dp

    internal val DefaultSolidBorderIdleColor = Color(0.25f, 0.25f, 0.25f, 0.5f)
}

@Language(value = "AGSL")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun getBlurShader(isVertical: Boolean): String {
    val loopOffset =
        if (isVertical) "float2(0.0, i + weightH / weight)" else "float2(i + weightH / weight, 0.0)"
    val oddOffset = if (isVertical) "float2(0.0, r)" else "float2(r, 0.0)"
    return """
        uniform shader uContent;

        uniform float2 uResolution;
        uniform float4 uCornerRadii;
        uniform float uFocusProgress;
        uniform float uAmbientProgress;
        uniform float uPressedProgress;

        uniform float uIdleRadiusStart;
        uniform float uIdleRadiusEnd;
        uniform float uFocusedRadiusStart;
        uniform float uFocusedRadiusEnd;
        uniform float uAmbientMaxRadiusStart;
        uniform float uAmbientMaxRadiusEnd;

        const float MAX_RADIUS = 120.0;
        const float PI = 3.14159265;

        float sdRoundedBox(float2 p, float2 b, float4 r) {
            float2 sign_p = step(0.0, p);
            float2 r2 = mix(r.zw, r.xy, sign_p.x);
            float rad = mix(r2.y, r2.x, sign_p.y);

            float2 q = abs(p) - b + rad;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
        }

        float gaussian(float x, float sigma) { return exp(-(x * x) / (2.0 * sigma * sigma)); }

        float radiusAt(float2 coord, float2 start, float2 end, float r0, float r1) {
            float2 axis = end - start;
            float t = clamp(dot(coord - start, axis) / max(dot(axis, axis), 1.0), 0.0, 1.0);
            return mix(r0, r1, t);
        }

        float4 blur(float2 coord, float radius) {
            float r = floor(radius);
            if (r < 1.0) return uContent.eval(coord);
            float sigma = max(radius / 2.0, 1.0);
            float2 center = uResolution / 2.0;
            float2 halfSize = uResolution / 2.0;
            float weightSum = 1.0;
            float limit = min(sigma * 3, MAX_RADIUS);
            float4 result = uContent.eval(coord);
            for (float i = 1.0; i < MAX_RADIUS; i += 2.0) {
                if (i >= limit) { break; }
                float weightL = gaussian(i, sigma);
                float weightH = gaussian(i + 1.0, sigma);
                float weight = weightL + weightH;
                float2 off = $loopOffset;

                float2 c1 = coord - off;
                float d1 = sdRoundedBox(c1 - center, halfSize, uCornerRadii);
                float w1 = mix(weight, 0.0, step(0.0, d1));
                if (w1 > 0.0) {
                    result += w1 * uContent.eval(c1);
                    weightSum += w1;
                }

                float2 c2 = coord + off;
                float d2 = sdRoundedBox(c2 - center, halfSize, uCornerRadii);
                float w2 = mix(weight, 0.0, step(0.0, d2));
                if (w2 > 0.0) {
                    result += w2 * uContent.eval(c2);
                    weightSum += w2;
                }
            }
            float oddMask = fract(r * 0.5) * 2.0 * (1.0 - step(MAX_RADIUS, r));
            float ow = gaussian(r, sigma) * oddMask;

            if (ow > 0.0) {
                float2 oo = $oddOffset;
                float2 o1 = coord - oo;
                float2 o2 = coord + oo;
                result += ow * uContent.eval(o1) + ow * uContent.eval(o2);
                weightSum += 2.0 * ow;
            }
            return result / weightSum;
        }

        half4 main(float2 coord) {
            // Compute ambient pulse
            // Phase 1: Increase Ambient Pulse
            // Linearly scales 0.0 -> 0.2835 onto a 0.0 -> 1.0 range.
            float t1 = clamp(uAmbientProgress / 0.2835, 0.0, 1.0);

            // Phase 2: Steady and then Ramp-Down
            // Delays the fade-out until progress passes 0.375
            // Uses the remaining 0.625 of the duration to smoothly decline back to 0.0.
            float t2 = clamp(1.0 - (uAmbientProgress - 0.375) / 0.625, 0.0, 1.0);

            // Uses t1 (ramp-up/hold) for the first 37.5%, then switches to t2 (ramp-down).
            float pulse = mix(t1, t2, step(0.375, uAmbientProgress));

            // Mix focused and ambient blurs based on the pulse
            float focusedBlurStart = mix(uFocusedRadiusStart, uAmbientMaxRadiusStart, pulse);
            float focusedBlurEnd = mix(uFocusedRadiusEnd, uAmbientMaxRadiusEnd, pulse);

            // Interpolate between idle and focused/ambient blur ranges
            float blur0 = mix(uIdleRadiusStart, focusedBlurStart, uFocusProgress);
            float blur1 = mix(uIdleRadiusEnd, focusedBlurEnd, uFocusProgress);

            // Enforce massive uniform during press interaction
            float minDimension = min(uResolution.x, uResolution.y);
            float pressedBlur = minDimension / 3.0;
            blur0 = mix(blur0, pressedBlur, uPressedProgress);
            blur1 = mix(blur1, pressedBlur, uPressedProgress);

            float centerX = uResolution.x / 2.0;
            float centerY = uResolution.y / 2.0;

            // Start position of blur shifts across the width based on focus progress
            float startX = uFocusProgress * uResolution.x;
            float startY = 0.0;
            float endX = uResolution.x / 2.0;
            float endY = uResolution.y;

            // Anchor coordinates for rotational animation phase
            float baseStartX = uResolution.x;
            float baseStartY = 0.0;
            float baseEndX = uResolution.x / 2.0;
            float baseEndY = uResolution.y;

            // Drive a full circle around the center for ambient motion
            float angle = uAmbientProgress * PI * 2;
            float cosA = cos(angle);
            float sinA = sin(angle);

            // Calculate start and end rotation X and Y
            float rotStartX = centerX + (baseStartX - centerX) * cosA - (baseStartY - centerY) * sinA;
            float rotStartY = centerY + (baseStartX - centerX) * sinA + (baseStartY - centerY) * cosA;
            float rotEndX = centerX + (baseEndX - centerX) * cosA - (baseEndY - centerY) * sinA;
            float rotEndY = centerY + (baseEndX - centerX) * sinA + (baseEndY - centerY) * cosA;

            float2 finalStart = mix(float2(startX, startY), float2(rotStartX, rotStartY), uFocusProgress);
            float2 finalEnd = mix(float2(endX, endY), float2(rotEndX, rotEndY), uFocusProgress);

            return blur(coord, radiusAt(coord, finalStart, finalEnd, blur0, blur1));
        }
    """
}

@Language("AGSL")
private const val BorderShaderAgsl =
    """
uniform float2 uResolution;
uniform float uFocusProgress;
uniform float uAmbientProgress;

const half4 uBorderIdleColor0 = half4(0.81, 0.81, 0.81, 0.9); // Top-Left
const half4 uBorderIdleColor1 = half4(0.25, 0.25, 0.25, 0.5); // Top-Right
const half4 uBorderIdleColor2 = half4(0.16, 0.16, 0.16, 0.4); // Bottom-Right
const half4 uBorderIdleColor3 = half4(0.49, 0.49, 0.49, 0.7); // Bottom-Left

uniform half4 uBorderFocusedColor0;
uniform half4 uBorderFocusedColor1;
uniform half4 uBorderFocusedColor2;
uniform half4 uBorderFocusedColor3;

const half4 uAnimationPeakColor = half4(1.0, 1.0, 1.0, 1.0); // White

const float PI = 3.14159265;

half4 main(float2 fragCoord) {
    // Compute ambient pulse
    // Phase 1: Increase Ambient Pulse
    // Linearly scales 0.0 -> 0.2835 onto a 0.0 -> 1.0 range.
    float t1 = clamp(uAmbientProgress / 0.2835, 0.0, 1.0);

    // Phase 2: Steady and then Ramp-Down
    // Delays the fade-out until progress passes 0.375
    // Uses the remaining 0.625 of the duration to smoothly decline back to 0.0.
    float t2 = clamp(1.0 - (uAmbientProgress - 0.375) / 0.625, 0.0, 1.0);

    // Uses t1 (ramp-up/hold) for the first 37.5%, then switches to t2 (ramp-down).
    float ambientPulse = mix(t1, t2, step(0.375, uAmbientProgress));

    // Compute focus pulse peaking animation peak color at exactly 0.5
    float focusPulse = clamp(1.0 - 2.0 * abs(uFocusProgress - 0.5), 0.0, 1.0);

    // Use maximum of focus and ambient pulses
    float maxAnimationPeakPulse = max(ambientPulse, focusPulse);

    half4 colors[4];
    colors[0] = mix(mix(uBorderIdleColor0, uBorderFocusedColor0, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[1] = mix(mix(uBorderIdleColor1, uBorderFocusedColor1, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[2] = mix(mix(uBorderIdleColor2, uBorderFocusedColor2, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[3] = mix(mix(uBorderIdleColor3, uBorderFocusedColor3, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);

    float w = uResolution.x;
    float h = uResolution.y;

    // Normalize coordinates to [-1, 1] relative to center
    float nx = 2.0 * fragCoord.x / max(w, 1.0) - 1.0;
    float ny = 2.0 * fragCoord.y / max(h, 1.0) - 1.0;

    // Compute the angle of the current pixel relative to the center in radians [-PI, PI].
    float theta = atan(ny, nx);

    // Map the angle from [-PI, PI] to a clean [0.0, 1.0] percentage circle.
    // The "+ 0.75 * PI" offsets the starting point (0.0) to the Top-Left corner.
    float d_cw = fract((theta + 0.75 * PI) / (2.0 * PI));

    // As focus progresses from 0 to 1, the gradient start position shifts from top left to top right
    float d_start = mix(0.0, 0.25, uFocusProgress);

    // Apply the rotation shift to the current pixel's position.
    float d_rotated = mod(d_cw - d_start + 1.0, 1.0);
    float dist = min(d_rotated, 1.0 - d_rotated);

    // Branchless color interpolation using chained mixes
    float factor0 = smoothstep(0.0, 0.1, dist);
    float factor1 = smoothstep(0.1, 0.25, dist);
    float factor2 = smoothstep(0.25, 0.4, dist);

    half4 color = mix(colors[0], colors[1], factor0);
    color = mix(color, colors[2], factor1);
    color = mix(color, colors[3], factor2);

    return color;
}
"""

private const val UniformResolution = "uResolution"
private const val UniformCornerRadii = "uCornerRadii"
private const val UniformFocusProgress = "uFocusProgress"
private const val UniformAmbientProgress = "uAmbientProgress"
private const val UniformPressedProgress = "uPressedProgress"
private const val UniformIdleStartRadius = "uIdleRadiusStart"
private const val UniformIdleEndRadius = "uIdleRadiusEnd"
private const val UniformFocusedStartRadius = "uFocusedRadiusStart"
private const val UniformFocusedEndRadius = "uFocusedRadiusEnd"
private const val UniformAmbientMaxStartRadius = "uAmbientMaxRadiusStart"
private const val UniformAmbientMaxEndRadius = "uAmbientMaxRadiusEnd"

private const val UniformBorderFocusedColor0 = "uBorderFocusedColor0"
private const val UniformBorderFocusedColor1 = "uBorderFocusedColor1"
private const val UniformBorderFocusedColor2 = "uBorderFocusedColor2"
private const val UniformBorderFocusedColor3 = "uBorderFocusedColor3"
