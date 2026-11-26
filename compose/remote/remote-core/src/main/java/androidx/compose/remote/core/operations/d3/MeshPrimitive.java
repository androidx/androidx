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
package androidx.compose.remote.core.operations.d3;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.d3.MeshData;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Define a 3D mesh (by id) from a parametric primitive. The geometry math lives in
 * {@link Primitive3D}; this op only carries the {@code type}, {@code flags} and a
 * variable-length {@code params} float array, and on {@link #paint} builds the mesh once and
 * uploads it via {@code defineMesh3D}. The mesh is then drawn with {@code drawMesh3D}.
 *
 * <p>{@code segments} is a float sampling control whose meaning is type-dependent — the number of
 * radial/longitudinal divisions to split the shape into (e.g. a cylinder with {@code segments = 3}
 * is a triangular prism); secondary subdivisions (a sphere's latitude bands, etc.) are derived
 * from it. {@code flags} is a per-type mask for alternative samplings — currently
 * {@code TUBE}/{@code CAP_TUBE} default to a monotonic-spline path swept with disks normal to the
 * tangent (the along-path ring count is adaptive — see {@link Primitive3D#tube}), and {@link
 * Primitive3D#FLAG_TUBE_LEGACY} selects the older raw-polyline sweep.
 *
 * <p>Type / params layout (angles in radians, trailing {@code x,y,z} translates the shape):
 *
 * <ul>
 *   <li>{@code SPHERE} (0): {@code radius, x, y, z}
 *   <li>{@code CYLINDER} (1): {@code radius, x1,y1,z1, x2,y2,z2}
 *   <li>{@code CONE} (2): {@code radius, height, x,y,z} (base at {@code (x,y,z)}, apex up)
 *   <li>{@code CUBE} (3): {@code dimX, dimY, dimZ, x,y,z}
 *   <li>{@code ROUNDED_CUBE} (4): {@code dimX, dimY, dimZ, x,y,z, cornerRadius}
 *   <li>{@code SPHERICAL_SECTOR} (5): {@code radius, angle, x,y,z}
 *   <li>{@code SPHERICAL_DOME} (6): {@code radius, angle, x,y,z}
 *   <li>{@code TUBE} (7): {@code radius, x0,y0,z0, x1,y1,z1, ...} (>= 2 points)
 *   <li>{@code CAP_TUBE} (8): same as TUBE, with end caps
 *   <li>{@code PROFILE_TUBE} (9): {@code nPoints, x0,y0,z0, …, r0, r1, …} — positions and an
 *       independent radius list (counts may differ) each splined; radius tapers/bulges along the
 *       path. Caps via {@code FLAG_TUBE_CAP}.
 *   <li>{@code TORUS} (10): {@code majorRadius, minorRadius, x,y,z} (donut in the XZ plane)
 *   <li>{@code PLANE} (11): {@code width, height, x,y,z} (flat rect in the XY plane, faces +Z)
 *   <li>{@code EXTRUDE_*} (12–18), {@code LATHE} (19), {@code SWEEP} (20), {@code HELIX} (21),
 *       {@code ICOSPHERE} (22): see the type constants of {@link Primitive3D}; {@code LATHE} and
 *       {@code SWEEP} carry their profile / section / path in channels 1+.
 *   <li>{@code INFINITE_PLANE} (23): {@code cx,cy,cz, spacing, lineWidth, lineA,lineR,lineG,
 *       lineB} — a point on the plane, the grid cell size (0 = no grid), the grid line width in
 *       pixels and the grid line color (ARGB, each 0..1). It is not built into a mesh here but
 *       handed to {@link Paint3DContext#defineInfinitePlane3D(int, int, float[])}, whose default
 *       defines the finite fallback quad of {@link Primitive3D#infinitePlaneFallback}. Missing
 *       trailing params take the defaults {@code 0,0,0, 0, 1, 1,0.5,0.5,0.5}; extra ones are
 *       ignored.
 * </ul>
 *
 * <p>{@code flags} bits 4-5 are a UV-mapping mode ({@code FLAG_UV_*} / {@code uvMode}); set it to
 * generate UV coordinates so the mesh can be textured (every type generates its default mapping,
 * see {@link Primitive3D}). Bits 6-7 are the axis field ({@link Primitive3D#axisMode}: native, +X,
 * +Y or +Z) and bit 8 is {@link Primitive3D#FLAG_TUBE_CURVATURE_DENSITY}.
 *
 * <p>Reading is tolerant: an unknown type, too few params for the type, or params the builder
 * rejects (e.g. a tube with fewer than 2 points) define no mesh — a no-op, logged in debug builds,
 * never an exception. Only corrupt lengths (negative or absurd) fail the read.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshPrimitive extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.MESH_PRIMITIVE_3D;
    private static final String CLASS_NAME = "MeshPrimitive";
    private static final boolean DEBUG = false;

    /** Most float channels an operation may carry; more means a corrupt document. */
    private static final int MAX_CHANNELS = 64;

    /** Most floats in one channel; more means a corrupt document. */
    private static final int MAX_CHANNEL_LENGTH = 1 << 20;

    /**
     * Values of the {@link Primitive3D#INFINITE_PLANE} params a document leaves out: center
     * {@code 0,0,0}, no grid, 1 pixel lines, opaque mid-gray lines.
     */
    private static final float[] INFINITE_PLANE_DEFAULTS = {
        0f, 0f, 0f, 0f, 1f, 1f, 0.5f, 0.5f, 0.5f
    };

    /**
     * Fewest channel-0 params each type ({@link Primitive3D#SPHERE} ..
     * {@link Primitive3D#ICOSPHERE}) reads unconditionally. The variable-length types check the
     * rest themselves (or in {@link #countsFit}).
     */
    private static final int[] MIN_PARAMS = {
        4, // SPHERE
        7, // CYLINDER
        5, // CONE
        6, // CUBE
        7, // ROUNDED_CUBE
        5, // SPHERICAL_SECTOR
        5, // SPHERICAL_DOME
        7, // TUBE: radius + 2 points
        7, // CAP_TUBE
        1, // PROFILE_TUBE: nPoints (see countsFit)
        5, // TORUS
        5, // PLANE
        5, // EXTRUDE_CIRCLE
        7, // EXTRUDE_SECTOR
        7, // EXTRUDE_SEGMENT
        8, // EXTRUDE_ARC
        7, // EXTRUDE_ROUNDED_RECT
        6, // EXTRUDE_SQUIRCLE
        6, // EXTRUDE_PATH: depth, cx,cy,cz, bevel, contourCount (see countsFit)
        0, // LATHE: [cx,cy,cz] all optional
        0, // SWEEP: scalars all optional
        4, // HELIX
        1, // ICOSPHERE
    };

    private final int mId;
    private final int mType;
    private final float mSegments; // source: may be a NaN-encoded variable id
    private final int mFlags;

    /**
     * Input geometry as one or more float channels. {@code mData[0]} is the scalar params (radius,
     * depth, {@code x,y,z}, …) — the only channel for most shapes; channels {@code 1+} carry
     * geometry
     * streams (profile / path points) for the multi-channel types (lathe, sweep, …). Any value
     * may be
     * a NaN-encoded variable id; {@link #mResolvedData} holds the resolved geometry.
     */
    private final float @NonNull [][] mData;

    /** Resolved copies (variables substituted) used to build the mesh. */
    private float mResolvedSegments;
    private final float @NonNull [][] mResolvedData;

    /**
     * True until the resolved geometry has been handed to the renderer; set again when a watched
     * variable changes, so the next {@link #paint} redefines the mesh.
     */
    private boolean mDirty = true;

    public MeshPrimitive(int id, int type, float segments, int flags, float @NonNull [][] data) {
        mId = id;
        mType = type;
        mSegments = segments;
        mFlags = flags;
        mData = data;
        mResolvedSegments = segments;
        mResolvedData = new float[data.length][];
        for (int c = 0; c < data.length; c++) {
            mResolvedData[c] = data[c].clone();
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mResolvedSegments =
                Utils.isVariable(mSegments)
                        ? context.getFloat(Utils.idFromNan(mSegments))
                        : mSegments;
        for (int c = 0; c < mData.length; c++) {
            float[] src = mData[c];
            float[] out = mResolvedData[c];
            for (int i = 0; i < src.length; i++) {
                float v = src[i];
                out[i] = Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
            }
        }
        // A driving variable changed → the geometry must be regenerated on the next paint().
        mDirty = true;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Utils.isVariable(mSegments)) {
            context.listensTo(Utils.idFromNan(mSegments), this);
        }
        for (float[] channel : mData) {
            for (float v : channel) {
                if (Utils.isVariable(v)) {
                    context.listensTo(Utils.idFromNan(v), this);
                }
            }
        }
    }

    /** Convenience for the common single-channel (scalar-params-only) case. */
    public MeshPrimitive(int id, int type, float segments, int flags, float @NonNull [] params) {
        this(id, type, segments, flags, new float[][]{params});
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mType, mSegments, mFlags, mData);
    }

    /**
     * Write a {@link MeshPrimitive} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param id mesh id
     * @param type primitive type
     * @param segments radial/longitudinal subdivisions
     * @param flags per-type sampling mask
     * @param data per-channel data
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            int type,
            float segments,
            int flags,
            float @NonNull [][] data) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(type);
        buffer.writeFloat(segments);
        buffer.writeInt(flags);
        buffer.writeInt(data.length); // channel count
        for (float[] channel : data) {
            buffer.writeInt(channel.length);
            for (float v : channel) {
                buffer.writeFloat(v);
            }
        }
    }

    /** Convenience for the common single-channel (scalar-params-only) case. */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            int type,
            float segments,
            int flags,
            float @NonNull [] params) {
        apply(buffer, id, type, segments, flags, new float[][]{params});
    }

    /**
     * Read a {@link MeshPrimitive} from a {@link WireBuffer}. Any type is accepted (an unknown one
     * paints nothing); only a negative or absurd channel count or length fails.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int type = buffer.readInt();
        float segments = buffer.readNanId();
        int flags = buffer.readInt();
        int channels = buffer.readInt();
        if (channels < 0 || channels > MAX_CHANNELS) {
            throw new RuntimeException("MeshPrimitive: bad channel count " + channels);
        }
        float[][] data = new float[channels][];
        for (int c = 0; c < channels; c++) {
            int len = buffer.readInt();
            if (len < 0 || len > MAX_CHANNEL_LENGTH) {
                throw new RuntimeException("MeshPrimitive: bad channel length " + len);
            }
            float[] channel = new float[len];
            for (int i = 0; i < len; i++) {
                channel[i] = buffer.readNanId();
            }
            data[c] = channel;
        }
        operations.add(new MeshPrimitive(id, type, segments, flags, data));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (!(context instanceof Paint3DContext) || !mDirty) {
            return;
        }
        // Cleared first so a primitive that can't be built is not retried every frame.
        mDirty = false;
        Paint3DContext p3d = (Paint3DContext) context;
        if (mType == Primitive3D.INFINITE_PLANE) {
            p3d.defineInfinitePlane3D(mId, mFlags, infinitePlaneParams(mResolvedData));
            return;
        }
        MeshData mesh = buildMesh();
        if (mesh != null) {
            p3d.defineMesh3D(mId, mesh.indices, mesh.verts, mesh.normals, mesh.uv);
        }
    }

    /**
     * The {@link Primitive3D#INFINITE_PLANE_PARAMS} params of an infinite plane: channel 0 of
     * {@code data}, with missing trailing params taken from {@link #INFINITE_PLANE_DEFAULTS} and
     * extra ones dropped. Always a new array, so a renderer may keep it.
     */
    static float @NonNull [] infinitePlaneParams(float @NonNull [][] data) {
        float[] params = INFINITE_PLANE_DEFAULTS.clone();
        if (data.length > 0) {
            float[] p = data[0];
            System.arraycopy(p, 0, params, 0, Math.min(p.length, params.length));
        }
        return params;
    }

    /**
     * Builds the mesh from the resolved params, or returns {@code null} (logged in debug builds)
     * for an unknown type or params the type can't be built from.
     */
    private @Nullable MeshData buildMesh() {
        if (mType < 0 || mType >= MIN_PARAMS.length) {
            if (DEBUG) {
                Utils.log(CLASS_NAME + " " + mId + ": unknown type " + mType + ", skipped");
            }
            return null;
        }
        float[] p = mResolvedData.length > 0 ? mResolvedData[0] : null;
        if (p == null || p.length < MIN_PARAMS[mType] || !countsFit(p)) {
            if (DEBUG) {
                Utils.log(CLASS_NAME + " " + mId + ": too few params for type " + mType
                        + ", skipped");
            }
            return null;
        }
        try {
            return Primitive3D.build(mType, mResolvedSegments, mFlags, mResolvedData);
        } catch (RuntimeException e) {
            // The builders reject malformed geometry (e.g. a tube with fewer than 2 points).
            if (DEBUG) {
                Utils.log(CLASS_NAME + " " + mId + ": type " + mType + " not built: " + e);
            }
            return null;
        }
    }

    /**
     * Checks the counts {@link Primitive3D#PROFILE_TUBE} and {@link Primitive3D#EXTRUDE_PATH} read
     * from their params against the params actually there, so a corrupt count is skipped instead
     * of making the builder allocate absurd arrays. Accepts exactly what the builders accept,
     * except contour point counts above the channel length limit.
     */
    private boolean countsFit(float @NonNull [] p) {
        if (mType == Primitive3D.PROFILE_TUBE) {
            long n = Math.round(p[0]);
            int posOff = (mFlags & Primitive3D.FLAG_TUBE_PATH_DENSITY) != 0 ? 2 : 1;
            // At least 2 points and 1 radius.
            return n >= 2 && posOff + 3 * n < p.length;
        }
        if (mType == Primitive3D.EXTRUDE_PATH) {
            int contours = Math.round(p[5]);
            int idx = 6;
            for (int c = 0; c < contours; c++) {
                if (idx >= p.length) {
                    return false;
                }
                int count = Math.round(p[idx++]);
                if (count < 0 || count > MAX_CHANNEL_LENGTH) {
                    return false;
                }
                // A contour cut short by the end of the params is zero-filled by the builder.
                idx += Math.min(count * 2, p.length - idx);
            }
        }
        return true;
    }

    /**
     * Documentation for {@link MeshPrimitive}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description("Define a 3D mesh by id from a parametric primitive (sphere, cylinder,"
                        + " cone, cube, rounded cube, spherical sector/dome, tubes, torus, plane,"
                        + " extrusions, lathe, sweep, helix, icosphere: types 0-22, see"
                        + " Primitive3D) or an infinite plane (type 23: params cx,cy,cz,"
                        + " spacing, lineWidth, lineA,lineR,lineG,lineB; missing trailing params"
                        + " default to 0,0,0, 0, 1, 1,0.5,0.5,0.5). An unknown type or too few"
                        + " params define nothing.")
                .field(DocumentedOperation.INT, "id", "Mesh id to define")
                .field(DocumentedOperation.INT, "type", "Primitive type (see Primitive3D)")
                .field(DocumentedOperation.FLOAT, "segments", "Radial/longitudinal subdivisions")
                .field(DocumentedOperation.INT, "flags",
                        "Bits 0-3 per-type sampling (FLAG_SPLINE, FLAG_TUBE_LEGACY,"
                                + " FLAG_TUBE_PATH_DENSITY, FLAG_TUBE_CAP), bits 4-5 UV mode,"
                                + " bits 6-7 axis (native, X, Y, Z), bit 8"
                                + " FLAG_TUBE_CURVATURE_DENSITY")
                .field(DocumentedOperation.INT, "channelCount", "Number of float channels")
                .field(DocumentedOperation.FLOAT, "channels", "channelCount",
                        "Per-channel [length, floats…]: channel 0 = scalar params, 1+ = geometry");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("type", mType)
                .add("segments", mSegments)
                .add("flags", mFlags)
                .add("channels", mData.length)
                .addFloatExpressionSrc("params", mData.length > 0 ? mData[0] : new float[0]);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(id=" + mId + ", type=" + mType + ", channels=" + mData.length + ")";
    }
}
