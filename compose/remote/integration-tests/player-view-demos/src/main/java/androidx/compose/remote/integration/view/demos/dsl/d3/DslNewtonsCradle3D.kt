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

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcImage
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPrimitive3D
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.Textures3D
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

private const val BG = 0xFF0D1018.toInt()
private const val BALL = 0xFFBFC6D2.toInt() // brushed steel
private const val STRING = 0xFF6A7280.toInt()

private const val BALL_R = 0.42f
private const val L = 2.3f // string length (pivot → ball center); balls hang clear of the base
private const val PIVOT_Y = 2.7f
private const val BASE_Y = -0.6f // base bar sits well below the resting balls (no clipping)
private const val TOPBAR_Y = PIVOT_Y + 0.25f
private const val SWING = 0.62f // max swing angle (radians, ~35°)
private const val NBALL = 5

/**
 * A **Newton's cradle**: five steel balls on bifilar strings. The two end balls take turns swinging
 * while the middle three hang dead still, recreating the momentum-transfer illusion. The trick is
 * that real collision physics isn't needed (or expressible) — a single shared driver `base =
 * sin(t)·SWING` is split with [min]/[max] so the **right** ball only ever swings out to the right
 * (`max(base,0)`) and the **left** ball only to the left (`min(base,0)`). At the instant `base`
 * crosses zero one ball arrives at vertical exactly as the other departs: the classic clack.
 *
 * Each ball is a pivot [androidx.compose.remote.integration.view.demos.dsl.translate] + a reactive
 * [rotate] about Z; the string and ball ride that one rotation. This is a strong fit for the API —
 * clean closed-form motion, no per-frame state.
 */
@Suppress("RestrictedApiAndroidX")
public fun newtonsCradle3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Newton's Cradle"),
    ) {
        val woodTex: RcImage = remoteBitmap(Textures3D.wood())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }

                // One shared pendulum driver. Period ≈ 2s (t·π advances π each second).
                val base = (continuousSeconds() * PI.toFloat()).sin() * SWING
                val swingLeft: RcFloat = base.min(0f) // ≤0: left ball only
                val swingRight: RcFloat = base - swingLeft // ≥0: right ball only (max(base,0))

                val spacing = 2f * BALL_R
                val x0 = -spacing * (NBALL - 1) / 2f
                // Drag to orbit the cradle (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    val ball = primitiveSphere(1, BALL_R, segments = 28f)
                    val string = primitiveCylinder(2, 0.02f, 0f, 0f, 0f, 0f, -L, 0f, segments = 8f)
                    // Frame parts (UV-mapped so the wood texture wraps each face).
                    val topBar =
                        primitiveCube(3, spacing * NBALL + 0.6f, 0.16f, 0.5f, uv = RcPrimitive3D.UV)
                    val postH = TOPBAR_Y - BASE_Y
                    val post = primitiveCube(4, 0.16f, postH, 0.16f, uv = RcPrimitive3D.UV)
                    val baseBar =
                        primitiveCube(5, spacing * NBALL + 1.0f, 0.18f, 1.7f, uv = RcPrimitive3D.UV)

                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        eye = vec(RcFloat(0.6f), RcFloat(1.7f), RcFloat(8.6f)),
                        center = vec(RcFloat(0f), RcFloat(0.95f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.5f), RcFloat(-0.75f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(0.2f), RcFloat(0.4f)),
                        color = 0xFF8FA6CC.toInt(),
                        intensity = RcFloat(0.5f),
                    )
                    // ---- Frame (matte wood texture) ----
                    material(0f) // matte
                    color(0xFFFFFFFF.toInt()) // white base so the texture shows untinted, lit
                    texture(woodTex)
                    orbitIdentity(view, tilt)
                    translate(0f, TOPBAR_Y, 0f)
                    drawMesh(topBar, mode = mode)
                    orbitIdentity(view, tilt)
                    translate(0f, BASE_Y, 0f)
                    drawMesh(baseBar, mode = mode)
                    val px = spacing * NBALL / 2f + 0.2f
                    val postCy = (TOPBAR_Y + BASE_Y) / 2f
                    orbitIdentity(view, tilt)
                    translate(-px, postCy, 0f)
                    drawMesh(post, mode = mode)
                    orbitIdentity(view, tilt)
                    translate(px, postCy, 0f)
                    drawMesh(post, mode = mode)
                    texture(null)

                    // ---- Balls (glossy steel) ----
                    material(specStrength = 0.55f, shininess = 36f)
                    for (i in 0 until NBALL) {
                        val pivotX = x0 + i * spacing
                        val angle: RcFloat? =
                            when (i) {
                                0 -> swingLeft
                                NBALL - 1 -> swingRight
                                else -> null // middle balls hang still
                            }
                        ball(ball, string, mode, pivotX, angle, view, tilt)
                    }
                }
            }
        }
    }
}

/**
 * One ball + string hanging from ([pivotX], [PIVOT_Y]); [angle] (about Z) null = hangs vertical.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.ball(
    ballMesh: RcMesh,
    stringMesh: RcMesh,
    mode: RcMeshMode,
    pivotX: Float,
    angle: RcFloat?,
    view: RcFloat,
    tilt: RcFloat,
) {
    orbitIdentity(view, tilt)
    translate(pivotX, PIVOT_Y, 0f)
    if (angle != null) {
        rotate(angle, 0f, 0f, 1f)
    }
    color(STRING)
    drawMesh(stringMesh, mode = mode)
    translate(0f, -L, 0f)
    color(BALL)
    drawMesh(ballMesh, mode = mode)
}

@Suppress("RestrictedApiAndroidX")
public fun newtonsCradle3DSoftware(): ByteArray = newtonsCradle3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun newtonsCradle3DCanvas(): ByteArray = newtonsCradle3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun NewtonsCradle3DPreview() {
    RemoteDocumentPreview(RemoteDocument(newtonsCradle3DSoftware()))
}
