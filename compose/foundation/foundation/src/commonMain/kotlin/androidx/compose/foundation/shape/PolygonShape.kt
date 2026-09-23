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
 * Creates a [PolygonShape] from geometry returned by [builder].
 *
 * - Runs [builder] in [PolygonShapeScope] on each resolution with the resolved
 *   [PolygonShapeScope.size], [PolygonShapeScope.layoutDirection], and [Density].
 * - Re-reads captured state inside [builder] on each resolution and rebuilds the outline when the
 *   returned [PolygonShapeGeometry] changes.
 * - Interprets [PolygonShapeGeometry] coordinates in layout pixels (`(0f, 0f)` at the top-left to
 *   `(size.width, size.height)` at the bottom-right). Call [PolygonShapeTransformScope.scaleToFit]
 *   inside [PolygonShape.transform] to fit normalized or custom coordinates (such as a `[0, 1]`
 *   unit square) into the layout bounds.
 *
 * @sample androidx.compose.foundation.samples.CustomPolygonShapeSample
 * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
 * @param builder lambda run in [PolygonShapeScope] returning the shape's [PolygonShapeGeometry]
 */
@RememberInComposition
public fun PolygonShape(builder: PolygonShapeScope.() -> PolygonShapeGeometry): PolygonShape =
    BuilderPolygonShape(builder)

/**
 * Defines a [Shape] built from rounded polygon geometry.
 *
 * Create instances with [PolygonShape] or companion factories like [regularPolygon] and [star].
 * - Call [transform] to rotate, translate, scale, apply a [Matrix], or fit the geometry into the
 *   layout bounds in place.
 * - Call [copy] before mutating a shared shape to keep the original unchanged.
 *
 * Note: Caches the resolved [Outline] for the most recent layout size, and shares returned
 * [Outline] instances across callers. Do not mutate the [Outline] returned by [createOutline].
 */
public sealed class PolygonShape : Shape {

    /** Builds the un-transformed [RoundedPolygon] for the given resolution inputs. */
    internal abstract fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon

    /** Creates a new instance of this subclass with the same base parameters. */
    internal abstract fun copyBase(): PolygonShape

    /**
     * Returns a version token tracking dynamic inputs that affect [buildPolygon].
     *
     * Bumps when captured external state (such as state read inside a builder lambda) changes.
     */
    internal open fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int = 0

    private var cachedBaseSize: Size? = null
    private var cachedBaseLayoutDirection: LayoutDirection? = null
    private var cachedBaseDensity: Density? = null
    private var cachedBaseVersion = 0
    private var cachedBasePolygon: RoundedPolygon? = null

    private var transformation: PolygonTransformation? = null

    private var cachedOutlinePolygon: RoundedPolygon? = null
    private var cachedOutline: Outline? = null

    /**
     * Transforms this [PolygonShape]'s geometry in place using [block].
     *
     * - Runs [block] in [PolygonShapeTransformScope] during outline resolution, deferring Compose
     *   state reads out of composition so animated transforms update without recomposition.
     * - Replaces any transformation block previously set on this shape via [transform].
     * - Mutates this [PolygonShape] in place; call [copy] first to keep a shared base shape
     *   unchanged.
     *
     * @sample androidx.compose.foundation.samples.TransformedPolygonShapeSample
     * @sample androidx.compose.foundation.samples.RuntimeTransformedPolygonShapeSample
     * @param block transformation block run in [PolygonShapeTransformScope] at resolution time
     */
    @RememberInComposition
    public fun transform(block: PolygonShapeTransformScope.() -> Unit) {
        val current = transformation
        if (current == null) {
            transformation = PolygonTransformation(block)
        } else {
            current.setTransformBlock(block)
        }
        cachedOutline = null
    }

    /**
     * Creates an independent copy of this [PolygonShape] and its transformations.
     *
     * Subsequent in-place mutations via [transform] on the returned copy do not affect this shape,
     * and vice versa.
     *
     * @sample androidx.compose.foundation.samples.RuntimeTransformedPolygonShapeSample
     * @return a new [PolygonShape] with the same base geometry and transformation configuration
     */
    @RememberInComposition
    public fun copy(): PolygonShape =
        copyBase().also { copy -> copy.transformation = this.transformation?.copy() }

    private fun resolveBasePolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val version = contentVersion(size, layoutDirection, density)
        val cached = cachedBasePolygon
        if (
            cached != null &&
                size == cachedBaseSize &&
                layoutDirection == cachedBaseLayoutDirection &&
                density == cachedBaseDensity &&
                version == cachedBaseVersion
        ) {
            return cached
        }
        return buildPolygon(size, layoutDirection, density).also {
            cachedBaseSize = size
            cachedBaseLayoutDirection = layoutDirection
            cachedBaseDensity = density
            cachedBaseVersion = version
            cachedBasePolygon = it
        }
    }

    internal fun resolvePolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val basePolygon = resolveBasePolygon(size, layoutDirection, density)
        val currentTransform = transformation ?: return basePolygon
        return currentTransform.resolve(basePolygon, size, layoutDirection, density)
    }

    final override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val polygon = resolvePolygon(size, layoutDirection, density)
        val cached = cachedOutline
        if (cached != null && polygon === cachedOutlinePolygon) {
            return cached
        }
        return Outline.Generic(polygon.asComposePath()).also {
            cachedOutlinePolygon = polygon
            cachedOutline = it
        }
    }

    public companion object {
        /**
         * Creates a regular polygon [PolygonShape] inscribed in the container's smaller dimension.
         *
         * @sample androidx.compose.foundation.samples.PolygonShapeWithRoundingFractionSample
         * @param numVertices number of vertices, at least 3
         * @param rounding corner rounding resolved against the polygon's radius
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
         * Creates a star [PolygonShape] inscribed in the container's smaller dimension.
         *
         * @sample androidx.compose.foundation.samples.StarPolygonShapeSample
         * @param numPoints number of star tips, at least 2
         * @param innerRadiusRatio ratio of inner radius to outer radius, in `(0, 1]`
         * @param outerRounding rounding for outer tips, resolved against the star's outer radius
         * @param innerRounding rounding for inner corners, resolved against the star's outer radius
         * @throws IllegalArgumentException if [numPoints] is less than 2 or [innerRadiusRatio] is
         *   outside `(0, 1]`
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
         * Creates a pill [PolygonShape] with rounded ends along the shorter container sides.
         *
         * @param smoothing transition smoothness between the endcap arcs and straight edges, in
         *   `0f..1f` (`0f` keeps a purely circular arc)
         * @throws IllegalArgumentException if [smoothing] is outside `0f..1f`
         */
        public fun pill(@FloatRange(0.0, 1.0) smoothing: Float = 0f): PolygonShape {
            requireValidSmoothing(smoothing)
            return PillPolygonShape(smoothing)
        }

        /**
         * Creates a star [PolygonShape] with vertices distributed along a [pill] outline.
         *
         * [vertexSpacing] controls how vertices are spaced around the curved ends:
         * - `0f`: spaces inner vertices evenly along the curved ends
         * - `1f`: spaces outer vertices evenly along the curved ends
         * - `0.5f` (default): averages inner and outer spacing symmetrically
         *
         * @sample androidx.compose.foundation.samples.PillStarPolygonShapeSample
         * @param numPoints number of star tips, at least 2
         * @param innerRadiusRatio ratio of inner radius to outer radius, in `(0, 1]`
         * @param vertexSpacing curved-end vertex spacing policy, in `0f..1f`
         * @param startLocation perimeter offset where the outline starts, in `0f..1f`
         * @param outerRounding rounding for outer tips, resolved against the star's outer radius
         * @param innerRounding rounding for inner corners, resolved against the star's outer radius
         * @throws IllegalArgumentException if [numPoints] is less than 2, [innerRadiusRatio] is
         *   outside `(0, 1]`, or [vertexSpacing] or [startLocation] is outside `0f..1f`
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
         * @param numVertices number of vertices approximating the circle, at least 3
         * @throws IllegalArgumentException if [numVertices] is less than 3
         */
        public fun circle(@IntRange(from = 3) numVertices: Int = 8): PolygonShape =
            CirclePolygonShape(numVertices)

        /**
         * Creates a rectangular [PolygonShape] filling the layout bounds with uniform [rounding].
         *
         * @param rounding corner rounding for all 4 corners, resolved against the layout bounds
         */
        public fun rectangle(rounding: CornerRounding): PolygonShape =
            RectanglePolygonShape(
                topStartRounding = rounding,
                topEndRounding = rounding,
                bottomEndRounding = rounding,
                bottomStartRounding = rounding,
            )

        /**
         * Creates a rectangular [PolygonShape] with independent layout-direction-aware corners.
         *
         * Corner roundings mirror automatically in [LayoutDirection.Rtl].
         *
         * @param topStartRounding top-start corner rounding, resolved against the layout bounds
         * @param topEndRounding top-end corner rounding, resolved against the layout bounds
         * @param bottomEndRounding bottom-end corner rounding, resolved against the layout bounds
         * @param bottomStartRounding bottom-start corner rounding, resolved against the layout
         *   bounds
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
         * Creates a rectangular [PolygonShape] with fixed left/right corner roundings.
         *
         * Corner roundings do not mirror with [LayoutDirection].
         *
         * @param topLeftRounding top-left corner rounding, resolved against the layout bounds
         * @param topRightRounding top-right corner rounding, resolved against the layout bounds
         * @param bottomRightRounding bottom-right corner rounding, resolved against the layout
         *   bounds
         * @param bottomLeftRounding bottom-left corner rounding, resolved against the layout bounds
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
 * Transforms a [PolygonShape]'s geometry inside [PolygonShape.transform].
 *
 * Exposes the layout resolution inputs ([size], [layoutDirection], and [Density]). Operations
 * ([rotate], [translate], [scale], [transform], and [scaleToFit]) concatenate onto the
 * transformation matrix in the exact order they are called (for example, [scale] followed by
 * [translate] differs from [translate] followed by [scale]).
 *
 * @sample androidx.compose.foundation.samples.TransformedPolygonShapeSample
 * @sample androidx.compose.foundation.samples.RuntimeTransformedPolygonShapeSample
 */
public sealed interface PolygonShapeTransformScope : Density {
    /** Size of the shape's layout container in pixels. */
    public val size: Size

    /** Layout direction of the shape. */
    public val layoutDirection: LayoutDirection

    /**
     * Rotates the polygon clockwise by [degrees] around [pivot].
     *
     * @param degrees clockwise rotation in degrees
     * @param pivot rotation center in pixels, or [Offset.Unspecified] for the polygon's center
     */
    public fun rotate(degrees: Float, pivot: Offset = Offset.Unspecified)

    /**
     * Translates the polygon by ([x], [y]) pixels.
     *
     * @param x horizontal offset in pixels
     * @param y vertical offset in pixels
     */
    public fun translate(x: Float = 0f, y: Float = 0f)

    /**
     * Scales the polygon by [scaleX] horizontally and [scaleY] vertically around [pivot].
     *
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor, defaults to [scaleX] for uniform scaling
     * @param pivot scale center in pixels, or [Offset.Unspecified] for the polygon's center
     */
    public fun scale(
        scaleX: Float,
        scaleY: Float = scaleX,
        pivot: Offset = Offset.Unspecified,
    )

    /**
     * Multiplies the current transformation by the affine [matrix] around the polygon's center.
     *
     * Linear components (rotation, scale, skew) pivot around the polygon's center, while
     * translation components offset the polygon directly.
     *
     * @param matrix affine transformation matrix to multiply
     * @throws IllegalArgumentException if [matrix] contains perspective components
     */
    public fun transform(matrix: Matrix)

    /**
     * Scales and aligns the polygon's bounding box into [size] in call order.
     *
     * - Call before [rotate], [scale], or [translate] to fit the unrotated polygon first, then
     *   transform it around its fitted center.
     * - Call after [rotate] or [transform] to fit the transformed bounding box into [size].
     * - [ContentScale.Fit] (default) scales uniformly to fit within bounds, preserving aspect ratio
     * - [ContentScale.FillBounds] stretches each axis independently to fill the bounds
     * - [ContentScale.Crop] fills the bounds, preserving aspect ratio and clipping overflow
     * - [ContentScale.None] applies no scaling
     *
     * @sample androidx.compose.foundation.samples.ScaledToFitPolygonShapeSample
     * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
     * @param contentScale policy for scaling the polygon's bounding box into the layout bounds
     * @param alignment placement of the scaled polygon within the layout bounds
     */
    public fun scaleToFit(
        contentScale: ContentScale = ContentScale.Fit,
        alignment: Alignment = Alignment.Center,
    )
}

/**
 * Builds [PolygonShapeGeometry] for a [PolygonShape] at layout resolution.
 *
 * Exposes [size], [layoutDirection], and [Density] along with [polygon] factories. Coordinates are
 * in layout pixels; [CornerRounding.dp] resolves with [Density], and [CornerRounding.fraction]
 * resolves against the polygon's geometry.
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
     * Creates a regular polygon with [numVertices] vertices on a circle of [radius] pixels.
     *
     * [CornerRounding.fraction] resolves against [radius], and [CornerRounding.dp] resolves with
     * the current [Density].
     *
     * @param numVertices number of vertices, at least 3
     * @param radius circle radius in pixels
     * @param center polygon center in pixels, defaults to [Size.center]
     * @param rounding corner rounding applied to every vertex
     * @throws IllegalArgumentException if [numVertices] is less than 3
     */
    public fun polygon(
        numVertices: Int,
        radius: Float = size.minDimension / 2f,
        center: Offset = size.center,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ): PolygonShapeGeometry

    /**
     * Creates a regular polygon with [numVertices] vertices and per-vertex [perVertexRounding].
     *
     * [perVertexRounding] applies to vertices in clockwise order starting from the right (`0`
     * degrees). [CornerRounding.fraction] resolves against [radius], and [CornerRounding.dp]
     * resolves with the current [Density].
     *
     * @param numVertices number of vertices, at least 3
     * @param perVertexRounding rounding for each vertex, matching [numVertices] in size
     * @param radius circle radius in pixels
     * @param center polygon center in pixels, defaults to [Size.center]
     * @throws IllegalArgumentException if [numVertices] is less than 3 or [perVertexRounding]
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
     * Creates polygon geometry from [vertices] with uniform [rounding].
     *
     * [CornerRounding.fraction] resolves against the smaller dimension of the vertex bounds, and
     * [CornerRounding.dp] resolves with [Density].
     *
     * @param vertices ordered vertex positions in pixels (can be transformed or fitted later via
     *   [PolygonShape.transform]), at least 3
     * @param center polygon center in pixels, or [Offset.Unspecified] to compute from [vertices]
     * @param rounding corner rounding applied to every vertex
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries
     */
    @Suppress("PrimitiveInCollection")
    public fun polygon(
        vertices: List<Offset>,
        center: Offset = Offset.Unspecified,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ): PolygonShapeGeometry

    /**
     * Creates polygon geometry from [vertices] with per-vertex [perVertexRounding].
     *
     * [CornerRounding.fraction] resolves against the smaller dimension of the vertex bounds, and
     * [CornerRounding.dp] resolves with [Density].
     *
     * @param vertices ordered vertex positions in pixels (can be transformed or fitted later via
     *   [PolygonShape.transform]), at least 3
     * @param perVertexRounding rounding for each vertex, matching [vertices] in size
     * @param center polygon center in pixels, or [Offset.Unspecified] to compute from [vertices]
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries or
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
 * Defines a polygon's [vertices], [center], and corner rounding.
 *
 * [CornerRounding.fraction] resolves against the smaller dimension of the vertex bounds, and
 * [CornerRounding.dp] resolves with [Density].
 *
 * @sample androidx.compose.foundation.samples.UnitSpacePolygonShapeSample
 * @property vertices ordered vertex positions in pixels (can be transformed via
 *   [PolygonShape.transform]), at least 3
 * @property center polygon center in pixels, or [Offset.Unspecified] when computed from [vertices]
 * @property rounding uniform vertex rounding, or [CornerRounding.Unrounded] when using
 *   [perVertexRounding]
 * @property perVertexRounding optional per-vertex rounding matching [vertices] in size
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
     * Creates polygon geometry with uniform [rounding] at every vertex.
     *
     * @param vertices ordered vertex positions in pixels (can be transformed via
     *   [PolygonShape.transform]), at least 3
     * @param center polygon center in pixels, or [Offset.Unspecified] to compute from [vertices]
     * @param rounding corner rounding applied to every vertex
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries
     */
    @Suppress("PrimitiveInCollection")
    public constructor(
        vertices: List<Offset>,
        center: Offset = Offset.Unspecified,
        rounding: CornerRounding = CornerRounding.Unrounded,
    ) : this(vertices, center, rounding, perVertexRounding = null, roundingReference = null)

    /**
     * Creates polygon geometry with per-vertex [perVertexRounding].
     *
     * @param vertices ordered vertex positions in pixels (can be transformed via
     *   [PolygonShape.transform]), at least 3
     * @param perVertexRounding rounding for each vertex, matching [vertices] in size
     * @param center polygon center in pixels, or [Offset.Unspecified] to compute from [vertices]
     * @throws IllegalArgumentException if [vertices] has fewer than 3 entries or
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
     * Resolves this geometry into a [RoundedPolygon]. Fraction rounding resolves against the vertex
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
}

/**
 * Defines the radius and smoothness of rounding at a polygon vertex.
 *
 * - [Unrounded] (`radius = 0`) keeps a sharp corner.
 * - `smoothing = 0f` rounds the corner with a circular arc.
 * - `smoothing > 0f` extends symmetric cubic Bézier flanking curves from the circular arc toward
 *   the adjacent edges (`1f` maximizes the flanking curves).
 *
 * Create instances with [CornerRounding.dp], [CornerRounding.fraction], or [Unrounded].
 */
@Immutable
public class CornerRounding
internal constructor(
    internal val value: Float,
    internal val unit: Int,
    internal val smoothing: Float,
) {

    public companion object {
        /** Sharp corner with a rounding radius of zero. */
        public val Unrounded: CornerRounding = CornerRounding(0f, UnitDp, 0f)

        internal const val UnitDp = 0
        internal const val UnitFraction = 1

        /**
         * Creates a [CornerRounding] with a [radius] in [Dp].
         *
         * @sample androidx.compose.foundation.samples.PolygonShapeSample
         * @param radius corner rounding radius, at least `0.dp`
         * @param smoothing transition smoothness from the circular arc to adjacent edges, in
         *   `0f..1f`
         * @throws IllegalArgumentException if [radius] is negative/unspecified or [smoothing] is
         *   outside `0f..1f`
         */
        public fun dp(
            radius: Dp,
            @FloatRange(from = 0.0, to = 1.0) smoothing: Float = 0f,
        ): CornerRounding {
            require(radius.value >= 0f) { "radius must be non-negative, was $radius." }
            requireValidSmoothing(smoothing)
            return CornerRounding(radius.value, UnitDp, smoothing)
        }

        /**
         * Creates a [CornerRounding] with a radius as a [fraction] of the polygon's geometry.
         *
         * Resolves against the generating radius for regular polygons and stars, or the smaller
         * dimension of the vertex bounds for vertex-list geometry.
         *
         * @sample androidx.compose.foundation.samples.PolygonShapeWithRoundingFractionSample
         * @param fraction rounding radius as a fraction of the geometry, in `0f..1f`
         * @param smoothing transition smoothness from the circular arc to adjacent edges, in
         *   `0f..1f`
         * @throws IllegalArgumentException if [fraction] or [smoothing] is outside `0f..1f`
         */
        public fun fraction(
            @FloatRange(from = 0.0, to = 1.0) fraction: Float,
            @FloatRange(from = 0.0, to = 1.0) smoothing: Float = 0f,
        ): CornerRounding {
            require(fraction in 0f..1f) { "fraction must be in the range 0..1, was $fraction." }
            requireValidSmoothing(smoothing)
            return CornerRounding(fraction, UnitFraction, smoothing)
        }
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
                else -> "fraction=$value"
            }
        return "CornerRounding($valueText, smoothing=$smoothing)"
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
    if (value == 0f) return androidx.graphics.shapes.CornerRounding.Unrounded
    val pxRadius =
        when (unit) {
            CornerRounding.UnitDp -> value * density.density
            else -> value * reference.minDimension
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
    override var density: Float,
    override var fontScale: Float,
    override var size: Size,
    override var layoutDirection: LayoutDirection,
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

private class PolygonShapeTransformScopeImpl : PolygonShapeTransformScope {
    override var density: Float = 1f
    override var fontScale: Float = 1f
    override var size: Size = Size.Zero
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    private var basePolygon: RoundedPolygon? = null
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private val boundsBuffer = FloatArray(4)

    val matrix: Matrix = Matrix()
    var hasMatrix: Boolean = false

    fun reset(
        basePolygon: RoundedPolygon,
        density: Float,
        fontScale: Float,
        size: Size,
        layoutDirection: LayoutDirection,
    ) {
        this.basePolygon = basePolygon
        this.density = density
        this.fontScale = fontScale
        this.size = size
        this.layoutDirection = layoutDirection
        this.centerX = basePolygon.centerX
        this.centerY = basePolygon.centerY
        if (this.hasMatrix) {
            this.matrix.reset()
            this.hasMatrix = false
        }
    }

    override fun rotate(degrees: Float, pivot: Offset) {
        if (degrees == 0f) return
        hasMatrix = true
        val px = if (pivot.isSpecified) pivot.x else centerX
        val py = if (pivot.isSpecified) pivot.y else centerY
        val radians = degrees * (PI / 180.0)
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        val m00 = matrix[0, 0]
        val m01 = matrix[0, 1]
        val m10 = matrix[1, 0]
        val m11 = matrix[1, 1]
        val tx = matrix[3, 0] - px
        val ty = matrix[3, 1] - py
        matrix[0, 0] = c * m00 - s * m01
        matrix[0, 1] = s * m00 + c * m01
        matrix[1, 0] = c * m10 - s * m11
        matrix[1, 1] = s * m10 + c * m11
        matrix[3, 0] = c * tx - s * ty + px
        matrix[3, 1] = s * tx + c * ty + py
    }

    override fun translate(x: Float, y: Float) {
        if (x == 0f && y == 0f) return
        hasMatrix = true
        matrix[3, 0] += x
        matrix[3, 1] += y
    }

    override fun scale(scaleX: Float, scaleY: Float, pivot: Offset) {
        if (scaleX == 1f && scaleY == 1f) return
        hasMatrix = true
        val px = if (pivot.isSpecified) pivot.x else centerX
        val py = if (pivot.isSpecified) pivot.y else centerY
        matrix[0, 0] *= scaleX
        matrix[1, 0] *= scaleX
        matrix[3, 0] = (matrix[3, 0] - px) * scaleX + px
        matrix[0, 1] *= scaleY
        matrix[1, 1] *= scaleY
        matrix[3, 1] = (matrix[3, 1] - py) * scaleY + py
    }

    override fun transform(matrix: Matrix) {
        // Cubic control points are mapped independently at resolution, which is exact only for
        // affine transforms; 2D points have z = 0, so only these perspective terms apply.
        val v = matrix.values
        require(v[3] == 0f && v[7] == 0f && v[15] == 1f) {
            "matrix must be an affine transform, without perspective components."
        }
        if (matrix.isIdentity()) return
        hasMatrix = true
        this.matrix[3, 0] -= centerX
        this.matrix[3, 1] -= centerY
        this.matrix *= matrix
        this.matrix[3, 0] += centerX
        this.matrix[3, 1] += centerY
    }

    override fun scaleToFit(contentScale: ContentScale, alignment: Alignment) {
        val polygon = basePolygon ?: return
        val currentMatrix = if (hasMatrix && !matrix.isIdentity()) matrix else null
        polygon.calculateTransformedBounds(currentMatrix, boundsBuffer)
        val boundsLeft = boundsBuffer[0]
        val boundsTop = boundsBuffer[1]
        val width = boundsBuffer[2] - boundsLeft
        val height = boundsBuffer[3] - boundsTop
        if (width <= 0f || height <= 0f) return

        val factor = contentScale.computeScaleFactor(Size(width, height), size)
        val position =
            computeAlignedOffset(
                scaledWidth = width * factor.scaleX,
                scaledHeight = height * factor.scaleY,
                containerSize = size,
                alignment = alignment,
                layoutDirection = layoutDirection,
            )
        val scaleX = factor.scaleX
        val scaleY = factor.scaleY
        val offsetX = position.x - boundsLeft * scaleX
        val offsetY = position.y - boundsTop * scaleY
        if (scaleX == 1f && scaleY == 1f && offsetX == 0f && offsetY == 0f) return

        hasMatrix = true
        matrix[0, 0] *= scaleX
        matrix[1, 0] *= scaleX
        matrix[3, 0] = matrix[3, 0] * scaleX + offsetX
        matrix[0, 1] *= scaleY
        matrix[1, 1] *= scaleY
        matrix[3, 1] = matrix[3, 1] * scaleY + offsetY
        centerX = centerX * scaleX + offsetX
        centerY = centerY * scaleY + offsetY
    }
}

/**
 * Encapsulates a [PolygonShape]'s in-place transformation block, reusable scope, and single-entry
 * transformed [RoundedPolygon] cache. Allocated lazily only when [PolygonShape.transform] is
 * called.
 */
private class PolygonTransformation(
    private var transformBlock: PolygonShapeTransformScope.() -> Unit
) {
    private var scope: PolygonShapeTransformScopeImpl? = null

    private var cachedBasePolygon: RoundedPolygon? = null
    private var cachedMatrixValues: FloatArray? = null
    private var cachedTransformedPolygon: RoundedPolygon? = null

    fun setTransformBlock(block: PolygonShapeTransformScope.() -> Unit) {
        transformBlock = block
        cachedTransformedPolygon = null
    }

    fun copy(): PolygonTransformation = PolygonTransformation(transformBlock)

    fun resolve(
        basePolygon: RoundedPolygon,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val activeScope = evaluateScope(basePolygon, size, layoutDirection, density)
        if (!activeScope.hasMatrix || activeScope.matrix.isIdentity()) {
            return basePolygon
        }
        val matrix = activeScope.matrix

        val cached = cachedTransformedPolygon
        if (
            cached != null &&
                basePolygon === cachedBasePolygon &&
                matrix.values.contentEquals(cachedMatrixValues)
        ) {
            return cached
        }

        val transformed = basePolygon.transformed(matrix)
        cachedBasePolygon = basePolygon
        val buffer = cachedMatrixValues ?: FloatArray(16).also { cachedMatrixValues = it }
        matrix.values.copyInto(buffer)
        cachedTransformedPolygon = transformed
        return transformed
    }

    private fun evaluateScope(
        basePolygon: RoundedPolygon,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): PolygonShapeTransformScopeImpl {
        val instance = scope ?: PolygonShapeTransformScopeImpl().also { scope = it }
        instance.reset(
            basePolygon = basePolygon,
            density = density.density,
            fontScale = density.fontScale,
            size = size,
            layoutDirection = layoutDirection,
        )
        transformBlock(instance)
        return instance
    }
}

/** Transforms a [RoundedPolygon] with the given [Matrix], applied about the origin `(0, 0)`. */
private fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon = transformed { x, y ->
    val transformedPoint = matrix.map(Offset(x, y))
    TransformResult(transformedPoint.x, transformedPoint.y)
}

/**
 * Writes `[left, top, right, bottom]` of [this] polygon's control points (mapped by [matrix] if
 * non-null) into [outBounds].
 */
private fun RoundedPolygon.calculateTransformedBounds(matrix: Matrix?, outBounds: FloatArray) {
    if (matrix == null) {
        calculateBounds(outBounds, approximate = true)
        return
    }
    val m00 = matrix[0, 0]
    val m01 = matrix[0, 1]
    val m10 = matrix[1, 0]
    val m11 = matrix[1, 1]
    val m30 = matrix[3, 0]
    val m31 = matrix[3, 1]
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    val cubics = this.cubics
    for (i in 0 until cubics.size) {
        val c = cubics[i]
        val a0x = m00 * c.anchor0X + m10 * c.anchor0Y + m30
        val a0y = m01 * c.anchor0X + m11 * c.anchor0Y + m31
        if (a0x < minX) minX = a0x
        if (a0y < minY) minY = a0y
        if (a0x > maxX) maxX = a0x
        if (a0y > maxY) maxY = a0y

        val c0x = m00 * c.control0X + m10 * c.control0Y + m30
        val c0y = m01 * c.control0X + m11 * c.control0Y + m31
        if (c0x < minX) minX = c0x
        if (c0y < minY) minY = c0y
        if (c0x > maxX) maxX = c0x
        if (c0y > maxY) maxY = c0y

        val c1x = m00 * c.control1X + m10 * c.control1Y + m30
        val c1y = m01 * c.control1X + m11 * c.control1Y + m31
        if (c1x < minX) minX = c1x
        if (c1y < minY) minY = c1y
        if (c1x > maxX) maxX = c1x
        if (c1y > maxY) maxY = c1y
    }
    outBounds[0] = minX
    outBounds[1] = minY
    outBounds[2] = maxX
    outBounds[3] = maxY
}

private fun computeAlignedOffset(
    scaledWidth: Float,
    scaledHeight: Float,
    containerSize: Size,
    alignment: Alignment,
    layoutDirection: LayoutDirection,
): Offset {
    if (alignment is BiasAlignment) {
        val horizontalBias =
            if (layoutDirection == LayoutDirection.Ltr) alignment.horizontalBias
            else -alignment.horizontalBias
        return Offset(
            (containerSize.width - scaledWidth) / 2f * (1f + horizontalBias),
            (containerSize.height - scaledHeight) / 2f * (1f + alignment.verticalBias),
        )
    }
    val aligned =
        alignment.align(
            IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt()),
            IntSize(containerSize.width.roundToInt(), containerSize.height.roundToInt()),
            layoutDirection,
        )
    return Offset(aligned.x.toFloat(), aligned.y.toFloat())
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
    PolygonShape() {

    private val scope = PolygonShapeScopeImpl(1f, 1f, Size.Zero, LayoutDirection.Ltr)
    private var lastGeometry: PolygonShapeGeometry? = null
    private var version = 0

    // Size-independent geometry (such as a unit-space definition fitted via scaleToFit) keeps the
    // same value across resizes, so the built polygon is memoized on the geometry instance.
    private var builtGeometry: PolygonShapeGeometry? = null
    private var builtDensity: Density? = null
    private var builtPolygon: RoundedPolygon? = null

    override fun copyBase(): PolygonShape = BuilderPolygonShape(builder)

    private fun evaluateGeometry(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): PolygonShapeGeometry {
        scope.density = density.density
        scope.fontScale = density.fontScale
        scope.size = size
        scope.layoutDirection = layoutDirection
        return scope.builder()
    }

    // The builder can read captured state that changes while this instance stays the same, so
    // the geometry (value data only, no polygon build) is recomputed on every resolution and the
    // caches are keyed on its value.
    override fun contentVersion(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Int {
        val geometry = evaluateGeometry(size, layoutDirection, density)
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
                ?: evaluateGeometry(size, layoutDirection, density).also { lastGeometry = it }
        val built = builtPolygon
        if (built != null && geometry === builtGeometry && density == builtDensity) {
            return built
        }
        return geometry.toRoundedPolygon(density).also {
            builtGeometry = geometry
            builtDensity = density
            builtPolygon = it
        }
    }
}

private class RegularPolygonShape(val numVertices: Int, val rounding: CornerRounding) :
    PolygonShape() {
    override fun copyBase(): PolygonShape = RegularPolygonShape(numVertices, rounding)

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
}

private class StarPolygonShape(
    val numPoints: Int,
    val innerRadiusRatio: Float,
    val outerRounding: CornerRounding,
    val innerRounding: CornerRounding,
) : PolygonShape() {
    override fun copyBase(): PolygonShape =
        StarPolygonShape(numPoints, innerRadiusRatio, outerRounding, innerRounding)

    override fun buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon {
        val radius = min(size.width, size.height) / 2f
        // Rounding resolves against the star's outer radius (geometry-relative), so a fraction
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
}

private class PillPolygonShape(val smoothing: Float) : PolygonShape() {
    override fun copyBase(): PolygonShape = PillPolygonShape(smoothing)

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
}

private class PillStarPolygonShape(
    val numPoints: Int,
    val innerRadiusRatio: Float,
    val vertexSpacing: Float,
    val startLocation: Float,
    val outerRounding: CornerRounding,
    val innerRounding: CornerRounding,
) : PolygonShape() {
    override fun copyBase(): PolygonShape =
        PillStarPolygonShape(
            numPoints,
            innerRadiusRatio,
            vertexSpacing,
            startLocation,
            outerRounding,
            innerRounding,
        )

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
        // star factory's fraction convention.
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
}

private class CirclePolygonShape(val numVertices: Int) : PolygonShape() {
    override fun copyBase(): PolygonShape = CirclePolygonShape(numVertices)

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
}

private class RectanglePolygonShape(
    val topStartRounding: CornerRounding,
    val topEndRounding: CornerRounding,
    val bottomEndRounding: CornerRounding,
    val bottomStartRounding: CornerRounding,
    val absolute: Boolean = false,
) : PolygonShape() {
    override fun copyBase(): PolygonShape =
        RectanglePolygonShape(
            topStartRounding,
            topEndRounding,
            bottomEndRounding,
            bottomStartRounding,
            absolute,
        )

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
}

/**
 * Converts a [RoundedCornerShape] to a [PolygonShape].
 *
 * Note: Approximates [RoundedCornerShape] using cubic Bézier curves and is not pixel-identical.
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
 * absolute variants). The RTL corner swap is resolved at build time.
 */
private class CornerShapePolygonShape(
    val source: CornerBasedShape,
    val cut: Boolean,
    val absolute: Boolean,
) : PolygonShape() {
    override fun copyBase(): PolygonShape = CornerShapePolygonShape(source, cut, absolute)

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
