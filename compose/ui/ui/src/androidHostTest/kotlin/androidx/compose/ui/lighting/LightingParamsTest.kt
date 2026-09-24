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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LightingParamsTest {

    @Test
    fun surfaceMaterialDefaultsToAllNone() {
        val material = SurfaceMaterial.None
        assertFalse(material.isDiffuseEnabled)
        assertFalse(material.isSpecularEnabled)
        assertFalse(material.isFresnelEnabled)
        assertEquals(Color.Unspecified, material.diffuseColor)
        assertEquals(Color.Unspecified, material.specularColor)
        assertEquals(0f, material.fresnelReflectivity)
        assertEquals(0f, material.fresnelIntensity)
    }

    @Test
    fun fresnelDefaultIntensityIsOne() {
        val material = SurfaceMaterial.fresnel(reflectivity = 0.04f)
        assertEquals(0.04f, material.fresnelReflectivity)
        assertEquals(1.0f, material.fresnelIntensity)
    }

    @Test
    fun surfaceMaterialEqualsAndHashCode() {
        val m1 = SurfaceMaterial.diffuse(Color.Red).specular(Color.White, 64f).fresnel(0.04f, 0.8f)
        val m2 = SurfaceMaterial.diffuse(Color.Red).specular(Color.White, 64f).fresnel(0.04f, 0.8f)
        assertEquals(m1, m2)
        assertEquals(m1.hashCode(), m2.hashCode())

        assertNotEquals(m1, m1.diffuse(Color.Blue))
        assertNotEquals(m1, m1.specular(Color.White, 32f))
        assertNotEquals(m1, m1.fresnel(0.5f, 0.8f))
    }

    @Test
    fun surfaceMaterialThrowsForInvalidSpecularOrFresnelParameters() {
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.specular(Color.White, shininess = 0f)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.specular(Color.White, shininess = -1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.fresnel(reflectivity = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.fresnel(reflectivity = 1.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.fresnel(reflectivity = 0.04f, intensity = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceMaterial.fresnel(reflectivity = 0.04f, intensity = 1.1f)
        }
    }

    @Test
    fun surfaceGeometrySpecThrowsForUnspecifiedOrNegativeBevelWidth() {
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.flat(RectangleShape, thickness = Dp.Unspecified)
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.convex(
                RectangleShape,
                thickness = Dp.Unspecified,
                bevelWidth = 4.dp,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.convex(
                RectangleShape,
                thickness = 4.dp,
                bevelWidth = Dp.Unspecified,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.convex(
                RectangleShape,
                thickness = 4.dp,
                bevelWidth = (-2).dp,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.concave(
                RectangleShape,
                thickness = Dp.Unspecified,
                bevelWidth = 4.dp,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.concave(
                RectangleShape,
                thickness = 4.dp,
                bevelWidth = Dp.Unspecified,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SurfaceGeometrySpec.concave(
                RectangleShape,
                thickness = 4.dp,
                bevelWidth = (-2).dp,
            )
        }
    }

    @Test
    fun createGeometryResolvesPixelDimensionsWithDensityAndClampsBevelWidth() {
        val density = Density(density = 2.5f)
        val spec = SurfaceGeometrySpec.convex(RectangleShape, thickness = 10.dp, bevelWidth = 4.dp)
        val geometry = spec.createGeometry(Size(100f, 80f), LayoutDirection.Ltr, density)

        assertTrue(geometry is ConvexSurfaceGeometry)
        assertEquals(25f, geometry.thickness)
        assertEquals(10f, geometry.bevelWidth)
        assertEquals(Outline.Rectangle(Rect(0f, 0f, 100f, 80f)), geometry.outline)

        // When bevelWidth (30.dp * 2.5 = 75px) exceeds half the shorter side (80px / 2 = 40px),
        // it clamps to 40px.
        val oversizedSpec =
            SurfaceGeometrySpec.concave(RectangleShape, thickness = 10.dp, bevelWidth = 30.dp)
        val clampedGeometry =
            oversizedSpec.createGeometry(Size(100f, 80f), LayoutDirection.Ltr, density)
        assertTrue(clampedGeometry is ConcaveSurfaceGeometry)
        assertEquals(40f, clampedGeometry.bevelWidth)
    }

    @Test
    fun createGeometryAcceptsUniformAndPerCornerCircularRoundedRects() {
        val uniformRoundedShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(
                            rect = Rect(0f, 0f, size.width, size.height),
                            cornerRadius = CornerRadius(12f, 12f),
                        )
                    )
            }
        val perCornerCircularShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(
                            rect = Rect(0f, 0f, size.width, size.height),
                            topLeft = CornerRadius(16f, 16f),
                            topRight = CornerRadius.Zero,
                            bottomRight = CornerRadius(8f, 8f),
                            bottomLeft = CornerRadius(4f, 4f),
                        )
                    )
            }

        val flatGeometry =
            SurfaceGeometrySpec.flat(uniformRoundedShape, thickness = 2.dp)
                .createGeometry(Size(50f, 50f), LayoutDirection.Ltr, Density(1f))
        assertTrue(flatGeometry is FlatSurfaceGeometry)
        assertTrue(flatGeometry.outline is Outline.Rounded)

        val convexGeometry =
            SurfaceGeometrySpec.convex(perCornerCircularShape, thickness = 2.dp, bevelWidth = 4.dp)
                .createGeometry(Size(50f, 50f), LayoutDirection.Ltr, Density(1f))
        assertTrue(convexGeometry is ConvexSurfaceGeometry)
        assertTrue(convexGeometry.outline is Outline.Rounded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createGeometryThrowsForGenericPathOutline() {
        val genericShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Generic(Path())
            }
        val spec = SurfaceGeometrySpec.flat(genericShape, thickness = 2.dp)
        spec.createGeometry(Size(50f, 50f), LayoutDirection.Ltr, Density(1f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createGeometryThrowsForEllipticalRoundedRects() {
        val ellipticalShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(
                            rect = Rect(0f, 0f, size.width, size.height),
                            cornerRadius = CornerRadius(12f, 6f),
                        )
                    )
            }

        SurfaceGeometrySpec.concave(ellipticalShape, 2.dp, 4.dp)
            .createGeometry(Size(50f, 50f), LayoutDirection.Ltr, Density(1f))
    }

    @Test
    fun surfaceGeometrySpecEqualsAndHashCodeDistinguishesSubtypes() {
        val convex1 = SurfaceGeometrySpec.convex(RectangleShape, 4.dp, 8.dp)
        val convex2 = SurfaceGeometrySpec.convex(RectangleShape, 4.dp, 8.dp)
        val concave = SurfaceGeometrySpec.concave(RectangleShape, 4.dp, 8.dp)
        val flat = SurfaceGeometrySpec.flat(RectangleShape, 4.dp)

        assertEquals(convex1, convex2)
        assertEquals(convex1.hashCode(), convex2.hashCode())
        assertNotEquals<SurfaceGeometrySpec>(convex1, concave)
        assertNotEquals<SurfaceGeometrySpec>(convex1, flat)
    }

    @Test
    fun anchoredLightPositionUsesReferentialEqualityForState() {
        val stateA = LightAnchorState()
        val stateB = LightAnchorState()

        val anchoredA1 = LightPosition.Anchored(stateA, DpOffset(10.dp, 20.dp))
        val anchoredA2 = LightPosition.Anchored(stateA, DpOffset(10.dp, 20.dp))
        val anchoredB = LightPosition.Anchored(stateB, DpOffset(10.dp, 20.dp))

        assertEquals(anchoredA1, anchoredA2)
        assertEquals(anchoredA1.hashCode(), anchoredA2.hashCode())
        assertNotEquals(anchoredA1, anchoredB)
    }

    @Test
    fun directionalLightEqualsAndHashcode() {
        val dir1 =
            DirectionalLight(
                Color.White,
                intensity = 1f,
                directionX = 1f,
                directionY = 2f,
                directionZ = 3f,
            )
        val dir2 =
            DirectionalLight(
                Color.White,
                intensity = 1f,
                directionX = 1f,
                directionY = 2f,
                directionZ = 3f,
            )
        assertEquals(dir1, dir2)
        assertEquals(dir1.hashCode(), dir2.hashCode())
        assertNotEquals(
            dir1,
            DirectionalLight(
                Color.White,
                intensity = 1f,
                directionX = 1f,
                directionY = 2f,
                directionZ = 4f,
            ),
        )
    }

    @Test
    fun pointLightEqualsAndHashcode() {
        val pt1 =
            PointLight(
                Color.White,
                LightPosition.Local(DpOffset(5.dp, 6.dp)),
                elevation = 20.dp,
                falloffRadius = 100.dp,
                intensity = 1f,
            )
        val pt2 =
            PointLight(
                Color.White,
                LightPosition.Local(DpOffset(5.dp, 6.dp)),
                elevation = 20.dp,
                falloffRadius = 100.dp,
                intensity = 1f,
            )
        assertEquals(pt1, pt2)
        assertEquals(pt1.hashCode(), pt2.hashCode())
        assertNotEquals(
            pt1,
            PointLight(
                Color.White,
                LightPosition.Global(DpOffset(5.dp, 6.dp)),
                elevation = 20.dp,
                falloffRadius = 100.dp,
                intensity = 1f,
            ),
        )
    }

    @Test
    fun spotLightEqualsAndHashcode() {
        val spot1 =
            SpotLight(
                Color.Cyan,
                LightPosition.Local(DpOffset.Zero),
                targetOffset = DpOffset(1.dp, 2.dp),
                elevation = 30.dp,
                falloffRadius = 120.dp,
                coneAngleDegrees = 30f,
                intensity = 0.8f,
            )
        val spot2 =
            SpotLight(
                Color.Cyan,
                LightPosition.Local(DpOffset.Zero),
                targetOffset = DpOffset(1.dp, 2.dp),
                elevation = 30.dp,
                falloffRadius = 120.dp,
                coneAngleDegrees = 30f,
                intensity = 0.8f,
            )
        assertEquals(spot1, spot2)
        assertEquals(spot1.hashCode(), spot2.hashCode())
    }

    @Test
    fun inLocalSpaceReturnsSameInstanceForLocalAndDirectional() {
        val density = Density(1f)
        val unattachedCoords = DetachedCoordinates()

        val localPoint =
            PointLight(
                color = Color.White,
                position = LightPosition.Local(DpOffset(10.dp, 10.dp)),
                elevation = 20.dp,
                falloffRadius = 50.dp,
                intensity = 1f,
            )
        assertSame(localPoint, with(localPoint) { density.inLocalSpace(unattachedCoords) })

        val directional =
            DirectionalLight(
                color = Color.White,
                intensity = 1f,
                directionX = 0f,
                directionY = 0f,
                directionZ = 1f,
            )
        assertSame(directional, with(directional) { density.inLocalSpace(unattachedCoords) })
    }

    @Test
    fun inLocalSpaceReturnsUnspecifiedLocalWhenAnchorUnplacedOrDetached() {
        val density = Density(1f)
        val unattachedCoords = DetachedCoordinates()
        val anchorState = LightAnchorState() // coordinates == null since we never anchor this light

        val anchoredLight =
            PointLight(
                color = Color.White,
                position = LightPosition.Anchored(anchorState, DpOffset(5.dp, 5.dp)),
                elevation = 20.dp,
                falloffRadius = 50.dp,
                intensity = 1f,
            )
        val resolvedAnchored = with(anchoredLight) { density.inLocalSpace(unattachedCoords) }
        assertEquals(LightPosition.Local(DpOffset.Unspecified), resolvedAnchored.position)

        val globalLight =
            SpotLight(
                color = Color.White,
                position = LightPosition.Global(DpOffset(5.dp, 5.dp)),
                targetOffset = DpOffset.Zero,
                elevation = 20.dp,
                falloffRadius = 50.dp,
                coneAngleDegrees = 30f,
                intensity = 1f,
            )
        val resolvedGlobal = with(globalLight) { density.inLocalSpace(unattachedCoords) }
        assertEquals(LightPosition.Local(DpOffset.Unspecified), resolvedGlobal.position)
    }

    private class DetachedCoordinates : LayoutCoordinates {
        override val size: IntSize = IntSize(100, 100)
        override val providedAlignmentLines: Set<AlignmentLine> = emptySet()
        override val parentLayoutCoordinates: LayoutCoordinates? = null
        override val parentCoordinates: LayoutCoordinates? = null
        override val isAttached: Boolean = false

        override fun windowToLocal(relativeToWindow: Offset): Offset = Offset.Zero

        override fun localToWindow(relativeToLocal: Offset): Offset = Offset.Zero

        override fun localToRoot(relativeToLocal: Offset): Offset = Offset.Zero

        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset,
        ): Offset = Offset.Zero

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean,
        ): Rect = Rect.Zero

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
    }
}
