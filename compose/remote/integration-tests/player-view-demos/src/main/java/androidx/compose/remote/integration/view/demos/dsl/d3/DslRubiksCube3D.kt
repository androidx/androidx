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
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.fraction
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveRoundedCube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.clamp01
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

private const val BG = 0xFF0A0C12.toInt()
private const val GAP = 1.04f // cubie center spacing
private const val CUBIE = 0.96f
private const val SEC_PER_MOVE = 1.5f

// Classic-ish sticker palette (whole cubies, not per-face — see API note).
private val PALETTE =
    intArrayOf(
        0xFFE03A3A.toInt(), // red
        0xFFF7A21B.toInt(), // orange
        0xFFF2E33B.toInt(), // yellow
        0xFF3FA93F.toInt(), // green
        0xFF2D6BD6.toInt(), // blue
        0xFFEDEDED.toInt(), // white
    )

// Move schedule: (axis 0=X,1=Y,2=Z ; layer +1/-1). Each move turns that face out to 90° and back.
private val MOVE_AXIS = intArrayOf(0, 1, 2, 0, 1, 2)
private val MOVE_LAYER = intArrayOf(1, 1, 1, -1, -1, -1)
private val N_MOVES = MOVE_AXIS.size

/**
 * A **Rubik's Cube turn animation**: a 3×3×3 of rounded cubies, cycling through face turns. Each
 * move rotates one layer out to 90° and back (`(π/2)·sin(π·frac)`), then the next face goes.
 * Because every move **returns to zero** before the next begins, a cubie's layer membership can be
 * decided once at author time from its home cell and stays valid forever — no permutation state to
 * track.
 *
 * How the gate works without per-frame state: a `slot = floor(phase·N)` selects the active move and
 * `active = clamp(1 − 2·|slot − s|, 0, 1)` is 1 only on that move; a cubie in the move's layer adds
 * `active·turn` to the matching axis angle. Since one move touches one axis at a time, at most one
 * of the three [rotate]s is ever non-identity.
 *
 * **API note — the honest limitation:** a *real* Rubik's cube permutes stickers, so after a quarter
 * turn the layer's cubies occupy new cells. Expressing that needs cross-frame state (or indexing a
 * lookup table by a reactive move counter and re-attaching colors), which the closed-form reactive
 * model can't do. Turn-and-return sidesteps it; true solving/scrambling would need a stateful op.
 */
@Suppress("RestrictedApiAndroidX")
public fun rubiksCube3D(mode: RcMeshMode): ByteArray {
    val cycle = SEC_PER_MOVE * N_MOVES

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Rubik's Cube"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag to orbit the cube (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                // Move timing (computed once, shared by every cubie). No manual flush() needed:
                // FloatExpressionOptimizer auto-extracts the shared ph/slot/turn sub-expressions
                // into hidden variables and keeps every per-cubie angle under the 32-token cap.
                val ph = fraction(continuousSeconds() / cycle).flush() // 0..1 over the whole cycle
                val slot = floor(ph * N_MOVES.toFloat()).flush()
                val frac = fraction(ph * N_MOVES.toFloat()).flush()
                val turn = (frac * PI.toFloat()).sin() * (PI.toFloat() / 2f) // 0→90°→0

                // moveAngle[s] = active(s) * turn ; active(s)=1 only while slot==s.
                val moveAngle =
                    Array(N_MOVES) { s ->
                        val active: RcFloat = ((slot - s.toFloat()).abs() * -2f + 1f).clamp01()
                        active * turn
                    }
                // Look up by (axis, layer) → the reactive angle for that face's move.
                fun angleFor(axis: Int, coord: Int): RcFloat? {
                    if (coord == 0) return null
                    for (s in 0 until N_MOVES) {
                        if (MOVE_AXIS[s] == axis && MOVE_LAYER[s] == coord) {
                            return moveAngle[s].flush()
                        }
                    }
                    return null
                }

                scene3D {
                    val cubie = primitiveRoundedCube(1, CUBIE, CUBIE, CUBIE, 0.14f, segments = 2f)

                    perspectiveCameraDeg(
                        fovYDeg = 38f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        eye = vec(RcFloat(4.6f), RcFloat(4.0f), RcFloat(6.6f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.55f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(0.4f), RcFloat(0.5f)),
                        color = 0xFF8090B0.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    var p = 0
                    for (i in -1..1) {
                        for (j in -1..1) {
                            for (k in -1..1) {
                                color(PALETTE[p % PALETTE.size])
                                p++
                                val aX = angleFor(0, i)
                                val aY = angleFor(1, j)
                                val aZ = angleFor(2, k)
                                orbitIdentity(view, tilt)
                                if (aX != null) rotate(aX, 1f, 0f, 0f)
                                if (aY != null) rotate(aY, 0f, 1f, 0f)
                                if (aZ != null) rotate(aZ, 0f, 0f, 1f)
                                translate(i * GAP, j * GAP, k * GAP)
                                drawMesh(cubie, mode = mode)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Flat-shaded: a Rubik's cube reads better faceted/matte than smoothed.
@Suppress("RestrictedApiAndroidX")
public fun rubiksCube3DSoftware(): ByteArray = rubiksCube3D(RcMeshMode.SoftwareFlat)

@Suppress("RestrictedApiAndroidX")
public fun rubiksCube3DCanvas(): ByteArray = rubiksCube3D(RcMeshMode.CanvasFlat)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun RubiksCube3DPreview() {
    RemoteDocumentPreview(RemoteDocument(rubiksCube3DSoftware()))
}
