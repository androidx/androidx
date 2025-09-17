/*
 * Copyright (C) 2023 The Android Open Source Project
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** high performance matrix processing engine */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MatrixOperations {

    /** The START POINT in the float NaN space for operators */
    public static final int OFFSET = 0x320_000;

    /** add identity operator */
    public static final float IDENTITY = asNan(OFFSET + 1);

    /** ROT z axis operator */
    public static final float ROT_X = asNan(OFFSET + 2);

    /** ROT z axis operator */
    public static final float ROT_Y = asNan(OFFSET + 3);

    /** ROT z axis operator */
    public static final float ROT_Z = asNan(OFFSET + 4);

    /** TRANSLATE x axis operator */
    public static final float TRANSLATE_X = asNan(OFFSET + 5);

    /** TRANSLATE y axis operator */
    public static final float TRANSLATE_Y = asNan(OFFSET + 6);

    /** TRANSLATE z axis operator */
    public static final float TRANSLATE_Z = asNan(OFFSET + 7);

    /** TRANSLATE x,y axis operator */
    public static final float TRANSLATE2 = asNan(OFFSET + 8);

    /** TRANSLATE x,y,z axis operator */
    public static final float TRANSLATE3 = asNan(OFFSET + 9);

    /** SCALE X axis operator */
    public static final float SCALE_X = asNan(OFFSET + 10);

    /** SCALE Y axis operator */
    public static final float SCALE_Y = asNan(OFFSET + 11);

    /** SCALE Z axis operator */
    public static final float SCALE_Z = asNan(OFFSET + 12);

    /** SCALE2 operator */
    public static final float SCALE2 = asNan(OFFSET + 13);

    /** SCALE3 operator */
    public static final float SCALE3 = asNan(OFFSET + 14);

    /** Multiply operator */
    public static final float MUL = asNan(OFFSET + 15);

    /** ROT about pivot z axis operator */
    public static final float ROT_PZ = asNan(OFFSET + 16);

    /** Rotate about a vector operator */
    public static final float ROT_AXIS = asNan(OFFSET + 17);

    /** add a projection matrix */
    public static final float PROJECTION = asNan(OFFSET + 18);

    /** LAST valid operator */
    public static final int LAST_OP = OFFSET + 54;

    /** VAR1 operator */
    // TODO SQUARE, DUP, HYPOT, SWAP
    //    private static final float FP_PI = (float) Math.PI;
    // private static final float FP_TO_RAD = 57.29578f; // 180/PI

    // private static final float FP_TO_DEG = 0.017453292f; // 180/PI
    @NonNull static IntMap<String> sNames = new IntMap<>();

    Matrix[] mMatrices = new Matrix[10];
    Matrix mTmpMatrix = new Matrix();
    int mMatrixIndex = -1;

    {
        for (int i = 0; i < mMatrices.length; i++) {
            mMatrices[i] = new Matrix(4, 4);
        }
    }

    float @NonNull [] mStack = new float[0];
    // float @NonNull [] mLocalStack = new float[128];
    float @NonNull [] mVar = new float[0];

    // @Nullable CollectionsAccess mCollectionsAccess;
    // IntMap<MonotonicSpline> mSplineMap = new IntMap<>();
    // private static Random sRandom;

    /**
     * Get the max op for a given API level
     *
     * @param level
     * @return
     */
    public static int getMaxOpForLevel(int level) {
        if (level == 7) {
            return LAST_OP;
        } else {
            return 0;
        }
    }

    /**
     * is float a math operator
     *
     * @param v
     * @return
     */
    public static boolean isOperator(float v) {
        if (Float.isNaN(v)) {
            int pos = fromNaN(v);
            // a data variable is a type of math operator for expressions
            // it dereference to a value
            if (NanMap.isDataVariable(v)) {
                return false;
            }
            return pos > OFFSET && pos <= LAST_OP;
        }
        return false;
    }

    interface Op {
        int eval(int sp);
    }

    /**
     * Evaluate the matrix expression
     *
     * @param exp expression encoded as an array of floats
     * @param var variables
     * @return resulting Matrix object
     */
    public @NonNull Matrix eval(float @NonNull [] exp, float @NonNull ... var) {
        mStack = exp;
        mVar = var;
        mMatrixIndex = 0;
        mMatrices[mMatrixIndex].setIdentity();
        for (int i = 0; i < mStack.length; i++) {
            float v = mStack[i];
            if (Float.isNaN(v)) {
                opEval(i, fromNaN(v));
            }
        }
        return mMatrices[0];
    }

    static {
        int k = 0;
        sNames.put(k++, "NOP");
        sNames.put(k++, "+");
        sNames.put(k++, "-");
        sNames.put(k++, "*");
        sNames.put(k++, "/");
        sNames.put(k++, "%");
        sNames.put(k++, "min");
        sNames.put(k++, "max");
        sNames.put(k++, "pow");
        sNames.put(k++, "sqrt");
        sNames.put(k++, "abs");
        sNames.put(k++, "sign");
        sNames.put(k++, "copySign");
        sNames.put(k++, "exp");
        sNames.put(k++, "floor");
        sNames.put(k++, "log");
        sNames.put(k++, "ln");
        sNames.put(k++, "round");
        sNames.put(k++, "sin");
        sNames.put(k++, "cos");
        sNames.put(k++, "tan");
        sNames.put(k++, "asin");
    }

    /**
     * given a float command return its math name (e.g sin, cos etc.)
     *
     * @param f
     * @return
     */
    @Nullable
    public static String toMathName(float f) {
        int id = fromNaN(f) - OFFSET;
        return sNames.get(id);
    }

    /**
     * Convert an expression encoded as an array of floats int ot a string
     *
     * @param exp
     * @param labels
     * @return
     */
    @NonNull
    public static String toString(float @NonNull [] exp, @Nullable String[] labels) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < exp.length; i++) {
            float v = exp[i];
            if (Float.isNaN(v)) {
                if (isOperator(v)) {
                    s.append(toMathName(v));
                } else {
                    int id = fromNaN(v);
                    String idString =
                            (id > NanMap.ID_REGION_ARRAY) ? ("A_" + (id & 0xFFFFF)) : "" + id;
                    s.append("[");
                    s.append(idString);
                    s.append("]");
                }
            } else {
                if (labels != null && labels[i] != null) {
                    s.append(labels[i]);
                    if (!labels[i].contains("_")) {
                        s.append(v);
                    }
                } else {
                    s.append(v);
                }
            }
            s.append(" ");
        }
        return s.toString();
    }

    static String toString(float @NonNull [] exp, int sp) {
        //        String[] str = new String[exp.length];
        if (Float.isNaN(exp[sp])) {
            int id = fromNaN(exp[sp]) - OFFSET;
            switch (NO_OF_OPS[id]) {
                case -1:
                    return "nop";
                case 1:
                    return sNames.get(id) + "(" + toString(exp, sp + 1) + ") ";
                case 2:
                    if (infix(id)) {
                        return "("
                                + toString(exp, sp + 1)
                                + sNames.get(id)
                                + " "
                                + toString(exp, sp + 2)
                                + ") ";
                    } else {
                        return sNames.get(id)
                                + "("
                                + toString(exp, sp + 1)
                                + ", "
                                + toString(exp, sp + 2)
                                + ")";
                    }
                case 3:
                    if (infix(id)) {
                        return "(("
                                + toString(exp, sp + 1)
                                + ") ? "
                                + toString(exp, sp + 2)
                                + ":"
                                + toString(exp, sp + 3)
                                + ")";
                    } else {
                        return sNames.get(id)
                                + "("
                                + toString(exp, sp + 1)
                                + ", "
                                + toString(exp, sp + 2)
                                + ", "
                                + toString(exp, sp + 3)
                                + ")";
                    }
            }
        }
        return Float.toString(exp[sp]);
    }

    static final int[] NO_OF_OPS = {
        -1, // no op
        2, 2, 2, 2, 2, // + - * / %
        2, 2, 2, // min max, power
        1, 1, 1, 1, 1, 1, 1, 1, // sqrt,abs,CopySign,exp,floor,log,ln
        1, 1, 1, 1, 1, 1, 1, 2, // round,sin,cos,tan,asin,acos,atan,atan2
        3, 3, 3, 1, 1, 1, 1, 0, 0, 0, // mad, ?:, clamp, cbrt, deg, rad, ceil , a[0],a[1],a[2]
        1, // log2
        1, // inv
        1, // fract
        2, // ping_pong
    };

    /**
     * to be used by parser to determine if command is infix
     *
     * @param n
     * @return
     */
    static boolean infix(int n) {
        return ((n < 6) || (n == 25) || (n == 26));
    }

    /**
     * Convert an id into a NaN object
     *
     * @param v
     * @return
     */
    public static float asNan(int v) {
        return Float.intBitsToFloat(v | -0x800000);
    }

    /**
     * Get ID from a NaN float
     *
     * @param v
     * @return
     */
    public static int fromNaN(float v) {
        int b = Float.floatToRawIntBits(v);
        return b & 0x7FFFFF;
    }

    // ================= New approach ========
    private static final int OP_IDENTITY = OFFSET + 1;
    private static final int OP_ROT_X = OFFSET + 2;
    private static final int OP_ROT_Y = OFFSET + 3;
    private static final int OP_ROT_Z = OFFSET + 4;
    private static final int OP_TRANSLATE_X = OFFSET + 5;
    private static final int OP_TRANSLATE_Y = OFFSET + 6;
    private static final int OP_TRANSLATE_Z = OFFSET + 7;
    private static final int OP_TRANSLATE2 = OFFSET + 8;
    private static final int OP_TRANSLATE3 = OFFSET + 9;

    private static final int OP_SCALE_X = OFFSET + 10;
    private static final int OP_SCALE_Y = OFFSET + 11;
    private static final int OP_SCALE_Z = OFFSET + 12;
    private static final int OP_SCALE2 = OFFSET + 13;
    private static final int OP_SCALE3 = OFFSET + 14;

    private static final int OP_MUL = OFFSET + 15;
    private static final int OP_ROT_PZ = OFFSET + 16;
    private static final int OP_ROT_AXIS = OFFSET + 17;
    private static final int OP_PROJECTION = OFFSET + 18;

    void opEval(int sp, int id) {
        switch (id) {
            case OP_IDENTITY:
                mMatrices[++mMatrixIndex].setIdentity();
                return;
            case OP_ROT_X:
                mMatrices[mMatrixIndex].rotateX(mStack[sp - 1]);
                return;
            case OP_ROT_Y:
                mMatrices[mMatrixIndex].rotateY(mStack[sp - 1]);
                return;
            case OP_ROT_Z:
                mMatrices[mMatrixIndex].rotateZ(mStack[sp - 1]);
                return;
            case OP_TRANSLATE_X:
                mMatrices[mMatrixIndex].translate(mStack[sp - 1], 0, 0);
                return;
            case OP_TRANSLATE_Y:
                mMatrices[mMatrixIndex].translate(0, mStack[sp - 1], 0);
                return;
            case OP_TRANSLATE_Z:
                mMatrices[mMatrixIndex].translate(0, 0, mStack[sp - 1]);
                return;
            case OP_TRANSLATE2:
                mMatrices[mMatrixIndex].translate(mStack[sp - 2], mStack[sp - 1], 0);
                return;
            case OP_TRANSLATE3:
                mMatrices[mMatrixIndex].translate(mStack[sp - 3], mStack[sp - 2], mStack[sp - 1]);
                return;
            case OP_SCALE_X:
                mMatrices[mMatrixIndex].setScale(mStack[sp - 1], 1, 1);
                return;
            case OP_SCALE_Y:
                mMatrices[mMatrixIndex].setScale(1, mStack[sp - 1], 1);
                return;
            case OP_SCALE_Z:
                mMatrices[mMatrixIndex].setScale(1, 1, mStack[sp - 1]);
                return;
            case OP_SCALE2:
                mMatrices[mMatrixIndex].setScale(mStack[sp - 2], mStack[sp - 1], 0);
                return;
            case OP_SCALE3:
                mMatrices[mMatrixIndex].setScale(mStack[sp - 3], mStack[sp - 2], mStack[sp - 1]);
                return;
            case OP_MUL:
                Matrix.multiply(mMatrices[mMatrixIndex - 1], mMatrices[mMatrixIndex], mTmpMatrix);
                mMatrices[mMatrixIndex - 1].copyFrom(mTmpMatrix);
                mMatrixIndex--;
                return;
            case OP_ROT_PZ:
                mMatrices[mMatrixIndex].rotateZ(mStack[sp - 2], mStack[sp - 1], mStack[sp - 3]);
                //  angle, pivot x, pivot y , ROT_PZ
                return;
            case OP_ROT_AXIS: // angle, x, y, z, ROT_AXIS
                mMatrices[mMatrixIndex].rotateAroundAxis(
                        mStack[sp - 3], mStack[sp - 2], mStack[sp - 1], mStack[sp - 4]);
                return;
            case OP_PROJECTION: // fovDegrees, aspectRatio, near, far, PROJECTION
                mMatrices[mMatrixIndex].projection(
                        mStack[sp - 4], mStack[sp - 3], mStack[sp - 2], mStack[sp - 1]);
                return;
        }
    }
}
