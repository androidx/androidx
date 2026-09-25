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

package androidx.compose.ui.lighting

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class IlluminationModifierTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun drawsTheLightOverTheContent() {
        rule.setContent {
            Stage(
                Modifier.illuminate(listOf(spotLightAt(25, 25)), WhiteDiffuse, FlatSurface),
                color = Color.Blue,
            )
        }

        val pixels = rule.captureStage()
        pixels.assertLit(25, 25)
        assertThat(pixels[75, 75]).isEqualTo(Color.Blue)
    }

    @Test
    fun changingTheLightsMovesTheLight() {
        var lightX by mutableIntStateOf(25)
        rule.setContent {
            Stage(Modifier.illuminate(listOf(spotLightAt(lightX, 50)), WhiteDiffuse, FlatSurface))
        }
        rule.captureStage().assertLit(25, 50)

        rule.runOnIdle { lightX = 75 }

        rule.captureStage().run {
            assertLit(75, 50)
            assertUnlit(25, 50)
        }
    }

    @Test
    fun changingTheMaterialRedraws() {
        var material by mutableStateOf(WhiteDiffuse)
        rule.setContent { Stage(Modifier.illuminate(listOf(FrontLight), material, FlatSurface)) }
        rule.captureStage().assertLit(50, 50)

        rule.runOnIdle { material = SurfaceMaterial.None }

        rule.captureStage().assertUnlit(50, 50)
    }

    @Test
    fun changingTheBlendModeRedraws() {
        var blendMode by mutableStateOf(BlendMode.SrcOver)
        rule.setContent {
            Stage(Modifier.illuminate(listOf(FrontLight), WhiteDiffuse, FlatSurface, blendMode))
        }
        rule.captureStage().assertLit(50, 50)

        // Keeps the content and discards the light.
        rule.runOnIdle { blendMode = BlendMode.Dst }

        rule.captureStage().assertUnlit(50, 50)
    }

    @Test
    fun recomposingWithEqualInputsDoesNotRedraw() {
        val recompose = mutableIntStateOf(0)
        var compositions = 0
        var draws = 0
        val countDraws = Modifier.drawBehind { draws++ }
        rule.setContent {
            compositions++
            recompose.intValue
            // A new, but equal, list of lights on every composition.
            Box(
                countDraws.size(StageSize).illuminate(listOf(FrontLight), WhiteDiffuse, FlatSurface)
            )
        }
        val initialDraws = rule.runOnIdle { draws }

        rule.runOnIdle { recompose.intValue++ }

        rule.runOnIdle {
            assertThat(compositions).isEqualTo(2)
            assertThat(draws).isEqualTo(initialDraws)
        }
    }

    @Test
    fun growingTheSurfaceGrowsTheLitArea() {
        var surfaceSize by mutableStateOf(50.dp)
        rule.setContent {
            Stage {
                Box(
                    Modifier.size(surfaceSize)
                        .illuminate(listOf(FrontLight), WhiteDiffuse, FlatSurface)
                )
            }
        }
        rule.captureStage().run {
            assertLit(25, 25)
            assertUnlit(75, 75)
        }

        rule.runOnIdle { surfaceSize = StageSize }

        rule.captureStage().assertLit(75, 75)
    }

    @Test
    fun blockOverloadRedrawsOnStateChangesWithoutRecomposing() {
        var lightX by mutableIntStateOf(25)
        var compositions = 0
        rule.setContent {
            compositions++
            Stage(
                Modifier.illuminate {
                    lights = listOf(spotLightAt(lightX, 50))
                    surfaceMaterial = WhiteDiffuse
                }
            )
        }
        rule.captureStage().assertLit(25, 50)

        rule.runOnIdle { lightX = 75 }

        rule.captureStage().run {
            assertLit(75, 50)
            assertUnlit(25, 50)
        }
        rule.runOnIdle { assertThat(compositions).isEqualTo(1) }
    }

    @Test
    fun anchoredLightFollowsItsAnchorWhenAnAncestorMoves() {
        val anchor = LightAnchorState()
        var anchorX by mutableIntStateOf(20)
        rule.setContent {
            Stage {
                MovingAncestor({ IntOffset(anchorX, 40) }) {
                    Box(Modifier.size(10.dp).lightAnchor(anchor))
                }
                Box(
                    Modifier.matchParentSize()
                        .graphicsLayer()
                        .illuminate(
                            listOf(spotLight(LightPosition.Anchored(anchor, DpOffset(5.dp, 5.dp)))),
                            WhiteDiffuse,
                            FlatSurface,
                        )
                )
            }
        }
        rule.captureStage().assertLit(25, 45)

        rule.runOnIdle { anchorX = 70 }

        rule.captureStage().run {
            assertLit(75, 45)
            assertUnlit(25, 45)
        }
    }

    @Test
    fun globalLightStaysPutWhenTheSurfaceMoves() = assertGlobalLightStaysPutWhenTheSurfaceMoves {
        illuminate(it, WhiteDiffuse, FlatSurface)
    }

    @Test
    fun blockOverloadGlobalLightStaysPutWhenTheSurfaceMoves() =
        assertGlobalLightStaysPutWhenTheSurfaceMoves { globalLights ->
            illuminate {
                lights = globalLights
                surfaceMaterial = WhiteDiffuse
            }
        }

    @Test
    fun anchorInAnotherWindowContributesNoLight() {
        val anchor = LightAnchorState()
        rule.setContent {
            Box(Modifier.size(10.dp).lightAnchor(anchor))
            Popup {
                Stage(
                    Modifier.illuminate(
                        listOf(spotLight(LightPosition.Anchored(anchor))),
                        WhiteDiffuse,
                        FlatSurface,
                    )
                )
            }
        }

        rule.captureStage().run {
            assertUnlit(5, 5)
            assertUnlit(50, 50)
        }
    }

    private fun assertGlobalLightStaysPutWhenTheSurfaceMoves(
        lighting: Modifier.(List<Light>) -> Modifier
    ) {
        val lights = listOf(spotLight(LightPosition.Global(DpOffset(50.dp, 50.dp))))
        var surfaceX by mutableIntStateOf(0)
        rule.setContent {
            Stage {
                MovingAncestor({ IntOffset(surfaceX, 0) }) {
                    Box(Modifier.size(StageSize).graphicsLayer().lighting(lights))
                }
            }
        }
        rule.captureStage().assertLit(50, 50)

        rule.runOnIdle { surfaceX = -30 }

        rule.captureStage().run {
            assertLit(50, 50)
            assertUnlit(20, 50)
        }
    }
}

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(
    minSdkVersion = Build.VERSION_CODES.O,
    maxSdkVersion = Build.VERSION_CODES.S_V2,
)
class IlluminationModifierUnsupportedTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun drawsOnlyTheContent() {
        rule.setContent {
            Stage(
                Modifier.illuminate(listOf(FrontLight), WhiteDiffuse, FlatSurface),
                color = Color.Blue,
            )
        }

        assertThat(rule.captureStage()[50, 50]).isEqualTo(Color.Blue)
    }
}

private const val StageTag = "stage"
private val StageSize = 100.dp

private val WhiteDiffuse = SurfaceMaterial.diffuse(Color.White)
private val FlatSurface = SurfaceGeometrySpec.flat(RectangleShape, thickness = 0.dp)

/** Lights the whole surface evenly. */
private val FrontLight =
    DirectionalLight(Color.White, directionX = 0f, directionY = 0f, directionZ = 1f, intensity = 1f)

/** Shines straight down, lighting a disc with a radius of about 15px around [position]. */
private fun spotLight(position: LightPosition) =
    SpotLight(
        color = Color.White,
        position = position,
        targetOffset = DpOffset.Zero,
        elevation = 40.dp,
        falloffRadius = 400.dp,
        coneAngleDegrees = 20f,
        intensity = 1f,
    )

private fun spotLightAt(x: Int, y: Int) = spotLight(LightPosition.Local(DpOffset(x.dp, y.dp)))

/** A 100x100px surface (1dp == 1px) filled with [color], with [modifier] drawn over it. */
@Composable
private fun Stage(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    content: @Composable BoxScope.() -> Unit = {},
) {
    CompositionLocalProvider(LocalDensity provides Density(1f)) {
        Box(
            Modifier.testTag(StageTag).size(StageSize).background(color).then(modifier),
            content = content,
        )
    }
}

/**
 * Places [content] at [offset], nested one level below the node that moves. Changing [offset] moves
 * [content] in the window without re-placing it within its parent, so the move is reported by
 * `onGloballyPositioned` but not by `onPlaced`.
 */
@Composable
private fun MovingAncestor(offset: Density.() -> IntOffset, content: @Composable () -> Unit) {
    Box(Modifier.offset(offset)) { Box { content() } }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.captureStage(): PixelMap =
    onNodeWithTag(StageTag).captureToImage().toPixelMap()

private fun PixelMap.assertLit(x: Int, y: Int) =
    assertWithMessage("red at ($x, $y)").that(this[x, y].red).isGreaterThan(0.5f)

private fun PixelMap.assertUnlit(x: Int, y: Int) =
    assertWithMessage("red at ($x, $y)").that(this[x, y].red).isLessThan(0.05f)
