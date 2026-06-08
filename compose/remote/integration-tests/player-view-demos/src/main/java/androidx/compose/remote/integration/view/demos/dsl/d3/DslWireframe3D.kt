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
 * Demonstrates the hidden-line **wireframe** render mode ([RcMeshMode.SoftwareWireframe]). The mode
 * rasterizes each front-facing triangle into the depth buffer only (transparent faces), then draws
 * its edges as depth-tested lines, so edges behind nearer surface are occluded — a true hidden-line
 * wireframe, not a see-through one.
 *
 * Three tori, tilted so the depth ordering is obvious:
 * 1. **left** — pure wireframe (you should NOT see the back rim or the far side of the hole);
 * 2. **center** — shaded smooth, then a wireframe overlay (draw the same mesh twice; the depth
 *    buffer is shared within the pass, so the dark edges sit cleanly on the lit surface);
 * 3. **right** — a transparent see-through wireframe would show every edge; this one is
 *    hidden-line, for direct comparison with the lit+wire version.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframe3D(mode: RcMeshMode): ByteArray {
    val seg = 22f
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Wireframe 3D"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFFF7F8FA.toInt())) {
                paint { color(0xFFF7F8FA.toInt()) }
                // Drag horizontally to spin both meshes about the vertical axis; flick to coast.
                val spinY = verticalSpinAngle()
                scene3D {
                    // Centered, straight-on: the definitive hidden-line test. A correct hidden-line
                    // sphere shows ONLY the front hemisphere's lat/long lines; the torus hole is
                    // clean (no far rim showing through the near body).
                    val sphere = primitiveSphere(1, 1f, segments = seg)
                    val torus = primitiveTorus(2, 0.62f, 0.26f, segments = seg)

                    perspectiveCameraDeg(
                        fovYDeg = 30f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.9f), RcFloat(5.2f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.9f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(-0.1f), RcFloat(0.4f)),
                        color = 0xFFD0D8E8.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    // Top: pure hidden-line wireframe sphere (back hemisphere must be hidden).
                    color(0xFF1A2330.toInt())
                    spunIdentity(spinY)
                    translate(0f, 1.15f, 0f)
                    drawMesh(sphere, mode = RcMeshMode.SoftwareWireframe)

                    // Bottom: shaded smooth + wireframe overlay (same mesh, two passes).
                    spunIdentity(spinY)
                    translate(0f, -1.15f, 0f)
                    rotate(angleRad = 0.5f, ax = 1f, ay = 0f, az = 0f)
                    color(0xFF4A90D9.toInt())
                    drawMesh(torus, mode = RcMeshMode.SoftwareSmooth)
                    color(0xFF0E1A2A.toInt())
                    drawMesh(torus, mode = RcMeshMode.SoftwareWireframe)
                }

                paint {
                    color(0xFF2A2A2A.toInt())
                    textSize(34f)
                }
                drawTextAnchored(remoteText("Hidden-line Wireframe"), 500f, 60f, 0f, 0f)
                paint {
                    color(0xFF666666.toInt())
                    textSize(22f)
                }
                drawTextAnchored(remoteText("wireframe sphere"), 720f, 300f, 0f, 0f)
                drawTextAnchored(remoteText("shaded + wireframe"), 720f, 760f, 0f, 0f)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun wireframe3DSoftware(): ByteArray = wireframe3D(RcMeshMode.SoftwareWireframe)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Wireframe3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframe3DSoftware()))
}
