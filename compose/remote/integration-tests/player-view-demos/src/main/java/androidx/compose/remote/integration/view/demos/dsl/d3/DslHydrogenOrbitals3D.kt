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
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val BG = 0xFF06080F.toInt()
private const val STACKS = 56
private const val SLICES = 72

// Each orbital gets a distinct color; the surface drawn is the constant-|ψ| (angular) lobe shape.
private const val C_1S = 0xFF6CC5FF.toInt()
private const val C_2P = 0xFFE0876E.toInt()
private const val C_3DZ = 0xFFB07AE0.toInt()
private const val C_3DXY = 0xFF6EE0A0.toInt()

/**
 * Build a constant-probability lobe surface as `radius(θ,φ) = scale·|f(θ,φ)|` over a (θ,φ) grid,
 * with finite-difference normals so the lobes shade smoothly. [f] is the angular part of the
 * wavefunction; its sign nodes pinch the surface into the familiar lobes.
 */
private fun RcScene3DScope.orbital(id: Int, scale: Float, f: (Double, Double) -> Double): RcMesh {
    val rows = STACKS + 1
    val cols = SLICES + 1
    val pos = FloatArray(rows * cols * 3)
    var p = 0
    for (i in 0..STACKS) {
        val th = PI * i / STACKS
        val st = sin(th)
        val ct = cos(th)
        for (j in 0..SLICES) {
            val ph = 2.0 * PI * j / SLICES
            val r = (scale * abs(f(th, ph))).toFloat() + 0.02f
            pos[p] = r * (st * cos(ph)).toFloat()
            pos[p + 1] = r * ct.toFloat()
            pos[p + 2] = r * (st * sin(ph)).toFloat()
            p += 3
        }
    }
    // Finite-difference normals (outward-corrected by the radial direction).
    val nor = FloatArray(rows * cols * 3)
    fun idx(i: Int, j: Int) = (i * cols + j) * 3
    for (i in 0..STACKS) {
        for (j in 0..SLICES) {
            val i0 = if (i > 0) i - 1 else i
            val i1 = if (i < STACKS) i + 1 else i
            val j0 = if (j > 0) j - 1 else j
            val j1 = if (j < SLICES) j + 1 else j
            val ax = pos[idx(i1, j)] - pos[idx(i0, j)]
            val ay = pos[idx(i1, j) + 1] - pos[idx(i0, j) + 1]
            val az = pos[idx(i1, j) + 2] - pos[idx(i0, j) + 2]
            val bx = pos[idx(i, j1)] - pos[idx(i, j0)]
            val by = pos[idx(i, j1) + 1] - pos[idx(i, j0) + 1]
            val bz = pos[idx(i, j1) + 2] - pos[idx(i, j0) + 2]
            var nx = ay * bz - az * by
            var ny = az * bx - ax * bz
            var nz = ax * by - ay * bx
            var nl = sqrt(nx * nx + ny * ny + nz * nz)
            if (nl < 1e-6f) {
                nx = pos[idx(i, j)]
                ny = pos[idx(i, j) + 1]
                nz = pos[idx(i, j) + 2]
                nl = sqrt(nx * nx + ny * ny + nz * nz) + 1e-6f
            }
            nx /= nl
            ny /= nl
            nz /= nl
            // Flip to face outward (same hemisphere as the radial position vector).
            val rx = pos[idx(i, j)]
            val ry = pos[idx(i, j) + 1]
            val rz = pos[idx(i, j) + 2]
            if (nx * rx + ny * ry + nz * rz < 0f) {
                nx = -nx
                ny = -ny
                nz = -nz
            }
            val o = idx(i, j)
            nor[o] = nx
            nor[o + 1] = ny
            nor[o + 2] = nz
        }
    }
    val indices = IntArray(STACKS * SLICES * 6)
    var k = 0
    for (i in 0 until STACKS) {
        for (j in 0 until SLICES) {
            val a = i * cols + j
            val b = (i + 1) * cols + j
            val c = (i + 1) * cols + (j + 1)
            val d = i * cols + (j + 1)
            indices[k++] = a
            indices[k++] = d
            indices[k++] = b
            indices[k++] = b
            indices[k++] = d
            indices[k++] = c
        }
    }
    return defineMesh(id, indices, pos, nor)
}

/**
 * **Hydrogen orbital probability clouds**: four constant-probability-density surfaces of the lowest
 * orbitals, side by side and slowly turning — 1s (a sphere), 2p (a dumbbell), 3d_z² (lobes through
 * a ring) and 3d_x²−y² (a four-lobe cloverleaf). Each is a parametric surface `r(θ,φ)=|Yₗₘ(θ,φ)|`
 * generated as a custom [defineMesh] at author time (the case the API supports by handing you raw
 * verts/normals/indices), so the sign nodes of the harmonic pinch the surface into the textbook
 * lobes. A tiny nucleus marks each center.
 */
@Suppress("RestrictedApiAndroidX")
public fun hydrogenOrbitals3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Hydrogen Orbitals"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag to orbit the whole 2×2 set (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    val s = (1.0 / sqrt(2.0)) // d-orbital tidy factor
                    val mesh1s = orbital(1, 1.05f) { _, _ -> 1.0 }
                    val mesh2p = orbital(2, 1.7f) { th, _ -> cos(th) }
                    val mesh3dz = orbital(3, 1.0f) { th, _ -> 3.0 * cos(th) * cos(th) - 1.0 }
                    val mesh3dxy =
                        orbital(4, 1.8f) { th, ph -> sin(th) * sin(th) * cos(2.0 * ph) * s }
                    val nucleus = primitiveSphere(5, 0.10f, segments = 14f)

                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 80f,
                        eye = vec(RcFloat(0f), RcFloat(0f), RcFloat(15.5f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.45f), RcFloat(-0.8f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.5f), RcFloat(0.4f)),
                        color = 0xFF8090C0.toInt(),
                        intensity = RcFloat(0.5f),
                    )
                    material(specStrength = 0.35f, shininess = 24f)

                    // 2×2 grid: 1s | 2p (top row), 3d z² | 3d x²−y² (bottom row).
                    val gx = 2.9f
                    val gy = 2.9f
                    val pos =
                        arrayOf(
                            floatArrayOf(-gx, gy),
                            floatArrayOf(gx, gy),
                            floatArrayOf(-gx, -gy),
                            floatArrayOf(gx, -gy),
                        )
                    val meshes = arrayOf(mesh1s, mesh2p, mesh3dz, mesh3dxy)
                    val cols = intArrayOf(C_1S, C_2P, C_3DZ, C_3DXY)
                    for (n in 0 until 4) {
                        color(cols[n])
                        orbitIdentity(view, tilt)
                        translate(pos[n][0], pos[n][1], 0f)
                        drawMesh(meshes[n], mode = mode)
                        // nucleus
                        color(0xFFFFFFFF.toInt())
                        orbitIdentity(view, tilt)
                        translate(pos[n][0], pos[n][1], 0f)
                        drawMesh(nucleus, mode = mode)
                    }
                }

                paint {
                    color(0xFFE9ECF2.toInt())
                    textSize(34f)
                }
                drawTextAnchored(remoteText("Hydrogen orbital clouds"), 500f, 34f, 0.5f, 0f)
                // Labels sit under each orbital at rest (the 2×2 grid orbits with touch).
                paint { textSize(28f) }
                drawTextAnchored(remoteText("1s"), 243f, 452f, 0.5f, 0f)
                drawTextAnchored(remoteText("2p"), 757f, 452f, 0.5f, 0f)
                drawTextAnchored(remoteText("3d z²"), 243f, 956f, 0.5f, 0f)
                drawTextAnchored(remoteText("3d x²-y²"), 757f, 956f, 0.5f, 0f)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun hydrogenOrbitals3DSoftware(): ByteArray = hydrogenOrbitals3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun hydrogenOrbitals3DCanvas(): ByteArray = hydrogenOrbitals3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun HydrogenOrbitals3DPreview() {
    RemoteDocumentPreview(RemoteDocument(hydrogenOrbitals3DSoftware()))
}
