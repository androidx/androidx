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
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitiveExtrudedCircle
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BG = 0xFF0C1018.toInt()
private const val FACE = 0xFF1B2536.toInt()
private const val RIM = 0xFF34618E.toInt()
private const val TICK = 0xFFCBD6E8.toInt()
private const val HOUR_COL = 0xFFE9EEF7.toInt()
private const val MIN_COL = 0xFF9FC0FF.toInt()
private const val SEC_COL = 0xFFE0876E.toInt()
private const val HUB = 0xFFE0876E.toInt()

private const val RADIUS = 2.3f
private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * A **3D analog clock** whose hands you see edge-on in space. The face is an extruded disk with a
 * raised rim; twelve tick blocks ring it; three box hands (hour/minute/second) stand proud of the
 * face at different depths and rotate by **wall-clock time** read live on the player (`hour()`,
 * `minutes()`, `seconds()`). This is the clean case for the API: each hand is one [rotate] with a
 * reactive [RcFloat] angle about Z, so the document is tiny and the hands tick on-device with no
 * re-upload. The whole clock also tips slowly so you read it as a solid object, not a flat dial.
 */
@Suppress("RestrictedApiAndroidX")
public fun clock3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Clock"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }

                // Wall-clock angles (radians), measured clockwise from 12 o'clock (straight up).
                // Hours: 30°/hr + smooth creep from the minute; minutes/seconds: 6° each.
                val deg = (PI / 180.0).toFloat()
                val secA = seconds() * (6f * deg)
                val minA = (minutes() + seconds() / 60f) * (6f * deg)
                val hourA = (hour() + minutes() / 60f) * (30f * deg)

                // Drag to orbit the clock (horizontal = spin, vertical = tilt); rests facing front.
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    // Reusable meshes.
                    val faceDisk = primitiveExtrudedCircle(1, RADIUS, 0.30f, segments = 64f)
                    val rimRing = primitiveExtrudedCircle(2, RADIUS + 0.12f, 0.18f, segments = 64f)
                    val tickBox = primitiveCube(3, 0.10f, 0.34f, 0.14f, y = 0.17f)
                    val hourHand =
                        primitiveCube(4, 0.16f, RADIUS * 0.55f, 0.10f, y = RADIUS * 0.275f)
                    val minHand = primitiveCube(5, 0.11f, RADIUS * 0.80f, 0.09f, y = RADIUS * 0.40f)
                    val secHand =
                        primitiveCube(6, 0.045f, RADIUS * 0.92f, 0.06f, y = RADIUS * 0.46f)
                    val hub = primitiveSphere(7, 0.18f, segments = 20f)

                    perspectiveCameraDeg(
                        fovYDeg = 42f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        eye = vec(RcFloat(0f), RcFloat(2.4f), RcFloat(7.6f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.35f), RcFloat(-0.45f), RcFloat(-0.82f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                        color = 0xFF9FB6E0.toInt(),
                        intensity = RcFloat(0.45f),
                    )

                    // Rim (slightly behind the face), then the recessed face.
                    color(RIM)
                    orbitIdentity(view, tilt)
                    translate(0f, 0f, -0.10f)
                    drawMesh(rimRing, mode = mode)

                    color(FACE)
                    orbitIdentity(view, tilt)
                    drawMesh(faceDisk, mode = mode)

                    // Twelve tick blocks around the dial. Built at the 12 position then rotated.
                    color(TICK)
                    for (i in 0 until 12) {
                        val a = i * (TWO_PI / 12f)
                        val tx = (RADIUS * 0.84f) * sin(a.toDouble()).toFloat()
                        val ty = (RADIUS * 0.84f) * cos(a.toDouble()).toFloat()
                        orbitIdentity(view, tilt)
                        translate(tx, ty, 0.22f)
                        rotate(-a, 0f, 0f, 1f)
                        // The 12/3/6/9 marks are the same blocks; depth alone reads as a dial.
                        drawMesh(tickBox, mode = mode)
                    }

                    // Hands: each is a box whose base sits at the hub; rotate clockwise (−Z).
                    color(HOUR_COL)
                    orbitIdentity(view, tilt)
                    translate(0f, 0f, 0.26f)
                    rotate(hourA * -1f, 0f, 0f, 1f)
                    drawMesh(hourHand, mode = mode)

                    color(MIN_COL)
                    orbitIdentity(view, tilt)
                    translate(0f, 0f, 0.34f)
                    rotate(minA * -1f, 0f, 0f, 1f)
                    drawMesh(minHand, mode = mode)

                    color(SEC_COL)
                    orbitIdentity(view, tilt)
                    translate(0f, 0f, 0.42f)
                    rotate(secA * -1f, 0f, 0f, 1f)
                    drawMesh(secHand, mode = mode)

                    // Center hub caps the hands.
                    color(HUB)
                    orbitIdentity(view, tilt)
                    translate(0f, 0f, 0.46f)
                    drawMesh(hub, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun clock3DSoftware(): ByteArray = clock3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun clock3DCanvas(): ByteArray = clock3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Clock3DPreview() {
    RemoteDocumentPreview(RemoteDocument(clock3DSoftware()))
}
