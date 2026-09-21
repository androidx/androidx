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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.DrawMesh2D
import androidx.compose.remote.core.operations.MatrixFromMesh2D
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter

/** A handle to a 2D mesh added with [RcScope.remoteMesh2D] or [RcScope.remoteMesh2DValues]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcMesh internal constructor(internal val id: Int)

/**
 * The domain a mesh's `(u, v)` parameters live in.
 *
 * A rectangular grid is the obvious default and the wrong only option, since half the things worth
 * drawing are radial. The layout decides three things at once: how `(u, v)` is sampled, how the
 * vertices are wired into triangles, and - when no position expression is supplied - what the
 * default geometry is. Those defaults sit in the unit square or unit circle at the origin, so the
 * ordinary canvas matrix is what places and sizes them.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcMeshLayout(public val value: Int) {
    /** u across, v down. Warps, cloth, gradient fields, heat maps. */
    Grid(Mesh2DGenerator.LAYOUT_GRID),
    /** u is an angle over a full turn, v is radius 0..1. Radial gradients, ripples, sunbursts. */
    Polar(Mesh2DGenerator.LAYOUT_POLAR),
    /** u is an angle, v crosses a band between two radii. Gauges, arcs, donuts, dials. */
    Ring(Mesh2DGenerator.LAYOUT_RING),
    /** u along, v across. Ribbons, trails, variable width strokes. */
    Strip(Mesh2DGenerator.LAYOUT_STRIP),
    /** u is an angle about a shared centre vertex. Pie wedges, cones of light. */
    Fan(Mesh2DGenerator.LAYOUT_FAN),
    /** u is arclength along a path, v crosses its width. Strokes that follow a path. */
    PathStrip(Mesh2DGenerator.LAYOUT_PATH_STRIP),
}

/** How a mesh's vertex colours combine with the texel sampled from its image. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcMeshBlend(public val value: Int) {
    /** Ignore the image and use the vertex colours alone. */
    ColorsOnly(DrawMesh2D.BLEND_COLORS_ONLY),
    /** Multiply texel by vertex colour, so the colours tint the image. */
    Modulate(DrawMesh2D.BLEND_MODULATE),
}

/**
 * How much of a mesh's local frame [RcScope.matrixFromMesh2D] applies.
 *
 * These are increasing levels of faithfulness rather than independent bits, which is why they are
 * an enum and not an OR-able flag set. Where the patch is degenerate - a fan's apex, a ring at
 * radius zero, a collapsed grid cell - the matrix is singular, and the player falls back to the
 * next level down rather than emitting a matrix that scales content to nothing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcMeshMatrix(public val value: Int) {
    /** Translate to the mesh position only. */
    Origin(MatrixFromMesh2D.FLAG_ORIGIN),
    /** Position, plus the rotation of the surface. Text along a path strip. */
    Rotation(MatrixFromMesh2D.FLAG_ROTATION),
    /** Position and rotation, plus scale - a label that grows as the ribbon widens. */
    Scale(MatrixFromMesh2D.FLAG_SCALE),
    /** The full 2x3 affine, skew included. Content shears with the patch. */
    Full(MatrixFromMesh2D.FLAG_FULL),
}

/**
 * Builder for the channels of a parametric 2D mesh.
 *
 * Every channel is an expression over the mesh's domain parameters, [u] and [v], and every channel
 * is optional. What an absent channel means is deliberately not "zero": an absent position falls
 * back to the layout's default geometry, an absent uv is the identity mapping, and absent colours
 * mean the mesh carries no vertex colours at all rather than black ones.
 *
 * The animation is free. Adding `continuousSec()` to [y] does not change the byte count, so a
 * waving flag costs what a flat one costs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcMesh2DScope internal constructor(writer: RemoteComposeWriter) {
    /** The first domain parameter, 0..1. Its meaning comes from the layout. */
    public val u: RcFloat = RcFloat(writer, floatArrayOf(Rc.FloatExpression.VAR1))

    /** The second domain parameter, 0..1. Its meaning comes from the layout. */
    public val v: RcFloat = RcFloat(writer, floatArrayOf(Rc.FloatExpression.VAR2))

    /** Vertex x, or null for the layout's default geometry. */
    public var x: RcFloat? = null

    /** Vertex y, or null for the layout's default geometry. */
    public var y: RcFloat? = null

    /** Texture u in 0..1, or null for the identity mapping from the domain. */
    public var texU: RcFloat? = null

    /** Texture v in 0..1, or null for the identity mapping from the domain. */
    public var texV: RcFloat? = null

    /** Vertex alpha in 0..1. Defaults to 1 when any other colour channel is set. */
    public var alpha: RcFloat? = null

    /** Vertex red in 0..1. Defaults to 1 when any other colour channel is set. */
    public var red: RcFloat? = null

    /** Vertex green in 0..1. Defaults to 1 when any other colour channel is set. */
    public var green: RcFloat? = null

    /** Vertex blue in 0..1. Defaults to 1 when any other colour channel is set. */
    public var blue: RcFloat? = null

    /**
     * The cross width of a [RcMeshLayout.PathStrip], in the path's own units.
     *
     * Being an expression rather than a constant is the point: a ribbon that tapers along its
     * length is what makes a path strip more than a thick stroke.
     */
    public var width: RcFloat? = null

    /** Set [x] and [y] to one expression each, in one statement. */
    public fun xy(x: RcFloat, y: RcFloat) {
        this.x = x
        this.y = y
    }

    /** Set [texU] and [texV] to one expression each, in one statement. */
    public fun texUv(u: RcFloat, v: RcFloat) {
        this.texU = u
        this.texV = v
    }

    /** Set [red], [green] and [blue] to one expression each, in one statement. */
    public fun rgb(red: RcFloat, green: RcFloat, blue: RcFloat) {
        this.red = red
        this.green = green
        this.blue = blue
    }

    /** Set [red], [green], [blue] and [alpha] to one expression each, in one statement. */
    public fun rgba(red: RcFloat, green: RcFloat, blue: RcFloat, alpha: RcFloat) {
        this.red = red
        this.green = green
        this.blue = blue
        this.alpha = alpha
    }
}
