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
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Z-fighting repro: two large quads (red then blue) at the **same place**, with the blue one tilted
 * by a hair ([tiltRad]) so the two surfaces are *nearly coplanar* and cross through each other near
 * the middle. The whole pair spins slowly. Along the intersection the depth values are within float
 * rounding of each other, so the winner flips pixel-to-pixel and frame-to-frame — the classic
 * z-fight speckle. Software backend only (the depth-buffered path).
 *
 * @param tiltRad relative tilt between the two quads (smaller = a wider, more violent fight band)
 * @param bias depth bias applied to the blue quad before drawing it; a small negative value pulls
 *   it nearer so it wins cleanly over red (no fight). `0f` = no bias (shows the fight).
 */
@Suppress("RestrictedApiAndroidX")
public fun zFight3D(
    mode: RcMeshMode,
    tiltRad: Float,
    bias: Float = 0f,
    animate: Boolean = true,
): ByteArray {
    // Static three-quarter angle used when not animating (deterministic for capture/compare).
    val staticYaw = 0.45f
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101014.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                // continuousSeconds() is a Canvas-scope value; capture it here for use inside.
                val spin = if (animate) continuousSeconds() * 0.3f else null
                scene3D {
                    val quad = primitivePlane(1, 1f, 1f)

                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0f), RcFloat(3.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Flat, even lighting so the two faces read as solid red / solid blue and the
                    // fight is unmistakable.
                    directionalLight(
                        direction = vec(RcFloat(0f), RcFloat(0f), RcFloat(-1f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1f),
                    )

                    // Red quad: at the origin plane.
                    color(0xFFE03828.toInt())
                    identity()
                    if (spin != null) {
                        rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                    } else {
                        rotate(angleRad = staticYaw, ax = 0f, ay = 1f, az = 0f)
                    }
                    scale(2.4f, 2.4f, 1f)
                    drawMesh(quad, mode = mode)

                    // Blue quad: same place, tilted by a hair so the two planes interpenetrate.
                    // A small negative depth bias pulls it nearer so it wins cleanly (the fix).
                    if (bias != 0f) {
                        depthBias(bias)
                    }
                    color(0xFF2848E0.toInt())
                    identity()
                    if (spin != null) {
                        rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                    } else {
                        rotate(angleRad = staticYaw, ax = 0f, ay = 1f, az = 0f)
                    }
                    rotate(angleRad = tiltRad, ax = 1f, ay = 0f, az = 0f)
                    scale(2.4f, 2.4f, 1f)
                    drawMesh(quad, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun zFight3DSoftware(): ByteArray = zFight3D(RcMeshMode.SoftwareFlat, 0.01f)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ZFight3DPreview() {
    RemoteDocumentPreview(RemoteDocument(zFight3DSoftware()))
}
