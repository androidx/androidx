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
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPrimitive3D
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
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

/** Equirectangular Earth texture (NASA Blue Marble, public domain), 2048×1024 (2:1, lat×long). */
private const val EARTH_TEXTURE = "textures/earth_bluemarble.jpg"

/** Earth's axial tilt, ~23.4°, so the spin axis leans like the real planet. */
private const val AXIAL_TILT = 0.41f

/**
 * Decode the bundled Earth map (a Java classloader resource, so no Android `Context` is needed —
 * the demo suppliers are plain `() -> ByteArray`). The sphere's lat/long UV expects the equator
 * across the middle and the north pole at the top; the [flipV]/[flipH] knobs let the mapping be
 * corrected to the renderer's UV convention without re-exporting the asset. Falls back to a plain
 * blue marble if the resource is somehow missing.
 */
private fun earthBitmap(flipV: Boolean, flipH: Boolean): Bitmap {
    val cl = object {}.javaClass.classLoader
    val stream = cl?.getResourceAsStream(EARTH_TEXTURE)
    val src =
        if (stream != null) {
            stream.use { BitmapFactory.decodeStream(it) }
        } else {
            null
        }
    if (src == null) {
        // Fallback: a flat ocean-blue bitmap so the demo still renders something sensible.
        val bmp = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
        AndroidCanvas(bmp).drawColor(0xFF1B3A6B.toInt())
        return bmp
    }
    if (!flipV && !flipH) {
        return src
    }
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(out)
    val m =
        Matrix().apply {
            postScale(
                if (flipH) -1f else 1f,
                if (flipV) -1f else 1f,
                src.width / 2f,
                src.height / 2f,
            )
        }
    c.drawBitmap(src, m, AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG))
    return out
}

/**
 * A **spinning Earth**: the NASA Blue Marble equirectangular map wrapped onto a UV sphere
 * ([primitiveSphere] with [RcPrimitive3D.UV] = longitude × latitude) and rotated about a tilted
 * polar axis, lit by a single "sun" directional light, over a dark space background. The texture is
 * sampled through the sphere's lat/long UVs (per-pixel on the software backend, via a
 * `BitmapShader` on the canvas backend).
 */
@Suppress("RestrictedApiAndroidX")
public fun spinningGlobe3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        val tex = remoteBitmap(earthBitmap(flipV = false, flipH = false))
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF05070D.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val globe = primitiveSphere(1, 1.3f, segments = 96f, uv = RcPrimitive3D.UV)

                    perspectiveCameraDeg(
                        fovYDeg = 42f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.5f), RcFloat(4.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Sun from the upper front-left; faint cool fill so the night side isn't black.
                    directionalLight(
                        direction = vec(RcFloat(-0.6f), RcFloat(-0.35f), RcFloat(-0.7f)),
                        color = 0xFFFFF4E0.toInt(),
                        intensity = RcFloat(1.1f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(0.2f), RcFloat(0.5f)),
                        color = 0xFF24304A.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    texture(tex)
                    // Lean the spin axis (axial tilt), then spin about that tilted axis.
                    rotate(angleRad = AXIAL_TILT, ax = 0f, ay = 0f, az = 1f)
                    rotate(angleRad = time * 0.4f, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(globe, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun spinningGlobe3DSoftware(): ByteArray = spinningGlobe3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun spinningGlobe3DCanvas(): ByteArray = spinningGlobe3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SpinningGlobe3DPreview() {
    RemoteDocumentPreview(RemoteDocument(spinningGlobe3DSoftware()))
}
