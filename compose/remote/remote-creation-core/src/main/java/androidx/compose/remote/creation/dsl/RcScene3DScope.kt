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

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Paint3DContext
import androidx.compose.remote.core.operations.d3.MeshExpression3D
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi

/**
 * Typed DSL scope for 3D rendering inside an [RcCanvasScope]. Opened via [RcCanvasScope.scene3D].
 * Emits the 3D operations (meshes, primitives, camera, matrix ops, lights, materials, textures,
 * draw mesh, clear depth) following OpenGL conventions.
 *
 * Conventions: right-handed, +Y up, −Z forward; column-major 4×4; matrix ops post-multiply the
 * current modelview (`glTranslate`-style); CCW front-facing; angles in **radians**.
 *
 * ```
 * Canvas {
 *     scene3D {
 *         val cube = primitiveCube(id = 1, dimX = 1f, dimY = 1f, dimZ = 1f)
 *         perspectiveCamera(
 *             fovYRad = (60f * Math.PI / 180f).toFloat(),
 *             aspect = 1f,
 *             near = 0.1f,
 *             far = 100f,
 *             eye = floatArrayOf(0f, 0f, 4f),
 *             center = floatArrayOf(0f, 0f, 0f),
 *             up = floatArrayOf(0f, 1f, 0f),
 *         )
 *         clearDepth()
 *         rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
 *         drawMesh(cube)
 *     }
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@RcDslMarker
public sealed interface RcScene3DScope {

    /**
     * Upload a cached mesh. The returned [RcMesh] is the typed handle used by [drawMesh].
     *
     * @param id stable mesh id (caller-chosen, must be unique per document)
     * @param indices CCW triangle index list (length must be a multiple of 3); clockwise if the
     *   mesh is drawn with [RcMeshMode.FrontCw]
     * @param verts xyz triples (length must be a multiple of 3)
     * @param normals optional per-vertex normals as nxnynz triples; null = flat per-tri
     */
    public fun defineMesh(
        id: Int,
        indices: IntArray,
        verts: FloatArray,
        normals: FloatArray? = null,
        uv: FloatArray? = null,
    ): RcMesh

    /** Set a perspective camera + view in one call (`gluPerspective` + `gluLookAt`). */
    public fun perspectiveCamera(
        fovYRad: Float,
        aspect: Float,
        near: Float,
        far: Float,
        eye: FloatArray,
        center: FloatArray,
        up: FloatArray,
    )

    /**
     * **Reactive** perspective camera: [fovYRad] and the [eye]/[center] components are [RcFloat]s,
     * so the projection and view re-resolve on the player every frame (animated FOV, dolly, orbit)
     * with no document re-upload. [eye] and [center] are 3-element [RcFloat] arrays; [up] stays
     * static.
     */
    public fun perspectiveCamera(
        fovYRad: RcFloat,
        aspect: Float,
        near: Float,
        far: Float,
        eye: Array<RcFloat>,
        center: Array<RcFloat>,
        up: FloatArray,
    )

    /** Set an orthographic camera + view in one call (`glOrtho` + `gluLookAt`). */
    public fun orthoCamera(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        near: Float,
        far: Float,
        eye: FloatArray,
        center: FloatArray,
        up: FloatArray,
    )

    /** Reset the current modelview to identity. */
    public fun identity()

    /** Post-multiply current modelview by a translation. */
    public fun translate(x: Float, y: Float, z: Float)

    /**
     * [translate] with remote [RcFloat] components (e.g. driven by a [loop] index / data array).
     */
    public fun translate(x: RcFloat, y: RcFloat, z: RcFloat)

    /** Post-multiply current modelview by a non-uniform scale. */
    public fun scale(x: Float, y: Float, z: Float)

    /** Post-multiply current modelview by uniform scale. */
    public fun scale(s: Float): Unit = scale(s, s, s)

    /**
     * Uniform [scale] with a remote [RcFloat] factor (e.g. driven by a [loop] index / data array).
     */
    public fun scale(s: RcFloat)

    /** Non-uniform [scale] with reactive [RcFloat] components. */
    public fun scale(x: RcFloat, y: RcFloat, z: RcFloat)

    /**
     * Post-multiply current modelview by a rotation of [angleRad] around the given axis. `glRotate`
     * semantics; the axis is normalized internally.
     */
    public fun rotate(angleRad: Float, ax: Float, ay: Float, az: Float)

    /** [rotate] taking a remote [RcFloat] angle (e.g. driven by [RcScope.continuousSeconds]). */
    public fun rotate(angleRad: RcFloat, ax: Float, ay: Float, az: Float)

    /** [rotate] with a reactive [RcFloat] angle and reactive axis components. */
    public fun rotate(angleRad: RcFloat, ax: RcFloat, ay: RcFloat, az: RcFloat)

    /**
     * Player-side loop: repeats [block] for `index` from [start] to [end] (exclusive) stepping by
     * [step], pushing the iteration to the player instead of unrolling at author time (one set of
     * ops on the wire instead of N). The block's [RcFloat] `index` can drive transforms and data
     * lookups — e.g. `translate(xs[index], ys[index], zs[index])` over a `remoteFloatArray`.
     */
    public fun loop(start: Float, step: Float, end: Float, block: RcScene3DScope.(RcFloat) -> Unit)

    /** Post-multiply current modelview by an arbitrary column-major 4×4. */
    public fun multiply(matrix4: FloatArray)

    /**
     * Save the current model matrix on the player's matrix stack, to be restored by the matching
     * [popMatrix]. Only the model matrix is saved; the camera is never stacked. The stack holds 32
     * matrices: a deeper push and its matching pop do nothing, as does a pop with nothing pushed.
     * [clearDepth] empties the stack.
     */
    public fun pushMatrix()

    /** Restore the model matrix saved by the matching [pushMatrix]. */
    public fun popMatrix()

    /**
     * Run [block] between a [pushMatrix] and its [popMatrix], so the transforms it applies do not
     * leak into later draws — the 3D counterpart of the 2D `save { }`.
     */
    public fun pushMatrix(block: RcScene3DScope.() -> Unit) {
        pushMatrix()
        block()
        popMatrix()
    }

    /**
     * Draw a cached mesh with the current modelview, camera, and paint color.
     *
     * @param mesh handle returned by [defineMesh]
     * @param mode render mode selecting engine + shading — see [RcMeshMode]
     *   ([RcMeshMode.SoftwareFlat], [RcMeshMode.SoftwareSmooth], [RcMeshMode.CanvasFlat],
     *   [RcMeshMode.CanvasSmooth]). Pick by what the content needs: a depth-correct CPU raster vs
     *   the faster painter-ordered `Canvas.drawVertices`, and flat/faceted vs smooth. Smooth on a
     *   mesh with no normals degrades to flat. Combine with the flags [RcMeshMode.Transparent],
     *   [RcMeshMode.DoubleSided], [RcMeshMode.FrontCw], [RcMeshMode.Gpu] or [RcMeshMode.Pbr] using
     *   [RcMeshMode.or] as needed.
     */
    public fun drawMesh(mesh: RcMesh, mode: RcMeshMode = RcMeshMode.SoftwareSmooth)

    /** Clear the depth buffer. Required to start a 3D pass cleanly. */
    public fun clearDepth()

    /**
     * Set the base ARGB color used by subsequent [drawMesh] calls (the mesh's flat/smooth Lambert
     * shading multiplies this). Lets a single `scene3D` block draw meshes in different colors
     * without leaving the scope.
     */
    public fun color(@ColorInt argb: Int)

    /** [color] from a 32-bit packed ARGB `Long` hex literal, e.g. `color(0xFF3366CC)`. */
    public fun color(argb: Long): Unit = color(argb.toInt())

    /**
     * Set the base color from a **reactive** [RcColor] resource (e.g. a themed color, or an HSV
     * color whose hue is an [RcFloat] from `remoteColorExpression`) for subsequent [drawMesh]
     * calls. Unlike [color] with a raw `Int`, this is re-resolved on the player every frame, so the
     * mesh color can animate (pulse, drift, heat-map a value) without re-uploading the document.
     */
    public fun color(color: RcColor)

    /** [color] from a packed ARGB [RcColorValue]. */
    public fun color(color: RcColorValue)

    /**
     * Add a directional light (e.g. sun). [direction] is the world-space direction the light
     * travels (it strikes surfaces facing `-direction`). Lights accumulate until [clearLights];
     * with none set the renderer uses a default headlight.
     *
     * @param direction 3 floats `dx,dy,dz` (need not be normalized)
     * @param color light color (ARGB; RGB used)
     * @param intensity scalar multiplier on the light contribution
     */
    public fun directionalLight(
        direction: FloatArray,
        @ColorInt color: Int,
        @FloatRange(from = 0.0) intensity: Float = 1f,
    )

    /**
     * **Reactive** directional light: [direction] (3-element [RcFloat] array) and [intensity]
     * re-resolve on the player every frame, so the light can sweep/pulse without re-upload. [color]
     * stays static.
     */
    public fun directionalLight(direction: Array<RcFloat>, @ColorInt color: Int, intensity: RcFloat)

    /**
     * Add a point light at world-space [position].
     *
     * @param position 3 floats `px,py,pz`
     * @param color light color (ARGB; RGB used)
     * @param intensity scalar multiplier on the light contribution
     */
    public fun pointLight(
        position: FloatArray,
        @ColorInt color: Int,
        @FloatRange(from = 0.0) intensity: Float = 1f,
    )

    /**
     * **Reactive** point light: [position] (3-element [RcFloat] array) and [intensity] re-resolve
     * per frame.
     */
    public fun pointLight(position: Array<RcFloat>, @ColorInt color: Int, intensity: RcFloat)

    /**
     * Add an ambient light: its [color] times [intensity] lights every surface equally, whatever
     * way it faces. Ambient lights add up; a light set without one gets a default ambient of 0.2
     * white.
     *
     * @param color light color (ARGB; RGB used)
     * @param intensity scalar multiplier on the light contribution
     */
    public fun ambientLight(@ColorInt color: Int, @FloatRange(from = 0.0) intensity: Float = 1f)

    /** **Reactive** ambient light: [intensity] re-resolves per frame. [color] stays static. */
    public fun ambientLight(@ColorInt color: Int, intensity: RcFloat)

    /**
     * Empty the light set: later draws get only the default ambient light until lights are added
     * again.
     */
    public fun clearLights()

    /**
     * Texture subsequent [drawMesh] calls with an uploaded bitmap (see `remoteBitmap`), sampled
     * through each mesh's UV coordinates. Pass `null` to clear. Meshes without UV are unaffected.
     */
    public fun texture(image: RcImage?)

    /**
     * Give subsequent [drawMesh] calls a glossy specular highlight (Blinn-Phong, every engine).
     * [specStrength] scales an additive highlight in each light's color (0 = matte, the default);
     * [shininess] is the specular exponent (higher = tighter, glossier). Call `material(0f, …)` to
     * return to a matte finish.
     */
    public fun material(
        @FloatRange(from = 0.0) specStrength: Float,
        @FloatRange(from = 0.0, fromInclusive = false) shininess: Float = 32f,
    )

    /**
     * **Reactive** material: [specStrength] and [shininess] are [RcFloat]s re-resolved on the
     * player every frame, so the gloss can pulse/animate without re-uploading the document.
     */
    public fun material(specStrength: RcFloat, shininess: RcFloat)

    // ---- Unified vec3 overloads (RcVec) ----
    // One [RcVec] parameter replaces the static-FloatArray + reactive-Array<RcFloat> pair, and also
    // accepts a computed vector (flushed to component variables on demand). Default impls delegate
    // to
    // the existing reactive overloads via [RcVec.components].

    /** [translate] from a vec3. */
    public fun translate(v: RcVec) {
        val c = v.components()
        translate(c[0], c[1], c[2])
    }

    /** Non-uniform [scale] from a vec3. */
    public fun scale(v: RcVec) {
        val c = v.components()
        scale(c[0], c[1], c[2])
    }

    /** [rotate] around a vec3 [axis] by a reactive angle. */
    public fun rotate(angleRad: RcFloat, axis: RcVec) {
        val c = axis.components()
        rotate(angleRad, c[0], c[1], c[2])
    }

    /** [rotate] around a vec3 [axis] by a constant angle. */
    public fun rotate(angleRad: Float, axis: RcVec): Unit = rotate(RcFloat(angleRad), axis)

    /** Reactive directional light from a vec3 direction. */
    public fun directionalLight(direction: RcVec, @ColorInt color: Int, intensity: RcFloat) {
        directionalLight(direction.components(), color, intensity)
    }

    /** Reactive point light from a vec3 position. */
    public fun pointLight(position: RcVec, @ColorInt color: Int, intensity: RcFloat) {
        pointLight(position.components(), color, intensity)
    }

    /** Reactive perspective camera with vec3 [eye]/[center] ([up] stays static). */
    public fun perspectiveCamera(
        fovYRad: RcFloat,
        aspect: Float,
        near: Float,
        far: Float,
        eye: RcVec,
        center: RcVec,
        up: FloatArray,
    ) {
        perspectiveCamera(fovYRad, aspect, near, far, eye.components(), center.components(), up)
    }

    /** [orthoCamera] with vec3 [eye]/[center] (constant — ortho has no reactive form). */
    public fun orthoCamera(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        near: Float,
        far: Float,
        eye: RcVec,
        center: RcVec,
        up: FloatArray,
    ) {
        orthoCamera(left, right, bottom, top, near, far, eye.constArray(), center.constArray(), up)
    }

    /**
     * Give subsequent [drawMesh] calls a polygon-offset depth bias (window-z units) so
     * near-coincident surfaces can be ordered deterministically and avoid z-fighting. The applied
     * offset is `constant + slope * maxDepthSlope` (glPolygonOffset-style): positive pushes a
     * surface toward the far plane, negative pulls it nearer. Call `depthBias(0f)` to clear.
     * Software backend only (the canvas backend is painter-ordered).
     */
    public fun depthBias(constant: Float, slope: Float = 0f)

    /** **Reactive** depth bias: [constant] and [slope] are [RcFloat]s re-resolved per frame. */
    public fun depthBias(constant: RcFloat, slope: RcFloat)

    /**
     * Low-level procedural mesh: evaluate RPN float-[pos] expressions over a (u,v) grid into a
     * cached mesh (see [RcSurface] for [type] and the per-type [params] layout). [normal] and [uv]
     * are optional expression groups (empty = auto). [flags] may hold [RcSurface.FLAG_FLIP_WINDING]
     * and [RcSurface.FLAG_FLIP_NORMALS]. The returned [RcMesh] is drawn by [drawMesh]; if the
     * expressions reference time the surface animates. Prefer the typed
     * [expressionSurface]/[expressionSphere]/[expressionCylinder]/[expressionGeneral] helpers.
     */
    public fun meshExpression(
        id: Int,
        type: Int,
        flags: Int = 0,
        params: FloatArray,
        pos: Array<FloatArray>,
        normal: Array<FloatArray> = emptyArray(),
        uv: Array<FloatArray> = emptyArray(),
    ): RcMesh

    /**
     * Define a 3D mesh by [id] from a parametric primitive (see [RcPrimitive3D] for [type] and the
     * per-type [params] layout; angles in radians). [segments] is a type-dependent sampling count
     * (e.g. a cylinder with `segments = 3f` is a triangular prism; `0f` = a sensible default);
     * [flags] is a per-type sampling mask (e.g. [RcPrimitive3D.FLAG_SPLINE]). The geometry is
     * generated on the player. The returned [RcMesh] is drawn by [drawMesh]. Prefer the typed
     * [primitiveSphere]/[primitiveCylinder]/… helpers.
     */
    public fun meshPrimitive(
        id: Int,
        type: Int,
        segments: Float = 0f,
        flags: Int = 0,
        params: FloatArray,
    ): RcMesh

    /**
     * Multi-channel variant of [meshPrimitive]: [channels]`[0]` is the scalar params, channels `1+`
     * carry geometry streams (a lathe profile, a sweep cross-section + path, …) for the shapes that
     * need more than a flat scalar list.
     */
    public fun meshPrimitive(
        id: Int,
        type: Int,
        segments: Float = 0f,
        flags: Int = 0,
        channels: Array<FloatArray>,
    ): RcMesh
}

/** Surface-family constants and flags for [RcScene3DScope.meshExpression]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public object RcSurface {
    public const val GENERAL: Int = MeshExpression3D.SURFACE_GENERAL
    public const val HEIGHT_FIELD: Int = MeshExpression3D.SURFACE_HEIGHT_FIELD
    public const val SPHERE: Int = MeshExpression3D.SURFACE_SPHERE
    public const val CYLINDER: Int = MeshExpression3D.SURFACE_CYLINDER

    /** Flag for [RcScene3DScope.meshExpression]: reverse the triangle winding. */
    public const val FLAG_FLIP_WINDING: Int = MeshExpression3D.FLAG_FLIP_WINDING

    /** Flag for [RcScene3DScope.meshExpression]: reverse the generated normals. */
    public const val FLAG_FLIP_NORMALS: Int = MeshExpression3D.FLAG_FLIP_NORMALS
}

/** Primitive-type constants and flags for [RcScene3DScope.meshPrimitive]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public object RcPrimitive3D {
    public const val SPHERE: Int = Primitive3D.SPHERE
    public const val CYLINDER: Int = Primitive3D.CYLINDER
    public const val CONE: Int = Primitive3D.CONE
    public const val CUBE: Int = Primitive3D.CUBE
    public const val ROUNDED_CUBE: Int = Primitive3D.ROUNDED_CUBE
    public const val SPHERICAL_SECTOR: Int = Primitive3D.SPHERICAL_SECTOR
    public const val SPHERICAL_DOME: Int = Primitive3D.SPHERICAL_DOME
    public const val TUBE: Int = Primitive3D.TUBE
    public const val CAP_TUBE: Int = Primitive3D.CAP_TUBE
    public const val PROFILE_TUBE: Int = Primitive3D.PROFILE_TUBE
    public const val TORUS: Int = Primitive3D.TORUS
    public const val PLANE: Int = Primitive3D.PLANE

    // Extruded 2D shapes (a flat profile swept along Z by a `depth`).
    public const val EXTRUDE_CIRCLE: Int = Primitive3D.EXTRUDE_CIRCLE
    public const val EXTRUDE_SECTOR: Int = Primitive3D.EXTRUDE_SECTOR
    public const val EXTRUDE_SEGMENT: Int = Primitive3D.EXTRUDE_SEGMENT
    public const val EXTRUDE_ARC: Int = Primitive3D.EXTRUDE_ARC
    public const val EXTRUDE_ROUNDED_RECT: Int = Primitive3D.EXTRUDE_ROUNDED_RECT
    public const val EXTRUDE_SQUIRCLE: Int = Primitive3D.EXTRUDE_SQUIRCLE
    public const val EXTRUDE_PATH: Int = Primitive3D.EXTRUDE_PATH
    public const val LATHE: Int = Primitive3D.LATHE
    public const val SWEEP: Int = Primitive3D.SWEEP
    public const val HELIX: Int = Primitive3D.HELIX
    public const val ICOSPHERE: Int = Primitive3D.ICOSPHERE

    /** An infinite plane, optionally with a grid; see [primitiveInfinitePlane] for the params. */
    public const val INFINITE_PLANE: Int = Primitive3D.INFINITE_PLANE

    // UV-mapping modes (the `uv` arg of the texturable primitive helpers): 0 = none, 1 = the type's
    // common mapping, which every type generates; 2/3 are reserved for alternative patterns and
    // currently give the same mapping as 1.
    public const val UV_NONE: Int = Primitive3D.UV_NONE
    public const val UV: Int = Primitive3D.UV
    public const val UV2: Int = Primitive3D.UV2
    public const val UV3: Int = Primitive3D.UV3

    /**
     * Pack a UV mode ([UV_NONE]..[UV3]) into the flags bitmask.
     *
     * @throws IllegalArgumentException if [uvMode] is not one of the UV modes
     */
    public fun uvFlag(uvMode: Int): Int {
        require(uvMode in UV_NONE..UV3) {
            "uv mode must be UV_NONE, UV, UV2 or UV3 (0..3), got $uvMode"
        }
        return uvMode shl Primitive3D.FLAG_UV_SHIFT
    }

    // Axis remaps (the `axis` arg of the primitive helpers): AXIS_NATIVE keeps the type's
    // built-in orientation (+Y up for the round solids, +Z depth for the plane and the extrusions);
    // AXIS_X/Y/Z turn the finished mesh about its center (a cone's base center) so that direction
    // points along +X/+Y/+Z. Types placed by explicit points (cylinder, tubes, sweep) ignore it.
    public const val AXIS_NATIVE: Int = Primitive3D.AXIS_NATIVE
    public const val AXIS_X: Int = Primitive3D.AXIS_X
    public const val AXIS_Y: Int = Primitive3D.AXIS_Y
    public const val AXIS_Z: Int = Primitive3D.AXIS_Z

    /**
     * Pack an axis remap ([AXIS_NATIVE]..[AXIS_Z]) into the flags bitmask.
     *
     * @throws IllegalArgumentException if [axis] is not one of the axis remaps
     */
    public fun axisFlag(axis: Int): Int {
        require(axis in AXIS_NATIVE..AXIS_Z) {
            "axis must be AXIS_NATIVE, AXIS_X, AXIS_Y or AXIS_Z (0..3), got $axis"
        }
        return axis shl Primitive3D.FLAG_AXIS_SHIFT
    }

    /** Tube/cap-tube flag: parallel-transport sweep over the raw polyline instead of a spline. */
    public const val FLAG_TUBE_LEGACY: Int = Primitive3D.FLAG_TUBE_LEGACY

    /** Extrusion/lathe/sweep flag: fit a monotonic spline through the profile or path points. */
    public const val FLAG_SPLINE: Int = Primitive3D.FLAG_SPLINE

    /**
     * Tube/cap-tube flag: explicit rings-per-span float at params[1] (overrides adaptive density).
     */
    public const val FLAG_TUBE_PATH_DENSITY: Int = Primitive3D.FLAG_TUBE_PATH_DENSITY

    /** Profile-tube flag: close the two ends with flat disks. */
    public const val FLAG_TUBE_CAP: Int = Primitive3D.FLAG_TUBE_CAP

    /**
     * Tube/cap-tube/profile-tube flag: place the rings along the path by how much it turns rather
     * than by arc length, so thin tubes are not over-tessellated and straight runs get one ring per
     * span. An explicit [FLAG_TUBE_PATH_DENSITY] count still wins.
     */
    public const val FLAG_TUBE_CURVATURE_DENSITY: Int = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY
}

// ---- Typed parametric-primitive helpers (all compile to one MeshPrimitive op) -------------

// RcVec center overloads for primitives. Primitive geometry is baked constant, so the [center] vec
// must be constant (built from constant components); use [translate]/[scale] for reactive
// placement.

/** [primitiveSphere] with a vec3 [center]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveSphere(
    id: Int,
    radius: Float,
    center: RcVec,
    segments: Float = 0f,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    val c = center.constArray()
    return primitiveSphere(id, radius, c[0], c[1], c[2], segments, uv, axis)
}

/** [primitiveTorus] with a vec3 [center]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveTorus(
    id: Int,
    majorRadius: Float,
    minorRadius: Float,
    center: RcVec,
    segments: Float = 0f,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    val c = center.constArray()
    return primitiveTorus(id, majorRadius, minorRadius, c[0], c[1], c[2], segments, uv, axis)
}

/** [primitiveCone] with a vec3 base [center]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCone(
    id: Int,
    radius: Float,
    height: Float,
    center: RcVec,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    val c = center.constArray()
    return primitiveCone(id, radius, height, c[0], c[1], c[2], segments, axis)
}

/** [primitiveCube] of dimensions ([dimX],[dimY],[dimZ]) with a vec3 [center]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCube(
    id: Int,
    dimX: Float,
    dimY: Float,
    dimZ: Float,
    center: RcVec,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    val c = center.constArray()
    return primitiveCube(id, dimX, dimY, dimZ, c[0], c[1], c[2], uv, axis)
}

/** [primitivePlane] with a vec3 [center]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitivePlane(
    id: Int,
    width: Float,
    height: Float,
    center: RcVec,
    uv: Int = RcPrimitive3D.UV,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    val c = center.constArray()
    return primitivePlane(id, width, height, c[0], c[1], c[2], uv, axis)
}

/** [primitiveCylinder] between two vec3 endpoints [p1] and [p2]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCylinder(
    id: Int,
    radius: Float,
    p1: RcVec,
    p2: RcVec,
    segments: Float = 0f,
): RcMesh {
    val a = p1.constArray()
    val b = p2.constArray()
    return primitiveCylinder(id, radius, a[0], a[1], a[2], b[0], b[1], b[2], segments)
}

/**
 * Sphere of [radius] centered at ([x],[y],[z]); [segments] = longitude slices (0 = default). [uv]
 * selects a UV mapping ([RcPrimitive3D.UV] = longitude/latitude globe) so it can be textured.
 * [axis] turns the poles from the Y axis onto another (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveSphere(
    id: Int,
    radius: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.SPHERE,
        segments,
        RcPrimitive3D.uvFlag(uv) or RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, x, y, z),
    )

/**
 * Torus (donut) of ring radius [majorRadius] and tube radius [minorRadius] centered at
 * ([x],[y],[z]), lying in the XZ plane (hole along +Y). [segments] = ring segments (the tube
 * segments scale with the radius ratio for ~square quads; 0 = default). [uv] selects a UV mapping
 * ([RcPrimitive3D.UV] = around-ring × around-tube). [axis] turns the hole from the Y axis onto
 * another (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveTorus(
    id: Int,
    majorRadius: Float,
    minorRadius: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.TORUS,
        segments,
        RcPrimitive3D.uvFlag(uv) or RcPrimitive3D.axisFlag(axis),
        floatArrayOf(majorRadius, minorRadius, x, y, z),
    )

/**
 * Cylinder of [radius] between the two points ([x1],[y1],[z1]) and ([x2],[y2],[z2]), with caps.
 * [segments] = sides around (e.g. `3f` = a triangular prism; 0 = default).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCylinder(
    id: Int,
    radius: Float,
    x1: Float,
    y1: Float,
    z1: Float,
    x2: Float,
    y2: Float,
    z2: Float,
    segments: Float = 0f,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.CYLINDER,
        segments,
        0,
        floatArrayOf(radius, x1, y1, z1, x2, y2, z2),
    )

/**
 * Cone of base [radius] and [height] with its base centered at ([x],[y],[z]), apex up. [segments] =
 * sides around the base (e.g. `4f` = a square pyramid; 0 = default). [axis] points the apex along
 * another axis (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCone(
    id: Int,
    radius: Float,
    height: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.CONE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, height, x, y, z),
    )

/**
 * Box of full dimensions ([dimX],[dimY],[dimZ]) centered at ([x],[y],[z]). [uv] selects a UV
 * mapping ([RcPrimitive3D.UV] = each face mapped to the unit square) so it can be textured. [axis]
 * turns the box's Y axis onto another (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveCube(
    id: Int,
    dimX: Float,
    dimY: Float,
    dimZ: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    uv: Int = 0,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.CUBE,
        0f,
        RcPrimitive3D.uvFlag(uv) or RcPrimitive3D.axisFlag(axis),
        floatArrayOf(dimX, dimY, dimZ, x, y, z),
    )

/**
 * Flat rectangular plane of [width]×[height] centered at ([x],[y],[z]), in the XY plane facing +Z.
 * [uv] selects a UV mapping ([RcPrimitive3D.UV] = corners to the unit square). The texturable
 * replacement for `defineQuadMesh`. [axis] makes it face another axis instead (see
 * [RcPrimitive3D.AXIS_NATIVE]): [RcPrimitive3D.AXIS_Y] gives a floor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitivePlane(
    id: Int,
    width: Float = 1f,
    height: Float = 1f,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    uv: Int = RcPrimitive3D.UV,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.PLANE,
        0f,
        RcPrimitive3D.uvFlag(uv) or RcPrimitive3D.axisFlag(axis),
        floatArrayOf(width, height, x, y, z),
    )

/**
 * An infinite plane through ([x],[y],[z]), drawn out to the horizon. Its normal is +Y, so by
 * default it is a floor; [axis] makes it face another axis. With [spacing] above 0 a grid of
 * [spacing]-sized cells is drawn on it, with lines [lineWidth] pixels wide in [lineColor] (ARGB;
 * its alpha blends the lines over the plane) that fade out where the cells get too small to see.
 * The plane itself is shaded like any mesh in the current color, so draw it with
 * [RcScene3DScope.drawMesh]; only its front side is drawn unless the mode has
 * [RcMeshMode.DoubleSided].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveInfinitePlane(
    id: Int,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    spacing: Float = 0f,
    lineWidth: Float = 1f,
    lineColor: Int = 0xFF808080.toInt(),
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.INFINITE_PLANE,
        0f,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(
            x,
            y,
            z,
            spacing,
            lineWidth,
            ((lineColor ushr 24) and 0xFF) / 255f,
            ((lineColor shr 16) and 0xFF) / 255f,
            ((lineColor shr 8) and 0xFF) / 255f,
            (lineColor and 0xFF) / 255f,
        ),
    )

// ---- Extruded 2D shapes: a flat profile in the XY plane swept along Z by [depth], producing a
// prism with two cap faces + a side wall. Angles in radians. [segments] = profile tessellation.
// [axis] turns the extrusion direction from Z onto another axis (see
// [RcPrimitive3D.AXIS_NATIVE]).

/** Extruded circle (a coin): [radius] disk of thickness [depth], [segments] sides around. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedCircle(
    id: Int,
    radius: Float,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_CIRCLE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, depth, x, y, z),
    )

/**
 * Extruded sector (pie slice) of [radius], from [startAngle] sweeping [sweepAngle] radians (apex at
 * the center), thickness [depth]. [segments] = arc divisions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedSector(
    id: Int,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_SECTOR,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, startAngle, sweepAngle, depth, x, y, z),
    )

/**
 * Extruded circular segment (the region between an arc and its chord): [radius], from [startAngle]
 * sweeping [sweepAngle] radians, thickness [depth]. [segments] = arc divisions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedSegment(
    id: Int,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_SEGMENT,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, startAngle, sweepAngle, depth, x, y, z),
    )

/**
 * Extruded annular sector (a curved bar / ring slice) between [innerRadius] and [outerRadius], from
 * [startAngle] sweeping [sweepAngle] radians, thickness [depth]. [segments] = arc divisions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedArc(
    id: Int,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_ARC,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(innerRadius, outerRadius, startAngle, sweepAngle, depth, x, y, z),
    )

/**
 * Extruded rounded rectangle: [width]×[height] with corner radius [cornerRadius], thickness
 * [depth]. [segments] = arc subdivisions per corner.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedRoundedRect(
    id: Int,
    width: Float,
    height: Float,
    cornerRadius: Float,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_ROUNDED_RECT,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(width, height, cornerRadius, depth, x, y, z),
    )

/**
 * Extruded squircle (superellipse `|x/r|^n + |y/r|^n = 1`): [radius], [exponent] `n` (2 = ellipse,
 * ~4 = squircle, large = square), thickness [depth]. [segments] = perimeter samples.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedSquircle(
    id: Int,
    radius: Float,
    exponent: Float = 4f,
    depth: Float = 0.2f,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.EXTRUDE_SQUIRCLE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, exponent, depth, x, y, z),
    )

/**
 * Extrude an arbitrary 2D path into a 3D prism: one or more closed [contours] (each a flat
 * `x0,y0,x1,y1,…` polyline, no repeated closing point) swept along Z by [depth], centered at
 * ([x],[y],[z]). Concave outlines, holes (e.g. the counters of letters like `e`/`o`) and
 * overlapping contours are supported — the player fills the caps with the nonzero-winding rule
 * (matching how fonts and `Path` fill by default). This is the path-shaped analogue of the
 * parametric `primitiveExtruded*` helpers; feed it the flattened contours of any
 * `android.graphics.Path` (e.g. text via `Paint.getTextPath`, walked per-contour with
 * `PathMeasure`).
 *
 * The caps lie in the XY plane (front faces +Z); rotate the drawn mesh to orient it in the scene,
 * or turn the extrusion onto another axis with [axis] (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveExtrudedPath(
    id: Int,
    contours: List<FloatArray>,
    depth: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    bevel: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh {
    // Pack: [depth, cx, cy, cz, bevel, contourCount, (count, x0,y0,…) per contour].
    var total = 6
    for (c in contours) {
        total += 1 + c.size
    }
    val params = FloatArray(total)
    params[0] = depth
    params[1] = x
    params[2] = y
    params[3] = z
    params[4] = bevel
    params[5] = contours.size.toFloat()
    var w = 6
    for (c in contours) {
        params[w++] = (c.size / 2).toFloat()
        for (v in c) {
            params[w++] = v
        }
    }
    return meshPrimitive(id, RcPrimitive3D.EXTRUDE_PATH, 0f, RcPrimitive3D.axisFlag(axis), params)
}

/**
 * Revolve a 2D [profile] around the Y axis (a lathe), centered at ([x],[y],[z]) with [segments]
 * sides. [profile] is a flat list of `r0,y0, r1,y1, …` pairs — radius from the axis and height.
 * This is the rotational analogue of [primitiveExtrudedPath]; it generalizes
 * sphere/cylinder/cone/torus and makes vases, goblets, bottles, lamp bases and turned legs. A
 * profile point at `r = 0` closes that end to a point. [axis] revolves it around another axis
 * instead (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveLathe(
    id: Int,
    profile: FloatArray,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.LATHE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        arrayOf(floatArrayOf(x, y, z), profile),
    )

/**
 * Geodesic sphere of [radius] centered at ([x],[y],[z]) — a subdivided icosahedron with uniform,
 * evenly-sized triangles (unlike [primitiveSphere], which pinches at the poles). [segments] selects
 * the subdivision level (0..4). Better for displacement/deformation, planets and dice. [axis] turns
 * its Y axis onto another (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveIcosphere(
    id: Int,
    radius: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.ICOSPHERE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, x, y, z),
    )

/**
 * Sweep a 2D [crossSection] (a flat `x0,y0, x1,y1, …` list, in the moving frame's XY) along a 3D
 * [path] (`x0,y0,z0, …`), centered at ([x],[y],[z]). [closed] wraps the section into a tube (vs. an
 * open ribbon); [caps] closes the two ends. Optional [scales] (one per path station) taper/bulge it
 * and [twists] (radians per station) rotate the section as it travels. Parallel-transport frames
 * keep the section from spinning. Generalizes the circular [primitiveTube] to any cross-section —
 * rails, ribbons, pipes, twisted bars; the base of `HELIX` springs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveSweep(
    id: Int,
    crossSection: FloatArray,
    path: FloatArray,
    closed: Boolean = true,
    caps: Boolean = false,
    scales: FloatArray? = null,
    twists: FloatArray? = null,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
): RcMesh {
    val scalars =
        floatArrayOf(if (closed) 1f else 0f, if (caps) 1f else 0f, if (caps) 1f else 0f, x, y, z)
    val channels = ArrayList<FloatArray>()
    channels.add(scalars)
    channels.add(crossSection)
    channels.add(path)
    // data[3] = scales, data[4] = twists. Insert an empty scales channel if only twists are given.
    if (scales != null || twists != null) {
        channels.add(scales ?: FloatArray(0))
    }
    if (twists != null) {
        channels.add(twists)
    }
    return meshPrimitive(id, RcPrimitive3D.SWEEP, 0f, 0, channels.toTypedArray())
}

/**
 * A coil/spring: a circular tube of radius [tubeRadius] swept along a helix of radius [coilRadius]
 * about the Y axis, rising [pitch] per turn for [turns] turns, centered at ([x],[y],[z]).
 * [segments] = tube facets. Built on [primitiveSweep] — springs, screws, coils, spiral stairs.
 * [axis] coils it about another axis instead (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveHelix(
    id: Int,
    coilRadius: Float,
    tubeRadius: Float,
    pitch: Float,
    turns: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.HELIX,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(coilRadius, tubeRadius, pitch, turns, x, y, z),
    )

/**
 * Rounded box of full dims ([dimX],[dimY],[dimZ]) centered at ([x],[y],[z]), corner [radius].
 * [segments] = arc subdivisions per corner (0 = default). [axis] turns the box's Y axis onto
 * another (see [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveRoundedCube(
    id: Int,
    dimX: Float,
    dimY: Float,
    dimZ: Float,
    radius: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.ROUNDED_CUBE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(dimX, dimY, dimZ, x, y, z, radius),
    )

/**
 * **Reactive** rounded cube: the corner [radius] is an [RcFloat], so the mesh is **regenerated on
 * the player** whenever it changes (the variable-aware
 * [MeshPrimitive][androidx.compose.remote.core.operations.d3.MeshPrimitive] rebuilds the geometry)
 * — morphing a box ↔ a rounded blob. This is true geometry change, not a transform. Pass a large
 * enough fixed [segments] so the rounded corners are smooth at the maximum radius.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveRoundedCube(
    id: Int,
    dimX: Float,
    dimY: Float,
    dimZ: Float,
    radius: RcFloat,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.ROUNDED_CUBE,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(dimX, dimY, dimZ, x, y, z, radius.toFloat()),
    )

/**
 * Spherical sector (cap + cone to center) of [radius] and half-[angle] (radians) at ([x],[y],[z]).
 * [segments] = longitude slices (0 = default). [axis] points the cap along another axis (see
 * [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveSphericalSector(
    id: Int,
    radius: Float,
    angle: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.SPHERICAL_SECTOR,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, angle, x, y, z),
    )

/**
 * Spherical dome (cap + flat base) of [radius] and polar [angle] (radians) at ([x],[y],[z]).
 * [segments] = longitude slices (0 = default). [axis] points the cap along another axis (see
 * [RcPrimitive3D.AXIS_NATIVE]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveSphericalDome(
    id: Int,
    radius: Float,
    angle: Float,
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    segments: Float = 0f,
    axis: Int = RcPrimitive3D.AXIS_NATIVE,
): RcMesh =
    meshPrimitive(
        id,
        RcPrimitive3D.SPHERICAL_DOME,
        segments,
        RcPrimitive3D.axisFlag(axis),
        floatArrayOf(radius, angle, x, y, z),
    )

/**
 * Tube of [radius] through the [points] (x,y,z triples, >= 2 points). [capped] closes the ends with
 * flat disks. [segments] = sides around the cross-section (`3f` = triangular tube).
 *
 * By default the points are fit to a monotonic cubic spline and the shell is swept as disks normal
 * to the spline tangent (smooth, non-overshooting); [segments] sets the cross-section sides and the
 * along-path ring count is adaptive (each span gets ~ its arc length / the cross-section edge
 * length, so long/curvy spans densify automatically). Set [curvatureDensity] to place the rings by
 * how much the path turns instead, so thin tubes are not over-tessellated. Pass [pathSegments] `>
 * 0` to override either with an explicit number of rings per control span. Set [legacy] to instead
 * use the parallel-transport sweep over the raw polyline (faceted at the control points).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveTube(
    id: Int,
    radius: Float,
    points: FloatArray,
    capped: Boolean = false,
    segments: Float = 4f,
    legacy: Boolean = false,
    pathSegments: Float = 0f,
    curvatureDensity: Boolean = false,
): RcMesh {
    val type = if (capped) RcPrimitive3D.CAP_TUBE else RcPrimitive3D.TUBE
    var flags = if (legacy) RcPrimitive3D.FLAG_TUBE_LEGACY else 0
    if (curvatureDensity) flags = flags or RcPrimitive3D.FLAG_TUBE_CURVATURE_DENSITY
    val params: FloatArray
    if (pathSegments > 0f) {
        // Explicit density: params = [radius, pathSegments, points…]; set the flag.
        flags = flags or RcPrimitive3D.FLAG_TUBE_PATH_DENSITY
        params = FloatArray(points.size + 2)
        params[0] = radius
        params[1] = pathSegments
        System.arraycopy(points, 0, params, 2, points.size)
    } else {
        params = FloatArray(points.size + 1)
        params[0] = radius
        System.arraycopy(points, 0, params, 1, points.size)
    }
    return meshPrimitive(id, type, segments, flags, params)
}

/**
 * A **profile tube**: a tube whose radius varies along its length. [points] (x,y,z triples) define
 * the centerline and [radii] is an independent list of radii — both are fit to a monotonic spline,
 * so the tube tapers / bulges / pinches smoothly. The two lists may have **different lengths**: the
 * radius profile is mapped across the whole path, so `radii.first()` is the radius at the start and
 * `radii.last()` at the end, spread by path fraction in between.
 *
 * [segments] = cross-section sides; [capped] closes the ends; the along-path ring count is adaptive
 * unless [pathSegments] `> 0` overrides it (rings per control span). Set [curvatureDensity] to
 * place the rings by how much the path turns instead of by arc length.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.primitiveProfileTube(
    id: Int,
    points: FloatArray,
    radii: FloatArray,
    capped: Boolean = false,
    segments: Float = 0f,
    pathSegments: Float = 0f,
    curvatureDensity: Boolean = false,
): RcMesh {
    val nPoints = points.size / 3
    var flags = if (capped) RcPrimitive3D.FLAG_TUBE_CAP else 0
    if (curvatureDensity) flags = flags or RcPrimitive3D.FLAG_TUBE_CURVATURE_DENSITY
    val density = pathSegments > 0f
    if (density) flags = flags or RcPrimitive3D.FLAG_TUBE_PATH_DENSITY
    // Layout: [nPoints, (pathSegments?), points…, radii…]
    val head = if (density) 2 else 1
    val params = FloatArray(head + points.size + radii.size)
    params[0] = nPoints.toFloat()
    if (density) params[1] = pathSegments
    System.arraycopy(points, 0, params, head, points.size)
    System.arraycopy(radii, 0, params, head + points.size, radii.size)
    return meshPrimitive(id, RcPrimitive3D.PROFILE_TUBE, segments, flags, params)
}

// ---- Convenience helpers --------------------------------------------------

/**
 * Convenience: define a cone along +Y (apex up, base at the bottom) with smooth side normals and a
 * flat base. CCW winding. The base rim is a sharp crease (separate side/cap normals).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineConeMesh(
    id: Int,
    radius: Float = 0.8f,
    height: Float = 1.6f,
    segments: Int = 48,
): RcMesh {
    val cols = segments + 1
    val hy = height * 0.5f
    val slant = Math.sqrt((height * height + radius * radius).toDouble()).toFloat()
    val nY = radius / slant // slant-normal vertical component
    val nS = height / slant // slant-normal radial scale
    val baseRing = 0
    val apexRing = cols
    val botCenter = 2 * cols
    val botRing = botCenter + 1
    val vertCount = botRing + cols
    val verts = FloatArray(vertCount * 3)
    val normals = FloatArray(vertCount * 3)
    fun set(i: Int, px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float) {
        val p = i * 3
        verts[p] = px
        verts[p + 1] = py
        verts[p + 2] = pz
        normals[p] = nx
        normals[p + 1] = ny
        normals[p + 2] = nz
    }
    set(botCenter, 0f, -hy, 0f, 0f, -1f, 0f)
    for (j in 0..segments) {
        val phi = 2.0 * Math.PI * j / segments
        val cx = Math.cos(phi).toFloat()
        val cz = Math.sin(phi).toFloat()
        set(baseRing + j, radius * cx, -hy, radius * cz, nS * cx, nY, nS * cz)
        set(apexRing + j, 0f, hy, 0f, nS * cx, nY, nS * cz) // duplicated apex, slant normal
        set(botRing + j, radius * cx, -hy, radius * cz, 0f, -1f, 0f)
    }
    val indices = IntArray(segments * 6)
    var k = 0
    for (j in 0 until segments) {
        indices[k++] = baseRing + j
        indices[k++] = apexRing + j
        indices[k++] = baseRing + j + 1
        indices[k++] = botCenter
        indices[k++] = botRing + j
        indices[k++] = botRing + j + 1
    }
    return defineMesh(id, indices, verts, normals)
}

/**
 * Convenience: define an axis-aligned ellipsoid with radii [rx]/[ry]/[rz] and correct smooth
 * normals (which are NOT the position direction for a non-uniform ellipsoid). CCW winding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineEllipsoidMesh(
    id: Int,
    rx: Float = 1f,
    ry: Float = 0.6f,
    rz: Float = 1f,
    stacks: Int = 24,
    slices: Int = 32,
): RcMesh {
    val rows = stacks + 1
    val cols = slices + 1
    val verts = FloatArray(rows * cols * 3)
    val normals = FloatArray(rows * cols * 3)
    var p = 0
    for (i in 0..stacks) {
        val theta = Math.PI * i / stacks
        val sinT = Math.sin(theta).toFloat()
        val cosT = Math.cos(theta).toFloat()
        for (j in 0..slices) {
            val phi = 2.0 * Math.PI * j / slices
            val ux = sinT * Math.cos(phi).toFloat()
            val uy = cosT
            val uz = sinT * Math.sin(phi).toFloat()
            verts[p] = rx * ux
            verts[p + 1] = ry * uy
            verts[p + 2] = rz * uz
            val nx = ux / rx
            val ny = uy / ry
            val nz = uz / rz
            val inv = (1.0 / Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble())).toFloat()
            normals[p] = nx * inv
            normals[p + 1] = ny * inv
            normals[p + 2] = nz * inv
            p += 3
        }
    }
    val indices = IntArray(stacks * slices * 6)
    var k = 0
    for (i in 0 until stacks) {
        for (j in 0 until slices) {
            val a = i * cols + j
            val b = (i + 1) * cols + j
            val c = (i + 1) * cols + (j + 1)
            val d = i * cols + (j + 1)
            indices[k++] = a
            indices[k++] = d
            indices[k++] = b
            indices[k++] = b
            indices[k++] = d
            indices[k++] = c
        }
    }
    return defineMesh(id, indices, verts, normals)
}

/**
 * Convenience: define a capsule along +Y — a cylinder of straight length [cylHeight] capped by two
 * hemispheres of [radius]. Total height is {@code cylHeight + 2*radius}. Fully smooth (the
 * hemisphere/cylinder join shares radial normals, so no crease). CCW winding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineCapsuleMesh(
    id: Int,
    radius: Float = 0.5f,
    cylHeight: Float = 1.0f,
    segments: Int = 48,
    hemiStacks: Int = 12,
): RcMesh {
    val cols = segments + 1
    val hh = cylHeight * 0.5f
    val ringCount = 2 * (hemiStacks + 1)
    val verts = FloatArray(ringCount * cols * 3)
    val normals = FloatArray(ringCount * cols * 3)
    var p = 0
    // Top hemisphere (theta 0..pi/2 around +hh) then bottom hemisphere (pi/2..pi around -hh);
    // the strip between the two equator rings is the cylinder wall.
    for (ring in 0 until ringCount) {
        val top = ring <= hemiStacks
        val centerY = if (top) hh else -hh
        val theta =
            if (top) (Math.PI / 2.0) * (ring.toDouble() / hemiStacks)
            else (Math.PI / 2.0) * (1.0 + (ring - hemiStacks - 1).toDouble() / hemiStacks)
        val sinT = Math.sin(theta).toFloat()
        val cosT = Math.cos(theta).toFloat()
        for (j in 0..segments) {
            val phi = 2.0 * Math.PI * j / segments
            val cx = Math.cos(phi).toFloat()
            val cz = Math.sin(phi).toFloat()
            verts[p] = radius * sinT * cx
            verts[p + 1] = centerY + radius * cosT
            verts[p + 2] = radius * sinT * cz
            normals[p] = sinT * cx
            normals[p + 1] = cosT
            normals[p + 2] = sinT * cz
            p += 3
        }
    }
    val indices = IntArray((ringCount - 1) * segments * 6)
    var k = 0
    for (i in 0 until ringCount - 1) {
        for (j in 0 until segments) {
            val a = i * cols + j
            val b = (i + 1) * cols + j
            val c = (i + 1) * cols + (j + 1)
            val d = i * cols + (j + 1)
            indices[k++] = a
            indices[k++] = d
            indices[k++] = b
            indices[k++] = b
            indices[k++] = d
            indices[k++] = c
        }
    }
    return defineMesh(id, indices, verts, normals)
}

/**
 * Convenience: define a rounded box — an axis-aligned box of half-extents [ex]/[ey]/[ez] (the
 * straight part) inflated by corner radius [radius], so edges become quarter-cylinders and corners
 * sphere-octants. Built by projecting a subdivided cube onto the rounded surface, which yields both
 * the position and the smooth normal. CCW winding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineRoundedBoxMesh(
    id: Int,
    ex: Float = 0.7f,
    ey: Float = 0.7f,
    ez: Float = 0.7f,
    radius: Float = 0.25f,
    seg: Int = 10,
): RcMesh {
    // Inner half-extents (clamp targets) and the outer cube we subdivide.
    val ix = ex
    val iy = ey
    val iz = ez
    val ox = ex + radius
    val oy = ey + radius
    val oz = ez + radius
    // Six faces, each (uDir, vDir) chosen so uDir x vDir == outward face normal.
    val faces =
        arrayOf(
            intArrayOf(0, +1), // +X: u=+Y, v=+Z
            intArrayOf(0, -1), // -X: u=+Z, v=+Y
            intArrayOf(1, +1), // +Y: u=+Z, v=+X
            intArrayOf(1, -1), // -Y: u=+X, v=+Z
            intArrayOf(2, +1), // +Z: u=+X, v=+Y
            intArrayOf(2, -1), // -Z: u=+Y, v=+X
        )
    val perFace = (seg + 1) * (seg + 1)
    val vertCount = 6 * perFace
    val verts = FloatArray(vertCount * 3)
    val normals = FloatArray(vertCount * 3)
    val indices = IntArray(6 * seg * seg * 6)
    var vp = 0
    var ip = 0
    var base = 0
    for (f in faces) {
        val axis = f[0]
        val sign = f[1]
        // Basis vectors for this face.
        val uDir = FloatArray(3)
        val vDir = FloatArray(3)
        val nDir = FloatArray(3)
        nDir[axis] = sign.toFloat()
        when (axis) {
            0 -> { // X
                if (sign > 0) {
                    uDir[1] = 1f
                    vDir[2] = 1f
                } else {
                    uDir[2] = 1f
                    vDir[1] = 1f
                }
            }
            1 -> { // Y
                if (sign > 0) {
                    uDir[2] = 1f
                    vDir[0] = 1f
                } else {
                    uDir[0] = 1f
                    vDir[2] = 1f
                }
            }
            else -> { // Z
                if (sign > 0) {
                    uDir[0] = 1f
                    vDir[1] = 1f
                } else {
                    uDir[1] = 1f
                    vDir[0] = 1f
                }
            }
        }
        for (s in 0..seg) {
            val u = (s.toFloat() / seg) * 2f - 1f // -1..1
            for (t in 0..seg) {
                val v = (t.toFloat() / seg) * 2f - 1f
                // Point on the outer cube surface.
                val px = nDir[0] * ox + uDir[0] * (u * ox) + vDir[0] * (v * ox)
                val py = nDir[1] * oy + uDir[1] * (u * oy) + vDir[1] * (v * oy)
                val pz = nDir[2] * oz + uDir[2] * (u * oz) + vDir[2] * (v * oz)
                // Project onto the rounded box: q = clamp to inner box, push out by radius.
                val qx = px.coerceIn(-ix, ix)
                val qy = py.coerceIn(-iy, iy)
                val qz = pz.coerceIn(-iz, iz)
                var dx = px - qx
                var dy = py - qy
                var dz = pz - qz
                var len = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
                if (len < 1e-6f) {
                    dx = nDir[0]
                    dy = nDir[1]
                    dz = nDir[2]
                    len = 1f
                }
                val invLen = 1f / len
                val pp = vp * 3
                verts[pp] = qx + radius * dx * invLen
                verts[pp + 1] = qy + radius * dy * invLen
                verts[pp + 2] = qz + radius * dz * invLen
                normals[pp] = dx * invLen
                normals[pp + 1] = dy * invLen
                normals[pp + 2] = dz * invLen
                vp++
            }
        }
        val stride = seg + 1
        for (s in 0 until seg) {
            for (t in 0 until seg) {
                val a = base + s * stride + t
                val b = base + (s + 1) * stride + t
                val c = base + (s + 1) * stride + (t + 1)
                val d = base + s * stride + (t + 1)
                // uDir x vDir == outward normal, so (a,b,d),(b,c,d) faces outward.
                indices[ip++] = a
                indices[ip++] = b
                indices[ip++] = d
                indices[ip++] = b
                indices[ip++] = c
                indices[ip++] = d
            }
        }
        base += perFace
    }
    return defineMesh(id, indices, verts, normals)
}

/**
 * Convenience: define a wedge / right-triangular prism (a ramp): full height at {@code x=-w/2}
 * sloping down to zero at {@code x=+w/2}, extruded along Z by [depth]. Flat-faced — uploaded
 * without normals so the renderer flat-shades every face. CCW winding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineWedgeMesh(
    id: Int,
    width: Float = 1.4f,
    height: Float = 1.2f,
    depth: Float = 1.0f,
): RcMesh {
    val w = width * 0.5f
    val h = height * 0.5f
    val d = depth * 0.5f
    // Front (z=+d): A,B,C ; Back (z=-d): D,E,F.
    val verts =
        floatArrayOf(
            -w,
            -h,
            d, // A 0
            +w,
            -h,
            d, // B 1
            -w,
            +h,
            d, // C 2
            -w,
            -h,
            -d, // D 3
            +w,
            -h,
            -d, // E 4
            -w,
            +h,
            -d, // F 5
        )
    val indices =
        intArrayOf(
            0,
            1,
            2, // front (+Z)
            3,
            5,
            4, // back (-Z)
            0,
            3,
            4,
            0,
            4,
            1, // bottom (-Y)
            0,
            2,
            5,
            0,
            5,
            3, // left vertical (-X)
            2,
            1,
            4,
            2,
            4,
            5, // slope (+X,+Y)
        )
    return defineMesh(id, indices, verts, null)
}

/**
 * Convenience: define a flat ground plane in the XZ plane at {@code y=0}, [size] across, subdivided
 * [segments]×[segments], normal +Y. Single-sided (front-facing from above).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.definePlaneMesh(id: Int, size: Float = 2f, segments: Int = 1): RcMesh {
    val cols = segments + 1
    val verts = FloatArray(cols * cols * 3)
    val normals = FloatArray(cols * cols * 3)
    var p = 0
    for (i in 0..segments) {
        val z = (i.toFloat() / segments - 0.5f) * size
        for (j in 0..segments) {
            val x = (j.toFloat() / segments - 0.5f) * size
            verts[p] = x
            verts[p + 1] = 0f
            verts[p + 2] = z
            normals[p] = 0f
            normals[p + 1] = 1f
            normals[p + 2] = 0f
            p += 3
        }
    }
    val indices = IntArray(segments * segments * 6)
    var k = 0
    for (i in 0 until segments) {
        for (j in 0 until segments) {
            val a = i * cols + j
            val b = i * cols + (j + 1)
            val c = (i + 1) * cols + (j + 1)
            val dd = (i + 1) * cols + j
            indices[k++] = a
            indices[k++] = dd
            indices[k++] = b
            indices[k++] = b
            indices[k++] = dd
            indices[k++] = c
        }
    }
    return defineMesh(id, indices, verts, normals)
}

/**
 * Convenience: define a single flat quad (billboard) in the XY plane facing +Z, [width] by
 * [height], normal +Z. CCW winding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.defineQuadMesh(
    id: Int,
    width: Float = 1.6f,
    height: Float = 1.6f,
): RcMesh {
    val w = width * 0.5f
    val h = height * 0.5f
    val verts =
        floatArrayOf(
            -w,
            -h,
            0f, // bl 0
            +w,
            -h,
            0f, // br 1
            +w,
            +h,
            0f, // tr 2
            -w,
            +h,
            0f, // tl 3
        )
    val normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)
    val uv = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f) // bl, br, tr, tl
    val indices = intArrayOf(0, 1, 3, 1, 2, 3)
    return defineMesh(id, indices, verts, normals, uv)
}

// ---- MeshExpression3D typed front-ends --------------------------------------
//
// These build the RPN expression arrays from an [RcFloat] lambda and emit a single
// MeshExpression3D op. The lambda receives the two surface parameters as [RcFloat]s and returns
// an [RcFloat] expression; capture a reactive value (e.g. continuousSeconds()) in the lambda to
// animate the surface.

/** The first surface parameter (`VAR1`) as an [RcFloat] for use inside a mesh-expression lambda. */
private fun meshU(): RcFloat = RcFloat(null, floatArrayOf(AnimatedFloatExpression.VAR1))

/** The second surface parameter (`VAR2`). */
private fun meshV(): RcFloat = RcFloat(null, floatArrayOf(AnimatedFloatExpression.VAR2))

/** [MeshExpression3D.FLAG_FLIP_NORMALS] when [flipNormals], else 0. */
private fun normalsFlag(flipNormals: Boolean): Int =
    if (flipNormals) MeshExpression3D.FLAG_FLIP_NORMALS else 0

/**
 * Height-field surface `y = height(x, z)` over the X/Z grid `[xMin,xMax]×[zMin,zMax]`, clamped to
 * `[yMin,yMax]`. The single expression keeps this efficient. CCW winding (front-facing from +Y,
 * i.e. from above). Auto normals/UV.
 *
 * @param flipNormals reverse the normals without changing the winding
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.expressionSurface(
    id: Int,
    xMin: Float,
    xMax: Float,
    zMin: Float,
    zMax: Float,
    yMin: Float = -1e6f,
    yMax: Float = 1e6f,
    uSeg: Int = 64,
    vSeg: Int = 64,
    flipNormals: Boolean = false,
    height: (x: RcFloat, z: RcFloat) -> RcFloat,
): RcMesh =
    meshExpression(
        id = id,
        type = RcSurface.HEIGHT_FIELD,
        flags = normalsFlag(flipNormals),
        params =
            floatArrayOf(
                xMin,
                xMax,
                (uSeg + 1).toFloat(),
                zMin,
                zMax,
                (vSeg + 1).toFloat(),
                yMin,
                yMax,
            ),
        pos = arrayOf(height(meshU(), meshV()).toArray()),
    )

/**
 * Deformed sphere of base [radius]: the radius at each point is `radius + displacement(u, v)` where
 * `u`∈[0,2π] is longitude and `v`∈[0,π] is latitude. CCW winding (front-facing from outside). Auto
 * normals/UV.
 *
 * @param flipNormals reverse the normals without changing the winding
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.expressionSphere(
    id: Int,
    radius: Float = 1f,
    uSeg: Int = 64,
    vSeg: Int = 48,
    flipNormals: Boolean = false,
    displacement: (u: RcFloat, v: RcFloat) -> RcFloat,
): RcMesh =
    meshExpression(
        id = id,
        type = RcSurface.SPHERE,
        flags = MeshExpression3D.FLAG_FLIP_WINDING or normalsFlag(flipNormals),
        params = floatArrayOf(radius, (uSeg + 1).toFloat(), (vSeg + 1).toFloat()),
        pos = arrayOf(displacement(meshU(), meshV()).toArray()),
    )

/**
 * Deformed cylinder of base [radius] and [height]: the radius at each point is `radius +
 * displacement(u, v)` where `u`∈[0,2π] is the angle and `v` runs along the axis. Open ended. CCW
 * winding (front-facing from outside). Auto normals/UV.
 *
 * @param flipNormals reverse the normals without changing the winding
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.expressionCylinder(
    id: Int,
    radius: Float = 0.8f,
    height: Float = 1.8f,
    uSeg: Int = 64,
    vSeg: Int = 24,
    flipNormals: Boolean = false,
    displacement: (u: RcFloat, v: RcFloat) -> RcFloat,
): RcMesh =
    meshExpression(
        id = id,
        type = RcSurface.CYLINDER,
        flags = normalsFlag(flipNormals),
        params = floatArrayOf(radius, height, (uSeg + 1).toFloat(), (vSeg + 1).toFloat()),
        pos = arrayOf(displacement(meshU(), meshV()).toArray()),
    )

/**
 * Fully general parametric surface: position `P(u,v) = (x(u,v), y(u,v), z(u,v))` over the grid
 * `[uMin,uMax]×[vMin,vMax]`. Three expressions per vertex — the most flexible, least efficient
 * form. CCW winding: the front faces along `-(∂P/∂u × ∂P/∂v)` (+Y for `P = (u, 0, v)`); set [flip]
 * to make it face the other way. Auto normals/UV.
 *
 * @param flip reverse the winding, and with it the generated normals
 * @param flipNormals reverse the normals without changing the winding
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.expressionGeneral(
    id: Int,
    uMin: Float,
    uMax: Float,
    vMin: Float,
    vMax: Float,
    uSeg: Int = 48,
    vSeg: Int = 48,
    flip: Boolean = false,
    flipNormals: Boolean = false,
    x: (u: RcFloat, v: RcFloat) -> RcFloat,
    y: (u: RcFloat, v: RcFloat) -> RcFloat,
    z: (u: RcFloat, v: RcFloat) -> RcFloat,
): RcMesh =
    meshExpression(
        id = id,
        type = RcSurface.GENERAL,
        flags = (if (flip) MeshExpression3D.FLAG_FLIP_WINDING else 0) or normalsFlag(flipNormals),
        params = floatArrayOf(uMin, uMax, (uSeg + 1).toFloat(), vMin, vMax, (vSeg + 1).toFloat()),
        pos =
            arrayOf(
                x(meshU(), meshV()).toArray(),
                y(meshU(), meshV()).toArray(),
                z(meshU(), meshV()).toArray(),
            ),
    )

/**
 * Convenience: set a perspective camera with a degrees-based fovY. Same semantics as
 * [RcScene3DScope.perspectiveCamera] but with the input in degrees.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.perspectiveCameraDeg(
    fovYDeg: Float,
    aspect: Float,
    near: Float,
    far: Float,
    eye: FloatArray,
    center: FloatArray,
    up: FloatArray,
) {
    perspectiveCamera(
        fovYRad = (fovYDeg * Math.PI.toFloat() / 180f),
        aspect = aspect,
        near = near,
        far = far,
        eye = eye,
        center = center,
        up = up,
    )
}

/**
 * Reactive [perspectiveCameraDeg]: [fovYDeg] (degrees) and the [eye]/[center] components are
 * [RcFloat]s, so the camera animates on the player — animated FOV, dolly, orbit — with no document
 * re-upload (resolved by the variable-aware
 * [SetCamera3D][androidx.compose.remote.core.operations.d3.SetCamera3D]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.perspectiveCameraDeg(
    fovYDeg: RcFloat,
    aspect: Float,
    near: Float,
    far: Float,
    eye: Array<RcFloat>,
    center: Array<RcFloat>,
    up: FloatArray,
) {
    perspectiveCamera(
        fovYRad = fovYDeg * (Math.PI.toFloat() / 180f),
        aspect = aspect,
        near = near,
        far = far,
        eye = eye,
        center = center,
        up = up,
    )
}

/** [perspectiveCameraDeg] with vec3 [eye]/[center] and a constant fov. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.perspectiveCameraDeg(
    fovYDeg: Float,
    aspect: Float,
    near: Float,
    far: Float,
    eye: RcVec,
    center: RcVec,
    up: FloatArray,
) {
    perspectiveCamera(
        RcFloat(fovYDeg * (Math.PI.toFloat() / 180f)),
        aspect,
        near,
        far,
        eye,
        center,
        up,
    )
}

/** Reactive [perspectiveCameraDeg] with vec3 [eye]/[center] and a reactive fov. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public fun RcScene3DScope.perspectiveCameraDeg(
    fovYDeg: RcFloat,
    aspect: Float,
    near: Float,
    far: Float,
    eye: RcVec,
    center: RcVec,
    up: FloatArray,
) {
    perspectiveCamera(fovYDeg * (Math.PI.toFloat() / 180f), aspect, near, far, eye, center, up)
}

/**
 * The render mode of [RcScene3DScope.drawMesh]: an engine and shading choice, combined with flags
 * using [or]. Reserved future engines: `SHADER` and `GL`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcMeshMode internal constructor(internal val value: Int) {
    /** Combines this mode with the flags of [other]. */
    public infix fun or(other: RcMeshMode): RcMeshMode = RcMeshMode(value or other.value)

    /** True if every bit of [other] is set in this mode. */
    public operator fun contains(other: RcMeshMode): Boolean =
        (value and other.value) == other.value

    override fun toString(): String = "RcMeshMode(0x${value.toString(16)})"

    public companion object {
        /** Software CPU rasterizer (per-pixel depth buffer), flat per-face shading. */
        public val SoftwareFlat: RcMeshMode = RcMeshMode(Paint3DContext.MODE_SOFTWARE_FLAT)

        /**
         * Software CPU rasterizer (per-pixel depth buffer), smooth per-vertex (Gouraud) shading.
         */
        public val SoftwareSmooth: RcMeshMode = RcMeshMode(Paint3DContext.MODE_SOFTWARE_SMOOTH)

        /**
         * Host `Canvas.drawVertices` (painter-ordered, GPU fill, no depth buffer), flat shading.
         */
        public val CanvasFlat: RcMeshMode = RcMeshMode(Paint3DContext.MODE_CANVAS_FLAT)

        /**
         * Host `Canvas.drawVertices` (painter-ordered, GPU fill, no depth buffer), smooth shading.
         */
        public val CanvasSmooth: RcMeshMode = RcMeshMode(Paint3DContext.MODE_CANVAS_SMOOTH)

        /**
         * Host `Canvas.drawMesh` (API 34+, painter-ordered, GPU fill, no depth buffer), flat
         * shading. Falls back to software on devices below API 34.
         */
        public val DrawMeshFlat: RcMeshMode = RcMeshMode(Paint3DContext.MODE_DRAWMESH_FLAT)

        /**
         * Host `Canvas.drawMesh` (API 34+, painter-ordered, GPU fill, no depth buffer), smooth
         * per-vertex (Gouraud) shading. Falls back to software on devices below API 34.
         */
        public val DrawMeshSmooth: RcMeshMode = RcMeshMode(Paint3DContext.MODE_DRAWMESH_SMOOTH)

        /**
         * Global painter-sorted `Canvas.drawVertices`: collects triangles across all meshes in the
         * scene, sorts them back-to-front by average depth at [Paint3DContext.endFrame3D], and
         * draws them in a single `Canvas.drawVertices` batch with flat (per-face) shading.
         */
        public val DrawMeshZBufFlat: RcMeshMode = RcMeshMode(Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT)

        /**
         * Global painter-sorted `Canvas.drawVertices`: collects triangles across all meshes in the
         * scene, sorts them back-to-front by average depth at [Paint3DContext.endFrame3D], and
         * draws them in a single `Canvas.drawVertices` batch with smooth (Gouraud per-vertex)
         * shading.
         */
        public val DrawMeshZBufSmooth: RcMeshMode =
            RcMeshMode(Paint3DContext.MODE_DRAWMESH_ZBUF_SMOOTH)

        /**
         * Hidden-line wireframe (software, depth-buffered): transparent faces, edges drawn in the
         * current paint with edges behind nearer surface occluded. Draw a shaded pass then a
         * `SoftwareWireframe` pass of the same mesh for a shaded+wireframe look.
         */
        public val SoftwareWireframe: RcMeshMode =
            RcMeshMode(Paint3DContext.MODE_SOFTWARE_WIREFRAME)

        /**
         * Hidden-line wireframe drawing only each triangle's **first** edge (`v0→v1`). With a
         * consistent triangle emit order, these edges form one family of the mesh's grid lines — a
         * quick visual check that a generator's triangulation order is consistent.
         */
        public val SoftwareWireframe1: RcMeshMode =
            RcMeshMode(Paint3DContext.MODE_SOFTWARE_WIREFRAME1)

        /** Hidden-line wireframe drawing only each triangle's **second** edge (`v1→v2`). */
        public val SoftwareWireframe2: RcMeshMode =
            RcMeshMode(Paint3DContext.MODE_SOFTWARE_WIREFRAME2)

        /** Hidden-line wireframe drawing each triangle's **first and second** edges. */
        public val SoftwareWireframe3: RcMeshMode =
            RcMeshMode(Paint3DContext.MODE_SOFTWARE_WIREFRAME3)

        /**
         * Style flag OR-ed into any mode to add the hidden-line wireframe (forces the software
         * backend). E.g. `SOFTWARE_SMOOTH or WIREFRAME` is equivalent to drawing edges over the
         * surface, but as a single pass it draws edges only (transparent faces) — use
         * [SoftwareWireframe] for the common case.
         */
        public val Wireframe: RcMeshMode = RcMeshMode(Paint3DContext.MODE_WIREFRAME)

        /** Edge-selection bit: draw each triangle's first edge (`v0→v1`). OR into [Wireframe]. */
        public val WireEdge0: RcMeshMode = RcMeshMode(Paint3DContext.WIRE_EDGE0)

        /** Edge-selection bit: draw each triangle's second edge (`v1→v2`). OR into [Wireframe]. */
        public val WireEdge1: RcMeshMode = RcMeshMode(Paint3DContext.WIRE_EDGE1)

        /** Edge-selection bit: draw each triangle's third edge (`v2→v0`). OR into [Wireframe]. */
        public val WireEdge2: RcMeshMode = RcMeshMode(Paint3DContext.WIRE_EDGE2)

        /**
         * Flag OR-ed into any mode: draw the mesh see-through, blending it over what is already
         * drawn by the paint alpha. It is depth-tested but does not write depth, so draw
         * transparent meshes after the opaque ones, farthest first.
         */
        public val Transparent: RcMeshMode = RcMeshMode(Paint3DContext.MODE_TRANSPARENT)

        /**
         * Flag OR-ed into any mode: prefer a GPU engine. A hint only: renderers without one ignore
         * it.
         */
        public val Gpu: RcMeshMode = RcMeshMode(Paint3DContext.MODE_GPU)

        /**
         * Flag OR-ed into any mode: ask for physically based shading. A hint only: renderers
         * without it use the standard lighting.
         */
        public val Pbr: RcMeshMode = RcMeshMode(Paint3DContext.MODE_PBR)

        /**
         * Flag OR-ed into any mode: draw back faces too, lit from their own side, for open or
         * single-sheet meshes (a plane, a leaf, a cut-away shell) that are seen from both sides.
         */
        public val DoubleSided: RcMeshMode = RcMeshMode(Paint3DContext.MODE_DOUBLE_SIDED)

        /**
         * Flag OR-ed into any mode: make the triangles that are clockwise as the viewer sees them
         * the front faces, like `glFrontFace(GL_CW)`, for a mesh wound the other way round.
         * Back-face culling, [DoubleSided] and flat shading follow it.
         */
        public val FrontCw: RcMeshMode = RcMeshMode(Paint3DContext.MODE_FRONT_CW)
    }
}

/** Light-type constants of the light set. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public object RcLight3D {
    public const val DIRECTIONAL: Int = Paint3DContext.LIGHT_DIRECTIONAL
    public const val POINT: Int = Paint3DContext.LIGHT_POINT

    /** Ambient light: lights every surface equally; see [RcScene3DScope.ambientLight]. */
    public const val AMBIENT: Int = Paint3DContext.LIGHT_AMBIENT
}

/** Model-matrix sub-op constants, for readability at call sites. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public object RcMatrix3D {
    public const val IDENTITY: Int = Paint3DContext.M3_IDENTITY
    public const val TRANSLATE: Int = Paint3DContext.M3_TRANSLATE
    public const val SCALE: Int = Paint3DContext.M3_SCALE
    public const val ROTATE_AXIS: Int = Paint3DContext.M3_ROTATE_AXIS
    public const val MULTIPLY: Int = Paint3DContext.M3_MULTIPLY

    /** Save the model matrix; see [RcScene3DScope.pushMatrix]. */
    public const val PUSH: Int = Paint3DContext.M3_PUSH

    /** Restore the model matrix; see [RcScene3DScope.popMatrix]. */
    public const val POP: Int = Paint3DContext.M3_POP
}
