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
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * 3D DSL demo: a flat-shaded cube rotating around the Y axis, then a tilt around the X axis. Uses
 * the pure-Java software 3D backend ([JavaPaint3DContext]) bound inside [AndroidPaintContext].
 *
 * Operations exercised:
 * - `defineCubeMesh` → `DefineMesh3D`
 * - `perspectiveCameraDeg` → `SetCamera3D` (perspective + lookAt)
 * - `clearDepth` → `Paint3DState`
 * - `rotate(RcFloat, …)` → `Matrix3DOp(ROTATE_AXIS)` with a NaN-encoded reactive angle
 * - `drawMesh` → `DrawMesh3D`
 *
 * The rotation is driven by `continuousSeconds()` so the angle updates every frame.
 */
@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun RotatingCube3DPreview() {
    RemoteDocumentPreview(RemoteDocument(rotatingCube3D()))
}

@Suppress("RestrictedApiAndroidX")
public fun rotatingCube3D(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                // Color the cube faces; the engine modulates this by per-triangle Lambert.
                paint { color(0xFFFFAA33.toInt()) }

                scene3D {
                    val cube = primitiveCube(1, 1f, 1f, 1f)

                    perspectiveCameraDeg(
                        fovYDeg = 50f,
                        // The runtime resizes the backing depth/color buffer to the
                        // current viewport, so a fixed 1.0 aspect is fine for this demo;
                        // a future enhancement can plumb a reactive width/height ratio.
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.6f), RcFloat(3.5f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()

                    // Constant tilt so the top face is visible.
                    rotate(angleRad = 0.45f, ax = 1f, ay = 0f, az = 0f)

                    // Reactive spin around the Y axis — one revolution every ~6 seconds.
                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)

                    drawMesh(cube)
                }
            }
        }
    }
}
