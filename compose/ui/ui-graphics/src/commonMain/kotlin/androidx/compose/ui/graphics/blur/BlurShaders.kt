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

package androidx.compose.ui.graphics.blur

internal object BlurShaders {
    const val UniformContent = "content"
    const val UniformMask = "mask"
    const val UniformBlurRadius = "blurRadius"
    const val UniformCrop = "crop"
    const val UniformUnbounded = "unbounded"
    const val UniformStartIntensity = "startIntensity"
    const val UniformEndIntensity = "endIntensity"
    const val UniformStartPoint = "startPoint"
    const val UniformEndPoint = "endPoint"
    const val UniformIntensities = "intensities"
    const val UniformPositions = "positions"
    const val UniformCenter = "center"
    const val UniformRadius = "radius"

    /**
     * Maximum blur radius in pixels.
     *
     * Clamps the radius before reaching shader uniforms (see `setBaseBlurUniforms`). Interpolated
     * into the generated blur source as `maxRadius`.
     */
    const val MaxBlurRadiusPx = 150f

    // SkSL for `getIntensity(t)`: evaluates a piecewise-linear function over fixed-size
    // ([BlurStop.MaxStops]) intensity and position arrays.
    // Arrays are padded to [BlurStop.MaxStops] using the last real stop at position 1.0.
    // The `max(p1 - p0, 0.0001)` guard prevents division by zero in zero-width tail segments.
    private val intensitySkSl: String = run {
        val last = BlurStop.MaxStops - 1
        val body =
            """
            if (t <= positions[0]) return intensities[0];
            if (t >= positions[$last]) return intensities[$last];

            for (int i = 0; i < $last; i++) {
                float p0 = positions[i];
                float p1 = positions[i + 1];

                if (t >= p0 && t <= p1) {
                    float fraction = (t - p0) / max(p1 - p0, 0.0001);
                    return mix(intensities[i], intensities[i + 1], clamp(fraction, 0.0, 1.0));
                }
            }
            return intensities[$last];
            """
                .trimIndent()

        """
        float getIntensity(float t) {
                 ${body.prependIndent("            ")}
        }
        """
            .trimIndent()
    }

    /**
     * Generates the core SkSL blur functions: gaussian weight, bounds test, and separable blur.
     *
     * Shared by all blur passes. Variants only differ in uniform headers and `main()`.
     *
     * @param loopOffset per-sample offset expression for even (paired) taps
     * @param oddOffset per-sample offset expression for the odd tail tap
     * @param boundsBody body of `inBoundsOnMovedAxis` - a moved-axis-only in-bounds test
     */
    private fun blurCoreSkSl(loopOffset: String, oddOffset: String, boundsBody: String): String =
        """
        float gaussian(float x, float sigma) {
            return exp(-(x * x) / (2.0 * sigma * sigma));
        }

        float inBoundsOnMovedAxis(vec2 sampleCoord, float4 bounds) {
            $boundsBody
        }

        vec4 blur(vec2 coord, float radius) {
            float r = floor(radius);

            if (r < 1.0) { return content.eval(coord); }

            float sigma = max(radius / 2.0, 1.0);
            float weightSum = 1.0;
            vec4 result = content.eval(coord);

            for (float i = 1.0; i < maxRadius; i += 2.0) {
                if (i >= r) { break; }

                float weightL = gaussian(i, sigma);
                float weightH = gaussian(i + 1.0, sigma);
                float weight = weightL + weightH;
                vec2 offset = $loopOffset;

                // Accumulates weightSum using max(mask, unbounded) outside the in-bounds fetch.
                // Decal mode (unbounded == 1.0): denominator stays full, out-of-bounds fetches are
                // skipped, and edges fade to transparent.
                // Clamp mode (unbounded == 0.0): denominator shrinks to in-bounds weight,
                // renormalizing interior samples - an approximation of true clamp.
                // Do not fold weightSum inside the mask multiply in Decal mode to avoid the
                // historical clamp-instead-of-fade bug.
                vec2 newCoord1 = coord - offset;
                float mask1 = inBoundsOnMovedAxis(newCoord1, crop);
                weightSum += weight * max(mask1, unbounded);
                if (mask1 > 0.0) {
                    result += weight * content.eval(newCoord1);
                }

                vec2 newCoord2 = coord + offset;
                float mask2 = inBoundsOnMovedAxis(newCoord2, crop);
                weightSum += weight * max(mask2, unbounded);
                if (mask2 > 0.0) {
                    result += weight * content.eval(newCoord2);
                }
            }

            float oddMask = mod(r, 2.0) * (1.0 - step(maxRadius, r));
            float oddWeight = gaussian(r, sigma) * oddMask;
            vec2 oddOffset = $oddOffset;

            vec2 oddCoord1 = coord - oddOffset;
            float oddBounds1 = inBoundsOnMovedAxis(oddCoord1, crop);
            weightSum += oddWeight * max(oddBounds1, unbounded);
            if (oddBounds1 > 0.0) {
                result += oddWeight * content.eval(oddCoord1);
            }

            vec2 oddCoord2 = coord + oddOffset;
            float oddBounds2 = inBoundsOnMovedAxis(oddCoord2, crop);
            weightSum += oddWeight * max(oddBounds2, unbounded);
            if (oddBounds2 > 0.0) {
                result += oddWeight * content.eval(oddCoord2);
            }

            return result / weightSum;
        }
        """
            .trimIndent()

    /**
     * The gradient line is data, not behavior: [UniformStartPoint]/[UniformEndPoint] carry it in
     * layer pixel coordinates, and each pixel projects onto that segment for its intensity
     * fraction. Only the blur pass axis (structurally different loop code) remains a source-level
     * variant, so this family is two programs instead of four. The projection form renders
     * pixel-identically to the previous baked-axis form (verified on host raster and device GPU).
     */
    fun progressiveLinearBlurSkSl(isVerticalBlur: Boolean): String {
        val loopOffset =
            if (isVerticalBlur) "vec2(0.0, i + weightH / weight)"
            else "vec2(i + weightH / weight, 0.0)"

        val oddOffset = if (isVerticalBlur) "vec2(0.0, r)" else "vec2(r, 0.0)"

        val boundsBody =
            if (isVerticalBlur) {
                "return step(bounds.y, sampleCoord.y) * (1.0 - step(bounds.w, sampleCoord.y));"
            } else {
                "return step(bounds.x, sampleCoord.x) * (1.0 - step(bounds.z, sampleCoord.x));"
            }

        val header =
            """
            uniform shader content;
            uniform float blurRadius;
            uniform float4 crop;
            uniform float unbounded;
            const float maxRadius = $MaxBlurRadiusPx;
            uniform float startIntensity;
            uniform float endIntensity;
            uniform float2 startPoint;
            uniform float2 endPoint;
            """
                .trimIndent()

        val main =
            """
            half4 main(float2 coord) {
                float2 pa = coord - startPoint;
                float2 ba = endPoint - startPoint;

                float fraction = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);

                float intensity = mix(startIntensity, endIntensity, fraction);
                float radius = blurRadius * intensity;

                return half4(blur(coord, radius));
            }
            """
                .trimIndent()

        return header + "\n\n" + blurCoreSkSl(loopOffset, oddOffset, boundsBody) + "\n\n" + main
    }

    fun maskedBlurSkSl(isVertical: Boolean): String {
        val loopOffset =
            if (isVertical) "vec2(0.0, i + weightH / weight)" else "vec2(i + weightH / weight, 0.0)"

        val oddOffset = if (isVertical) "vec2(0.0, r)" else "vec2(r, 0.0)"

        val boundsBody =
            if (isVertical) {
                "return step(bounds.y, sampleCoord.y) * (1.0 - step(bounds.w, sampleCoord.y));"
            } else {
                "return step(bounds.x, sampleCoord.x) * (1.0 - step(bounds.z, sampleCoord.x));"
            }

        val header =
            """
            uniform shader content;
            uniform shader mask;
            uniform float blurRadius;
            uniform float4 crop;
            uniform float unbounded;
            const float maxRadius = $MaxBlurRadiusPx;
            """
                .trimIndent()

        val main =
            """
            half4 main(float2 coord) {
                vec2 maskCoord = max(coord - crop.xy, vec2(0.0, 0.0));
                // Mask alpha is a 0..1 intensity over blurRadius; the clamp defends against
                // out-of-range custom masks.
                float intensity = clamp(mask.eval(maskCoord).a, 0.0, 1.0);
                float finalRadius = blurRadius * intensity;

                return half4(blur(coord, finalRadius));
            }
            """
                .trimIndent()

        return header + "\n\n" + blurCoreSkSl(loopOffset, oddOffset, boundsBody) + "\n\n" + main
    }

    fun multiLinearMaskSkSl(): String {
        val header =
            """
            uniform float2 startPoint;
            uniform float2 endPoint;

            uniform float intensities[${BlurStop.MaxStops}];
            uniform float positions[${BlurStop.MaxStops}];
            """
                .trimIndent()

        val main =
            """
            half4 main(float2 coord) {
                float2 pa = coord - startPoint;
                float2 ba = endPoint - startPoint;

                float ba_length_sq = dot(ba, ba);
                float t = ba_length_sq > 0.0 ? dot(pa, ba) / ba_length_sq : 0.0;
                t = clamp(t, 0.0, 1.0);

                return half4(0.0, 0.0, 0.0, getIntensity(t));
            }
            """
                .trimIndent()

        return header + "\n\n" + intensitySkSl + "\n\n" + main
    }

    fun multiRadialMaskSkSl(): String {
        val header =
            """
            uniform float2 center;
            uniform float radius;

            // Arrays for multi-stop data
            uniform float intensities[${BlurStop.MaxStops}];
            uniform float positions[${BlurStop.MaxStops}];
            """
                .trimIndent()

        val main =
            """
            half4 main(float2 coord) {
                float d = distance(coord, center);

                // Normalize against max radius
                float t = radius > 0.0 ? d / radius : 0.0;
                t = clamp(t, 0.0, 1.0);

                float alpha = getIntensity(t);

                return half4(0.0, 0.0, 0.0, alpha);
            }
            """
                .trimIndent()

        return header + "\n\n" + intensitySkSl + "\n\n" + main
    }
}
