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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Derives a 2D affine from a point on a mesh and multiplies it into the current canvas matrix.
 *
 * <p>The exact analogue of {@link MatrixFromPath}: a path is one dimensional and yields a transform
 * at a fraction along it, a mesh is two dimensional and yields one at {@code (u, v)}. It draws
 * nothing - it is a matrix operation that happens to read its numbers from a mesh.
 *
 * <p>This is what stops meshes being a closed world. Ordinary 2D drawing can be placed <b>onto</b>
 * a deformed surface without any of it needing to know that meshes exist: attach a label to a
 * waving flag, put icons around a ring, run text along a path strip. The content is drawn with the
 * commands that already exist; the mesh only supplies the matrix.
 *
 * <p>The frame is built from the surface's partial derivatives at the point:
 *
 * <pre>
 *   [ du.x  dv.x  P.x ]
 *   [ du.y  dv.y  P.y ]
 * </pre>
 *
 * so the unit square in local space maps onto the surface patch at {@code (u, v)}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MatrixFromMesh2D extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.MATRIX_FROM_MESH_2D;
    private static final String CLASS_NAME = "MatrixFromMesh2D";

    /** Keep the origin only: content rides the surface, staying upright and unscaled. */
    public static final int FLAG_ORIGIN = 0;

    /** Keep origin and rotation: content turns with the surface. */
    public static final int FLAG_ROTATION = 1;

    /** Keep origin, rotation and scale: content stretches with the surface. */
    public static final int FLAG_SCALE = 2;

    /** Keep the full 2x3, skew included: the faithful case. */
    public static final int FLAG_FULL = 3;

    private final int mMeshId;
    private final float mU;
    private final float mV;
    private final int mFlags;
    private float mOutU;
    private float mOutV;

    public MatrixFromMesh2D(int meshId, float u, float v, int flags) {
        mMeshId = meshId;
        mU = u;
        mOutU = u;
        mV = v;
        mOutV = v;
        mFlags = flags;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutU = Float.isNaN(mU) ? context.getFloat(Utils.idFromNan(mU)) : mU;
        mOutV = Float.isNaN(mV) ? context.getFloat(Utils.idFromNan(mV)) : mV;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mU)) {
            context.listensTo(Utils.idFromNan(mU), this);
        }
        if (Float.isNaN(mV)) {
            context.listensTo(Utils.idFromNan(mV), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mMeshId, mU, mV, mFlags);
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.matrixFromMesh(mMeshId, mOutU, mOutV, mFlags);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME
                + " ["
                + mMeshId
                + "] "
                + Utils.floatToString(mU, mOutU)
                + ", "
                + Utils.floatToString(mV, mOutV)
                + ", "
                + mFlags;
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
     * Set the matrix from a point on a mesh.
     *
     * @param buffer the buffer to add to
     * @param meshId the mesh to read the surface from
     * @param u the u parameter, 0..1
     * @param v the v parameter, 0..1
     * @param flags which parts of the local frame to apply
     */
    public static void apply(@NonNull WireBuffer buffer, int meshId, float u, float v, int flags) {
        buffer.start(OP_CODE);
        buffer.writeInt(meshId);
        buffer.writeFloat(u);
        buffer.writeFloat(v);
        buffer.writeInt(flags);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int meshId = buffer.readId();
        float u = buffer.readNanId();
        float v = buffer.readNanId();
        int flags = buffer.readInt();
        operations.add(new MatrixFromMesh2D(meshId, u, v, flags));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Matrix Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .description(
                        "Multiply the local frame of a 2D mesh at (u, v) into the current canvas"
                                + " matrix, so ordinary drawing can be placed onto a deformed"
                                + " surface")
                .field(INT, "meshId", "The mesh to read the surface from")
                .field(FLOAT, "u", "The u parameter [0..1]")
                .field(FLOAT, "v", "The v parameter [0..1]")
                .field(INT, "flags", "Which parts of the local frame to apply")
                .possibleValues("FLAG_ORIGIN", FLAG_ORIGIN)
                .possibleValues("FLAG_ROTATION", FLAG_ROTATION)
                .possibleValues("FLAG_SCALE", FLAG_SCALE)
                .possibleValues("FLAG_FULL", FLAG_FULL);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("meshId", mMeshId)
                .add("u", mU, mOutU)
                .add("v", mV, mOutV)
                .add("flags", mFlags);
    }
}
