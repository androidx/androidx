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

package androidx.compose.ui.lighting

internal object SurfaceGeometryShaders {

    const val UNIFORM_CORNER_POSITIONS = "uCornerPositions"
    const val UNIFORM_CORNER_RADIUS = "uCornerRadius"
    const val UNIFORM_BEVEL = "uBevel"

    private const val RoundedRectHelperShaderSource =
        """
        // Packs the outline bounds as (left, right, top, bottom).
        uniform float4 $UNIFORM_CORNER_POSITIONS;
        // Packs the four circular corner radii as (topLeft, topRight, bottomRight, bottomLeft).
        uniform float4 $UNIFORM_CORNER_RADIUS;

        const float HALF_PI = 1.5707963267948966;

        /**
         * Outputs edgeDelta and local coordinates given the pixel coordinate.
         * edgeDelta is the per-axis signed distance from the edges of the inner rectangle.
         * local is the local coordinates of the pixel in the round rect (origin at center).
         */
        float roundedRectSdf(float2 fragCoord, out float2 edgeDelta, out float2 local) {
            float2 rectCenter =
                ($UNIFORM_CORNER_POSITIONS.yw + $UNIFORM_CORNER_POSITIONS.xz) * 0.5;
            float2 halfSizes =
                ($UNIFORM_CORNER_POSITIONS.yw - $UNIFORM_CORNER_POSITIONS.xz) * 0.5;
            local = fragCoord - rectCenter;
            float2 radiiY = local.x < 0.0 ? $UNIFORM_CORNER_RADIUS.xw : $UNIFORM_CORNER_RADIUS.yz;
            float radius = local.y < 0.0 ? radiiY.x : radiiY.y;
            edgeDelta = abs(local) - halfSizes + float2(radius, radius);
            return min(max(edgeDelta.x, edgeDelta.y), 0.0) + length(max(edgeDelta, 0.0)) - radius;
        }

        // Unit length 2D normal of the boundary, pointing away from the center.
        float2 outwardNormal(float2 edgeDelta, float2 local) {
            if (edgeDelta.x > 0.0 && edgeDelta.y > 0.0) {
                return normalize(max(edgeDelta, 0.0)) * sign(local);
            }
            return edgeDelta.x > edgeDelta.y
                ? float2(sign(local.x), 0.0)
                : float2(0.0, sign(local.y));
        }
    """

    /**
     * Builds a normal shader whose interior is flat and whose outline is wrapped in a curved band
     * of [UNIFORM_BEVEL] pixels.
     *
     * [tiltSign] controls which way the band tilts: `1.0` tilts the normals away from the center,
     * which reads as a surface bulging towards the viewer, and `-1.0` tilts them towards the center
     * (in case of a concave surface).
     */
    private fun beveledSurfaceShader(tiltSign: String) =
        """
        $RoundedRectHelperShaderSource

        uniform float $UNIFORM_BEVEL;

        half4 main(float2 fragCoord) {
            float2 edgeDelta;
            float2 local;
            float sdf = roundedRectSdf(fragCoord, edgeDelta, local);
            if (sdf > 0.0) {
                return half4(0.0);
            }

            float nzSign = 1.0;
            if ($UNIFORM_BEVEL <= 0.0 || sdf <= -$UNIFORM_BEVEL) {
                return half4(float4(0.0, 0.0, nzSign, 1.0));
            }

            // 0 at the inner edge of the band, 1 at the outline.
            float t = saturate((sdf + $UNIFORM_BEVEL) / $UNIFORM_BEVEL);
            float angle = t * HALF_PI;
            float3 normal =
                float3($tiltSign * outwardNormal(edgeDelta, local) * sin(angle),
                       cos(angle) * nzSign);
            return half4(float4(normalize(normal), 1.0));
        }
    """

    val ConvexSurfaceShader = beveledSurfaceShader(tiltSign = "1.0")

    val ConcaveSurfaceShader = beveledSurfaceShader(tiltSign = "-1.0")

    const val FlatSurfaceShader =
        """
        $RoundedRectHelperShaderSource

        half4 main(float2 fragCoord) {
            float2 edgeDelta;
            float2 local;
            float sdf = roundedRectSdf(fragCoord, edgeDelta, local);
            if (sdf > 0.0) {
                return half4(0.0);
            }

            float nzSign = 1.0;
            return half4(0.0, 0.0, nzSign, 1.0);
        }
    """
}
