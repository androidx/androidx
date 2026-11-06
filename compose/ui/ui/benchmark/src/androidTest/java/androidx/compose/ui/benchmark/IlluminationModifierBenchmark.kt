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

package androidx.compose.ui.benchmark

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayoutDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.lighting.Light
import androidx.compose.ui.lighting.LightAnchorState
import androidx.compose.ui.lighting.LightPosition
import androidx.compose.ui.lighting.PointLight
import androidx.compose.ui.lighting.SurfaceGeometrySpec
import androidx.compose.ui.lighting.SurfaceMaterial
import androidx.compose.ui.lighting.illuminate
import androidx.compose.ui.lighting.lightAnchor
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class IlluminationModifierBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun lightIllumination_firstDraw() {
        benchmarkRule.benchmarkFirstDraw { LitSurfaceTestCase(LocalLights) }
    }

    @Test
    fun lightPositionUpdate_draw() {
        benchmarkRule.toggleStateBenchmarkDraw({ LightPositionUpdateTestCase() })
    }

    @Test
    fun lightPositionUpdate_recomposeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw({
            LightPositionUpdateTestCase()
        })
    }

    @Test
    fun lambdaLightPositionUpdate_recomposeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            { LambdaLitSurfaceTestCase() },
            requireRecomposition = false,
        )
    }

    @Test
    fun resizeLitComposable_draw() {
        benchmarkRule.toggleStateBenchmarkDraw({ ResizeLitComposableTestCase() })
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class LightAnchorBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun anchoredLightPositionUpdate_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            { AnchoredLightTestCase() },
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun anchoredLightPositionUpdate_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            { AnchoredLightTestCase() },
            toggleCausesRecompose = false,
        )
    }
}

private class LitSurfaceTestCase(private val lights: List<Light>) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        Box(Modifier.size(SurfaceSize).illuminate(lights, Material, Geometry))
    }
}

private class LightPositionUpdateTestCase : ComposeTestCase, ToggleableTestCase {
    private var moved by mutableStateOf(false)

    @Composable
    override fun Content() {
        Box(
            Modifier.size(SurfaceSize)
                .illuminate(if (moved) MovedLocalLights else LocalLights, Material, Geometry)
        )
    }

    override fun toggleState() {
        moved = !moved
    }
}

private class LambdaLitSurfaceTestCase : ComposeTestCase, ToggleableTestCase {
    private var moved by mutableStateOf(false)

    @Composable
    override fun Content() {
        Box(
            Modifier.size(SurfaceSize).illuminate {
                lights = if (moved) MovedLocalLights else LocalLights
                surfaceMaterial = Material
                surfaceGeometrySpec = Geometry
            }
        )
    }

    override fun toggleState() {
        moved = !moved
    }
}

private class ResizeLitComposableTestCase : ComposeTestCase, ToggleableTestCase {
    private var expanded by mutableStateOf(false)

    @Composable
    override fun Content() {
        Box(
            Modifier.size(if (expanded) ExpandedSurfaceSize else SurfaceSize)
                .illuminate(LocalLights, Material, Geometry)
        )
    }

    override fun toggleState() {
        expanded = !expanded
    }
}

private class AnchoredLightTestCase : ComposeTestCase, ToggleableTestCase {
    private val anchor = LightAnchorState()
    private val lights = listOf(pointLight(LightPosition.Anchored(anchor)))
    private var moved by mutableStateOf(false)

    @Composable
    override fun Content() {
        Row {
            Box(
                Modifier.offset { IntOffset(if (moved) AnchorTravel.roundToPx() else 0, 0) }
                    .lightAnchor(anchor)
                    .size(AnchorSize)
            )
            Box(Modifier.size(SurfaceSize).illuminate(lights, Material, Geometry))
        }
    }

    override fun toggleState() {
        moved = !moved
    }
}

private fun pointLight(position: LightPosition): Light =
    PointLight(
        color = Color.Red,
        position = position,
        elevation = 60.dp,
        falloffRadius = 400.dp,
        intensity = 1f,
    )

private val SurfaceSize = 100.dp
private val ExpandedSurfaceSize = 120.dp
private val AnchorSize = 8.dp
private val AnchorTravel = 40.dp

private val Material = SurfaceMaterial.diffuse(Color.White).specular(Color.White, 32f)

private val Geometry =
    SurfaceGeometrySpec.convex(RoundedCornerShape(16.dp), thickness = 4.dp, bevelWidth = 12.dp)

private val LocalLight = pointLight(LightPosition.Local(DpOffset(30.dp, 30.dp)))
private val MovedLocalLight = pointLight(LightPosition.Local(DpOffset(70.dp, 70.dp)))
private val LocalLights = listOf(LocalLight)
private val MovedLocalLights = listOf(MovedLocalLight)
