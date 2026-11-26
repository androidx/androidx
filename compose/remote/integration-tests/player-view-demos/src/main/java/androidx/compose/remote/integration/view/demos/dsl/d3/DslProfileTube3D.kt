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
@file:SuppressLint("PrimitiveInCollection")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.annotation.SuppressLint
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
import androidx.compose.remote.creation.dsl.primitiveProfileTube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Demo of the **profile tube** ([primitiveProfileTube]): a tube whose radius is splined along its
 * length, so it tapers / bulges / pinches. Three examples, spun in place — a tapering horn, a
 * double-bulge vase (more radii than points), and a beaded curve. Each uses a different number of
 * radius values than centerline points, which the profile maps across the whole path.
 */
@Suppress("RestrictedApiAndroidX")
public fun profileTube3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val spin = continuousSeconds() * 0.5f
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF12151B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    // Horn: 2 points, radius tapers to a near-point.
                    val horn =
                        primitiveProfileTube(
                            id = 1,
                            points = floatArrayOf(0f, -0.75f, 0f, 0f, 0.75f, 0f),
                            radii = floatArrayOf(0.34f, 0.02f),
                            capped = true,
                        )
                    // Vase: 2 points but 5 radii (small-big-mid-big-small → double bulge).
                    val vase =
                        primitiveProfileTube(
                            id = 2,
                            points = floatArrayOf(0f, -0.78f, 0f, 0f, 0.78f, 0f),
                            radii = floatArrayOf(0.05f, 0.28f, 0.11f, 0.28f, 0.05f),
                            capped = true,
                        )
                    // Beads: a curved (S) path with 3 points and 5 oscillating radii.
                    val beads =
                        primitiveProfileTube(
                            id = 3,
                            points =
                                floatArrayOf(
                                    -0.18f,
                                    -0.72f,
                                    0f,
                                    0.18f,
                                    0f,
                                    0.12f,
                                    -0.12f,
                                    0.72f,
                                    0f,
                                ),
                            radii = floatArrayOf(0.05f, 0.2f, 0.05f, 0.2f, 0.05f),
                            capped = true,
                        )
                    val meshes: Array<RcMesh> = arrayOf(horn, vase, beads)
                    val colors =
                        intArrayOf(0xFFE8902E.toInt(), 0xFF36A89A.toInt(), 0xFFE0509A.toInt())

                    perspectiveCameraDeg(
                        fovYDeg = 38f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.4f), RcFloat(5.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.35f), RcFloat(-0.55f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.9f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(-0.1f), RcFloat(0.4f)),
                        color = 0xFF8090C0.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    for (k in 0 until 3) {
                        val gx = (k - 1) * 1.5f
                        color(colors[k])
                        identity()
                        translate(gx, 0f, 0f)
                        rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                        drawMesh(meshes[k], mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun profileTube3DSoftware(): ByteArray = profileTube3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ProfileTube3DPreview() {
    RemoteDocumentPreview(RemoteDocument(profileTube3DSoftware()))
}
