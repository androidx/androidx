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
import androidx.compose.remote.creation.dsl.RcMesh
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
 * DSL demo that makes the depth-handling difference between the two backends **unmistakable**: a
 * gray ring with a teal ball threaded through it (the ball is larger than the hole, so the two
 * meshes interpenetrate). Drawn as two separate `drawMesh` calls in one pass.
 * - [RcMeshMode.SoftwareSmooth] keeps a per-pixel z-buffer that is **shared across draw calls**
 *   within the pass, so the ring and ball interleave correctly — the near side of the ring is in
 *   front of the ball, the ball bulges in front of the far side. (This is also the proof that the
 *   software z-buffer works: if it didn't, this view would be wrong too.)
 * - [RcMeshMode.CanvasSmooth] has no depth buffer; each `drawMesh` is an independent
 *   `Canvas.drawVertices`, painter-sorted only within itself. The ball is drawn second, so it
 *   paints entirely over the ring wherever they overlap — the near ring that should occlude the
 *   ball vanishes. That is the weakness of the canvas path.
 *
 * A single (non-interpenetrating) torus would look identical on both backends, because backface
 * culling leaves almost no front-facing self-overlap for the painter's sort to get wrong — which is
 * why this demo uses two interpenetrating meshes instead.
 *
 * It also exercises the **real light op**: a clear + two directional lights (warm key, cool fill)
 * drive shading instead of the implicit default headlight.
 */
@Suppress("RestrictedApiAndroidX")
public fun torus3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                var ring: RcMesh? = null
                var ball: RcMesh? = null

                // Pass 1 — the ring (gray). Sets camera, depth, and the lights for the frame.
                paint { color(0xFFBFC4CC.toInt()) }
                scene3D {
                    ring = primitiveTorus(1, 1f, 0.4f, segments = 48f)
                    ball = primitiveSphere(2, 0.85f, segments = 32f)

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(2.2f), RcFloat(3.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-1f), RcFloat(-1f), RcFloat(-0.6f)),
                        color = 0xFFFFE6C0.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(1f), RcFloat(-0.2f), RcFloat(0.4f)),
                        color = 0xFF4060FF.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    rotate(angleRad = 1.1f, ax = 1f, ay = 0f, az = 0f)
                    rotate(angleRad = time, ax = 0f, ay = 0f, az = 1f)
                    drawMesh(ring!!, mode = mode)
                }

                // Pass 2 — the ball (teal). Same camera + transform; deliberately no clearDepth
                // and no clearLights, so the software z-buffer (and the lights) carry over from
                // pass 1 and the two meshes share a depth test.
                paint { color(0xFF30C0A8.toInt()) }
                scene3D {
                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(2.2f), RcFloat(3.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    rotate(angleRad = 1.1f, ax = 1f, ay = 0f, az = 0f)
                    rotate(angleRad = time, ax = 0f, ay = 0f, az = 1f)
                    drawMesh(ball!!, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun torus3DSoftwareFlat(): ByteArray = torus3D(RcMeshMode.SoftwareFlat)

@Suppress("RestrictedApiAndroidX")
public fun torus3DSoftware(): ByteArray = torus3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun torus3WireFrame1(): ByteArray = torus3D(RcMeshMode.SoftwareWireframe1)

@Suppress("RestrictedApiAndroidX")
public fun torus3WireFrame2(): ByteArray = torus3D(RcMeshMode.SoftwareWireframe2)

@Suppress("RestrictedApiAndroidX")
public fun torus3WireFrame3(): ByteArray = torus3D(RcMeshMode.SoftwareWireframe3)

@Suppress("RestrictedApiAndroidX")
public fun torus3DCanvasSmooth(): ByteArray = torus3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
public fun torus3DCanvasFlat(): ByteArray = torus3D(RcMeshMode.CanvasFlat)

@Suppress("RestrictedApiAndroidX")
public fun torus3DMeshFlat(): ByteArray = torus3D(RcMeshMode.DrawMeshFlat)

@Suppress("RestrictedApiAndroidX")
public fun torus3DMeshSmooth(): ByteArray = torus3D(RcMeshMode.DrawMeshSmooth)

@Suppress("RestrictedApiAndroidX")
public fun torus3DMeshZFlat(): ByteArray = torus3D(RcMeshMode.DrawMeshZBufFlat)

@Suppress("RestrictedApiAndroidX")
public fun torus3DMeshZSmooth(): ByteArray = torus3D(RcMeshMode.DrawMeshZBufSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Torus3DSoftwarePreview() {
    RemoteDocumentPreview(RemoteDocument(torus3DSoftware()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Torus3DCanvasPreview() {
    RemoteDocumentPreview(RemoteDocument(torus3DSoftwareFlat()))
}
