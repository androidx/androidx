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
package androidx.compose.remote.core.operations.utilities.d3;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.easing.MonotonicCurveFit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Procedural geometry for {@code MeshPrimitive}: builds a triangulated {@link MeshData} (positions
 * + per-vertex normals + CCW-from-outside indices) for a small set of parametric solids. All
 * meshes wind counter-clockwise as seen from outside (outward normal = front-facing), matching the
 * rest of the 3D pipeline.
 *
 * <p>Each solid is assembled from reusable pieces ({@link #sphereBand}, {@link #disk},
 * {@link #ring}) via {@link MeshData#concat}, so the shapes stay short and the winding/normal
 * conventions live in one place.
 *
 * <p>Angles are in radians. The trailing {@code (x,y,z)} of most types translates the whole shape.
 *
 * <p><b>Native axes.</b> Upright and revolved solids are built around +Y: {@link #SPHERE},
 * {@link #CONE} (apex up), {@link #CUBE}, {@link #ROUNDED_CUBE}, {@link #SPHERICAL_SECTOR} and
 * {@link #SPHERICAL_DOME} (opening up), {@link #TORUS} (hole along Y), {@link #LATHE},
 * {@link #HELIX} (coil axis) and {@link #ICOSPHERE}. Flat and extruded shapes face +Z:
 * {@link #PLANE} and the {@code EXTRUDE_*} types (profile in XY, depth along Z).
 * {@link #CYLINDER}, the tubes and {@link #SWEEP} follow their explicit points and have no native
 * axis. The axis field of the flags ({@link #axisMode}) rigidly rotates a finished Y- or
 * Z-native mesh about its pivot (its center, or the cone's base center) so the native axis points
 * along +X, +Y or +Z instead.
 *
 * <p><b>UV</b> ({@link #uvMode}; generated for every type, UV2/UV3 alias UV). Coordinates stay in
 * [0,1] and read unmirrored from outside with v = 0 at the bottom, except the torus:
 *
 * <ul>
 *   <li>{@link #SPHERE}, {@link #ICOSPHERE} and the sector/dome caps: longitude/latitude globe.
 *   <li>{@link #PLANE}, {@link #CUBE}, {@link #ROUNDED_CUBE}: the unit square on each face.
 *   <li>{@link #CYLINDER}, {@link #CONE}, {@link #LATHE}: u once around, v along the axis or
 *       profile; flat caps are disks inscribed in the unit square.
 *   <li>{@link #TORUS}: u around the ring, v around the tube (mirrored, kept for compatibility).
 *   <li>{@code EXTRUDE_*}: caps planar over the profile's bounding box (back cap mirrored so it
 *       reads correctly from behind), walls u = perimeter fraction, v = 0 back to 1 front.
 *   <li>Tubes, {@link #SWEEP}, {@link #HELIX}: u = arc-length fraction along the path, v around
 *       the section; caps as disks (tubes) or planar over the section (sweep).
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Primitive3D {
    private Primitive3D() {
    }

    // Type ids (wire + DSL).
    public static final int SPHERE = 0;
    public static final int CYLINDER = 1;
    public static final int CONE = 2;
    public static final int CUBE = 3;
    public static final int ROUNDED_CUBE = 4;
    public static final int SPHERICAL_SECTOR = 5;
    public static final int SPHERICAL_DOME = 6;
    public static final int TUBE = 7;
    public static final int CAP_TUBE = 8;
    public static final int PROFILE_TUBE = 9;
    public static final int TORUS = 10;
    public static final int PLANE = 11;
    // Extruded 2D shapes: a flat profile in the XY plane swept along Z by a `depth`, giving a prism
    // with two cap faces (the 2D shape) + a side wall along the profile boundary. Angles in
    // radians.
    public static final int EXTRUDE_CIRCLE = 12;
    public static final int EXTRUDE_SECTOR = 13;
    public static final int EXTRUDE_SEGMENT = 14;
    public static final int EXTRUDE_ARC = 15;
    public static final int EXTRUDE_ROUNDED_RECT = 16;
    public static final int EXTRUDE_SQUIRCLE = 17;
    /**
     * Extrude an arbitrary 2D path (one or more closed contours, e.g. the glyph outlines of text)
     * along Z by a {@code depth}. The contours are supplied pre-flattened to polylines in the
     * {@code params} (see {@link #extrudePath}); holes (counters of letters like {@code e}/
     * {@code o})
     * and overlapping contours are handled by nonzero-winding fill, so author-side triangulation is
     * not needed.
     */
    public static final int EXTRUDE_PATH = 18;
    /**
     * Revolve a 2D profile around the Y axis (a lathe). Multi-channel: {@code data[0] = [cx,cy,
     * cz]},
     * {@code data[1] = [r0,y0, r1,y1, …]} — radius-from-axis / height pairs, swept {@code segments}
     * times around Y. Generalizes sphere/cylinder/cone/torus; a profile that reaches {@code r=0} at
     * an end closes there. Unlocks vases, goblets, bottles, lamp bases, etc.
     */
    public static final int LATHE = 19;
    /**
     * Sweep a 2D cross-section along a 3D path (a loft/extrude-along-path). Multi-channel:
     * {@code data[0] = [closedXSection, capStart, capEnd, cx,cy,cz]}, {@code data[1]} = cross
     * -section
     * {@code [x0,y0, …]} (in the frame's XY), {@code data[2]} = path {@code [x0,y0,z0, …]},
     * {@code data[3]} = optional per-station scale (1/station), {@code data[4]} = optional
     * per-station
     * twist (rad/station). Parallel-transport frames keep the section from spinning along the path.
     * Generalizes the circular tube to any cross-section: rails, ribbons, pipes, springs, screws.
     */
    public static final int SWEEP = 20;
    /**
     * A coil/spring: a circular tube swept along an internally-generated helix. Params
     * {@code [coilRadius, tubeRadius, pitch, turns, cx,cy,cz]} ({@code pitch} = rise per turn);
     * {@code segments} = tube facets. Built on {@link #sweep} — springs, screws, coils, helices.
     */
    public static final int HELIX = 21;
    /**
     * Geodesic sphere from a subdivided icosahedron — uniform, evenly-sized triangles (unlike the
     * pole-pinching lat/long {@link #SPHERE}). Params {@code [radius, cx,cy,cz]}; {@code segments}
     * selects the subdivision level (0..4). Better for displacement/deformation, planets, dice.
     */
    public static final int ICOSPHERE = 22;
    /**
     * An infinite plane, optionally with a grid. Params ({@link #INFINITE_PLANE_PARAMS}
     * floats): {@code [cx,cy,cz, spacing, lineWidth, lineA,lineR,lineG,lineB]} — a point on the
     * plane, the grid cell size (0 = no grid), the grid line width in pixels and the grid line
     * color (ARGB, each 0..1). {@code segments} is unused. The normal is +Y (native axis Y), or the
     * axis field of the flags. Renderers draw it through {@code Paint3DContext
     * .defineInfinitePlane3D}; {@link #build} returns the finite fallback quad.
     */
    public static final int INFINITE_PLANE = 23;
    /** Number of params of {@link #INFINITE_PLANE}. */
    public static final int INFINITE_PLANE_PARAMS = 9;

    // Flags (per-type sampling mask).
    /**
     * Tube/cap-tube legacy mode: within {@link #FLAG_TUBE_LEGACY}, densify the control points with
     * a Catmull-Rom spline ({@code segments} samples per span) before sweeping.
     */
    public static final int FLAG_SPLINE = 0x1;
    /**
     * Tube/cap-tube: use the legacy parallel-transport sweep over the raw control polyline instead
     * of the default monotonic-spline path (disks oriented by the spline tangent).
     */
    public static final int FLAG_TUBE_LEGACY = 0x2;
    /**
     * Tube/cap-tube: the params carry an explicit "rings per control span" float at index 1 (points
     * then start at index 2), overriding the adaptive along-path density.
     */
    public static final int FLAG_TUBE_PATH_DENSITY = 0x4;
    /** Profile tube: close the two ends with flat disks (profile tube selects caps by flag). */
    public static final int FLAG_TUBE_CAP = 0x8;
    /**
     * Tube/cap-tube/profile tube (default spline path): place the rings along the path by
     * curvature — one per cross-section facet angle of turning, independent of the radius —
     * instead of by arc length, so thin tubes are not over-tessellated and straight runs get a
     * single ring per span. An explicit {@link #FLAG_TUBE_PATH_DENSITY} count still wins;
     * ignored by {@link #FLAG_TUBE_LEGACY}.
     */
    public static final int FLAG_TUBE_CURVATURE_DENSITY = 0x100;

    // ---- UV-mapping mode: a 2-bit field (bits 4-5) selecting which UV pattern to generate, so a
    // primitive mesh can be textured. UV_NONE = no UV. UV = the type's common/default mapping; UV2,
    // UV3 are reserved for alternative patterns (currently aliased to the common one).
    public static final int FLAG_UV_SHIFT = 4;
    public static final int FLAG_UV_MASK = 0x3 << FLAG_UV_SHIFT; // 0x30
    public static final int UV_NONE = 0;
    public static final int UV = 1;
    public static final int UV2 = 2;
    public static final int UV3 = 3;

    /** Extract the 2-bit UV mode (0..3) from a flags value. */
    public static int uvMode(int flags) {
        return (flags & FLAG_UV_MASK) >> FLAG_UV_SHIFT;
    }

    // ---- Axis remap: a 2-bit field (bits 6-7). AXIS_NATIVE keeps each type's built-in
    // orientation (see the class docs); AXIS_X/Y/Z rigidly rotate the finished mesh about the
    // type's pivot so its native axis points along +X/+Y/+Z. Types placed by explicit points
    // (cylinder, tubes, sweep) ignore it.
    public static final int FLAG_AXIS_SHIFT = 6;
    public static final int FLAG_AXIS_MASK = 0x3 << FLAG_AXIS_SHIFT; // 0xC0
    public static final int AXIS_NATIVE = 0;
    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;
    public static final int AXIS_Z = 3;

    /** Extract the 2-bit axis mode ({@link #AXIS_NATIVE}..{@link #AXIS_Z}) from a flags value. */
    public static int axisMode(int flags) {
        return (flags & FLAG_AXIS_MASK) >> FLAG_AXIS_SHIFT;
    }

    // Fallback tessellation when segments <= 0 (i.e. "use a sensible default").
    private static final int DEFAULT_RADIAL = 24;
    private static final int DEFAULT_TUBE_SIDES = 14;
    private static final int DEFAULT_ROUND_SEG = 4;
    // Cap on monotonic-spline path rings per control span (the count is otherwise adaptive: the
    // span's estimated arc length / the cross-section edge length). Bounds runaway tessellation.
    private static final int MAX_TUBE_PER_SPAN = 256;
    // Debug-only diagnostics (extrudePath bevel self-intersection warnings). Keep false.
    private static final boolean DEBUG = false;
    // extrudePath: consecutive contour points closer than this fraction of the path's extent are
    // merged (and the duplicate closing point dropped).
    private static final float CONTOUR_MERGE_EPS = 1e-6f;
    // extrudePath bevel: each inset edge keeps at least this fraction of its length.
    private static final float INSET_MIN_EDGE_FRACTION = 0.1f;
    // FLAG_TUBE_CURVATURE_DENSITY: turning-angle samples per spline span.
    private static final int CURVATURE_SAMPLES = 16;
    // Icosphere UV: |z| below this counts as lying on the seam plane z = 0.
    private static final float ICO_SEAM_EPS = 1e-6f;
    // extrudePath caps: triangles more elongated than this (longest edge squared over twice the
    // area) seed the edge-flip repair.
    private static final double CAP_FLIP_ASPECT = 100.0;

    /**
     * Dispatch a (type, segments, flags, params) tuple to the matching builder. {@code segments}
     * is the primary radial/longitudinal division count (rounded; clamped per type); a value
     * {@code <= 0} means "use the type's default". Besides the per-type sampling bits,
     * {@code flags} select the UV mapping ({@link #uvMode}) and an axis remap ({@link #axisMode}),
     * applied last as a rigid rotation of the finished mesh about the type's pivot.
     */
    @NonNull
    public static MeshData build(int type, float segments, int flags, float @NonNull [][] data) {
        MeshData mesh = buildNative(type, segments, flags, data);
        int axis = axisMode(flags);
        if (axis != AXIS_NATIVE) {
            remapAxis(mesh, type, axis, data[0]);
        }
        return mesh;
    }

    /** {@link #build} in the type's native orientation (no axis remap). */
    @NonNull
    private static MeshData buildNative(
            int type, float segments, int flags, float @NonNull [][] data) {
        // data[0] = scalar params (radius, depth, x,y,z, …) — the only channel for most shapes.
        // Channels 1+ carry geometry streams (profile / path points) for the multi-channel types.
        float[] p = data[0];
        // UV mode (bits 4-5). Every type generates its default mapping when it is not UV_NONE
        // (documented on each builder); UV2/UV3 currently alias UV.
        int uv = uvMode(flags);
        switch (type) {
            case SPHERE: {
                int slices = segCount(segments, DEFAULT_RADIAL, 3);
                int stacks = Math.max(2, Math.round(slices * 0.5f));
                return sphere(p[0], p[1], p[2], p[3], slices, stacks, uv);
            }
            case CYLINDER:
                return cylinder(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                        segCount(segments, DEFAULT_RADIAL, 3), uv);
            case CONE:
                return cone(p[0], p[1], p[2], p[3], p[4], segCount(segments, DEFAULT_RADIAL, 3),
                        uv);
            case CUBE:
                return cube(p[0], p[1], p[2], p[3], p[4], p[5], uv);
            case ROUNDED_CUBE:
                return roundedCube(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                        segCount(segments, DEFAULT_ROUND_SEG, 1), uv);
            case SPHERICAL_SECTOR: {
                int slices = segCount(segments, DEFAULT_RADIAL, 3);
                int stacks = Math.max(2, Math.round(slices / 3f));
                return sphericalSector(p[0], p[1], p[2], p[3], p[4], slices, stacks, uv);
            }
            case SPHERICAL_DOME: {
                int slices = segCount(segments, DEFAULT_RADIAL, 3);
                int stacks = Math.max(2, Math.round(slices / 3f));
                return sphericalDome(p[0], p[1], p[2], p[3], p[4], slices, stacks, uv);
            }
            case TUBE:
                return tube(p, false, segments, flags);
            case CAP_TUBE:
                return tube(p, true, segments, flags);
            case PROFILE_TUBE:
                return profileTube(p, segCount(segments, DEFAULT_TUBE_SIDES, 3), flags);
            case TORUS: {
                float majorR = p[0];
                float minorR = p[1];
                int majorSeg = segCount(segments, DEFAULT_RADIAL, 3);
                // Minor (tube) segments scaled to the radius ratio so the quads stay ~square.
                float ratio = Math.abs(majorR) > 1e-6f ? Math.abs(minorR / majorR) : 0.5f;
                int minorSeg = Math.max(3, Math.round(majorSeg * ratio));
                return torus(majorR, minorR, p[2], p[3], p[4], majorSeg, minorSeg, uv);
            }
            case PLANE:
                return plane(p[0], p[1], p[2], p[3], p[4], uv);
            case EXTRUDE_CIRCLE:
                // (radius, depth, cx, cy, cz); segments = sides around.
                return extrudeCircle(p[0], p[1], p[2], p[3], p[4],
                        segCount(segments, DEFAULT_RADIAL, 3), uv);
            case EXTRUDE_SECTOR:
                // (radius, startAngle, sweepAngle, depth, cx, cy, cz); segments = arc divisions.
                return extrudeSector(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                        segCount(segments, DEFAULT_RADIAL, 1), uv);
            case EXTRUDE_SEGMENT:
                // (radius, startAngle, sweepAngle, depth, cx, cy, cz); segments = arc divisions.
                return extrudeSegment(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                        segCount(segments, DEFAULT_RADIAL, 1), uv);
            case EXTRUDE_ARC:
                // (innerR, outerR, startAngle, sweepAngle, depth, cx, cy, cz); seg = arc divisions.
                return extrudeArc(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                        segCount(segments, DEFAULT_RADIAL, 1), uv);
            case EXTRUDE_ROUNDED_RECT:
                // (width, height, cornerRadius, depth, cx, cy, cz); segments = per-corner
                // divisions.
                return extrudeRoundedRect(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                        segCount(segments, 6, 1), uv);
            case EXTRUDE_SQUIRCLE:
                // (radius, exponent, depth, cx, cy, cz); segments = perimeter samples.
                return extrudeSquircle(p[0], p[1], p[2], p[3], p[4], p[5],
                        segCount(segments, 48, 8), uv);
            case EXTRUDE_PATH:
                // (depth, cx, cy, cz, contourCount, [count, x0,y0,...] per contour); segments
                // unused.
                return extrudePath(p, uv);
            case LATHE:
                // data[0] = [cx,cy,cz]; data[1] = profile [r0,y0,...]; segments = sides around Y.
                return lathe(data.length > 1 ? data[1] : new float[0],
                        p.length > 0 ? p[0] : 0f, p.length > 1 ? p[1] : 0f,
                        p.length > 2 ? p[2] : 0f,
                        segCount(segments, DEFAULT_RADIAL, 3), uv);
            case SWEEP:
                // data[0]=[closedX,capStart,capEnd,cx,cy,cz]; [1]=cross-section; [2]=path;
                // [3]=per-station scale (optional); [4]=per-station twist (optional).
                return sweep(p, data.length > 1 ? data[1] : new float[0],
                        data.length > 2 ? data[2] : new float[0],
                        data.length > 3 ? data[3] : null,
                        data.length > 4 ? data[4] : null, uv);
            case HELIX:
                // [coilRadius, tubeRadius, pitch, turns, cx,cy,cz]; segments = tube facets.
                return helix(p[0], p[1], p[2], p[3],
                        p.length > 4 ? p[4] : 0f, p.length > 5 ? p[5] : 0f,
                        p.length > 6 ? p[6] : 0f,
                        segCount(segments, 12, 3), uv);
            case ICOSPHERE:
                // [radius, cx,cy,cz]; segments = subdivision level (0..4).
                return icosphere(p[0], p.length > 1 ? p[1] : 0f, p.length > 2 ? p[2] : 0f,
                        p.length > 3 ? p[3] : 0f,
                        Math.max(0, Math.min(4, segments > 0f ? Math.round(segments) : 2)), uv);
            case INFINITE_PLANE:
                // [cx,cy,cz, spacing, lineWidth, lineA,lineR,lineG,lineB]: the fallback quad only
                // uses the center (no grid); segments unused.
                return infinitePlaneFallback(p.length > 0 ? p[0] : 0f, p.length > 1 ? p[1] : 0f,
                        p.length > 2 ? p[2] : 0f, uv);
            default:
                throw new IllegalArgumentException("MeshPrimitive: unknown type " + type);
        }
    }

    /**
     * Convenience for the common single-channel case: wraps {@code params} as the sole {@code data}
     * channel. Existing callers (and shapes that need only scalars) use this; multi-channel shapes
     * (lathe profile, sweep cross-section + path) call the {@code float[][]} form directly.
     */
    @NonNull
    public static MeshData build(int type, float segments, int flags, float @NonNull [] params) {
        return build(type, segments, flags, new float[][]{params});
    }

    /**
     * The axis {@code type} is built around ({@link #AXIS_Y} or {@link #AXIS_Z}, see the class
     * docs), or {@link #AXIS_NATIVE} for the types placed by explicit points, which ignore the
     * axis remap.
     */
    static int nativeAxis(int type) {
        switch (type) {
            case SPHERE:
            case CONE:
            case CUBE:
            case ROUNDED_CUBE:
            case SPHERICAL_SECTOR:
            case SPHERICAL_DOME:
            case TORUS:
            case LATHE:
            case HELIX:
            case ICOSPHERE:
            case INFINITE_PLANE:
                return AXIS_Y;
            case PLANE:
            case EXTRUDE_CIRCLE:
            case EXTRUDE_SECTOR:
            case EXTRUDE_SEGMENT:
            case EXTRUDE_ARC:
            case EXTRUDE_ROUNDED_RECT:
            case EXTRUDE_SQUIRCLE:
            case EXTRUDE_PATH:
                return AXIS_Z;
            default:
                return AXIS_NATIVE;
        }
    }

    /**
     * Index in {@code data[0]} of the pivot {@code (x,y,z)} the axis remap rotates about: the
     * type's center (the base center for {@link #CONE}).
     */
    private static int pivotIndex(int type) {
        switch (type) {
            case SPHERE:
            case ICOSPHERE:
            case EXTRUDE_PATH:
                return 1;
            case CONE:
            case SPHERICAL_SECTOR:
            case SPHERICAL_DOME:
            case TORUS:
            case PLANE:
            case EXTRUDE_CIRCLE:
                return 2;
            case CUBE:
            case ROUNDED_CUBE:
            case EXTRUDE_SQUIRCLE:
                return 3;
            case EXTRUDE_SECTOR:
            case EXTRUDE_SEGMENT:
            case EXTRUDE_ROUNDED_RECT:
            case HELIX:
                return 4;
            case EXTRUDE_ARC:
                return 5;
            default: // LATHE: data[0] = [cx,cy,cz]; INFINITE_PLANE: [cx,cy,cz, …]
                return 0;
        }
    }

    /**
     * Rotate {@code mesh} in place so the native axis of {@code type} points along {@code axis}:
     * a rigid quarter turn about the remaining axis through the pivot ({@link #pivotIndex}; a
     * missing pivot param counts as 0), applied to positions and normals (winding and UV are
     * unaffected). No-op when the type has no native axis or already points that way.
     */
    private static void remapAxis(
            @NonNull MeshData mesh, int type, int axis, float @NonNull [] p) {
        int from = nativeAxis(type);
        if (from == AXIS_NATIVE || from == axis) {
            return;
        }
        int pi = pivotIndex(type);
        float ox = pi < p.length ? p[pi] : 0f;
        float oy = pi + 1 < p.length ? p[pi + 1] : 0f;
        float oz = pi + 2 < p.length ? p[pi + 2] : 0f;
        rotateQuarter(mesh.verts, from, axis, ox, oy, oz);
        rotateQuarter(mesh.normals, from, axis, 0f, 0f, 0f);
    }

    /**
     * Quarter-turn the x,y,z triples of {@code a} about {@code (ox,oy,oz)} so +{@code from}
     * ({@link #AXIS_Y} or {@link #AXIS_Z}) maps onto +{@code to}. Only the two rotated
     * components are rewritten.
     */
    private static void rotateQuarter(
            float @NonNull [] a, int from, int to, float ox, float oy, float oz) {
        if (from == AXIS_Y && to == AXIS_X) { // about Z: +Y -> +X, +X -> -Y
            for (int i = 0; i + 2 < a.length; i += 3) {
                float x = a[i] - ox;
                float y = a[i + 1] - oy;
                a[i] = ox + y;
                a[i + 1] = oy - x;
            }
        } else if (from == AXIS_Y && to == AXIS_Z) { // about X: +Y -> +Z, +Z -> -Y
            for (int i = 0; i + 2 < a.length; i += 3) {
                float y = a[i + 1] - oy;
                float z = a[i + 2] - oz;
                a[i + 1] = oy - z;
                a[i + 2] = oz + y;
            }
        } else if (from == AXIS_Z && to == AXIS_X) { // about Y: +Z -> +X, +X -> -Z
            for (int i = 0; i + 2 < a.length; i += 3) {
                float x = a[i] - ox;
                float z = a[i + 2] - oz;
                a[i] = ox + z;
                a[i + 2] = oz - x;
            }
        } else if (from == AXIS_Z && to == AXIS_Y) { // about X: +Z -> +Y, +Y -> -Z
            for (int i = 0; i + 2 < a.length; i += 3) {
                float y = a[i + 1] - oy;
                float z = a[i + 2] - oz;
                a[i + 1] = oy + z;
                a[i + 2] = oz - y;
            }
        }
    }

    /**
     * Revolve a 2D {@code profile} (as {@code r,y} pairs — radius from the Y axis, height)
     * around the
     * Y axis with {@code seg} sides, centered at {@code (cx,cy,cz)}. The lathe analogue of the
     * extrude family: each profile vertex becomes a ring; consecutive rings are joined by a quad
     * strip. Per-vertex normals come from the profile's 2D outward normal {@code (dy,-dr)} rotated
     * around the axis, so the surface shades smoothly. Winds CCW-from-outside (outward = away from
     * the axis). A profile point at {@code r=0} collapses its ring to a point (a closed cap/tip).
     *
     * <p>UV ({@code uvMode != UV_NONE}): u wraps once around like the {@link #sphere}
     * (u = 1 - angle/2π) and v is the arc-length fraction along the profile (0 at its first
     * point, 1 at its last), so a bottom-to-top profile reads upright and unmirrored from outside.
     */
    @NonNull
    public static MeshData lathe(
            float @NonNull [] profile, float cx, float cy, float cz, int seg, int uvMode) {
        int nProf = profile.length / 2;
        if (nProf < 2) {
            throw new IllegalArgumentException("MeshPrimitive lathe: need >= 2 profile points");
        }
        int cols = seg + 1; // duplicate the seam column (j = seg == j = 0) for clean normals
        int nv = nProf * cols;
        float[] verts = new float[nv * 3];
        float[] normals = new float[nv * 3];
        float[] uv = uvMode != UV_NONE ? new float[nv * 2] : null;
        float[] along = uv != null ? profileFractions(profile, nProf) : null;

        // Per-profile-vertex 2D outward normal (nr, ny), averaged from adjacent profile edges.
        float[] pnr = new float[nProf];
        float[] pny = new float[nProf];
        for (int e = 0; e < nProf - 1; e++) {
            float dr = profile[(e + 1) * 2] - profile[e * 2];
            float dy = profile[(e + 1) * 2 + 1] - profile[e * 2 + 1];
            float nr = dy; // outward perpendicular of the (dr,dy) edge, pointing away from the axis
            float ny = -dr;
            float len = (float) Math.sqrt(nr * nr + ny * ny);
            if (len > 1e-9f) {
                nr /= len;
                ny /= len;
            }
            pnr[e] += nr;
            pny[e] += ny;
            pnr[e + 1] += nr;
            pny[e + 1] += ny;
        }
        for (int i = 0; i < nProf; i++) {
            float len = (float) Math.sqrt(pnr[i] * pnr[i] + pny[i] * pny[i]);
            if (len > 1e-9f) {
                pnr[i] /= len;
                pny[i] /= len;
            } else {
                pnr[i] = 1f;
                pny[i] = 0f;
            }
        }

        for (int i = 0; i < nProf; i++) {
            float r = profile[i * 2];
            float y = profile[i * 2 + 1];
            for (int j = 0; j <= seg; j++) {
                double phi = 2.0 * Math.PI * j / seg;
                float c = (float) Math.cos(phi);
                float s = (float) Math.sin(phi);
                int idx = (i * cols + j) * 3;
                verts[idx] = cx + r * c;
                verts[idx + 1] = cy + y;
                verts[idx + 2] = cz + r * s;
                normals[idx] = pnr[i] * c;
                normals[idx + 1] = pny[i];
                normals[idx + 2] = pnr[i] * s;
                if (uv != null) {
                    uv[(i * cols + j) * 2] = 1f - (float) j / seg;
                    uv[(i * cols + j) * 2 + 1] = along[i];
                }
            }
        }

        int[] indices = new int[(nProf - 1) * seg * 6];
        int k = 0;
        for (int i = 0; i < nProf - 1; i++) {
            for (int j = 0; j < seg; j++) {
                int a = i * cols + j;
                int b = i * cols + j + 1;
                int c = (i + 1) * cols + j;
                int d = (i + 1) * cols + j + 1;
                // (b,a,c),(c,d,b): same triangles/winding as (a,c,b),(b,c,d), each triple
                // rotated so
                // its diagonal is edge 2. edge0 = around the revolution (b→a / c→d), edge1 = along
                // the profile (a→c / d→b); so wireframe edge-0 draws the rings, edge-1 the profile
                // lines, edge-0+1 the full grid, diagonal only in all-edges. CCW-from-outside.
                indices[k++] = b;
                indices[k++] = a;
                indices[k++] = c;
                indices[k++] = c;
                indices[k++] = d;
                indices[k++] = b;
            }
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Arc-length fraction (0 at the first point, exactly 1 at the last) of each of the {@code n}
     * points of a 2D polyline given as x,y pairs; uniform spacing if it has no length.
     */
    private static float @NonNull [] profileFractions(float @NonNull [] xy, int n) {
        float[] out = new float[n];
        double total = 0.0;
        for (int i = 1; i < n; i++) {
            double dx = xy[i * 2] - xy[(i - 1) * 2];
            double dy = xy[i * 2 + 1] - xy[(i - 1) * 2 + 1];
            total += Math.sqrt(dx * dx + dy * dy);
            out[i] = (float) total;
        }
        for (int i = 1; i < n; i++) {
            out[i] = i == n - 1 ? 1f
                    : (total > 0.0 ? Math.min((float) (out[i] / total), 1f) : (float) i / (n - 1));
        }
        return out;
    }

    /**
     * Round {@code segments} to an int, falling back to {@code dflt} when {@code <= 0}, clamped
     * to {@code min}.
     */
    private static int segCount(float segments, int dflt, int min) {
        int n = segments > 0f ? Math.round(segments) : dflt;
        return Math.max(min, n);
    }

    // ----------------------------------------------------------------------------------------
    // Solids
    // ----------------------------------------------------------------------------------------

    /**
     * Sphere of {@code radius} centered at {@code (cx,cy,cz)} with {@code slices}×{@code stacks
     * }.
     *
     * <p>UV ({@code uvMode != UV_NONE}): longitude/latitude globe, u = 1 - longitude/2π
     * (longitude from +X toward +Z, so the map reads unmirrored from outside) and v = 1 at the +Y
     * pole down to 0 at the -Y pole.
     */
    @NonNull
    public static MeshData sphere(
            float radius, float cx, float cy, float cz, int slices, int stacks, int uvMode) {
        return sphereBand(radius, 0f, (float) Math.PI, stacks, slices, cx, cy, cz, uvMode);
    }

    /**
     * Torus (donut) of ring radius {@code majorR} and tube radius {@code minorR}, centered at
     * {@code (cx,cy,cz)}, lying in the XZ plane (hole along +Y). {@code segU} segments around the
     * ring × {@code segV} around the tube; outward normals, CCW-from-outside.
     *
     * <p>UV ({@code uvMode != UV_NONE}): u = fraction around the ring (from +X toward +Z), v =
     * fraction around the tube (starting at the outer equator, going up). Unlike the other
     * primitives this reads mirrored from outside; kept as is for compatibility.
     */
    @NonNull
    public static MeshData torus(
            float majorR,
            float minorR,
            float cx,
            float cy,
            float cz,
            int segU,
            int segV,
            int uvMode) {
        int cols = segV + 1;
        int rows = segU + 1;
        float[] verts = new float[rows * cols * 3];
        float[] normals = new float[rows * cols * 3];
        float[] uv = uvMode != 0 ? new float[rows * cols * 2] : null;
        int p = 0;
        int q = 0;
        for (int i = 0; i <= segU; i++) {
            double u = 2.0 * Math.PI * i / segU;
            float cu = (float) Math.cos(u);
            float su = (float) Math.sin(u);
            for (int j = 0; j <= segV; j++) {
                double v = 2.0 * Math.PI * j / segV;
                float cv = (float) Math.cos(v);
                float sv = (float) Math.sin(v);
                normals[p] = cv * cu;
                normals[p + 1] = sv;
                normals[p + 2] = cv * su;
                verts[p] = cx + (majorR + minorR * cv) * cu;
                verts[p + 1] = cy + minorR * sv;
                verts[p + 2] = cz + (majorR + minorR * cv) * su;
                p += 3;
                if (uv != null) {
                    uv[q] = (float) i / segU; // around the ring
                    uv[q + 1] = (float) j / segV; // around the tube
                    q += 2;
                }
            }
        }
        int[] indices = new int[segU * segV * 6];
        int k = 0;
        for (int i = 0; i < segU; i++) {
            for (int j = 0; j < segV; j++) {
                int a = i * cols + j;
                int b = (i + 1) * cols + j;
                int c = (i + 1) * cols + (j + 1);
                int d = i * cols + (j + 1);
                // (d,c,b),(b,a,d): same triangles/winding as before, each triple rotated so its
                // diagonal is edge 2. edge0 = around the major ring (d→c / b→a), edge1 = around the
                // tube (c→b / a→d); so wireframe edge-0 draws the ring loops, edge-1 the tube
                // loops,
                // edge-0+1 the full grid, diagonal only in all-edges. CCW-from-outside.
                indices[k++] = d;
                indices[k++] = c;
                indices[k++] = b;
                indices[k++] = b;
                indices[k++] = a;
                indices[k++] = d;
            }
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Flat rectangular plane of {@code width}×{@code height} centered at {@code (cx,cy,cz)}, in the
     * XY plane facing +Z. With {@code uvMode != 0} the corners map to the unit square (0,0)..(1,1)
     * so it can be textured (the texturable replacement for {@code defineQuadMesh}).
     */
    @NonNull
    public static MeshData plane(
            float width, float height, float cx, float cy, float cz, int uvMode) {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        float[] verts = {
                cx - hw, cy - hh, cz,
                cx + hw, cy - hh, cz,
                cx + hw, cy + hh, cz,
                cx - hw, cy + hh, cz,
        };
        float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
        int[] indices = {0, 1, 2, 0, 2, 3}; // CCW from +Z
        float[] uv = uvMode != 0 ? new float[]{0, 0, 1, 0, 1, 1, 0, 1} : null;
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Half the side, in model units, of the square {@link #infinitePlaneFallback} uses for an
     * {@link #INFINITE_PLANE}.
     */
    public static final float INFINITE_PLANE_FALLBACK_HALF_SIZE = 1000f;

    /**
     * The finite stand-in for an {@link #INFINITE_PLANE} through {@code (cx,cy,cz)}: a square of
     * side {@code 2 * INFINITE_PLANE_FALLBACK_HALF_SIZE} centered there, in the XZ plane facing +Y
     * (two triangles, CCW seen from +Y, normals +Y), without a grid. {@link #build} turns it to
     * face +X or +Z about its center when the axis field of the flags asks for it. Renderers
     * without an infinite-plane path draw it (see {@code Paint3DContext.defineInfinitePlane3D}).
     *
     * <p>UV ({@code uvMode != UV_NONE}): the square maps once onto the unit square, u along +X and
     * v toward -Z, like the top face of {@link #cube}.
     */
    @NonNull
    public static MeshData infinitePlaneFallback(float cx, float cy, float cz, int uvMode) {
        float e = INFINITE_PLANE_FALLBACK_HALF_SIZE;
        float[] verts = {
                cx - e, cy, cz + e,
                cx + e, cy, cz + e,
                cx + e, cy, cz - e,
                cx - e, cy, cz - e,
        };
        float[] normals = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};
        int[] indices = {0, 1, 2, 0, 2, 3}; // CCW from +Y
        float[] uv = uvMode != UV_NONE ? new float[]{0, 0, 1, 0, 1, 1, 0, 1} : null;
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Cube (box) of full dimensions {@code (dx,dy,dz)} centered at {@code (cx,cy,cz)}.
     *
     * <p>UV ({@code uvMode != UV_NONE}): each face maps to the unit square, unmirrored from
     * outside, with v up (+Y) on the four side faces; the top face has v toward -Z and the bottom
     * face v toward +Z.
     */
    @NonNull
    public static MeshData cube(
            float dx, float dy, float dz, float cx, float cy, float cz, int uvMode) {
        float hx = dx * 0.5f;
        float hy = dy * 0.5f;
        float hz = dz * 0.5f;
        // 24 verts (4 per face) so each face has its own outward normal.
        float[][] face = {
                {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
        };
        // Corner offsets per face (CCW from outside): listed as 4 (sx,sy,sz) in {-1,+1}.
        int[][][] corners = {
                {{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}}, // +Z
                {{1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}}, // -Z
                {{1, -1, 1}, {1, -1, -1}, {1, 1, -1}, {1, 1, 1}}, // +X
                {{-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}}, // -X
                {{-1, 1, 1}, {1, 1, 1}, {1, 1, -1}, {-1, 1, -1}}, // +Y
                {{-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1}}, // -Y
        };
        float[] verts = new float[24 * 3];
        float[] normals = new float[24 * 3];
        int[] indices = new int[6 * 6];
        // Each face's 4 corners (CCW from outside) map to the unit square (0,0),(1,0),(1,1),(0,1).
        float[] uv = uvMode != 0 ? new float[24 * 2] : null;
        float[][] faceUv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        int vp = 0;
        int ip = 0;
        int qp = 0;
        for (int f = 0; f < 6; f++) {
            int base = f * 4;
            for (int c = 0; c < 4; c++) {
                verts[vp] = cx + corners[f][c][0] * hx;
                verts[vp + 1] = cy + corners[f][c][1] * hy;
                verts[vp + 2] = cz + corners[f][c][2] * hz;
                normals[vp] = face[f][0];
                normals[vp + 1] = face[f][1];
                normals[vp + 2] = face[f][2];
                vp += 3;
                if (uv != null) {
                    uv[qp] = faceUv[c][0];
                    uv[qp + 1] = faceUv[c][1];
                    qp += 2;
                }
            }
            indices[ip++] = base;
            indices[ip++] = base + 1;
            indices[ip++] = base + 2;
            indices[ip++] = base;
            indices[ip++] = base + 2;
            indices[ip++] = base + 3;
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Cone of base {@code radius} and {@code height}, base centered at {@code (cx,cy,cz)}, apex
     * up at {@code (cx, cy+height, cz)}. Smooth slant normals + a flat base.
     *
     * <p>UV ({@code uvMode != UV_NONE}): the slant wraps once around like the {@link #sphere}
     * (u = 1 - angle/2π, unmirrored from outside) with v = 0 at the base rim and 1 at the apex;
     * the base is a disk inscribed in the unit square, reading unmirrored from below.
     */
    @NonNull
    public static MeshData cone(
            float radius, float height, float cx, float cy, float cz, int seg, int uvMode) {
        float slant = (float) Math.sqrt(height * height + radius * radius);
        float nY = radius / slant; // vertical component of the slant normal
        float nS = height / slant; // radial scale of the slant normal
        float[] sv = new float[(seg + 1) * 2 * 3]; // base ring + apex ring (slant)
        float[] sn = new float[(seg + 1) * 2 * 3];
        float[] suv = uvMode != UV_NONE ? new float[(seg + 1) * 2 * 2] : null;
        for (int j = 0; j <= seg; j++) {
            double phi = 2.0 * Math.PI * j / seg;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            int b = j * 3;
            int a = (seg + 1 + j) * 3;
            sv[b] = cx + radius * c;
            sv[b + 1] = cy;
            sv[b + 2] = cz + radius * s;
            sn[b] = nS * c;
            sn[b + 1] = nY;
            sn[b + 2] = nS * s;
            sv[a] = cx; // apex (duplicated per slice for a crisp slant normal)
            sv[a + 1] = cy + height;
            sv[a + 2] = cz;
            sn[a] = nS * c;
            sn[a + 1] = nY;
            sn[a + 2] = nS * s;
            if (suv != null) {
                suv[j * 2] = 1f - (float) j / seg;
                suv[j * 2 + 1] = 0f;
                // The apex copy of slice j serves the triangle j..j+1: u at its middle.
                suv[(seg + 1 + j) * 2] = clamp(1f - (j + 0.5f) / seg, 0f, 1f);
                suv[(seg + 1 + j) * 2 + 1] = 1f;
            }
        }
        int[] si = new int[seg * 3];
        int k = 0;
        for (int j = 0; j < seg; j++) {
            si[k++] = j;
            si[k++] = seg + 1 + j;
            si[k++] = j + 1;
        }
        MeshData side = new MeshData(sv, sn, si, suv);
        // Base disk faces -Y (u×v = (1,0,0)×(0,0,1) = -Y, so flip=false keeps it outward/down).
        MeshData base = disk(cx, cy, cz, 1, 0, 0, 0, 0, 1, radius, seg, false);
        if (suv != null) {
            base.uv = diskUv(seg, false);
        }
        ArrayList<MeshData> parts = new ArrayList<>();
        parts.add(side);
        parts.add(base);
        return MeshData.concat(parts);
    }

    /**
     * Cylinder of {@code radius} between the two points {@code p1} and {@code p2}, with end
     * caps.
     *
     * <p>UV ({@code uvMode != UV_NONE}): the side wraps once around (u = 0..1, unmirrored from
     * outside) with v = 0 at {@code p1} and 1 at {@code p2}; each cap is a disk inscribed in the
     * unit square, reading unmirrored from the side it faces.
     */
    @NonNull
    public static MeshData cylinder(
            float radius, float x1, float y1, float z1, float x2, float y2, float z2, int seg,
            int uvMode) {
        float ax = x2 - x1;
        float ay = y2 - y1;
        float az = z2 - z1;
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1e-6f) {
            len = 1e-6f;
        }
        ax /= len;
        ay /= len;
        az /= len;
        float[] u = new float[3];
        float[] v = new float[3];
        perpBasis(ax, ay, az, u, v);
        // Side wall: bottom ring (p1) then top ring (p2), radial normals.
        float[] sv = new float[(seg + 1) * 2 * 3];
        float[] sn = new float[(seg + 1) * 2 * 3];
        float[] suv = uvMode != UV_NONE ? new float[(seg + 1) * 2 * 2] : null;
        for (int j = 0; j <= seg; j++) {
            double phi = 2.0 * Math.PI * j / seg;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            float dx = c * u[0] + s * v[0];
            float dy = c * u[1] + s * v[1];
            float dz = c * u[2] + s * v[2];
            int bi = j * 3;
            int ti = (seg + 1 + j) * 3;
            sv[bi] = x1 + radius * dx;
            sv[bi + 1] = y1 + radius * dy;
            sv[bi + 2] = z1 + radius * dz;
            sv[ti] = x2 + radius * dx;
            sv[ti + 1] = y2 + radius * dy;
            sv[ti + 2] = z2 + radius * dz;
            sn[bi] = dx;
            sn[bi + 1] = dy;
            sn[bi + 2] = dz;
            sn[ti] = dx;
            sn[ti + 1] = dy;
            sn[ti + 2] = dz;
            if (suv != null) {
                // (u, v, axis) is right-handed, so increasing phi runs left-to-right outside.
                float tu = (float) j / seg;
                suv[j * 2] = tu;
                suv[j * 2 + 1] = 0f;
                suv[(seg + 1 + j) * 2] = tu;
                suv[(seg + 1 + j) * 2 + 1] = 1f;
            }
        }
        int[] si = new int[seg * 6];
        int k = 0;
        for (int j = 0; j < seg; j++) {
            int bj = j;
            int bj1 = j + 1;
            int tj = seg + 1 + j;
            int tj1 = seg + 1 + j + 1;
            // (tj,bj,bj1),(bj1,tj1,tj): same triangles/winding, each triple rotated so its diagonal
            // is edge 2. edge0 = the vertical seam along the axis (tj→bj / bj1→tj1), edge1 = around
            // the ring (bj→bj1 / tj1→tj); so wireframe edge-0 draws the verticals, edge-1 the
            // rings,
            // edge-0+1 the full grid, diagonal only in all-edges.
            si[k++] = tj;
            si[k++] = bj;
            si[k++] = bj1;
            si[k++] = bj1;
            si[k++] = tj1;
            si[k++] = tj;
        }
        MeshData side = new MeshData(sv, sn, si, suv);
        // Caps: bottom at p1 faces -axis, top at p2 faces +axis.
        MeshData capB = disk(x1, y1, z1, u[0], u[1], u[2], v[0], v[1], v[2], radius, seg, true);
        MeshData capT = disk(x2, y2, z2, u[0], u[1], u[2], v[0], v[1], v[2], radius, seg, false);
        if (suv != null) {
            capB.uv = diskUv(seg, true);
            capT.uv = diskUv(seg, false);
        }
        ArrayList<MeshData> parts = new ArrayList<>();
        parts.add(side);
        parts.add(capB);
        parts.add(capT);
        return MeshData.concat(parts);
    }

    /**
     * Rounded box of full dims {@code (dx,dy,dz)} centered at {@code (cx,cy,cz)} with corner
     * {@code r}.
     *
     * <p>UV ({@code uvMode != UV_NONE}): like {@link #cube}, each face — including its half of
     * the surrounding rounded edges and corners — maps to the unit square, oriented as the cube's
     * faces (unmirrored from outside, v up on the side faces).
     */
    @NonNull
    public static MeshData roundedCube(
            float dx, float dy, float dz, float cx, float cy, float cz, float r, int seg,
            int uvMode) {
        float ex = Math.max(dx * 0.5f - r, 0f); // inner half-extents
        float ey = Math.max(dy * 0.5f - r, 0f);
        float ez = Math.max(dz * 0.5f - r, 0f);
        float ox = ex + r;
        float oy = ey + r;
        float oz = ez + r;
        int[][] faces = {
                {0, 1}, {0, -1}, {1, 1}, {1, -1}, {2, 1}, {2, -1},
        };
        // Texture u / v axes per face (same orientation as cube()'s faces), in faces[] order.
        int[][] texU = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {-1, 0, 0}};
        int[][] texV = {{0, 1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, 1, 0}};
        int perFace = (seg + 1) * (seg + 1);
        int vc = 6 * perFace;
        float[] verts = new float[vc * 3];
        float[] normals = new float[vc * 3];
        float[] uv = uvMode != UV_NONE ? new float[vc * 2] : null;
        int[] indices = new int[6 * seg * seg * 6];
        int vp = 0;
        int ip = 0;
        int qp = 0;
        int vbase = 0;
        for (int fi = 0; fi < faces.length; fi++) {
            int[] fc = faces[fi];
            int axis = fc[0];
            int sign = fc[1];
            float[] uDir = new float[3];
            float[] vDir = new float[3];
            float[] nDir = new float[3];
            nDir[axis] = sign;
            if (axis == 0) {
                if (sign > 0) {
                    uDir[1] = 1;
                    vDir[2] = 1;
                } else {
                    uDir[2] = 1;
                    vDir[1] = 1;
                }
            } else if (axis == 1) {
                if (sign > 0) {
                    uDir[2] = 1;
                    vDir[0] = 1;
                } else {
                    uDir[0] = 1;
                    vDir[2] = 1;
                }
            } else {
                if (sign > 0) {
                    uDir[0] = 1;
                    vDir[1] = 1;
                } else {
                    uDir[1] = 1;
                    vDir[0] = 1;
                }
            }
            // The grid point (u, w) in [-1,1]² projects onto the texture axes as these weights.
            float uOnTu = uDir[0] * texU[fi][0] + uDir[1] * texU[fi][1] + uDir[2] * texU[fi][2];
            float wOnTu = vDir[0] * texU[fi][0] + vDir[1] * texU[fi][1] + vDir[2] * texU[fi][2];
            float uOnTv = uDir[0] * texV[fi][0] + uDir[1] * texV[fi][1] + uDir[2] * texV[fi][2];
            float wOnTv = vDir[0] * texV[fi][0] + vDir[1] * texV[fi][1] + vDir[2] * texV[fi][2];
            for (int s = 0; s <= seg; s++) {
                float u = (s / (float) seg) * 2f - 1f;
                for (int t = 0; t <= seg; t++) {
                    float w = (t / (float) seg) * 2f - 1f;
                    float px = nDir[0] * ox + uDir[0] * (u * ox) + vDir[0] * (w * ox);
                    float py = nDir[1] * oy + uDir[1] * (u * oy) + vDir[1] * (w * oy);
                    float pz = nDir[2] * oz + uDir[2] * (u * oz) + vDir[2] * (w * oz);
                    float qx = clamp(px, -ex, ex);
                    float qy = clamp(py, -ey, ey);
                    float qz = clamp(pz, -ez, ez);
                    float ddx = px - qx;
                    float ddy = py - qy;
                    float ddz = pz - qz;
                    float l = (float) Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
                    if (l < 1e-6f) {
                        ddx = nDir[0];
                        ddy = nDir[1];
                        ddz = nDir[2];
                        l = 1f;
                    }
                    float inv = 1f / l;
                    verts[vp] = cx + qx + r * ddx * inv;
                    verts[vp + 1] = cy + qy + r * ddy * inv;
                    verts[vp + 2] = cz + qz + r * ddz * inv;
                    normals[vp] = ddx * inv;
                    normals[vp + 1] = ddy * inv;
                    normals[vp + 2] = ddz * inv;
                    vp += 3;
                    if (uv != null) {
                        uv[qp] = 0.5f + 0.5f * (u * uOnTu + w * wOnTu);
                        uv[qp + 1] = 0.5f + 0.5f * (u * uOnTv + w * wOnTv);
                        qp += 2;
                    }
                }
            }
            int stride = seg + 1;
            for (int s = 0; s < seg; s++) {
                for (int t = 0; t < seg; t++) {
                    int a = vbase + s * stride + t;
                    int b = vbase + (s + 1) * stride + t;
                    int c = vbase + (s + 1) * stride + (t + 1);
                    int d = vbase + s * stride + (t + 1);
                    // (a,b,c),(c,d,a): split on the a–c diagonal so each triangle lists its two
                    // grid
                    // edges first and closes on the diagonal. edge0 = an s-edge (a→b / c→d),
                    // edge1 =
                    // a t-edge (b→c / d→a); so wireframe edge-0 draws one grid axis, edge-1 the
                    // other, edge-0+1 the full per-face grid, diagonal only in all-edges.
                    indices[ip++] = a;
                    indices[ip++] = b;
                    indices[ip++] = c;
                    indices[ip++] = c;
                    indices[ip++] = d;
                    indices[ip++] = a;
                }
            }
            vbase += perFace;
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Spherical sector: a spherical cap (polar angle {@code 0..angle}) plus the cone joining the
     * cap rim back to the center — the surface of an "orange slice"/ice-cream solid. Centered at
     * {@code (cx,cy,cz)}, opening up the +Y axis.
     *
     * <p>UV ({@code uvMode != UV_NONE}): the cap keeps the {@link #sphere} globe mapping (so v
     * stops at the rim latitude {@code 1 - angle/π}); the cone continues u around and runs v
     * from the rim latitude down to 0 at the center.
     */
    @NonNull
    public static MeshData sphericalSector(
            float radius, float angle, float cx, float cy, float cz, int slices, int stacks,
            int uvMode) {
        MeshData cap = sphereBand(radius, 0f, angle, stacks, slices, cx, cy, cz, uvMode);
        // Cone from the center (apex) to the rim ring at theta = angle.
        int seg = slices;
        float ca = (float) Math.cos(angle);
        float sa = (float) Math.sin(angle);
        float ringY = radius * ca;
        float ringR = radius * sa;
        float[] cv = new float[(seg + 1) * 2 * 3]; // rim ring + apex ring
        float[] cn = new float[(seg + 1) * 2 * 3];
        float[] cuv = uvMode != UV_NONE ? new float[(seg + 1) * 2 * 2] : null;
        float rimV = clamp(1f - angle / (float) Math.PI, 0f, 1f);
        for (int j = 0; j <= seg; j++) {
            double phi = 2.0 * Math.PI * j / seg;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            int ri = j * 3;
            int ai = (seg + 1 + j) * 3;
            cv[ri] = cx + ringR * c;
            cv[ri + 1] = cy + ringY;
            cv[ri + 2] = cz + ringR * s;
            cv[ai] = cx; // apex = sector center
            cv[ai + 1] = cy;
            cv[ai + 2] = cz;
            // Outward normal of the cone surface (apex at origin, half-angle = angle).
            float nx = ca * c;
            float ny = -sa;
            float nz = ca * s;
            cn[ri] = nx;
            cn[ri + 1] = ny;
            cn[ri + 2] = nz;
            cn[ai] = nx;
            cn[ai + 1] = ny;
            cn[ai + 2] = nz;
            if (cuv != null) {
                cuv[j * 2] = 1f - (float) j / seg;
                cuv[j * 2 + 1] = rimV;
                // The apex copy of slice j serves the triangle j..j+1: u at its middle.
                cuv[(seg + 1 + j) * 2] = clamp(1f - (j + 0.5f) / seg, 0f, 1f);
                cuv[(seg + 1 + j) * 2 + 1] = 0f;
            }
        }
        int[] ci = new int[seg * 3];
        int k = 0;
        for (int j = 0; j < seg; j++) {
            ci[k++] = j; // rim j
            ci[k++] = j + 1; // rim j+1
            ci[k++] = seg + 1 + j; // apex
        }
        ArrayList<MeshData> parts = new ArrayList<>();
        parts.add(cap);
        parts.add(new MeshData(cv, cn, ci, cuv));
        return MeshData.concat(parts);
    }

    /**
     * Spherical dome: a spherical cap (polar angle {@code 0..angle}) closed by a flat disk at the
     * rim plane. Centered at {@code (cx,cy,cz)}, opening up the +Y axis (a hemisphere at
     * {@code angle = PI/2}).
     *
     * <p>UV ({@code uvMode != UV_NONE}): the cap keeps the {@link #sphere} globe mapping; the
     * base is a disk inscribed in the unit square, reading unmirrored from below.
     */
    @NonNull
    public static MeshData sphericalDome(
            float radius, float angle, float cx, float cy, float cz, int slices, int stacks,
            int uvMode) {
        MeshData cap = sphereBand(radius, 0f, angle, stacks, slices, cx, cy, cz, uvMode);
        float ringY = radius * (float) Math.cos(angle);
        float ringR = radius * (float) Math.sin(angle);
        // Flat base disk at the rim, facing -Y (downward, outward of the dome).
        MeshData base = disk(cx, cy + ringY, cz, 1, 0, 0, 0, 0, 1, ringR, slices, false);
        if (uvMode != UV_NONE) {
            base.uv = diskUv(slices, false);
        }
        ArrayList<MeshData> parts = new ArrayList<>();
        parts.add(cap);
        parts.add(base);
        return MeshData.concat(parts);
    }

    /**
     * Tube of {@code radius} following the points given by {@code params[1..]} as {@code x,y,z}
     * triples ({@code params[0]} is the radius). {@code segments} is the cross-section side count
     * (>= 3); {@code capped} closes the ends with disks.
     *
     * <p>By default the points are fit to a <b>monotonic cubic spline</b> ({@link
     * MonotonicCurveFit}, chord-length parametrized) and the shell is swept as rings (disks) placed
     * <b>normal to the spline tangent</b> ({@code getSlope}) — a smooth, non-overshooting tube.
     * {@link #FLAG_TUBE_LEGACY} selects the older parallel-transport sweep over the raw polyline
     * (optionally Catmull-Rom-densified with {@link #FLAG_SPLINE}).
     *
     * <p>For the default spline path, {@code segments} controls the cross-section side count; the
     * number of rings taken <i>along</i> the spline is <b>adaptive</b> — each span emits about
     * (estimated arc length / cross-section edge length) rings (capped at {@link
     * #MAX_TUBE_PER_SPAN}), so long and sharply curved spans are densified automatically and the
     * quads stay roughly square. {@link #FLAG_TUBE_CURVATURE_DENSITY} places the rings by
     * curvature instead (one per facet angle of turning, independent of the radius), and an
     * explicit {@link #FLAG_TUBE_PATH_DENSITY} count overrides both.
     *
     * <p>UV ({@code uvMode(flags) != UV_NONE}): u along the path, v around (see
     * {@link #buildTubeFromCenterline}).
     */
    @NonNull
    public static MeshData tube(
            float @NonNull [] params, boolean capped, float segments, int flags) {
        float radius = params[0];
        // FLAG_TUBE_PATH_DENSITY inserts an explicit "rings per span" float at params[1] (so the
        // points then start at params[2]); without it the count is adaptive and points start at
        // params[1]. pathPerSpan <= 0 means "adaptive".
        boolean manualDensity = (flags & FLAG_TUBE_PATH_DENSITY) != 0;
        int off = manualDensity ? 2 : 1;
        float pathPerSpan = manualDensity ? params[1] : 0f;
        int cn = (params.length - off) / 3;
        if (cn < 2) {
            throw new IllegalArgumentException("MeshPrimitive tube: need >= 2 points");
        }
        float[] cxp = new float[cn];
        float[] cyp = new float[cn];
        float[] czp = new float[cn];
        for (int i = 0; i < cn; i++) {
            cxp[i] = params[off + i * 3];
            cyp[i] = params[off + i * 3 + 1];
            czp[i] = params[off + i * 3 + 2];
        }
        int sides = segCount(segments, DEFAULT_TUBE_SIDES, 3);
        if ((flags & FLAG_TUBE_LEGACY) != 0) {
            return tubeLegacy(
                    cxp, cyp, czp, cn, radius, sides, capped, segments, flags, pathPerSpan);
        }
        return tubeSpline(cxp, cyp, czp, cn, radius, sides, capped, pathPerSpan, flags);
    }

    /**
     * Default tube: fit the control points to a chord-length-parametrized {@link MonotonicCurveFit}
     * and sweep disks (constant {@code radius}) normal to the spline tangent.
     */
    @NonNull
    private static MeshData tubeSpline(
            float[] cxp,
            float[] cyp,
            float[] czp,
            int cn,
            float radius,
            int sides,
            boolean capped,
            float pathPerSpan,
            int flags) {
        double[] knot = new double[cn];
        MonotonicCurveFit fit = buildPositionFit(cxp, cyp, czp, cn, knot);
        return splineSweep(fit, knot, cn, sides, capped, pathPerSpan, null, radius,
                (flags & FLAG_TUBE_CURVATURE_DENSITY) != 0, uvMode(flags));
    }

    /**
     * Profile tube: positions ({@code params} after the count/density header, as x,y,z triples) and
     * an independent list of radii are each fit to a {@link MonotonicCurveFit}; the radius profile
     * is parametrized over the whole path (first radius at the start, last at the end, spread
     * uniformly by path fraction in between), so the point and radius counts may differ. Swept with
     * a per-ring radius — the tube can taper, bulge or pinch along its length.
     *
     * <p>Params layout: {@code [nPoints, (ringsPerSpan if FLAG_TUBE_PATH_DENSITY), x0,y0,z0, …,
     * r0, r1, …]}. Caps via {@link #FLAG_TUBE_CAP}. {@link #FLAG_TUBE_CURVATURE_DENSITY} and UV
     * behave as for {@link #tube} (the curvature measure also counts the bending of the radius
     * profile).
     */
    @NonNull
    private static MeshData profileTube(float @NonNull [] params, int sides, int flags) {
        int nPoints = Math.round(params[0]);
        boolean manualDensity = (flags & FLAG_TUBE_PATH_DENSITY) != 0;
        boolean capped = (flags & FLAG_TUBE_CAP) != 0;
        int posOff = manualDensity ? 2 : 1;
        float pathPerSpan = manualDensity ? params[1] : 0f;
        if (nPoints < 2) {
            throw new IllegalArgumentException("MeshPrimitive profile tube: need >= 2 points");
        }
        int rOff = posOff + 3 * nPoints;
        int nR = params.length - rOff;
        if (nR < 1) {
            throw new IllegalArgumentException("MeshPrimitive profile tube: need >= 1 radius");
        }
        float[] cxp = new float[nPoints];
        float[] cyp = new float[nPoints];
        float[] czp = new float[nPoints];
        for (int i = 0; i < nPoints; i++) {
            cxp[i] = params[posOff + i * 3];
            cyp[i] = params[posOff + i * 3 + 1];
            czp[i] = params[posOff + i * 3 + 2];
        }
        double[] knot = new double[nPoints];
        MonotonicCurveFit fit = buildPositionFit(cxp, cyp, czp, nPoints, knot);
        double pathLen = knot[nPoints - 1];

        // Radius spline over the same path-parameter range [0, pathLen]: knots spread uniformly so
        // radius[0] is at the start and radius[nR-1] at the end. nR == 1 → constant radius.
        MonotonicCurveFit radiusFit = null;
        if (nR >= 2) {
            double[] rk = new double[nR];
            double[][] rv = new double[nR][1];
            for (int j = 0; j < nR; j++) {
                rk[j] = pathLen * j / (nR - 1);
                rv[j][0] = params[rOff + j];
            }
            radiusFit = new MonotonicCurveFit(rk, rv);
        }
        return splineSweep(fit, knot, nPoints, sides, capped, pathPerSpan, radiusFit,
                params[rOff], (flags & FLAG_TUBE_CURVATURE_DENSITY) != 0, uvMode(flags));
    }

    /**
     * Build a chord-length-parametrized position spline; fills {@code knot} (strictly
     * increasing).
     */
    @NonNull
    private static MonotonicCurveFit buildPositionFit(
            float[] cxp, float[] cyp, float[] czp, int cn, double[] knot) {
        double[][] y = new double[cn][3];
        y[0][0] = cxp[0];
        y[0][1] = cyp[0];
        y[0][2] = czp[0];
        for (int i = 1; i < cn; i++) {
            float dx = cxp[i] - cxp[i - 1];
            float dy = cyp[i] - cyp[i - 1];
            float dz = czp[i] - czp[i - 1];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            knot[i] = knot[i - 1] + Math.max(d, 1e-4);
            y[i][0] = cxp[i];
            y[i][1] = cyp[i];
            y[i][2] = czp[i];
        }
        return new MonotonicCurveFit(knot, y);
    }

    /**
     * Shared spline sweep: sample the centerline {@code fit} along its {@code knot} range with an
     * adaptive (or explicit {@code pathPerSpan}) ring count, taking the per-ring radius from
     * {@code radiusFit} (or {@code constRadius} when null), and build the tube. The adaptive count
     * matches the lengthwise step to the cross-section edge length using the local radius.
     * With {@code byCurvature} (and no explicit count) the rings are placed by turning angle
     * instead, see {@link #curvatureRingParams}.
     */
    @NonNull
    private static MeshData splineSweep(
            MonotonicCurveFit fit,
            double[] knot,
            int cn,
            int sides,
            boolean capped,
            float pathPerSpan,
            MonotonicCurveFit radiusFit,
            float constRadius,
            boolean byCurvature,
            int uvMode) {
        boolean manual = pathPerSpan > 0f;
        int fixedNs = manual ? clampInt(Math.round(pathPerSpan), 1, MAX_TUBE_PER_SPAN) : 0;
        final int arcSubSamples = 6;
        int[] perSpan = new int[cn - 1];
        // Curvature mode: explicit ring parameters per span (otherwise rings are evenly spaced).
        double[][] spanT = byCurvature && !manual ? new double[cn - 1][] : null;
        int total = 1; // + the final endpoint
        double[] a = new double[3];
        double[] b = new double[3];
        for (int i = 0; i < cn - 1; i++) {
            int ns;
            if (manual) {
                ns = fixedNs;
            } else if (spanT != null) {
                spanT[i] = curvatureRingParams(fit, radiusFit, knot[i], knot[i + 1], sides);
                ns = spanT[i].length;
            } else {
                double k0 = knot[i];
                double k1 = knot[i + 1];
                float rMid =
                        radiusFit != null
                                ? (float) radiusFit.getPos((k0 + k1) * 0.5, 0)
                                : constRadius;
                double step = Math.max(2.0 * Math.abs(rMid) * Math.sin(Math.PI / sides), 1e-4);
                double arc = 0.0;
                fit.getPos(k0, a);
                for (int s = 1; s <= arcSubSamples; s++) {
                    fit.getPos(k0 + (k1 - k0) * s / arcSubSamples, b);
                    double dx = b[0] - a[0];
                    double dy = b[1] - a[1];
                    double dz = b[2] - a[2];
                    arc += Math.sqrt(dx * dx + dy * dy + dz * dz);
                    a[0] = b[0];
                    a[1] = b[1];
                    a[2] = b[2];
                }
                ns = clampInt((int) Math.round(arc / step), 1, MAX_TUBE_PER_SPAN);
            }
            perSpan[i] = ns;
            total += ns;
        }

        int n = total;
        float[] px = new float[n];
        float[] py = new float[n];
        float[] pz = new float[n];
        float[] tx = new float[n];
        float[] ty = new float[n];
        float[] tz = new float[n];
        float[] radii = new float[n];
        float[] pos = new float[3];
        double[] slope = new double[3];
        float pTx = 0f;
        float pTy = 1f;
        float pTz = 0f;
        int w = 0;
        for (int i = 0; i < cn - 1; i++) {
            double k0 = knot[i];
            double k1 = knot[i + 1];
            int ns = perSpan[i];
            for (int s = 0; s < ns; s++) {
                // Each span emits its start + interior (not its end — that's the next span's start,
                // or the final endpoint added below), so rings are not duplicated at the joins.
                double t = spanT != null ? spanT[i][s] : k0 + (k1 - k0) * s / ns;
                w = sampleCenterline(fit, t, pos, slope, px, py, pz, tx, ty, tz, w, pTx, pTy, pTz);
                radii[w - 1] = radiusAt(radiusFit, t, constRadius);
                pTx = tx[w - 1];
                pTy = ty[w - 1];
                pTz = tz[w - 1];
            }
        }
        double tEnd = knot[cn - 1];
        w = sampleCenterline(fit, tEnd, pos, slope, px, py, pz, tx, ty, tz, w, pTx, pTy, pTz);
        radii[w - 1] = radiusAt(radiusFit, tEnd, constRadius);
        return buildTubeFromCenterline(px, py, pz, tx, ty, tz, radii, n, sides, capped, uvMode);
    }

    /**
     * Ring parameters for the spline span [{@code k0}, {@code k1}] placed by curvature
     * ({@link #FLAG_TUBE_CURVATURE_DENSITY}). The span's turning is measured over
     * {@link #CURVATURE_SAMPLES} sub-intervals: the angle swept by the centerline tangent plus,
     * for a profile tube ({@code radiusFit != null}), the turning of the radius silhouette (the
     * direction of (arc length, radius)). The span gets ceil(turning / (2π / sides)) rings —
     * one per cross-section facet angle, at least 1, at most {@link #MAX_TUBE_PER_SPAN} — spread
     * at equal turning increments, so straight runs collapse to a single ring per span and bends
     * are densified, independently of the tube radius. Returns the span start followed by its
     * interior ring parameters (the span end is the next span's start).
     */
    private static double @NonNull [] curvatureRingParams(
            @NonNull MonotonicCurveFit fit, @Nullable MonotonicCurveFit radiusFit,
            double k0, double k1, int sides) {
        final int k = CURVATURE_SAMPLES;
        double[] ts = new double[k + 1];
        for (int s = 0; s <= k; s++) {
            ts[s] = k0 + (k1 - k0) * s / k;
        }
        double[] w = new double[k + 1]; // turning attributed to sub-interval s (1..k)
        double[] tan0 = new double[3];
        double[] tan1 = new double[3];
        double[] pos0 = new double[3];
        double[] pos1 = new double[3];
        fit.getSlope(ts[0], tan0);
        double r0 = 0.0;
        double prevSil = Double.NaN;
        if (radiusFit != null) {
            fit.getPos(ts[0], pos0);
            r0 = radiusFit.getPos(ts[0], 0);
        }
        for (int s = 1; s <= k; s++) {
            fit.getSlope(ts[s], tan1);
            w[s] += angleBetween(tan0, tan1);
            double[] swap = tan0;
            tan0 = tan1;
            tan1 = swap;
            if (radiusFit != null) {
                fit.getPos(ts[s], pos1);
                double dx = pos1[0] - pos0[0];
                double dy = pos1[1] - pos0[1];
                double dz = pos1[2] - pos0[2];
                double r1 = radiusFit.getPos(ts[s], 0);
                double sil = Math.atan2(r1 - r0, Math.sqrt(dx * dx + dy * dy + dz * dz));
                if (!Double.isNaN(prevSil)) {
                    // The silhouette bends at sample s-1: split it over the two sub-intervals.
                    double turn = 0.5 * Math.abs(sil - prevSil);
                    w[s - 1] += turn;
                    w[s] += turn;
                }
                prevSil = sil;
                swap = pos0;
                pos0 = pos1;
                pos1 = swap;
                r0 = r1;
            }
        }
        double[] cum = new double[k + 1];
        for (int s = 1; s <= k; s++) {
            cum[s] = cum[s - 1] + w[s];
        }
        double turning = cum[k];
        int ns = clampInt((int) Math.ceil(turning * sides / (2.0 * Math.PI) - 1e-9), 1,
                MAX_TUBE_PER_SPAN);
        double[] out = new double[ns];
        out[0] = k0;
        int seg = 1;
        for (int r = 1; r < ns; r++) {
            double target = turning * r / ns;
            while (seg < k && cum[seg] < target) {
                seg++;
            }
            double c0 = cum[seg - 1];
            double c1 = cum[seg];
            double f = c1 > c0 ? (target - c0) / (c1 - c0) : 0.0;
            out[r] = ts[seg - 1] + (ts[seg] - ts[seg - 1]) * f;
        }
        return out;
    }

    /** Angle in radians between two (not necessarily unit) vectors; 0 if either is ~zero. */
    private static double angleBetween(double @NonNull [] a, double @NonNull [] b) {
        double la = a[0] * a[0] + a[1] * a[1] + a[2] * a[2];
        double lb = b[0] * b[0] + b[1] * b[1] + b[2] * b[2];
        if (!(la > 1e-24) || !(lb > 1e-24)) {
            return 0.0;
        }
        double cx = a[1] * b[2] - a[2] * b[1];
        double cy = a[2] * b[0] - a[0] * b[2];
        double cz = a[0] * b[1] - a[1] * b[0];
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.atan2(Math.sqrt(cx * cx + cy * cy + cz * cz), dot);
    }

    private static float radiusAt(MonotonicCurveFit radiusFit, double t, float constRadius) {
        if (radiusFit == null) {
            return constRadius;
        }
        float r = (float) radiusFit.getPos(t, 0);
        return r < 0f ? 0f : r; // monotonic fit won't overshoot, but guard against negative radius
    }

    /**
     * Evaluate the spline at {@code t}: store the position into {@code px/py/pz[w]} and the unit
     * tangent ({@code getSlope}, normalized; falling back to the previous direction when the slope
     * is degenerate) into {@code tx/ty/tz[w]}. Returns {@code w + 1}.
     */
    private static int sampleCenterline(
            MonotonicCurveFit fit,
            double t,
            float[] pos,
            double[] slope,
            float[] px,
            float[] py,
            float[] pz,
            float[] tx,
            float[] ty,
            float[] tz,
            int w,
            float pTx,
            float pTy,
            float pTz) {
        fit.getPos(t, pos);
        fit.getSlope(t, slope);
        px[w] = pos[0];
        py[w] = pos[1];
        pz[w] = pos[2];
        float sx = (float) slope[0];
        float sy = (float) slope[1];
        float sz = (float) slope[2];
        float l = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (l < 1e-6f) {
            tx[w] = pTx;
            ty[w] = pTy;
            tz[w] = pTz;
        } else {
            tx[w] = sx / l;
            ty[w] = sy / l;
            tz[w] = sz / l;
        }
        return w + 1;
    }

    /**
     * Legacy tube: parallel-transport sweep over the raw control polyline (optionally Catmull-Rom
     * densified by {@link #FLAG_SPLINE}); tangents are the average of adjacent segment directions.
     */
    @NonNull
    private static MeshData tubeLegacy(
            float[] cxp,
            float[] cyp,
            float[] czp,
            int cn,
            float radius,
            int sides,
            boolean capped,
            float segments,
            int flags,
            float pathPerSpan) {
        float[] px;
        float[] py;
        float[] pz;
        if ((flags & FLAG_SPLINE) != 0) {
            // Catmull samples per control span: explicit (FLAG_TUBE_PATH_DENSITY) or default.
            int per =
                    pathPerSpan > 0f
                            ? clampInt(Math.round(pathPerSpan), 1, MAX_TUBE_PER_SPAN)
                            : segCount(segments, 8, 1);
            int n = (cn - 1) * per + 1;
            px = new float[n];
            py = new float[n];
            pz = new float[n];
            int w = 0;
            for (int i = 0; i < cn - 1; i++) {
                int i0 = Math.max(0, i - 1);
                int i2 = i + 1;
                int i3 = Math.min(cn - 1, i + 2);
                for (int s = 0; s < per; s++) {
                    float t = s / (float) per;
                    px[w] = catmull(cxp[i0], cxp[i], cxp[i2], cxp[i3], t);
                    py[w] = catmull(cyp[i0], cyp[i], cyp[i2], cyp[i3], t);
                    pz[w] = catmull(czp[i0], czp[i], czp[i2], czp[i3], t);
                    w++;
                }
            }
            px[w] = cxp[cn - 1];
            py[w] = cyp[cn - 1];
            pz[w] = czp[cn - 1];
        } else {
            px = cxp;
            py = cyp;
            pz = czp;
        }
        int n = px.length;
        float[] tx = new float[n];
        float[] ty = new float[n];
        float[] tz = new float[n];
        for (int i = 0; i < n; i++) {
            float ax = 0;
            float ay = 0;
            float az = 0;
            if (i > 0) {
                ax += px[i] - px[i - 1];
                ay += py[i] - py[i - 1];
                az += pz[i] - pz[i - 1];
            }
            if (i < n - 1) {
                ax += px[i + 1] - px[i];
                ay += py[i + 1] - py[i];
                az += pz[i + 1] - pz[i];
            }
            float l = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (l < 1e-6f) {
                ax = 0;
                ay = 1;
                az = 0;
                l = 1;
            }
            tx[i] = ax / l;
            ty[i] = ay / l;
            tz[i] = az / l;
        }
        float[] radii = new float[n];
        java.util.Arrays.fill(radii, radius);
        return buildTubeFromCenterline(px, py, pz, tx, ty, tz, radii, n, sides, capped,
                uvMode(flags));
    }

    /**
     * Sweep a tube along a centerline ({@code px,py,pz}) with unit tangents ({@code tx,ty,tz}) and
     * a per-ring {@code radii}: a parallel-transport frame (no twist) places a {@code sides}-gon
     * ring (disk) of radius {@code radii[i]} normal to the tangent at each sample, then connects
     * consecutive rings. {@code capped} adds end disks (sized by the end radii).
     *
     * <p>UV ({@code uvMode != UV_NONE}): u is the arc-length fraction along the centerline (0 at
     * the first ring) and v = 1 - j/sides around the ring (unmirrored seen from outside); the caps
     * use {@link #diskUv}.
     */
    @NonNull
    private static MeshData buildTubeFromCenterline(
            float[] px,
            float[] py,
            float[] pz,
            float[] tx,
            float[] ty,
            float[] tz,
            float[] radii,
            int n,
            int sides,
            boolean capped,
            int uvMode) {
        float[] u = new float[3];
        float[] vv = new float[3];
        perpBasis(tx[0], ty[0], tz[0], u, vv);
        float[] verts = new float[n * (sides + 1) * 3];
        float[] normals = new float[n * (sides + 1) * 3];
        float[] uv = null;
        if (uvMode != UV_NONE) {
            uv = new float[n * (sides + 1) * 2];
            float[] along = polylineFractions(px, py, pz, n);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= sides; j++) {
                    int q = (i * (sides + 1) + j) * 2;
                    uv[q] = along[i];
                    uv[q + 1] = 1f - (float) j / sides;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                // Re-orthogonalize the carried u against the new tangent, then rebuild v.
                float dot = u[0] * tx[i] + u[1] * ty[i] + u[2] * tz[i];
                u[0] -= dot * tx[i];
                u[1] -= dot * ty[i];
                u[2] -= dot * tz[i];
                float ul = (float) Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
                if (ul < 1e-6f) {
                    perpBasis(tx[i], ty[i], tz[i], u, vv);
                } else {
                    u[0] /= ul;
                    u[1] /= ul;
                    u[2] /= ul;
                    cross(tx[i], ty[i], tz[i], u[0], u[1], u[2], vv);
                }
            }
            for (int j = 0; j <= sides; j++) {
                double phi = 2.0 * Math.PI * j / sides;
                float c = (float) Math.cos(phi);
                float s = (float) Math.sin(phi);
                float dx = c * u[0] + s * vv[0];
                float dy = c * u[1] + s * vv[1];
                float dz = c * u[2] + s * vv[2];
                int p = (i * (sides + 1) + j) * 3;
                float ri = radii[i];
                verts[p] = px[i] + ri * dx;
                verts[p + 1] = py[i] + ri * dy;
                verts[p + 2] = pz[i] + ri * dz;
                normals[p] = dx;
                normals[p + 1] = dy;
                normals[p + 2] = dz;
            }
        }
        int cols = sides + 1;
        int[] indices = new int[(n - 1) * sides * 6];
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < sides; j++) {
                int a = i * cols + j;
                int b = i * cols + j + 1;
                int cc = (i + 1) * cols + j;
                int d = (i + 1) * cols + j + 1;
                // (cc,a,b),(b,d,cc): the same two triangles and winding as before, each index
                // triple rotated so its DIAGONAL is the third edge. The first edge of every
                // triangle is longitudinal (cc→a / b→d, along the path at constant section index),
                // the second runs around the section (a→b / d→cc, constant ring). So a wireframe
                // edge-0 pass draws the longitudinal lines, edge-1 the section rings, edge-0+1 the
                // full tube grid, and the diagonal shows only in the all-edges pass. Geometry/
                // lighting are unchanged (identical triangles).
                indices[k++] = cc;
                indices[k++] = a;
                indices[k++] = b;
                indices[k++] = b;
                indices[k++] = d;
                indices[k++] = cc;
            }
        }
        MeshData wall = new MeshData(verts, normals, indices, uv);
        if (!capped) {
            return wall;
        }
        // End caps: disk at sample 0 facing -t0, disk at sample n-1 facing +t(n-1).
        float[] u0 = new float[3];
        float[] v0 = new float[3];
        perpBasis(tx[0], ty[0], tz[0], u0, v0);
        float[] u1 = new float[3];
        float[] v1 = new float[3];
        perpBasis(tx[n - 1], ty[n - 1], tz[n - 1], u1, v1);
        MeshData cap0 =
                disk(px[0], py[0], pz[0], u0[0], u0[1], u0[2], v0[0], v0[1], v0[2], radii[0], sides,
                        true);
        MeshData cap1 =
                disk(px[n - 1], py[n - 1], pz[n - 1], u1[0], u1[1], u1[2], v1[0], v1[1], v1[2],
                        radii[n - 1], sides, false);
        if (uv != null) {
            cap0.uv = diskUv(sides, true);
            cap1.uv = diskUv(sides, false);
        }
        ArrayList<MeshData> parts = new ArrayList<>();
        parts.add(wall);
        parts.add(cap0);
        parts.add(cap1);
        return MeshData.concat(parts);
    }

    // ----------------------------------------------------------------------------------------
    // Reusable pieces
    // ----------------------------------------------------------------------------------------

    /**
     * A latitude band of a sphere of {@code radius} for polar angle {@code thetaMin..thetaMax}
     * (0 = +Y pole), {@code stacks} rows × {@code slices} columns, centered at {@code (cx,cy,cz)}.
     * Radial (outward) normals, wound CCW-from-outside.
     */
    @NonNull
    public static MeshData sphereBand(
            float radius,
            float thetaMin,
            float thetaMax,
            int stacks,
            int slices,
            float cx,
            float cy,
            float cz,
            int uvMode) {
        int rows = stacks + 1;
        int cols = slices + 1;
        float[] verts = new float[rows * cols * 3];
        float[] normals = new float[rows * cols * 3];
        float[] uv = uvMode != 0 ? new float[rows * cols * 2] : null;
        boolean fullV = thetaMin == 0f && thetaMax == (float) Math.PI;
        int p = 0;
        int q = 0;
        for (int i = 0; i <= stacks; i++) {
            float theta = thetaMin + (thetaMax - thetaMin) * i / stacks;
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            for (int j = 0; j <= slices; j++) {
                double phi = 2.0 * Math.PI * j / slices;
                float nx = sinT * (float) Math.cos(phi);
                float ny = cosT;
                float nz = sinT * (float) Math.sin(phi);
                normals[p] = nx;
                normals[p + 1] = ny;
                normals[p + 2] = nz;
                verts[p] = cx + nx * radius;
                verts[p + 1] = cy + ny * radius;
                verts[p + 2] = cz + nz * radius;
                p += 3;
                if (uv != null) {
                    // Longitude/latitude globe mapping (matches defineSphereMesh): U reversed so
                    // text reads left-to-right on the camera-facing front; north pole at the top.
                    uv[q] = 1f - (float) j / slices;
                    // A partial band (sector/dome cap) keeps the globe latitude, v = 1 - theta/PI.
                    uv[q + 1] = fullV ? 1f - (float) i / stacks
                            : clamp(1f - theta / (float) Math.PI, 0f, 1f);
                    q += 2;
                }
            }
        }
        int[] indices = new int[stacks * slices * 6];
        int k = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * cols + j;
                int b = (i + 1) * cols + j;
                int c = (i + 1) * cols + (j + 1);
                int d = i * cols + (j + 1);
                // Triangles (d,c,b),(b,a,d): each lists its two grid edges FIRST and closes on the
                // shared b–d diagonal. The first edge of every triangle is a meridian (d→c / b→a,
                // constant longitude) and the second is a parallel (c→b / a→d, constant latitude),
                // so a wireframe edge-0 pass draws all meridians, edge-1 all parallels, edge-0+1
                // the
                // full globe grid, and the diagonal shows only in the all-edges pass. Same outward
                // (CCW-from-outside) winding as before — only the per-triangle edge ORDER changed,
                // so solid/lit rendering is unaffected.
                indices[k++] = d;
                indices[k++] = c;
                indices[k++] = b;
                indices[k++] = b;
                indices[k++] = a;
                indices[k++] = d;
            }
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * A flat triangle-fan disk of {@code radius} centered at {@code c}, in the plane spanned by
     * unit vectors {@code u,v}. Faces {@code +(u×v)}; pass {@code flip} to face {@code -(u×v)}
     * (and reverse the winding so it stays front-facing).
     */
    @NonNull
    public static MeshData disk(
            float cx,
            float cy,
            float cz,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float radius,
            int sides,
            boolean flip) {
        float[] nrm = new float[3];
        cross(ux, uy, uz, vx, vy, vz, nrm);
        if (flip) {
            nrm[0] = -nrm[0];
            nrm[1] = -nrm[1];
            nrm[2] = -nrm[2];
        }
        float[] verts = new float[(sides + 1) * 3];
        float[] normals = new float[(sides + 1) * 3];
        for (int j = 0; j < sides; j++) {
            double phi = 2.0 * Math.PI * j / sides;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            int p = j * 3;
            verts[p] = cx + radius * (c * ux + s * vx);
            verts[p + 1] = cy + radius * (c * uy + s * vy);
            verts[p + 2] = cz + radius * (c * uz + s * vz);
            normals[p] = nrm[0];
            normals[p + 1] = nrm[1];
            normals[p + 2] = nrm[2];
        }
        int center = sides;
        verts[center * 3] = cx;
        verts[center * 3 + 1] = cy;
        verts[center * 3 + 2] = cz;
        normals[center * 3] = nrm[0];
        normals[center * 3 + 1] = nrm[1];
        normals[center * 3 + 2] = nrm[2];
        int[] indices = new int[sides * 3];
        int k = 0;
        for (int j = 0; j < sides; j++) {
            int j1 = (j + 1) % sides;
            if (flip) {
                indices[k++] = center;
                indices[k++] = j1;
                indices[k++] = j;
            } else {
                indices[k++] = center;
                indices[k++] = j;
                indices[k++] = j1;
            }
        }
        return new MeshData(verts, normals, indices);
    }

    /**
     * UV for a {@link #disk} with the same {@code sides}/{@code flip}: the rim maps onto the circle
     * inscribed in the unit square (center at (0.5, 0.5)), with u along the disk's {@code u} axis
     * and v along its {@code v} axis, u mirrored when {@code flip} so the texture reads unmirrored
     * from the side the disk faces.
     */
    private static float @NonNull [] diskUv(int sides, boolean flip) {
        float[] uv = new float[(sides + 1) * 2];
        float su = flip ? -0.5f : 0.5f;
        for (int j = 0; j < sides; j++) {
            double phi = 2.0 * Math.PI * j / sides;
            uv[j * 2] = 0.5f + su * (float) Math.cos(phi);
            uv[j * 2 + 1] = 0.5f + 0.5f * (float) Math.sin(phi);
        }
        uv[sides * 2] = 0.5f;
        uv[sides * 2 + 1] = 0.5f;
        return uv;
    }

    /** A ring of {@code sides} points (no fill) — used as scratch by callers that need it. */
    public static float @NonNull [] ring(
            float cx, float cy, float cz, float @NonNull [] u, float @NonNull [] v, float radius,
            int sides) {
        float[] out = new float[sides * 3];
        for (int j = 0; j < sides; j++) {
            double phi = 2.0 * Math.PI * j / sides;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            out[j * 3] = cx + radius * (c * u[0] + s * v[0]);
            out[j * 3 + 1] = cy + radius * (c * u[1] + s * v[1]);
            out[j * 3 + 2] = cz + radius * (c * u[2] + s * v[2]);
        }
        return out;
    }

    // ----------------------------------------------------------------------------------------
    // Vector helpers
    // ----------------------------------------------------------------------------------------

    /** Fill {@code u,v} with an orthonormal basis perpendicular to the unit vector {@code d}. */
    private static void perpBasis(float dx, float dy, float dz, float[] u, float[] v) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        float az = Math.abs(dz);
        float rx;
        float ry;
        float rz;
        if (ax <= ay && ax <= az) {
            rx = 1;
            ry = 0;
            rz = 0;
        } else if (ay <= az) {
            rx = 0;
            ry = 1;
            rz = 0;
        } else {
            rx = 0;
            ry = 0;
            rz = 1;
        }
        // u = normalize(d × r)
        float ux = dy * rz - dz * ry;
        float uy = dz * rx - dx * rz;
        float uz = dx * ry - dy * rx;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1e-6f) {
            ul = 1e-6f;
        }
        u[0] = ux / ul;
        u[1] = uy / ul;
        u[2] = uz / ul;
        // v = d × u
        cross(dx, dy, dz, u[0], u[1], u[2], v);
    }

    // ----------------------------------------------------------------------------------------
    // Extruded 2D shapes
    // ----------------------------------------------------------------------------------------

    /**
     * Extrude a 2D profile (in the XY plane) along Z by {@code depth}, centered at
     * {@code (cx,cy,cz)}: a front cap (+Z), a back cap (-Z), and a side wall swept along the closed
     * boundary {@code loop}. The whole solid winds CCW-from-outside (outward normals).
     *
     * @param poly2D  cap vertices as {@code x,y} pairs (perimeter points plus any interior points)
     * @param capTris triangle list over {@code poly2D} indices, wound CCW seen from +Z
     * @param loop    ordered {@code poly2D} indices forming the closed boundary, CCW (material
     *                on left)
     * @param uvMode  {@link #UV_NONE} for no UV; otherwise both caps are mapped planar over the
     *                profile's bounding box (the front cap as seen from +Z, the back cap mirrored
     *                so it reads correctly from -Z) and the wall gets u = perimeter fraction
     *                along {@code loop} (increasing to the right seen from outside), v = 0 at the
     *                back face to 1 at the front face
     */
    @NonNull
    private static MeshData extrude(
            float @NonNull [] poly2D,
            int @NonNull [] capTris,
            int @NonNull [] loop,
            float depth,
            float cx,
            float cy,
            float cz,
            int uvMode) {
        int np = poly2D.length / 2; // cap vertex count
        float hd = depth * 0.5f;
        int le = loop.length; // boundary edge count (closed)
        int vcount = np * 2 + le * 4; // front cap + back cap + 4 per side edge
        float[] verts = new float[vcount * 3];
        float[] normals = new float[vcount * 3];
        float[] uv = uvMode != UV_NONE ? new float[vcount * 2] : null;
        float[] box = uv != null ? uvBox(new float[][]{poly2D}) : null;
        for (int i = 0; i < np; i++) {
            float x = poly2D[i * 2];
            float y = poly2D[i * 2 + 1];
            int f = i * 3; // front cap vertex i
            verts[f] = cx + x;
            verts[f + 1] = cy + y;
            verts[f + 2] = cz + hd;
            normals[f + 2] = 1f;
            int b = (np + i) * 3; // back cap vertex i
            verts[b] = cx + x;
            verts[b + 1] = cy + y;
            verts[b + 2] = cz - hd;
            normals[b + 2] = -1f;
            if (uv != null) {
                float u = clamp((x - box[0]) * box[2], 0f, 1f);
                float v = clamp((y - box[1]) * box[3], 0f, 1f);
                uv[i * 2] = u;
                uv[i * 2 + 1] = v;
                uv[(np + i) * 2] = 1f - u;
                uv[(np + i) * 2 + 1] = v;
            }
        }
        int[] indices = new int[capTris.length * 2 + le * 6];
        int io = 0;
        for (int t = 0; t < capTris.length; t += 3) { // front cap (CCW from +Z)
            indices[io++] = capTris[t];
            indices[io++] = capTris[t + 1];
            indices[io++] = capTris[t + 2];
        }
        for (int t = 0; t < capTris.length; t += 3) { // back cap (reversed → faces -Z)
            indices[io++] = np + capTris[t];
            indices[io++] = np + capTris[t + 2];
            indices[io++] = np + capTris[t + 1];
        }
        float perim = 0f;
        if (uv != null) {
            for (int e = 0; e < le; e++) {
                int ia = loop[e];
                int ib = loop[(e + 1) % le];
                float dx = poly2D[ib * 2] - poly2D[ia * 2];
                float dy = poly2D[ib * 2 + 1] - poly2D[ia * 2 + 1];
                perim += (float) Math.sqrt(dx * dx + dy * dy);
            }
        }
        float run = 0f;
        int sideBase = np * 2;
        for (int e = 0; e < le; e++) {
            int ia = loop[e];
            int ib = loop[(e + 1) % le];
            float ax = poly2D[ia * 2];
            float ay = poly2D[ia * 2 + 1];
            float bx = poly2D[ib * 2];
            float by = poly2D[ib * 2 + 1];
            float dx = bx - ax;
            float dy = by - ay;
            float nx = dy; // outward normal = right of travel for a CCW loop
            float ny = -dx;
            float len = (float) Math.sqrt(nx * nx + ny * ny);
            if (len > 1e-9f) {
                nx /= len;
                ny /= len;
            }
            int base = sideBase + e * 4; // 0=front_a, 1=front_b, 2=back_a, 3=back_b
            setSideVert(verts, normals, base, cx + ax, cy + ay, cz + hd, nx, ny);
            setSideVert(verts, normals, base + 1, cx + bx, cy + by, cz + hd, nx, ny);
            setSideVert(verts, normals, base + 2, cx + ax, cy + ay, cz - hd, nx, ny);
            setSideVert(verts, normals, base + 3, cx + bx, cy + by, cz - hd, nx, ny);
            if (uv != null) {
                float u0 = perim > 0f ? Math.min(run / perim, 1f) : (float) e / le;
                run += len;
                float u1 = e == le - 1 ? 1f
                        : (perim > 0f ? Math.min(run / perim, 1f) : (float) (e + 1) / le);
                setWallUv(uv, base, u0, u1, 1f, 0f);
            }
            indices[io++] = base;
            indices[io++] = base + 2;
            indices[io++] = base + 3;
            indices[io++] = base;
            indices[io++] = base + 3;
            indices[io++] = base + 1;
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Geodesic sphere: a base icosahedron midpoint-subdivided {@code subdiv} times, each vertex
     * projected onto the sphere of {@code radius} about {@code (cx,cy,cz)}. Triangles stay nearly
     * uniform (no pole pinching). Normals are the normalized position; edge midpoints are shared
     * via
     * a cache so vertices aren't duplicated. Winds CCW-from-outside.
     *
     * <p>UV ({@code uvMode != UV_NONE}): the {@link #sphere} globe mapping; triangles crossing the
     * u seam are split and pole vertices duplicated per triangle (see {@link #icosphereUv}), so
     * the UV mesh has a few more vertices and triangles than the plain one.
     */
    @NonNull
    public static MeshData icosphere(
            float radius, float cx, float cy, float cz, int subdiv, int uvMode) {
        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
        float[][] base = {
                {-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0},
                {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
                {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1},
        };
        int[][] faces = {
                {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
                {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1},
        };
        ArrayList<float[]> dirs = new ArrayList<>(); // unit sphere directions
        for (float[] v : base) {
            float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            dirs.add(new float[]{v[0] / len, v[1] / len, v[2] / len});
        }
        ArrayList<int[]> tris = new ArrayList<>();
        for (int[] f : faces) {
            tris.add(new int[]{f[0], f[1], f[2]});
        }
        HashMap<Long, Integer> mid = new HashMap<>();
        for (int s = 0; s < subdiv; s++) {
            ArrayList<int[]> next = new ArrayList<>();
            for (int[] tr : tris) {
                int a = icoMid(tr[0], tr[1], dirs, mid);
                int b = icoMid(tr[1], tr[2], dirs, mid);
                int c = icoMid(tr[2], tr[0], dirs, mid);
                next.add(new int[]{tr[0], a, c});
                next.add(new int[]{tr[1], b, a});
                next.add(new int[]{tr[2], c, b});
                next.add(new int[]{a, b, c});
            }
            tris = next;
        }
        if (uvMode != UV_NONE) {
            return icosphereUv(dirs, tris, radius, cx, cy, cz);
        }
        int nv = dirs.size();
        float[] verts = new float[nv * 3];
        float[] normals = new float[nv * 3];
        for (int i = 0; i < nv; i++) {
            float[] d = dirs.get(i);
            verts[i * 3] = cx + radius * d[0];
            verts[i * 3 + 1] = cy + radius * d[1];
            verts[i * 3 + 2] = cz + radius * d[2];
            normals[i * 3] = d[0];
            normals[i * 3 + 1] = d[1];
            normals[i * 3 + 2] = d[2];
        }
        int[] indices = new int[tris.size() * 3];
        int k = 0;
        for (int[] tr : tris) {
            indices[k++] = tr[0];
            indices[k++] = tr[1];
            indices[k++] = tr[2];
        }
        return new MeshData(verts, normals, indices);
    }

    /** Shared, sphere-projected midpoint of edge (i,j), cached by the unordered edge key. */
    private static int icoMid(
            int i, int j, @NonNull ArrayList<float[]> dirs, @NonNull HashMap<Long, Integer> cache) {
        long key = i < j ? (((long) i) << 32) | j : (((long) j) << 32) | i;
        Integer e = cache.get(key);
        if (e != null) {
            return e;
        }
        float[] a = dirs.get(i);
        float[] b = dirs.get(j);
        float mx = (a[0] + b[0]) * 0.5f;
        float my = (a[1] + b[1]) * 0.5f;
        float mz = (a[2] + b[2]) * 0.5f;
        float len = (float) Math.sqrt(mx * mx + my * my + mz * mz);
        int idx = dirs.size();
        dirs.add(new float[]{mx / len, my / len, mz / len});
        cache.put(key, idx);
        return idx;
    }

    /**
     * UV variant of {@link #icosphere}: the same surface with the {@link #sphere} globe mapping
     * (u = 1 - longitude/2π, longitude measured from +X toward +Z; v = 1 - polar angle/π). The
     * mapping wraps from u = 1 back to 0 across the seam half-plane {@code z = 0, x > 0}, so
     * triangles straddling it are cut along it: each cut point and each vertex lying on the seam
     * gets a u = 1 copy for the z > 0 side and a u = 0 copy for the z < 0 side (cut points are
     * shared by the two triangles of the cut edge, so the surface stays watertight and flat-faced).
     * A vertex at a pole, where longitude is undefined, is duplicated per triangle with u = the
     * mean of the triangle's other two vertices.
     */
    @NonNull
    private static MeshData icosphereUv(
            @NonNull ArrayList<float[]> dirs, @NonNull ArrayList<int[]> tris,
            float radius, float cx, float cy, float cz) {
        int nv = dirs.size();
        // Vertex records {x,y,z, nx,ny,nz, u,v} (unit-sphere space); originals first, same order.
        ArrayList<float[]> vs = new ArrayList<>(nv + nv / 4 + 16);
        int[] side = new int[nv]; // +1: z > 0, -1: z < 0, 0: on the z = 0 plane
        for (int i = 0; i < nv; i++) {
            float[] d = dirs.get(i);
            side[i] = d[2] > ICO_SEAM_EPS ? 1 : (d[2] < -ICO_SEAM_EPS ? -1 : 0);
            float u = side[i] == 0 && d[0] > 0f ? 1f : globeU(d[0], d[2]);
            vs.add(new float[]{d[0], d[1], d[2], d[0], d[1], d[2], u, globeV(d[1])});
        }
        int[] lowCopy = new int[nv]; // u = 0 copy of each seam vertex, made on demand
        java.util.Arrays.fill(lowCopy, -1);
        HashMap<Long, int[]> cuts = new HashMap<>(); // cut edge -> {u = 1 copy, u = 0 copy}
        ArrayList<int[]> out = new ArrayList<>(tris.size() + 64);
        for (int[] tr : tris) {
            int pos = 0;
            int neg = 0;
            for (int q = 0; q < 3; q++) {
                if (side[tr[q]] > 0) {
                    pos++;
                } else if (side[tr[q]] < 0) {
                    neg++;
                }
            }
            if (pos > 0 && neg > 0 && crossesSeam(tr, side, dirs)) {
                splitAtSeam(tr, side, dirs, vs, lowCopy, cuts, out);
                continue;
            }
            // One side only (or straddling the opposite meridian, x < 0, where u is continuous).
            int[] t = tr.clone();
            if (neg > 0 && pos == 0) {
                for (int q = 0; q < 3; q++) {
                    t[q] = seamCopy(t[q], -1, side, vs, lowCopy);
                }
            }
            out.add(t);
        }
        // Poles: a private copy per triangle, u = the mean of the other two vertices.
        for (int[] t : out) {
            for (int q = 0; q < 3; q++) {
                float[] a = vs.get(t[q]);
                if (a[3] * a[3] + a[5] * a[5] < 1e-12f) {
                    float[] copy = a.clone();
                    copy[6] = 0.5f * (vs.get(t[(q + 1) % 3])[6] + vs.get(t[(q + 2) % 3])[6]);
                    t[q] = vs.size();
                    vs.add(copy);
                }
            }
        }
        int n = vs.size();
        float[] verts = new float[n * 3];
        float[] normals = new float[n * 3];
        float[] uv = new float[n * 2];
        for (int i = 0; i < n; i++) {
            float[] a = vs.get(i);
            verts[i * 3] = cx + radius * a[0];
            verts[i * 3 + 1] = cy + radius * a[1];
            verts[i * 3 + 2] = cz + radius * a[2];
            normals[i * 3] = a[3];
            normals[i * 3 + 1] = a[4];
            normals[i * 3 + 2] = a[5];
            uv[i * 2] = a[6];
            uv[i * 2 + 1] = a[7];
        }
        int[] indices = new int[out.size() * 3];
        int k = 0;
        for (int[] t : out) {
            indices[k++] = t[0];
            indices[k++] = t[1];
            indices[k++] = t[2];
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /** Globe u of a direction: 1 - (longitude from +X toward +Z, in [0, 2π)) / 2π. */
    private static float globeU(float x, float z) {
        double phi = Math.atan2(z, x);
        if (phi < 0.0) {
            phi += 2.0 * Math.PI;
        }
        return (float) (1.0 - phi / (2.0 * Math.PI));
    }

    /** Globe v of a unit direction's {@code y}: 1 at the +Y pole, 0 at the -Y pole. */
    private static float globeV(float y) {
        return (float) (1.0 - Math.acos(clamp(y, -1f, 1f)) / Math.PI);
    }

    /** True if the mixed-side triangle's z = 0 crossing lies on the seam ({@code x > 0}). */
    private static boolean crossesSeam(
            int @NonNull [] tr, int @NonNull [] side, @NonNull ArrayList<float[]> dirs) {
        for (int q = 0; q < 3; q++) {
            int i = tr[q];
            int j = tr[(q + 1) % 3];
            if (side[i] * side[j] < 0) {
                float[] a = dirs.get(i);
                float[] b = dirs.get(j);
                float t = a[2] / (a[2] - b[2]);
                if (a[0] + t * (b[0] - a[0]) > 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cut a triangle that crosses the seam into pieces lying on one side each (winding kept),
     * using the side-appropriate u copies of the cut points and of any vertex on the seam.
     */
    private static void splitAtSeam(
            int @NonNull [] tr, int @NonNull [] side, @NonNull ArrayList<float[]> dirs,
            @NonNull ArrayList<float[]> vs, int @NonNull [] lowCopy,
            @NonNull HashMap<Long, int[]> cuts, @NonNull ArrayList<int[]> out) {
        // Rotate so `a` is the vertex on the plane, or else the one alone on its side.
        int k = 0;
        for (int q = 0; q < 3; q++) {
            int s = side[tr[q]];
            if (s == 0) {
                k = q;
                break;
            }
            if (s != side[tr[(q + 1) % 3]] && s != side[tr[(q + 2) % 3]]) {
                k = q;
            }
        }
        int a = tr[k];
        int b = tr[(k + 1) % 3];
        int c = tr[(k + 2) % 3];
        if (side[a] == 0) {
            // b and c on opposite sides: cut b–c at p; (a,b,p) on b's side, (a,p,c) on c's.
            int[] p = seamCut(b, c, dirs, vs, cuts);
            int sb = side[b];
            out.add(new int[]{seamCopy(a, sb, side, vs, lowCopy), b, sb > 0 ? p[0] : p[1]});
            out.add(new int[]{seamCopy(a, -sb, side, vs, lowCopy), sb > 0 ? p[1] : p[0], c});
        } else {
            // a alone on its side: cut a–b at p and c–a at r; (a,p,r) on a's side, the quad
            // (p,b,c,r) on the other.
            int[] p = seamCut(a, b, dirs, vs, cuts);
            int[] r = seamCut(c, a, dirs, vs, cuts);
            int mine = side[a] > 0 ? 0 : 1;
            out.add(new int[]{a, p[mine], r[mine]});
            out.add(new int[]{p[1 - mine], b, c});
            out.add(new int[]{p[1 - mine], c, r[1 - mine]});
        }
    }

    /**
     * The seam crossing of edge (i,j) as {u = 1 copy, u = 0 copy}; computed once per edge from
     * the canonical (lower index first) end so both triangles of the edge share it.
     */
    private static int @NonNull [] seamCut(
            int i, int j, @NonNull ArrayList<float[]> dirs, @NonNull ArrayList<float[]> vs,
            @NonNull HashMap<Long, int[]> cuts) {
        int lo = Math.min(i, j);
        int hi = Math.max(i, j);
        long key = (((long) lo) << 32) | hi;
        int[] e = cuts.get(key);
        if (e != null) {
            return e;
        }
        float[] a = dirs.get(lo);
        float[] b = dirs.get(hi);
        float t = a[2] / (a[2] - b[2]);
        float x = a[0] + t * (b[0] - a[0]);
        float y = a[1] + t * (b[1] - a[1]);
        float len = (float) Math.sqrt(x * x + y * y);
        float nx = x / len;
        float ny = y / len;
        float v = globeV(ny);
        e = new int[]{vs.size(), vs.size() + 1};
        vs.add(new float[]{x, y, 0f, nx, ny, 0f, 1f, v});
        vs.add(new float[]{x, y, 0f, nx, ny, 0f, 0f, v});
        cuts.put(key, e);
        return e;
    }

    /**
     * Vertex {@code i} as used from the {@code s} side of the seam: a vertex on the seam
     * ({@code z = 0, x > 0}) maps to its u = 0 copy for {@code s < 0}; any other vertex is itself.
     */
    private static int seamCopy(
            int i, int s, int @NonNull [] side, @NonNull ArrayList<float[]> vs,
            int @NonNull [] lowCopy) {
        if (s >= 0 || side[i] != 0 || !(vs.get(i)[0] > 0f)) {
            return i;
        }
        if (lowCopy[i] < 0) {
            float[] copy = vs.get(i).clone();
            copy[6] = 0f;
            lowCopy[i] = vs.size();
            vs.add(copy);
        }
        return lowCopy[i];
    }

    /**
     * Sweep a 2D {@code xsec} cross-section (in the moving frame's XY) along a 3D {@code path},
     * with
     * optional per-station {@code scales} and {@code twists}. Parallel-transport frames (carry the
     * frame's {@code u} forward, re-orthogonalize against each new tangent) avoid the section
     * spinning. {@code scalars = [closed, capStart, capEnd, cx,cy,cz]}: {@code closed} wraps the
     * cross-section (a tube) vs. leaving it open (a ribbon); the caps close the two ends.
     * Per-vertex
     * normals come from the cross-section's 2D outward normal rotated into the frame.
     *
     * <p>UV ({@code uvMode != UV_NONE}): on the wall u is the arc-length fraction along the path
     * (0 at the first station) and v = 1 - the perimeter fraction along the cross-section, which
     * reads unmirrored from outside for a CCW section; the caps are mapped planar over the
     * section's bounding box (see {@link #sweepCap}).
     */
    @NonNull
    public static MeshData sweep(
            float @NonNull [] scalars,
            float @NonNull [] xsec,
            float @NonNull [] path,
            float @Nullable [] scales,
            float @Nullable [] twists,
            int uvMode) {
        int m = xsec.length / 2;
        int n = path.length / 3;
        if (m < 2 || n < 2) {
            throw new IllegalArgumentException(
                    "MeshPrimitive sweep: need >= 2 cross-section points and >= 2 path points");
        }
        boolean closed = scalars.length > 0 && scalars[0] != 0f;
        boolean capStart = scalars.length > 1 && scalars[1] != 0f;
        boolean capEnd = scalars.length > 2 && scalars[2] != 0f;
        float cx = scalars.length > 3 ? scalars[3] : 0f;
        float cy = scalars.length > 4 ? scalars[4] : 0f;
        float cz = scalars.length > 5 ? scalars[5] : 0f;

        // Path positions and unit tangents (central difference).
        float[] px = new float[n];
        float[] py = new float[n];
        float[] pz = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = cx + path[i * 3];
            py[i] = cy + path[i * 3 + 1];
            pz[i] = cz + path[i * 3 + 2];
        }
        float[] tx = new float[n];
        float[] ty = new float[n];
        float[] tz = new float[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1);
            int b = Math.min(n - 1, i + 1);
            float dx = px[b] - px[a];
            float dy = py[b] - py[a];
            float dz = pz[b] - pz[a];
            float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (l < 1e-9f) {
                l = 1e-9f;
            }
            tx[i] = dx / l;
            ty[i] = dy / l;
            tz[i] = dz / l;
        }

        // Cross-section per-vertex 2D outward normals (averaged from adjacent edges; CCW → (dy,
        // -dx)).
        float[] nlx = new float[m];
        float[] nly = new float[m];
        int xEdges = closed ? m : m - 1;
        for (int e = 0; e < xEdges; e++) {
            int k0 = e;
            int k1 = (e + 1) % m;
            float dx = xsec[k1 * 2] - xsec[k0 * 2];
            float dy = xsec[k1 * 2 + 1] - xsec[k0 * 2 + 1];
            float ex = dy;
            float ey = -dx;
            float l = (float) Math.sqrt(ex * ex + ey * ey);
            if (l > 1e-9f) {
                ex /= l;
                ey /= l;
            }
            nlx[k0] += ex;
            nly[k0] += ey;
            nlx[k1] += ex;
            nly[k1] += ey;
        }
        for (int k = 0; k < m; k++) {
            float l = (float) Math.sqrt(nlx[k] * nlx[k] + nly[k] * nly[k]);
            if (l > 1e-9f) {
                nlx[k] /= l;
                nly[k] /= l;
            } else {
                nlx[k] = 1f;
                nly[k] = 0f;
            }
        }

        int cols = closed ? m + 1 : m; // duplicate the seam column when closed
        int wallV = n * cols;
        int capV = (capStart ? m + 1 : 0) + (capEnd ? m + 1 : 0);
        float[] verts = new float[(wallV + capV) * 3];
        float[] normals = new float[(wallV + capV) * 3];
        int wallStrips = closed ? m : m - 1;
        int wallTris = (n - 1) * wallStrips * 2;
        int capTris = (capStart ? m : 0) + (capEnd ? m : 0);
        int[] indices = new int[(wallTris + capTris) * 3];
        float[] uv = null;
        if (uvMode != UV_NONE) {
            uv = new float[(wallV + capV) * 2];
            float[] along = polylineFractions(px, py, pz, n);
            // Perimeter fraction per section column (the duplicated seam column ends at 1).
            float[] around = new float[cols];
            float perim = 0f;
            for (int j = 1; j < cols; j++) {
                int k0 = (j - 1) % m;
                int k1 = j % m;
                float dx = xsec[k1 * 2] - xsec[k0 * 2];
                float dy = xsec[k1 * 2 + 1] - xsec[k0 * 2 + 1];
                perim += (float) Math.sqrt(dx * dx + dy * dy);
                around[j] = perim;
            }
            for (int j = 1; j < cols; j++) {
                around[j] = j == cols - 1 ? 1f
                        : (perim > 0f ? Math.min(around[j] / perim, 1f) : (float) j / (cols - 1));
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < cols; j++) {
                    int q = (i * cols + j) * 2;
                    uv[q] = along[i];
                    uv[q + 1] = 1f - around[j];
                }
            }
        }

        float[] u = new float[3];
        float[] v = new float[3];
        perpBasis(tx[0], ty[0], tz[0], u, v);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                // Carry u forward: re-orthogonalize against the new tangent, then v = t × u.
                float dot = u[0] * tx[i] + u[1] * ty[i] + u[2] * tz[i];
                u[0] -= dot * tx[i];
                u[1] -= dot * ty[i];
                u[2] -= dot * tz[i];
                float ul = (float) Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
                if (ul < 1e-6f) {
                    perpBasis(tx[i], ty[i], tz[i], u, v);
                } else {
                    u[0] /= ul;
                    u[1] /= ul;
                    u[2] /= ul;
                    cross(tx[i], ty[i], tz[i], u[0], u[1], u[2], v);
                }
            }
            float s = (scales != null && scales.length > i) ? scales[i] : 1f;
            float tw = (twists != null && twists.length > i) ? twists[i] : 0f;
            float ct = (float) Math.cos(tw);
            float st = (float) Math.sin(tw);
            for (int j = 0; j < cols; j++) {
                int k = j % m;
                float lx = xsec[k * 2];
                float ly = xsec[k * 2 + 1];
                float rx = ct * lx - st * ly; // twist the section in its own plane
                float ry = st * lx + ct * ly;
                int p = (i * cols + j) * 3;
                verts[p] = px[i] + s * (rx * u[0] + ry * v[0]);
                verts[p + 1] = py[i] + s * (rx * u[1] + ry * v[1]);
                verts[p + 2] = pz[i] + s * (rx * u[2] + ry * v[2]);
                float nrx = ct * nlx[k] - st * nly[k];
                float nry = st * nlx[k] + ct * nly[k];
                float nx = nrx * u[0] + nry * v[0];
                float ny = nrx * u[1] + nry * v[1];
                float nz = nrx * u[2] + nry * v[2];
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nl > 1e-9f) {
                    nx /= nl;
                    ny /= nl;
                    nz /= nl;
                }
                normals[p] = nx;
                normals[p + 1] = ny;
                normals[p + 2] = nz;
            }
        }

        int io = 0;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < wallStrips; j++) {
                int a = i * cols + j;
                int b = i * cols + j + 1;
                int cc = (i + 1) * cols + j;
                int d = (i + 1) * cols + j + 1;
                // (cc,a,b),(b,d,cc): same triangles/winding, each triple rotated so its diagonal is
                // edge 2. edge0 = longitudinal along the path (cc→a / b→d), edge1 = around the
                // section (a→b / d→cc); wireframe edge-0 draws the path lines, edge-1 the section
                // rings, edge-0+1 the full grid, diagonal only in all-edges. (Helix uses this.)
                indices[io++] = cc;
                indices[io++] = a;
                indices[io++] = b;
                indices[io++] = b;
                indices[io++] = d;
                indices[io++] = cc;
            }
        }
        int capBase = wallV;
        if (capStart) {
            io = sweepCap(verts, normals, indices, io, capBase, 0, cols, m,
                    -tx[0], -ty[0], -tz[0], uv, xsec, true);
            capBase += m + 1;
        }
        if (capEnd) {
            io = sweepCap(verts, normals, indices, io, capBase, n - 1, cols, m,
                    tx[n - 1], ty[n - 1], tz[n - 1], uv, xsec, false);
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * Arc-length fraction (0 at the first point, exactly 1 at the last) of each of the {@code n}
     * points of a 3D polyline; uniform spacing if it has no length.
     */
    private static float @NonNull [] polylineFractions(
            float @NonNull [] px, float @NonNull [] py, float @NonNull [] pz, int n) {
        float[] out = new float[n];
        double total = 0.0;
        for (int i = 1; i < n; i++) {
            double dx = px[i] - px[i - 1];
            double dy = py[i] - py[i - 1];
            double dz = pz[i] - pz[i - 1];
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
            out[i] = (float) total;
        }
        for (int i = 1; i < n; i++) {
            out[i] = i == n - 1 ? 1f
                    : (total > 0.0 ? Math.min((float) (out[i] / total), 1f) : (float) i / (n - 1));
        }
        return out;
    }

    /**
     * Build a flat end cap for {@link #sweep}: a centroid fan over the cross-section ring at
     * {@code stationRow}, all normals set to the face direction {@code (fnx,fny,fnz)}. The fan
     * winding is chosen to match that face direction (works for convex / star-convex sections).
     * With {@code uv}, the cap is mapped planar over the bounding box of the section's local XY
     * ({@code xsec}), with u mirrored for the start cap (which is seen from behind the frame).
     */
    private static int sweepCap(
            float[] verts, float[] normals, int[] indices, int io,
            int vbase, int stationRow, int cols, int m, float fnx, float fny, float fnz,
            float @Nullable [] uv, float @NonNull [] xsec, boolean mirrorU) {
        float ccx = 0f;
        float ccy = 0f;
        float ccz = 0f;
        for (int k = 0; k < m; k++) {
            int p = (stationRow * cols + k) * 3;
            ccx += verts[p];
            ccy += verts[p + 1];
            ccz += verts[p + 2];
        }
        ccx /= m;
        ccy /= m;
        ccz /= m;
        int centroid = vbase;
        verts[centroid * 3] = ccx;
        verts[centroid * 3 + 1] = ccy;
        verts[centroid * 3 + 2] = ccz;
        normals[centroid * 3] = fnx;
        normals[centroid * 3 + 1] = fny;
        normals[centroid * 3 + 2] = fnz;
        for (int k = 0; k < m; k++) {
            int src = (stationRow * cols + k) * 3;
            int dst = (vbase + 1 + k) * 3;
            verts[dst] = verts[src];
            verts[dst + 1] = verts[src + 1];
            verts[dst + 2] = verts[src + 2];
            normals[dst] = fnx;
            normals[dst + 1] = fny;
            normals[dst + 2] = fnz;
        }
        if (uv != null) {
            float[] box = uvBox(new float[][]{xsec});
            float su = 0f;
            float sv = 0f;
            for (int k = 0; k < m; k++) {
                float u = clamp((xsec[k * 2] - box[0]) * box[2], 0f, 1f);
                float v = clamp((xsec[k * 2 + 1] - box[1]) * box[3], 0f, 1f);
                if (mirrorU) {
                    u = 1f - u;
                }
                uv[(vbase + 1 + k) * 2] = u;
                uv[(vbase + 1 + k) * 2 + 1] = v;
                su += u;
                sv += v;
            }
            uv[vbase * 2] = su / m; // the centroid is the vertex average, so is its UV
            uv[vbase * 2 + 1] = sv / m;
        }
        // Winding: orient the fan so its geometric normal agrees with the face direction.
        int r0 = vbase + 1;
        int r1 = vbase + 1 + (1 % m);
        float ux = verts[r0 * 3] - ccx;
        float uy = verts[r0 * 3 + 1] - ccy;
        float uz = verts[r0 * 3 + 2] - ccz;
        float wx = verts[r1 * 3] - ccx;
        float wy = verts[r1 * 3 + 1] - ccy;
        float wz = verts[r1 * 3 + 2] - ccz;
        float gx = uy * wz - uz * wy;
        float gy = uz * wx - ux * wz;
        float gz = ux * wy - uy * wx;
        boolean flip = (gx * fnx + gy * fny + gz * fnz) < 0f;
        for (int k = 0; k < m; k++) {
            int aa = vbase + 1 + k;
            int bb = vbase + 1 + ((k + 1) % m);
            indices[io++] = centroid;
            if (flip) {
                indices[io++] = bb;
                indices[io++] = aa;
            } else {
                indices[io++] = aa;
                indices[io++] = bb;
            }
        }
        return io;
    }

    /**
     * Coil/spring: generate a helical centerline (radius {@code coilR} about the Y axis, rising
     * {@code pitch} per turn for {@code turns} turns, centered at {@code (cx,cy,cz)}) and a
     * circular
     * cross-section of radius {@code tubeR} with {@code sides} facets, then {@link #sweep} the
     * section (capped) along it. UV as in {@link #sweep}.
     */
    @NonNull
    public static MeshData helix(
            float coilR, float tubeR, float pitch, float turns,
            float cx, float cy, float cz, int sides, int uvMode) {
        int perTurn = Math.max(8, sides * 2);
        int stations = Math.max(2, Math.round(perTurn * Math.abs(turns)) + 1);
        float[] path = new float[stations * 3];
        float totalRise = pitch * turns;
        for (int i = 0; i < stations; i++) {
            float f = i / (float) (stations - 1);
            double ang = 2.0 * Math.PI * turns * f;
            path[i * 3] = coilR * (float) Math.cos(ang);
            path[i * 3 + 1] = totalRise * (f - 0.5f); // center the coil vertically
            path[i * 3 + 2] = coilR * (float) Math.sin(ang);
        }
        float[] xsec = new float[sides * 2];
        for (int k = 0; k < sides; k++) {
            double a = 2.0 * Math.PI * k / sides;
            xsec[k * 2] = tubeR * (float) Math.cos(a);
            xsec[k * 2 + 1] = tubeR * (float) Math.sin(a);
        }
        float[] scalars = {1f, 1f, 1f, cx, cy, cz}; // closed section, both ends capped
        return sweep(scalars, xsec, path, null, null, uvMode);
    }

    private static void setSideVert(
            float[] verts, float[] normals, int i, float x, float y, float z, float nx, float ny) {
        int p = i * 3;
        verts[p] = x;
        verts[p + 1] = y;
        verts[p + 2] = z;
        normals[p] = nx;
        normals[p + 1] = ny;
    }

    /**
     * Extrude an arbitrary 2D path (the general case behind {@link #EXTRUDE_PATH}). The path
     * arrives
     * pre-flattened in {@code p} as:
     * {@code [depth, cx, cy, cz, bevel, contourCount, (count, x0,y0,x1,y1,…)*]} — one or more
     * closed
     * contours of {@code (x,y)} points (no repeated closing point). {@code bevel > 0} chamfers the
     * cap↔wall edge (insets the cap by {@code bevel}, adds a 45° ring); {@code bevel <= 0} is the
     * plain sharp-edged extrusion (the chamfer code is skipped entirely — no extra cost).
     *
     * <p>Unlike the analytic extrude shapes, an arbitrary outline can be concave and can contain
     * holes (the counters of {@code e}, {@code o}, {@code d}, …) or overlapping contours
     * (synthetic-bold glyphs). The two cap faces are triangulated by a <b>scanline</b> sweep using
     * the <b>nonzero-winding</b> rule (what fonts and {@code Path} use by default): between
     * adjacent vertex-{@code y} values, the sorted edge crossings are swept left→right while
     * accumulating each edge's winding direction, and the interior spans (running sum nonzero)
     * are joined into y-monotone pieces, each cut into triangles ({@link #scanlineCaps}). This
     * needs no ear-clipping and handles holes and overlaps for free. The side wall is a quad per
     * contour edge, with each contour first normalized to "material on the left" (solid CCW, hole
     * CW) so the wall normals point outward. Winds CCW-from-outside like the rest of the
     * pipeline.
     *
     * <p>Input hygiene: non-finite points, consecutive near-duplicate points and contours left
     * with fewer than 3 points are dropped first ({@link #sanitizeContours}). The bevel is limited
     * to 90% of half the depth (no bevel when the depth is zero or negative) and is clamped per
     * vertex so no inset edge folds over ({@link #miterInset}).
     *
     * <p>UV ({@code uvMode != UV_NONE}): both caps are mapped planar over the contours' bounding
     * box, the front cap as seen from +Z and the back cap mirrored so it reads correctly from -Z;
     * the walls as in {@link #extrudePathSides}.
     */
    @NonNull
    public static MeshData extrudePath(float @NonNull [] p, int uvMode) {
        float depth = p[0];
        float cx = p[1];
        float cy = p[2];
        float cz = p[3];
        float bevel = p[4];
        int nc = Math.round(p[5]);
        // Parse the contours.
        float[][] contours = new float[Math.max(0, nc)][];
        int idx = 6;
        for (int c = 0; c < nc; c++) {
            int cnt = Math.round(p[idx++]);
            float[] ring = new float[cnt * 2];
            for (int k = 0; k < cnt * 2 && idx < p.length; k++) {
                ring[k] = p[idx++];
            }
            contours[c] = ring;
        }
        contours = sanitizeContours(contours);
        // Even-odd hole classification, shared by the walls (orientation) and the bevel inset.
        boolean[] holes = new boolean[contours.length];
        for (int c = 0; c < contours.length; c++) {
            holes[c] = isHole(contours, contours[c]);
        }
        float hd = depth * 0.5f;
        boolean withUv = uvMode != UV_NONE;
        float[] uvBox = withUv ? uvBox(contours) : null;
        // Beveled: inset the cap boundary by `b` (mitered), so the flat cap is smaller and a 45°
        // chamfer ring bridges the inset cap edge to the full-size wall.
        float b = Math.min(bevel, hd * 0.9f);

        if (!(b > 0f)) {
            // Sharp (default): flat caps over the full contours + straight side walls. No inset,
            // no chamfer rings — the bevel path below is skipped entirely.
            MeshData caps = scanlineCaps(contours, cx, cy, cz + hd, cz - hd, uvBox);
            MeshData sides =
                    extrudePathSides(contours, holes, null, cx, cy, cz, hd, 0f, withUv);
            return MeshData.concat(java.util.Arrays.asList(caps, sides));
        }

        float[][] inset = new float[contours.length][];
        for (int c = 0; c < contours.length; c++) {
            inset[c] = miterInset(contours[c], b, 4f, holes[c]);
            if (DEBUG && hasSelfIntersection(inset[c])) {
                Utils.log("Primitive3D.extrudePath: bevel " + b + " folds contour " + c
                        + " (a neck narrower than twice the bevel)");
            }
        }
        MeshData caps = scanlineCaps(inset, cx, cy, cz + hd, cz - hd, uvBox);
        MeshData sides = extrudePathSides(contours, holes, inset, cx, cy, cz, hd, b, withUv);
        return MeshData.concat(java.util.Arrays.asList(caps, sides));
    }

    /** {@code [minX, minY, 1/width, 1/height]} of all contour points (0 for an empty extent). */
    private static float @NonNull [] uvBox(float @NonNull [][] contours) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float[] ring : contours) {
            for (int i = 0; i + 1 < ring.length; i += 2) {
                minX = Math.min(minX, ring[i]);
                maxX = Math.max(maxX, ring[i]);
                minY = Math.min(minY, ring[i + 1]);
                maxY = Math.max(maxY, ring[i + 1]);
            }
        }
        if (!(maxX >= minX)) {
            return new float[4];
        }
        float w = maxX - minX;
        float h = maxY - minY;
        return new float[]{minX, minY, w > 0f ? 1f / w : 0f, h > 0f ? 1f / h : 0f};
    }

    /**
     * Triangulate the two flat caps for {@link #extrudePath}: a scanline sweep over all contour
     * edges using the nonzero-winding rule (front cap at {@code frontZ} facing +Z, back cap at
     * {@code backZ} facing -Z). This is the cap geometry shared by the sharp and beveled paths;
     * for the beveled path it is fed the inset contours so the flat top shrinks by the bevel.
     *
     * <p>Slab boundaries are the distinct vertex {@code y}s (values closer than a tolerance
     * relative to the coordinate magnitude count as one) plus every {@code y} where two edges
     * cross, so no piece twists. Within a slab the filled spans are merged into runs between
     * the outermost edges (internal edges of overlapping contours add nothing). A run carries on
     * the piece of the slab below when it is bounded by the same two edges, or by edges that
     * continue them through a contour vertex; that vertex then joins the piece's left or right
     * boundary chain. Every piece is therefore a y-monotone polygon, a plain trapezoid when no
     * vertex joined it, and is cut into triangles between its own vertices, so neither vertices
     * elsewhere in the path nor vertices on the opposite side of the piece cut it into slivers.
     * An end collapsed to a point (a peak or valley vertex) becomes a single vertex, triangles of
     * (near) zero area are left out, and thin triangles the cut leaves along shallow curves are
     * repaired by edge flips inside their piece ({@link #delaunayFlips}).
     *
     * @param uvBox {@code [minX, minY, 1/width, 1/height]} for planar cap UV, or null for none
     */
    @NonNull
    private static MeshData scanlineCaps(
            float[][] contours, float cx, float cy, float frontZ, float backZ,
            float @Nullable [] uvBox) {
        int edgeCount = 0;
        // cz removed from the parameters
        float maxAbs = 0f;
        int[] ringBase = new int[contours.length];
        for (int r = 0; r < contours.length; r++) {
            float[] ring = contours[r];
            ringBase[r] = edgeCount;
            edgeCount += ring.length / 2;
            for (float v : ring) {
                maxAbs = Math.max(maxAbs, Math.abs(v));
            }
        }
        // Same-y and point-width tolerance; float precision is relative to the magnitude.
        float eps = Math.max(1e-7f, maxAbs * 1e-6f);
        float[] eYlo = new float[edgeCount];
        float[] eYhi = new float[edgeCount];
        float[] eXlo = new float[edgeCount];
        float[] eSlope = new float[edgeCount];
        float[] eXmin = new float[edgeCount];
        float[] eXmax = new float[edgeCount];
        int[] eDir = new int[edgeCount];
        int[] eRing = new int[edgeCount];
        int[] eVert = new int[edgeCount];
        // Stored edge of the contour edge leaving vertex i of ring r (at ringBase[r] + i), or -1
        // for a horizontal edge.
        int[] edgeOf = new int[edgeCount];
        int ne = 0;
        float[] ys = new float[edgeCount + 8];
        int nys = 0;
        for (int r = 0; r < contours.length; r++) {
            float[] ring = contours[r];
            int n = ring.length / 2;
            for (int i = 0; i < n; i++) {
                float ay = ring[i * 2 + 1];
                int j = (i + 1) % n;
                float by = ring[j * 2 + 1];
                ys[nys++] = ay;
                edgeOf[ringBase[r] + i] = -1;
                if (ay == by) {
                    continue;
                }
                float ax = ring[i * 2];
                float bx = ring[j * 2];
                if (ay < by) {
                    eYlo[ne] = ay;
                    eYhi[ne] = by;
                    eXlo[ne] = ax;
                } else {
                    eYlo[ne] = by;
                    eYhi[ne] = ay;
                    eXlo[ne] = bx;
                }
                eSlope[ne] = (bx - ax) / (by - ay);
                eDir[ne] = (by > ay) ? 1 : -1;
                eXmin[ne] = Math.min(ax, bx);
                eXmax[ne] = Math.max(ax, bx);
                eRing[ne] = r;
                eVert[ne] = i;
                edgeOf[ringBase[r] + i] = ne;
                ne++;
            }
        }
        // eBelow[e] is the edge whose top vertex is edge e's bottom vertex, so the two rise
        // through that contour vertex as one boundary; -1 at a local minimum or when the
        // neighbouring edge is horizontal.
        int[] eBelow = new int[ne];
        for (int e = 0; e < ne; e++) {
            float[] ring = contours[eRing[e]];
            int n = ring.length / 2;
            int i = eVert[e];
            int j = (i + 1) % n;
            int nb = -1;
            if (ring[i * 2 + 1] < ring[j * 2 + 1]) {
                int h = (i + n - 1) % n; // bottom is i: the neighbour arrives from h
                if (ring[h * 2 + 1] < ring[i * 2 + 1]) {
                    nb = edgeOf[ringBase[eRing[e]] + h];
                }
            } else {
                int k = (j + 1) % n; // bottom is j: the neighbour leaves towards k
                if (ring[k * 2 + 1] < ring[j * 2 + 1]) {
                    nb = edgeOf[ringBase[eRing[e]] + j];
                }
            }
            eBelow[e] = nb;
        }
        // Edges in order of their bottom y, for the crossing search and the sweep's active list.
        int[] byLo = sortedByLo(eYlo, ne);
        // Where two edges that don't share a vertex cross (overlapping or self-intersecting
        // contours), add a slab boundary so the left-to-right edge order is fixed in every slab.
        // Only edges whose y ranges overlap can cross: scan each edge's successors in byLo order
        // until one starts above its top.
        for (int ia = 0; ia < ne; ia++) {
            int ea = byLo[ia];
            for (int ib = ia + 1; ib < ne && eYlo[byLo[ib]] < eYhi[ea]; ib++) {
                int a = Math.min(ea, byLo[ib]);
                int b = Math.max(ea, byLo[ib]);
                if (eXmax[a] < eXmin[b] || eXmax[b] < eXmin[a]) {
                    continue;
                }
                float lo = Math.max(eYlo[a], eYlo[b]);
                float hi = Math.min(eYhi[a], eYhi[b]);
                if (!(hi - lo > eps) || sharesVertex(a, b, eRing, eVert, contours)) {
                    continue;
                }
                float d0 = (eXlo[a] + (lo - eYlo[a]) * eSlope[a])
                        - (eXlo[b] + (lo - eYlo[b]) * eSlope[b]);
                float d1 = (eXlo[a] + (hi - eYlo[a]) * eSlope[a])
                        - (eXlo[b] + (hi - eYlo[b]) * eSlope[b]);
                if ((d0 > eps && d1 < -eps) || (d0 < -eps && d1 > eps)) {
                    if (nys == ys.length) {
                        ys = java.util.Arrays.copyOf(ys, ys.length * 2);
                    }
                    ys[nys++] = lo + (hi - lo) * (d0 / (d0 - d1));
                }
            }
        }
        java.util.Arrays.sort(ys, 0, nys);
        int uy = 0;
        for (int i = 0; i < nys; i++) {
            if (uy == 0 || ys[i] > ys[uy - 1] + eps) {
                ys[uy++] = ys[i];
            }
        }
        // Sweep the slabs bottom-up. Open pieces carry over from the slab below; those that no
        // run of the slab carries on are closed at its bottom.
        ArrayList<float[]> polys = new ArrayList<>();
        ArrayList<int[]> polyTris = new ArrayList<>();
        float[] crossX = new float[ne];
        int[] crossE = new int[ne];
        int nx = 0; // active edges (eYlo < ymid < eYhi), kept in the last slab's crossing order
        int nextIn = 0; // next edge of byLo to become active
        ArrayList<CapPiece> open = new ArrayList<>();
        ArrayList<CapPiece> next = new ArrayList<>();
        int[] openByLeft = new int[ne];
        java.util.Arrays.fill(openByLeft, -1);
        float top = 0f;
        for (int s = 0; s + 1 < uy; s++) {
            float yA = ys[s];
            float yB = ys[s + 1];
            float ymid = 0.5f * (yA + yB);
            int kept = 0;
            for (int i = 0; i < nx; i++) {
                if (ymid < eYhi[crossE[i]]) {
                    crossE[kept++] = crossE[i];
                }
            }
            nx = kept;
            while (nextIn < ne && eYlo[byLo[nextIn]] < ymid) {
                int e = byLo[nextIn++];
                if (ymid < eYhi[e]) {
                    crossE[nx++] = e;
                }
            }
            for (int i = 0; i < nx; i++) {
                int e = crossE[i];
                crossX[i] = eXlo[e] + (ymid - eYlo[e]) * eSlope[e];
            }
            // Sort by x, ties by edge index.
            for (int a = 1; a < nx; a++) {
                float kx = crossX[a];
                int ke = crossE[a];
                int b = a - 1;
                while (b >= 0 && (crossX[b] > kx || (crossX[b] == kx && crossE[b] > ke))) {
                    crossX[b + 1] = crossX[b];
                    crossE[b + 1] = crossE[b];
                    b--;
                }
                crossX[b + 1] = kx;
                crossE[b + 1] = ke;
            }
            int winding = 0;
            int runL = -1;
            for (int i = 0; i < nx; i++) {
                int before = winding;
                winding += eDir[crossE[i]];
                if (before == 0) {
                    runL = crossE[i];
                } else if (winding == 0) {
                    int runR = crossE[i];
                    CapPiece pc = carriedPiece(open, openByLeft, runL, runR, eBelow);
                    if (pc == null && eBelow[runL] >= 0) {
                        pc = carriedPiece(open, openByLeft, eBelow[runL], runR, eBelow);
                    }
                    if (pc == null) {
                        pc = new CapPiece(runL, runR, yA);
                        pc.addLeft(eXlo[runL] + (yA - eYlo[runL]) * eSlope[runL], yA);
                        pc.addRight(eXlo[runR] + (yA - eYlo[runR]) * eSlope[runR], yA);
                    } else {
                        // Carried on, possibly through a vertex on either side.
                        pc.mClaimed = true;
                        if (pc.mLeft != runL) {
                            pc.addLeft(eXlo[runL], eYlo[runL]);
                            pc.mLeft = runL;
                        }
                        if (pc.mRight != runR) {
                            pc.addRight(eXlo[runR], eYlo[runR]);
                            pc.mRight = runR;
                        }
                    }
                    next.add(pc);
                }
            }
            for (CapPiece c : open) {
                openByLeft[c.mKey] = -1;
                if (!c.mClaimed) {
                    closeCapPiece(polys, polyTris, c, yA, eXlo, eYlo, eSlope, eps);
                }
            }
            ArrayList<CapPiece> t = open;
            open = next;
            next = t;
            next.clear();
            for (int o = 0; o < open.size(); o++) {
                CapPiece c = open.get(o);
                c.mClaimed = false;
                c.mKey = c.mLeft;
                openByLeft[c.mLeft] = o;
            }
            top = yB;
        }
        for (CapPiece c : open) {
            closeCapPiece(polys, polyTris, c, top, eXlo, eYlo, eSlope, eps);
        }

        int nVerts = 0;
        int nTris = 0;
        for (int q = 0; q < polys.size(); q++) {
            int k = polys.get(q).length / 2;
            int[] tris = polyTris.get(q);
            nVerts += k;
            nTris += tris == null ? k - 2 : tris.length / 3;
        }
        float[] verts = new float[nVerts * 2 * 3];
        float[] normals = new float[nVerts * 2 * 3];
        float[] uv = uvBox != null ? new float[nVerts * 2 * 2] : null;
        int[] indices = new int[nTris * 2 * 3];
        int io = 0;
        int fb = 0; // first front vertex of the current piece; its back copy is nVerts later
        for (int q = 0; q < polys.size(); q++) {
            float[] g = polys.get(q);
            int[] tris = polyTris.get(q);
            int k = g.length / 2;
            int bb = nVerts + fb;
            for (int j = 0; j < k; j++) {
                float x = g[j * 2];
                float y = g[j * 2 + 1];
                int f = (fb + j) * 3;
                verts[f] = cx + x;
                verts[f + 1] = cy + y;
                verts[f + 2] = frontZ;
                normals[f + 2] = 1f;
                int bk = (bb + j) * 3;
                verts[bk] = cx + x;
                verts[bk + 1] = cy + y;
                verts[bk + 2] = backZ;
                normals[bk + 2] = -1f;
                if (uv != null) {
                    float u = clamp((x - uvBox[0]) * uvBox[2], 0f, 1f);
                    float v = clamp((y - uvBox[1]) * uvBox[3], 0f, 1f);
                    uv[(fb + j) * 2] = u;
                    uv[(fb + j) * 2 + 1] = v;
                    uv[(bb + j) * 2] = 1f - u;
                    uv[(bb + j) * 2 + 1] = v;
                }
            }
            if (tris != null) {
                // A monotone piece: CCW (seen from +Z) triangles; the back copy is reversed.
                for (int i = 0; i < tris.length; i += 3) {
                    indices[io++] = fb + tris[i];
                    indices[io++] = fb + tris[i + 1];
                    indices[io++] = fb + tris[i + 2];
                    indices[io++] = bb + tris[i];
                    indices[io++] = bb + tris[i + 2];
                    indices[io++] = bb + tris[i + 1];
                }
            } else if (k == 4) {
                indices[io++] = fb;
                indices[io++] = fb + 2;
                indices[io++] = fb + 1;
                indices[io++] = fb;
                indices[io++] = fb + 3;
                indices[io++] = fb + 2;
                indices[io++] = bb;
                indices[io++] = bb + 1;
                indices[io++] = bb + 2;
                indices[io++] = bb;
                indices[io++] = bb + 2;
                indices[io++] = bb + 3;
            } else {
                indices[io++] = fb;
                indices[io++] = fb + 1;
                indices[io++] = fb + 2;
                indices[io++] = bb;
                indices[io++] = bb + 2;
                indices[io++] = bb + 1;
            }
            fb += k;
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /** Indices {@code 0..ne-1} ordered by {@code eYlo} (ties by index). */
    private static int @NonNull [] sortedByLo(float[] eYlo, int ne) {
        long[] keys = new long[ne];
        for (int e = 0; e < ne; e++) {
            int bits = Float.floatToIntBits(eYlo[e]);
            bits ^= (bits >> 31) & 0x7fffffff; // orders like the float value
            keys[e] = ((long) bits << 32) | e;
        }
        java.util.Arrays.sort(keys);
        int[] order = new int[ne];
        for (int i = 0; i < ne; i++) {
            order[i] = (int) keys[i];
        }
        return order;
    }

    /** True if stored edges {@code a} and {@code b} are consecutive edges of the same contour. */
    private static boolean sharesVertex(
            int a, int b, int[] eRing, int[] eVert, float[][] contours) {
        if (eRing[a] != eRing[b]) {
            return false;
        }
        int n = contours[eRing[a]].length / 2;
        return (eVert[a] + 1) % n == eVert[b] || (eVert[b] + 1) % n == eVert[a];
    }

    /**
     * The open piece of {@link #scanlineCaps} not yet carried on in this slab whose left edge is
     * {@code left} and whose right edge is {@code right} or the edge {@code right} continues
     * through a vertex; null if there is none.
     */
    private static @Nullable CapPiece carriedPiece(
            ArrayList<CapPiece> open, int[] openByLeft, int left, int right, int[] eBelow) {
        int o = openByLeft[left];
        if (o < 0) {
            return null;
        }
        CapPiece c = open.get(o);
        if (c.mClaimed || (c.mRight != right && c.mRight != eBelow[right])) {
            return null;
        }
        return c;
    }

    /**
     * Close the cap piece {@code c} at {@code y}: a piece never carried through a vertex is the
     * trapezoid of {@link #addCapPiece}; any other is cut up by {@link #addMonotonePiece}.
     */
    private static void closeCapPiece(
            ArrayList<float[]> polys, ArrayList<int[]> polyTris, CapPiece c, float y,
            float[] eXlo, float[] eYlo, float[] eSlope, float tol) {
        if (c.mNl == 1 && c.mNr == 1) {
            addCapPiece(polys, polyTris, c.mLeft, c.mRight, c.mY0, y, eXlo, eYlo, eSlope, tol);
            return;
        }
        c.addLeft(eXlo[c.mLeft] + (y - eYlo[c.mLeft]) * eSlope[c.mLeft], y);
        c.addRight(eXlo[c.mRight] + (y - eYlo[c.mRight]) * eSlope[c.mRight], y);
        addMonotonePiece(polys, polyTris, c.mL, c.mNl, c.mR, c.mNr, tol);
    }

    /**
     * Append the cap piece between edge {@code le} (left) and {@code re} (right) over
     * {@code y0..y1}: the trapezoid {@code {xLtop,y1, xRtop,y1, xRbot,y0, xLbot,y0}}, or, when one
     * end is narrower than {@code tol} (collapsed to a point), the single CCW triangle that
     * remains; nothing when both ends collapsed. Its triangles follow from the vertex count, so
     * a null entry goes to {@code polyTris}.
     */
    private static void addCapPiece(
            ArrayList<float[]> polys, ArrayList<int[]> polyTris, int le, int re, float y0,
            float y1, float[] eXlo, float[] eYlo, float[] eSlope, float tol) {
        float xLtop = eXlo[le] + (y1 - eYlo[le]) * eSlope[le];
        float xLbot = eXlo[le] + (y0 - eYlo[le]) * eSlope[le];
        float xRtop = eXlo[re] + (y1 - eYlo[re]) * eSlope[re];
        float xRbot = eXlo[re] + (y0 - eYlo[re]) * eSlope[re];
        boolean topPoint = xRtop - xLtop <= tol;
        boolean botPoint = xRbot - xLbot <= tol;
        if (topPoint && botPoint) {
            return;
        }
        if (topPoint) {
            polys.add(new float[]{0.5f * (xLtop + xRtop), y1, xLbot, y0, xRbot, y0});
        } else if (botPoint) {
            polys.add(new float[]{xLtop, y1, 0.5f * (xLbot + xRbot), y0, xRtop, y1});
        } else {
            polys.add(new float[]{xLtop, y1, xRtop, y1, xRbot, y0, xLbot, y0});
        }
        polyTris.add(null);
    }

    /**
     * Triangulate the y-monotone cap piece between the left chain {@code l} ({@code nl} points)
     * and the right chain {@code r} ({@code nr} points), flat {@code x,y} lists that run bottom
     * to top with rising {@code y}; the first points of the two chains share one {@code y}, as do
     * the last points. An end narrower than {@code tol} becomes a single vertex. Uses the
     * classic stack sweep over the vertices in {@code y} order (ties left first); appends the
     * vertices to {@code polys} and the triangles, CCW seen from +Z, to {@code polyTris}, leaving
     * out those of (near) zero area.
     */
    private static void addMonotonePiece(
            ArrayList<float[]> polys, ArrayList<int[]> polyTris,
            float[] l, int nl, float[] r, int nr, float tol) {
        boolean botPoint = r[0] - l[0] <= tol;
        boolean topPoint = r[nr * 2 - 2] - l[nl * 2 - 2] <= tol;
        int n = nl + nr - (botPoint ? 1 : 0) - (topPoint ? 1 : 0);
        float[] xy = new float[n * 2];
        int[] side = new int[n]; // -1 left chain, 1 right chain, 0 a collapsed end (both)
        int k = 0;
        int li = 0;
        int ri = 0;
        if (botPoint) {
            xy[0] = 0.5f * (l[0] + r[0]);
            xy[1] = l[1];
            k = 1;
            li = 1;
            ri = 1;
        }
        // Otherwise the bottom-left point is the first vertex and the bottom-right one starts the
        // right chain. Without a collapsed top the top-right point is the last vertex and the
        // top-left one ends the left chain.
        int lEnd = topPoint ? nl - 1 : nl;
        int rEnd = nr - 1;
        while (li < lEnd || ri < rEnd) {
            if (ri >= rEnd || (li < lEnd && l[li * 2 + 1] <= r[ri * 2 + 1])) {
                xy[k * 2] = l[li * 2];
                xy[k * 2 + 1] = l[li * 2 + 1];
                side[k++] = -1;
                li++;
            } else {
                xy[k * 2] = r[ri * 2];
                xy[k * 2 + 1] = r[ri * 2 + 1];
                side[k++] = 1;
                ri++;
            }
        }
        if (topPoint) {
            xy[k * 2] = 0.5f * (l[nl * 2 - 2] + r[nr * 2 - 2]);
            xy[k * 2 + 1] = l[nl * 2 - 1];
        } else {
            xy[k * 2] = r[nr * 2 - 2];
            xy[k * 2 + 1] = r[nr * 2 - 1];
            side[k] = 1;
        }
        if (n < 3) {
            return;
        }
        int[] tri = new int[(n - 2) * 3];
        int nt = 0;
        int[] stack = new int[n];
        stack[0] = 0;
        stack[1] = 1;
        int sp = 2;
        for (int j = 2; j < n - 1; j++) {
            int last = stack[sp - 1];
            if (side[j] != side[last]) {
                // Opposite chain: fan from j to the whole stack.
                for (int i = 0; i + 1 < sp; i++) {
                    nt = side[j] > 0
                            ? addCapTri(tri, nt, xy, j, stack[i + 1], stack[i])
                            : addCapTri(tri, nt, xy, j, stack[i], stack[i + 1]);
                }
                stack[0] = last;
                stack[1] = j;
                sp = 2;
            } else {
                // Same chain: cut off the convex corners j can see, then push j.
                sp--;
                while (sp > 0) {
                    int w = stack[sp - 1];
                    int added = side[j] < 0
                            ? addCapTri(tri, nt, xy, w, j, last)
                            : addCapTri(tri, nt, xy, w, last, j);
                    if (added == nt) {
                        break; // reflex or straight at `last`
                    }
                    nt = added;
                    last = w;
                    sp--;
                }
                stack[sp++] = last;
                stack[sp++] = j;
            }
        }
        int u = n - 1;
        boolean leftStack = side[stack[sp - 1]] < 0;
        for (int i = 0; i + 1 < sp; i++) {
            nt = leftStack
                    ? addCapTri(tri, nt, xy, u, stack[i + 1], stack[i])
                    : addCapTri(tri, nt, xy, u, stack[i], stack[i + 1]);
        }
        if (nt > 0) {
            int[] tris = nt == tri.length ? tri : java.util.Arrays.copyOf(tri, nt);
            delaunayFlips(xy, tris);
            polys.add(xy);
            polyTris.add(tris);
        }
    }

    /**
     * Append the triangle {@code (a, b, c)} of the points in {@code xy} to {@code tri} at
     * {@code nt} if it passes {@link #clearCcw}; returns the new triangle-index count.
     */
    private static int addCapTri(int[] tri, int nt, float[] xy, int a, int b, int c) {
        if (!clearCcw(xy, a, b, c)) {
            return nt;
        }
        tri[nt] = a;
        tri[nt + 1] = b;
        tri[nt + 2] = c;
        return nt + 3;
    }

    /**
     * True if the triangle {@code (a, b, c)} of the points in {@code xy} winds CCW with an area
     * clearly above zero: twice its area exceeds 1e-7 of its longest edge squared.
     */
    private static boolean clearCcw(float[] xy, int a, int b, int c) {
        double abx = xy[b * 2] - xy[a * 2];
        double aby = xy[b * 2 + 1] - xy[a * 2 + 1];
        double acx = xy[c * 2] - xy[a * 2];
        double acy = xy[c * 2 + 1] - xy[a * 2 + 1];
        double bcx = acx - abx;
        double bcy = acy - aby;
        double area2 = abx * acy - aby * acx;
        double lmax = Math.max(abx * abx + aby * aby,
                Math.max(acx * acx + acy * acy, bcx * bcx + bcy * bcy));
        return area2 > 1e-7 * lmax;
    }

    /**
     * Repair thin triangles among the CCW triangles {@code tri} over the points {@code xy} by
     * Lawson edge flips: starting from the edges of triangles more elongated than
     * {@link #CAP_FLIP_ASPECT}, an inner edge is swapped for the other diagonal of its two
     * triangles' quad when the far corner of one lies inside the other's circumcircle and both
     * new triangles stay {@link #clearCcw}; the edges around each flip are then re-tested.
     * Vertices and boundary edges are kept; the long thin fans a sweep leaves along shallow
     * curves become well-shaped triangles. Pieces without thin triangles are left as they are,
     * and the number of flips is bounded.
     */
    private static void delaunayFlips(float[] xy, int @NonNull [] tri) {
        int nt = tri.length;
        boolean[] thin = null;
        for (int t = 0; t < nt; t += 3) {
            if (capAspect(xy, tri[t], tri[t + 1], tri[t + 2]) > CAP_FLIP_ASPECT) {
                if (thin == null) {
                    thin = new boolean[nt / 3];
                }
                thin[t / 3] = true;
            }
        }
        if (thin == null || nt < 6) {
            return;
        }
        // nbr[t * 3 + k]: the triangle across edge k (tri[t * 3 + k] -> next corner), or -1,
        // found through an open-addressing table of the directed edges.
        int mask = (Integer.highestOneBit(nt) << 2) - 1;
        long[] keys = new long[mask + 1];
        int[] owner = new int[mask + 1];
        java.util.Arrays.fill(owner, -1);
        for (int i = 0; i < nt; i++) {
            long key = ((long) tri[i] << 32) | tri[i - i % 3 + (i + 1) % 3];
            int h = edgeSlot(key, mask);
            while (owner[h] >= 0) {
                h = (h + 1) & mask;
            }
            keys[h] = key;
            owner[h] = i / 3;
        }
        int[] nbr = new int[nt];
        int[] work = new int[nt + 16];
        int nw = 0;
        for (int i = 0; i < nt; i++) {
            long twin = ((long) tri[i - i % 3 + (i + 1) % 3] << 32) | tri[i];
            int o = -1;
            for (int h = edgeSlot(twin, mask); owner[h] >= 0; h = (h + 1) & mask) {
                if (keys[h] == twin) {
                    o = owner[h];
                    break;
                }
            }
            nbr[i] = o;
            if (o > i / 3 && (thin[i / 3] || thin[o])) {
                work[nw++] = i;
            }
        }
        // Re-test the four outer edges of every flipped quad until nothing flips.
        int budget = 8 * nt;
        while (nw > 0 && budget > 0) {
            int i = work[--nw];
            if (nbr[i] < 0 || !flipCapEdge(xy, tri, nbr, i)) {
                continue;
            }
            budget--;
            int t1 = i / 3;
            int t2 = nbr[t1 * 3 + 1];
            if (nw + 4 > work.length) {
                work = java.util.Arrays.copyOf(work, work.length * 2);
            }
            work[nw++] = t1 * 3;
            work[nw++] = t1 * 3 + 2;
            work[nw++] = t2 * 3;
            work[nw++] = t2 * 3 + 1;
        }
    }

    /**
     * How elongated the triangle {@code (a, b, c)} of the points in {@code xy} is: its longest
     * edge squared over twice its area (about the longest edge over the height on it), infinite
     * for a degenerate one.
     */
    private static double capAspect(float[] xy, int a, int b, int c) {
        double abx = xy[b * 2] - xy[a * 2];
        double aby = xy[b * 2 + 1] - xy[a * 2 + 1];
        double acx = xy[c * 2] - xy[a * 2];
        double acy = xy[c * 2 + 1] - xy[a * 2 + 1];
        double bcx = acx - abx;
        double bcy = acy - aby;
        double area2 = Math.abs(abx * acy - aby * acx);
        double lmax = Math.max(abx * abx + aby * aby,
                Math.max(acx * acx + acy * acy, bcx * bcx + bcy * bcy));
        return area2 > 0 ? lmax / area2 : Double.POSITIVE_INFINITY;
    }

    /** Hash slot of a directed-edge key in {@link #delaunayFlips}' table. */
    private static int edgeSlot(long key, int mask) {
        return (int) ((key * 0x9E3779B97F4A7C15L) >>> 32) & mask;
    }

    /**
     * Flip edge {@code i % 3} of triangle {@code i / 3} in {@link #delaunayFlips} if that makes
     * it locally Delaunay; returns whether it flipped.
     */
    private static boolean flipCapEdge(float[] xy, int[] tri, int[] nbr, int i) {
        int t1 = i / 3;
        int k = i % 3;
        int t2 = nbr[i];
        int a = tri[i];
        int b = tri[t1 * 3 + (k + 1) % 3];
        int c = tri[t1 * 3 + (k + 2) % 3];
        int m = -1;
        for (int j = 0; j < 3; j++) {
            if (tri[t2 * 3 + j] == b && tri[t2 * 3 + (j + 1) % 3] == a) {
                m = j;
            }
        }
        if (m < 0) {
            return false;
        }
        int d = tri[t2 * 3 + (m + 2) % 3];
        if (!inCircumcircle(xy, a, b, c, d) || !clearCcw(xy, a, d, c)
                || !clearCcw(xy, d, b, c)) {
            return false;
        }
        int nAd = nbr[t2 * 3 + (m + 1) % 3];
        int nDb = nbr[t2 * 3 + (m + 2) % 3];
        int nBc = nbr[t1 * 3 + (k + 1) % 3];
        int nCa = nbr[t1 * 3 + (k + 2) % 3];
        tri[t1 * 3] = a;
        tri[t1 * 3 + 1] = d;
        tri[t1 * 3 + 2] = c;
        nbr[t1 * 3] = nAd;
        nbr[t1 * 3 + 1] = t2;
        nbr[t1 * 3 + 2] = nCa;
        tri[t2 * 3] = d;
        tri[t2 * 3 + 1] = b;
        tri[t2 * 3 + 2] = c;
        nbr[t2 * 3] = nDb;
        nbr[t2 * 3 + 1] = nBc;
        nbr[t2 * 3 + 2] = t1;
        // Edge a-d moved from t2 to t1 and edge b-c from t1 to t2.
        relinkCapTri(nbr, nAd, t2, t1);
        relinkCapTri(nbr, nBc, t1, t2);
        return true;
    }

    /** Point triangle {@code t}'s neighbour link from {@code from} to {@code to}. */
    private static void relinkCapTri(int[] nbr, int t, int from, int to) {
        if (t < 0) {
            return;
        }
        for (int j = t * 3; j < t * 3 + 3; j++) {
            if (nbr[j] == from) {
                nbr[j] = to;
                return;
            }
        }
    }

    /**
     * True if point {@code d} lies clearly inside the circumcircle of the CCW triangle
     * {@code (a, b, c)} of the points in {@code xy} (relative tolerance, so cocircular points
     * never flip back and forth).
     */
    private static boolean inCircumcircle(float[] xy, int a, int b, int c, int d) {
        double dx = xy[d * 2];
        double dy = xy[d * 2 + 1];
        double adx = xy[a * 2] - dx;
        double ady = xy[a * 2 + 1] - dy;
        double bdx = xy[b * 2] - dx;
        double bdy = xy[b * 2 + 1] - dy;
        double cdx = xy[c * 2] - dx;
        double cdy = xy[c * 2 + 1] - dy;
        double ad = adx * adx + ady * ady;
        double bd = bdx * bdx + bdy * bdy;
        double cd = cdx * cdx + cdy * cdy;
        double ta = bdx * cdy - cdx * bdy;
        double tb = cdx * ady - adx * cdy;
        double tc = adx * bdy - bdx * ady;
        double det = ad * ta + bd * tb + cd * tc;
        double scale = ad * Math.abs(ta) + bd * Math.abs(tb) + cd * Math.abs(tc);
        return det > 1e-9 * scale;
    }

    /**
     * An open piece of the {@link #scanlineCaps} sweep: its current left and right edges, the
     * {@code y} it starts at, and its left and right boundary chains as flat {@code x,y} lists
     * from the bottom up.
     */
    private static final class CapPiece {
        int mLeft;
        int mRight;
        final float mY0;
        /** The left edge this piece is filed under in the sweep's lookup table. */
        int mKey;
        /** Whether a run of the current slab already carries this piece on. */
        boolean mClaimed;
        float[] mL = new float[8];
        int mNl;
        float[] mR = new float[8];
        int mNr;

        CapPiece(int left, int right, float y0) {
            mLeft = left;
            mRight = right;
            mY0 = y0;
        }

        void addLeft(float x, float y) {
            if (mNl * 2 + 2 > mL.length) {
                mL = java.util.Arrays.copyOf(mL, mL.length * 2);
            }
            mL[mNl * 2] = x;
            mL[mNl * 2 + 1] = y;
            mNl++;
        }

        void addRight(float x, float y) {
            if (mNr * 2 + 2 > mR.length) {
                mR = java.util.Arrays.copyOf(mR, mR.length * 2);
            }
            mR[mNr * 2] = x;
            mR[mNr * 2 + 1] = y;
            mNr++;
        }
    }

    /**
     * Side geometry for {@link #extrudePath}: one outward-facing quad per contour edge. When
     * {@code b <= 0} (or {@code inset == null}) it is a single straight wall spanning the full
     * depth
     * (the sharp case). When {@code b > 0} each edge becomes three rings — a front 45° chamfer
     * (inset cap edge → full wall), the straight wall (shortened by {@code 2b}), and a back
     * chamfer —
     * so the cap↔wall corner is bevelled. {@code inset[c][i]} is the mitered inset of contour
     * vertex
     * {@code i}, paired 1:1 with the full position {@code contours[c][i]}. {@code holes[c]} tells
     * whether contour {@code c} is a hole (so its wall faces into it).
     *
     * <p>With {@code withUv}, u runs along each contour's perimeter in wall order (0..1 per
     * contour, increasing to the right seen from outside) and v from 0 at the back face to 1 at
     * the front face.
     */
    private static MeshData extrudePathSides(
            float @NonNull [][] contours, boolean @NonNull [] holes, float @Nullable [][] inset,
            float cx, float cy, float cz, float hd, float b, boolean withUv) {
        boolean beveled = b > 0f && inset != null;
        float wallHd = hd - b;
        float inv = (float) 0.70710678; // 1/sqrt(2), the 45° chamfer normal split
        int edgeCount = 0;
        for (float[] ring : contours) {
            edgeCount += ring.length / 2;
        }
        int vpe = beveled ? 12 : 4;
        int tpe = beveled ? 6 : 2;
        float[] verts = new float[edgeCount * vpe * 3];
        float[] normals = new float[edgeCount * vpe * 3];
        int[] indices = new int[edgeCount * tpe * 3];
        float[] uv = withUv ? new float[edgeCount * vpe * 2] : null;
        float vWallTop = hd != 0f ? (hd + wallHd) / (2f * hd) : 1f;
        float vWallBot = hd != 0f ? (hd - wallHd) / (2f * hd) : 0f;
        int io = 0;
        int se = 0;
        for (int c = 0; c < contours.length; c++) {
            float[] ring = contours[c];
            int n = ring.length / 2;
            if (n < 3) {
                se += n;
                continue;
            }
            float[] ins = beveled ? inset[c] : null;
            boolean hole = holes[c];
            boolean reverse = (signedArea(ring) > 0f) == hole;
            float perim = uv != null ? perimeter(ring) : 0f;
            float run = 0f;
            for (int i = 0; i < n; i++) {
                int ia = reverse ? (n - i) % n : i;
                int ib = reverse ? (n - i - 1 + n) % n : (i + 1) % n;
                float ax = ring[ia * 2];
                float ay = ring[ia * 2 + 1];
                float bx = ring[ib * 2];
                float by = ring[ib * 2 + 1];
                float nx = by - ay; // outward normal = right of travel for a material-on-left loop
                float ny = -(bx - ax);
                float len = (float) Math.sqrt(nx * nx + ny * ny);
                if (len > 1e-9f) {
                    nx /= len;
                    ny /= len;
                }
                float u0 = 0f;
                float u1 = 0f;
                if (uv != null) {
                    u0 = perim > 0f ? Math.min(run / perim, 1f) : (float) i / n;
                    run += len;
                    u1 = i == n - 1 ? 1f : (perim > 0f ? Math.min(run / perim, 1f)
                            : (float) (i + 1) / n);
                }
                if (!beveled) {
                    int v = se * 4;
                    setV(verts, normals, v, cx + ax, cy + ay, cz + hd, nx, ny, 0f);
                    setV(verts, normals, v + 1, cx + bx, cy + by, cz + hd, nx, ny, 0f);
                    setV(verts, normals, v + 2, cx + ax, cy + ay, cz - hd, nx, ny, 0f);
                    setV(verts, normals, v + 3, cx + bx, cy + by, cz - hd, nx, ny, 0f);
                    if (uv != null) {
                        setWallUv(uv, v, u0, u1, 1f, 0f);
                    }
                    indices[io++] = v;
                    indices[io++] = v + 2;
                    indices[io++] = v + 3;
                    indices[io++] = v;
                    indices[io++] = v + 3;
                    indices[io++] = v + 1;
                } else {
                    float iax = ins[ia * 2];
                    float iay = ins[ia * 2 + 1];
                    float ibx = ins[ib * 2];
                    float iby = ins[ib * 2 + 1];
                    int v = se * 12;
                    if (uv != null) {
                        setWallUv(uv, v, u0, u1, 1f, vWallTop);
                        setWallUv(uv, v + 4, u0, u1, vWallTop, vWallBot);
                        setWallUv(uv, v + 8, u0, u1, vWallBot, 0f);
                    }
                    // Front chamfer: inset cap edge (+hd) → full wall top (+wallHd); normal up+out.
                    setV(verts, normals, v, cx + iax, cy + iay, cz + hd, nx * inv, ny * inv, inv);
                    setV(verts, normals, v + 1, cx + ibx, cy + iby, cz + hd, nx * inv, ny * inv,
                            inv);
                    setV(verts, normals, v + 2, cx + ax, cy + ay, cz + wallHd, nx * inv, ny * inv,
                            inv);
                    setV(verts, normals, v + 3, cx + bx, cy + by, cz + wallHd, nx * inv, ny * inv,
                            inv);
                    indices[io++] = v;
                    indices[io++] = v + 2;
                    indices[io++] = v + 3;
                    indices[io++] = v;
                    indices[io++] = v + 3;
                    indices[io++] = v + 1;
                    // Straight wall: full XY, +wallHd → -wallHd; normal purely outward.
                    setV(verts, normals, v + 4, cx + ax, cy + ay, cz + wallHd, nx, ny, 0f);
                    setV(verts, normals, v + 5, cx + bx, cy + by, cz + wallHd, nx, ny, 0f);
                    setV(verts, normals, v + 6, cx + ax, cy + ay, cz - wallHd, nx, ny, 0f);
                    setV(verts, normals, v + 7, cx + bx, cy + by, cz - wallHd, nx, ny, 0f);
                    indices[io++] = v + 4;
                    indices[io++] = v + 6;
                    indices[io++] = v + 7;
                    indices[io++] = v + 4;
                    indices[io++] = v + 7;
                    indices[io++] = v + 5;
                    // Back chamfer: full wall bottom (-wallHd) → inset cap edge (-hd); normal
                    // down+out.
                    setV(verts, normals, v + 8, cx + ax, cy + ay, cz - wallHd, nx * inv, ny * inv,
                            -inv);
                    setV(verts, normals, v + 9, cx + bx, cy + by, cz - wallHd, nx * inv, ny * inv,
                            -inv);
                    setV(verts, normals, v + 10, cx + iax, cy + iay, cz - hd, nx * inv, ny * inv,
                            -inv);
                    setV(verts, normals, v + 11, cx + ibx, cy + iby, cz - hd, nx * inv, ny * inv,
                            -inv);
                    indices[io++] = v + 8;
                    indices[io++] = v + 10;
                    indices[io++] = v + 11;
                    indices[io++] = v + 8;
                    indices[io++] = v + 11;
                    indices[io++] = v + 9;
                }
                se++;
            }
        }
        return new MeshData(verts, normals, indices, uv);
    }

    /**
     * UV of one wall quad laid out as (a top, b top, a bottom, b bottom) from vertex {@code v}:
     * u = {@code u0}/{@code u1} at the edge ends, v = {@code vTop}/{@code vBot}.
     */
    private static void setWallUv(
            float[] uv, int v, float u0, float u1, float vTop, float vBot) {
        int q = v * 2;
        uv[q] = u0;
        uv[q + 1] = vTop;
        uv[q + 2] = u1;
        uv[q + 3] = vTop;
        uv[q + 4] = u0;
        uv[q + 5] = vBot;
        uv[q + 6] = u1;
        uv[q + 7] = vBot;
    }

    /** Perimeter of a closed contour given as x,y pairs. */
    private static float perimeter(float @NonNull [] ring) {
        int n = ring.length / 2;
        float sum = 0f;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float dx = ring[j * 2] - ring[i * 2];
            float dy = ring[j * 2 + 1] - ring[i * 2 + 1];
            sum += (float) Math.sqrt(dx * dx + dy * dy);
        }
        return sum;
    }

    /**
     * Write vertex {@code i}'s position and normal (the bevel rings need a non-zero {@code nz})
     * .
     */
    private static void setV(
            float[] verts, float[] normals, int i,
            float x, float y, float z, float nx, float ny, float nz) {
        int p = i * 3;
        verts[p] = x;
        verts[p + 1] = y;
        verts[p + 2] = z;
        normals[p] = nx;
        normals[p + 1] = ny;
        normals[p + 2] = nz;
    }

    /**
     * Offset a closed contour {@code ring} (x,y pairs) into the material by {@code b}, mitering
     * at corners so adjacent edges share the moved vertex (no gaps): a solid contour moves toward
     * its own interior, a {@code hole} away from it (the hole grows), whatever the ring's
     * orientation. {@code miterLimit} caps the corner stretch at sharp spikes.
     *
     * <p>Where the bevel is comparable to an edge, the two mitered end offsets of that edge can
     * slide past each other along it, folding the inset contour through itself (inverted chamfer
     * quads and cap trapezoids). Each vertex's offset is therefore scaled down just enough that
     * every inset edge keeps at least {@link #INSET_MIN_EDGE_FRACTION} of its length and its
     * direction; vertices that need no clamp are computed exactly as without it. Global
     * self-intersections (a neck narrower than {@code 2b}) are not resolved here.
     */
    private static float @NonNull [] miterInset(
            float @NonNull [] ring, float b, float miterLimit, boolean hole) {
        int n = ring.length / 2;
        float[] out = new float[ring.length];
        float[] dirX = new float[n]; // unit miter direction per vertex
        float[] dirY = new float[n];
        float[] dist = new float[n]; // offset distance along it
        // +1: offset to the left of travel (a CCW solid's interior, a CW hole's exterior).
        float sign = (signedArea(ring) >= 0f) != hole ? 1f : -1f;
        for (int i = 0; i < n; i++) {
            int ip = (i - 1 + n) % n;
            int in = (i + 1) % n;
            float pdx = ring[i * 2] - ring[ip * 2];
            float pdy = ring[i * 2 + 1] - ring[ip * 2 + 1];
            float ndx = ring[in * 2] - ring[i * 2];
            float ndy = ring[in * 2 + 1] - ring[i * 2 + 1];
            float pl = (float) Math.sqrt(pdx * pdx + pdy * pdy);
            if (pl > 1e-9f) {
                pdx /= pl;
                pdy /= pl;
            }
            float nl = (float) Math.sqrt(ndx * ndx + ndy * ndy);
            if (nl > 1e-9f) {
                ndx /= nl;
                ndy /= nl;
            }
            // Inward normal toward the ring interior = sign · left-of-travel (-dy, dx).
            float n1x = sign * -pdy;
            float n1y = sign * pdx;
            float n2x = sign * -ndy;
            float n2y = sign * ndx;
            float mx = n1x + n2x;
            float my = n1y + n2y;
            float ml = (float) Math.sqrt(mx * mx + my * my);
            if (ml < 1e-6f) {
                dirX[i] = n2x;
                dirY[i] = n2y;
                dist[i] = b;
                continue;
            }
            mx /= ml;
            my /= ml;
            float cos = mx * n1x + my * n1y; // angle between the bisector and an edge normal
            dirX[i] = mx;
            dirY[i] = my;
            dist[i] = b / Math.max(cos, 1f / miterLimit);
        }
        // Per edge i→j: the components of the two end offsets along the edge (positive = moving
        // toward the other end) must leave INSET_MIN_EDGE_FRACTION of it, else scale them down.
        float[] scale = null;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ex = ring[j * 2] - ring[i * 2];
            float ey = ring[j * 2 + 1] - ring[i * 2 + 1];
            float len = (float) Math.sqrt(ex * ex + ey * ey);
            if (!(len > 0f)) {
                continue;
            }
            float ti = dist[i] * (dirX[i] * ex + dirY[i] * ey) / len;
            float tj = -dist[j] * (dirX[j] * ex + dirY[j] * ey) / len;
            float slide = Math.max(ti, 0f) + Math.max(tj, 0f);
            float room = (1f - INSET_MIN_EDGE_FRACTION) * len;
            if (slide > room) {
                if (scale == null) {
                    scale = new float[n];
                    java.util.Arrays.fill(scale, 1f);
                }
                float f = room / slide;
                if (ti > 0f) {
                    scale[i] = Math.min(scale[i], f);
                }
                if (tj > 0f) {
                    scale[j] = Math.min(scale[j], f);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            float l = scale == null ? dist[i] : dist[i] * scale[i];
            out[i * 2] = ring[i * 2] + l * dirX[i];
            out[i * 2 + 1] = ring[i * 2 + 1] + l * dirY[i];
        }
        return out;
    }

    /**
     * Clean the parsed {@link #extrudePath} contours before triangulation: drop non-finite points,
     * merge consecutive near-coincident points (including the closing pair), and drop contours
     * left with fewer than 3 points. The merge tolerance is relative to the overall path size, so
     * zero-length edges (zero-area wall quads, unstable miter directions) cannot survive. Contours
     * that need no change are returned as the same array.
     */
    private static float @NonNull [][] sanitizeContours(float @NonNull [][] contours) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float[] ring : contours) {
            for (int i = 0; i + 1 < ring.length; i += 2) {
                float x = ring[i];
                float y = ring[i + 1];
                if (isFinite(x) && isFinite(y)) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        float eps = maxX >= minX ? Math.max(maxX - minX, maxY - minY) * CONTOUR_MERGE_EPS : 0f;
        ArrayList<float[]> out = new ArrayList<>(contours.length);
        for (float[] ring : contours) {
            int n = ring.length / 2;
            float[] kept = new float[n * 2];
            int m = 0;
            for (int i = 0; i < n; i++) {
                float x = ring[i * 2];
                float y = ring[i * 2 + 1];
                if (!isFinite(x) || !isFinite(y)) {
                    continue;
                }
                if (m > 0 && near(kept[m * 2 - 2], kept[m * 2 - 1], x, y, eps)) {
                    continue;
                }
                kept[m * 2] = x;
                kept[m * 2 + 1] = y;
                m++;
            }
            while (m > 1 && near(kept[m * 2 - 2], kept[m * 2 - 1], kept[0], kept[1], eps)) {
                m--;
            }
            if (m < 3) {
                continue;
            }
            out.add(m == n && ring.length == n * 2 ? ring : java.util.Arrays.copyOf(kept, m * 2));
        }
        return out.toArray(new float[0][]);
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    private static boolean near(float ax, float ay, float bx, float by, float eps) {
        return Math.abs(bx - ax) <= eps && Math.abs(by - ay) <= eps;
    }

    /**
     * True if two non-adjacent edges of the closed contour {@code ring} (x,y pairs) properly cross.
     * O(n²); used only for the {@link #DEBUG} bevel diagnostics and by tests.
     */
    static boolean hasSelfIntersection(float @NonNull [] ring) {
        int n = ring.length / 2;
        for (int i = 0; i < n; i++) {
            int i1 = (i + 1) % n;
            for (int j = i + 2; j < n; j++) {
                int j1 = (j + 1) % n;
                if (j1 == i) {
                    continue; // the closing edge shares vertex i
                }
                if (segmentsCross(ring[i * 2], ring[i * 2 + 1], ring[i1 * 2], ring[i1 * 2 + 1],
                        ring[j * 2], ring[j * 2 + 1], ring[j1 * 2], ring[j1 * 2 + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Proper (interior) crossing test of segments ab and cd. */
    private static boolean segmentsCross(
            double ax, double ay, double bx, double by,
            double cx, double cy, double dx, double dy) {
        double o1 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        double o2 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        double o3 = (dx - cx) * (ay - cy) - (dy - cy) * (ax - cx);
        double o4 = (dx - cx) * (by - cy) - (dy - cy) * (bx - cx);
        return ((o1 > 0 && o2 < 0) || (o1 < 0 && o2 > 0))
                && ((o3 > 0 && o4 < 0) || (o3 < 0 && o4 > 0));
    }

    /** Signed area (shoelace) of a contour given as x,y pairs; >0 = CCW in a y-up frame. */
    private static float signedArea(float @NonNull [] ring) {
        int n = ring.length / 2;
        float a = 0f;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            a += ring[i * 2] * ring[j * 2 + 1] - ring[j * 2] * ring[i * 2 + 1];
        }
        return a * 0.5f;
    }

    /**
     * Even-odd hole test: a contour is a hole if it lies inside an odd number of the other
     * contours.
     */
    private static boolean isHole(float @NonNull [][] contours, float @NonNull [] ring) {
        if (ring.length < 2) {
            return false;
        }
        float px = ring[0];
        float py = ring[1];
        int inside = 0;
        for (float[] other : contours) {
            if (other == ring) {
                continue;
            }
            if (pointInPolygon(other, px, py)) {
                inside++;
            }
        }
        return (inside & 1) == 1;
    }

    /** Standard ray-cast point-in-polygon for a contour given as x,y pairs. */
    private static boolean pointInPolygon(float @NonNull [] ring, float px, float py) {
        int n = ring.length / 2;
        boolean in = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = ring[i * 2];
            float yi = ring[i * 2 + 1];
            float xj = ring[j * 2];
            float yj = ring[j * 2 + 1];
            boolean crosses = (yi > py) != (yj > py);
            if (crosses && px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /**
     * Extruded disk (a coin): {@code radius}, thickness {@code depth}, {@code sides} around. UV as
     * in {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeCircle(
            float radius, float depth, float cx, float cy, float cz, int sides, int uvMode) {
        float[] poly = new float[(sides + 1) * 2];
        for (int j = 0; j < sides; j++) {
            double phi = 2.0 * Math.PI * j / sides;
            poly[j * 2] = radius * (float) Math.cos(phi);
            poly[j * 2 + 1] = radius * (float) Math.sin(phi);
        }
        int center = sides; // (0,0) already zero
        int[] cap = new int[sides * 3];
        int[] loop = new int[sides];
        int k = 0;
        for (int j = 0; j < sides; j++) {
            int j1 = (j + 1) % sides;
            cap[k++] = center;
            cap[k++] = j;
            cap[k++] = j1;
            loop[j] = j;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    /**
     * Extruded pie slice: {@code radius}, from {@code a0} sweeping {@code sweep} rad, fan apex
     * at the center. UV as in {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeSector(
            float radius, float a0, float sweep, float depth, float cx, float cy, float cz, int n,
            int uvMode) {
        float[] poly = new float[(n + 2) * 2]; // index 0 = center, 1..n+1 = arc
        for (int k = 0; k <= n; k++) {
            double phi = a0 + sweep * k / n;
            poly[(1 + k) * 2] = radius * (float) Math.cos(phi);
            poly[(1 + k) * 2 + 1] = radius * (float) Math.sin(phi);
        }
        int[] cap = new int[n * 3];
        int m = 0;
        for (int k = 0; k < n; k++) {
            cap[m++] = 0;
            cap[m++] = 1 + k;
            cap[m++] = 2 + k;
        }
        int[] loop = new int[n + 2]; // center, arc0..arcN (closed)
        loop[0] = 0;
        for (int k = 0; k <= n; k++) {
            loop[1 + k] = 1 + k;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    /**
     * Extruded circular segment (the region between an arc and its chord). UV as in
     * {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeSegment(
            float radius, float a0, float sweep, float depth, float cx, float cy, float cz, int n,
            int uvMode) {
        float[] poly = new float[(n + 1) * 2];
        for (int k = 0; k <= n; k++) {
            double phi = a0 + sweep * k / n;
            poly[k * 2] = radius * (float) Math.cos(phi);
            poly[k * 2 + 1] = radius * (float) Math.sin(phi);
        }
        int[] cap = new int[Math.max(0, n - 1) * 3]; // fan from arc[0] across the arc
        int m = 0;
        for (int k = 1; k < n; k++) {
            cap[m++] = 0;
            cap[m++] = k;
            cap[m++] = k + 1;
        }
        int[] loop = new int[n + 1]; // arc 0..n, closed by the chord n->0
        for (int k = 0; k <= n; k++) {
            loop[k] = k;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    /**
     * Extruded annular sector (a curved bar / ring slice): {@code r0}..{@code r1}, {@code a0}+
     * {@code sweep}. UV as in {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeArc(
            float r0,
            float r1,
            float a0,
            float sweep,
            float depth,
            float cx,
            float cy,
            float cz,
            int n,
            int uvMode) {
        int inner = n + 1; // outer 0..n, inner (n+1)..(2n+1)
        float[] poly = new float[(2 * (n + 1)) * 2];
        for (int k = 0; k <= n; k++) {
            double phi = a0 + sweep * k / n;
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);
            poly[k * 2] = r1 * c;
            poly[k * 2 + 1] = r1 * s;
            int ii = inner + k;
            poly[ii * 2] = r0 * c;
            poly[ii * 2 + 1] = r0 * s;
        }
        int[] cap = new int[n * 2 * 3]; // quad strip between outer and inner arcs
        int m = 0;
        for (int k = 0; k < n; k++) {
            int o0 = k;
            int o1 = k + 1;
            int i0 = inner + k;
            int i1 = inner + k + 1;
            cap[m++] = o0;
            cap[m++] = o1;
            cap[m++] = i1;
            cap[m++] = o0;
            cap[m++] = i1;
            cap[m++] = i0;
        }
        int[] loop = new int[2 * (n + 1)]; // outer fwd, then inner reversed
        int li = 0;
        for (int k = 0; k <= n; k++) {
            loop[li++] = k;
        }
        for (int k = n; k >= 0; k--) {
            loop[li++] = inner + k;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    /**
     * Extruded rounded rectangle: {@code width}×{@code height}, corner radius {@code cr},
     * {@code cseg} per corner. UV as in {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeRoundedRect(
            float width,
            float height,
            float cr,
            float depth,
            float cx,
            float cy,
            float cz,
            int cseg,
            int uvMode) {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        cr = clamp(cr, 0f, Math.min(hw, hh));
        float ix = hw - cr; // corner-arc center offsets
        float iy = hh - cr;
        // Four corner centers + starting angles, CCW from the bottom-right corner.
        float[] ccx = {ix, ix, -ix, -ix};
        float[] ccy = {-iy, iy, iy, -iy};
        float[] startA = {-(float) (Math.PI / 2), 0f, (float) (Math.PI / 2), (float) Math.PI};
        int per = cseg + 1; // points per corner arc
        int pcount = 4 * per;
        float[] poly = new float[(pcount + 1) * 2]; // perimeter + center
        int pi = 0;
        for (int corner = 0; corner < 4; corner++) {
            for (int s = 0; s <= cseg; s++) {
                double ang = startA[corner] + (Math.PI / 2) * s / cseg;
                poly[pi * 2] = ccx[corner] + cr * (float) Math.cos(ang);
                poly[pi * 2 + 1] = ccy[corner] + cr * (float) Math.sin(ang);
                pi++;
            }
        }
        int center = pcount; // (0,0) already zero
        int[] cap = new int[pcount * 3];
        int[] loop = new int[pcount];
        int k = 0;
        for (int j = 0; j < pcount; j++) {
            int j1 = (j + 1) % pcount;
            cap[k++] = center;
            cap[k++] = j;
            cap[k++] = j1;
            loop[j] = j;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    /**
     * Extruded squircle (superellipse {@code |x/R|^n + |y/R|^n = 1}): {@code radius}, {@code
     * exponent}. UV as in {@link #extrude}.
     */
    @NonNull
    public static MeshData extrudeSquircle(
            float radius, float exponent, float depth, float cx, float cy, float cz, int samples,
            int uvMode) {
        float e = exponent <= 0f ? 4f : exponent;
        float p2 = 2f / e;
        float[] poly = new float[(samples + 1) * 2];
        for (int j = 0; j < samples; j++) {
            double phi = 2.0 * Math.PI * j / samples;
            float ct = (float) Math.cos(phi);
            float st = (float) Math.sin(phi);
            poly[j * 2] = radius * Math.signum(ct) * (float) Math.pow(Math.abs(ct), p2);
            poly[j * 2 + 1] = radius * Math.signum(st) * (float) Math.pow(Math.abs(st), p2);
        }
        int center = samples; // (0,0)
        int[] cap = new int[samples * 3];
        int[] loop = new int[samples];
        int k = 0;
        for (int j = 0; j < samples; j++) {
            int j1 = (j + 1) % samples;
            cap[k++] = center;
            cap[k++] = j;
            cap[k++] = j1;
            loop[j] = j;
        }
        return extrude(poly, cap, loop, depth, cx, cy, cz, uvMode);
    }

    private static void cross(
            float ax, float ay, float az, float bx, float by, float bz, float[] out) {
        out[0] = ay * bz - az * by;
        out[1] = az * bx - ax * bz;
        out[2] = ax * by - ay * bx;
    }

    private static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    private static int clampInt(int x, int lo, int hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    /**
     * Catmull-Rom interpolation of the span {@code p1..p2} (with neighbours {@code p0,p3}) at
     * {@code t}.
     */
    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f
                * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}

