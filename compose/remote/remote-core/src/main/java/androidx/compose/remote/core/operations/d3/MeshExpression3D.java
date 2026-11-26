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
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Procedural 3D mesh generator — the 3D analogue of {@code PathExpression}. Evaluates RPN
 * float-expressions over a {@code (u,v)} grid (with {@code VAR1=u}, {@code VAR2=v}) each frame
 * and uploads the resulting mesh into the {@link Paint3DContext} under {@link #mId}, so the
 * existing {@code drawMesh3D(id, mode)} draws it. It is {@link VariableSupport}, so an
 * expression that references {@code time} animates the surface.
 *
 * <p>A single {@link #mType} enum selects the surface family, which fixes how many position
 * expressions are evaluated per vertex (efficiency): specialized types evaluate one expression
 * over an analytic base; {@link #SURFACE_GENERAL} evaluates three.
 *
 * <ul>
 *   <li>{@link #SURFACE_GENERAL}: params {@code [uMin,uMax,uCount, vMin,vMax,vCount]};
 *       position = {@code (X(u,v), Y(u,v), Z(u,v))} — 3 expressions.</li>
 *   <li>{@link #SURFACE_HEIGHT_FIELD}: params {@code [xMin,xMax,uCount, zMin,zMax,vCount,
 *       yMin,yMax]}; grid over X/Z, height {@code Y = clamp(h(x,z), yMin, yMax)} — 1 expr.</li>
 *   <li>{@link #SURFACE_SPHERE}: params {@code [radius, uCount, vCount]}; {@code u}∈[0,2π]
 *       longitude, {@code v}∈[0,π] latitude; radius {@code = radius + d(u,v)} — 1 expr.</li>
 *   <li>{@link #SURFACE_CYLINDER}: params {@code [radius, height, uCount, vCount]}; {@code u}
 *       ∈[0,2π] angle, {@code v} along the axis; radius {@code = radius + d(u,v)} — 1 expr.</li>
 * </ul>
 *
 * <p>Optional per-vertex <b>normal</b> expressions ({@code NX,NY,NZ(u,v)}; 0 = absent →
 * finite-difference normals from the grid) and <b>UV</b> expressions ({@code U,V(u,v)}; 0 =
 * absent → normalized grid coords) follow the position expressions on the wire.
 *
 * <p>Triangles are wound counter-clockwise as seen from the surface's front side, and
 * finite-difference normals point to that side. Without {@link #FLAG_FLIP_WINDING} the front side
 * is {@code +Y} for a {@link #SURFACE_HEIGHT_FIELD} (when {@code xMin < xMax} and
 * {@code zMin < zMax}), the inside of a {@link #SURFACE_SPHERE} (for a positive radius), the
 * outside of a {@link #SURFACE_CYLINDER} (for a positive height), and for
 * {@link #SURFACE_GENERAL} the side {@code ∂P/∂v × ∂P/∂u} points to, e.g. {@code -Z} for
 * {@code (u, v, 0)} (when {@code uMin < uMax} and {@code vMin < vMax}).
 * {@link #FLAG_FLIP_WINDING} reverses both the winding and the finite-difference normals; normal
 * expressions are used as authored. {@link #FLAG_FLIP_NORMALS} reverses every normal,
 * normal expressions included, and keeps the winding. Other flag bits are ignored.
 *
 * <p>Reading is tolerant so that documents written by a newer version still play: extra
 * trailing params and expressions are ignored, while an unknown surface type, too few params or
 * position expressions, a grid of more than {@link #MAX_GRID_VERTICES} vertices, or an
 * expression that fails to evaluate defines no mesh (nothing is passed to the renderer).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshExpression3D extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.MESH_EXPRESSION_3D;
    private static final String CLASS_NAME = "MeshExpression3D";
    private static final boolean DEBUG = false;

    /** Surface families for {@link #mType}. */
    public static final int SURFACE_GENERAL = 0;

    public static final int SURFACE_HEIGHT_FIELD = 1;
    public static final int SURFACE_SPHERE = 2;
    public static final int SURFACE_CYLINDER = 3;

    /**
     * {@link #mFlags} bit: reverse the triangle winding and the finite-difference normals, which
     * swaps the front and back sides of the surface.
     */
    public static final int FLAG_FLIP_WINDING = 0x1;

    /**
     * {@link #mFlags} bit: reverse every normal (finite-difference normals and normal
     * expressions) without changing the winding. Combines with {@link #FLAG_FLIP_WINDING}.
     */
    public static final int FLAG_FLIP_NORMALS = 0x2;

    /** Maximum number of grid vertices ({@code uCount * vCount}) a mesh is generated for. */
    public static final int MAX_GRID_VERTICES = 1_000_000;

    /**
     * Maximum length of the params array or of one expression a reader accepts (the expression
     * evaluator itself handles at most 128 floats).
     */
    static final int MAX_ARRAY_LENGTH = 4096;

    /** Maximum number of expressions in a position, normal or UV group a reader accepts. */
    static final int MAX_GROUP_SIZE = 8;

    /** Minimum number of params, indexed by surface type. */
    private static final int[] MIN_PARAMS = {6, 8, 3, 4};

    private final int mId;
    private final int mType;
    private final int mFlags;
    private final float @NonNull [] mParams;
    private final float @NonNull [] @NonNull [] mPos;
    private final float @NonNull [] @NonNull [] mNormal;
    private final float @NonNull [] @NonNull [] mUv;

    // Variable-resolved copies (globals replaced by literals; VAR1/VAR2 and operators kept).
    private final float @NonNull [] mOutParams;
    private final float @NonNull [] @NonNull [] mOutPos;
    private final float @NonNull [] @NonNull [] mOutNormal;
    private final float @NonNull [] @NonNull [] mOutUv;

    private final AnimatedFloatExpression mEval = new AnimatedFloatExpression();

    /**
     * False when the surface type is unknown or there are too few params or position
     * expressions; the operation then defines no mesh.
     */
    private final boolean mValid;

    // Output buffers, resized when the grid resolution changes.
    private float[] mVerts = new float[0];
    private float[] mNormalsBuf = new float[0];
    private float[] mUvBuf = new float[0];
    private int[] mIndices = new int[0];
    private int mGridU = -1;
    private int mGridV = -1;

    public MeshExpression3D(
            int id,
            int type,
            int flags,
            float @NonNull [] params,
            float @NonNull [] @NonNull [] pos,
            float @NonNull [] @NonNull [] normal,
            float @NonNull [] @NonNull [] uv) {
        mId = id;
        mType = type;
        mFlags = flags;
        mParams = params;
        mPos = pos;
        mNormal = normal;
        mUv = uv;
        mOutParams = params.clone();
        mOutPos = deepClone(pos);
        mOutNormal = deepClone(normal);
        mOutUv = deepClone(uv);
        mValid = isValid(type, params.length, pos.length);
    }

    /**
     * Returns true if the surface type is known and has enough params and position expressions.
     */
    private static boolean isValid(int type, int paramCount, int posCount) {
        if (type < 0 || type >= MIN_PARAMS.length) {
            return false;
        }
        int posNeeded = type == SURFACE_GENERAL ? 3 : 1;
        return paramCount >= MIN_PARAMS[type] && posCount >= posNeeded;
    }

    private static float[][] deepClone(float[][] src) {
        float[][] out = new float[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i].clone();
        }
        return out;
    }

    // ----- Variable support -------------------------------------------------

    /** A global variable reference that must be resolved to a literal before evaluation. */
    private static boolean isResolvable(float v) {
        return Float.isNaN(v)
                && !AnimatedFloatExpression.isMathOperator(v)
                && !NanMap.isDataVariable(v);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int i = 0; i < mParams.length; i++) {
            mOutParams[i] = isResolvable(mParams[i])
                    ? context.getFloat(Utils.idFromNan(mParams[i]))
                    : mParams[i];
        }
        resolveGroup(context, mPos, mOutPos);
        resolveGroup(context, mNormal, mOutNormal);
        resolveGroup(context, mUv, mOutUv);
    }

    private static void resolveGroup(
            RemoteContext context, float[][] src, float[][] dst) {
        for (int e = 0; e < src.length; e++) {
            float[] in = src[e];
            float[] out = dst[e];
            for (int i = 0; i < in.length; i++) {
                out[i] = isResolvable(in[i]) ? context.getFloat(Utils.idFromNan(in[i])) : in[i];
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mParams) {
            if (isResolvable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        listenGroup(context, mPos);
        listenGroup(context, mNormal);
        listenGroup(context, mUv);
    }

    private void listenGroup(RemoteContext context, float[][] group) {
        for (float[] e : group) {
            for (float v : e) {
                if (isResolvable(v)) {
                    context.listensTo(Utils.idFromNan(v), this);
                }
            }
        }
    }

    // ----- Wire -------------------------------------------------------------

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mType, mFlags, mParams, mPos, mNormal, mUv);
    }

    /**
     * Write a {@link MeshExpression3D} to the wire.
     *
     * @param buffer wire buffer
     * @param id     mesh id
     * @param type   surface family
     * @param flags  {@link #FLAG_FLIP_WINDING} and {@link #FLAG_FLIP_NORMALS}
     * @param params parameters
     * @param pos    position
     * @param normal normal
     * @param uv     texture coordinates
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            int type,
            int flags,
            float @NonNull [] params,
            float @NonNull [] @NonNull [] pos,
            float @NonNull [] @NonNull [] normal,
            float @NonNull [] @NonNull [] uv) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(type);
        buffer.writeInt(flags);
        writeArray(buffer, params);
        writeGroup(buffer, pos);
        writeGroup(buffer, normal);
        writeGroup(buffer, uv);
    }

    private static void writeArray(WireBuffer buffer, float[] a) {
        buffer.writeInt(a.length);
        for (float v : a) {
            buffer.writeFloat(v);
        }
    }

    private static void writeGroup(WireBuffer buffer, float[][] group) {
        buffer.writeInt(group.length);
        for (float[] e : group) {
            writeArray(buffer, e);
        }
    }

    /**
     * Read a {@link MeshExpression3D} from the wire.
     *
     * @param buffer     wire buffer
     * @param operations list of operations to add
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int type = buffer.readInt();
        int flags = buffer.readInt();
        float[] params = readArray(buffer);
        float[][] pos = readGroup(buffer);
        float[][] normal = readGroup(buffer);
        float[][] uv = readGroup(buffer);
        if (DEBUG && !isValid(type, params.length, pos.length)) {
            Utils.log("MeshExpression3D: unknown surface type " + type + " or too few params ("
                    + params.length + ") / position expressions (" + pos.length + "), skipped");
        }
        operations.add(new MeshExpression3D(id, type, flags, params, pos, normal, uv));
    }

    private static float[] readArray(WireBuffer buffer) {
        int len = buffer.readInt();
        if (len < 0 || len > MAX_ARRAY_LENGTH) {
            throw new RuntimeException("MeshExpression3D: bad array length " + len);
        }
        float[] a = new float[len];
        for (int i = 0; i < len; i++) {
            a[i] = buffer.readNanId();
        }
        return a;
    }

    private static float[][] readGroup(WireBuffer buffer) {
        int n = buffer.readInt();
        if (n < 0 || n > MAX_GROUP_SIZE) {
            throw new RuntimeException("MeshExpression3D: bad group count " + n);
        }
        float[][] g = new float[n][];
        for (int i = 0; i < n; i++) {
            g[i] = readArray(buffer);
        }
        return g;
    }

    // ----- Evaluation + upload ---------------------------------------------

    @Override
    public void paint(@NonNull PaintContext context) {
        if (!mValid || !(context instanceof Paint3DContext)) {
            return;
        }
        Paint3DContext p3d = (Paint3DContext) context;
        CollectionsAccess ca = context.getContext().getCollectionsAccess();

        int uCount = gridCount(0);
        int vCount = gridCount(1);
        if (uCount < 2 || vCount < 2) {
            return;
        }
        if ((long) uCount * vCount > MAX_GRID_VERTICES) {
            if (DEBUG) {
                Utils.log("MeshExpression3D: grid " + uCount + "x"
                        + vCount + " too large, skipped");
            }
            return;
        }
        ensureBuffers(uCount, vCount);
        try {
            evaluate(uCount, vCount, ca);
        } catch (RuntimeException e) {
            // A malformed expression, or one using an operator this version does not know.
            if (DEBUG) {
                Utils.log("MeshExpression3D: evaluation failed, skipped: " + e);
            }
            return;
        }
        p3d.defineMesh3D(mId, mIndices, mVerts, mNormalsBuf, mUvBuf);
    }

    /** Evaluate positions, UVs and normals of a {@code uCount × vCount} grid into the buffers. */
    private void evaluate(int uCount, int vCount, CollectionsAccess ca) {
        // Pass 1: positions + (optional) UV.
        boolean hasUv = mOutUv.length >= 2;
        for (int i = 0; i < uCount; i++) {
            float fu = i / (float) (uCount - 1);
            for (int j = 0; j < vCount; j++) {
                float fv = j / (float) (vCount - 1);
                int idx = i * vCount + j;
                position(idx * 3, fu, fv, ca);
                if (hasUv) {
                    mUvBuf[idx * 2] = eval(mOutUv[0], mLastA, mLastB, ca);
                    mUvBuf[idx * 2 + 1] = eval(mOutUv[1], mLastA, mLastB, ca);
                } else {
                    mUvBuf[idx * 2] = fu;
                    mUvBuf[idx * 2 + 1] = fv;
                }
            }
        }

        // Pass 2: normals (explicit expressions, else finite differences of the grid).
        if (mOutNormal.length >= 3) {
            float sign = (mFlags & FLAG_FLIP_NORMALS) != 0 ? -1f : 1f;
            for (int i = 0; i < uCount; i++) {
                float fu = i / (float) (uCount - 1);
                for (int j = 0; j < vCount; j++) {
                    float fv = j / (float) (vCount - 1);
                    int n = (i * vCount + j) * 3;
                    paramsFor(fu, fv);
                    mNormalsBuf[n] = sign * eval(mOutNormal[0], mLastA, mLastB, ca);
                    mNormalsBuf[n + 1] = sign * eval(mOutNormal[1], mLastA, mLastB, ca);
                    mNormalsBuf[n + 2] = sign * eval(mOutNormal[2], mLastA, mLastB, ca);
                }
            }
        } else {
            computeFiniteDifferenceNormals(uCount, vCount);
        }
    }

    private int gridCount(int axis) {
        switch (mType) {
            case SURFACE_HEIGHT_FIELD:
                return (int) mOutParams[axis == 0 ? 2 : 5];
            case SURFACE_SPHERE:
                return (int) mOutParams[axis == 0 ? 1 : 2];
            case SURFACE_CYLINDER:
                return (int) mOutParams[axis == 0 ? 2 : 3];
            default: // GENERAL
                return (int) mOutParams[axis == 0 ? 2 : 5];
        }
    }

    // Scratch: the two parameters last fed to the position/normal/uv expressions (u,v or x,z).
    private float mLastA;
    private float mLastB;

    /** Compute the (a,b) expression parameters for grid fractions (fu,fv) per surface type. */
    private void paramsFor(float fu, float fv) {
        switch (mType) {
            case SURFACE_HEIGHT_FIELD:
                mLastA = lerp(mOutParams[0], mOutParams[1], fu); // x
                mLastB = lerp(mOutParams[3], mOutParams[4], fv); // z
                break;
            case SURFACE_SPHERE:
                mLastA = fu * (float) (2.0 * Math.PI); // longitude
                mLastB = fv * (float) Math.PI; // latitude
                break;
            case SURFACE_CYLINDER:
                mLastA = fu * (float) (2.0 * Math.PI); // angle
                mLastB = lerp(-mOutParams[1] * 0.5f, mOutParams[1] * 0.5f, fv); // axis
                break;
            default: // GENERAL
                mLastA = lerp(mOutParams[0], mOutParams[1], fu);
                mLastB = lerp(mOutParams[3], mOutParams[4], fv);
                break;
        }
    }

    /** Evaluate position into mVerts at {@code off}; also sets mLastA/mLastB. */
    private void position(int off, float fu, float fv, CollectionsAccess ca) {
        paramsFor(fu, fv);
        float a = mLastA;
        float b = mLastB;
        switch (mType) {
            case SURFACE_HEIGHT_FIELD: {
                float h = eval(mOutPos[0], a, b, ca);
                float yMin = mOutParams[6];
                float yMax = mOutParams[7];
                if (h < yMin) h = yMin;
                if (h > yMax) h = yMax;
                mVerts[off] = a;
                mVerts[off + 1] = h;
                mVerts[off + 2] = b;
                break;
            }
            case SURFACE_SPHERE: {
                float r = mOutParams[0] + eval(mOutPos[0], a, b, ca);
                float sinV = (float) Math.sin(b);
                mVerts[off] = r * sinV * (float) Math.cos(a);
                mVerts[off + 1] = r * (float) Math.cos(b);
                mVerts[off + 2] = r * sinV * (float) Math.sin(a);
                break;
            }
            case SURFACE_CYLINDER: {
                float r = mOutParams[0] + eval(mOutPos[0], a, b, ca);
                mVerts[off] = r * (float) Math.cos(a);
                mVerts[off + 1] = b;
                mVerts[off + 2] = r * (float) Math.sin(a);
                break;
            }
            default: { // GENERAL
                mVerts[off] = eval(mOutPos[0], a, b, ca);
                mVerts[off + 1] = eval(mOutPos[1], a, b, ca);
                mVerts[off + 2] = eval(mOutPos[2], a, b, ca);
                break;
            }
        }
    }

    /**
     * Fills the normals with finite differences of the grid positions, pointing to the front side
     * of the triangles {@link #buildIndices} generates (to the back with {@link
     * #FLAG_FLIP_NORMALS}).
     */
    private void computeFiniteDifferenceNormals(int uCount, int vCount) {
        // du x dv (grid differences along u, then v) points to the back of the triangles unless
        // their winding is flipped; FLAG_FLIP_NORMALS reverses the result.
        float sign = (mFlags & FLAG_FLIP_WINDING) != 0 ? 1f : -1f;
        if ((mFlags & FLAG_FLIP_NORMALS) != 0) {
            sign = -sign;
        }
        boolean sphere = mType == SURFACE_SPHERE;
        for (int i = 0; i < uCount; i++) {
            int ip = Math.min(i + 1, uCount - 1);
            int im = Math.max(i - 1, 0);
            for (int j = 0; j < vCount; j++) {
                int n = (i * vCount + j) * 3;
                if (sphere && (j == 0 || j == vCount - 1)) {
                    // A pole: its grid row collapses to a point, but rounding can leave a tiny,
                    // mirrored ring whose du x dv is not small enough to be caught below.
                    fallbackNormal(n, i / (float) (uCount - 1), j / (float) (vCount - 1), sign);
                    continue;
                }
                int jp = Math.min(j + 1, vCount - 1);
                int jm = Math.max(j - 1, 0);
                int a = (ip * vCount + j) * 3;
                int b = (im * vCount + j) * 3;
                int c = (i * vCount + jp) * 3;
                int d = (i * vCount + jm) * 3;
                float dux = mVerts[a] - mVerts[b];
                float duy = mVerts[a + 1] - mVerts[b + 1];
                float duz = mVerts[a + 2] - mVerts[b + 2];
                float dvx = mVerts[c] - mVerts[d];
                float dvy = mVerts[c + 1] - mVerts[d + 1];
                float dvz = mVerts[c + 2] - mVerts[d + 2];
                float nx = duy * dvz - duz * dvy;
                float ny = duz * dvx - dux * dvz;
                float nz = dux * dvy - duy * dvx;
                float len = (float) Math.sqrt((double) (nx * nx + ny * ny + nz * nz));
                if (len > 1e-8f) {
                    float s = sign / len;
                    mNormalsBuf[n] = nx * s;
                    mNormalsBuf[n + 1] = ny * s;
                    mNormalsBuf[n + 2] = nz * s;
                } else {
                    fallbackNormal(n, i / (float) (uCount - 1), j / (float) (vCount - 1), sign);
                }
            }
        }
    }

    /**
     * Writes the normal used where the finite difference vanishes to {@code mNormalsBuf} at
     * {@code n}: {@code sign} times the direction of {@code du x dv} on the undisplaced surface
     * (radial for a sphere or cylinder, vertical for a height field; {@code -Y} for a general
     * surface, which has none), so that it is oriented like the finite-difference normals.
     */
    private void fallbackNormal(int n, float fu, float fv, float sign) {
        double x = 0;
        double y = 0;
        double z = 0;
        switch (mType) {
            case SURFACE_SPHERE: {
                double u = fu * 2.0 * Math.PI;
                double v = fv * Math.PI;
                x = Math.sin(v) * Math.cos(u);
                y = Math.cos(v);
                z = Math.sin(v) * Math.sin(u);
                break;
            }
            case SURFACE_CYLINDER: {
                // -radius * height * (cos u, 0, sin u)
                double u = fu * 2.0 * Math.PI;
                double s = mOutParams[0] * mOutParams[1] < 0 ? 1 : -1;
                x = s * Math.cos(u);
                z = s * Math.sin(u);
                break;
            }
            case SURFACE_HEIGHT_FIELD:
                // (0, -(xMax - xMin) * (zMax - zMin), 0)
                y = (mOutParams[1] - mOutParams[0]) * (mOutParams[4] - mOutParams[3]) < 0 ? 1 : -1;
                break;
            default: // GENERAL: no analytic surface
                y = -1;
                break;
        }
        mNormalsBuf[n] = (float) (x * sign);
        mNormalsBuf[n + 1] = (float) (y * sign);
        mNormalsBuf[n + 2] = (float) (z * sign);
    }

    private float eval(float[] expr, float a, float b, CollectionsAccess ca) {
        if (ca != null) {
            return mEval.eval(ca, expr, expr.length, a, b);
        }
        return mEval.eval(expr, expr.length, a, b);
    }

    private void ensureBuffers(int uCount, int vCount) {
        int verts = uCount * vCount;
        if (mVerts.length != verts * 3) {
            mVerts = new float[verts * 3];
            mNormalsBuf = new float[verts * 3];
            mUvBuf = new float[verts * 2];
        }
        if (mGridU != uCount || mGridV != vCount) {
            buildIndices(uCount, vCount);
            mGridU = uCount;
            mGridV = vCount;
        }
    }

    private void buildIndices(int uCount, int vCount) {
        mIndices = new int[(uCount - 1) * (vCount - 1) * 6];
        boolean flip = (mFlags & FLAG_FLIP_WINDING) != 0;
        int k = 0;
        for (int i = 0; i < uCount - 1; i++) {
            for (int j = 0; j < vCount - 1; j++) {
                int a = i * vCount + j;
                int b = i * vCount + (j + 1);
                int c = (i + 1) * vCount + (j + 1);
                int d = (i + 1) * vCount + j;
                if (flip) {
                    mIndices[k++] = a;
                    mIndices[k++] = c;
                    mIndices[k++] = b;
                    mIndices[k++] = c;
                    mIndices[k++] = a;
                    mIndices[k++] = d;
                } else {
                    mIndices[k++] = a;
                    mIndices[k++] = b;
                    mIndices[k++] = c;
                    mIndices[k++] = c;
                    mIndices[k++] = d;
                    mIndices[k++] = a;
                }
            }
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ----- Boilerplate ------------------------------------------------------

    /**
     * Documentation for {@link MeshExpression3D}.
     *
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Procedural mesh from float-expressions over a (u,v) grid. Type:"
                                + " 0=general(X,Y,Z), 1=heightField, 2=sphere, 3=cylinder."
                                + " A reader ignores extra params and expressions; an unknown"
                                + " type, too few params or position expressions, or a grid of"
                                + " more than 1000000 vertices defines no mesh.")
                .field(DocumentedOperation.INT, "id", "Mesh id used by DrawMesh3D")
                .field(DocumentedOperation.INT, "type", "Surface family")
                .field(DocumentedOperation.INT, "flags",
                        "Bit 0: flip the winding and the finite-difference normals; bit 1:"
                                + " flip every normal, keeping the winding; other bits ignored")
                .field(DocumentedOperation.INT, "paramsLen",
                        "Length of params (general 6, heightField 8, sphere 3, cylinder 4)")
                .field(DocumentedOperation.FLOAT_ARRAY, "params", "Grid parameters")
                .field(DocumentedOperation.INT, "posCount",
                        "Position expressions (general 3, others 1), each [len][len floats]")
                .field(DocumentedOperation.INT, "normalCount",
                        "Normal expressions (0 or 3), each [len][len floats]")
                .field(DocumentedOperation.INT, "uvCount",
                        "UV expressions (0 or 2), each [len][len floats]");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("type", mType)
                .add("posExprs", mPos.length)
                .add("normalExprs", mNormal.length)
                .add("uvExprs", mUv.length);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(id=" + mId + ", type=" + mType + ", pos=" + mPos.length + ")";
    }
}
