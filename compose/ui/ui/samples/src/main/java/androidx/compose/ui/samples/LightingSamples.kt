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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.lighting.DirectionalLight
import androidx.compose.ui.lighting.LightAnchorState
import androidx.compose.ui.lighting.LightPosition
import androidx.compose.ui.lighting.PointLight
import androidx.compose.ui.lighting.SpotLight
import androidx.compose.ui.lighting.SurfaceGeometrySpec
import androidx.compose.ui.lighting.SurfaceMaterial
import androidx.compose.ui.lighting.illuminate
import androidx.compose.ui.lighting.lightAnchor
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun IlluminateSample() {
    // Since our light color is white, we set a darker background so that its visible.
    val backgroundColor = remember { Color.Black }
    val surfaceShape = remember { RoundedCornerShape(24.dp) }
    Box(
        Modifier.size(160.dp)
            .background(backgroundColor)
            .illuminate(
                lights =
                    listOf(
                        PointLight(
                            color = Color.White,
                            // Above the top-left corner of the lit composable.
                            position = LightPosition.Local(DpOffset(x = 0.dp, y = 0.dp)),
                            elevation = 48.dp,
                            falloffRadius = 320.dp,
                            intensity = 1f,
                        )
                    ),
                surfaceMaterial =
                    SurfaceMaterial.diffuse(Color.White).specular(Color.White, shininess = 32f),
                surfaceGeometrySpec =
                    SurfaceGeometrySpec.convex(
                        shape = surfaceShape,
                        thickness = 1.dp,
                        bevelWidth = 12.dp,
                    ),
            )
    )
}

@Sampled
@Composable
fun IlluminateWithDirectionalLightSample() {
    // Since our light color is white, we set a darker background so that its visible.
    val surfaceColor = remember { Color.Black }
    val surfaceShape = remember { RoundedCornerShape(24.dp) }
    Box(
        Modifier.size(160.dp)
            .background(surfaceColor)
            .illuminate(
                lights =
                    listOf(
                        DirectionalLight(
                            color = Color.White,
                            // Points towards the light: above and to the left of the surface.
                            directionX = -1f,
                            directionY = -1f,
                            intensity = 1f,
                            directionZ = 1f,
                        )
                    ),
                surfaceMaterial = SurfaceMaterial.diffuse(Color.White),
                surfaceGeometrySpec =
                    SurfaceGeometrySpec.convex(
                        shape = surfaceShape,
                        thickness = 1.dp,
                        bevelWidth = 12.dp,
                    ),
            )
    )
}

@Sampled
@Composable
fun IlluminateWithSpotLightSample() {
    // Since our light color is white, we set a darker background so that its visible.
    val surfaceColor = remember { Color.Black }
    val surfaceShape = remember { RoundedCornerShape(24.dp) }
    val lightPosition = remember { DpOffset(x = 20.dp, y = 20.dp) }
    Box(
        Modifier.size(160.dp)
            .background(surfaceColor)
            .illuminate(
                lights =
                    listOf(
                        SpotLight(
                            color = Color.White,
                            position = LightPosition.Local(lightPosition),
                            // Aim at the middle of the 160.dp surface, the offset from the light
                            // to the point the cone should land on.
                            targetOffset = DpOffset(x = 160.dp / 2, y = 160.dp / 2) - lightPosition,
                            elevation = 60.dp,
                            falloffRadius = 200.dp,
                            coneAngleDegrees = 30f,
                            intensity = 1f,
                        )
                    ),
                surfaceMaterial =
                    SurfaceMaterial.diffuse(Color.White)
                        .specular(Color.White, shininess = 48f)
                        .fresnel(reflectivity = 0.04f),
                surfaceGeometrySpec =
                    SurfaceGeometrySpec.flat(shape = surfaceShape, thickness = 1.dp),
            )
    )
}

@Sampled
@Composable
fun LightAnchorSample() {
    // A single anchor, shared by the composable that defines the light's position and by the
    // light itself.
    val anchor = remember { LightAnchorState() }

    var moved by remember { mutableStateOf(false) }
    val lightX by animateDpAsState(if (moved) 200.dp else 24.dp, label = "lightX")

    // Since our light color is white, we set a darker background so that its visible.
    val surfaceColor = remember { Color.Black }
    val surfaceShape = remember { RoundedCornerShape(24.dp) }

    Box(Modifier.size(240.dp).clickable { moved = !moved }) {
        // The light using this anchor will now follow this composable
        Box(
            Modifier.offset { IntOffset(x = lightX.roundToPx(), y = 24.dp.roundToPx()) }
                .size(16.dp)
                .lightAnchor(anchor)
        )

        Box(
            Modifier.align(Alignment.Center)
                .size(160.dp)
                .background(surfaceColor)
                .illuminate(
                    lights =
                        listOf(
                            PointLight(
                                color = Color.White,
                                position = LightPosition.Anchored(anchor),
                                elevation = 48.dp,
                                falloffRadius = 320.dp,
                                intensity = 1f,
                            )
                        ),
                    surfaceMaterial =
                        SurfaceMaterial.diffuse(Color.White).specular(Color.White, shininess = 32f),
                    surfaceGeometrySpec =
                        SurfaceGeometrySpec.convex(
                            shape = surfaceShape,
                            thickness = 1.dp,
                            bevelWidth = 12.dp,
                        ),
                )
        )
    }
}
