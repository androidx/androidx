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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT_ARRAY;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.operations.utilities.easing.MonotonicSpline;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Defines a 2D vertex mesh.
 *
 * <p>One operation covers every way of supplying vertices, discriminated by {@code type}, because
 * the literal and the parametric form differ only in how the data arrives - that is a field, not an
 * operation. The 3D side spends two opcodes on this distinction; the 2D side does not repeat that,
 * and {@code type} leaves room to add supply formats later without spending more opcode space.
 *
 * <p>The mesh is defined once and may be drawn repeatedly under different transforms by {@link
 * DrawMesh2D}, which is why definition and drawing are separate operations: collapsing them would
 * force the vertex data to be re-sent per draw.
 *
 * <p>Nothing here touches the 3D subsystem. There is no z, no normal, no camera, no light and no
 * depth buffer; a mesh is positioned by the ordinary 2D canvas matrix.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AddMesh2D extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.ADD_MESH_2D;
    private static final String CLASS_NAME = "AddMesh2D";

    /** RPN expressions over {@code (u, v)}; topology implicit, no index array. */
    public static final int TYPE_EXPRESSION = 0;

    /** Explicit indices and vertices, 32 bit. The interop path for tool generated geometry. */
    public static final int TYPE_VALUES = 1;

    /** As {@link #TYPE_VALUES}, with positions and uv as IEEE half floats. A wire format only. */
    public static final int TYPE_F16_VALUES = 2;

    /**
     * A ribbon along a path whose cross width is a monotonic spline through control points.
     *
     * <p>Always uses {@link Mesh2DGenerator#LAYOUT_PATH_STRIP}: the topology is exactly that of an
     * ordinary path strip and only the source of the width differs. Where {@link #TYPE_EXPRESSION}
     * spends expression tokens computing a width from {@code u}, this carries the widths
     * themselves, which is both cheaper and far easier to author for the common case of a stroke
     * that swells and tapers. Geometry only: uv is the identity mapping and there are no vertex
     * colours, so colour comes from the paint or from a textured {@code drawMesh2D}.
     */
    public static final int TYPE_PATH_SPLINE_STRIP = 3;

    /**
     * As {@link #TYPE_PATH_SPLINE_STRIP}, but the ribbon ends in a semicircle instead of a squared
     * off edge - the mesh equivalent of a round stroke cap.
     *
     * <p>Identical on the wire to the flat variant, and identical in topology: still {@link
     * Mesh2DGenerator#LAYOUT_PATH_STRIP}, still two vertices across, still one quad per column. The
     * caps are simply extra columns at each end whose {@code u} sweeps a quarter turn around the
     * endpoint rather than advancing along the path, so the strip's own quads sweep out the half
     * disc. Because the caps are added to the column budget rather than taken out of it, a rounded
     * strip follows its path at the same resolution as a flat one with the same {@code segments}.
     *
     * <p>The cap radius is half the ribbon's width at that end, so a profile that tapers to zero
     * ends in a point and one that ends wide ends in a correspondingly large dome. The number of
     * columns in each cap is carried in the header's {@code flags} field; see {@link
     * Mesh2DGenerator#roundCapSegments}.
     */
    public static final int TYPE_SPLINE_ROUND_STRIP = 4;

    /** Index into the expression group array: x position. */
    private static final int EXP_X = 0;

    /** Index into the expression group array: y position. */
    private static final int EXP_Y = 1;

    /** Index into the expression group array: texture u. */
    private static final int EXP_TEX_U = 2;

    /** Index into the expression group array: texture v. */
    private static final int EXP_TEX_V = 3;

    /** Index into the expression group array: colour alpha, 0..1. */
    private static final int EXP_COLOR_A = 4;

    /** Index into the expression group array: colour red, 0..1. */
    private static final int EXP_COLOR_R = 5;

    /** Index into the expression group array: colour green, 0..1. */
    private static final int EXP_COLOR_G = 6;

    /** Index into the expression group array: colour blue, 0..1. */
    private static final int EXP_COLOR_B = 7;

    /**
     * Index into the expression group array: the cross width of a strip, in the path's own units.
     *
     * <p>Only {@link Mesh2DGenerator#LAYOUT_PATH_STRIP} reads it. Because it is an expression over
     * {@code (u, v)} rather than a constant, the ribbon's width can vary along its length - which
     * is the whole reason a path strip is more than a thick stroke. Absent means a width of 1,
     * leaving the strip one unit across.
     */
    private static final int EXP_WIDTH = 8;

    /** The number of expression groups carried by {@link #TYPE_EXPRESSION}. */
    public static final int EXPRESSION_GROUPS = 9;

    /** A quarter turn, the angle a round end cap sweeps from its tip to the body of the strip. */
    private static final float HALF_PI = (float) (Math.PI * 0.5);

    private final int mMeshId;
    private final int mType;
    private final int mLayout;
    private final int mUCount;
    private final int mVCount;
    private final int mFlags;
    private final int mAux;

    // TYPE_EXPRESSION payload: one RPN array per group, empty when the group is absent.
    private final float[][] mExpressions;
    private final float[][] mOutExpressions;

    // TYPE_VALUES / TYPE_F16_VALUES payload.
    private final int[] mSrcIndices;
    private final float[] mSrcVerts;
    private final float[] mOutSrcVerts;
    private final float[] mSrcUv;
    private final int[] mSrcColors;

    // Spline strip payload: the width control points and, optionally, where they sit.
    private final float[] mWidths;
    private final float[] mOutWidths;
    private final float[] mWidthPositions;
    private final float[] mOutWidthPositions;

    /** The fit through {@link #mOutWidths}, rebuilt lazily whenever a width input changes. */
    private @Nullable MonotonicSpline mWidthSpline;

    private boolean mWidthSplineValid;

    // Expanded geometry, in the exact layout drawVertices wants.
    private float[] mVerts = new float[0];
    private float[] mUv = new float[0];
    private int[] mColors = new int[0];
    private int[] mIndices = new int[0];
    private boolean mMeshChanged = true;

    private final AnimatedFloatExpression mExpressionEvaluator = new AnimatedFloatExpression();
    private final float[] mScratchPosition = new float[2];
    private final float[] mScratchPathSample = new float[4];
    private float[] mScratchPolyline = new float[0];

    @SuppressWarnings("UnknownNullness") // Annotations on a primitive array are compile error.
    public AddMesh2D(
            int meshId,
            int type,
            int layout,
            int uCount,
            int vCount,
            int flags,
            int aux,
            float @Nullable [] @Nullable [] expressions,
            int @Nullable [] indices,
            float @Nullable [] verts,
            float @Nullable [] uv,
            int @Nullable [] colors) {
        this(
                meshId,
                type,
                layout,
                uCount,
                vCount,
                flags,
                aux,
                expressions,
                indices,
                verts,
                uv,
                colors,
                null,
                null);
    }

    @SuppressWarnings("UnknownNullness") // Annotations on a primitive array are compile error.
    public AddMesh2D(
            int meshId,
            int type,
            int layout,
            int uCount,
            int vCount,
            int flags,
            int aux,
            float @Nullable [] @Nullable [] expressions,
            int @Nullable [] indices,
            float @Nullable [] verts,
            float @Nullable [] uv,
            int @Nullable [] colors,
            float @Nullable [] widths,
            float @Nullable [] widthPositions) {
        mMeshId = meshId;
        mType = type;
        mLayout = layout;
        mUCount = uCount;
        mVCount = vCount;
        mFlags = flags;
        mAux = aux;

        mExpressions = new float[EXPRESSION_GROUPS][];
        mOutExpressions = new float[EXPRESSION_GROUPS][];
        for (int i = 0; i < EXPRESSION_GROUPS; i++) {
            float[] group =
                    (expressions != null && i < expressions.length && expressions[i] != null)
                            ? expressions[i]
                            : new float[0];
            mExpressions[i] = group;
            mOutExpressions[i] = new float[group.length];
            System.arraycopy(group, 0, mOutExpressions[i], 0, group.length);
        }

        mSrcIndices = indices != null ? indices : new int[0];
        mSrcVerts = verts != null ? verts : new float[0];
        mOutSrcVerts = new float[mSrcVerts.length];
        System.arraycopy(mSrcVerts, 0, mOutSrcVerts, 0, mSrcVerts.length);
        mSrcUv = uv != null ? uv : new float[0];
        mSrcColors = colors != null ? colors : new int[0];

        mWidths = widths != null ? widths : new float[0];
        mOutWidths = new float[mWidths.length];
        System.arraycopy(mWidths, 0, mOutWidths, 0, mWidths.length);
        mWidthPositions = widthPositions != null ? widthPositions : new float[0];
        mOutWidthPositions = new float[mWidthPositions.length];
        System.arraycopy(mWidthPositions, 0, mOutWidthPositions, 0, mWidthPositions.length);
    }

    /**
     * The id this mesh is stored under.
     *
     * @return the mesh id
     */
    public int getMeshId() {
        return mMeshId;
    }

    /**
     * The expanded vertex positions, x and y interleaved.
     *
     * @return the vertex array, valid after the mesh has been expanded
     */
    public float @NonNull [] getVerts() {
        return mVerts;
    }

    /**
     * The expanded triangle list.
     *
     * @return the index array, valid after the mesh has been expanded
     */
    public int @NonNull [] getIndices() {
        return mIndices;
    }

    /**
     * The expanded per-vertex colours, packed ARGB, empty when the mesh has none.
     *
     * @return the colour array, valid after the mesh has been expanded
     */
    public int @NonNull [] getColors() {
        return mColors;
    }

    /**
     * The expanded texture coordinates, u and v interleaved, empty when the mesh has none.
     *
     * @return the uv array, valid after the mesh has been expanded
     */
    public float @NonNull [] getUv() {
        return mUv;
    }

    private static boolean isResolvableVariable(float v) {
        return Float.isNaN(v)
                && !AnimatedFloatExpression.isMathOperator(v)
                && !NanMap.isDataVariable(v)
                && !NanMap.isVar1(v);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int g = 0; g < EXPRESSION_GROUPS; g++) {
            float[] src = mExpressions[g];
            float[] out = mOutExpressions[g];
            for (int i = 0; i < src.length; i++) {
                float v = src[i];
                out[i] = isResolvableVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
            }
        }
        for (int i = 0; i < mSrcVerts.length; i++) {
            float v = mSrcVerts[i];
            mOutSrcVerts[i] = isResolvableVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
        for (int i = 0; i < mWidths.length; i++) {
            float v = mWidths[i];
            mOutWidths[i] = isResolvableVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
        for (int i = 0; i < mWidthPositions.length; i++) {
            float v = mWidthPositions[i];
            mOutWidthPositions[i] =
                    isResolvableVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
        // The fit is over the resolved values, so any of them moving retires it.
        mWidthSplineValid = false;
        mMeshChanged = true;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (int g = 0; g < EXPRESSION_GROUPS; g++) {
            for (float v : mExpressions[g]) {
                if (Float.isNaN(v)
                        && !AnimatedFloatExpression.isMathOperator(v)
                        && !NanMap.isDataVariable(v)) {
                    context.listensTo(Utils.idFromNan(v), this);
                }
            }
        }
        for (float v : mSrcVerts) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        for (float v : mWidths) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        for (float v : mWidthPositions) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    /** Whether {@code type} takes its width from control points rather than from an expression. */
    private static boolean isSplineStrip(int type) {
        return type == TYPE_PATH_SPLINE_STRIP || type == TYPE_SPLINE_ROUND_STRIP;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        if (isSplineStrip(mType)) {
            // The caps sit outside the body, so subtract them back off to recover what the author
            // asked for; applySplineStrip re-derives the same count from it.
            int segments = Math.max(1, mUCount - 1 - 2 * roundCapColumns());
            applySplineStrip(buffer, mMeshId, mType, segments, mAux, mWidths, mWidthPositions);
            return;
        }
        apply(
                buffer,
                mMeshId,
                mType,
                mLayout,
                mUCount,
                mVCount,
                mFlags,
                mAux,
                mExpressions,
                mSrcIndices,
                mSrcVerts,
                mSrcUv,
                mSrcColors);
    }

    /**
     * Expand the definition into the flat arrays a rasteriser wants.
     *
     * <p>Kept separate from {@link #paint} so it can be driven directly by tests.
     *
     * @param context the context used to resolve path data and array collections
     */
    public void expand(@NonNull RemoteContext context) {
        if (mType == TYPE_EXPRESSION || isSplineStrip(mType)) {
            expandParametric(context);
        } else {
            expandLiteral();
        }
        mMeshChanged = false;
    }

    private void expandLiteral() {
        int vertexCount = mOutSrcVerts.length / 2;
        if (mVerts.length != mOutSrcVerts.length) {
            mVerts = new float[mOutSrcVerts.length];
        }
        System.arraycopy(mOutSrcVerts, 0, mVerts, 0, mOutSrcVerts.length);

        if (mSrcUv.length == vertexCount * 2) {
            if (mUv.length != mSrcUv.length) {
                mUv = new float[mSrcUv.length];
            }
            System.arraycopy(mSrcUv, 0, mUv, 0, mSrcUv.length);
        } else {
            mUv = new float[0];
        }

        if (mSrcColors.length == vertexCount) {
            if (mColors.length != mSrcColors.length) {
                mColors = new int[mSrcColors.length];
            }
            System.arraycopy(mSrcColors, 0, mColors, 0, mSrcColors.length);
        } else {
            mColors = new int[0];
        }

        if (mIndices.length != mSrcIndices.length) {
            mIndices = new int[mSrcIndices.length];
        }
        System.arraycopy(mSrcIndices, 0, mIndices, 0, mSrcIndices.length);
    }

    private void expandParametric(@NonNull RemoteContext context) {
        int uCount = Math.max(1, mUCount);
        int vCount = Math.max(1, mVCount);
        int vertexCount = Mesh2DGenerator.vertexCount(mLayout, uCount, vCount);
        int indexCount = Mesh2DGenerator.indexCount(mLayout, uCount, vCount);

        if (mVerts.length != vertexCount * 2) {
            mVerts = new float[vertexCount * 2];
        }
        if (mIndices.length != indexCount) {
            mIndices = new int[indexCount];
        }

        boolean hasTex =
                mOutExpressions[EXP_TEX_U].length > 0 || mOutExpressions[EXP_TEX_V].length > 0;
        // uv is meaningful for every parametric mesh - the identity mapping is the common case -
        // so it is always produced; the backend simply ignores it when nothing is textured.
        if (mUv.length != vertexCount * 2) {
            mUv = new float[vertexCount * 2];
        }

        boolean hasColor =
                mOutExpressions[EXP_COLOR_A].length > 0
                        || mOutExpressions[EXP_COLOR_R].length > 0
                        || mOutExpressions[EXP_COLOR_G].length > 0
                        || mOutExpressions[EXP_COLOR_B].length > 0;
        if (hasColor) {
            if (mColors.length != vertexCount) {
                mColors = new int[vertexCount];
            }
        } else {
            mColors = new int[0];
        }

        int polylinePoints = 0;
        if (mLayout == Mesh2DGenerator.LAYOUT_PATH_STRIP) {
            polylinePoints = preparePolyline(context);
        }

        boolean fan = mLayout == Mesh2DGenerator.LAYOUT_FAN;
        for (int index = 0; index < vertexCount; index++) {
            float u;
            float v;
            if (fan) {
                if (index == 0) {
                    u = 0f;
                    v = 0f;
                } else {
                    u = Mesh2DGenerator.domainU(mLayout, index - 1, uCount);
                    v = 1f;
                }
            } else {
                int i = index % uCount;
                int j = index / uCount;
                u = Mesh2DGenerator.domainU(mLayout, i, uCount);
                v = Mesh2DGenerator.domainV(j, vCount);
            }

            if (mLayout == Mesh2DGenerator.LAYOUT_PATH_STRIP && polylinePoints > 0) {
                positionOnPath(context, u, v, polylinePoints);
            } else {
                Mesh2DGenerator.defaultPosition(mLayout, u, v, mScratchPosition);
            }

            float x = evaluate(context, EXP_X, u, v, mScratchPosition[0]);
            float y = evaluate(context, EXP_Y, u, v, mScratchPosition[1]);
            mVerts[index * 2] = x;
            mVerts[index * 2 + 1] = y;

            if (hasTex) {
                mUv[index * 2] = evaluate(context, EXP_TEX_U, u, v, u);
                mUv[index * 2 + 1] = evaluate(context, EXP_TEX_V, u, v, v);
            } else {
                mUv[index * 2] = u;
                mUv[index * 2 + 1] = v;
            }

            if (hasColor) {
                float a = evaluate(context, EXP_COLOR_A, u, v, 1f);
                float r = evaluate(context, EXP_COLOR_R, u, v, 1f);
                float g = evaluate(context, EXP_COLOR_G, u, v, 1f);
                float b = evaluate(context, EXP_COLOR_B, u, v, 1f);
                mColors[index] = packColor(a, r, g, b);
            }
        }

        Mesh2DGenerator.generateIndices(mLayout, uCount, vCount, mIndices);
    }

    private int preparePolyline(@NonNull RemoteContext context) {
        float[] pathData = context.getPathData(mAux);
        if (pathData == null) {
            return 0;
        }
        int capacity = Math.max(64, pathData.length * 8);
        if (mScratchPolyline.length < capacity * 2) {
            mScratchPolyline = new float[capacity * 2];
        }
        return Mesh2DGenerator.flattenPath(pathData, mScratchPolyline);
    }

    /**
     * How many columns at each end of the strip are given over to a round cap.
     *
     * <p>Zero for every type but {@link #TYPE_SPLINE_ROUND_STRIP}. The count is clamped against
     * {@link #mUCount} rather than trusted, because it arrives from the wire in a field that older
     * documents left as a reserved zero and that a corrupt one could set to anything; at least one
     * column must be left for the body or the strip would be nothing but caps.
     */
    private int roundCapColumns() {
        if (mType != TYPE_SPLINE_ROUND_STRIP || mFlags < 1) {
            return 0;
        }
        int columns = mUCount - 1;
        if (columns < 3) {
            return 0;
        }
        return Math.min(mFlags, (columns - 1) / 2);
    }

    private void positionOnPath(@NonNull RemoteContext context, float u, float v, int points) {
        float fraction = u;
        int capColumns = roundCapColumns();
        if (capColumns > 0) {
            // The caps own the first and last capColumns columns of the u domain; the body is
            // squeezed into what is left, so u is no longer the path fraction and has to be
            // rescaled. At the joins the two agree exactly, which is what keeps the seam invisible.
            float capSpan = capColumns / (float) (mUCount - 1);
            if (u <= capSpan) {
                capPosition(context, points, 0f, u / capSpan, v, -1f);
                return;
            }
            if (u >= 1f - capSpan) {
                capPosition(context, points, 1f, (1f - u) / capSpan, v, 1f);
                return;
            }
            fraction = (u - capSpan) / (1f - 2f * capSpan);
        }
        Mesh2DGenerator.samplePolyline(mScratchPolyline, points, fraction, mScratchPathSample);
        float halfWidth = strokeWidthAt(context, fraction, v) * 0.5f;
        float offset = (v - 0.5f) * 2f * halfWidth;
        // the normal is the tangent turned a quarter turn
        float nx = -mScratchPathSample[3];
        float ny = mScratchPathSample[2];
        mScratchPosition[0] = mScratchPathSample[0] + nx * offset;
        mScratchPosition[1] = mScratchPathSample[1] + ny * offset;
    }

    /**
     * Place a vertex on one of the two round caps.
     *
     * @param end 0 at the start of the path, 1 at its end
     * @param sweep 0 at the tip of the cap, 1 where it meets the body
     * @param outward 1 at the end of the path, -1 at the start, being the way the cap bulges
     */
    private void capPosition(
            @NonNull RemoteContext context,
            int points,
            float end,
            float sweep,
            float v,
            float outward) {
        Mesh2DGenerator.samplePolyline(mScratchPolyline, points, end, mScratchPathSample);
        float halfWidth = strokeWidthAt(context, end, v) * 0.5f;
        Mesh2DGenerator.roundCapPoint(
                mScratchPathSample, halfWidth, sweep * HALF_PI, v, outward, mScratchPosition);
    }

    /**
     * The full cross width of the ribbon at {@code u}, in the path's own units.
     *
     * <p>Two sources, chosen by the type. The spline strips interpolate the width control points,
     * and because those may be variables the fit is cached and rebuilt only when {@link
     * #updateVariables} retires it. Everything else evaluates the width expression, which defaults
     * to 1 and so leaves the strip one unit across.
     */
    private float strokeWidthAt(@NonNull RemoteContext context, float u, float v) {
        if (!isSplineStrip(mType)) {
            return evaluate(context, EXP_WIDTH, u, v, 1f);
        }
        if (!mWidthSplineValid) {
            mWidthSpline = Mesh2DGenerator.widthSpline(mOutWidths, mOutWidthPositions);
            mWidthSplineValid = true;
        }
        return Mesh2DGenerator.widthAt(mWidthSpline, mOutWidths, u);
    }

    private float evaluate(
            @NonNull RemoteContext context, int group, float u, float v, float fallback) {
        float[] exp = mOutExpressions[group];
        if (exp.length == 0) {
            return fallback;
        }
        if (exp.length == 1 && !Float.isNaN(exp[0])) {
            return exp[0];
        }
        CollectionsAccess ca = context.getCollectionsAccess();
        if (ca == null) {
            return mExpressionEvaluator.eval(exp, exp.length, u, v);
        } else {
            return mExpressionEvaluator.eval(ca, exp, exp.length, u, v);
        }
    }

    private static int packColor(float a, float r, float g, float b) {
        int ai = clamp255(a);
        int ri = clamp255(r);
        int gi = clamp255(g);
        int bi = clamp255(b);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static int clamp255(float channel) {
        if (Float.isNaN(channel)) {
            return 0;
        }
        int value = (int) (channel * 255f + 0.5f);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        if (mMeshChanged || mVerts.length == 0) {
            expand(remoteContext);
        }
        context.setMesh(mMeshId, mLayout, mUCount, mVCount, mVerts, mUv, mColors, mIndices);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME
                + " ["
                + mMeshId
                + "] type="
                + mType
                + " layout="
                + mLayout
                + " u="
                + mUCount
                + " v="
                + mVCount
                + " flags="
                + mFlags
                + " aux="
                + mAux;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Write a 2D mesh definition to the buffer.
     *
     * @param buffer the buffer to add to
     * @param meshId the id the mesh is stored under
     * @param type how the vertex data is supplied
     * @param layout the domain topology
     * @param uCount grid resolution along u
     * @param vCount grid resolution along v
     * @param flags reserved
     * @param aux layout dependent, e.g. a path id for PATH_STRIP
     * @param expressions the RPN expression groups, for TYPE_EXPRESSION
     * @param indices the triangle list, for the literal types
     * @param verts x,y pairs, for the literal types
     * @param uv u,v pairs, for the literal types
     * @param colors packed ARGB per vertex, for the literal types
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int meshId,
            int type,
            int layout,
            int uCount,
            int vCount,
            int flags,
            int aux,
            float @Nullable [] @Nullable [] expressions,
            int @Nullable [] indices,
            float @Nullable [] verts,
            float @Nullable [] uv,
            int @Nullable [] colors) {
        validate(type, uCount, vCount, indices, verts);

        buffer.start(OP_CODE);
        buffer.writeInt(meshId);
        buffer.writeInt(type);
        buffer.writeInt(layout);
        buffer.writeInt(uCount);
        buffer.writeInt(vCount);
        buffer.writeInt(flags);
        buffer.writeInt(aux);

        if (type == TYPE_EXPRESSION) {
            for (int g = 0; g < EXPRESSION_GROUPS; g++) {
                float[] group =
                        (expressions != null && g < expressions.length && expressions[g] != null)
                                ? expressions[g]
                                : null;
                if (group == null) {
                    // an absent optional channel is a zero length, not a missing field
                    buffer.writeInt(0);
                } else {
                    if (group.length > Limits.MAX_EXPRESSION_SIZE) {
                        throw new RuntimeException("Mesh2D expression too long");
                    }
                    buffer.writeInt(group.length);
                    for (float datum : group) {
                        buffer.writeFloat(datum);
                    }
                }
            }
            return;
        }

        int[] safeIndices = indices != null ? indices : new int[0];
        float[] safeVerts = verts != null ? verts : new float[0];
        float[] safeUv = uv != null ? uv : new float[0];
        int[] safeColors = colors != null ? colors : new int[0];

        buffer.writeInt(safeIndices.length);
        for (int index : safeIndices) {
            // 16 bit indices are settled by the platform: Android's drawVertices takes short[]
            // and SkVertices takes uint16_t
            buffer.writeShort(index & 0xFFFF);
        }

        buffer.writeInt(safeVerts.length);
        buffer.writeInt(safeUv.length);
        buffer.writeInt(safeColors.length);

        if (type == TYPE_F16_VALUES) {
            for (float value : safeVerts) {
                buffer.writeShort(Mesh2DGenerator.floatToHalf(value));
            }
            for (float value : safeUv) {
                buffer.writeShort(Mesh2DGenerator.floatToHalf(value));
            }
        } else {
            for (float value : safeVerts) {
                buffer.writeFloat(value);
            }
            for (float value : safeUv) {
                buffer.writeFloat(value);
            }
        }
        for (int color : safeColors) {
            buffer.writeInt(color);
        }
    }

    /**
     * Write a path strip whose cross width is a spline through control points.
     *
     * <p>The header is derived here rather than taken from the caller: the layout is always {@link
     * Mesh2DGenerator#LAYOUT_PATH_STRIP} and the strip is two vertices across, so the only free
     * choices are how finely the path is sampled and the widths themselves.
     *
     * <p>{@code widths} and {@code positions} may hold NaN variable ids, so the profile can be
     * animated; the fit is rebuilt whenever one of them changes.
     *
     * @param buffer the buffer to add to
     * @param meshId the id the mesh is stored under
     * @param segments roughly how many quads to divide the path into; at least 1
     * @param pathId the path to follow
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path, 0..1, empty for evenly spaced
     */
    public static void applyPathSplineStrip(
            @NonNull WireBuffer buffer,
            int meshId,
            int segments,
            int pathId,
            float @Nullable [] widths,
            float @Nullable [] positions) {
        applySplineStrip(buffer, meshId, TYPE_PATH_SPLINE_STRIP, segments, pathId, widths,
                positions);
    }

    /**
     * Write a spline width path strip that ends in a semicircle at each end.
     *
     * <p>Exactly {@link #applyPathSplineStrip} but for the type and the cap columns it adds; see
     * {@link #TYPE_SPLINE_ROUND_STRIP}. {@code segments} still describes the body alone, so a
     * rounded strip and a flat one with the same argument follow the path identically.
     *
     * @param buffer the buffer to add to
     * @param meshId the id the mesh is stored under
     * @param segments roughly how many quads to divide the path into, excluding the caps
     * @param pathId the path to follow
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path, 0..1, empty for evenly spaced
     */
    public static void applySplineRoundStrip(
            @NonNull WireBuffer buffer,
            int meshId,
            int segments,
            int pathId,
            float @Nullable [] widths,
            float @Nullable [] positions) {
        applySplineStrip(buffer, meshId, TYPE_SPLINE_ROUND_STRIP, segments, pathId, widths,
                positions);
    }

    private static void applySplineStrip(
            @NonNull WireBuffer buffer,
            int meshId,
            int type,
            int segments,
            int pathId,
            float @Nullable [] widths,
            float @Nullable [] positions) {
        float[] safeWidths = widths != null ? widths : new float[0];
        float[] safePositions = positions != null ? positions : new float[0];
        int capSegments =
                type == TYPE_SPLINE_ROUND_STRIP ? Mesh2DGenerator.roundCapSegments(segments) : 0;
        validateSplineStrip(segments, capSegments, safeWidths, safePositions);

        buffer.start(OP_CODE);
        buffer.writeInt(meshId);
        buffer.writeInt(type);
        buffer.writeInt(Mesh2DGenerator.LAYOUT_PATH_STRIP);
        // segments quads need segments + 1 rings of vertices, two across, plus a cap at each end.
        buffer.writeInt(segments + 1 + 2 * capSegments);
        buffer.writeInt(2);
        // flags carries the cap width so the reader can tell body columns from cap columns.
        buffer.writeInt(capSegments);
        buffer.writeInt(pathId);

        buffer.writeInt(safeWidths.length);
        for (float width : safeWidths) {
            buffer.writeFloat(width);
        }
        buffer.writeInt(safePositions.length);
        for (float position : safePositions) {
            buffer.writeFloat(position);
        }
    }

    private static void validateSplineStrip(
            int segments,
            int capSegments,
            float @NonNull [] widths,
            float @NonNull [] positions) {
        if (segments < 1) {
            throw new RuntimeException("Mesh2D path strip needs at least 1 segment");
        }
        if (widths.length < 1) {
            throw new RuntimeException("Mesh2D path strip needs at least 1 width");
        }
        if (widths.length > Limits.MAX_MESH_2D_WIDTH_SAMPLES) {
            throw new RuntimeException(
                    "Mesh2D width sample count "
                            + widths.length
                            + " exceeds MAX_MESH_2D_WIDTH_SAMPLES ("
                            + Limits.MAX_MESH_2D_WIDTH_SAMPLES
                            + ")");
        }
        if (positions.length != 0 && positions.length != widths.length) {
            throw new RuntimeException(
                    "Mesh2D width positions ("
                            + positions.length
                            + ") must match widths ("
                            + widths.length
                            + ")");
        }
        long grid = (long) (segments + 1 + 2 * capSegments) * 2L;
        if (grid > Limits.MAX_MESH_2D_GRID) {
            throw new RuntimeException(
                    "Mesh2D path strip "
                            + segments
                            + " segments exceeds MAX_MESH_2D_GRID ("
                            + Limits.MAX_MESH_2D_GRID
                            + ")");
        }
        // A variable position is only known at playback, so only literals can be ordered here.
        float previous = Float.NEGATIVE_INFINITY;
        for (float position : positions) {
            if (Float.isNaN(position)) {
                previous = Float.NEGATIVE_INFINITY;
                continue;
            }
            if (previous != Float.NEGATIVE_INFINITY && position <= previous) {
                throw new RuntimeException("Mesh2D width positions must be increasing");
            }
            previous = position;
        }
    }

    private static void validate(
            int type, int uCount, int vCount, int @Nullable [] indices, float @Nullable [] verts) {
        if (type == TYPE_EXPRESSION) {
            long grid = (long) uCount * (long) vCount;
            if (grid > Limits.MAX_MESH_2D_GRID) {
                throw new RuntimeException(
                        "Mesh2D grid "
                                + uCount
                                + "x"
                                + vCount
                                + " exceeds MAX_MESH_2D_GRID ("
                                + Limits.MAX_MESH_2D_GRID
                                + ")");
            }
            return;
        }
        int vertexCount = verts != null ? verts.length / 2 : 0;
        if (vertexCount > Limits.MAX_MESH_2D_VERTICES) {
            throw new RuntimeException(
                    "Mesh2D vertex count "
                            + vertexCount
                            + " exceeds MAX_MESH_2D_VERTICES ("
                            + Limits.MAX_MESH_2D_VERTICES
                            + ")");
        }
        int indexCount = indices != null ? indices.length : 0;
        if (indexCount > Limits.MAX_MESH_2D_INDICES) {
            throw new RuntimeException(
                    "Mesh2D index count "
                            + indexCount
                            + " exceeds MAX_MESH_2D_INDICES ("
                            + Limits.MAX_MESH_2D_INDICES
                            + ")");
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int meshId = buffer.readId();
        int type = buffer.readInt();
        int layout = buffer.readInt();
        int uCount = buffer.readInt();
        int vCount = buffer.readInt();
        int flags = buffer.readInt();
        int aux = buffer.readInt();

        float[][] expressions = null;
        int[] indices = null;
        float[] verts = null;
        float[] uv = null;
        int[] colors = null;
        float[] widths = null;
        float[] widthPositions = null;

        if (isSplineStrip(type)) {
            long grid = (long) uCount * (long) vCount;
            if (grid > Limits.MAX_MESH_2D_GRID) {
                throw new RuntimeException(
                        "Mesh2D grid exceeds MAX_MESH_2D_GRID (" + Limits.MAX_MESH_2D_GRID + ")");
            }
            int widthCount = buffer.readInt();
            if (widthCount > Limits.MAX_MESH_2D_WIDTH_SAMPLES) {
                throw new RuntimeException(
                        "Mesh2D width sample count exceeds MAX_MESH_2D_WIDTH_SAMPLES ("
                                + Limits.MAX_MESH_2D_WIDTH_SAMPLES
                                + ")");
            }
            widths = new float[widthCount];
            for (int i = 0; i < widthCount; i++) {
                widths[i] = buffer.readNanId();
            }
            int positionCount = buffer.readInt();
            if (positionCount > Limits.MAX_MESH_2D_WIDTH_SAMPLES) {
                throw new RuntimeException(
                        "Mesh2D width position count exceeds MAX_MESH_2D_WIDTH_SAMPLES ("
                                + Limits.MAX_MESH_2D_WIDTH_SAMPLES
                                + ")");
            }
            widthPositions = new float[positionCount];
            for (int i = 0; i < positionCount; i++) {
                widthPositions[i] = buffer.readNanId();
            }
        } else if (type == TYPE_EXPRESSION) {
            long grid = (long) uCount * (long) vCount;
            if (grid > Limits.MAX_MESH_2D_GRID) {
                throw new RuntimeException(
                        "Mesh2D grid exceeds MAX_MESH_2D_GRID (" + Limits.MAX_MESH_2D_GRID + ")");
            }
            expressions = new float[EXPRESSION_GROUPS][];
            for (int g = 0; g < EXPRESSION_GROUPS; g++) {
                int len = buffer.readInt();
                if (len > Limits.MAX_EXPRESSION_SIZE) {
                    throw new RuntimeException("Mesh2D expression too long");
                }
                float[] group = new float[len];
                for (int i = 0; i < len; i++) {
                    group[i] = buffer.readNanId();
                }
                expressions[g] = group;
            }
        } else {
            int indexCount = buffer.readInt();
            if (indexCount > Limits.MAX_MESH_2D_INDICES) {
                throw new RuntimeException(
                        "Mesh2D index count exceeds MAX_MESH_2D_INDICES ("
                                + Limits.MAX_MESH_2D_INDICES
                                + ")");
            }
            indices = new int[indexCount];
            for (int i = 0; i < indexCount; i++) {
                indices[i] = buffer.readShort() & 0xFFFF;
            }

            int vertsLen = buffer.readInt();
            int uvLen = buffer.readInt();
            int colorsLen = buffer.readInt();
            if (vertsLen / 2 > Limits.MAX_MESH_2D_VERTICES) {
                throw new RuntimeException(
                        "Mesh2D vertex count exceeds MAX_MESH_2D_VERTICES ("
                                + Limits.MAX_MESH_2D_VERTICES
                                + ")");
            }

            verts = new float[vertsLen];
            uv = new float[uvLen];
            colors = new int[colorsLen];

            if (type == TYPE_F16_VALUES) {
                // widened here, on read, so nothing downstream knows the document used halves
                for (int i = 0; i < vertsLen; i++) {
                    verts[i] = Mesh2DGenerator.halfToFloat(buffer.readShort() & 0xFFFF);
                }
                for (int i = 0; i < uvLen; i++) {
                    uv[i] = Mesh2DGenerator.halfToFloat(buffer.readShort() & 0xFFFF);
                }
            } else {
                for (int i = 0; i < vertsLen; i++) {
                    verts[i] = buffer.readNanId();
                }
                for (int i = 0; i < uvLen; i++) {
                    uv[i] = buffer.readFloat();
                }
            }
            for (int i = 0; i < colorsLen; i++) {
                colors[i] = buffer.readInt();
            }
        }

        operations.add(
                new AddMesh2D(
                        meshId,
                        type,
                        layout,
                        uCount,
                        vCount,
                        flags,
                        aux,
                        expressions,
                        indices,
                        verts,
                        uv,
                        colors,
                        widths,
                        widthPositions));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Canvas Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .description(
                        "Define a 2D vertex mesh. Independent of the 3D subsystem: no z, no"
                                + " normals, no camera, no lights, no depth buffer. Positioned by"
                                + " the ordinary 2D canvas matrix.")
                .field(INT, "meshId", "The id the mesh is stored under")
                .field(INT, "type", "How the vertex data is supplied")
                .possibleValues("TYPE_EXPRESSION", TYPE_EXPRESSION)
                .possibleValues("TYPE_VALUES", TYPE_VALUES)
                .possibleValues("TYPE_F16_VALUES", TYPE_F16_VALUES)
                .possibleValues("TYPE_PATH_SPLINE_STRIP", TYPE_PATH_SPLINE_STRIP)
                .possibleValues("TYPE_SPLINE_ROUND_STRIP", TYPE_SPLINE_ROUND_STRIP)
                .field(INT, "layout", "The domain topology")
                .possibleValues("LAYOUT_GRID", Mesh2DGenerator.LAYOUT_GRID)
                .possibleValues("LAYOUT_POLAR", Mesh2DGenerator.LAYOUT_POLAR)
                .possibleValues("LAYOUT_RING", Mesh2DGenerator.LAYOUT_RING)
                .possibleValues("LAYOUT_STRIP", Mesh2DGenerator.LAYOUT_STRIP)
                .possibleValues("LAYOUT_FAN", Mesh2DGenerator.LAYOUT_FAN)
                .possibleValues("LAYOUT_PATH_STRIP", Mesh2DGenerator.LAYOUT_PATH_STRIP)
                .field(INT, "uCount", "Grid resolution along u")
                .field(INT, "vCount", "Grid resolution along v")
                .field(INT, "flags", "Cap columns for TYPE_SPLINE_ROUND_STRIP, otherwise reserved")
                .field(INT, "aux", "Layout dependent, e.g. a path id for PATH_STRIP")
                .field(FLOAT_ARRAY, "expressions", "Eight RPN groups: x, y, texU, texV, a, r, g, b")
                .field(INT_ARRAY, "indices", "Triangle list, 16 bit, for the literal types")
                .field(FLOAT_ARRAY, "verts", "x,y pairs for the literal types")
                .field(FLOAT_ARRAY, "uv", "u,v pairs for the literal types")
                .field(INT_ARRAY, "colors", "Packed ARGB per vertex for the literal types")
                .field(FLOAT_ARRAY, "widths", "Width control points, for the spline strip types")
                .field(
                        FLOAT_ARRAY,
                        "widthPositions",
                        "Where each width sits along the path, 0..1, empty for evenly spaced");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("meshId", mMeshId)
                .add("type", mType)
                .add("layout", mLayout)
                .add("uCount", mUCount)
                .add("vCount", mVCount)
                .add("flags", mFlags)
                .add("aux", mAux);
    }
}
