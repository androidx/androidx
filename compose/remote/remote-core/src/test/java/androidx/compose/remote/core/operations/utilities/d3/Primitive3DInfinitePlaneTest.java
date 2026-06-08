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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/**
 * Tests the finite quad {@link Primitive3D#build} returns for {@link Primitive3D#INFINITE_PLANE},
 * which is what a renderer without an infinite-plane path draws.
 */
public class Primitive3DInfinitePlaneTest {

    private static final float E = Primitive3D.INFINITE_PLANE_FALLBACK_HALF_SIZE;
    private static final float[] PARAMS = {1, 2, 3, 0.5f, 1, 1, 0.5f, 0.5f, 0.5f};

    @Test
    public void defaultAxis_isALargeQuadFacingUp() {
        MeshData mesh = Primitive3D.build(Primitive3D.INFINITE_PLANE, 0f, 0, PARAMS);
        assertQuad(mesh, 1, 2, 3, 1);
        assertNull(mesh.uv);
    }

    @Test
    public void axisField_turnsTheQuadAboutItsCenter() {
        assertQuad(build(Primitive3D.AXIS_X), 1, 2, 3, 0);
        assertQuad(build(Primitive3D.AXIS_Y), 1, 2, 3, 1);
        assertQuad(build(Primitive3D.AXIS_Z), 1, 2, 3, 2);
    }

    @Test
    public void uv_onlyWhenRequested() {
        int flags = Primitive3D.UV << Primitive3D.FLAG_UV_SHIFT;
        MeshData mesh = Primitive3D.build(Primitive3D.INFINITE_PLANE, 0f, flags, PARAMS);
        assertNotNull(mesh.uv);
        assertEquals(8, mesh.uv.length);
        for (float v : mesh.uv) {
            assertTrue(v == 0f || v == 1f);
        }
    }

    @Test
    public void missingCenter_isTheOrigin() {
        assertQuad(Primitive3D.build(Primitive3D.INFINITE_PLANE, 0f, 0, new float[0]), 0, 0, 0, 1);
    }

    @Test
    public void defaultDefineInfinitePlane3D_definesTheFallbackQuad() {
        MeshRecorder recorder = new MeshRecorder();
        int flags = Primitive3D.AXIS_Z << Primitive3D.FLAG_AXIS_SHIFT;
        recorder.defineInfinitePlane3D(4, flags, PARAMS);
        assertEquals(4, recorder.mId);
        assertQuad(
                new MeshData(recorder.mVerts, recorder.mNormals, recorder.mIndices), 1, 2, 3, 2);
    }

    private static MeshData build(int axis) {
        return Primitive3D.build(
                Primitive3D.INFINITE_PLANE, 0f, axis << Primitive3D.FLAG_AXIS_SHIFT, PARAMS);
    }

    /**
     * Checks a two-triangle square of half side {@link #E} centered on {@code (cx,cy,cz)},
     * perpendicular to {@code axis} (0=X, 1=Y, 2=Z), with every normal along +axis and both
     * triangles counter-clockwise seen from +axis.
     */
    private static void assertQuad(MeshData mesh, float cx, float cy, float cz, int axis) {
        float[] center = {cx, cy, cz};
        assertEquals(12, mesh.verts.length);
        assertEquals(6, mesh.indices.length);
        for (int v = 0; v < 4; v++) {
            for (int c = 0; c < 3; c++) {
                float d = mesh.verts[v * 3 + c] - center[c];
                if (c == axis) {
                    assertEquals(0f, d, 1e-3f);
                    assertEquals(1f, mesh.normals[v * 3 + c], 1e-6f);
                } else {
                    assertEquals(E, Math.abs(d), 1e-3f);
                    assertEquals(0f, mesh.normals[v * 3 + c], 1e-6f);
                }
            }
        }
        for (int t = 0; t < 2; t++) {
            int a = mesh.indices[t * 3] * 3;
            int b = mesh.indices[t * 3 + 1] * 3;
            int c = mesh.indices[t * 3 + 2] * 3;
            float[] u = new float[3];
            float[] w = new float[3];
            for (int k = 0; k < 3; k++) {
                u[k] = mesh.verts[b + k] - mesh.verts[a + k];
                w[k] = mesh.verts[c + k] - mesh.verts[a + k];
            }
            float[] n = {
                u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]
            };
            assertTrue("triangle " + t + " faces +axis", n[axis] > 0);
        }
    }

    /** A {@link Paint3DContext} with only the methods it must have, recording the mesh. */
    private static final class MeshRecorder implements Paint3DContext {
        int mId;
        int[] mIndices;
        float[] mVerts;
        float[] mNormals;

        @Override
        public void defineMesh3D(
                int id,
                int @NonNull [] indices,
                float @NonNull [] verts,
                float @Nullable [] normals) {
            mId = id;
            mIndices = indices;
            mVerts = verts;
            mNormals = normals;
        }

        @Override
        public void setCamera3D(
                int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {}

        @Override
        public void matrix3DOp(int sub, float @NonNull [] args) {}

        @Override
        public void drawMesh3D(int meshId, int mode) {}

        @Override
        public void clearDepth3D() {}

        @Override
        public void setLights3D(
                int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params) {}

        @Override
        public void setTexture3D(int bitmapId) {}
    }
}
