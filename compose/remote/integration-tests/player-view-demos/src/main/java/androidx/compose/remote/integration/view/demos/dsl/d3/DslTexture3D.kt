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
@file:OptIn(ExperimentalRemoteCreationApi::class)
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPrimitive3D
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.expressionGeneral
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** A 512×256 colorful "Hello Texture" bitmap, drawn with the Android 2D Canvas. */
private fun helloTextureBitmap(): Bitmap {
    val w = 512
    val h = 256
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val grad =
        LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                0xFFFF2D6B.toInt(),
                0xFFFFC400.toInt(),
                0xFF14E0C0.toInt(),
                0xFF7A3CFF.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), AndroidPaint().apply { shader = grad })
    c.drawRect(
        7f,
        7f,
        w - 7f,
        h - 7f,
        AndroidPaint().apply {
            style = AndroidPaint.Style.STROKE
            strokeWidth = 12f
            color = 0xFFFFFFFF.toInt()
        },
    )
    val tp =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 78f
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 5f, 0xAA000000.toInt())
        }
    c.drawText("Hello", w / 2f, h / 2f - 6f, tp)
    c.drawText("Texture", w / 2f, h / 2f + 74f, tp)
    return bmp
}

/**
 * Texture demo: a "Hello Texture" bitmap mapped onto a waving flag. The flag is an
 * [expressionGeneral] surface (a plane in XY whose Z ripples with `time`), so it carries UV;
 * `texture(...)` binds the uploaded bitmap, and the renderer samples it through the UVs — on the
 * canvas backend via a `BitmapShader` on the `drawVertices` paint, on the software backend via
 * per-pixel sampling. The white paint base keeps the texture untinted (modulated only by the
 * Lambert lighting).
 */
@Suppress("RestrictedApiAndroidX")
public fun texture3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        val tex = remoteBitmap(helloTextureBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    // Plane in XY (faces +Z toward the camera) with a travelling ripple in Z.
                    val flag =
                        expressionGeneral(
                            id = 1,
                            uMin = -1.4f,
                            uMax = 1.4f,
                            vMin = -0.7f,
                            vMax = 0.7f,
                            uSeg = 60,
                            vSeg = 32,
                            flip = true,
                            x = { u, _ -> u },
                            y = { _, v -> v },
                            z = { u, v -> (((u * 3.5f) + (v * 1.5f) + time).sin()) * 0.16f },
                        )

                    perspectiveCameraDeg(
                        fovYDeg = 42f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0.3f), RcFloat(0.5f), RcFloat(3.0f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.5f), RcFloat(-1f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )

                    texture(tex)
                    drawMesh(flag, mode = mode)
                }
            }
        }
    }
}

/**
 * Perspective-correctness test: a **single quad** (uSeg=vSeg=1 → 2 triangles), laid flat as a floor
 * and viewed at a grazing angle so it recedes strongly. With affine UV the texture warps and kinks
 * along the quad's diagonal; with perspective-correct sampling the "Hello Texture" text and the
 * rectangular border stay straight all the way into the distance.
 */
@Suppress("RestrictedApiAndroidX")
public fun textureFloor3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val tex = remoteBitmap(helloTextureBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    // Reverse only the v range so the default (fu,fv) UV maps the texture
                    // upright (and not mirrored) from this camera; reversing one axis flips the
                    // winding, so flip=true restores front-facing.
                    val floor =
                        expressionGeneral(
                            id = 1,
                            uMin = -1.6f,
                            uMax = 1.6f,
                            vMin = 0.8f,
                            vMax = -2.4f,
                            uSeg = 16,
                            vSeg = 16,
                            flip = true,
                            x = { u, _ -> u },
                            y = { _, _ -> RcFloat(0f) },
                            z = { _, v -> v },
                        )

                    perspectiveCameraDeg(
                        fovYDeg = 50f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(1.8f), RcFloat(2.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(-0.6f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.3f), RcFloat(-1f), RcFloat(-0.3f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    texture(tex)
                    drawMesh(floor, mode = mode)
                }
            }
        }
    }
}

/**
 * Textured **globe**: the "Hello Texture" bitmap wrapped onto a static [defineSphereMesh] — which
 * now carries longitude/latitude UV — and spun about its axis. Shows that the static primitives
 * (not just MeshExpression3D surfaces) can be textured.
 */
@Suppress("RestrictedApiAndroidX")
public fun textureGlobe3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        val tex = remoteBitmap(helloTextureBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val globe = primitiveSphere(1, 1.25f, segments = 96f, uv = RcPrimitive3D.UV)

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.4f), RcFloat(4.2f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.4f), RcFloat(-1f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )

                    texture(tex)
                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(globe, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun textureGlobe3DSoftware(): ByteArray = textureGlobe3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun textureGlobe3DCanvas(): ByteArray = textureGlobe3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
public fun texture3DSoftware(): ByteArray = texture3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun texture3DCanvas(): ByteArray = texture3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Texture3DPreview() {
    RemoteDocumentPreview(RemoteDocument(texture3DCanvas()))
}
