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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Helper to create a stylized 512x512 texturized bitmap face for the cubosphere calibration test
 * pattern.
 */
private fun createFaceBitmap(text: String, color: Int, textColor: Int): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)

    val paint =
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            this.color = textColor
            textSize = 64f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

    // Draw card outline border
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 16f
    canvas.drawRect(12f, 12f, size - 12f, size - 12f, paint)

    // Draw card diagonal cross grid
    paint.strokeWidth = 4f
    canvas.drawLine(12f, 12f, size - 12f, size - 12f, paint)
    canvas.drawLine(12f, size - 12f, size - 12f, 12f, paint)

    // Draw text label
    paint.style = Paint.Style.FILL
    canvas.drawText(text, size / 2f, size / 2f + 20f, paint)

    return bitmap
}

val CUBESPHERE_SHADER_SRC =
    """
    uniform shader img0;
    uniform shader img1;
    uniform shader img2;
    uniform shader img3;
    uniform shader img4;
    uniform shader img5;

    uniform float2 iResolution;
    uniform float iTime;

    float sdBox(vec3 p, vec3 b) {
        vec3 q = abs(p) - b;
        return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
    }

    float sdSphere(vec3 p, float r) {
        return length(p) - r;
    }

    float sdMorph(vec3 p, float progress) {
        float r = 0.82;
        float d_cube = sdBox(p, vec3(r));
        float d_sphere = sdSphere(p, r);
        return mix(d_cube, d_sphere, progress);
    }

    vec3 getNormal(vec3 p, float progress) {
        vec2 e = vec2(0.001, 0.0);
        return normalize(vec3(
            sdMorph(p + e.xyy, progress) - sdMorph(p - e.xyy, progress),
            sdMorph(p + e.yxy, progress) - sdMorph(p - e.yxy, progress),
            sdMorph(p + e.yyx, progress) - sdMorph(p - e.yyx, progress)
        ));
    }

    half4 main(vec2 fragcoord) {
        // Center coordinate viewport space mapping in [-1, 1]
        vec2 uv = (fragcoord.xy - 0.5 * iResolution.xy) / iResolution.y;

        // Ray vectors
        vec3 ro = vec3(0.0, 0.0, -2.5);
        vec3 rd = normalize(vec3(uv, 1.0));

        // Self-animating morphing factor (smooth transition between cube and sphere)
        float uProgress = abs(sin(iTime * 0.5));

        // Auto-rotates target shapes dynamically to reveal all six faces
        float angleY = iTime * 0.45;
        float angleX = iTime * 0.25;

        // Rotate ray around Y axis
        float cosY = cos(angleY);
        float sinY = sin(angleY);
        ro = vec3(ro.x * cosY - ro.z * sinY, ro.y, ro.x * sinY + ro.z * cosY);
        rd = vec3(rd.x * cosY - rd.z * sinY, rd.y, rd.x * sinY + rd.z * cosY);

        // Rotate ray around X axis
        float cosX = cos(angleX);
        float sinX = sin(angleX);
        ro = vec3(ro.x, ro.y * cosX - ro.z * sinX, ro.y * sinX + ro.z * cosX);
        rd = vec3(rd.x, rd.y * cosX - rd.z * sinX, rd.y * sinX + rd.z * cosX);

        // Raymarching sphere tracing loop
        float t = 0.0;
        float d = 0.0;
        vec3 p;
        bool hit = false;

        for (int i = 0; i < 50; i++) {
            p = ro + t * rd;
            d = sdMorph(p, uProgress);
            if (d < 0.001) {
                hit = true;
                break;
            }
            if (t > 5.5) break;
            t += d;
        }

        vec3 color = vec3(0.0);

        if (hit) {
            vec3 N = getNormal(p, uProgress);

            // Cubemap texture coordinates projection
            vec3 dTex = normalize(p);
            vec3 absD = abs(dTex);
            float maxVal = max(absD.x, max(absD.y, absD.z));

            vec3 dir = dTex / maxVal;
            vec2 uvTex = vec2(0.0);
            vec4 texColor = vec4(0.0);

            // Sample texture face depending on maximum coordinate projection
            if (absD.x == maxVal) {
                if (dTex.x > 0.0) {
                    uvTex = vec2(0.5 - dir.z * 0.5, 0.5 - dir.y * 0.5);
                    texColor = img0.eval(uvTex * 512.0);
                } else {
                    uvTex = vec2(0.5 + dir.z * 0.5, 0.5 - dir.y * 0.5);
                    texColor = img1.eval(uvTex * 512.0);
                }
            } else if (absD.y == maxVal) {
                if (dTex.y > 0.0) {
                    uvTex = vec2(0.5 + dir.x * 0.5, 0.5 + dir.z * 0.5);
                    texColor = img2.eval(uvTex * 512.0);
                } else {
                    uvTex = vec2(0.5 + dir.x * 0.5, 0.5 - dir.z * 0.5);
                    texColor = img3.eval(uvTex * 512.0);
                }
            } else {
                if (dTex.z > 0.0) {
                    uvTex = vec2(0.5 + dir.x * 0.5, 0.5 - dir.y * 0.5);
                    texColor = img4.eval(uvTex * 512.0);
                } else {
                    uvTex = vec2(0.5 - dir.x * 0.5, 0.5 - dir.y * 0.5);
                    texColor = img5.eval(uvTex * 512.0);
                }
            }

            // Studio lighting pipeline (diffuse + specular + edge fresnel)
            vec3 L = normalize(vec3(1.0, 1.2, -1.5));
            float diff = max(0.0, dot(N, L));

            vec3 V = -rd;
            vec3 H = normalize(L + V);
            float spec = pow(max(0.0, dot(N, H)), 32.0);

            float fresnel = pow(1.0 - max(0.0, dot(N, V)), 4.0);

            color = texColor.rgb * (diff * 0.85 + 0.25) + vec3(0.45) * spec + vec3(0.0, 0.55, 0.55) * fresnel * 0.25;
        } else {
            // Cosmic dark background gradient
            vec2 bgUv = fragcoord.xy / iResolution.xy;
            color = mix(vec3(0.01, 0.01, 0.03), vec3(0.04, 0.06, 0.15), bgUv.y);
        }

        return vec4(color, 1.0);
    }
    """
        .trimIndent()
        .removeLineComments()

@Suppress("RestrictedApiAndroidX")
fun createCubeSphereTransitionShader(): RemoteComposeContext {
    val tw = 800f
    val th = 800f

    return RemoteComposeContextAndroid(
        800,
        800,
        "CubeSphereTransitionShader",
        apiLevel = 7,
        profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        platform = AndroidxRcPlatformServices(),
    ) {
        val w = windowWidth()
        val h = windowHeight()
        // Generate 6 unique texturized square faces representing a Calibration Test Pattern
        val face0 = createFaceBitmap("RIGHT (+X)", Color.rgb(225, 29, 72), Color.WHITE)
        val face1 = createFaceBitmap("LEFT (-X)", Color.rgb(5, 150, 105), Color.WHITE)
        val face2 = createFaceBitmap("TOP (+Y)", Color.rgb(37, 99, 235), Color.WHITE)
        val face3 = createFaceBitmap("BOTTOM (-Y)", Color.rgb(217, 119, 6), Color.WHITE)
        val face4 = createFaceBitmap("FRONT (+Z)", Color.rgb(217, 70, 239), Color.WHITE)
        val face5 = createFaceBitmap("BACK (-Z)", Color.rgb(6, 182, 212), Color.WHITE)

        val bId0 = addBitmap(face0)
        val bId1 = addBitmap(face1)
        val bId2 = addBitmap(face2)
        val bId3 = addBitmap(face3)
        val bId4 = addBitmap(face4)
        val bId5 = addBitmap(face5)

        val id =
            createShader(CUBESPHERE_SHADER_SRC)
                .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                .setFloatUniform("iResolution", w.toFloat(), h.toFloat())
                .setBitmapUniform("img0", bId0)
                .setBitmapUniform("img1", bId1)
                .setBitmapUniform("img2", bId2)
                .setBitmapUniform("img3", bId3)
                .setBitmapUniform("img4", bId4)
                .setBitmapUniform("img5", bId5)
                .commit()

        painter.setShader(id).commit()
        drawRect(0f, 0f, w, h)
    }
}

private fun String.removeLineComments(): String {
    // Matches "//" and everything that follows it to the end of the line
    val shaderCode = this
    val commentRegex = """//.*""".toRegex()

    return shaderCode.lines().map { line -> line.replace(commentRegex, "") }.joinToString("\n")
}

@Preview
@Composable
private fun CubeSphereTransitionShaderPreview() =
    RemoteDocumentPreview(createCubeSphereTransitionShader())
