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
@file:SuppressLint("PrimitiveInCollection")

package androidx.compose.remote.integration.view.demos.dsl

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPrimitive3D
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A UV test card: 4 colored quadrants + grid + a big "F" so flips/rotations of the UV are visible.
 */
private fun uvTextureBitmap(): Bitmap {
    val s = 256
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val fill = AndroidPaint()
    fill.color = 0xFFE0492E.toInt() // top-left red
    c.drawRect(0f, 0f, s / 2f, s / 2f, fill)
    fill.color = 0xFF3FAE4A.toInt() // top-right green
    c.drawRect(s / 2f, 0f, s.toFloat(), s / 2f, fill)
    fill.color = 0xFF3D7FE0.toInt() // bottom-left blue
    c.drawRect(0f, s / 2f, s / 2f, s.toFloat(), fill)
    fill.color = 0xFFE8C72E.toInt() // bottom-right yellow
    c.drawRect(s / 2f, s / 2f, s.toFloat(), s.toFloat(), fill)
    val grid =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0x66000000
            strokeWidth = 2f
            style = AndroidPaint.Style.STROKE
        }
    for (k in 1 until 8) {
        val t = s * k / 8f
        c.drawLine(t, 0f, t, s.toFloat(), grid)
        c.drawLine(0f, t, s.toFloat(), t, grid)
    }
    val text =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 150f
            isFakeBoldText = true
        }
    c.drawText("F", 80f, 185f, text)
    return bmp
}

/**
 * Verifies UV generation on parametric primitives ([RcPrimitive3D.UV]). A textured plane, sphere
 * (longitude/latitude globe), cube and torus, all spun, sampling the same UV test card — so the
 * texture sticking to each surface confirms the generated UVs.
 */
@Suppress("RestrictedApiAndroidX")
public fun texturedPrimitives3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val spin = continuousSeconds() * 0.4f
        val tex = remoteBitmap(uvTextureBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF12151B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val plane = primitivePlane(1, 1.0f, 1.0f, uv = RcPrimitive3D.UV)
                    val sphere = primitiveSphere(2, 0.55f, segments = 32f, uv = RcPrimitive3D.UV)
                    val cube = primitiveCube(3, 0.85f, 0.85f, 0.85f, uv = RcPrimitive3D.UV)
                    val torus =
                        primitiveTorus(4, 0.45f, 0.2f, segments = 36f, uv = RcPrimitive3D.UV)
                    val meshes: Array<RcMesh> = arrayOf(plane, sphere, cube, torus)

                    perspectiveCameraDeg(
                        fovYDeg = 34f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(1.0f), RcFloat(0.9f), RcFloat(7.2f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.3f), RcFloat(-0.5f), RcFloat(-0.8f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.85f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(0.1f), RcFloat(0.4f)),
                        color = 0xFFB0C0E0.toInt(),
                        intensity = RcFloat(0.45f),
                    )

                    color(0xFFFFFFFF.toInt())
                    texture(tex)
                    val gx = floatArrayOf(-1.65f, -0.55f, 0.55f, 1.65f)
                    for (i in 0 until 4) {
                        identity()
                        translate(gx[i], 0f, 0f)
                        rotate(angleRad = spin, ax = 0.2f, ay = 1f, az = 0f)
                        drawMesh(meshes[i], mode = mode)
                    }
                    texture(null)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun texturedPrimitives3DSoftware(): ByteArray =
    texturedPrimitives3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun TexturedPrimitives3DPreview() {
    RemoteDocumentPreview(RemoteDocument(texturedPrimitives3DSoftware()))
}
