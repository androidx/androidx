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

package androidx.compose.ui.graphics

import android.graphics.BlendMode
import android.graphics.Mesh
import android.graphics.MeshSpecification
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import org.intellij.lang.annotations.Language

/**
 * API 34+ [BaseShaderMeshGradientRenderer] that renders a mesh gradient using Android's `Mesh` API
 * (available from API 34).
 *
 * This API leverages Android's `Mesh` API to render a bicubic Bezier patch mesh. Each patch is
 * defined by 4 corner points of a cell in the grid, 8 bezier control points (2 per side), and 16
 * color points (4x4 grid around the patch). The per-pixel shading is done by the [meshSpec] vertex
 * and fragment shaders; the platform-neutral setup lives in [BaseShaderMeshGradientRenderer].
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class MeshGradientRendererV34Impl : BaseShaderMeshGradientRenderer() {
    private val paint = Paint()

    private val attributes =
        arrayOf(MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT2, 0, "uv"))

    private val stride = 8

    private val varyings = arrayOf(MeshSpecification.Varying(MeshSpecification.TYPE_FLOAT2, "uv"))

    private val meshSpec =
        MeshSpecification.make(
            attributes,
            stride,
            varyings,
            vertexShaderSource,
            fragmentShaderSource,
            ColorSpaces.LinearSrgb.toAndroidColorSpace(),
            MeshSpecification.ALPHA_TYPE_PREMULTIPLIED,
        )

    // Mesh instances once created for a patch are stored here for reuse in subsequent draw calls.
    private var meshObjects: Array<Mesh>? = null

    private var vertexDataBuffer: ByteBuffer? = null
    private var indexBuffer: ShortBuffer? = null

    // In case the canvas is not hardware-accelerated, we use this renderer instance that uses
    // `drawVertices` to draw the mesh.
    private var meshGradientFallbackRendererImpl: MeshGradientRenderer? = null

    override fun DrawScope.drawFallbackIfNeeded(config: MeshGradientConfig): Boolean {
        if (drawContext.canvas.nativeCanvas.isHardwareAccelerated) return false
        // Fallback to `drawVertices` since it is supported on a software backed canvas.
        val fallback =
            meshGradientFallbackRendererImpl
                ?: MeshGradientRendererImpl().also { meshGradientFallbackRendererImpl = it }
        with(fallback) { draw(config) }
        return true
    }

    override fun onGeometryChanged(
        uvBuffer: FloatArray,
        indexBuffer: ShortArray,
        subdivisionsU: Int,
        subdivisionsV: Int,
    ) {
        vertexDataBuffer =
            ByteBuffer.allocateDirect(uvBuffer.size * 4).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().put(uvBuffer)
                position(0)
            }
        this.indexBuffer =
            ByteBuffer.allocateDirect(indexBuffer.size * 2)
                .apply {
                    order(ByteOrder.nativeOrder())
                    asShortBuffer().put(indexBuffer)
                    position(0)
                }
                .asShortBuffer()
    }

    override fun onMeshInstancesChanged(
        subdivisionsU: Int,
        subdivisionsV: Int,
        patchCount: Int,
        bounds: Size,
    ) {
        val vertexDataBuffer = this.vertexDataBuffer ?: return
        val indexBuffer = this.indexBuffer ?: return
        val vertexCount = subdivisionsU * subdivisionsV
        meshObjects =
            Array(patchCount) {
                Mesh(
                    meshSpec,
                    Mesh.TRIANGLES,
                    vertexDataBuffer,
                    vertexCount,
                    indexBuffer,
                    RectF(0f, 0f, bounds.width, bounds.height),
                )
            }
    }

    override fun drawPatch(
        canvas: Canvas,
        patchIndex: Int,
        hasBicubicColor: Boolean,
        pointLocations: FloatArray,
        pointColors: FloatArray,
        leftBezierOffsets: FloatArray,
        rightBezierOffsets: FloatArray,
        topBezierOffsets: FloatArray,
        bottomBezierOffsets: FloatArray,
    ) {
        val mesh = meshObjects!![patchIndex]

        mesh.setIntUniform("useBicubicColorInterpolation", if (hasBicubicColor) 1 else 0)
        mesh.setFloatUniform("baseMeshPointLocations", pointLocations)
        mesh.setFloatUniform("baseMeshPointColors", pointColors)
        mesh.setFloatUniform("baseMeshPointLeftBezierOffsets", leftBezierOffsets)
        mesh.setFloatUniform("baseMeshPointRightBezierOffsets", rightBezierOffsets)
        mesh.setFloatUniform("baseMeshPointTopBezierOffsets", topBezierOffsets)
        mesh.setFloatUniform("baseMeshPointBottomBezierOffsets", bottomBezierOffsets)

        // Using BlendMode.DST since this argument dictates blending with mesh primitives as the
        // destination color and paint's color/shader as the source color.
        canvas.nativeCanvas.drawMesh(mesh, BlendMode.DST, paint)
    }
}

@Language("AGSL")
private val vertexShaderSource =
    """
    uniform int useBicubicColorInterpolation;
    uniform float2 baseMeshPointLocations[4];
    uniform float4 baseMeshPointColors[16];
    uniform float2 baseMeshPointLeftBezierOffsets[4];
    uniform float2 baseMeshPointRightBezierOffsets[4];
    uniform float2 baseMeshPointTopBezierOffsets[4];
    uniform float2 baseMeshPointBottomBezierOffsets[4];
    
    // Basis matrix
    const float4x4 bM = float4x4(
        vec4(-1.0, 3.0, -3, 1),
        vec4(3.0, -6.0, 3.0, 0),
        vec4(-3.0, 3.0, 0.0, 0),
        vec4(1.0, 0.0, 0.0, 0)
    );

    Varyings main(const Attributes attributes) {

        float2 uv = attributes.uv;

        int patchRow = 0;
        int patchColumn = 0;

        float u = uv.x;
        float v = uv.y;

        float2 pointAPos = baseMeshPointLocations[0];
        float2 pointBPos = baseMeshPointLocations[1];
        float2 pointCPos = baseMeshPointLocations[2];
        float2 pointDPos = baseMeshPointLocations[3];

        float2 pointALBO = baseMeshPointLeftBezierOffsets[0];
        float2 pointBLBO = baseMeshPointLeftBezierOffsets[1];
        float2 pointCLBO = baseMeshPointLeftBezierOffsets[2];
        float2 pointDLBO = baseMeshPointLeftBezierOffsets[3];

        float2 pointARBO = baseMeshPointRightBezierOffsets[0];
        float2 pointBRBO = baseMeshPointRightBezierOffsets[1];
        float2 pointCRBO = baseMeshPointRightBezierOffsets[2];
        float2 pointDRBO = baseMeshPointRightBezierOffsets[3];

        float2 pointATBO = baseMeshPointTopBezierOffsets[0];
        float2 pointBTBO = baseMeshPointTopBezierOffsets[1];
        float2 pointCTBO = baseMeshPointTopBezierOffsets[2];
        float2 pointDTBO = baseMeshPointTopBezierOffsets[3];

        float2 pointABBO = baseMeshPointBottomBezierOffsets[0];
        float2 pointBBBO = baseMeshPointBottomBezierOffsets[1];
        float2 pointCBBO = baseMeshPointBottomBezierOffsets[2];
        float2 pointDBBO = baseMeshPointBottomBezierOffsets[3];


        vec2 controlPointMatrix[4 * 4];

        controlPointMatrix[0 * 4 + 0] = pointAPos;
        controlPointMatrix[0 * 4 + 3] = pointBPos;
        controlPointMatrix[3 * 4 + 0] = pointCPos;
        controlPointMatrix[3 * 4 + 3] = pointDPos;

        controlPointMatrix[0 * 4 + 1] = pointAPos + pointARBO;
        controlPointMatrix[0 * 4 + 2] = pointBPos + pointBLBO;
        controlPointMatrix[3 * 4 + 1] = pointCPos + pointCRBO;
        controlPointMatrix[3 * 4 + 2] = pointDPos + pointDLBO;
        controlPointMatrix[1 * 4 + 0] = pointAPos + pointABBO;
        controlPointMatrix[2 * 4 + 0] = pointCPos + pointCTBO;
        controlPointMatrix[1 * 4 + 3] = pointBPos + pointBBBO;
        controlPointMatrix[2 * 4 + 3] = pointDPos + pointDTBO;

        controlPointMatrix[1 * 4 + 1] = controlPointMatrix[0 * 4 + 1] + controlPointMatrix[1 * 4 + 0] - controlPointMatrix[0 * 4 + 0];
        controlPointMatrix[1 * 4 + 2] = controlPointMatrix[0 * 4 + 2] + controlPointMatrix[1 * 4 + 3] - controlPointMatrix[0 * 4 + 3];
        controlPointMatrix[2 * 4 + 1] = controlPointMatrix[2 * 4 + 0] + controlPointMatrix[3 * 4 + 1] - controlPointMatrix[3 * 4 + 0];
        controlPointMatrix[2 * 4 + 2] = controlPointMatrix[2 * 4 + 3] + controlPointMatrix[3 * 4 + 2] - controlPointMatrix[3 * 4 + 3];

        
        float4 Tu = vec4(u*u*u, u*u, u, 1);
        float4 Tv = vec4(v*v*v, v*v, v, 1);
        vec4 TubM = Tu * bM;
        vec4 TvbM = Tv * bM;

        vec2 pT[4];
        for(int rowIdx = 0; rowIdx < 4; rowIdx++) {
            vec2 p0 = controlPointMatrix[rowIdx * 4 + 0];
            vec2 p1 = controlPointMatrix[rowIdx * 4 + 1];
            vec2 p2 = controlPointMatrix[rowIdx * 4 + 2];
            vec2 p3 = controlPointMatrix[rowIdx * 4 + 3];
            float4x4 G = float4x4(
                vec4(p0.x, p1.x, p2.x, p3.x),
                vec4(p0.y, p1.y, p2.y, p3.y),
                vec4(1.0, 1.0, 1.0, 1.0),
                vec4(1.0, 1.0, 1.0, 1.0)
            );
            pT[rowIdx] = (TubM * G).xy;
        }
        float4x4 G = float4x4(
            vec4(pT[0].x, pT[1].x, pT[2].x, pT[3].x),
            vec4(pT[0].y, pT[1].y, pT[2].y, pT[3].y),
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0)
        );

        vec2 finalPos = (TvbM * G).xy;

        Varyings varyings;
        varyings.position = finalPos;
        varyings.uv = uv;
        return varyings;
    }
    """

@Language("AGSL")
private val fragmentShaderSource =
    """
    uniform int useBicubicColorInterpolation;
    uniform float2 baseMeshPointLocations[4];
    uniform float4 baseMeshPointColors[16];
    uniform float2 baseMeshPointLeftBezierOffsets[4];
    uniform float2 baseMeshPointRightBezierOffsets[4];
    uniform float2 baseMeshPointTopBezierOffsets[4];
    uniform float2 baseMeshPointBottomBezierOffsets[4];
    
    const float4x4 bM = float4x4(
        vec4(0, -1, 2, -1),
        vec4(2, 0, -5, 3),
        vec4(0, 1, 4, -3),
        vec4(0, 0, -1, 1)
    );

    /** 
     *  From Bjorn's original blog introducing OkLab
     *  https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
     */
    vec3 oklab_to_linear_srgb(vec3 c) 
    {
        float l_ = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
        float m_ = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
        float s_ = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        return vec3(
        	    +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        	    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        	    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
            );
    }

    float2 main(const Varyings varyings, out float4 color) {
        float u = varyings.uv.x;
        float v = varyings.uv.y;

        if (useBicubicColorInterpolation == 0) {
            float4 pA = baseMeshPointColors[5];
            float4 pB = baseMeshPointColors[6];
            float4 pC = baseMeshPointColors[9];
            float4 pD = baseMeshPointColors[10];

            vec4 left = mix(pA, pC, v);
            vec4 right = mix(pB, pD, v);
            vec4 mixed = mix(left, right, u);
            vec3 linearSrgbColor = oklab_to_linear_srgb(mixed.xyz);
            linearSrgbColor = clamp(linearSrgbColor, 0.0, 1.0);
            color = vec4(linearSrgbColor * mixed.a, mixed.a);
        } else {
            vec4 Tu = vec4(1.0, u, u * u, u * u * u);
            vec4 Tv = vec4(1.0, v, v * v, v * v * v);
                
            vec4 TubM = 0.5 * Tu * bM;
            vec4 TvbM = 0.5 * Tv * bM;

            vec4 uInterp[4];
            for (int r = 0; r < 4; r++) {
                vec4 p0 = baseMeshPointColors[r * 4 + 0];
                vec4 p1 = baseMeshPointColors[r * 4 + 1];
                vec4 p2 = baseMeshPointColors[r * 4 + 2];
                vec4 p3 = baseMeshPointColors[r * 4 + 3];

                float4x4 G = float4x4(
                    vec4(p0.r, p1.r, p2.r, p3.r),
                    vec4(p0.g, p1.g, p2.g, p3.g),
                    vec4(p0.b, p1.b, p2.b, p3.b),
                    vec4(p0.a, p1.a, p2.a, p3.a)
                );
                uInterp[r] = TubM * G;
            }
            float4x4 G = float4x4(
                vec4(uInterp[0].r, uInterp[1].r, uInterp[2].r, uInterp[3].r),
                vec4(uInterp[0].g, uInterp[1].g, uInterp[2].g, uInterp[3].g),
                vec4(uInterp[0].b, uInterp[1].b, uInterp[2].b, uInterp[3].b),
                vec4(uInterp[0].a, uInterp[1].a, uInterp[2].a, uInterp[3].a)
            );
            vec4 interpolatedColor = TvbM * G;
            vec3 linearSrgbColor = oklab_to_linear_srgb(interpolatedColor.xyz);
            linearSrgbColor = clamp(linearSrgbColor, 0.0, 1.0);
            float alpha = clamp(interpolatedColor.a, 0.0, 1.0);
            color = vec4(linearSrgbColor * alpha, alpha);
        }
        return varyings.position;
    }
    """
