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
package androidx.compose.remote.core.operations.utilities;

import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OFFSET;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_ABS;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_ADD;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_CEIL;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_CHANGE_SIGN;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_COS;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_DIV;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_FLOOR;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_INV;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_MAX;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_MIN;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_MOD;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_MUL;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_NOP;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_POW;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_ROUND;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_SIN;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_SQRT;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_SQUARE;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_SUB;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VBUILD2;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VBUILD3;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VBUILD4;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VCROSS;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VDOT;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VLEN;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VLENSQ;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.OP_VNORM;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.fromNaN;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Reverse-polish evaluator for <b>vector-valued</b> expressions — the vector analog of
 * {@link AnimatedFloatExpression}. Standalone copy of that evaluator's structure (sharing only the
 * opcode numbering via {@link VectorOpCodes}); {@code AnimatedFloatExpression} is left unmodified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VectorRpn {
    /** Fixed vector width (vec4 / rgba). Every stack value occupies this many lanes. */
    public static final int MAX_DIM = 4;

    private static final int MAX_STACK = 64;
    private static final int MAX_PROGRAM = 512;
    private static final float SOFT_DOMAIN_EPS = 1e-6f;

    private final float[] mProgram = new float[MAX_PROGRAM];
    private final float[] mStack = new float[MAX_STACK * MAX_DIM];
    private final int[] mDim = new int[MAX_STACK]; // logical dimensionality of each stack value

    /**
     * When true, divide/normalize substitute a tiny denominator for a (near-)zero one instead of
     * producing Inf/NaN. Off by default (exact); the owning op flips it for a corrective retry.
     */
    public boolean mSoftDomain = false;

    /**
     * Evaluate {@code program[0..len)} and write the result's components into {@code out} (length ≥
     * {@link #MAX_DIM}).
     *
     * @return the logical dimensionality of the result (1 = scalar, 2/3/4 = vector)
     */
    public int apply(float @NonNull [] program, int len, float @NonNull [] out) {
        System.arraycopy(program, 0, mProgram, 0, len); // local copy: cache locality
        final float[] s = mStack;
        int sp = -1;
        for (int i = 0; i < len; i++) {
            float v = mProgram[i];
            if (Float.isNaN(v)) {
                sp = opEval(sp, fromNaN(v));
            } else {
                sp++;
                int p = sp * MAX_DIM;
                s[p] = v;
                s[p + 1] = v;
                s[p + 2] = v;
                s[p + 3] = v; // scalar = broadcast across all lanes
                mDim[sp] = 1;
            }
        }
        int p = sp * MAX_DIM;
        out[0] = s[p];
        out[1] = s[p + 1];
        out[2] = s[p + 2];
        out[3] = s[p + 3];
        return mDim[sp];
    }

    private int opEval(int sp, int id) {
        final float[] s = mStack;
        final int a = (sp - 1) * MAX_DIM; // lhs / binary result base
        final int b = sp * MAX_DIM; // rhs / unary operand base
        switch (id) {
            case OP_ADD:
                s[a] += s[b];
                s[a + 1] += s[b + 1];
                s[a + 2] += s[b + 2];
                s[a + 3] += s[b + 3];
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_SUB:
                s[a] -= s[b];
                s[a + 1] -= s[b + 1];
                s[a + 2] -= s[b + 2];
                s[a + 3] -= s[b + 3];
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_MUL:
                s[a] *= s[b];
                s[a + 1] *= s[b + 1];
                s[a + 2] *= s[b + 2];
                s[a + 3] *= s[b + 3];
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_DIV:
                s[a] /= softDenom(s[b]);
                s[a + 1] /= softDenom(s[b + 1]);
                s[a + 2] /= softDenom(s[b + 2]);
                s[a + 3] /= softDenom(s[b + 3]);
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_MOD:
                s[a] %= softDenom(s[b]);
                s[a + 1] %= softDenom(s[b + 1]);
                s[a + 2] %= softDenom(s[b + 2]);
                s[a + 3] %= softDenom(s[b + 3]);
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_MIN:
                s[a] = Math.min(s[a], s[b]);
                s[a + 1] = Math.min(s[a + 1], s[b + 1]);
                s[a + 2] = Math.min(s[a + 2], s[b + 2]);
                s[a + 3] = Math.min(s[a + 3], s[b + 3]);
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_MAX:
                s[a] = Math.max(s[a], s[b]);
                s[a + 1] = Math.max(s[a + 1], s[b + 1]);
                s[a + 2] = Math.max(s[a + 2], s[b + 2]);
                s[a + 3] = Math.max(s[a + 3], s[b + 3]);
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;
            case OP_POW:
                s[a] = (float) Math.pow(s[a], s[b]);
                s[a + 1] = (float) Math.pow(s[a + 1], s[b + 1]);
                s[a + 2] = (float) Math.pow(s[a + 2], s[b + 2]);
                s[a + 3] = (float) Math.pow(s[a + 3], s[b + 3]);
                mDim[sp - 1] = Math.max(mDim[sp - 1], mDim[sp]);
                return sp - 1;

            case OP_SQRT:
                s[b] = (float) Math.sqrt(s[b]);
                s[b + 1] = (float) Math.sqrt(s[b + 1]);
                s[b + 2] = (float) Math.sqrt(s[b + 2]);
                s[b + 3] = (float) Math.sqrt(s[b + 3]);
                return sp;
            case OP_ABS:
                s[b] = Math.abs(s[b]);
                s[b + 1] = Math.abs(s[b + 1]);
                s[b + 2] = Math.abs(s[b + 2]);
                s[b + 3] = Math.abs(s[b + 3]);
                return sp;
            case OP_SQUARE:
                s[b] *= s[b];
                s[b + 1] *= s[b + 1];
                s[b + 2] *= s[b + 2];
                s[b + 3] *= s[b + 3];
                return sp;
            case OP_SIN:
                s[b] = (float) Math.sin(s[b]);
                s[b + 1] = (float) Math.sin(s[b + 1]);
                s[b + 2] = (float) Math.sin(s[b + 2]);
                s[b + 3] = (float) Math.sin(s[b + 3]);
                return sp;
            case OP_COS:
                s[b] = (float) Math.cos(s[b]);
                s[b + 1] = (float) Math.cos(s[b + 1]);
                s[b + 2] = (float) Math.cos(s[b + 2]);
                s[b + 3] = (float) Math.cos(s[b + 3]);
                return sp;
            case OP_FLOOR:
                s[b] = (float) Math.floor(s[b]);
                s[b + 1] = (float) Math.floor(s[b + 1]);
                s[b + 2] = (float) Math.floor(s[b + 2]);
                s[b + 3] = (float) Math.floor(s[b + 3]);
                return sp;
            case OP_CEIL:
                s[b] = (float) Math.ceil(s[b]);
                s[b + 1] = (float) Math.ceil(s[b + 1]);
                s[b + 2] = (float) Math.ceil(s[b + 2]);
                s[b + 3] = (float) Math.ceil(s[b + 3]);
                return sp;
            case OP_ROUND:
                s[b] = Math.round(s[b]);
                s[b + 1] = Math.round(s[b + 1]);
                s[b + 2] = Math.round(s[b + 2]);
                s[b + 3] = Math.round(s[b + 3]);
                return sp;
            case OP_CHANGE_SIGN:
                s[b] = -s[b];
                s[b + 1] = -s[b + 1];
                s[b + 2] = -s[b + 2];
                s[b + 3] = -s[b + 3];
                return sp;
            case OP_INV:
                s[b] = 1f / softDenom(s[b]);
                s[b + 1] = 1f / softDenom(s[b + 1]);
                s[b + 2] = 1f / softDenom(s[b + 2]);
                s[b + 3] = 1f / softDenom(s[b + 3]);
                return sp;

            case OP_VBUILD2:
                return build(sp, 2);
            case OP_VBUILD3:
                return build(sp, 3);
            case OP_VBUILD4:
                return build(sp, 4);
            case OP_VDOT: {
                // zero-padded high lanes contribute 0, so summing all 4 is correct for vec2/3/4
                float d = s[a] * s[b] + s[a + 1] * s[b + 1] + s[a + 2] * s[b + 2]
                        + s[a + 3] * s[b + 3];
                s[a] = d;
                s[a + 1] = d;
                s[a + 2] = d;
                s[a + 3] = d;
                mDim[sp - 1] = 1;
                return sp - 1;
            }
            case OP_VCROSS: {
                float ax = s[a], ay = s[a + 1], az = s[a + 2];
                float bx = s[b], by = s[b + 1], bz = s[b + 2];
                s[a] = ay * bz - az * by;
                s[a + 1] = az * bx - ax * bz;
                s[a + 2] = ax * by - ay * bx;
                s[a + 3] = 0f;
                mDim[sp - 1] = 3;
                return sp - 1;
            }
            case OP_VLEN:
            case OP_VLENSQ: {
                float sq = s[b] * s[b] + s[b + 1] * s[b + 1] + s[b + 2] * s[b + 2]
                        + s[b + 3] * s[b + 3];
                float r = (id == OP_VLEN) ? (float) Math.sqrt(sq) : sq;
                s[b] = r;
                s[b + 1] = r;
                s[b + 2] = r;
                s[b + 3] = r;
                mDim[sp] = 1;
                return sp;
            }
            case OP_VNORM: {
                float sq = s[b] * s[b] + s[b + 1] * s[b + 1] + s[b + 2] * s[b + 2]
                        + s[b + 3] * s[b + 3];
                float inv = 1f / softDenom((float) Math.sqrt(sq));
                s[b] *= inv;
                s[b + 1] *= inv;
                s[b + 2] *= inv;
                s[b + 3] *= inv;
                return sp;
            }

            case OP_NOP:
                return sp; // produced by two-body first()/second() substitution

            default:
                throw new UnsupportedOperationException(
                        "VectorRpn op not implemented: " + (id - OFFSET));
        }
    }

    /**
     * Pop {@code n} scalars (lanes 0..n-1 of the top n slots) into one vec{n}; zero-pad the
     * rest.
     */
    private int build(int sp, int n) {
        final float[] s = mStack;
        int base = sp - n + 1;
        // gather lane 0 of each of the n scalar slots into a temp register set
        float c0 = s[base * MAX_DIM];
        float c1 = (n > 1) ? s[(base + 1) * MAX_DIM] : 0f;
        float c2 = (n > 2) ? s[(base + 2) * MAX_DIM] : 0f;
        float c3 = (n > 3) ? s[(base + 3) * MAX_DIM] : 0f;
        int p = base * MAX_DIM;
        s[p] = c0;
        s[p + 1] = c1;
        s[p + 2] = c2;
        s[p + 3] = c3; // unused high lanes are zero so dot/len are correct over all 4
        mDim[base] = n;
        return base;
    }

    private float softDenom(float denom) {
        if (mSoftDomain && denom < SOFT_DOMAIN_EPS && denom > -SOFT_DOMAIN_EPS) {
            return (denom < 0f) ? -SOFT_DOMAIN_EPS : SOFT_DOMAIN_EPS;
        }
        return denom;
    }
}
