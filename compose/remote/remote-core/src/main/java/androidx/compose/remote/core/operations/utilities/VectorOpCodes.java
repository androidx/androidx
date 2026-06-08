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

import androidx.annotation.RestrictTo;

/**
 * Opcodes for {@link VectorRpn}.
 *
 * <p>The scalar opcodes (1..79) are a <b>copy</b> of the values in {@link AnimatedFloatExpression}
 * (which is intentionally left unmodified); copying — rather than referencing — keeps the vector
 * evaluator standalone while staying numerically consistent so a {@code SUB} means the same
 * thing in
 * both evaluators. New <b>vector</b> opcodes start at {@link #OFFSET}{@code + 100}. (Long term
 * these
 * may be refactored into a single shared table.)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class VectorOpCodes {
    private VectorOpCodes() {
    }

    /** Base offset for all opcodes; matches {@code AnimatedFloatExpression.OFFSET}. */
    public static final int OFFSET = 0x310_000;

    /** Encode an opcode id as a NaN-tagged float (matches the scalar evaluator's scheme). */
    public static float op(int code) {
        return Float.intBitsToFloat(code | 0xFF80_0000);
    }

    /** Decode the opcode id from a NaN-tagged float. */
    public static int fromNaN(float v) {
        return Float.floatToRawIntBits(v) & 0x7F_FFFF;
    }

    /**
     * True iff {@code v} is one of the vector-specific opcodes (OFFSET+100..). These live above the
     * scalar op range, so callers that classify tokens as operator-vs-variable (e.g. variable
     * resolution) must treat these as operators too — {@code AnimatedFloatExpression
     * .isMathOperator}
     * does not recognize them.
     */
    public static boolean isVectorOp(float v) {
        if (!Float.isNaN(v)) {
            return false;
        }
        int pos = fromNaN(v);
        return pos >= OP_VBUILD2 && pos <= OP_VNORM;
    }

    // ---- scalar opcodes (copied from AnimatedFloatExpression, identical values) ----
    public static final int OP_ADD = OFFSET + 1;
    public static final int OP_SUB = OFFSET + 2;
    public static final int OP_MUL = OFFSET + 3;
    public static final int OP_DIV = OFFSET + 4;
    public static final int OP_MOD = OFFSET + 5;
    public static final int OP_MIN = OFFSET + 6;
    public static final int OP_MAX = OFFSET + 7;
    public static final int OP_POW = OFFSET + 8;
    public static final int OP_SQRT = OFFSET + 9;
    public static final int OP_ABS = OFFSET + 10;
    public static final int OP_SIGN = OFFSET + 11;
    public static final int OP_COPY_SIGN = OFFSET + 12;
    public static final int OP_EXP = OFFSET + 13;
    public static final int OP_FLOOR = OFFSET + 14;
    public static final int OP_LOG = OFFSET + 15;
    public static final int OP_LN = OFFSET + 16;
    public static final int OP_ROUND = OFFSET + 17;
    public static final int OP_SIN = OFFSET + 18;
    public static final int OP_COS = OFFSET + 19;
    public static final int OP_TAN = OFFSET + 20;
    public static final int OP_ASIN = OFFSET + 21;
    public static final int OP_ACOS = OFFSET + 22;
    public static final int OP_ATAN = OFFSET + 23;
    public static final int OP_ATAN2 = OFFSET + 24;
    public static final int OP_MAD = OFFSET + 25;
    public static final int OP_TERNARY_CONDITIONAL = OFFSET + 26;
    public static final int OP_CLAMP = OFFSET + 27;
    public static final int OP_CBRT = OFFSET + 28;
    public static final int OP_DEG = OFFSET + 29;
    public static final int OP_RAD = OFFSET + 30;
    public static final int OP_CEIL = OFFSET + 31;
    public static final int OP_A_DEREF = OFFSET + 32;
    public static final int OP_A_MAX = OFFSET + 33;
    public static final int OP_A_MIN = OFFSET + 34;
    public static final int OP_A_SUM = OFFSET + 35;
    public static final int OP_A_AVG = OFFSET + 36;
    public static final int OP_A_LEN = OFFSET + 37;
    public static final int OP_A_SPLINE = OFFSET + 38;
    public static final int OP_RAND = OFFSET + 39;
    public static final int OP_RAND_SEED = OFFSET + 40;
    public static final int OP_NOISE_FROM = OFFSET + 41;
    public static final int OP_RAND_IN_RANGE = OFFSET + 42;
    public static final int OP_SQUARE_SUM = OFFSET + 43;
    public static final int OP_STEP = OFFSET + 44;
    public static final int OP_SQUARE = OFFSET + 45;
    public static final int OP_DUP = OFFSET + 46;
    public static final int OP_HYPOT = OFFSET + 47;
    public static final int OP_SWAP = OFFSET + 48;
    public static final int OP_LERP = OFFSET + 49;
    public static final int OP_SMOOTH_STEP = OFFSET + 50;
    public static final int OP_LOG2 = OFFSET + 51;
    public static final int OP_INV = OFFSET + 52;
    public static final int OP_FRACT = OFFSET + 53;
    public static final int OP_PINGPONG = OFFSET + 54;
    public static final int OP_NOP = OFFSET + 55;
    public static final int OP_STORE_R0 = OFFSET + 56;
    public static final int OP_STORE_R1 = OFFSET + 57;
    public static final int OP_STORE_R2 = OFFSET + 58;
    public static final int OP_STORE_R3 = OFFSET + 59;
    public static final int OP_LOAD_R0 = OFFSET + 60;
    public static final int OP_LOAD_R1 = OFFSET + 61;
    public static final int OP_LOAD_R2 = OFFSET + 62;
    public static final int OP_LOAD_R3 = OFFSET + 63;
    public static final int OP_CMD1 = OFFSET + 64;
    public static final int OP_CMD2 = OFFSET + 65;
    public static final int OP_FIRST_VAR = OFFSET + 70;
    public static final int OP_SECOND_VAR = OFFSET + 71;
    public static final int OP_THIRD_VAR = OFFSET + 72;
    public static final int OP_CHANGE_SIGN = OFFSET + 73;
    public static final int OP_CUBIC = OFFSET + 74;
    public static final int OP_A_SPLINE_LOOP = OFFSET + 75;
    public static final int OP_A_SUM_TILL = OFFSET + 76;
    public static final int OP_A_SUM_XY = OFFSET + 77;
    public static final int OP_A_SUM_SQR = OFFSET + 78;
    public static final int OP_A_LERP = OFFSET + 79;

    // ---- vector opcodes (new, start at OFFSET + 100) ----
    /** Pop 2 scalars, assemble a vec2. */
    public static final int OP_VBUILD2 = OFFSET + 100;
    /** Pop 3 scalars, assemble a vec3. */
    public static final int OP_VBUILD3 = OFFSET + 101;
    /** Pop 4 scalars, assemble a vec4. */
    public static final int OP_VBUILD4 = OFFSET + 102;
    /** vecN · vecN -> scalar. */
    public static final int OP_VDOT = OFFSET + 103;
    /** vec3 × vec3 -> vec3. */
    public static final int OP_VCROSS = OFFSET + 104;
    /** |v| -> scalar. */
    public static final int OP_VLEN = OFFSET + 105;
    /** |v|² -> scalar (no sqrt). */
    public static final int OP_VLENSQ = OFFSET + 106;
    /** v / |v| -> vecN (softened denominator). */
    public static final int OP_VNORM = OFFSET + 107;

    // ---- NaN-tagged float forms, for building programs in the writer/tests ----
    public static final float ADD = op(OP_ADD);
    public static final float SUB = op(OP_SUB);
    public static final float MUL = op(OP_MUL);
    public static final float DIV = op(OP_DIV);
    public static final float MOD = op(OP_MOD);
    public static final float MIN = op(OP_MIN);
    public static final float MAX = op(OP_MAX);
    public static final float POW = op(OP_POW);
    public static final float SQRT = op(OP_SQRT);
    public static final float ABS = op(OP_ABS);
    public static final float SQUARE = op(OP_SQUARE);
    public static final float SIN = op(OP_SIN);
    public static final float COS = op(OP_COS);
    public static final float FLOOR = op(OP_FLOOR);
    public static final float CEIL = op(OP_CEIL);
    public static final float ROUND = op(OP_ROUND);
    public static final float CHANGE_SIGN = op(OP_CHANGE_SIGN);
    public static final float INV = op(OP_INV);

    public static final float VBUILD2 = op(OP_VBUILD2);
    public static final float VBUILD3 = op(OP_VBUILD3);
    public static final float VBUILD4 = op(OP_VBUILD4);
    public static final float VDOT = op(OP_VDOT);
    public static final float VCROSS = op(OP_VCROSS);
    public static final float VLEN = op(OP_VLEN);
    public static final float VLENSQ = op(OP_VLENSQ);
    public static final float VNORM = op(OP_VNORM);
}
