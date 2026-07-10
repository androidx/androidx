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

package androidx.camera.core.processing.util

import androidx.camera.core.DynamicRange
import androidx.camera.core.processing.ShaderProvider
import androidx.core.util.Preconditions

/** Default shader providers for [GLUtils]. */
internal object ShaderProviders {
    @JvmField
    val DEFAULT_VERTEX_SHADER = createVertexShader(GLUtils.VAR_TEXTURE_COORD, isHdr = false)

    @JvmField val HDR_VERTEX_SHADER = createVertexShader(GLUtils.VAR_TEXTURE_COORD, isHdr = true)

    @JvmField
    val BLANK_VERTEX_SHADER =
        """
        uniform mat4 uTransMatrix;
        attribute vec4 aPosition;
        void main() {
            gl_Position = uTransMatrix * aPosition;
        }
        """
            .trimIndent()

    @JvmField
    val BLANK_FRAGMENT_SHADER =
        """
        precision mediump float;
        uniform float uAlphaScale;
        void main() {
            gl_FragColor = vec4(0.0, 0.0, 0.0, uAlphaScale);
        }
        """
            .trimIndent()

    @JvmStatic
    fun resolveDefaultShaderProvider(
        dynamicRange: DynamicRange,
        inputFormat: GLUtils.InputFormat,
        hasAdvancedStyling: Boolean,
    ): ShaderProvider {
        if (dynamicRange.is10BitHdr) {
            Preconditions.checkArgument(
                inputFormat != GLUtils.InputFormat.UNKNOWN,
                "No default sampler shader available for " + inputFormat,
            )
        }
        val isHdr = dynamicRange.is10BitHdr
        val isYuvHdr = (inputFormat == GLUtils.InputFormat.YUV && isHdr)
        return object : ShaderProvider {
            override fun createFragmentShader(
                samplerVarName: String,
                fragCoordsVarName: String,
            ): String {
                return createFragmentShaderInternal(
                    samplerVarName,
                    fragCoordsVarName,
                    isHdr = isHdr,
                    isYuvHdr = isYuvHdr,
                    hasAdvancedStyling,
                )
            }
        }
    }

    private fun createVertexShader(fragCoordsVarName: String, isHdr: Boolean): String {
        val version = if (isHdr) "#version 300 es" else ""
        val attr = if (isHdr) "in" else "attribute"
        val varying = if (isHdr) "out" else "varying"

        return """
            $version
            $attr vec4 aPosition;
            $attr vec4 aTextureCoord;
            uniform mat4 uTexMatrix;
            uniform mat4 uTransMatrix;
            $varying vec2 $fragCoordsVarName;
            $varying vec2 vPosition;
            void main() {
              gl_Position = uTransMatrix * aPosition;
              $fragCoordsVarName = (uTexMatrix * aTextureCoord).xy;
              vPosition = aPosition.xy;
            }
        """
            .trimIndent()
            .trim()
    }

    @JvmStatic
    fun createFragmentShaderInternal(
        samplerVarName: String,
        fragCoordsVarName: String,
        isHdr: Boolean,
        isYuvHdr: Boolean = false,
        hasAdvancedStyling: Boolean = false,
    ): String {
        val isGlEs30 = isHdr || isYuvHdr
        val outVarName = if (isGlEs30) "outColor" else "gl_FragColor"
        val textureFunc = if (isGlEs30) "texture" else "texture2D"

        val compositionUniforms =
            if (hasAdvancedStyling)
                """
                uniform float uCornerRadiusRatio;
                uniform float uAspectRatio;
                uniform float uBorderWidth;
                uniform vec4 uBorderColor;
                """
                    .trimIndent()
            else ""

        val roundCornerAndBorderLogic =
            if (hasAdvancedStyling)
                """
            vec2 abs_pos = abs(vPosition);
            float cornerDist = 0.0;
            if (uCornerRadiusRatio > 0.0) {
                // Calculate corner radius in NDC space
                vec2 rxry = vec2(uCornerRadiusRatio);
                if (uAspectRatio > 1.0) {
                    rxry.x /= uAspectRatio;
                } else {
                    rxry.y *= uAspectRatio;
                }

                // Calculate distance to the corner
                vec2 dist = max(abs_pos - (1.0 - rxry), 0.0);
                cornerDist = length(dist / rxry);

                // 1. Clip the outer edge
                if (cornerDist > 1.0) {
                    discard;
                    return;
                }
           }
           // 2. Draw the border
           if (uBorderWidth > 0.0) {
                // Straight edges border check
                vec2 bt = vec2(uBorderWidth);
                if (uAspectRatio > 1.0) {
                     bt.x /= uAspectRatio;
                } else {
                     bt.y *= uAspectRatio;
                }
                bool isStraightBorder = (abs_pos.x > (1.0 - bt.x)) || (abs_pos.y > (1.0 - bt.y));
                // Curved corner border check
                bool isCurvedBorder = false;
                if (uCornerRadiusRatio > 0.0) {
                     float borderThreshold = max(1.0 - (uBorderWidth / uCornerRadiusRatio), 0.0);
                     isCurvedBorder = cornerDist > borderThreshold;
                }
                if (isStraightBorder || isCurvedBorder) {
                     $outVarName = uBorderColor;
                     return;
                }
           }
        """
                    .trimIndent()
            else ""

        if (isYuvHdr) {
            // TODO(b/502166517): This yuvToRgb matrix assumes a specific YUV color space (BT.709).
            //  The matrix should be selected based on the actual input color space, similar to
            //  how it's done in Transformer.
            return """ 
                #version 300 es
                #extension GL_EXT_YUV_target : require
                precision mediump float;
                uniform __samplerExternal2DY2YEXT $samplerVarName;
                uniform float uAlphaScale;
                $compositionUniforms
                in vec2 $fragCoordsVarName;
                in vec2 vPosition;
                out vec4 outColor;

                vec3 yuvToRgb(vec3 yuv) {
                  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
                  const mat3 yuvToRgbColorMat = mat3(
                    1.1689f, 1.1689f, 1.1689f,
                    0.0000f, -0.1881f, 2.1502f,
                    1.6853f, -0.6530f, 0.0000f
                  );
                  return clamp(yuvToRgbColorMat * (yuv - yuvOffset), 0.0, 1.0);
                }

                void main() {
                  $roundCornerAndBorderLogic
                  vec3 srcYuv = texture($samplerVarName, $fragCoordsVarName).xyz;
                  vec3 srcRgb = yuvToRgb(srcYuv);
                  outColor = vec4(srcRgb, uAlphaScale);
                }
            """
                .trimIndent()
                .trim()
        }

        val version = if (isGlEs30) "#version 300 es" else ""
        val extension =
            if (isGlEs30) {
                "#extension GL_OES_EGL_image_external_essl3 : require"
            } else {
                "#extension GL_OES_EGL_image_external : require"
            }
        val varying = if (isGlEs30) "in" else "varying"
        val outVarDeclaration = if (isGlEs30) "out vec4 outColor;" else ""

        return """
            $version
            $extension
            precision mediump float;
            uniform samplerExternalOES $samplerVarName;
            uniform float uAlphaScale;
            $compositionUniforms
            $varying vec2 $fragCoordsVarName;
            $varying vec2 vPosition;
            $outVarDeclaration
            void main() {
                $roundCornerAndBorderLogic
                vec4 src = $textureFunc($samplerVarName, $fragCoordsVarName);
                $outVarName = vec4(src.rgb, src.a * uAlphaScale);
            }
        """
            .trimIndent()
            .trim()
    }
}
