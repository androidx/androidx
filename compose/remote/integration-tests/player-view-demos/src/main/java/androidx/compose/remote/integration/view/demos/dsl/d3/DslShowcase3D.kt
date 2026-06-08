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

package androidx.compose.remote.integration.view.demos.dsl.d3

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCone
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveSphericalDome
import androidx.compose.remote.creation.dsl.primitiveSweep
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.primitiveTube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.Geometry3D
import androidx.compose.remote.integration.view.demos.dsl.kit.Noise3D
import androidx.compose.remote.integration.view.demos.dsl.kit.Palettes
import androidx.compose.remote.integration.view.demos.dsl.kit.doc3DTags
import androidx.compose.remote.integration.view.demos.dsl.kit.gradient
import androidx.compose.remote.integration.view.demos.dsl.kit.hueColor
import androidx.compose.remote.integration.view.demos.dsl.kit.osc01
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================================
// A showcase of 10 visually-distinct 3D demos, each on its own background, built on the demo "kit"
// (Geometry3D paths, Noise3D, Palette3D, Textures3D) + the touch turntable (Demo3DTouch). They
// double
// as a gallery of techniques: procedural paths, instancing, reactive color, scatter, sweeps.
// Drag to orbit any of them.
// ============================================================================================

/** Two-light rig shared by most scenes: a key light + a cool fill. */
@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.keyAndFill(fill: Int = 0xFF8090C0.toInt()) {
    clearLights()
    directionalLight(
        vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
        0xFFFFFFFF.toInt(),
        RcFloat(1.0f),
    )
    directionalLight(vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)), fill, RcFloat(0.45f))
}

// ---- 1. Torus knot ------------------------------------------------------------- bg olive
// --------

/** A glossy (2,3) torus knot, hue drifting over time. Path from [Geometry3D.torusKnotPath]. */
@Suppress("RestrictedApiAndroidX")
public fun torusKnot3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Torus Knot"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF14160A.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val knotColor = hueColor((continuousSeconds() * 0.05f) % 1f, sat = 0.7f, value = 1f)
                scene3D {
                    val knot =
                        primitiveTube(
                            1,
                            0.22f,
                            Geometry3D.torusKnotPath(2, 3, 1.05f, 260),
                            capped = false,
                            segments = 12f,
                        )
                    perspectiveCameraDeg(
                        34f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(9.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill()
                    material(0.55f, 30f)
                    color(knotColor)
                    orbitIdentity(view, tilt)
                    drawMesh(knot, mode = mode)
                }
            }
        }
    }

// ---- 2. DNA double helix ------------------------------------------------------- bg teal
// ---------

/** Two phosphate strands (spheres on helices) joined by base-pair rungs; slowly turning. */
@Suppress("RestrictedApiAndroidX")
public fun dnaHelix3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("DNA Helix"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF06161A.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val spin = continuousSeconds() * 0.5f
                val rungColors = Palettes.NEON
                scene3D {
                    val bead = primitiveSphere(1, 0.28f, segments = 18f)
                    val rung = primitiveCylinder(2, 0.07f, 0f, 0f, 0f, 0f, 1f, 0f, segments = 8f)
                    perspectiveCameraDeg(
                        42f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(11f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill()
                    val n = 26
                    val rise = 0.42f
                    val radius = 1.5f
                    for (k in 0 until n) {
                        val a = k * 0.5f
                        val y = (k - (n - 1) / 2f) * rise
                        val ax = radius * cos(a)
                        val az = radius * sin(a)
                        // strand A (blue) and strand B (pink), 180° apart
                        color(0xFF4FA8E0.toInt())
                        place(bead, mode, ax, y, az, spin)
                        color(0xFFE06EA8.toInt())
                        place(bead, mode, -ax, y, -az, spin)
                        // a base-pair rung every other step
                        if (k % 2 == 0) {
                            color(rungColors[(k / 2) % rungColors.size])
                            identity()
                            rotate(spin, 0f, 1f, 0f)
                            rotate((Math.PI / 2.0).toFloat(), 0f, 0f, 1f) // cylinder +Y → +X-ish
                            // place the rung spanning the two strands at height y: build along X
                            // by scaling a +Y unit cylinder then rotating; simpler: two half rungs.
                            identity()
                            rotate(spin, 0f, 1f, 0f)
                            translate(ax, y, az)
                            // aim the unit-Y cylinder toward the opposite strand
                            val aim = Geometry3D.aimYTo(-2 * ax, 0f, -2 * az)
                            rotate(aim[0], aim[1], aim[2], aim[3])
                            scale(1f, 2f * radius, 1f)
                            drawMesh(rung, mode = mode)
                        }
                    }
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.place(
    m: RcMesh,
    mode: RcMeshMode,
    x: Float,
    y: Float,
    z: Float,
    spin: RcFloat,
) {
    identity()
    rotate(spin, 0f, 1f, 0f)
    translate(x, y, z)
    drawMesh(m, mode = mode)
}

// ---- 3. Gimbals --------------------------------------------------------------- bg slate --------

/** Three nested rings spinning on different axes at different rates — a gyroscope. */
@Suppress("RestrictedApiAndroidX")
public fun gimbals3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Gimbals"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF12141C.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val t = continuousSeconds()
                scene3D {
                    val r1 = primitiveTorus(1, 3.0f, 0.12f, segments = 80f)
                    val r2 = primitiveTorus(2, 2.4f, 0.12f, segments = 72f)
                    val r3 = primitiveTorus(3, 1.8f, 0.12f, segments = 64f)
                    val core = primitiveSphere(4, 0.7f, segments = 32f)
                    perspectiveCameraDeg(
                        40f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(2.5f), RcFloat(9.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill()
                    material(0.6f, 36f)
                    color(0xFFB9C6D8.toInt())
                    gimbal(r1, mode, view, tilt, t * 0.9f, 1f, 0f, 0f)
                    color(0xFFE0A85A.toInt())
                    gimbal(r2, mode, view, tilt, t * -1.3f, 0f, 0f, 1f)
                    color(0xFF6EC0E0.toInt())
                    gimbal(r3, mode, view, tilt, t * 1.7f, 0f, 1f, 0f)
                    color(0xFFE0876E.toInt())
                    orbitIdentity(view, tilt)
                    drawMesh(core, mode = mode)
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.gimbal(
    m: RcMesh,
    mode: RcMeshMode,
    view: RcFloat,
    tilt: RcFloat,
    spin: RcFloat,
    ax: Float,
    ay: Float,
    az: Float,
) {
    orbitIdentity(view, tilt)
    rotate(spin, ax, ay, az)
    drawMesh(m, mode = mode)
}

// ---- 4. Spiral galaxy --------------------------------------------------------- bg space --------

/**
 * A barred spiral: stars scattered along two logarithmic arms, hue by radius, with a bright core.
 */
@Suppress("RestrictedApiAndroidX")
public fun galaxy3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Spiral Galaxy"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF05060E.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val spin = continuousSeconds() * 0.12f
                scene3D {
                    val star = primitiveSphere(1, 1f, segments = 8f)
                    perspectiveCameraDeg(
                        42f,
                        1f,
                        0.5f,
                        80f,
                        vec(RcFloat(0f), RcFloat(7.5f), RcFloat(10.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        vec(RcFloat(0f), RcFloat(-1f), RcFloat(-0.2f)),
                        0xFFFFFFFF.toInt(),
                        RcFloat(1.1f),
                    )
                    val perArm = 80
                    for (arm in 0 until 2) {
                        val arm0 = arm * Math.PI.toFloat()
                        for (i in 0 until perArm) {
                            val f = i / (perArm - 1f)
                            val a = arm0 + f * 5.2f
                            val r = 0.5f + f * 6.2f
                            // jitter so the arm has thickness
                            val j1 = (Noise3D.hash(arm * 97 + i, i) - 0.5f) * 1.3f
                            val j2 = (Noise3D.hash(i, arm * 53 + i) - 0.5f) * 1.3f
                            val x = r * cos(a) + j1
                            val z = r * sin(a) + j2
                            val y = (Noise3D.hash(i, arm) - 0.5f) * 0.8f
                            val size = 0.06f + Noise3D.hash(arm + 7, i) * 0.13f
                            color(gradient(Palettes.NEON, f))
                            identity()
                            rotate(spin, 0f, 1f, 0f)
                            translate(x, y, z)
                            scale(size)
                            drawMesh(star, mode = mode)
                        }
                    }
                    // glowing core
                    color(0xFFFFF1C0.toInt())
                    identity()
                    rotate(spin, 0f, 1f, 0f)
                    scale(0.8f)
                    drawMesh(star, mode = mode)
                }
            }
        }
    }

// ---- 5. Lava lamp ------------------------------------------------------------- bg purple -------

/** Warm blobs drifting up and down on their own phases (reactive translate). */
@Suppress("RestrictedApiAndroidX")
public fun lavaLamp3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Lava Lamp"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF160A1E.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                scene3D {
                    val blob = primitiveSphere(1, 1f, segments = 28f)
                    perspectiveCameraDeg(
                        40f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(11f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill(0xFF7A3A9A.toInt())
                    material(0.4f, 18f)
                    val nBlob = 10
                    for (i in 0 until nBlob) {
                        val phase = i * 1.7f
                        val speed = 0.5f + Noise3D.hash(i, 3) * 0.5f
                        val x = (Noise3D.hash(i, 1) - 0.5f) * 3.2f
                        val z = (Noise3D.hash(i, 2) - 0.5f) * 2.2f
                        val y = (t * speed + phase).sin() * 3.4f
                        val size = 0.55f + Noise3D.hash(i, 9) * 0.7f
                        color(gradient(Palettes.EMBER, 0.3f + i / nBlob.toFloat() * 0.7f))
                        orbitIdentity(view, tilt)
                        translate(RcFloat(x), y, RcFloat(z))
                        scale(size)
                        drawMesh(blob, mode = mode)
                    }
                }
            }
        }
    }

// ---- 6. Audio ring ------------------------------------------------------------ bg midnight -----

/** A ring of cubes bouncing on staggered phases — a circular spectrum analyser. */
@Suppress("RestrictedApiAndroidX")
public fun audioRing3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Audio Ring"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF070B1A.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                scene3D {
                    val cube = primitiveCube(1, 0.5f, 0.5f, 0.5f)
                    perspectiveCameraDeg(
                        40f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(4.5f), RcFloat(9.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill()
                    val m = 32
                    val ringR = 3.4f
                    for (i in 0 until m) {
                        val a = (2.0 * Math.PI * i / m).toFloat()
                        val bx = ringR * cos(a)
                        val bz = ringR * sin(a)
                        // bounce height: a travelling wave around the ring
                        val y = (t * 2.2f + i * 0.5f).osc01() * 2.2f
                        color(gradient(Palettes.NEON, i / (m - 1f)))
                        orbitIdentity(view, tilt)
                        translate(RcFloat(bx), y, RcFloat(bz))
                        drawMesh(cube, mode = mode)
                    }
                }
            }
        }
    }

// ---- 7. Möbius strip ---------------------------------------------------------- bg magenta -----

/**
 * A Möbius ribbon: a flat cross-section swept around a circle with a half-twist (via
 * primitiveSweep).
 */
@Suppress("RestrictedApiAndroidX")
public fun mobius3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Mobius"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF1A0814.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val col = hueColor((continuousSeconds() * 0.06f) % 1f, sat = 0.6f, value = 1f)
                scene3D {
                    val samples = 160
                    val path = Geometry3D.circlePath(3.0f, samples)
                    // a flat ribbon cross-section (a thin rectangle in the moving frame's XY)
                    val w = 1.1f
                    val th = 0.06f
                    val section = floatArrayOf(-w, -th, w, -th, w, th, -w, th)
                    // half a turn (π) accumulated over the loop → a true Möbius strip
                    val twists = FloatArray(samples + 1) { (Math.PI * it / samples).toFloat() }
                    val strip =
                        primitiveSweep(
                            1,
                            section,
                            path,
                            closed = true,
                            caps = false,
                            twists = twists,
                        )
                    perspectiveCameraDeg(
                        40f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(5.5f), RcFloat(8.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill(0xFF9A3A7A.toInt())
                    material(0.4f, 22f)
                    color(col)
                    orbitIdentity(view, tilt)
                    drawMesh(strip, mode = mode)
                }
            }
        }
    }

// ---- 8. Mushrooms ------------------------------------------------------------- bg forest ------

/** A patch of toadstools (dome cap + stem) scattered by noise on a mound. */
@Suppress("RestrictedApiAndroidX")
public fun mushrooms3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Mushrooms"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF0A1408.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                scene3D {
                    val ground = primitiveSphere(1, 1f, segments = 40f) // squashed into a mound
                    val stem = primitiveCylinder(2, 0.16f, 0f, 0f, 0f, 0f, 1f, 0f, segments = 12f)
                    val cap = primitiveSphericalDome(3, 0.55f, 1.45f, segments = 24f)
                    perspectiveCameraDeg(
                        40f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(3.2f), RcFloat(8.5f)),
                        vec(RcFloat(0f), RcFloat(0.3f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill(0xFF3A6A2A.toInt())

                    // mound
                    color(0xFF35521F.toInt())
                    orbitIdentity(view, tilt)
                    translate(0f, -3.6f, 0f)
                    scale(6.5f, 4.0f, 6.5f)
                    drawMesh(ground, mode = mode)

                    val capCols =
                        intArrayOf(0xFFD83A3A.toInt(), 0xFFE0763A.toInt(), 0xFFE0C03A.toInt())
                    val n = 11
                    for (i in 0 until n) {
                        val a = i * 2.39996f
                        val rr = 0.6f + Noise3D.hash(i, 5) * 2.7f
                        val mx = rr * cos(a)
                        val mz = rr * sin(a)
                        val gy = 0.55f * (1f - (rr / 3.6f) * (rr / 3.6f)) // sit on the mound dome
                        val s = 0.55f + Noise3D.hash(i, 8) * 0.7f
                        // stem
                        color(0xFFEAE0C8.toInt())
                        orbitIdentity(view, tilt)
                        translate(mx, gy, mz)
                        scale(s, s * 1.1f, s)
                        drawMesh(stem, mode = mode)
                        // cap
                        color(capCols[i % capCols.size])
                        orbitIdentity(view, tilt)
                        translate(mx, gy + s * 1.0f, mz)
                        scale(s)
                        drawMesh(cap, mode = mode)
                    }
                }
            }
        }
    }

// ---- 9. Crystal --------------------------------------------------------------- bg icy ---------

/** An icy crystal cluster: cones radiating from a center in 3D, glossy blue. */
@Suppress("RestrictedApiAndroidX")
public fun crystal3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("Crystal"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF0C1422.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                scene3D {
                    val spike =
                        primitiveCone(1, 0.5f, 2.6f, segments = 6f) // hexagonal prism-ish cone
                    val core = primitiveSphere(2, 0.7f, segments = 24f)
                    perspectiveCameraDeg(
                        42f,
                        1f,
                        0.5f,
                        60f,
                        vec(RcFloat(0f), RcFloat(1.5f), RcFloat(9.5f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill(0xFF6FA8E0.toInt())
                    material(0.7f, 40f)
                    color(0xFF9FD8F0.toInt())
                    // directions: 6 around the equator + up + down + 4 diagonal
                    val dirs = ArrayList<FloatArray>()
                    for (k in 0 until 6) {
                        val a = (k * Math.PI / 3.0)
                        dirs.add(floatArrayOf(cos(a).toFloat(), 0.15f, sin(a).toFloat()))
                    }
                    dirs.add(floatArrayOf(0f, 1f, 0f))
                    dirs.add(floatArrayOf(0f, -1f, 0f))
                    for (k in 0 until 4) {
                        val a = (k * Math.PI / 2.0 + 0.4)
                        dirs.add(
                            floatArrayOf(0.5f * cos(a).toFloat(), 0.7f, 0.5f * sin(a).toFloat())
                        )
                    }
                    for (d in dirs) {
                        val len = sqrtf(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])
                        val aim = Geometry3D.aimYTo(d[0] / len, d[1] / len, d[2] / len)
                        orbitIdentity(view, tilt)
                        rotate(aim[0], aim[1], aim[2], aim[3])
                        drawMesh(spike, mode = mode)
                    }
                    color(0xFFD8EEFB.toInt())
                    orbitIdentity(view, tilt)
                    drawMesh(core, mode = mode)
                }
            }
        }
    }

private fun sqrtf(v: Float) = sqrt(v.toDouble()).toFloat()

// ---- 10. City at dusk --------------------------------------------------------- bg dusk --------

/** A procedural skyline: noise-height building boxes under a graded dusk sky. */
@Suppress("RestrictedApiAndroidX")
public fun city3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *doc3DTags("City"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF1B1430.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val grid = 12
                val cw = 0.8f
                val half = (grid - 1) / 2f
                scene3D {
                    val box = primitiveCube(1, 1f, 1f, 1f)
                    perspectiveCameraDeg(
                        38f,
                        1f,
                        0.5f,
                        90f,
                        vec(RcFloat(0f), RcFloat(6.5f), RcFloat(12.5f)),
                        vec(RcFloat(0f), RcFloat(1.5f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    keyAndFill(0xFFE08050.toInt())
                    for (gx in 0 until grid) {
                        for (gz in 0 until grid) {
                            val n = Noise3D.fbm(gx * 0.32f, gz * 0.32f, 3)
                            val hgt = 0.4f + n * n * 6.5f
                            val x = (gx - half) * cw
                            val z = (gz - half) * cw
                            // warmer/lit toward the front, cooler toward the back
                            val tint =
                                gradient(
                                    intArrayOf(
                                        0xFF2A3550.toInt(),
                                        0xFF4A5A86.toInt(),
                                        0xFFB59A86.toInt(),
                                    ),
                                    n,
                                )
                            color(tint)
                            orbitIdentity(view, tilt)
                            translate(x, hgt * 0.5f - 1.0f, z)
                            scale(cw * 0.82f, hgt, cw * 0.82f)
                            drawMesh(box, mode = mode)
                        }
                    }
                }
            }
        }
    }

// ---- software wrappers + preview ------------------------------------------------------------

@Suppress("RestrictedApiAndroidX")
public fun torusKnot3DSoftware(): ByteArray = torusKnot3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun dnaHelix3DSoftware(): ByteArray = dnaHelix3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun gimbals3DSoftware(): ByteArray = gimbals3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun galaxy3DSoftware(): ByteArray = galaxy3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun lavaLamp3DSoftware(): ByteArray = lavaLamp3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun audioRing3DSoftware(): ByteArray = audioRing3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun mobius3DSoftware(): ByteArray = mobius3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun mushrooms3DSoftware(): ByteArray = mushrooms3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun crystal3DSoftware(): ByteArray = crystal3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun city3DSoftware(): ByteArray = city3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Showcase3DPreview() {
    RemoteDocumentPreview(RemoteDocument(torusKnot3DSoftware()))
}
