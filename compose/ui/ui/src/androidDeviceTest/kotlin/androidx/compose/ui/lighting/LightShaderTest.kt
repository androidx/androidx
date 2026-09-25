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

import android.graphics.Bitmap
import android.graphics.Picture
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class LightShaderTest {

    @get:Rule val rule = createComposeRule()

    private val density = Density(1f)
    private val surfaceSize = Size(100f, 100f)
    private val whiteDiffuseMaterial = SurfaceMaterial.diffuse(Color.White)

    @Test
    fun compilesAndDrawsFlatConvexAndConcaveGeometries() {
        val light =
            DirectionalLight(
                color = Color.Red,
                intensity = 1f,
                directionX = 0f,
                directionY = 0f,
                directionZ = 1f,
            )
        val specs =
            listOf(
                SurfaceGeometrySpec.flat(RectangleShape, thickness = 4.dp),
                SurfaceGeometrySpec.convex(RectangleShape, thickness = 4.dp, bevelWidth = 10.dp),
                SurfaceGeometrySpec.concave(RectangleShape, thickness = 4.dp, bevelWidth = 10.dp),
            )

        for (spec in specs) {
            val pixels = renderShader(listOf(light), whiteDiffuseMaterial, spec)
            val center = pixels[50, 50]
            assertEqualsWithTolerance(Color.Red, center, 0.1f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenMoreThanMaxLightsProvided() {
        val light = DirectionalLight(Color.White, 0f, 0f, 1f, intensity = 1f)
        val spec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 2.dp)
        val tooManyLights = List(LightShader.MaxLightsAllowed + 1) { light }

        LightShader(
            lights = tooManyLights,
            surfaceMaterial = whiteDiffuseMaterial,
            surfaceGeometrySpec = spec,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            size = surfaceSize,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenLightIsNotResolvedIntoLocalSpace() {
        val unresolvedGlobal =
            PointLight(
                color = Color.White,
                position = LightPosition.Global(DpOffset(50.dp, 50.dp)),
                elevation = 40.dp,
                falloffRadius = 200.dp,
                intensity = 1f,
            )
        val spec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 2.dp)

        LightShader(
            lights = listOf(unresolvedGlobal),
            surfaceMaterial = whiteDiffuseMaterial,
            surfaceGeometrySpec = spec,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            size = surfaceSize,
        )
    }

    @Test
    fun unspecifiedLocalPositionContributesZeroLight() {
        val unresolvedSentinel =
            PointLight(
                color = Color.White,
                position = LightPosition.Local(DpOffset.Unspecified),
                elevation = 40.dp,
                falloffRadius = 200.dp,
                intensity = 1f,
            )
        val spec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 2.dp)
        val pixels = renderShader(listOf(unresolvedSentinel), whiteDiffuseMaterial, spec)

        assertThat(pixels[(surfaceSize.width / 2).toInt(), (surfaceSize.height / 2).toInt()].red)
            .isEqualTo(0f)
        assertThat(pixels[(surfaceSize.width / 2).toInt(), (surfaceSize.height / 2).toInt()].alpha)
            .isEqualTo(0f)
    }

    @Test
    fun roundedRectCoverageMaskRespectsPerCornerCircularRadii() {
        val perCornerRoundedShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(
                            rect = Rect(0f, 0f, size.width, size.height),
                            topLeft = CornerRadius(35f, 35f),
                            topRight = CornerRadius.Zero,
                            bottomRight = CornerRadius(35f, 35f),
                            bottomLeft = CornerRadius.Zero,
                        )
                    )
            }
        val light = DirectionalLight(Color.Red, 0f, 0f, 1f, intensity = 1f)
        val spec = SurfaceGeometrySpec.flat(perCornerRoundedShape, thickness = 4.dp)

        val pixels = renderShader(listOf(light), whiteDiffuseMaterial, spec)
        // Top-left (2, 2) and bottom-right (97, 97) are outside their 35px corners -> transparent.
        assertThat(pixels[2, 2].alpha).isEqualTo(0f)
        assertThat(pixels[97, 97].alpha).isEqualTo(0f)
        // Top-right (97, 2) and bottom-left (2, 97) have 0px radius -> lit, like the center.
        assertThat(pixels[97, 2].alpha).isGreaterThan(0.9f)
        assertEqualsWithTolerance(Color.Red, pixels[97, 2], 0.1f)
        assertThat(pixels[2, 97].alpha).isGreaterThan(0.9f)
        assertEqualsWithTolerance(Color.Red, pixels[2, 97], 0.1f)
        assertThat(pixels[50, 50].alpha).isGreaterThan(0.9f)
        assertEqualsWithTolerance(Color.Red, pixels[50, 50], 0.1f)
    }

    @Test
    fun convexAndConcaveInvertBevelBrightnessUnderAngledLight() {
        // Light arriving from the left (-x) and in front (+z).
        val leftLight =
            DirectionalLight(
                color = Color.White,
                intensity = 1f,
                directionX = -1f,
                directionY = 0f,
                directionZ = 1f,
            )
        val convexSpec =
            SurfaceGeometrySpec.convex(RectangleShape, thickness = 4.dp, bevelWidth = 20.dp)
        val concaveSpec =
            SurfaceGeometrySpec.concave(RectangleShape, thickness = 4.dp, bevelWidth = 20.dp)

        val convexPixels = renderShader(listOf(leftLight), whiteDiffuseMaterial, convexSpec)
        val concavePixels = renderShader(listOf(leftLight), whiteDiffuseMaterial, concaveSpec)

        // On a convex bevel (x=10 vs x=90), the left bevel tilts towards (-x), catching the light.
        assertThat(convexPixels[10, 50].alpha).isGreaterThan(convexPixels[90, 50].alpha)
        // On a concave bevel, the left bevel tilts inwards (+x), facing away from the left light.
        assertThat(concavePixels[10, 50].alpha).isLessThan(concavePixels[90, 50].alpha)
    }

    @Test
    fun pointLightRespectsFalloffRadiusAndElevation() {
        val spec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 0.dp)

        // Elevation (60dp) >= fallOffRadius (50dp) -> light never reaches the z=0 surface.
        val unreachableLight =
            PointLight(
                color = Color.White,
                position = LightPosition.Local(DpOffset(50.dp, 50.dp)),
                elevation = 60.dp,
                falloffRadius = 50.dp,
                intensity = 1f,
            )
        val darkPixels = renderShader(listOf(unreachableLight), whiteDiffuseMaterial, spec)
        assertThat(darkPixels[50, 50].red).isEqualTo(0f)

        // Elevation (30dp) < fallOffRadius (80dp) -> bright at center, dimmer at corner.
        val reachableLight =
            PointLight(
                color = Color.White,
                position = LightPosition.Local(DpOffset(50.dp, 50.dp)),
                elevation = 30.dp,
                falloffRadius = 80.dp,
                intensity = 1f,
            )
        val litPixels = renderShader(listOf(reachableLight), whiteDiffuseMaterial, spec)

        assertThat(
                litPixels[(surfaceSize.width / 2).toInt(), (surfaceSize.height / 2).toInt()].alpha
            )
            .isGreaterThan(litPixels[5, 5].alpha)
    }

    @Test
    fun spotLightClipsFragmentsOutsideConeAngle() {
        val spec = SurfaceGeometrySpec.flat(RectangleShape, thickness = 0.dp)
        // At elevation = 40dp and cone angle = 30 degrees, the radius of spotlight on surface is
        // 40 * tan(30°) ≈ 23.1px around (50, 50). Considering density = 1f.
        val spot =
            SpotLight(
                color = Color.White,
                position = LightPosition.Local(DpOffset(50.dp, 50.dp)),
                targetOffset = DpOffset.Zero,
                elevation = 40.dp,
                falloffRadius = 200.dp,
                coneAngleDegrees = 30f,
                intensity = 1f,
            )
        val pixels = renderShader(listOf(spot), whiteDiffuseMaterial, spec)

        // Center, inside the cone. The light is placed 40dp just above the center.
        val fallOff = with(density) { (1.0f - 40 / spot.falloffRadius.toPx()).coerceIn(0f, 1.0f) }
        val attenuation = spot.intensity * fallOff * fallOff

        assertEqualsWithTolerance(
            Color.White.copy(alpha = attenuation),
            pixels[50, 50],
            tolerance = 0.01f,
        )
        // 40px away, outside cone radius
        assertThat(pixels[10, 50].red).isEqualTo(0f)
    }

    @Test
    fun inLocalSpaceResolvesGlobalAndAnchoredCoordinatesInComposition() {
        val anchorState = LightAnchorState()
        var surfaceCoords: LayoutCoordinates? = null

        val anchorOffset = IntOffset(120, 90)
        val litSurfaceOffset = IntOffset(40, 30)

        rule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(
                    Modifier.offset { anchorOffset }
                        .size(10.dp)
                        .onGloballyPositioned { anchorState.coordinates = it }
                )
                Box(
                    Modifier.offset { litSurfaceOffset }
                        .size(50.dp)
                        .onGloballyPositioned { surfaceCoords = it }
                )
            }
        }
        rule.waitForIdle()

        val coords = checkNotNull(surfaceCoords)
        val lightOffsetInAnchor = DpOffset(5.dp, 10.dp)
        val anchoredLight =
            PointLight(
                color = Color.White,
                position = LightPosition.Anchored(anchorState, lightOffsetInAnchor),
                elevation = 20.dp,
                falloffRadius = 100.dp,
                intensity = 1f,
            )
        val resolvedAnchored = with(anchoredLight) { density.inLocalSpace(coords) }

        val anchoredToLocalOffset =
            with(density) {
                DpOffset(
                    (anchorOffset.x - litSurfaceOffset.x).toDp() + lightOffsetInAnchor.x,
                    (anchorOffset.y - litSurfaceOffset.y).toDp() + lightOffsetInAnchor.y,
                )
            }
        assertThat(resolvedAnchored.position).isEqualTo(LightPosition.Local(anchoredToLocalOffset))

        val globalOffset = DpOffset(100.dp, 80.dp)
        val globalLight =
            PointLight(
                color = Color.White,
                position = LightPosition.Global(globalOffset),
                elevation = 20.dp,
                falloffRadius = 100.dp,
                intensity = 1f,
            )
        val resolvedGlobal = with(globalLight) { density.inLocalSpace(coords) }

        val globalToLocalOffset =
            with(density) {
                globalOffset - DpOffset(litSurfaceOffset.x.toDp(), litSurfaceOffset.y.toDp())
            }
        assertThat(resolvedGlobal.position).isEqualTo(LightPosition.Local(globalToLocalOffset))
    }

    /** Draws a [LightShader] into a bitmap and returns its pixels. */
    private fun renderShader(
        lights: List<Light>,
        material: SurfaceMaterial,
        geometrySpec: SurfaceGeometrySpec,
    ): PixelMap {
        val shader =
            LightShader(
                lights = lights,
                surfaceMaterial = material,
                surfaceGeometrySpec = geometrySpec,
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                size = surfaceSize,
            )
        val imageBitmap =
            drawToImageBitmap(shader).asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        return imageBitmap.asImageBitmap().toPixelMap()
    }

    private fun drawToImageBitmap(shader: LightShader): ImageBitmap {
        val width = surfaceSize.width.toInt()
        val height = surfaceSize.height.toInt()
        val paint = Paint().also { shader.applyTo(it) }
        val picture =
            object : Picture() {
                override fun beginRecording(width: Int, height: Int): android.graphics.Canvas =
                    android.graphics.Canvas()

                override fun endRecording() {}

                override fun getWidth(): Int = width

                override fun getHeight(): Int = height

                override fun requiresHardwareAcceleration(): Boolean = true

                override fun draw(canvas: android.graphics.Canvas) {
                    Canvas(canvas).drawRect(0f, 0f, surfaceSize.width, surfaceSize.height, paint)
                }
            }
        return Bitmap.createBitmap(picture).asImageBitmap()
    }
}

fun assertEqualsWithTolerance(expected: Color, actual: Color, tolerance: Float) {
    Assert.assertEquals(expected.red, actual.red, tolerance)
    Assert.assertEquals(expected.green, actual.green, tolerance)
    Assert.assertEquals(expected.blue, actual.blue, tolerance)
    Assert.assertEquals(expected.alpha, actual.alpha, tolerance)
}
