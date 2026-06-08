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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.ADD;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.MUL;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VBUILD3;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VCROSS;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VDOT;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VLEN;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VLENSQ;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VNORM;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.utilities.VectorRpn;

import org.junit.Test;

public class VectorRpnTest {

    private final VectorRpn mE = new VectorRpn();
    private final float[] mOut = new float[VectorRpn.MAX_DIM];

    /** Evaluate a program and return the result components (length = result lanes). */
    private float[] eval(float[] program) {
        int lanes = mE.apply(program, program.length, mOut);
        float[] r = new float[lanes];
        System.arraycopy(mOut, 0, r, 0, lanes);
        return r;
    }

    @Test
    public void positionIntegrate_pPlusVdt() {
        // p=(1,2,3); v=(0.5,0.5,0.5); dt=2  ->  p + v*dt = (2,3,4)
        // RPN: p, v, dt, MUL, ADD   (MUL broadcasts the scalar dt across the vec3)
        float[] prog = {
            1f, 2f, 3f, VBUILD3,
            0.5f, 0.5f, 0.5f, VBUILD3,
            2f, MUL,
            ADD
        };
        assertArrayEquals(new float[] {2f, 3f, 4f}, eval(prog), 1e-6f);
    }

    @Test
    public void scalarBroadcast_bothOrders() {
        // vec3 * scalar  and  scalar * vec3 both broadcast
        assertArrayEquals(
                new float[] {2f, 4f, 6f},
                eval(new float[] {1f, 2f, 3f, VBUILD3, 2f, MUL}),
                1e-6f);
        assertArrayEquals(
                new float[] {2f, 4f, 6f},
                eval(new float[] {2f, 1f, 2f, 3f, VBUILD3, MUL}),
                1e-6f);
    }

    @Test
    public void dotProduct() {
        // (1,2,3)·(4,5,6) = 32, a scalar (1 lane)
        float[] r = eval(new float[] {1f, 2f, 3f, VBUILD3, 4f, 5f, 6f, VBUILD3, VDOT});
        assertEquals(1, r.length);
        assertEquals(32f, r[0], 1e-6f);
    }

    @Test
    public void crossProduct() {
        // x̂ × ŷ = ẑ
        assertArrayEquals(
                new float[] {0f, 0f, 1f},
                eval(new float[] {1f, 0f, 0f, VBUILD3, 0f, 1f, 0f, VBUILD3, VCROSS}),
                1e-6f);
    }

    @Test
    public void lengthAndLengthSquared() {
        float[] len = eval(new float[] {3f, 4f, 0f, VBUILD3, VLEN});
        assertEquals(1, len.length);
        assertEquals(5f, len[0], 1e-6f);

        float[] lensq = eval(new float[] {3f, 4f, 0f, VBUILD3, VLENSQ});
        assertEquals(25f, lensq[0], 1e-6f);
    }

    @Test
    public void normalize() {
        assertArrayEquals(
                new float[] {0.6f, 0.8f, 0f},
                eval(new float[] {3f, 4f, 0f, VBUILD3, VNORM}),
                1e-6f);
    }

    @Test
    public void scalarOnlyParity() {
        // A pure-scalar program (lanes == 1) runs through VectorRpn just like the scalar evaluator.
        float[] r = eval(new float[] {3f, 5f, ADD});
        assertEquals(1, r.length);
        assertEquals(8f, r[0], 0f);
    }

    @Test
    public void softDomainNormalizeOfZeroIsFinite() {
        // Normalizing a zero-length vector would divide by zero. With soft-domain on, the result is
        // finite (no NaN poisons the particle that would hold it).
        mE.mSoftDomain = true;
        float[] r = eval(new float[] {0f, 0f, 0f, VBUILD3, VNORM});
        assertEquals(3, r.length);
        for (float c : r) {
            assertTrue("expected finite, got " + c, !Float.isNaN(c) && !Float.isInfinite(c));
        }
    }
}
