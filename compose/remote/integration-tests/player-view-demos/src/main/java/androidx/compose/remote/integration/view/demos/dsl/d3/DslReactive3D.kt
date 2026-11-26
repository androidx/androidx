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
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveRoundedCube
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

private const val BG = 0xFF0B0E16.toInt()

@Suppress("RestrictedApiAndroidX")
private fun header(desc: String) =
    arrayOf(
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription(desc),
    )

/**
 * **Reactive material (Paint3DState as a variable op).** A sphere whose specular gloss pulses over
 * time: `material(specStrength = …, shininess = …)` is fed reactive [RcFloat]s. The
 * [Paint3DState][androidx.compose.remote.core.operations.d3.Paint3DState] op now implements
 * `VariableSupport`, so the player re-resolves the material each frame — matte ↔ glossy — with no
 * document re-upload. (Specular is the software backend.)
 */
@Suppress("RestrictedApiAndroidX")
public fun reactiveMaterial3D(mode: RcMeshMode): ByteArray {
    val h = header("Reactive Material")
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true), *h) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                // specStrength pulses 0..1 (matte → mirror-bright); shininess fixed.
                val spec = (t * 1.6f).sin() * 0.5f + 0.5f
                val shininess = RcFloat(26f)

                scene3D {
                    val sphere = primitiveSphere(1, 1.5f, segments = 40f)
                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 40f,
                        eye = vec(RcFloat(0f), RcFloat(0.6f), RcFloat(6.2f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        vec(RcFloat(-0.4f), RcFloat(-0.5f), RcFloat(-0.75f)),
                        0xFFFFFFFF.toInt(),
                        RcFloat(1.0f),
                    )
                    directionalLight(
                        vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                        0xFF8090C0.toInt(),
                        RcFloat(0.4f),
                    )

                    material(spec, shininess) // reactive material
                    color(0xFF3B6FB0.toInt())
                    orbitIdentity(view, tilt)
                    drawMesh(sphere, mode = mode)
                }
            }
        }
    }
}

/**
 * **Reactive geometry (MeshPrimitive as a variable op).** A box whose **corner radius** is an
 * [RcFloat], so the [MeshPrimitive][androidx.compose.remote.core.operations.d3.MeshPrimitive] op
 * re-resolves it and **regenerates the mesh on the player** each frame — a box morphing into a
 * rounded blob and back. This is a genuine geometry change (vertices move), not a transform.
 */
@Suppress("RestrictedApiAndroidX")
public fun reactiveGeometry3D(mode: RcMeshMode): ByteArray {
    val h = header("Reactive Geometry")
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true), *h) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                // Corner radius morphs 0.03 (boxy) → ~0.99 (≈ half the 2-unit side, fully rounded).
                val radius = (t * 1.1f).sin() * 0.5f + 0.5f

                scene3D {
                    // segments high enough that the corners stay smooth at the largest radius.
                    val box =
                        primitiveRoundedCube(
                            1,
                            2f,
                            2f,
                            2f,
                            radius = radius * 0.95f + 0.03f,
                            segments = 6f,
                        )
                    perspectiveCameraDeg(
                        fovYDeg = 42f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 40f,
                        eye = vec(RcFloat(0f), RcFloat(1.6f), RcFloat(6.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        0xFFFFF1D8.toInt(),
                        RcFloat(1.0f),
                    )
                    directionalLight(
                        vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                        0xFF7FA0C0.toInt(),
                        RcFloat(0.45f),
                    )

                    color(0xFFE0876E.toInt())
                    orbitIdentity(view, tilt)
                    drawMesh(box, mode = mode)
                }
            }
        }
    }
}

/**
 * **Reactive camera (SetCamera3D as a variable op).** The field-of-view and the eye Z are
 * [RcFloat]s, so the [SetCamera3D][androidx.compose.remote.core.operations.d3.SetCamera3D] op
 * re-resolves the projection + view each frame — a breathing zoom / dolly — with no re-upload.
 */
@Suppress("RestrictedApiAndroidX")
public fun reactiveCamera3D(mode: RcMeshMode): ByteArray {
    val h = header("Reactive Camera")
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true), *h) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val spin = t * 0.4f
                // FOV breathes 34°..70°; eye dollies in/out on Z — both reactive on the player.
                val fov = (t * 0.9f).sin() * 18f + 52f
                val eyeZ = (t * 0.9f).cos() * 1.6f + 7.0f

                scene3D {
                    val torus =
                        primitiveTorus(1, majorRadius = 1.6f, minorRadius = 0.55f, segments = 48f)
                    perspectiveCameraDeg(
                        fovYDeg = fov,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        eye = vec(RcFloat(0f), RcFloat(2.4f), eyeZ),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        0xFFFFFFFF.toInt(),
                        RcFloat(1.0f),
                    )
                    directionalLight(
                        vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                        0xFF6E86B8.toInt(),
                        RcFloat(0.45f),
                    )

                    color(0xFF59C3C3.toInt())
                    identity()
                    rotate(spin, 0f, 1f, 0f)
                    rotate((PI / 2.0).toFloat(), 1f, 0f, 0f)
                    drawMesh(torus, mode = mode)
                }
            }
        }
    }
}

/**
 * **Reactive lights (SetLights3D as a variable op).** A single directional light whose **direction
 * orbits** and **intensity pulses** — both [RcFloat]s. The
 * [SetLights3D][androidx.compose.remote.core.operations.d3.SetLights3D] op re-resolves the light
 * set each frame, so the lit side sweeps around the sphere with no re-upload.
 */
@Suppress("RestrictedApiAndroidX")
public fun reactiveLight3D(mode: RcMeshMode): ByteArray {
    val h = header("Reactive Light")
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true), *h) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                // Light direction orbits in the XZ plane; intensity pulses.
                val lx = (t * 0.9f).cos()
                val lz = (t * 0.9f).sin()
                val intensity = (t * 1.7f).sin() * 0.4f + 1.0f

                scene3D {
                    val sphere = primitiveSphere(1, 1.6f, segments = 48f)
                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 40f,
                        eye = vec(RcFloat(0f), RcFloat(0.5f), RcFloat(6.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    // Reactive directional light (orbiting direction + pulsing intensity).
                    directionalLight(
                        direction = vec(lx, RcFloat(-0.35f), lz),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = intensity,
                    )
                    // Faint fixed fill so the dark side isn't pure black.
                    directionalLight(
                        vec(RcFloat(0.2f), RcFloat(0.3f), RcFloat(0.5f)),
                        0xFF3A4A6A.toInt(),
                        RcFloat(0.25f),
                    )

                    color(0xFFCBD2E0.toInt())
                    orbitIdentity(view, tilt)
                    drawMesh(sphere, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun reactiveMaterial3DSoftware(): ByteArray = reactiveMaterial3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun reactiveGeometry3DSoftware(): ByteArray = reactiveGeometry3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun reactiveCamera3DSoftware(): ByteArray = reactiveCamera3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun reactiveLight3DSoftware(): ByteArray = reactiveLight3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Reactive3DPreview() {
    RemoteDocumentPreview(RemoteDocument(reactiveGeometry3DSoftware()))
}
