/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.operations.utilities;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.utilities.easing.MonotonicSpline;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Topology and domain helper for 2D vertex meshes.
 *
 * <p>A 2D mesh is a sampling of a {@code (u, v)} domain. The {@code layout} decides three things:
 *
 * <ul>
 *   <li>how {@code u} and {@code v} are sampled across the domain,
 *   <li>how the resulting vertices are connected into a triangle list (including whether the {@code
 *       u} axis wraps around, as it must for anything radial),
 *   <li>what the position is when the document supplies no position expression - so a {@code polar}
 *       mesh is a disc without the author having to spend expression tokens on {@code cos}/{@code
 *       sin}.
 * </ul>
 *
 * <p>Position expressions, when present, always win: they produce cartesian x/y directly and the
 * default below is ignored. The defaults are all built inside the unit square or unit circle
 * centred on the origin, so the ordinary 2D canvas matrix ({@code translate}/{@code scale}) places
 * and sizes them.
 *
 * <p>This class is deliberately free of any rendering or platform dependency: it computes flat
 * arrays in the exact layout {@code Canvas.drawVertices} and {@code SkVertices} want.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Mesh2DGenerator {

    /** Rectangular domain: u across, v down. Warps, cloth, gradient fields, heat maps. */
    public static final int LAYOUT_GRID = 0;

    /** Disc: u is angle 0..1 mapped to 0..2pi, v is radius 0..1. */
    public static final int LAYOUT_POLAR = 1;

    /** Annulus: u is angle, v runs across a band between an inner radius and 1. */
    public static final int LAYOUT_RING = 2;

    /** Ribbon: u along, v across. Trails, variable width strokes. */
    public static final int LAYOUT_STRIP = 3;

    /** Triangle fan from a centre vertex: u is angle. Pie wedges, cones of light. */
    public static final int LAYOUT_FAN = 4;

    /** Ribbon following a path: u is arclength along the path in {@code aux}, v across. */
    public static final int LAYOUT_PATH_STRIP = 5;

    /** Default inner radius for {@link #LAYOUT_RING}, as a fraction of the outer radius. */
    public static final float DEFAULT_RING_INNER_RADIUS = 0.5f;

    private static final float TWO_PI = (float) (Math.PI * 2);

    private Mesh2DGenerator() {
    }

    /**
     * Whether the u axis wraps, i.e. the last column of vertices connects back to the first.
     *
     * <p>Radial layouts wrap; rectangular ones do not. This also changes how u is sampled: a
     * wrapping layout must not place a vertex at both u=0 and u=1, because they are the same point.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @return true if the u axis is periodic
     */
    public static boolean wrapsU(int layout) {
        return layout == LAYOUT_POLAR || layout == LAYOUT_RING || layout == LAYOUT_FAN;
    }

    /**
     * The number of vertices this layout produces.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param uCount samples along u
     * @param vCount samples along v
     * @return the vertex count
     */
    public static int vertexCount(int layout, int uCount, int vCount) {
        if (layout == LAYOUT_FAN) {
            // one shared centre vertex plus a rim
            return uCount + 1;
        }
        return uCount * vCount;
    }

    /**
     * The number of indices this layout produces (3 per triangle).
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param uCount samples along u
     * @param vCount samples along v
     * @return the index count
     */
    public static int indexCount(int layout, int uCount, int vCount) {
        if (layout == LAYOUT_FAN) {
            return uCount * 3;
        }
        if (vCount < 2 || uCount < 2) {
            return 0;
        }
        int columns = wrapsU(layout) ? uCount : uCount - 1;
        return columns * (vCount - 1) * 6;
    }

    /**
     * The u parameter for sample {@code i}.
     *
     * <p>A wrapping layout divides by {@code uCount} so that the sample after the last is the
     * first; a non-wrapping one divides by {@code uCount - 1} so the domain is closed at both ends.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param i      the sample index along u
     * @param uCount samples along u
     * @return u in the range 0..1
     */
    public static float domainU(int layout, int i, int uCount) {
        if (uCount <= 1) {
            return 0f;
        }
        if (wrapsU(layout)) {
            return i / (float) uCount;
        }
        return i / (float) (uCount - 1);
    }

    /**
     * The v parameter for sample {@code j}.
     *
     * @param j      the sample index along v
     * @param vCount samples along v
     * @return v in the range 0..1
     */
    public static float domainV(int j, int vCount) {
        if (vCount <= 1) {
            return 0f;
        }
        return j / (float) (vCount - 1);
    }

    /**
     * Fill the triangle list for this layout.
     *
     * <p>Winding is consistent across every layout so a document can rely on it.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param uCount samples along u
     * @param vCount samples along v
     * @param out    the destination, at least {@link #indexCount} long
     */
    public static void generateIndices(int layout, int uCount, int vCount, int @NonNull [] out) {
        int k = 0;
        if (layout == LAYOUT_FAN) {
            // vertex 0 is the centre, 1..uCount are the rim
            for (int i = 0; i < uCount; i++) {
                out[k++] = 0;
                out[k++] = 1 + i;
                out[k++] = 1 + ((i + 1) % uCount);
            }
            return;
        }
        if (vCount < 2 || uCount < 2) {
            return;
        }
        boolean wrap = wrapsU(layout);
        int columns = wrap ? uCount : uCount - 1;
        for (int j = 0; j < vCount - 1; j++) {
            for (int i = 0; i < columns; i++) {
                int i1 = wrap ? (i + 1) % uCount : i + 1;
                int topLeft = j * uCount + i;
                int topRight = j * uCount + i1;
                int bottomLeft = (j + 1) * uCount + i;
                int bottomRight = (j + 1) * uCount + i1;

                out[k++] = topLeft;
                out[k++] = bottomLeft;
                out[k++] = topRight;

                out[k++] = topRight;
                out[k++] = bottomLeft;
                out[k++] = bottomRight;
            }
        }
    }

    /**
     * The position a vertex takes when the document supplies no position expression.
     *
     * <p>Every default lives in the unit square or the unit circle centred on the origin; the
     * canvas matrix is what places and sizes it. This is what lets {@code layout: polar} be useful
     * without the author spending scarce expression tokens on trigonometry.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param u      the u parameter, 0..1
     * @param v      the v parameter, 0..1
     * @param out    a 2 element array receiving x then y
     */
    public static void defaultPosition(int layout, float u, float v, float @NonNull [] out) {
        switch (layout) {
            case LAYOUT_POLAR:
            case LAYOUT_FAN: {
                float angle = u * TWO_PI;
                out[0] = v * (float) Math.cos(angle);
                out[1] = v * (float) Math.sin(angle);
                break;
            }
            case LAYOUT_RING: {
                float angle = u * TWO_PI;
                float radius = DEFAULT_RING_INNER_RADIUS + (1f - DEFAULT_RING_INNER_RADIUS) * v;
                out[0] = radius * (float) Math.cos(angle);
                out[1] = radius * (float) Math.sin(angle);
                break;
            }
            case LAYOUT_GRID:
            case LAYOUT_STRIP:
            case LAYOUT_PATH_STRIP:
            default:
                out[0] = u;
                out[1] = v;
                break;
        }
    }

    /**
     * Sample a flat path, as stored by {@code RemoteContext.loadPathData}, at a fraction of its
     * arclength, returning position and unit tangent.
     *
     * <p>Used by {@link #LAYOUT_PATH_STRIP} to turn an existing path into a ribbon. Curves are
     * flattened to line segments before measuring, which is what the players do to rasterise them
     * anyway.
     *
     * @param polyline   the flattened path as x,y pairs
     * @param pointCount the number of x,y pairs in {@code polyline}
     * @param fraction   the position along the path, 0..1
     * @param out        a 4 element array receiving x, y, tangentX, tangentY
     */
    public static void samplePolyline(
            float @NonNull [] polyline, int pointCount, float fraction, float @NonNull [] out) {
        if (pointCount <= 0) {
            out[0] = 0f;
            out[1] = 0f;
            out[2] = 1f;
            out[3] = 0f;
            return;
        }
        if (pointCount == 1) {
            out[0] = polyline[0];
            out[1] = polyline[1];
            out[2] = 1f;
            out[3] = 0f;
            return;
        }

        float total = 0f;
        for (int i = 1; i < pointCount; i++) {
            float dx = polyline[i * 2] - polyline[(i - 1) * 2];
            float dy = polyline[i * 2 + 1] - polyline[(i - 1) * 2 + 1];
            total += (float) Math.hypot(dx, dy);
        }
        if (total <= 0f) {
            out[0] = polyline[0];
            out[1] = polyline[1];
            out[2] = 1f;
            out[3] = 0f;
            return;
        }

        float target = Math.max(0f, Math.min(1f, fraction)) * total;
        float walked = 0f;
        for (int i = 1; i < pointCount; i++) {
            float x0 = polyline[(i - 1) * 2];
            float y0 = polyline[(i - 1) * 2 + 1];
            float x1 = polyline[i * 2];
            float y1 = polyline[i * 2 + 1];
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float) Math.hypot(dx, dy);
            if (len <= 0f) {
                continue;
            }
            if (walked + len >= target) {
                float t = (target - walked) / len;
                out[0] = x0 + dx * t;
                out[1] = y0 + dy * t;
                out[2] = dx / len;
                out[3] = dy / len;
                return;
            }
            walked += len;
        }

        // past the end: clamp to the final point and its incoming tangent
        float x0 = polyline[(pointCount - 2) * 2];
        float y0 = polyline[(pointCount - 2) * 2 + 1];
        float x1 = polyline[(pointCount - 1) * 2];
        float y1 = polyline[(pointCount - 1) * 2 + 1];
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.hypot(dx, dy);
        out[0] = x1;
        out[1] = y1;
        out[2] = len > 0f ? dx / len : 1f;
        out[3] = len > 0f ? dy / len : 0f;
    }

    /**
     * Smallest gap forced between two spline knots.
     *
     * <p>Knot positions may be driven by document variables, so they can become equal or go
     * backwards at runtime however carefully they were validated at authoring time. A zero gap
     * divides by zero inside the spline and poisons the whole ribbon with NaN, so the knots are
     * nudged apart instead.
     */
    private static final float MIN_KNOT_GAP = 1e-6f;

    /**
     * Build the spline that gives a path strip its cross width along the path.
     *
     * <p>Returns {@code null} when there is nothing to interpolate - no widths at all, or a single
     * width, which is a constant and needs no spline. Callers must handle that case; see {@link
     * #widthAt}.
     *
     * <p>{@code positions} is optional. When it is absent the widths are spread evenly over the
     * path, so the first is the width at the start and the last is the width at the end. When it is
     * present it must have one entry per width, each a fraction of arclength.
     *
     * @param widths the width control points, in the path's own units
     * @param positions where each width sits along the path, 0..1, or null for evenly spaced
     * @return the interpolating spline, or null when the width is constant or undefined
     */
    public static @Nullable MonotonicSpline widthSpline(
            float @Nullable [] widths, float @Nullable [] positions) {
        if (widths == null || widths.length < 2) {
            return null;
        }
        float[] knots = null;
        if (positions != null && positions.length == widths.length) {
            knots = new float[positions.length];
            float previous = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < positions.length; i++) {
                float t = positions[i];
                if (Float.isNaN(t)) {
                    t = previous == Float.NEGATIVE_INFINITY ? 0f : previous + MIN_KNOT_GAP;
                } else if (previous != Float.NEGATIVE_INFINITY && t <= previous) {
                    t = previous + MIN_KNOT_GAP;
                }
                knots[i] = t;
                previous = t;
            }
        }
        return new MonotonicSpline(knots, widths);
    }

    /**
     * The cross width of a path strip at a fraction of its arclength.
     *
     * <p>A negative width would fold the ribbon inside out, so it is clamped away. The monotonic
     * fit will not overshoot between control points, but it does extrapolate past the ends.
     *
     * @param spline the spline from {@link #widthSpline}, or null when the width is constant
     * @param widths the width control points the spline was built from
     * @param fraction the position along the path, 0..1
     * @return the width, never negative
     */
    public static float widthAt(
            @Nullable MonotonicSpline spline, float @Nullable [] widths, float fraction) {
        float width;
        if (spline != null) {
            width = spline.getPos(fraction);
        } else if (widths != null && widths.length > 0) {
            width = widths[0];
        } else {
            return 1f;
        }
        if (Float.isNaN(width) || width < 0f) {
            return 0f;
        }
        return width;
    }

    /**
     * The fewest columns a round end cap is ever drawn with. Below this the arc reads as a bevel.
     */
    public static final int MIN_ROUND_CAP_SEGMENTS = 3;

    /**
     * The most columns a round end cap is ever drawn with. Beyond this the arc is already smooth.
     */
    public static final int MAX_ROUND_CAP_SEGMENTS = 16;

    /**
     * How many columns of the strip to spend on each round end cap.
     *
     * <p>The caps are extra columns on top of the body rather than a slice out of it, so a rounded
     * strip follows its path at exactly the same resolution as a flat one with the same {@code
     * segments}. The count scales with the body so a finely sampled strip does not end in a visibly
     * coarse arc, but it is clamped at both ends: three columns is the least that reads as round
     * rather than chamfered, and past sixteen the extra vertices buy nothing a viewer can see.
     *
     * @param segments the number of columns spanning the path itself
     * @return the number of columns in each cap
     */
    public static int roundCapSegments(int segments) {
        int cap = segments / 4;
        if (cap < MIN_ROUND_CAP_SEGMENTS) {
            return MIN_ROUND_CAP_SEGMENTS;
        }
        return Math.min(cap, MAX_ROUND_CAP_SEGMENTS);
    }

    /**
     * Place a vertex on a semicircular end cap of a path strip.
     *
     * <p>The cap is swept as a quarter turn, {@code angle} running from 0 at the tip to {@code
     * PI/2} where the cap meets the body. At each angle the two edges of the strip sit at {@code
     * +/- halfWidth * sin(angle)} across the path and {@code halfWidth * cos(angle)} beyond its
     * end, so the segments joining them sweep out exactly the half disc of radius {@code
     * halfWidth}: a true round cap, built from the strip's own quads rather than from extra fan
     * geometry. At {@code angle == PI/2} the point coincides with the flat end of the body, so the
     * cap joins it seamlessly; at {@code angle == 0} both edges meet at the tip, leaving one
     * degenerate column that rasterises to nothing.
     *
     * @param sample position and unit tangent at the path end, as filled by {@link #samplePolyline}
     * @param halfWidth half the strip's cross width at this end, which is the cap's radius
     * @param angle 0 at the tip, {@code PI/2} at the base where the cap meets the body
     * @param v the cross parameter, 0..1
     * @param outward 1 at the end of the path, -1 at the start, being the direction the cap bulges
     * @param out a 2 element array receiving x then y
     */
    public static void roundCapPoint(
            float @NonNull [] sample,
            float halfWidth,
            float angle,
            float v,
            float outward,
            float @NonNull [] out) {
        float tx = sample[2];
        float ty = sample[3];
        // the normal is the tangent turned a quarter turn, matching the body of the strip
        float nx = -ty;
        float ny = tx;
        float along = outward * halfWidth * (float) Math.cos(angle);
        float across = (v - 0.5f) * 2f * halfWidth * (float) Math.sin(angle);
        out[0] = sample[0] + tx * along + nx * across;
        out[1] = sample[1] + ty * along + ny * across;
    }

    /**
     * Convert an IEEE 754 half float, as carried by {@code type: f16Values}, to a 32 bit float.
     *
     * <p>Half floats are a wire format only. They are widened here, on read, so the expression
     * evaluator, the mesh cache and the {@code PaintContext} all see ordinary 32 bit floats and
     * nothing downstream knows the document used them.
     *
     * @param half the 16 bit pattern, in the low bits of an int
     * @return the value as a float
     */
    public static float halfToFloat(int half) {
        int sign = (half >>> 15) & 0x1;
        int exponent = (half >>> 10) & 0x1F;
        int mantissa = half & 0x3FF;

        int bits;
        if (exponent == 0) {
            if (mantissa == 0) {
                // signed zero
                bits = sign << 31;
            } else {
                // subnormal: normalise it
                exponent = 127 - 15 + 1;
                while ((mantissa & 0x400) == 0) {
                    mantissa <<= 1;
                    exponent--;
                }
                mantissa &= 0x3FF;
                bits = (sign << 31) | (exponent << 23) | (mantissa << 13);
            }
        } else if (exponent == 0x1F) {
            // infinity or NaN
            bits = (sign << 31) | 0x7F800000 | (mantissa << 13);
        } else {
            bits = (sign << 31) | ((exponent - 15 + 127) << 23) | (mantissa << 13);
        }
        return Float.intBitsToFloat(bits);
    }

    /**
     * Convert a 32 bit float to an IEEE 754 half float for the {@code f16Values} wire format.
     *
     * <p>16 bits is ample for screen space coordinates and for uv in 0..1 - a half float is exact
     * for integers up to 2048 and carries about three decimal digits - so 4096 is roughly where
     * this stops being free.
     *
     * @param value the value to convert
     * @return the 16 bit pattern, in the low bits of an int
     */
    public static int floatToHalf(float value) {
        int bits = Float.floatToRawIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exponent = (bits >>> 23) & 0xFF;
        int mantissa = bits & 0x7FFFFF;

        if (exponent == 0xFF) {
            // infinity or NaN; keep NaN non-zero in the mantissa
            return sign | 0x7C00 | (mantissa != 0 ? 0x200 : 0);
        }

        int unbiased = exponent - 127 + 15;
        if (unbiased >= 0x1F) {
            // overflow to infinity
            return sign | 0x7C00;
        }
        if (unbiased <= 0) {
            if (unbiased < -10) {
                // underflow to signed zero
                return sign;
            }
            // subnormal
            mantissa |= 0x800000;
            int shift = 14 - unbiased;
            int half = mantissa >>> shift;
            // round to nearest even
            if (((mantissa >>> (shift - 1)) & 0x1) != 0) {
                half++;
            }
            return sign | half;
        }

        int half = (unbiased << 10) | (mantissa >>> 13);
        // round to nearest even
        if ((mantissa & 0x1000) != 0) {
            half++;
        }
        return sign | half;
    }

    /**
     * Flatten stored path data into an x,y polyline.
     *
     * <p>Returns the number of points written. Curves are subdivided uniformly; this is only used
     * to measure and place a ribbon, so a modest fixed subdivision is enough.
     *
     * @param pathData the path as stored by {@code RemoteContext.loadPathData}, may be null
     * @param out      destination for x,y pairs
     * @return the number of points written
     */
    public static int flattenPath(float @Nullable [] pathData, float @NonNull [] out) {
        if (pathData == null) {
            return 0;
        }
        final int subdivisions = 8;
        int count = 0;
        float cx = 0f;
        float cy = 0f;
        int i = 0;
        while (i < pathData.length && count * 2 + 2 <= out.length) {
            float cmd = pathData[i];
            if (!Float.isNaN(cmd)) {
                i++;
                continue;
            }
            int op = Float.floatToRawIntBits(cmd) & 0xFF;
            switch (op) {
                case PathCommands.MOVE:
                    if (i + 2 < pathData.length) {
                        cx = pathData[i + 1];
                        cy = pathData[i + 2];
                        out[count * 2] = cx;
                        out[count * 2 + 1] = cy;
                        count++;
                    }
                    i += 3;
                    break;
                case PathCommands.LINE:
                    if (i + 2 < pathData.length) {
                        cx = pathData[i + 1];
                        cy = pathData[i + 2];
                        out[count * 2] = cx;
                        out[count * 2 + 1] = cy;
                        count++;
                    }
                    i += 3;
                    break;
                case PathCommands.QUADRATIC:
                    if (i + 4 < pathData.length) {
                        float x1 = pathData[i + 1];
                        float y1 = pathData[i + 2];
                        float x2 = pathData[i + 3];
                        float y2 = pathData[i + 4];
                        for (int s = 1; s <= subdivisions; s++) {
                            if (count * 2 + 2 > out.length) {
                                break;
                            }
                            float t = s / (float) subdivisions;
                            float mt = 1f - t;
                            out[count * 2] = mt * mt * cx + 2 * mt * t * x1 + t * t * x2;
                            out[count * 2 + 1] = mt * mt * cy + 2 * mt * t * y1 + t * t * y2;
                            count++;
                        }
                        cx = x2;
                        cy = y2;
                    }
                    i += 5;
                    break;
                case PathCommands.CUBIC:
                    if (i + 6 < pathData.length) {
                        float x1 = pathData[i + 1];
                        float y1 = pathData[i + 2];
                        float x2 = pathData[i + 3];
                        float y2 = pathData[i + 4];
                        float x3 = pathData[i + 5];
                        float y3 = pathData[i + 6];
                        for (int s = 1; s <= subdivisions; s++) {
                            if (count * 2 + 2 > out.length) {
                                break;
                            }
                            float t = s / (float) subdivisions;
                            float mt = 1f - t;
                            out[count * 2] =
                                    mt * mt * mt * cx
                                            + 3 * mt * mt * t * x1
                                            + 3 * mt * t * t * x2
                                            + t * t * t * x3;
                            out[count * 2 + 1] =
                                    mt * mt * mt * cy
                                            + 3 * mt * mt * t * y1
                                            + 3 * mt * t * t * y2
                                            + t * t * t * y3;
                            count++;
                        }
                        cx = x3;
                        cy = y3;
                    }
                    i += 7;
                    break;
                case PathCommands.CLOSE:
                    i += 1;
                    break;
                case PathCommands.DONE:
                    return count;
                default:
                    i += 1;
                    break;
            }
        }
        return count;
    }

    /** Keep the origin only: content rides the surface, staying upright and unscaled. */
    public static final int FLAG_ORIGIN = 0;

    /** Keep origin and rotation: content turns with the surface. */
    public static final int FLAG_ROTATION = 1;

    /** Keep origin, rotation and scale: content stretches with the surface. */
    public static final int FLAG_SCALE = 2;

    /** Keep the full 2x3, skew included: the faithful case. */
    public static final int FLAG_FULL = 3;

    private static final float DEGENERATE_EPSILON = 1e-6f;

    /**
     * Sample the surface at {@code (u, v)} from the expanded vertex array and build a 2x3 affine
     * matrix {@code [duX, duY, dvX, dvY, originX, originY]} according to {@code flags}.
     *
     * @param layout one of the {@code LAYOUT_*} constants
     * @param uCount samples along u
     * @param vCount samples along v
     * @param verts  the expanded x,y vertex array
     * @param u      the u parameter, 0..1
     * @param v      the v parameter, 0..1
     * @param flags  which parts of the local frame to keep ({@code FLAG_*})
     * @param out    a 6-element array receiving {@code [duX, duY, dvX, dvY, originX, originY]}
     * @return true if the mesh had enough vertices to compute a matrix
     */
    public static boolean computeMatrixFromMesh(
            int layout,
            int uCount,
            int vCount,
            float @NonNull [] verts,
            float u,
            float v,
            int flags,
            float @NonNull [] out) {
        if (verts.length < 2 || out.length < 6) {
            return false;
        }
        float[] frame = new float[6];
        sampleMeshFrame(layout, uCount, vCount, verts, u, v, frame);
        buildMatrix(frame, flags, out);
        return true;
    }

    /**
     * Sample the surface's local frame {@code [duX, duY, dvX, dvY, originX, originY]} at {@code (u,
     * v)} directly from the expanded mesh vertices.
     */
    public static void sampleMeshFrame(
            int layout,
            int uCount,
            int vCount,
            float @NonNull [] verts,
            float u,
            float v,
            float @NonNull [] out) {
        int uc = Math.max(2, uCount);
        int vc = Math.max(2, vCount);
        boolean wrap = wrapsU(layout);
        float du = 1f / (wrap ? uc : (uc - 1));
        float dv = 1f / (vc - 1);

        float[] centre = new float[2];
        float[] uPlus = new float[2];
        float[] uMinus = new float[2];
        float[] vPlus = new float[2];
        float[] vMinus = new float[2];

        sampleMeshPosition(layout, uCount, vCount, verts, u, v, centre);

        float uSpan;
        if (wrap) {
            sampleMeshPosition(layout, uCount, vCount, verts, u + du * 0.5f, v, uPlus);
            sampleMeshPosition(layout, uCount, vCount, verts, u - du * 0.5f, v, uMinus);
            uSpan = du;
        } else {
            float uHi = Math.min(1f, u + du * 0.5f);
            float uLo = Math.max(0f, u - du * 0.5f);
            sampleMeshPosition(layout, uCount, vCount, verts, uHi, v, uPlus);
            sampleMeshPosition(layout, uCount, vCount, verts, uLo, v, uMinus);
            uSpan = uHi - uLo;
            if (uSpan <= 0f) {
                uSpan = du;
            }
        }

        float vHi = Math.min(1f, v + dv * 0.5f);
        float vLo = Math.max(0f, v - dv * 0.5f);
        sampleMeshPosition(layout, uCount, vCount, verts, u, vHi, vPlus);
        sampleMeshPosition(layout, uCount, vCount, verts, u, vLo, vMinus);
        float vSpan = vHi - vLo;
        if (vSpan <= 0f) {
            vSpan = dv;
        }

        out[0] = (uPlus[0] - uMinus[0]) / uSpan;
        out[1] = (uPlus[1] - uMinus[1]) / uSpan;
        out[2] = (vPlus[0] - vMinus[0]) / vSpan;
        out[3] = (vPlus[1] - vMinus[1]) / vSpan;
        out[4] = centre[0];
        out[5] = centre[1];
    }

    private static void sampleMeshPosition(
            int layout,
            int uCount,
            int vCount,
            float @NonNull [] verts,
            float u,
            float v,
            float @NonNull [] out) {
        int vertexCount = verts.length / 2;
        if (vertexCount == 0) {
            out[0] = 0f;
            out[1] = 0f;
            return;
        }
        if (layout == LAYOUT_FAN && uCount >= 1 && vertexCount == uCount + 1) {
            float uNorm = ((u % 1f) + 1f) % 1f;
            float fu = uNorm * uCount;
            int i0 = ((int) Math.floor(fu)) % uCount;
            int i1 = (i0 + 1) % uCount;
            float tu = fu - (float) Math.floor(fu);
            float rimX = (1f - tu) * verts[(1 + i0) * 2] + tu * verts[(1 + i1) * 2];
            float rimY = (1f - tu) * verts[(1 + i0) * 2 + 1] + tu * verts[(1 + i1) * 2 + 1];
            float vc = Math.max(0f, Math.min(1f, v));
            out[0] = (1f - vc) * verts[0] + vc * rimX;
            out[1] = (1f - vc) * verts[1] + vc * rimY;
            return;
        }
        if (uCount >= 2 && vCount >= 2 && uCount * vCount == vertexCount) {
            int i0;
            int i1;
            float tu;
            if (wrapsU(layout)) {
                float uNorm = ((u % 1f) + 1f) % 1f;
                float fu = uNorm * uCount;
                i0 = ((int) Math.floor(fu)) % uCount;
                i1 = (i0 + 1) % uCount;
                tu = fu - (float) Math.floor(fu);
            } else {
                float fu = Math.max(0f, Math.min(1f, u)) * (uCount - 1);
                i0 = (int) Math.floor(fu);
                i1 = Math.min(uCount - 1, i0 + 1);
                tu = fu - i0;
            }
            float fv = Math.max(0f, Math.min(1f, v)) * (vCount - 1);
            int j0 = (int) Math.floor(fv);
            int j1 = Math.min(vCount - 1, j0 + 1);
            float tv = fv - j0;
            out[0] = bilinear(verts, uCount, i0, j0, i1, j1, tu, tv, 0);
            out[1] = bilinear(verts, uCount, i0, j0, i1, j1, tu, tv, 1);
            return;
        }
        int nearest =
                Math.max(
                        0,
                        Math.min(
                                vertexCount - 1,
                                Math.round(Math.max(0f, Math.min(1f, u)) * (vertexCount - 1))));
        out[0] = verts[nearest * 2];
        out[1] = verts[nearest * 2 + 1];
    }

    private static float bilinear(
            float[] verts,
            int uCount,
            int i0,
            int j0,
            int i1,
            int j1,
            float tu,
            float tv,
            int component) {
        float v00 = verts[(j0 * uCount + i0) * 2 + component];
        float v10 = verts[(j0 * uCount + i1) * 2 + component];
        float v01 = verts[(j1 * uCount + i0) * 2 + component];
        float v11 = verts[(j1 * uCount + i1) * 2 + component];
        float top = v00 + (v10 - v00) * tu;
        float bottom = v01 + (v11 - v01) * tu;
        return top + (bottom - top) * tv;
    }

    /**
     * Turn a sampled surface frame into a 2x3 affine, honouring the requested level of detail and
     * degrading gracefully where the patch is degenerate.
     *
     * @param frame duX, duY, dvX, dvY, originX, originY
     * @param flags how much of the frame to keep
     * @param out   a 6 element array receiving the affine as duX, duY, dvX, dvY, originX, originY
     */
    public static void buildMatrix(float @NonNull [] frame, int flags, float @NonNull [] out) {
        float duX = frame[0];
        float duY = frame[1];
        float dvX = frame[2];
        float dvY = frame[3];
        float originX = frame[4];
        float originY = frame[5];

        float duLength = (float) Math.hypot(duX, duY);
        float dvLength = (float) Math.hypot(dvX, dvY);
        float cross = duX * dvY - duY * dvX;

        int effective = flags;
        if (effective >= FLAG_FULL && Math.abs(cross) < DEGENERATE_EPSILON) {
            effective = FLAG_SCALE;
        }
        if (effective >= FLAG_SCALE
                && (duLength < DEGENERATE_EPSILON || dvLength < DEGENERATE_EPSILON)) {
            effective = FLAG_ROTATION;
        }
        if (effective >= FLAG_ROTATION && duLength < DEGENERATE_EPSILON) {
            effective = FLAG_ORIGIN;
        }

        switch (effective) {
            case FLAG_FULL:
                out[0] = duX;
                out[1] = duY;
                out[2] = dvX;
                out[3] = dvY;
                break;
            case FLAG_SCALE: {
                float ux = duX / duLength;
                float uy = duY / duLength;
                out[0] = ux * duLength;
                out[1] = uy * duLength;
                float sign = cross < 0 ? -1f : 1f;
                out[2] = -uy * dvLength * sign;
                out[3] = ux * dvLength * sign;
                break;
            }
            case FLAG_ROTATION: {
                float ux = duX / duLength;
                float uy = duY / duLength;
                float sign = cross < 0 ? -1f : 1f;
                out[0] = ux;
                out[1] = uy;
                out[2] = -uy * sign;
                out[3] = ux * sign;
                break;
            }
            case FLAG_ORIGIN:
            default:
                out[0] = 1f;
                out[1] = 0f;
                out[2] = 0f;
                out[3] = 1f;
                break;
        }
        out[4] = originX;
        out[5] = originY;
    }

    /** Path command opcodes, mirroring the encoding used by {@code PathData}. */
    private static final class PathCommands {
        static final int MOVE = 10;
        static final int LINE = 11;
        static final int QUADRATIC = 12;
        static final int CUBIC = 14;
        static final int CLOSE = 15;
        static final int DONE = 16;

        private PathCommands() {
        }
    }
}
