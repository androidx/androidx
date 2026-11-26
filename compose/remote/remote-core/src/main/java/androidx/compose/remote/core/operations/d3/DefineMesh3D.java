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
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Upload a cached 3D mesh keyed by id.
 *
 * <p>Bit 30 of {@code id} marks the advanced form of a later format version; a reader of this
 * version refuses it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefineMesh3D extends PaintOperation {
    private static final int OP_CODE = Operations.DEFINE_MESH_3D;
    private static final String CLASS_NAME = "DefineMesh3D";
    // Sanity limits for corrupt documents: 1M vertices, 1M triangles.
    private static final int MAX_VERTS_FLOATS = 3 * 1_000_000;
    private static final int MAX_INDICES = 3 * 1_000_000;

    private final int mId;
    private final int @NonNull [] mIndices;
    private final float @NonNull [] mVerts;
    private final float @Nullable [] mNormals;
    private final float @Nullable [] mUv;

    public DefineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals) {
        this(id, indices, verts, normals, null);
    }

    public DefineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        mId = id;
        mIndices = indices;
        mVerts = verts;
        mNormals = normals;
        mUv = uv;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mIndices, mVerts, mNormals, mUv);
    }

    /** Serialize to {@code buffer} (no UV). */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals) {
        apply(buffer, id, indices, verts, normals, null);
    }

    /** Serialize to {@code buffer} with optional per-vertex UV. */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(indices.length);
        for (int v : indices) {
            buffer.writeInt(v);
        }
        buffer.writeInt(verts.length);
        for (float v : verts) {
            buffer.writeFloat(v);
        }
        if (normals == null) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(normals.length);
            for (float v : normals) {
                buffer.writeFloat(v);
            }
        }
        if (uv == null) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(uv.length);
            for (float v : uv) {
                buffer.writeFloat(v);
            }
        }
    }

    /**
     * Read from buffer into {@code operations}.
     *
     * @param buffer     WireBuffer
     * @param operations List of Operation to add to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        Advanced3D.check(id, CLASS_NAME);
        int idxLen = buffer.readInt();
        if (idxLen < 0 || idxLen > MAX_INDICES || (idxLen % 3) != 0) {
            throw new RuntimeException("DefineMesh3D: bad indices length " + idxLen);
        }
        int[] indices = new int[idxLen];
        for (int i = 0; i < idxLen; i++) {
            indices[i] = buffer.readInt();
        }
        int vertLen = buffer.readInt();
        if (vertLen < 0 || vertLen > MAX_VERTS_FLOATS || (vertLen % 3) != 0) {
            throw new RuntimeException("DefineMesh3D: bad verts length " + vertLen);
        }
        float[] verts = new float[vertLen];
        for (int i = 0; i < vertLen; i++) {
            verts[i] = buffer.readFloat();
        }
        int normLen = buffer.readInt();
        float[] normals;
        if (normLen == 0) {
            normals = null;
        } else {
            if (normLen != vertLen) {
                throw new RuntimeException(
                        "DefineMesh3D: normals length " + normLen + " != verts " + vertLen);
            }
            normals = new float[normLen];
            for (int i = 0; i < normLen; i++) {
                normals[i] = buffer.readFloat();
            }
        }
        int uvLen = buffer.readInt();
        float[] uv;
        if (uvLen == 0) {
            uv = null;
        } else {
            if (uvLen != (vertLen / 3) * 2) {
                throw new RuntimeException(
                        "DefineMesh3D: uv length " + uvLen + " != 2/3 verts " + vertLen);
            }
            uv = new float[uvLen];
            for (int i = 0; i < uvLen; i++) {
                uv[i] = buffer.readFloat();
            }
        }
        operations.add(new DefineMesh3D(id, indices, verts, normals, uv));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (context instanceof Paint3DContext) {
            ((Paint3DContext) context).defineMesh3D(mId, mIndices, mVerts, mNormals, mUv);
        }
    }

    /**
     * Documentation for {@link DefineMesh3D}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description("Upload a cached 3D mesh keyed by id.")
                .field(
                        DocumentedOperation.INT,
                        "id",
                        "Stable mesh id; bit 30: advanced form of a later format version,"
                                + " readers refuse it")
                .field(DocumentedOperation.INT, "indicesLen", "Number of indices")
                .field(DocumentedOperation.INT_ARRAY, "indices", "Triangle index list")
                .field(DocumentedOperation.INT, "vertsLen", "Number of vertex floats")
                .field(DocumentedOperation.FLOAT_ARRAY, "verts", "xyz triples")
                .field(DocumentedOperation.INT, "normalsLen", "0 for none, else == vertsLen")
                .field(DocumentedOperation.FLOAT_ARRAY, "normals", "Optional nxnynz triples")
                .field(DocumentedOperation.INT, "uvLen", "0 for none, else 2 per vertex")
                .field(DocumentedOperation.FLOAT_ARRAY, "uv", "Optional u,v pairs");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("indicesLen", mIndices.length)
                .add("vertsLen", mVerts.length)
                .add("hasNormals", mNormals != null ? 1 : 0);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME
                + "(id="
                + mId
                + ", tris="
                + (mIndices.length / 3)
                + ", verts="
                + (mVerts.length / 3)
                + ", normals="
                + (mNormals != null)
                + ")";
    }
}
