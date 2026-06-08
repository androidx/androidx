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
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCone
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveExtrudedArc
import androidx.compose.remote.creation.dsl.primitiveExtrudedCircle
import androidx.compose.remote.creation.dsl.primitiveExtrudedPath
import androidx.compose.remote.creation.dsl.primitiveExtrudedRoundedRect
import androidx.compose.remote.creation.dsl.primitiveExtrudedSegment
import androidx.compose.remote.creation.dsl.primitiveExtrudedSquircle
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.primitiveProfileTube
import androidx.compose.remote.creation.dsl.primitiveRoundedCube
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveSphericalDome
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.primitiveTube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val PI = Math.PI.toFloat()
private const val HALF_PI = (Math.PI / 2.0).toFloat()

// ---- Palette ---------------------------------------------------------------------------------
private const val BEIGE = 0xFFD8C49E.toInt() // knit oatmeal sock
private const val BEIGE_DARK = 0xFFB29A72.toInt() // lips / shaded knit
private const val HAIR = 0xFF1B2AE0.toInt() // vivid blue
private const val EYE_WHITE = 0xFFFAFAFA.toInt()
private const val EYE_BLACK = 0xFF0C0C12.toInt()
private const val MOUTH_DARK = 0xFF4A0810.toInt() // dark-red roof/floor of the mouth
private const val TONGUE = 0xFFD63A52.toInt()
private const val SHIRT = 0xFFFFFFFF.toInt()
private const val TIE = 0xFF0B0B0E.toInt()
private const val BG_DISK = 0xFF0A1A4A.toInt() // dark blue vignette
private const val NEON = 0xFF6C86FF.toInt() // glowing ring
private const val NEON2 = 0xFF9DB0FF.toInt()
private const val BACKDROP = 0xFF04050A.toInt()

// ---- Head geometry ---------------------------------------------------------------------------
private const val R = 1.0f // head (dome) radius
private const val SNX = 0.95f // snout scale X (width)
private const val SNY = 0.92f // snout scale Y (height)
private const val SNZ = 1.70f // snout scale Z (length, toward camera)
private const val HEAD_Y = 0.55f // head height in the world
private const val HEAD_PITCH = -0.12f // slight chin-forward tilt so the open mouth reads
private const val HINGE_Z = -1.45f // mouth hinges at the back of the snout

// Deterministic 0..1 pseudo-random for hair jitter (no Math.random → stable doc each build).
private fun hash(i: Int): Float {
    val x = Math.sin(i * 12.9898).toFloat() * 43758.547f
    return x - Math.floor(x.toDouble()).toFloat()
}

/**
 * A **sock-puppet character built entirely from the 3D primitive library**, with a talking mouth.
 *
 * The head is two scaled [primitiveSphericalDome] hemispheres — an upper and a lower jaw — that
 * hinge apart at the back of the snout; a **touch-driven** angle ([verticalOpenAngle], vertical
 * drag) opens and closes them, while horizontal drag ([verticalSpinAngle]) turns the whole puppet
 * on a turntable. Opening reveals the dark-red roof and floor of the mouth and a flattened,
 * elongated tongue — three [primitiveSphere] pads attached to (and hinging with) the two jaws. Eyes
 * are [primitiveSphere]s, the spiky blue hair is a fan of [primitiveCone]s plus curvy
 * [primitiveTube] strands, the knit body is a tapered [primitiveProfileTube], and the shirt/tie use
 * [primitiveTorus], [primitiveExtrudedRoundedRect], [primitiveRoundedCube], [primitiveCube] and an
 * [primitiveExtrudedPath] tie blade. The glowing backdrop ring is a [primitiveTorus] /
 * [primitiveExtrudedArc] over an [primitiveExtrudedCircle] disk and a [primitivePlane] backdrop.
 * Lips are [primitiveExtrudedSquircle]s, brows [primitiveExtrudedSegment]s, the neck a
 * [primitiveCylinder].
 */
@Suppress("RestrictedApiAndroidX")
public fun sockPuppet3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1080),
        RcHeaderTag.docHeight(1080),
        RcHeaderTag.contentDescription("3D Sock Puppet"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BACKDROP)) {
                paint { color(BEIGE) }
                // Interactive: horizontal drag spins the whole puppet (turntable); vertical drag
                // opens the mouth. `verticalSpinAngle`/`verticalOpenAngle` are RcCanvasScope
                // helpers,
                // so they must be called here (inside Canvas), not at the createRcBuffer level. The
                // small constant keeps the lips slightly apart at rest so the jaw faces don't
                // z-fight.
                val spinY = verticalSpinAngle()
                val openA =
                    verticalOpenAngle(radiansPerPixel = 0.0016f, maxAngleRad = 0.5f) + 0.035f
                scene3D {
                    // ---- Define every mesh once (drawn one or more times below) ----
                    val backdrop = primitivePlane(1, 16f, 16f)
                    val bgDisk = primitiveExtrudedCircle(2, 4.3f, 0.15f)
                    val neonRing = primitiveTorus(3, 3.55f, 0.05f)
                    val accentRing = primitiveExtrudedArc(4, 3.62f, 3.74f, 0f, 2f * PI, 0.04f)

                    val body =
                        primitiveProfileTube(
                            5,
                            floatArrayOf(
                                0f,
                                -0.30f,
                                0f,
                                0f,
                                -1.0f,
                                0f,
                                0f,
                                -2.0f,
                                0f,
                                0f,
                                -3.1f,
                                0f,
                            ),
                            floatArrayOf(0.50f, 0.62f, 0.92f, 1.05f),
                            capped = true,
                        )
                    val neck = primitiveCylinder(6, 0.52f, 0f, -0.15f, 0f, 0f, -0.85f, 0f)
                    val collar = primitiveTorus(7, 0.60f, 0.15f)
                    val placket = primitiveExtrudedRoundedRect(8, 0.46f, 1.5f, 0.08f, 0.05f)
                    val knot = primitiveRoundedCube(9, 0.26f, 0.28f, 0.16f, 0.05f)
                    val tie =
                        primitiveExtrudedPath(
                            10,
                            listOf(
                                floatArrayOf(
                                    -0.07f,
                                    0f,
                                    0.07f,
                                    0f,
                                    0.18f,
                                    -1.05f,
                                    0f,
                                    -1.28f,
                                    -0.18f,
                                    -1.05f,
                                )
                            ),
                            0.06f,
                        )
                    val button = primitiveCube(11, 0.07f, 0.07f, 0.05f)

                    val jaw = primitiveSphericalDome(12, R, HALF_PI, segments = 44f)
                    val eyeWhite = primitiveSphere(13, 0.40f, segments = 28f)
                    val pupil = primitiveSphere(14, 0.17f, segments = 20f)
                    val highlight = primitiveSphere(15, 0.055f, segments = 10f)
                    val cone = primitiveCone(16, 0.14f, 0.62f, segments = 10f)
                    val strand =
                        primitiveTube(
                            17,
                            0.05f,
                            floatArrayOf(0f, 0.8f, -0.6f, 0.15f, 1.2f, -0.7f, 0.05f, 1.7f, -0.55f),
                            capped = false,
                        )
                    val tuft =
                        primitiveTube(
                            18,
                            0.07f,
                            floatArrayOf(0f, 0.85f, 0.0f, -0.1f, 1.25f, 0.05f, 0.0f, 1.6f, 0.2f),
                            capped = true, // CAP_TUBE
                        )
                    val lip = primitiveExtrudedSquircle(19, 0.55f, 3.5f, 0.06f)
                    val brow = primitiveExtrudedSegment(20, 0.30f, PI + 0.5f, PI - 1.0f, 0.04f)
                    // One unit sphere reused (flattened by per-draw scale) as the three mouth pads:
                    // dark-red roof, dark-red floor, and the tongue.
                    val mouthPad = primitiveSphere(21, 1f, segments = 30f)

                    // ---- Camera ----
                    perspectiveCameraDeg(
                        fovYDeg = 32f,
                        aspect = 1f,
                        // Tight near/far around the scene so the z-buffer keeps the mouth seam
                        // crisp.
                        near = 3f,
                        far = 20f,
                        // 3/4 view (from front-right, slightly above) so the elongated snout, the
                        // mouth line and the eyes on top all read — like the reference photo.
                        eye = vec(RcFloat(3.0f), RcFloat(2.0f), RcFloat(8.0f)),
                        center = vec(RcFloat(0f), RcFloat(0.0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.5f), RcFloat(-0.7f), RcFloat(-0.5f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.8f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(-0.1f), RcFloat(0.5f)),
                        color = 0xFFAEC2FF.toInt(),
                        intensity = RcFloat(0.25f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.2f), RcFloat(0.3f), RcFloat(1.0f)),
                        color = 0xFF3048FF.toInt(),
                        intensity = RcFloat(0.35f),
                    )

                    // =========================== Background ===========================
                    color(BACKDROP)
                    identity()
                    translate(0f, 0f, -4.2f)
                    drawMesh(backdrop, mode = mode)

                    color(BG_DISK)
                    identity()
                    translate(0f, 0.2f, -3.6f)
                    drawMesh(bgDisk, mode = mode)

                    color(NEON)
                    identity()
                    translate(0f, 0.2f, -3.0f)
                    rotate(angleRad = HALF_PI, ax = 1f, ay = 0f, az = 0f) // face the camera
                    drawMesh(neonRing, mode = mode)

                    color(NEON2)
                    identity()
                    translate(0f, 0.2f, -2.9f)
                    drawMesh(accentRing, mode = mode)

                    // The whole puppet (body + head) spins on the vertical axis with horizontal
                    // drag;
                    // the background frame stays put. `spin` replaces `identity()` for puppet
                    // parts.
                    val spin = {
                        identity()
                        rotate(angleRad = spinY, ax = 0f, ay = 1f, az = 0f)
                    }

                    // =========================== Body / clothing ===========================
                    color(BEIGE)
                    spin()
                    drawMesh(body, mode = mode)

                    color(BEIGE)
                    spin()
                    drawMesh(neck, mode = mode)

                    color(SHIRT)
                    spin()
                    translate(0f, -1.05f, 0.58f)
                    drawMesh(placket, mode = mode)

                    color(SHIRT)
                    spin()
                    translate(0f, -0.55f, 0.18f)
                    drawMesh(collar, mode = mode)

                    color(TIE)
                    spin()
                    translate(0f, -0.80f, 0.66f)
                    drawMesh(knot, mode = mode)

                    color(TIE)
                    spin()
                    translate(0f, -0.92f, 0.63f)
                    drawMesh(tie, mode = mode)

                    color(EYE_BLACK)
                    for (by in floatArrayOf(-1.15f, -1.55f)) {
                        spin()
                        translate(0f, by, 0.66f)
                        drawMesh(button, mode = mode)
                    }

                    // =========================== Head transform helpers
                    // ===========================
                    // Each helper resets to identity, applies the turntable spin, places the head
                    // in
                    // the world, then (for the jaws) hinges about the back of the snout by ±openA.
                    // Call before each part, then post-multiply the part's own local placement.
                    val headBase = {
                        identity()
                        rotate(angleRad = spinY, ax = 0f, ay = 1f, az = 0f)
                        translate(0f, HEAD_Y, 0f)
                        rotate(angleRad = HEAD_PITCH, ax = 1f, ay = 0f, az = 0f)
                    }
                    val upper = {
                        headBase()
                        translate(0f, 0f, HINGE_Z)
                        rotate(angleRad = -openA, ax = 1f, ay = 0f, az = 0f)
                        translate(0f, 0f, -HINGE_Z)
                    }
                    val lower = {
                        headBase()
                        translate(0f, 0f, HINGE_Z)
                        rotate(angleRad = openA, ax = 1f, ay = 0f, az = 0f)
                        translate(0f, 0f, -HINGE_Z)
                    }

                    // =========================== Lower jaw ===========================
                    color(BEIGE)
                    lower()
                    scale(SNX, SNY, SNZ)
                    rotate(
                        angleRad = PI,
                        ax = 0f,
                        ay = 0f,
                        az = 1f,
                    ) // flip dome: round down, flat up
                    drawMesh(jaw, mode = mode)

                    // Dark-red floor of the mouth (a flattened sphere just inside the lower jaw,
                    // hinging with it).
                    color(MOUTH_DARK)
                    lower()
                    translate(0f, 0.05f, 0.15f)
                    scale(0.82f, 0.12f, 1.45f)
                    drawMesh(mouthPad, mode = mode)

                    // Tongue: a flattened, elongated sphere resting on the floor.
                    color(TONGUE)
                    lower()
                    translate(0f, 0.13f, 0.5f)
                    scale(0.34f, 0.11f, 0.74f)
                    drawMesh(mouthPad, mode = mode)

                    color(BEIGE_DARK)
                    lower()
                    translate(0f, -0.02f, 1.18f)
                    scale(1.15f, 0.28f, 1f)
                    drawMesh(lip, mode = mode)

                    // =========================== Upper jaw ===========================
                    color(BEIGE)
                    upper()
                    scale(SNX, SNY, SNZ)
                    drawMesh(jaw, mode = mode)

                    // Dark-red roof of the mouth (a flattened sphere just inside the upper jaw,
                    // hinging with it).
                    color(MOUTH_DARK)
                    upper()
                    translate(0f, -0.05f, 0.15f)
                    scale(0.82f, 0.12f, 1.45f)
                    drawMesh(mouthPad, mode = mode)

                    color(BEIGE_DARK)
                    upper()
                    translate(0f, 0.02f, 1.18f)
                    scale(1.15f, 0.28f, 1f)
                    drawMesh(lip, mode = mode)

                    // Eyes (white + pupil + highlight), attached to the upper jaw so they tilt with
                    // it.
                    for (sx in floatArrayOf(-1f, 1f)) {
                        color(EYE_WHITE)
                        upper()
                        translate(sx * 0.42f, 0.62f, 0.55f)
                        drawMesh(eyeWhite, mode = mode)

                        color(EYE_BLACK)
                        upper()
                        translate(sx * 0.42f, 0.58f, 0.90f)
                        drawMesh(pupil, mode = mode)

                        color(EYE_WHITE)
                        upper()
                        translate(sx * 0.42f + 0.06f, 0.66f, 1.02f)
                        drawMesh(highlight, mode = mode)

                        // Brow just above the eye.
                        color(BEIGE_DARK)
                        upper()
                        translate(sx * 0.42f, 1.02f, 0.55f)
                        drawMesh(brow, mode = mode)
                    }

                    // Hair: a full wild mop of cones spread over the top and sides of the upper
                    // jaw,
                    // each placed on the snout surface and tilted outward along its normal, with
                    // per-spike jitter so it looks tousled rather than gridded.
                    color(HAIR)
                    val rows = 7 // along the snout (z)
                    val cols = 9 // around the top (lateral angle)
                    var hairId = 0
                    for (iz in 0 until rows) {
                        // Cover the back of the head up to just behind the eyes (z < ~0.35).
                        val zf = -1.30f + iz * (1.65f / (rows - 1))
                        for (ic in 0 until cols) {
                            hairId++
                            // Lateral angle from the top: ±~75°, so hair reaches down the sides.
                            val phi = -1.30f + ic * (2.60f / (cols - 1))
                            val jx = (hash(hairId) - 0.5f) * 0.18f
                            val jz = (hash(hairId * 3 + 1) - 0.5f) * 0.20f
                            val jp = (hash(hairId * 7 + 2) - 0.5f) * 0.35f
                            val lenJ = 0.8f + hash(hairId * 5 + 3) * 0.9f
                            val a = phi + jp
                            val x = SNX * 0.93f * Math.sin(a.toDouble()).toFloat() + jx
                            val y = SNY * 0.97f * Math.cos(a.toDouble()).toFloat()
                            upper()
                            translate(x, y, zf + jz)
                            rotate(angleRad = a, ax = 0f, ay = 0f, az = 1f) // splay outward
                            rotate(angleRad = -0.5f + jp, ax = 1f, ay = 0f, az = 0f) // lean back
                            scale(0.9f, lenJ, 0.9f)
                            drawMesh(cone, mode = mode)
                        }
                    }
                    // A few long, curvy strands (TUBE + capped CAP_TUBE) for stray wild hairs.
                    for (sx in floatArrayOf(-1f, 1f)) {
                        upper()
                        translate(sx * 0.2f, 0f, -0.4f)
                        scale(sx, 1f, 1f)
                        drawMesh(strand, mode = mode)
                        upper()
                        translate(sx * 0.35f, 0f, -0.9f)
                        scale(sx, 1f, 1f)
                        drawMesh(tuft, mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun sockPuppet3DSoftware(): ByteArray = sockPuppet3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SockPuppet3DPreview() {
    RemoteDocumentPreview(RemoteDocument(sockPuppet3DSoftware()))
}
