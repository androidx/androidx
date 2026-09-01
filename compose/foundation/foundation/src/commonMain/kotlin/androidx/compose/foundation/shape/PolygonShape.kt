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

package androidx.compose.foundation.shape

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.shape.PolygonShapeGeometry.CornerRounding
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastMap
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Creates a [PolygonShape] whose geometry is defined by [builder] at layout resolution time.
 *
 * [builder] runs in [PolygonShapeScope] on every resolution, with access to the resolved size,
 * layout direction, and density; the [PolygonShapeGeometry] it returns is the shape's definition.
 * Values the lambda reads are re-read on each resolution, and the outline rebuilds when the
 * resulting geometry changes. Two shapes created with the same [builder] instance compare equal.
 *
 * Use this overload when the geometry depends on the resolved size or density, such as vertices
 * placed in pixels of the layout's coordinate space or rounding given in [Dp]. For a fixed geometry
 * that scales uniformly into any container, use the [PolygonShape] overload taking a
 * [PolygonShapeGeometry].
 *
 * @sample androidx.compose.foundation.samples.CustomPolygonShapeSample
 * @param builder lambda run in [PolygonShapeScope] returning the shape's geometry
 */
@RememberInComposition
public fun PolygonShape(builder: PolygonShapeScope.() -> PolygonShapeGeometry): PolygonShape =
    BuilderPolygonShape(builder)

/**
 * Creates a [PolygonShape] from [geometry] defined in an author-chosen coordinate space.
 *
 * The geometry is uniformly scaled and centered to fit within the layout bounds at resolution time,
 * preserving its aspect ratio.
 *
 * Use this overload for a fixed shape definition, such as unit-space vertices, that scales into
 * whatever bounds the shape is used in. To define geometry against the resolved size, layout
 * direction, or density instead, use the [PolygonShape] overload taking a [PolygonShapeScope]
 * builder.
 *
 * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
 * @param geometry polygon geometry to scale and center into the layout bounds
 */
@RememberInComposition
public fun PolygonShape(geometry: PolygonShapeGeometry): PolygonShape =
    GeometryPolygonShape(geometry)

/**
 * Shape built from rounded polygon geometry.
 *
 * Create instances using the [PolygonShape] factory functions or predefined companion factories
 * like [regularPolygon] and [star]. Instances created with equal parameters compare equal to enable
 * caching resolved outlines across recompositions.
 *
 * Shapes cache their resolved outline for their most recent layout size. Sharing a single shape
 * instance across composables with differing sizes is supported, but defining separate instances
 * per size avoids cache thrashing when repeatedly redrawing.
 *
 * Outlines returned by [createOutline] are shared between callers and must not be mutated.
 */
public sealed class PolygonShape : Shape {

    internal abstract fun resolvePolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon

    /**
     * Returns a version token representing dynamic inputs affecting geometry resolution.
     *
     * Used for cache invalidation when a shape's geometry depends on captured external state (such
     * as state read inside a builder lambda). Shapes with static geometry return 0.
     */
    internal open fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int = 0

    public companion object {
        /**
         * Creates a regular polygon [PolygonShape] with [numVertices] vertices, sized to the
         * container's smaller dimension.
         *
         * @sample androidx.compose.foundation.samples.PolygonShapeWithRoundingPercentSample
         * @param numVertices number of vertices, at least 3
         * @param rounding rounding applied to every vertex, resolved against the polygon's radius
         * @throws IllegalArgumentException if [numVertices] is less than 3
         */
        public fun regularPolygon(
            @IntRange(from = 3) numVertices: Int,
            rounding: CornerRounding = CornerRounding.Unrounded,
        ): PolygonShape {
            require(numVertices >= 3) {
                "A polygon requires at least 3 vertices, had $numVertices."
            }
            return RegularPolygonShape(numVertices, rounding)
        }

        /**
         * Creates a star [PolygonShape] with [numPoints] points on each of the outer and inner
         * radii, sized to the container's smaller dimension.
         *
         * @sample androidx.compose.foundation.samples.StarPolygonShapeSample
         * @param numPoints number of points (tips) on the star, at least 2
         * @param innerRadiusRatio ratio of inner radius to outer radius, in the range (0, 1]
         * @param outerRounding rounding for the outer corners, resolved against the star's outer
         *   radius
         * @param innerRounding rounding for the inner corners, resolved the same way; matches
         *   [outerRounding] when not specified
         * @throws IllegalArgumentException if [numPoints] is less than 2 or [innerRadiusRatio] is
         *   outside the range (0, 1]
         */
        public fun star(
            @IntRange(from = 2) numPoints: Int,
            innerRadiusRatio: Float = 0.5f,
            outerRounding: CornerRounding = CornerRounding.Unrounded,
            innerRounding: CornerRounding = outerRounding,
        ): PolygonShape {
            require(numPoints >= 2) { "A star requires at least 2 points, had $numPoints." }
            require(innerRadiusRatio > 0f && innerRadiusRatio <= 1f) {
                "innerRadiusRatio must be in the range (0, 1] but was $innerRadiusRatio."
            }
            return StarPolygonShape(
                numPoints = numPoints,
                innerRadiusRatio = innerRadiusRatio,
                outerRounding = outerRounding,
                innerRounding = innerRounding,
            )
        }

        /**
         * Creates a pill [PolygonShape] with rounded ends along the shorter sides of the layout
         * bounds.
         *
         * @param smoothing the amount by which the arc is "smoothed" by extending the curve from
         *   the circular arc on each endcap to the edge between the endcaps. A value of 0 (no
         *   smoothing) indicates that the corner is rounded by only a circular arc.
         * @throws IllegalArgumentException if [smoothing] is outside the range 0 to 1
         */
        public fun pill(@FloatRange(0.0, 1.0) smoothing: Float = 0f): PolygonShape {
            requireValidSmoothing(smoothing)
            return PillPolygonShape(smoothing)
        }

        /**
         * Creates a star [PolygonShape] with vertices placed along a pill outline.
         *
         * A `pillStar` is like a [pill] except it has inner and outer radii along its pill-shaped
         * outline, just as a [star] has inner and outer radii along a circular outline. This
         * produces an elongated star shape commonly used for badges, chips, and decorative
         * containers.
         *
         * Because vertices curve around the semicircular ends of the pill, outer and inner vertices
         * have different spacing along the curved ends depending on [innerRadiusRatio]. The
         * [vertexSpacing] parameter controls how vertices are spaced along those curved ends:
         * - `vertexSpacing = 0f`: Spaces inner vertices equally along the curved ends (outer
         *   vertices will be further apart).
         * - `vertexSpacing = 1f`: Spaces outer vertices equally along the curved ends (inner
         *   vertices will be closer together).
         * - `vertexSpacing = 0.5f` (default): Uses the average of the inner and outer spacing so
         *   both sets of vertices fall symmetrically along the curved ends.
         *
         * @sample androidx.compose.foundation.samples.PillStarPolygonShapeSample
         * @param numPoints number of points (tips) on the star, at least 2
         * @param innerRadiusRatio ratio of inner radius to outer radius, in the range (0, 1]
         * @param vertexSpacing how vertices on the curved ends are spaced, from 0 (inner vertices
         *   spaced evenly) to 1 (outer vertices spaced evenly); 0.5 takes the average
         * @param startLocation where along the perimeter the outline starts, in the range 0 to 1
         * @param outerRounding rounding for the outer corners, resolved against the star's outer
         *   radius
         * @param innerRounding rounding for the inner corners, resolved the same way; matches
         *   [outerRounding] when not specified
         * @throws IllegalArgumentException if [numPoints] is less than 2, or [innerRadiusRatio] is
         *   outside (0, 1], or [vertexSpacing] or [startLocation] are outside the range 0 to 1
         */
        public fun pillStar(
            @IntRange(from = 2) numPoints: Int,
            innerRadiusRatio: Float = 0.5f,
            vertexSpacing: Float = 0.5f,
            startLocation: Float = 0f,
            outerRounding: CornerRounding = CornerRounding.Unrounded,
            innerRounding: CornerRounding = outerRounding,
        ): PolygonShape {
            require(numPoints >= 2) { "A star requires at least 2 points, had $numPoints." }
            require(innerRadiusRatio > 0f && innerRadiusRatio <= 1f) {
                "innerRadiusRatio must be in the range (0, 1] but was $innerRadiusRatio."
            }
            require(vertexSpacing in 0f..1f) {
                "vertexSpacing must be in the range 0..1, was $vertexSpacing."
            }
            require(startLocation in 0f..1f) {
                "startLocation must be in the range 0..1, was $startLocation."
            }
            return PillStarPolygonShape(
                numPoints = numPoints,
                innerRadiusRatio = innerRadiusRatio,
                vertexSpacing = vertexSpacing,
                startLocation = startLocation,
                outerRounding = outerRounding,
                innerRounding = innerRounding,
            )
        }

        /**
         * Creates a circular [PolygonShape] approximated by [numVertices] rounded vertices.
         *
         * @param numVertices number of vertices used to approximate the circle, at least 3
         * @throws IllegalArgumentException if [numVertices] is less than 3
         */
        public fun circle(@IntRange(from = 3) numVertices: Int = 8): PolygonShape =
            CirclePolygonShape(numVertices)

        /**
         * Creates a rectangular [PolygonShape] filling the layout bounds, with the same [rounding]
         * at every corner.
         *
         * @param rounding rounding for all 4 corners, resolved against the layout bounds
         */
        public fun rectangle(rounding: CornerRounding): PolygonShape =
            RectanglePolygonShape(
                topStartRounding = rounding,
                topEndRounding = rounding,
                bottomEndRounding = rounding,
                bottomStartRounding = rounding,
            )

        /**
         * Creates a rectangular [PolygonShape] filling the layout bounds, with a separate rounding
         * for each corner.
         *
         * Corner roundings mirror automatically with the layout direction.
         *
         * @param topStartRounding rounding for the top start corner, resolved against the layout
         *   bounds
         * @param topEndRounding rounding for the top end corner, resolved the same way
         * @param bottomEndRounding rounding for the bottom end corner, resolved the same way
         * @param bottomStartRounding rounding for the bottom start corner, resolved the same way
         */
        public fun rectangle(
            topStartRounding: CornerRounding = CornerRounding.Unrounded,
            topEndRounding: CornerRounding = CornerRounding.Unrounded,
            bottomEndRounding: CornerRounding = CornerRounding.Unrounded,
            bottomStartRounding: CornerRounding = CornerRounding.Unrounded,
        ): PolygonShape =
            RectanglePolygonShape(
                topStartRounding = topStartRounding,
                topEndRounding = topEndRounding,
                bottomEndRounding = bottomEndRounding,
                bottomStartRounding = bottomStartRounding,
                absolute = false,
            )

        /**
         * Creates an absolute rectangular [PolygonShape] filling the layout bounds, with a separate
         * rounding for each corner.
         *
         * Corner roundings do not swap with the layout direction.
         *
         * @param topLeftRounding rounding for the top left corner, resolved against the layout
         *   bounds
         * @param topRightRounding rounding for the top right corner, resolved the same way
         * @param bottomRightRounding rounding for the bottom right corner, resolved the same way
         * @param bottomLeftRounding rounding for the bottom left corner, resolved the same way
         */
        public fun absoluteRectangle(
            topLeftRounding: CornerRounding = CornerRounding.Unrounded,
            topRightRounding: CornerRounding = CornerRounding.Unrounded,
            bottomRightRounding: CornerRounding = CornerRounding.Unrounded,
            bottomLeftRounding: CornerRounding = CornerRounding.Unrounded,
        ): PolygonShape =
            RectanglePolygonShape(
                topStartRounding = topLeftRounding,
                topEndRounding = topRightRounding,
                bottomEndRounding = bottomRightRounding,
                bottomStartRounding = bottomLeftRounding,
                absolute = true,
            )
    }
}

/**
 * Returns a new [PolygonShape] with [rotation] and [translation] applied to the underlying
 * geometry.
 *
 * [rotation] is applied clockwise in degrees around the geometry's center, and [translation] moves
 * the shape by the specified horizontal and vertical offsets.
 *
 * When [contentScale] is non-null, the transformed geometry is scaled and aligned into the layout
 * container bounds to prevent rotated corners from escaping the container. Pass `contentScale =
 * null` (the default) to apply only the transformation without scaling, or use [scaledToFit] to
 * adjust scaling on an existing shape.
 *
 * This function returns a new [PolygonShape] and should not be used for continuous animations. To
 * animate rotation or translation on every frame, apply
 * [androidx.compose.ui.graphics.graphicsLayer] instead to transform the rendered output without
 * rebuilding geometry.
 *
 * Leaves the receiver untouched and derives a new shape value.
 *
 * @sample androidx.compose.foundation.samples.TransformedPolygonShapeSample
 * @sample androidx.compose.foundation.samples.RuntimeTransformedPolygonShapeSample
 * @param rotation clockwise rotation in degrees, applied around the geometry center
 * @param translation offset by which to move the shape
 * @param contentScale optional scaling policy applied after the transformation, or null to skip
 *   scaling
 * @param alignment where to place the scaled geometry within the layout bounds
 */
@RememberInComposition
public fun PolygonShape.transformed(
    rotation: Float = 0f,
    translation: Offset = Offset.Zero,
    contentScale: ContentScale? = null,
    alignment: Alignment = Alignment.Center,
): PolygonShape {
    if (rotation == 0f && translation == Offset.Zero) {
        return if (contentScale != null) {
            scaledToFit(contentScale, alignment)
        } else {
            this
        }
    }
    if (
        this is TransformedPolygonShape &&
            this.matrix == null &&
            this.contentScale == null &&
            contentScale == null
    ) {
        if (this.translation == Offset.Zero && translation == Offset.Zero) {
            val mergedRotation = (this.rotation + rotation) % 360f
            return if (mergedRotation == 0f) {
                this.inner
            } else {
                TransformedPolygonShape(
                    inner = this.inner,
                    rotation = mergedRotation,
                    translation = Offset.Zero,
                    matrix = null,
                    contentScale = null,
                    alignment = alignment,
                )
            }
        }
        if (this.rotation == 0f && rotation == 0f) {
            val mergedTranslation = this.translation + translation
            return if (mergedTranslation == Offset.Zero) {
                this.inner
            } else {
                TransformedPolygonShape(
                    inner = this.inner,
                    rotation = 0f,
                    translation = mergedTranslation,
                    matrix = null,
                    contentScale = null,
                    alignment = alignment,
                )
            }
        }
    }
    return TransformedPolygonShape(
        inner = this,
        rotation = rotation,
        translation = translation,
        matrix = null,
        contentScale = contentScale,
        alignment = alignment,
    )
}

/**
 * Returns a new [PolygonShape] with [matrix] applied to the underlying geometry.
 *
 * Rotation, scale, and skew act around the resolved geometry's center, changing the shape's
 * orientation and size in place; translation components move the shape as written.
 *
 * When [contentScale] is non-null, the transformed geometry is scaled and aligned into the layout
 * container bounds to prevent rotated corners from escaping the container. Pass `contentScale =
 * null` (the default) to apply only the matrix transformation without scaling, or use [scaledToFit]
 * to adjust scaling on an existing shape.
 *
 * This function returns a new [PolygonShape] and should not be used for continuous animations. To
 * animate rotation or scale on every frame, apply [androidx.compose.ui.graphics.graphicsLayer]
 * instead to transform the rendered output without rebuilding geometry.
 *
 * Leaves the receiver untouched and derives a new shape value holding a snapshot of [matrix].
 *
 * @param matrix affine transformation to apply to the geometry
 * @param contentScale optional scaling policy applied after the transformation, or null to skip
 *   scaling
 * @param alignment where to place the scaled geometry within the layout bounds
 * @throws IllegalArgumentException if [matrix] has perspective components
 */
@RememberInComposition
public fun PolygonShape.transformed(
    matrix: Matrix,
    contentScale: ContentScale? = null,
    alignment: Alignment = Alignment.Center,
): PolygonShape {
    // Cubic control points are mapped independently at resolution, which is exact only for
    // affine transforms; 2D points have z = 0, so only these perspective terms apply.
    val v = matrix.values
    require(v[3] == 0f && v[7] == 0f && v[15] == 1f) {
        "matrix must be an affine transform, without perspective components."
    }
    if (matrix.isIdentity()) {
        return if (contentScale != null) {
            scaledToFit(contentScale, alignment)
        } else {
            this
        }
    }
    if (
        this is TransformedPolygonShape &&
            this.matrix != null &&
            this.contentScale == null &&
            contentScale == null
    ) {
        val merged =
            Matrix().apply {
                setFrom(this@transformed.matrix)
                timesAssign(matrix)
            }
        return if (merged.isIdentity()) {
            this.inner
        } else {
            TransformedPolygonShape(
                inner = this.inner,
                rotation = 0f,
                translation = Offset.Zero,
                matrix = merged,
                contentScale = null,
                alignment = alignment,
            )
        }
    }
    return TransformedPolygonShape(
        inner = this,
        rotation = 0f,
        translation = Offset.Zero,
        matrix = matrix,
        contentScale = contentScale,
        alignment = alignment,
    )
}

/**
 * Returns a new [PolygonShape] scaled and aligned into the layout bounds.
 *
 * [contentScale] selects the scaling policy and [alignment] positions the scaled geometry:
 * - [ContentScale.Fit] (the default) scales uniformly to fit within bounds, preserving aspect ratio
 * - [ContentScale.FillBounds] stretches each axis independently to fill the bounds
 * - [ContentScale.Crop] fills the bounds, preserving aspect ratio and clipping overflow
 * - [ContentScale.None] applies no scaling
 *
 * Measures the resolved geometry to place a shape accurately even when rounding pulls it inside its
 * nominal bounds. Predefined shapes and scope factories are already bounded; scaling is useful
 * after a [transformed] call or when applying non-default placement policies.
 *
 * Leaves the receiver untouched and derives a new shape value.
 *
 * @sample androidx.compose.foundation.samples.ScaledToFitPolygonShapeSample
 * @param contentScale how to scale the resolved geometry into the layout bounds
 * @param alignment where to place the scaled geometry within the layout bounds
 */
@RememberInComposition
public fun PolygonShape.scaledToFit(
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
): PolygonShape = ScaledToFitPolygonShape(this, contentScale, alignment)

/**
 * Receiver scope for the [PolygonShape] builder lambda.
 *
 * Provides the resolution inputs ([size], [layoutDirection], and [Density]) and the factories that
 * create the [PolygonShapeGeometry] the lambda returns. Coordinates are in pixels in the layout's
 * coordinate space; [Dp] values resolve with the current density, and percent values are
 * size-proportional.
 *
 * @sample androidx.compose.foundation.samples.PolygonShapeSample
 * @sample androidx.compose.foundation.samples.DirectionalPolygonShapeSample
 */
public sealed interface PolygonShapeScope : Density {
    /** Size of the shape's layout container in pixels. */
    public val size: Size

    /** Layout direction of the shape. */
    public val layoutDirection: LayoutDirection

    /**
     * Creates a regular polygon with [numVertices] vertices on a circle of [radius] pixels around
     * [center], with the same [rounding] at every vertex.
     *
     * Rounding resolves against the polygon itself, not the container. A percent radius is a
     * percentage of [radius], a [Dp] radius resolves with the current density, and a float radius
     * is a length in pixels.
     *
     * @param numVertices number of vertices, at least 3
     * @param radius radius in pixels of the circle the vertices are placed on
     * @param center center of the polygon in pixels, defaults to the container center
     * @param rounding rounding applied to every vertex
     * @throws IllegalArgumentException if [numVertices] is less than 3
     */
    public fun polygon(
        numVertices: Int,
        radius: Float = size.minDimension / 2f,
        center: Offset = size.center,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ): PolygonShapeGeometry

    /**
     * Creates a regular polygon with [numVertices] vertices on a circle of [radius] pixels around
     * [center], with a separate rounding for each vertex.
     *
     * Rounding resolves against the polygon itself, not the container. A percent radius is a
     * percentage of [radius], a [Dp] radius resolves with the current density, and a float radius
     * is a length in pixels. Vertex i of [perVertexRounding] rounds the vertex at angle
     * 2πi/[numVertices] on the generating circle.
     *
     * @param numVertices number of vertices, at least 3
     * @param perVertexRounding rounding for each vertex, matching [numVertices] in size
     * @param radius radius in pixels of the circle the vertices are placed on
     * @param center center of the polygon in pixels, defaults to the container center
     * @throws IllegalArgumentException if [numVertices] is less than 3, or if [perVertexRounding]
     *   differs from [numVertices] in size
     */
    @Suppress("PrimitiveInCollection")
    public fun polygon(
        numVertices: Int,
        perVertexRounding: List<CornerRounding>,
        radius: Float = size.minDimension / 2f,
        center: Offset = size.center,
    ): PolygonShapeGeometry

    /**
     * Creates polygon geometry from [vertices] in pixels, with the same [rounding] at every vertex.
     *
     * Rounding resolves against the vertex bounds, not the container. A percent radius is a
     * percentage of the bounds' smaller dimension, a [Dp] radius resolves with the current density,
     * and a float radius is a length in pixels.
     *
     * @param vertices vertex positions in pixels, at least 3
     * @param center polygon center in pixels, computed from the geometry when unspecified. An
     *   explicit center can be specified as a custom transformation or morphing anchor.
     * @param rounding rounding applied to every vertex
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries
     */
    @Suppress("PrimitiveInCollection")
    public fun polygon(
        vertices: List<Offset>,
        center: Offset = Offset.Unspecified,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ): PolygonShapeGeometry

    /**
     * Creates polygon geometry from [vertices] in pixels, with a separate rounding for each vertex.
     *
     * Rounding resolves against the vertex bounds, not the container. A percent radius is a
     * percentage of the bounds' smaller dimension, a [Dp] radius resolves with the current density,
     * and a float radius is a length in pixels.
     *
     * @param vertices vertex positions in pixels, at least 3
     * @param perVertexRounding rounding for each vertex, matching [vertices] in size
     * @param center polygon center in pixels, computed from the geometry when unspecified. An
     *   explicit center can be specified as a custom transformation or morphing anchor.
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries, or if
     *   [perVertexRounding] differs from [vertices] in size
     */
    @Suppress("PrimitiveInCollection")
    public fun polygon(
        vertices: List<Offset>,
        perVertexRounding: List<CornerRounding>,
        center: Offset = Offset.Unspecified,
    ): PolygonShapeGeometry
}

/**
 * Geometry describing a polygon's [vertices], [center], and [rounding].
 *
 * The consumer defines the coordinate space. [PolygonShapeScope] factories create geometry in
 * pixels in the layout's coordinate space, while geometry passed directly to [PolygonShape] uses an
 * author-chosen space and is scaled into the layout bounds at resolution time.
 *
 * Rounding resolves in the vertex coordinate space. A float radius is a length in that space, and a
 * percent radius resolves against the smaller dimension of the vertex bounds.
 *
 * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
 * @property vertices vertex positions, at least 3
 * @property center polygon center, computed from the geometry when unspecified. An explicit center
 *   can be specified as a custom transformation or morphing anchor.
 * @property rounding rounding applied to every vertex, [CornerRounding.Unrounded] for geometry
 *   created with per-vertex rounding
 * @property perVertexRounding optional per-vertex rounding, matching [vertices] in size
 */
// TODO: Add vertex overloads taking a public packed array type
//  (an OffsetArray-style value class over an interleaved FloatArray) so each Offset is not boxed.
@Immutable
@Suppress("PrimitiveInCollection")
public class PolygonShapeGeometry
internal constructor(
    public val vertices: List<Offset>,
    public val center: Offset,
    public val rounding: CornerRounding,
    @get:Suppress("NullableCollection") public val perVertexRounding: List<CornerRounding>?,
    internal val roundingReference: Size? = null,
) {

    /**
     * Creates polygon geometry with the same [rounding] at every vertex.
     *
     * @param vertices vertex positions, at least 3
     * @param center polygon center, computed from the geometry when unspecified. An explicit center
     *   can be provided as a custom transformation or morphing anchor.
     * @param rounding rounding applied to every vertex
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries
     */
    @Suppress("PrimitiveInCollection")
    public constructor(
        vertices: List<Offset>,
        center: Offset = Offset.Unspecified,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ) : this(vertices, center, rounding, perVertexRounding = null, roundingReference = null)

    /**
     * Creates polygon geometry with a separate rounding for each vertex.
     *
     * @param vertices vertex positions, at least 3
     * @param perVertexRounding rounding for each vertex, matching [vertices] in size
     * @param center polygon center, computed from the geometry when unspecified. An explicit center
     *   can be provided as a custom transformation or morphing anchor.
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries, or if
     *   [perVertexRounding] differs from [vertices] in size
     */
    @Suppress("PrimitiveInCollection")
    public constructor(
        vertices: List<Offset>,
        perVertexRounding: List<CornerRounding>,
        center: Offset = Offset.Unspecified,
    ) : this(
        vertices,
        center,
        CornerRounding.Unrounded,
        perVertexRounding,
        roundingReference = null,
    )

    init {
        require(vertices.size >= 3) {
            "A polygon requires at least 3 vertices, had ${vertices.size}."
        }
        requireMatchingPerVertexRounding(perVertexRounding, vertices.size)
    }

    /**
     * Resolves this geometry into a [RoundedPolygon]. Percent rounding resolves against the vertex
     * bounds.
     */
    internal fun toRoundedPolygon(density: Density): RoundedPolygon {
        val reference = roundingReference ?: vertexBounds(vertices)
        val verticesArray = FloatArray(vertices.size * 2)
        for (i in vertices.indices) {
            val v = vertices[i]
            verticesArray[i * 2] = v.x
            verticesArray[i * 2 + 1] = v.y
        }
        return RoundedPolygon(
            vertices = verticesArray,
            rounding = rounding.toRoundedPolygonRounding(reference, density),
            perVertexRounding = perVertexRounding.toRoundedPolygonRounding(reference, density),
            centerX = if (center.isSpecified) center.x else Float.MIN_VALUE,
            centerY = if (center.isSpecified) center.y else Float.MIN_VALUE,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PolygonShapeGeometry) return false
        return vertices == other.vertices &&
            center == other.center &&
            rounding == other.rounding &&
            perVertexRounding == other.perVertexRounding &&
            roundingReference == other.roundingReference
    }

    override fun hashCode(): Int {
        var result = vertices.hashCode()
        result = 31 * result + center.hashCode()
        result = 31 * result + rounding.hashCode()
        result = 31 * result + (perVertexRounding?.hashCode() ?: 0)
        result = 31 * result + (roundingReference?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "PolygonShapeGeometry(vertices=$vertices, center=$center, " +
            "rounding=$rounding, perVertexRounding=$perVertexRounding)"

    /**
     * Amount and quality of rounding around a polygon vertex.
     *
     * A corner takes one of three forms:
     * - unrounded, with a radius of 0, keeping the sharp vertex;
     * - rounded with a circular arc, with a [smoothing] of 0, following an approximated circular
     *   arc between adjacent edges;
     * - rounded with continuous curvature, with [smoothing] > 0, where two symmetric cubic Bézier
     *   flanking curves connect the circular arc to the edges. A [smoothing] of 0 keeps a purely
     *   circular arc and 1 maximizes the flanking curves so they meet in the middle.
     */
    @Immutable
    public class CornerRounding
    internal constructor(
        internal val value: Float,
        internal val unit: Int,
        internal val smoothing: Float,
    ) {

        public companion object {
            /** Rounding with a radius of zero, producing a sharp corner at the vertex. */
            public val Unrounded: CornerRounding = CornerRounding(0f, UnitLength, 0f)

            /**
             * How a [CornerRounding] radius value is interpreted: a length in geometry coordinate
             * space.
             */
            internal const val UnitLength = 0

            /** How a [CornerRounding] radius value is interpreted: a length in [Dp]. */
            internal const val UnitDp = 1

            /**
             * How a [CornerRounding] radius value is interpreted: a percentage of a reference size.
             */
            internal const val UnitPercent = 2
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CornerRounding) return false
            return value == other.value && unit == other.unit && smoothing == other.smoothing
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + unit
            result = 31 * result + smoothing.hashCode()
            return result
        }

        override fun toString(): String {
            val valueText =
                when (unit) {
                    UnitDp -> "${value}.dp"
                    UnitPercent -> "$value%"
                    else -> value.toString()
                }
            return "CornerRounding(radius=$valueText, smoothing=$smoothing)"
        }
    }

    public companion object {
        /**
         * Creates a [CornerRounding] with a [radius] in the same coordinate space as the vertices.
         *
         * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
         * @param radius rounding radius in the vertex coordinate space, at least 0
         * @param smoothing the amount by which the arc is "smoothed" by extending the curve from
         *   the inner circular arc to the edge between vertices. A value of 0 (no smoothing)
         *   indicates that the corner is rounded by only a circular arc; there are no flanking
         *   curves. A value of 1 indicates that there is no circular arc in the center; the
         *   flanking curves on either side meet at the middle.
         * @throws IllegalArgumentException if [radius] is negative or [smoothing] is outside the
         *   range 0 to 1
         */
        public fun CornerRounding(radius: Float, smoothing: Float = 0f): CornerRounding {
            require(radius >= 0f) { "radius must be non-negative, was $radius." }
            requireValidSmoothing(smoothing)
            return CornerRounding(radius, CornerRounding.UnitLength, smoothing)
        }

        /**
         * Creates a [CornerRounding] with a [radius] in [Dp], resolved with the shape's density.
         *
         * @param radius rounding radius of the corner, at least 0
         * @param smoothing the amount by which the arc is "smoothed" by extending the curve from
         *   the inner circular arc to the edge between vertices. A value of 0 (no smoothing)
         *   indicates that the corner is rounded by only a circular arc; there are no flanking
         *   curves. A value of 1 indicates that there is no circular arc in the center; the
         *   flanking curves on either side meet at the middle.
         * @throws IllegalArgumentException if [radius] is negative or unspecified, or [smoothing]
         *   is outside the range 0 to 1
         */
        public fun CornerRounding(radius: Dp, smoothing: Float = 0f): CornerRounding {
            require(radius.value >= 0f) { "radius must be non-negative, was $radius." }
            requireValidSmoothing(smoothing)
            return CornerRounding(radius.value, CornerRounding.UnitDp, smoothing)
        }

        /**
         * Creates a [CornerRounding] with a radius as a [percent] of the geometry it applies to.
         *
         * The reference is the generating radius for regular polygons and stars, or the smaller
         * dimension of the vertex bounds for vertex-list geometry, so the radius scales with the
         * shape rather than with the layout container.
         *
         * @sample androidx.compose.foundation.samples.PolygonShapeWithRoundingPercentSample
         * @param percent rounding radius as a percentage of the geometry, in the range 0 to 100
         * @param smoothing the amount by which the arc is "smoothed" by extending the curve from
         *   the inner circular arc to the edge between vertices. A value of 0 (no smoothing)
         *   indicates that the corner is rounded by only a circular arc; there are no flanking
         *   curves. A value of 1 indicates that there is no circular arc in the center; the
         *   flanking curves on either side meet at the middle.
         * @throws IllegalArgumentException if [percent] is outside the range 0 to 100 or
         *   [smoothing] is outside the range 0 to 1
         */
        public fun CornerRounding(
            @IntRange(from = 0, to = 100) percent: Int,
            smoothing: Float = 0f,
        ): CornerRounding {
            require(percent in 0..100) { "percent must be in the range 0..100, was $percent." }
            requireValidSmoothing(smoothing)
            return CornerRounding(percent.toFloat(), CornerRounding.UnitPercent, smoothing)
        }
    }
}

private fun requireValidSmoothing(smoothing: Float) {
    require(smoothing >= 0 && smoothing <= 1f) {
        "smoothing must be in the range 0..1, was $smoothing."
    }
}

@Suppress("PrimitiveInCollection")
private fun vertexBounds(vertices: List<Offset>): Size {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (i in vertices.indices) {
        val v = vertices[i]
        if (v.x < minX) minX = v.x
        if (v.y < minY) minY = v.y
        if (v.x > maxX) maxX = v.x
        if (v.y > maxY) maxY = v.y
    }
    return Size(maxX - minX, maxY - minY)
}

private fun CornerRounding.toRoundedPolygonRounding(
    reference: Size,
    density: Density,
): androidx.graphics.shapes.CornerRounding {
    if (this == CornerRounding.Unrounded) return androidx.graphics.shapes.CornerRounding.Unrounded
    val pxRadius =
        when (unit) {
            CornerRounding.UnitDp -> value * density.density
            CornerRounding.UnitPercent -> value / 100f * reference.minDimension
            else -> value
        }
    return androidx.graphics.shapes.CornerRounding(radius = pxRadius, smoothing = smoothing)
}

@Suppress("PrimitiveInCollection")
private fun List<CornerRounding>?.toRoundedPolygonRounding(
    reference: Size,
    density: Density,
): List<androidx.graphics.shapes.CornerRounding>? {
    return this?.fastMap { it.toRoundedPolygonRounding(reference, density) }
}

private fun requireMatchingPerVertexRounding(
    perVertexRounding: List<CornerRounding>?,
    vertexCount: Int,
) {
    require(perVertexRounding == null || perVertexRounding.size == vertexCount) {
        "perVertexRounding has ${perVertexRounding?.size} entries but the polygon has " +
            "$vertexCount vertices; the sizes must match."
    }
}

private class PolygonShapeScopeImpl(
    override val density: Float,
    override val fontScale: Float,
    override val size: Size,
    override val layoutDirection: LayoutDirection,
) : PolygonShapeScope {

    override fun polygon(
        numVertices: Int,
        radius: Float,
        center: Offset,
        rounding: CornerRounding,
    ): PolygonShapeGeometry {
        require(numVertices >= 3) { "A polygon requires at least 3 vertices, had $numVertices." }
        return PolygonShapeGeometry(
            vertices = regularPolygonVertices(numVertices, radius, center),
            center = center,
            rounding = rounding,
            perVertexRounding = null,
            roundingReference = Size(radius, radius),
        )
    }

    @Suppress("PrimitiveInCollection")
    override fun polygon(
        numVertices: Int,
        perVertexRounding: List<CornerRounding>,
        radius: Float,
        center: Offset,
    ): PolygonShapeGeometry {
        require(numVertices >= 3) { "A polygon requires at least 3 vertices, had $numVertices." }
        requireMatchingPerVertexRounding(perVertexRounding, numVertices)
        return PolygonShapeGeometry(
            vertices = regularPolygonVertices(numVertices, radius, center),
            center = center,
            rounding = CornerRounding.Unrounded,
            perVertexRounding = perVertexRounding,
            roundingReference = Size(radius, radius),
        )
    }

    @Suppress("PrimitiveInCollection")
    override fun polygon(
        vertices: List<Offset>,
        center: Offset,
        rounding: CornerRounding,
    ): PolygonShapeGeometry =
        PolygonShapeGeometry(vertices = vertices, center = center, rounding = rounding)

    @Suppress("PrimitiveInCollection")
    override fun polygon(
        vertices: List<Offset>,
        perVertexRounding: List<CornerRounding>,
        center: Offset,
    ): PolygonShapeGeometry =
        PolygonShapeGeometry(
            vertices = vertices,
            perVertexRounding = perVertexRounding,
            center = center,
        )

    // TODO: Build the vertices without boxing once a packed OffsetArray-style type exists
    //  (see PolygonShapeGeometry).
    @Suppress("PrimitiveInCollection")
    private fun regularPolygonVertices(numVertices: Int, radius: Float, center: Offset) =
        List(numVertices) { i ->
            val angle = PI.toFloat() / numVertices * 2 * i
            Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
        }
}

/** Transforms a [RoundedPolygon] with the given [Matrix], applied about the origin `(0, 0)`. */
private fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon = transformed { x, y ->
    val transformedPoint = matrix.map(Offset(x, y))
    TransformResult(transformedPoint.x, transformedPoint.y)
}

/**
 * Computes the scale-and-place matrix that maps geometry occupying the given bounds into the
 * container [size] according to [contentScale] and [alignment], or null when the bounds are
 * degenerate.
 *
 * Bounds are measured approximately (anchor and control points), matching the convention used by
 * `RoundedPolygon.normalized()` so that fitted shapes scale identically to Material's normalized
 * polygons. [BiasAlignment]s (all the standard [Alignment] values) are applied with float
 * precision; other alignments fall back to the [Alignment.align] integer contract.
 */
private fun computeFitMatrix(
    boundsLeft: Float,
    boundsTop: Float,
    boundsRight: Float,
    boundsBottom: Float,
    size: Size,
    contentScale: ContentScale,
    alignment: Alignment,
    layoutDirection: LayoutDirection,
): Matrix? {
    val width = boundsRight - boundsLeft
    val height = boundsBottom - boundsTop
    if (width <= 0f || height <= 0f) return null
    val factor = contentScale.computeScaleFactor(Size(width, height), size)
    val scaledWidth = width * factor.scaleX
    val scaledHeight = height * factor.scaleY
    val position =
        if (alignment is BiasAlignment) {
            val horizontalBias =
                if (layoutDirection == LayoutDirection.Ltr) alignment.horizontalBias
                else -alignment.horizontalBias
            Offset(
                (size.width - scaledWidth) / 2f * (1f + horizontalBias),
                (size.height - scaledHeight) / 2f * (1f + alignment.verticalBias),
            )
        } else {
            val aligned =
                alignment.align(
                    IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt()),
                    IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    layoutDirection,
                )
            Offset(aligned.x.toFloat(), aligned.y.toFloat())
        }
    // x' = x * scale + offset: the scale is applied first, then the translation.
    val matrix = Matrix()
    matrix.scale(factor.scaleX, factor.scaleY)
    matrix[3, 0] = position.x - boundsLeft * factor.scaleX
    matrix[3, 1] = position.y - boundsTop * factor.scaleY
    return matrix
}

/** Scales and places the polygon within [size] according to [contentScale] and [alignment]. */
private fun RoundedPolygon.scaledInto(
    size: Size,
    contentScale: ContentScale,
    alignment: Alignment,
    layoutDirection: LayoutDirection,
): RoundedPolygon {
    val b = calculateBounds(FloatArray(4), approximate = true)
    val matrix =
        computeFitMatrix(
            boundsLeft = b[0],
            boundsTop = b[1],
            boundsRight = b[2],
            boundsBottom = b[3],
            size = size,
            contentScale = contentScale,
            alignment = alignment,
            layoutDirection = layoutDirection,
        ) ?: return this
    if (matrix.isIdentity()) return this
    return transformed(matrix)
}

/** Uniformly scales and centers the polygon to fit within [size], preserving aspect ratio. */
private fun RoundedPolygon.fitCentered(size: Size): RoundedPolygon =
    scaledInto(size, ContentScale.Fit, Alignment.Center, LayoutDirection.Ltr)

/**
 * Base class for [PolygonShape] implementations. Caches the geometry and outline built by
 * [buildPolygon] per (size, layoutDirection, density, [contentVersion]).
 */
internal abstract class CachingPolygonShape : PolygonShape() {
    /**
     * Computes the geometry for the given resolution inputs. Always builds; implementations
     * override this and must not cache. Callers inside the library go through [resolvePolygon]
     * instead, which memoizes the result.
     */
    internal abstract fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon

    /**
     * Monotonic version of any inputs beyond (size, layoutDirection, density) that determine
     * [buildPolygon]'s result. Implementations whose geometry can change while those keys stay
     * equal (e.g. a builder lambda reading captured state) bump the version so the caches rebuild
     * instead of returning stale results.
     */
    override fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int = 0

    private var cachedPolygonSize: Size? = null
    private var cachedPolygonLayoutDirection: LayoutDirection? = null
    private var cachedPolygonDensity: Density? = null
    private var cachedPolygonVersion = 0
    private var cachedPolygon: RoundedPolygon? = null

    private var cachedOutlineSize: Size? = null
    private var cachedOutlineLayoutDirection: LayoutDirection? = null
    private var cachedOutlineDensity: Density? = null
    private var cachedOutlineVersion = 0
    private var cachedOutline: Outline? = null

    /**
     * Returns the geometry for the given resolution inputs, rebuilding only when they (or the
     * [contentVersion]) change. All consumers that need a materialized polygon resolve through this
     * cache, so a shape instance shared across consumers builds its geometry once per size instead
     * of once per consumer.
     */
    final override fun resolvePolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon =
        resolvePolygon(
            size,
            layoutDirection,
            density,
            contentVersion(size, layoutDirection, density),
        )

    /**
     * [resolvePolygon] with an already-computed [contentVersion], so a caller that has just
     * requested the version (like [createOutline]) does not evaluate it a second time.
     */
    private fun resolvePolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        version: Int,
    ): RoundedPolygon {
        val cached = cachedPolygon
        if (
            cached != null &&
                size == cachedPolygonSize &&
                layoutDirection == cachedPolygonLayoutDirection &&
                density == cachedPolygonDensity &&
                version == cachedPolygonVersion
        ) {
            return cached
        }
        val polygon = buildPolygon(size, layoutDirection, density)
        cachedPolygonSize = size
        cachedPolygonLayoutDirection = layoutDirection
        cachedPolygonDensity = density
        cachedPolygonVersion = version
        cachedPolygon = polygon
        return polygon
    }

    final override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val version = contentVersion(size, layoutDirection, density)
        // the property is mutable, so it cannot be smart-cast across the null check.
        val cached = cachedOutline
        if (
            cached != null &&
                size == cachedOutlineSize &&
                layoutDirection == cachedOutlineLayoutDirection &&
                density == cachedOutlineDensity &&
                version == cachedOutlineVersion
        ) {
            return cached
        }
        val outline =
            Outline.Generic(resolvePolygon(size, layoutDirection, density, version).asComposePath())
        cachedOutlineSize = size
        cachedOutlineLayoutDirection = layoutDirection
        cachedOutlineDensity = density
        cachedOutlineVersion = version
        cachedOutline = outline
        return outline
    }
}

/**
 * Generates a Compose [Path] from the given [RoundedPolygon].
 *
 * @param path An optional [Path] object to rewind and reuse. If not provided, a new one will be
 *   allocated.
 */
internal fun RoundedPolygon.asComposePath(path: Path = Path()): Path {
    path.rewind()
    var first = true
    for (i in cubics.indices) {
        val cubic = cubics[i]
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y,
        )
    }
    path.close()
    return path
}

private class BuilderPolygonShape(val builder: PolygonShapeScope.() -> PolygonShapeGeometry) :
    CachingPolygonShape() {

    private var lastGeometry: PolygonShapeGeometry? = null
    private var version = 0

    // The builder can read captured state that changes while this instance stays the same, so
    // the geometry (value data only, no polygon build) is recomputed on every resolution and the
    // caches are keyed on its value.
    override fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int {
        val geometry =
            PolygonShapeScopeImpl(density.density, density.fontScale, size, layoutDirection)
                .builder()
        if (geometry != lastGeometry) {
            lastGeometry = geometry
            version++
        }
        return version
    }

    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val geometry =
            lastGeometry
                ?: PolygonShapeScopeImpl(density.density, density.fontScale, size, layoutDirection)
                    .builder()
                    .also { lastGeometry = it }
        return geometry.toRoundedPolygon(density)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BuilderPolygonShape) return false
        return builder === other.builder
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class GeometryPolygonShape(val geometry: PolygonShapeGeometry) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon = geometry.toRoundedPolygon(density).fitCentered(size)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeometryPolygonShape) return false
        return geometry == other.geometry
    }

    override fun hashCode(): Int = geometry.hashCode()
}

private class RegularPolygonShape(val numVertices: Int, val rounding: CornerRounding) :
    CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val radius = min(size.width, size.height) / 2f
        val reference = Size(radius, radius)
        return RoundedPolygon(
            numVertices = numVertices,
            radius = radius,
            centerX = size.width / 2f,
            centerY = size.height / 2f,
            rounding = rounding.toRoundedPolygonRounding(reference, density),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegularPolygonShape) return false
        return numVertices == other.numVertices && rounding == other.rounding
    }

    override fun hashCode(): Int = 31 * numVertices + rounding.hashCode()
}

private class StarPolygonShape(
    val numPoints: Int,
    val innerRadiusRatio: Float,
    val outerRounding: CornerRounding,
    val innerRounding: CornerRounding,
) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val radius = min(size.width, size.height) / 2f
        // Rounding resolves against the star's outer radius (geometry-relative), so a percent
        // rounding matches RoundedPolygon.star's rounding-as-fraction-of-radius convention.
        val reference = Size(radius, radius)
        return RoundedPolygon.star(
            numVerticesPerRadius = numPoints,
            radius = radius,
            innerRadius = radius * innerRadiusRatio,
            rounding = outerRounding.toRoundedPolygonRounding(reference, density),
            innerRounding = innerRounding.toRoundedPolygonRounding(reference, density),
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StarPolygonShape) return false
        return numPoints == other.numPoints &&
            innerRadiusRatio == other.innerRadiusRatio &&
            outerRounding == other.outerRounding &&
            innerRounding == other.innerRounding
    }

    override fun hashCode(): Int {
        var result = numPoints
        result = 31 * result + innerRadiusRatio.hashCode()
        result = 31 * result + outerRounding.hashCode()
        result = 31 * result + innerRounding.hashCode()
        return result
    }
}

private class PillPolygonShape(val smoothing: Float) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon =
        RoundedPolygon.pill(
            width = size.width,
            height = size.height,
            smoothing = smoothing,
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PillPolygonShape) return false
        return smoothing == other.smoothing
    }

    override fun hashCode(): Int = smoothing.hashCode()
}

private class PillStarPolygonShape(
    val numPoints: Int,
    val innerRadiusRatio: Float,
    val vertexSpacing: Float,
    val startLocation: Float,
    val outerRounding: CornerRounding,
    val innerRounding: CornerRounding,
) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        // pillStar's width/height describe an inner rectangle that the outer star radius
        // (min(width, height)) extends beyond, so the built geometry spans
        // (|w - h| + 2 * min(w, h)) by (2 * min(w, h)). Inset both dimensions by half the min
        // so the resolved geometry is exactly inscribed in the container.
        val inset = min(size.width, size.height) / 2f
        val width = size.width - inset
        val height = size.height - inset
        // Rounding resolves against the star's outer radius (geometry-relative), matching the
        // star factory's percent convention.
        val radius = min(width, height)
        val reference = Size(radius, radius)
        return RoundedPolygon.pillStar(
            width = width,
            height = height,
            numVerticesPerRadius = numPoints,
            innerRadiusRatio = innerRadiusRatio,
            rounding = outerRounding.toRoundedPolygonRounding(reference, density),
            innerRounding = innerRounding.toRoundedPolygonRounding(reference, density),
            vertexSpacing = vertexSpacing,
            startLocation = startLocation,
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PillStarPolygonShape) return false
        return numPoints == other.numPoints &&
            innerRadiusRatio == other.innerRadiusRatio &&
            vertexSpacing == other.vertexSpacing &&
            startLocation == other.startLocation &&
            outerRounding == other.outerRounding &&
            innerRounding == other.innerRounding
    }

    override fun hashCode(): Int {
        var result = numPoints
        result = 31 * result + innerRadiusRatio.hashCode()
        result = 31 * result + vertexSpacing.hashCode()
        result = 31 * result + startLocation.hashCode()
        result = 31 * result + outerRounding.hashCode()
        result = 31 * result + innerRounding.hashCode()
        return result
    }
}

private class CirclePolygonShape(val numVertices: Int) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon =
        RoundedPolygon.circle(
            numVertices = numVertices,
            radius = min(size.width, size.height) / 2f,
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CirclePolygonShape) return false
        return numVertices == other.numVertices
    }

    override fun hashCode(): Int = numVertices
}

private class RectanglePolygonShape(
    val topStartRounding: CornerRounding,
    val topEndRounding: CornerRounding,
    val bottomEndRounding: CornerRounding,
    val bottomStartRounding: CornerRounding,
    val absolute: Boolean = false,
) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val rtl = !absolute && layoutDirection == LayoutDirection.Rtl
        val bottomRight = if (rtl) bottomStartRounding else bottomEndRounding
        val bottomLeft = if (rtl) bottomEndRounding else bottomStartRounding
        val topLeft = if (rtl) topEndRounding else topStartRounding
        val topRight = if (rtl) topStartRounding else topEndRounding

        return RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            // The rectangle geometry spans the container, so the geometry reference is the size.
            // RoundedPolygon.rectangle orders its vertices bottom right, bottom left, top left,
            // top right.
            perVertexRounding =
                listOf(
                    bottomRight.toRoundedPolygonRounding(size, density),
                    bottomLeft.toRoundedPolygonRounding(size, density),
                    topLeft.toRoundedPolygonRounding(size, density),
                    topRight.toRoundedPolygonRounding(size, density),
                ),
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RectanglePolygonShape) return false
        return topStartRounding == other.topStartRounding &&
            topEndRounding == other.topEndRounding &&
            bottomEndRounding == other.bottomEndRounding &&
            bottomStartRounding == other.bottomStartRounding &&
            absolute == other.absolute
    }

    override fun hashCode(): Int {
        var result = topStartRounding.hashCode()
        result = 31 * result + topEndRounding.hashCode()
        result = 31 * result + bottomEndRounding.hashCode()
        result = 31 * result + bottomStartRounding.hashCode()
        result = 31 * result + absolute.hashCode()
        return result
    }
}

/**
 * Converts a [RoundedCornerShape] to a [PolygonShape].
 *
 * Note: The resulting outline closely approximates the original shape, but is not pixel-identical
 * because [PolygonShape] and [RoundedCornerShape] use different geometry and curve calculations.
 * Avoid comparing them for 1:1 pixel equivalence.
 *
 * @sample androidx.compose.foundation.samples.RoundedCornerShapeToPolygonShapeSample
 */
public fun RoundedCornerShape.toPolygonShape(): PolygonShape =
    CornerShapePolygonShape(this, cut = false, absolute = false)

/**
 * Converts an [AbsoluteRoundedCornerShape] to a [PolygonShape].
 *
 * Corners do not swap with the layout direction.
 *
 * Note: The resulting outline closely approximates the original shape, but is not pixel-identical
 * because [PolygonShape] and [AbsoluteRoundedCornerShape] use different geometry and curve
 * calculations. Avoid comparing them for 1:1 pixel equivalence.
 */
public fun AbsoluteRoundedCornerShape.toPolygonShape(): PolygonShape =
    CornerShapePolygonShape(this, cut = false, absolute = true)

/**
 * Converts a [CutCornerShape] to a [PolygonShape].
 *
 * Note: The resulting outline closely approximates the original shape, but is not pixel-identical
 * because [PolygonShape] and [CutCornerShape] use different geometry and curve calculations. Avoid
 * comparing them for 1:1 pixel equivalence.
 */
public fun CutCornerShape.toPolygonShape(): PolygonShape =
    CornerShapePolygonShape(this, cut = true, absolute = false)

/**
 * Converts an [AbsoluteCutCornerShape] to a [PolygonShape].
 *
 * Corners do not swap with the layout direction.
 *
 * Note: The resulting outline closely approximates the original shape, but is not pixel-identical
 * because [PolygonShape] and [AbsoluteCutCornerShape] use different geometry and curve
 * calculations. Avoid comparing them for 1:1 pixel equivalence.
 */
public fun AbsoluteCutCornerShape.toPolygonShape(): PolygonShape =
    CornerShapePolygonShape(this, cut = true, absolute = true)

/**
 * Polygon conversion of a [CornerBasedShape] ([RoundedCornerShape]/[CutCornerShape] and their
 * absolute variants). Keyed on the [source] shape plus the [cut] and [absolute] flags; the RTL
 * corner swap is resolved at build time, so it is not part of identity.
 */
private class CornerShapePolygonShape(
    val source: CornerBasedShape,
    val cut: Boolean,
    val absolute: Boolean,
) : CachingPolygonShape() {
    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        var ts = source.topStart.toPx(size, density)
        var te = source.topEnd.toPx(size, density)
        var bs = source.bottomStart.toPx(size, density)
        var be = source.bottomEnd.toPx(size, density)
        // CornerBasedShape scales each start/end corner pair down when its sum exceeds the
        // minimum dimension; mirror that so the conversion matches the source outline.
        val minDimension = size.minDimension
        if (ts + bs > minDimension) {
            val scale = minDimension / (ts + bs)
            ts *= scale
            bs *= scale
        }
        if (te + be > minDimension) {
            val scale = minDimension / (te + be)
            te *= scale
            be *= scale
        }
        val rtl = !absolute && layoutDirection == LayoutDirection.Rtl
        val topLeft = if (rtl) te else ts
        val topRight = if (rtl) ts else te
        val bottomLeft = if (rtl) be else bs
        val bottomRight = if (rtl) bs else be
        return if (cut) {
            cutCornerPolygon(size, topLeft, topRight, bottomLeft, bottomRight)
        } else {
            roundedCornerPolygon(size, topLeft, topRight, bottomLeft, bottomRight)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CornerShapePolygonShape) return false
        return cut == other.cut && absolute == other.absolute && source == other.source
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + cut.hashCode()
        result = 31 * result + absolute.hashCode()
        return result
    }
}

/**
 * Builds a rectangular [RoundedPolygon] sized to [size] with per-corner rounding. Vertices are
 * ordered bottom-right, bottom-left, top-left, top-right.
 */
private fun roundedCornerPolygon(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
): RoundedPolygon {
    val width = size.width
    val height = size.height
    return RoundedPolygon(
        vertices =
            floatArrayOf(
                width,
                height, // Bottom-Right
                0f,
                height, // Bottom-Left
                0f,
                0f, // Top-Left
                width,
                0f, // Top-Right
            ),
        perVertexRounding =
            listOf(
                androidx.graphics.shapes.CornerRounding(bottomRight),
                androidx.graphics.shapes.CornerRounding(bottomLeft),
                androidx.graphics.shapes.CornerRounding(topLeft),
                androidx.graphics.shapes.CornerRounding(topRight),
            ),
        centerX = width / 2f,
        centerY = height / 2f,
    )
}

/** Builds a beveled (cut-corner) [RoundedPolygon] sized to [size]. */
private fun cutCornerPolygon(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
): RoundedPolygon {
    val width = size.width
    val height = size.height
    // A zero cut collapses a bevel's two endpoints into one point; skip the duplicates, as
    // RoundedPolygon rejects zero-length edges.
    val vertices = FloatArray(16)
    var count = 0
    fun add(x: Float, y: Float) {
        if (count >= 2 && vertices[count - 2] == x && vertices[count - 1] == y) return
        vertices[count++] = x
        vertices[count++] = y
    }
    add(topLeft, 0f)
    add(width - topRight, 0f)
    add(width, topRight)
    add(width, height - bottomRight)
    add(width - bottomRight, height)
    add(bottomLeft, height)
    add(0f, height - bottomLeft)
    add(0f, topLeft)
    if (count >= 4 && vertices[0] == vertices[count - 2] && vertices[1] == vertices[count - 1]) {
        count -= 2
    }
    return RoundedPolygon(
        vertices = vertices.copyOf(count),
        rounding = androidx.graphics.shapes.CornerRounding.Unrounded,
        centerX = width / 2f,
        centerY = height / 2f,
    )
}

/**
 * A [PolygonShape] that applies a transformation to its inner shape's resolved geometry and
 * optionally scales and aligns it into the container bounds.
 *
 * Holds a private snapshot of the matrix (if provided), so later caller-side mutations do not
 * affect this shape's behavior, equality, or hash code.
 */
private class TransformedPolygonShape(
    val inner: PolygonShape,
    val rotation: Float = 0f,
    val translation: Offset = Offset.Zero,
    matrix: Matrix? = null,
    val contentScale: ContentScale? = null,
    val alignment: Alignment = Alignment.Center,
) : CachingPolygonShape() {

    val matrix: Matrix? = matrix?.let { Matrix().apply { setFrom(it) } }

    /**
     * A wrapper adds no versioned inputs of its own, but its inner shape can (a builder lambda
     * reading captured state), so the inner version must key this shape's caches too; otherwise a
     * wrapper over a builder base would serve stale outlines.
     */
    override fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int = inner.contentVersion(size, layoutDirection, density)

    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val polygon = inner.resolvePolygon(size, layoutDirection, density)
        val transformed =
            if (matrix != null) {
                // Pivot the matrix around the geometry center: the linear components (rotation,
                // scale, skew) then only change the shape's orientation and size, never its
                // position, while translation components pass through unchanged.
                val pivot = Matrix()
                pivot.translate(-polygon.centerX, -polygon.centerY)
                pivot *= matrix
                pivot[3, 0] = pivot[3, 0] + polygon.centerX
                pivot[3, 1] = pivot[3, 1] + polygon.centerY
                polygon.transformed(pivot)
            } else if (rotation != 0f || translation != Offset.Zero) {
                val pivot = Matrix()
                pivot.translate(-polygon.centerX, -polygon.centerY)
                if (rotation != 0f) {
                    pivot.rotateZ(rotation)
                }
                pivot[3, 0] = pivot[3, 0] + polygon.centerX + translation.x
                pivot[3, 1] = pivot[3, 1] + polygon.centerY + translation.y
                polygon.transformed(pivot)
            } else {
                polygon
            }
        return if (contentScale != null) {
            transformed.scaledInto(size, contentScale, alignment, layoutDirection)
        } else {
            transformed
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformedPolygonShape) return false
        return contentScale == other.contentScale &&
            alignment == other.alignment &&
            rotation == other.rotation &&
            translation == other.translation &&
            inner == other.inner &&
            ((matrix == null && other.matrix == null) ||
                (matrix != null &&
                    other.matrix != null &&
                    matrix.values.contentEquals(other.matrix.values)))
    }

    override fun hashCode(): Int {
        var result = inner.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + translation.hashCode()
        result = 31 * result + (matrix?.values?.contentHashCode() ?: 0)
        result = 31 * result + (contentScale?.hashCode() ?: 0)
        result = 31 * result + alignment.hashCode()
        return result
    }
}

/** A [PolygonShape] that scales and places its inner shape's resolved geometry. */
private class ScaledToFitPolygonShape(
    private val inner: PolygonShape,
    private val contentScale: ContentScale,
    private val alignment: Alignment,
) : CachingPolygonShape() {

    /** See [TransformedPolygonShape.contentVersion]. */
    override fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int = inner.contentVersion(size, layoutDirection, density)

    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon =
        inner
            .resolvePolygon(size, layoutDirection, density)
            .scaledInto(size, contentScale, alignment, layoutDirection)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaledToFitPolygonShape) return false
        return contentScale == other.contentScale &&
            alignment == other.alignment &&
            inner == other.inner
    }

    override fun hashCode(): Int {
        var result = inner.hashCode()
        result = 31 * result + contentScale.hashCode()
        result = 31 * result + alignment.hashCode()
        return result
    }
}
