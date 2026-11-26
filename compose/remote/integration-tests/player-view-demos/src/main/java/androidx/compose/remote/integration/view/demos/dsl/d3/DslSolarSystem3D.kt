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
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * One planet of the model. Sizes and orbit radii are **not to scale** — just chosen so the bodies
 * read clearly and the gas giants look bigger than the rocky worlds.
 *
 * @param color base ARGB color
 * @param size sphere radius in world units
 * @param orbit orbit radius in world units (0 = the sun, at the center)
 * @param speed orbital angular speed (radians/sec); inner planets move faster
 * @param phase starting angle so the planets don't all line up
 */
private class Body(
    val color: Int,
    val size: Float,
    val orbit: Float,
    val speed: Float,
    val phase: Float,
)

// Sun + the eight planets (not to scale). Colors are stylized but recognizable.
private val SUN = Body(0xFFFFD23B.toInt(), 0.75f, 0f, 0f, 0f)
private val PLANETS =
    listOf(
        Body(0xFF9A8E84.toInt(), 0.11f, 1.25f, 0.90f, 0.4f), // Mercury — gray
        Body(0xFFE0B24E.toInt(), 0.17f, 1.75f, 0.66f, 2.1f), // Venus — pale gold
        Body(0xFF3D7AD6.toInt(), 0.19f, 2.30f, 0.50f, 4.0f), // Earth — blue
        Body(0xFFC1502A.toInt(), 0.15f, 2.85f, 0.40f, 0.9f), // Mars — rusty red
        Body(0xFFD7A56A.toInt(), 0.42f, 3.85f, 0.26f, 3.3f), // Jupiter — tan
        Body(0xFFE3C879.toInt(), 0.34f, 4.95f, 0.20f, 5.2f), // Saturn — pale gold (has a ring)
        Body(0xFF9FE0E5.toInt(), 0.28f, 5.85f, 0.15f, 1.6f), // Uranus — pale cyan
        Body(0xFF3E5BD0.toInt(), 0.27f, 6.55f, 0.12f, 3.9f), // Neptune — deep blue
    )
private const val SATURN_INDEX = 5

private const val ORBIT_COLOR = 0xFF2B3142.toInt() // faint gray guide rings
private const val RING_COLOR = 0xFFCBB68A.toInt() // Saturn's rings — pale tan
private const val RING_TILT = 0.45f // tip Saturn's rings off the orbital plane (~26°)
private const val BG = 0xFF05070D.toInt() // near-black space

/**
 * A **toy solar system**: the sun at the center with the eight planets orbiting it in the XZ plane,
 * each a colored sphere on a faint guide ring, plus a tilted flat ring around Saturn. Sizes, orbit
 * radii and speeds are **not to scale** — picked for legibility. The planets revolve over time
 * (inner ones faster); drag horizontally to swing the whole system around and look at it from any
 * side. Lighting is deliberately simple (two fixed directional lights) so every body stays brightly
 * colored rather than half in shadow.
 */
@Suppress("RestrictedApiAndroidX")
public fun solarSystem3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Solar System"),
    ) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to orbit the camera (azimuth) around the whole system.
                val view = verticalSpinAngle()
                scene3D {
                    // One unit sphere reused for the sun and every planet (scaled per body); one
                    // unit guide-ring torus reused for every orbit; one flat torus for Saturn.
                    val sphere = primitiveSphere(1, 1f, segments = 28f)
                    val orbitRing =
                        primitiveTorus(2, majorRadius = 1f, minorRadius = 0.006f, segments = 72f)
                    val saturnRing =
                        primitiveTorus(3, majorRadius = 0.62f, minorRadius = 0.22f, segments = 56f)

                    perspectiveCameraDeg(
                        fovYDeg = 46f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        // High and back, looking down ~35° so the circular orbits read as ellipses.
                        eye = vec(RcFloat(0f), RcFloat(6.6f), RcFloat(9.6f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.7f), RcFloat(-0.6f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.35f), RcFloat(0.55f)),
                        color = 0xFFB9C6E0.toInt(),
                        intensity = RcFloat(0.55f),
                    )

                    // Faint orbit guide rings (drawn first), each the unit ring scaled to its
                    // radius.
                    color(ORBIT_COLOR)
                    for (p in PLANETS) {
                        identity()
                        rotate(view, 0f, 1f, 0f)
                        scale(p.orbit)
                        drawMesh(orbitRing, mode = mode)
                    }

                    // The sun at the center.
                    color(SUN.color)
                    identity()
                    rotate(view, 0f, 1f, 0f)
                    scale(SUN.size)
                    drawMesh(sphere, mode = mode)

                    // The planets, each swept to its current orbital angle.
                    for ((i, p) in PLANETS.withIndex()) {
                        val angle = view + time * p.speed + p.phase
                        color(p.color)
                        identity()
                        rotate(angle, 0f, 1f, 0f)
                        translate(p.orbit, 0f, 0f)
                        scale(p.size)
                        drawMesh(sphere, mode = mode)

                        if (i == SATURN_INDEX) {
                            // Saturn's rings: a flat torus (flattened in Y), tipped off the orbital
                            // plane, centered on the planet (not scaled by the planet's size).
                            color(RING_COLOR)
                            identity()
                            rotate(angle, 0f, 1f, 0f)
                            translate(p.orbit, 0f, 0f)
                            rotate(RING_TILT, 1f, 0f, 0f)
                            scale(1f, 0.06f, 1f)
                            drawMesh(saturnRing, mode = mode)
                        }
                    }
                }

                paint {
                    color(0xFFE9ECF2.toInt())
                    textSize(36f)
                }
                drawTextAnchored(remoteText("Solar System (not to scale)"), 500f, 56f, 0f, 0f)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun solarSystem3DSoftware(): ByteArray = solarSystem3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun solarSystem3DCanvas(): ByteArray = solarSystem3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SolarSystem3DPreview() {
    RemoteDocumentPreview(RemoteDocument(solarSystem3DSoftware()))
}
