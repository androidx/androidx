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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.testing.HeadlessRemoteContext;
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/**
 * Checks which side of each {@link MeshExpression3D} surface its triangle winding makes the front:
 * the side {@code (v1 - v0) x (v2 - v0)} points to. Without {@link
 * MeshExpression3D#FLAG_FLIP_WINDING} that is the side {@code -(dP/du x dP/dv)} points to.
 *
 * <p>Only the winding is checked here, not the generated normals.
 */
public class MeshExpression3DWindingTest {
    private static final float[][] NONE = new float[0][];
    private static final float[][] NO_DISPLACEMENT = {{0}};
    private static final int FLIP = MeshExpression3D.FLAG_FLIP_WINDING;

    private static final Facing RADIAL = c -> new float[] {c[0], 0, c[2]};
    private static final Facing OUTWARD = c -> c;
    private static final Facing INWARD = c -> new float[] {-c[0], -c[1], -c[2]};
    private static final Facing UP = c -> new float[] {0, 1, 0};
    private static final Facing DOWN = c -> new float[] {0, -1, 0};

    private final HeadlessRemoteContext mContext = new HeadlessRemoteContext();
    private final Recorder mRecorder = new Recorder(mContext);

    @Test
    public void cylinder_withoutFlip_facesOutward() {
        generate(MeshExpression3D.SURFACE_CYLINDER, 0, new float[] {1, 2, 9, 4}, NO_DISPLACEMENT);
        assertFront("cylinder", RADIAL);
    }

    @Test
    public void sphere_withoutFlip_facesInward() {
        generate(MeshExpression3D.SURFACE_SPHERE, 0, new float[] {1, 9, 7}, NO_DISPLACEMENT);
        assertFront("sphere", INWARD);
    }

    @Test
    public void sphere_withFlip_facesOutward() {
        generate(MeshExpression3D.SURFACE_SPHERE, FLIP, new float[] {1, 9, 7}, NO_DISPLACEMENT);
        assertFront("flipped sphere", OUTWARD);
    }

    @Test
    public void heightField_withoutFlip_facesUp() {
        generate(
                MeshExpression3D.SURFACE_HEIGHT_FIELD,
                0,
                new float[] {-1, 1, 5, -1, 1, 5, -10, 10},
                NO_DISPLACEMENT);
        assertFront("height field", UP);
    }

    /** {@code P(u, v) = (u, 0, v)}, so {@code -(dP/du x dP/dv)} is +Y. */
    @Test
    public void general_facesMinusPuCrossPv_unlessFlipped() {
        float[][] pos = {
            {AnimatedFloatExpression.VAR1}, {0}, {AnimatedFloatExpression.VAR2},
        };
        float[] params = {-1, 1, 5, -1, 1, 5};
        generate(MeshExpression3D.SURFACE_GENERAL, 0, params, pos);
        assertFront("general", UP);
        generate(MeshExpression3D.SURFACE_GENERAL, FLIP, params, pos);
        assertFront("flipped general", DOWN);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    /** The direction the front of a triangle centred at {@code c} is expected to face. */
    private interface Facing {
        float @NonNull [] at(float @NonNull [] c);
    }

    private void generate(int type, int flags, float @NonNull [] params, float @NonNull [][] pos) {
        mRecorder.mIndices = null;
        MeshExpression3D op = new MeshExpression3D(1, type, flags, params, pos, NONE, NONE);
        op.updateVariables(mContext);
        op.paint(mRecorder);
    }

    /** Asserts that every non-degenerate triangle of the last mesh faces {@code facing}. */
    private void assertFront(@NonNull String surface, @NonNull Facing facing) {
        int[] indices = mRecorder.mIndices;
        float[] v = mRecorder.mVerts;
        assertNotNull(surface + ": no mesh defined", indices);
        int checked = 0;
        for (int t = 0; t < indices.length; t += 3) {
            int p0 = indices[t] * 3;
            int p1 = indices[t + 1] * 3;
            int p2 = indices[t + 2] * 3;
            float ax = v[p1] - v[p0];
            float ay = v[p1 + 1] - v[p0 + 1];
            float az = v[p1 + 2] - v[p0 + 2];
            float bx = v[p2] - v[p0];
            float by = v[p2 + 1] - v[p0 + 1];
            float bz = v[p2 + 2] - v[p0 + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            if (nx * nx + ny * ny + nz * nz < 1e-10f) {
                continue; // Degenerate, e.g. two corners on a sphere's pole.
            }
            float cx = (v[p0] + v[p1] + v[p2]) / 3;
            float cy = (v[p0 + 1] + v[p1 + 1] + v[p2 + 1]) / 3;
            float cz = (v[p0 + 2] + v[p1 + 2] + v[p2 + 2]) / 3;
            float[] e = facing.at(new float[] {cx, cy, cz});
            float dot = nx * e[0] + ny * e[1] + nz * e[2];
            assertTrue(surface + ": triangle " + t / 3 + " faces the wrong way", dot > 0);
            checked++;
        }
        assertTrue(surface + ": every triangle is degenerate", checked > 0);
    }

    /** Records the mesh a {@link MeshExpression3D} defines. */
    private static final class Recorder extends SoftwareRasterPaintContext
            implements Paint3DContext {
        int @Nullable [] mIndices;
        float @Nullable [] mVerts;

        Recorder(@NonNull HeadlessRemoteContext context) {
            super(context, 4, 4);
        }

        @Override
        public void defineMesh3D(
                int id,
                int @NonNull [] indices,
                float @NonNull [] verts,
                float @Nullable [] normals) {
            defineMesh3D(id, indices, verts, normals, null);
        }

        @Override
        public void defineMesh3D(
                int id,
                int @NonNull [] indices,
                float @NonNull [] verts,
                float @Nullable [] normals,
                float @Nullable [] uv) {
            mIndices = indices.clone();
            mVerts = verts.clone();
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
