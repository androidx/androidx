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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import kotlin.math.min

/**
 * Describes the virtual three-dimensional shape that a lit surface is treated as having.
 *
 * A spec is resolution independent. It is resolved into concrete geometry once the surface's size,
 * layout direction and density are known, so a single spec can be shared between surfaces of
 * different sizes.
 *
 * Lighting is currently implemented only for rectangles, and for rounded rectangles whose corners
 * each have a circular radius (`x == y`). Any other shape, including `CutCornerShape`,
 * `GenericShape`, and rounded rectangles with elliptical corner radii, throws an
 * [IllegalArgumentException] when the spec is resolved.
 */
@Immutable
public sealed class SurfaceGeometrySpec {

    internal abstract fun createGeometry(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): SurfaceGeometry

    public companion object {
        /**
         * A surface whose interior is flat and faces the viewer directly.
         *
         * Every interior pixel shares the same normal, so the interior takes a single even shade
         * This reads as a sheet of material rather than as a solid object, and
         * [SurfaceMaterial.fresnel] adds an even brightening instead of a rim.
         *
         * @param shape outline of the lit surface. See the class documentation for the shapes that
         *   are supported.
         * @param thickness height of the surface towards the viewer. The attenuation of the light
         *   is calculated based on the resultant distance `lightElevation - surfaceThickness`.
         * @throws IllegalArgumentException if [thickness] is [Dp.Unspecified].
         */
        public fun flat(shape: Shape, thickness: Dp): SurfaceGeometrySpec {
            requireSpecifiedThickness(thickness)
            return FlatSurfaceGeometrySpec(shape, thickness)
        }

        /**
         * A surface that bulges towards the viewer, like a dome.
         *
         * The interior is flat and the outline is wrapped in a curved band whose width is
         * determined by [bevelWidth]. The normals tilt outwards, away from the center of the
         * surface. The curved surface catches a highlight on the side facing a light and falls into
         * shadow on the opposite side, giving the surface a rounded, physical edge.
         *
         * @param shape outline of the lit surface. See the class documentation for the shapes that
         *   are supported.
         * @param thickness height of the surface towards the viewer. The attenuation of the light
         *   is calculated based on the resultant distance `lightElevation - surfaceThickness`. The
         *   thickness is considered uniform over the surface.
         * @param bevelWidth width of the curved band along the outline. Clamped to half of the
         *   surface's smaller dimension when resolved.
         * @throws IllegalArgumentException if [thickness] or [bevelWidth] is [Dp.Unspecified] or
         *   [bevelWidth] is negative.
         */
        public fun convex(shape: Shape, thickness: Dp, bevelWidth: Dp): SurfaceGeometrySpec {
            requireSpecifiedThickness(thickness)
            requireValidBevelWidth(bevelWidth)
            return ConvexSurfaceGeometrySpec(shape, thickness, bevelWidth)
        }

        /**
         * A surface that dishes away from the viewer, like a bowl.
         *
         * The interior is flat and the outline is wrapped in a curved band whose width is
         * determined by [bevelWidth]. The normals tilt inwards, towards the center of the surface.
         * The curved surface falls into shadow on the side facing a light and illuminates on the
         * opposite side, giving the surface a depressed shade.
         *
         * @param shape outline of the lit surface. See the class documentation for the shapes that
         *   are supported.
         * @param thickness height of the surface towards the viewer. The attenuation of the light
         *   is calculated based on the resultant distance `lightElevation - surfaceThickness`. The
         *   thickness is considered uniform over the surface.
         * @param bevelWidth width of the curved band along the outline. Clamped to half of the
         *   surface's smaller dimension when resolved.
         * @throws IllegalArgumentException if [thickness] or [bevelWidth] is [Dp.Unspecified] or
         *   [bevelWidth] is negative.
         */
        public fun concave(shape: Shape, thickness: Dp, bevelWidth: Dp): SurfaceGeometrySpec {
            requireSpecifiedThickness(thickness)
            requireValidBevelWidth(bevelWidth)
            return ConcaveSurfaceGeometrySpec(shape, thickness, bevelWidth)
        }
    }
}

private fun requireSpecifiedThickness(thickness: Dp) =
    requirePrecondition(thickness.isSpecified) { "thickness must be specified" }

private fun requireValidBevelWidth(bevelWidth: Dp) =
    requirePrecondition(bevelWidth.isSpecified && bevelWidth.value >= 0f) {
        "bevelWidth must be specified and non-negative, but was $bevelWidth"
    }

private fun Density.resolveBevelWidthPx(bevelWidth: Dp, size: Size): Float {
    val maxBevelPx = (min(size.width, size.height) * 0.5f).coerceAtLeast(0f)
    return bevelWidth.toPx().coerceIn(0f, maxBevelPx)
}

/** Resolves this [Shape] into an outline. */
private fun Shape.createSupportedOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): Outline {
    val outline = createOutline(size, layoutDirection, density)

    // TODO(gahlout): Look into if we can avoid type checking here.
    requirePrecondition(outline !is Outline.Generic) {
        "SurfaceGeometrySpec does not support $this, which produces a path based outline. " +
            "Lighting is only implemented for rectangles and for rounded rectangles whose " +
            "corners have circular radii. Shapes such as CutCornerShape, GenericShape and " +
            "custom Shape implementations are not supported."
    }
    requirePrecondition(outline !is Outline.Rounded || outline.roundRect.hasCircularCorners()) {
        "SurfaceGeometrySpec does not support $this, which produces a rounded rectangle with " +
            "elliptical corner radii. Lighting is only implemented for rounded rectangles whose " +
            "corners each have a circular radius (x == y)."
    }
    return outline
}

private fun RoundRect.hasCircularCorners(): Boolean =
    topLeftCornerRadius.x == topLeftCornerRadius.y &&
        topRightCornerRadius.x == topRightCornerRadius.y &&
        bottomRightCornerRadius.x == bottomRightCornerRadius.y &&
        bottomLeftCornerRadius.x == bottomLeftCornerRadius.y

@Immutable
internal class ConcaveSurfaceGeometrySpec(
    val shape: Shape,
    val thickness: Dp,
    val bevelWidth: Dp,
) : SurfaceGeometrySpec() {

    override fun createGeometry(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): SurfaceGeometry =
        ConcaveSurfaceGeometry(
            outline = shape.createSupportedOutline(size, layoutDirection, density),
            thickness = with(density) { thickness.toPx() },
            bevelWidth = with(density) { resolveBevelWidthPx(bevelWidth, size) },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConcaveSurfaceGeometrySpec) return false
        if (shape != other.shape) return false
        if (thickness != other.thickness) return false
        if (bevelWidth != other.bevelWidth) return false
        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + thickness.hashCode()
        result = 31 * result + bevelWidth.hashCode()
        return result
    }

    override fun toString(): String =
        "SurfaceGeometrySpec.concave(shape=$shape, thickness=$thickness, bevelWidth=$bevelWidth)"
}

@Immutable
internal class FlatSurfaceGeometrySpec(val shape: Shape, val thickness: Dp) :
    SurfaceGeometrySpec() {

    override fun createGeometry(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): SurfaceGeometry =
        FlatSurfaceGeometry(
            outline = shape.createSupportedOutline(size, layoutDirection, density),
            thickness = with(density) { thickness.toPx() },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlatSurfaceGeometrySpec) return false
        if (shape != other.shape) return false
        if (thickness != other.thickness) return false
        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + thickness.hashCode()
        return result
    }

    override fun toString(): String = "SurfaceGeometrySpec.flat(shape=$shape, thickness=$thickness)"
}

@Immutable
internal class ConvexSurfaceGeometrySpec(
    val shape: Shape,
    val thickness: Dp,
    val bevelWidth: Dp,
) : SurfaceGeometrySpec() {

    override fun createGeometry(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): SurfaceGeometry =
        ConvexSurfaceGeometry(
            outline = shape.createSupportedOutline(size, layoutDirection, density),
            thickness = with(density) { thickness.toPx() },
            bevelWidth = with(density) { resolveBevelWidthPx(bevelWidth, size) },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConvexSurfaceGeometrySpec) return false
        if (shape != other.shape) return false
        if (thickness != other.thickness) return false
        if (bevelWidth != other.bevelWidth) return false
        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + thickness.hashCode()
        result = 31 * result + bevelWidth.hashCode()
        return result
    }

    override fun toString(): String =
        "SurfaceGeometrySpec.convex(shape=$shape, thickness=$thickness, bevelWidth=$bevelWidth)"
}
