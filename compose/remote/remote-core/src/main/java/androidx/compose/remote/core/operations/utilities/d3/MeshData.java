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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A simple triangle-mesh container produced by {@link Primitive3D}: interleaved-free parallel
 * arrays of vertex positions, per-vertex normals, an optional UV array, and a triangle index list,
 * matching the layout expected by {@code Paint3DContext.defineMesh3D}.
 *
 * <ul>
 *   <li>{@link #verts} — {@code x,y,z} triples ({@code length % 3 == 0})
 *   <li>{@link #normals} — {@code nx,ny,nz} triples, same length as {@link #verts}
 *   <li>{@link #uv} — optional {@code u,v} pairs ({@code length == verts.length / 3 * 2}), or null
 *   <li>{@link #indices} — CCW (front-facing-from-outside) triangle list ({@code length % 3 == 0})
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshData {
    public float @NonNull [] verts;
    public float @NonNull [] normals;
    public int @NonNull [] indices;
    public float @Nullable [] uv;

    public MeshData(float @NonNull [] verts, float @NonNull [] normals, int @NonNull [] indices) {
        this(verts, normals, indices, null);
    }

    public MeshData(
            float @NonNull [] verts,
            float @NonNull [] normals,
            int @NonNull [] indices,
            float @Nullable [] uv) {
        this.verts = verts;
        this.normals = normals;
        this.indices = indices;
        this.uv = uv;
    }

    /**
     * Merge several meshes into one, re-basing each part's indices. Lets a primitive be assembled
     * from reusable pieces (e.g. a cylinder = side wall + two cap disks). UV is merged only if
     * every part carries it (otherwise the result has no UV).
     */
    @NonNull
    public static MeshData concat(@NonNull List<MeshData> parts) {
        int vc = 0;
        int ic = 0;
        boolean allUv = !parts.isEmpty();
        for (MeshData m : parts) {
            vc += m.verts.length;
            ic += m.indices.length;
            if (m.uv == null) {
                allUv = false;
            }
        }
        float[] v = new float[vc];
        float[] n = new float[vc];
        int[] idx = new int[ic];
        float[] uvOut = allUv ? new float[vc / 3 * 2] : null;
        int vo = 0;
        int io = 0;
        int uvo = 0;
        int base = 0;
        for (MeshData m : parts) {
            System.arraycopy(m.verts, 0, v, vo, m.verts.length);
            System.arraycopy(m.normals, 0, n, vo, m.normals.length);
            for (int k = 0; k < m.indices.length; k++) {
                idx[io + k] = m.indices[k] + base;
            }
            if (uvOut != null && m.uv != null) {
                System.arraycopy(m.uv, 0, uvOut, uvo, m.uv.length);
                uvo += m.uv.length;
            }
            base += m.verts.length / 3;
            vo += m.verts.length;
            io += m.indices.length;
        }
        return new MeshData(v, n, idx, uvOut);
    }
}
