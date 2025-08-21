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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.AnimatedFloatTestUtils.expToFloatExp;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ABS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ACOS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ASIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ATAN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ATAN2;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.CBRT;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.CEIL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.CLAMP;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.COPY_SIGN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DEG;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.EXP;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.FLOOR;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.FRACT;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.IFELSE;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.INV;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.LN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.LOG;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.LOG2;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MAD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.PINGPONG;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.POW;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND_SEED;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ROUND;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SIGN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SQRT;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.TAN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.text.DecimalFormat;

public class AnimatedFloatExpressionTest {

    @Test
    public void simpleTest() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        // (3+5)*(2-8) -> [3, 5, +, 2, 8, -, *]
        float[] rpn = new float[] {3, 5, ADD, 2, 8, SUB, MUL};
        assertEquals(-48.0f, e.eval(rpn), 0f);
        e.evalDB(new float[] {3, 5, ADD, 2, 8, SUB, MUL});
    }

    @Test
    public void allOperatorsTest() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        assertEquals(2 + 3f, e.eval(expToFloatExp("2 + 3")), 0f);
        assertEquals((float) Math.sin(2 / 3f), e.eval(expToFloatExp("sin(2 / 3)")), 0f);
        assertEquals((float) Math.cos(2 / 3f), e.eval(expToFloatExp("cos(2 / 3)")), 0f);
        assertEquals((float) Math.tan(2 / 3f), e.eval(expToFloatExp("tan(2 / 3)")), 0f);
        assertEquals((float) Math.sin(2 / 3f), e.eval(expToFloatExp("sin(2 / 3)")), 0f);
        assertEquals((float) Math.min(2f, 3f), e.eval(expToFloatExp("min(2 , 3)")), 0f);
        assertEquals((float) Math.max(2f, 3f), e.eval(expToFloatExp("max(2 , 3)")), 0f);
        assertEquals((float) Math.pow(2f, 3f), e.eval(expToFloatExp("pow(2 , 3)")), 0f);
        assertEquals((float) Math.sqrt(4f), e.eval(expToFloatExp("sqrt(4)")), 0f);
        assertEquals((float) Math.abs(-4f), e.eval(expToFloatExp("abs(-4)")), 0f);
        assertEquals((float) Math.signum(-4f), e.eval(expToFloatExp("sign(-4)")), 0f);
        assertEquals((float) Math.copySign(3f, -4f), e.eval(expToFloatExp("copySign(3,-4)")), 0f);
        assertEquals((float) Math.copySign(3f, 4f), e.eval(expToFloatExp("copySign(3, 4)")), 0f);
        assertEquals((float) Math.exp(-4f), e.eval(expToFloatExp("exp(-4)")), 0f);
        assertEquals((float) Math.floor(-4f), e.eval(expToFloatExp("floor(-4)")), 0f);
        assertEquals((float) Math.log(4f), e.eval(expToFloatExp("ln(4)")), 0f);
        assertEquals((float) Math.log10(4f), e.eval(expToFloatExp("log(4)")), 0f);
        assertEquals((float) Math.round(4.45f), e.eval(expToFloatExp("round(4.45)")), 0f);
        assertEquals((float) Math.asin(2 / 3f), e.eval(expToFloatExp("asin(2/3)")), 0f);
        assertEquals((float) Math.acos(2 / 3f), e.eval(expToFloatExp("acos(2/3)")), 0f);
        assertEquals((float) Math.atan(2 / 3f), e.eval(expToFloatExp("atan(2/3)")), 0f);
        assertEquals((float) Math.atan2(3, 4), e.eval(expToFloatExp("atan2(3, 4)")), 0f);
        assertEquals((float) (3 > 0 ? 4 : 2), e.eval(expToFloatExp("ifElse(3, 4, 2)")), 0f);
        assertEquals(2 * 3f + 4, e.eval(expToFloatExp("mad(2,3,4)")), 0f);
    }

    static long sLast;
    @NonNull static DecimalFormat sDf = new DecimalFormat("#.0000000");

    private static void duration(@Nullable String str, float divide) {
        long now = System.nanoTime();
        if (str == null) {
            sLast = now;
            return;
        }
        System.out.println(str + sDf.format((now - sLast) * 1E-6f / divide) + " ms");
        sLast = now;
    }

    @Test
    public void testAccuracy() {
        AnimatedFloatExpression exp = new AnimatedFloatExpression();
        long time = System.nanoTime();
        int total = 1000;

        float[][] rpn = new float[total][];
        // ################################################################

        {
            for (int i = 0; i < 3; i++) {
                String str =
                        (1 - (i % 2) * 2)
                                + " * 4 / ( "
                                + (i * 2 + 2)
                                + " * "
                                + (i * 2 + 3)
                                + " * "
                                + (i * 2 + 4)
                                + " )";
                System.out.print("expression \"" + str + "\" \n");
                float[] e = expToFloatExp(str);
                exp.evalDB(e);
            }
            System.out.println("....");
        }
        System.out.println("parse ");
        for (int k = 0; k < 6; k++) {
            System.out.println(" ------ " + k + " -----");
            duration(null, 0);
            // ========== Evaluate using java  ==========
            for (int i = 0; i < total; i++) {
                String str =
                        (1 - (i % 2) * 2)
                                + " * 4 / ( "
                                + (i * 2 + 2)
                                + " * "
                                + (i * 2 + 3)
                                + " * "
                                + (i * 2 + 4)
                                + " )";
                rpn[i] = expToFloatExp(str);
            }
            System.out.print(" parsing  output " + rpn[0].length + " float array ");
            duration(total + " times in(avg)= ", 1000000);
            duration(null, 0);
            float sum_jvm = 3;
            for (int i = 0; i < total; i++) {
                float v = (1 - (i % 2) * 2) * 4 / ((i * 2f + 2) * (i * 2 + 3) * (i * 2 + 4));
                sum_jvm += v;
            }
            assertEquals(sum_jvm, (float) Math.PI, 0.0001);

            System.out.print("1: sum = " + sum_jvm);
            duration(" run " + total + " Java calculations avg = ", total);

            // ========== Evaluate using pre_computed_tables  2 ==========
            float sum_eval = 3;
            for (int i = 0; i < total; i++) {
                float v = exp.eval(rpn[i]);
                sum_eval += v;
            }

            System.out.print("2: sum = " + sum_eval);
            duration(" run " + total + " eval calc         avg = ", total);
            assertEquals(sum_eval, (float) Math.PI, 0.0001);

            // ========== Evaluate parsing and table    3 ==========

            String[] eval_str = new String[total];
            for (int i = 0; i < total; i++) {
                eval_str[i] =
                        (1 - (i % 2) * 2)
                                + " * 4 / ( "
                                + (i * 2 + 2)
                                + " * "
                                + (i * 2 + 3)
                                + " * "
                                + (i * 2 + 4)
                                + " )";
            }
            duration(null, 0);
            float sum_parse_eval = 3;
            for (int i = 0; i < total; i++) {
                float[] rpn_floats = expToFloatExp(eval_str[i]);
                float v = exp.eval(rpn_floats);
                sum_parse_eval += v;
            }

            System.out.print("3: sum = " + sum_parse_eval);
            duration(" run " + total + " parse eval calc   avg = ", total);
            assertEquals(sum_parse_eval, (float) Math.PI, 0.0001);

            // ========== Evaluate gen strings parsing and table   5 ==========
            float sum_gen_parse_eval = 3;
            for (int i = 0; i < total; i++) {
                String str =
                        (1 - (i % 2) * 2)
                                + " * 4 / ( "
                                + (i * 2 + 2)
                                + " * "
                                + (i * 2 + 3)
                                + " * "
                                + (i * 2 + 4)
                                + " )";
                float[] rpn_floats = expToFloatExp(str);
                float v = exp.eval(rpn_floats);
                sum_gen_parse_eval += v;
            }

            System.out.print("4: sum = " + sum_gen_parse_eval);
            duration(" run " + total + " gen parse eval    avg = ", total);
            assertEquals(sum_gen_parse_eval, (float) Math.PI, 0.0001);

            System.out.println("  Pi = " + (float) (Math.PI));

            // System.gc();
            // try {
            //     Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //     throw new RuntimeException(e);
            // }
        }
    }

    @Test
    public void testRandom() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        System.out.println(e.eval(new float[] {RAND}));
        System.out.println(e.eval(new float[] {RAND}));
        System.out.println(e.eval(new float[] {RAND}));
        System.out.println(e.eval(new float[] {RAND}));
        float rand1 = e.eval(new float[] {12345f, RAND_SEED, RAND});
        float rand2 = e.eval(new float[] {RAND});
        float rand3 = e.eval(new float[] {RAND});
        float rand4 = e.eval(new float[] {RAND});
        // ideally this is fixed (using the same random algorithm)
        // but if it should at least be consistent
        assertEquals(rand1, e.eval(new float[] {12345f, RAND_SEED, RAND}), 0.0001);
        assertEquals(rand2, e.eval(new float[] {RAND}), 0.0001);
        assertEquals(rand3, e.eval(new float[] {RAND}), 0.0001);
        assertEquals(rand4, e.eval(new float[] {RAND}), 0.0001);
        assertTrue(true);
    }

    @Test
    public void testNames() {

        assertEquals("+", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ADD));
        assertEquals("-", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SUB));
        assertEquals("*", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.MUL));
        assertEquals("/", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.DIV));
        assertEquals("%", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.MOD));
        assertEquals("min", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.MIN));
        assertEquals("max", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.MAX));
        assertEquals("pow", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.POW));
        assertEquals("sqrt", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SQRT));
        assertEquals("abs", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ABS));
        assertEquals("sign", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SIGN));
        assertEquals(
                "copySign", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.COPY_SIGN));
        assertEquals("exp", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.EXP));
        assertEquals("floor", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.FLOOR));
        assertEquals("log", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.LOG));
        assertEquals("ln", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.LN));
        assertEquals("round", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ROUND));
        assertEquals("cos", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.COS));
        assertEquals("sin", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SIN));
        assertEquals("tan", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.TAN));
        assertEquals("asin", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ASIN));
        assertEquals("atan", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ATAN));
        assertEquals("atan2", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ATAN2));
        assertEquals("acos", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.ACOS));
        assertEquals("mad", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.MAD));
        assertEquals("ifElse", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.IFELSE));
        assertEquals("clamp", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.CLAMP));
        assertEquals("cbrt", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.CBRT));
        assertEquals("deg", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.DEG));
        assertEquals("rad", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.RAD));
        assertEquals("ceil", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.CEIL));
    }

    @Test
    public void testSet3Ops() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        float min = Float.MAX_VALUE, max = 0;
        for (int i = 0; i < 200; i++) {
            float f = (float) eval(e, 2f, 3f, AnimatedFloatExpression.RAND_IN_RANGE);
            min = Math.min(min, f);
            max = Math.max(max, f);
        }
        System.out.println(min + " " + max);
        assertTrue(min < 2.1 && max > 2.8);
        assertEquals((float) 25, eval(e, 4f, 3f, AnimatedFloatExpression.SQUARE_SUM), 0f);
        assertEquals((float) 1, eval(e, 4f, 3f, AnimatedFloatExpression.STEP), 0f);
        assertEquals((float) 0, eval(e, 2f, 3f, AnimatedFloatExpression.STEP), 0f);
        assertEquals((float) 9, eval(e, 3f, AnimatedFloatExpression.SQUARE), 0f);
        assertEquals((float) 9, eval(e, 3f, AnimatedFloatExpression.DUP, MUL), 0f);
        assertEquals((float) 5, eval(e, 3f, 4f, AnimatedFloatExpression.HYPOT), 0f);
        assertEquals((float) 1 / 2f, eval(e, 2f, 4f, DIV), 0f);
        assertEquals((float) 2f, eval(e, 2f, 4f, AnimatedFloatExpression.SWAP, DIV), 0f);
        assertEquals((float) 175f, eval(e, 100f, 200f, 0.75f, AnimatedFloatExpression.LERP), 0f);
        assertEquals((float) 0.5f, eval(e, 5f, 10f, 0f, AnimatedFloatExpression.SMOOTH_STEP), 0f);

        assertEquals(
                "rand_in_range",
                AnimatedFloatExpression.toMathName(AnimatedFloatExpression.RAND_IN_RANGE));
        assertEquals(
                "square_sum",
                AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SQUARE_SUM));
        assertEquals("step", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.STEP));
        assertEquals("square", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SQUARE));
        assertEquals("dup", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.DUP));
        assertEquals("hypot", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.HYPOT));
        assertEquals("swap", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SWAP));
        assertEquals("lerp", AnimatedFloatExpression.toMathName(AnimatedFloatExpression.LERP));
        assertEquals(
                "smooth_step",
                AnimatedFloatExpression.toMathName(AnimatedFloatExpression.SMOOTH_STEP));
    }

    float eval(@NonNull AnimatedFloatExpression e, float... a) {
        return e.eval(a);
    }

    @Test
    public void testAdvanceOperators() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        assertEquals((float) Math.acos(2 / 3f), eval(e, 2f, 3f, DIV, ACOS), 0f);
        assertEquals((float) Math.asin(2 / 3f), eval(e, 2f, 3f, DIV, ASIN), 0f);
        assertEquals((float) Math.atan(2 / 3f), eval(e, 2f, 3f, DIV, ATAN), 0f);
        assertEquals((float) Math.atan2(2, 3f), eval(e, 2f, 3f, ATAN2), 0f);
        assertEquals((float) (2f * 3f + 4f), eval(e, 2f, 3f, 4f, MAD), 0f);
        assertEquals((float) ((1 > 0) ? 4f : 3f), eval(e, 3f, 4f, 1, IFELSE), 0f);
        assertEquals((float) ((0 > 0) ? 4f : 3f), eval(e, 3f, 4f, 0, IFELSE), 0f);
        assertEquals((float) Math.min(Math.max(43f, 0f), 20f), eval(e, 43f, 20f, 0f, CLAMP), 0f);
        assertEquals((float) (Math.pow(32, 1 / 3.)), eval(e, 32, CBRT), 0f);
        assertEquals((float) (Math.toDegrees((float) 32)), eval(e, 32, DEG), 0f);
        assertEquals((float) (Math.toRadians((float) 32)), eval(e, 32, RAD), 0f);
        assertEquals((float) (Math.ceil((float) 234.2)), eval(e, 234.2f, CEIL), 0f);
        assertEquals((float) (Math.floor((float) 234.2)), eval(e, 234.2f, FLOOR), 0f);
        assertEquals((float) (Math.max(21f, 32f)), eval(e, 21f, 32f, MAX), 0f);
        assertEquals((float) (Math.min(21f, 32f)), eval(e, 21f, 32f, MIN), 0f);
        assertEquals((float) (Math.pow(2, 16)), eval(e, 2, 16, POW), 0f);
        assertEquals((float) (Math.sqrt(144f)), eval(e, 144f, SQRT), 0f);
        assertEquals((float) (Math.abs(-234.2f)), eval(e, -234.2f, ABS), 0f);
        assertEquals((float) (Math.signum(-234.2f)), eval(e, -234.2f, SIGN), 0f);
        assertEquals((float) (Math.exp(1.2f)), eval(e, 1.2f, EXP), 0f);

        assertEquals((float) (Math.log10(1.2f)), eval(e, 1.2f, LOG), 0f);
        assertEquals((float) (Math.log(1.2f)), eval(e, 1.2f, LN), 0f);
        assertEquals((float) (Math.round(1.2f)), eval(e, 1.2f, ROUND), 0f);
        assertEquals((float) (Math.copySign(2, -1.2f)), eval(e, 2, -1.2f, COPY_SIGN), 0f);
        assertEquals((float) (Math.tan(-1.2f)), eval(e, -1.2f, TAN), 0f);

        assertEquals((float) (Math.log(1.2f) / Math.log(2)), eval(e, 1.2f, LOG2), 0f);
        assertEquals((float) (1 / 1.2f), eval(e, 1.2f, INV), 0f);
        assertEquals((float) (1.2f - (int) 1.2f), eval(e, 1.2f, FRACT), 0f);
        assertEquals((float) (-1.2f - (int) -1.2f), eval(e, -1.2f, FRACT), 0f);
        assertEquals((float) (0.8), eval(e, 1.2f, 1, PINGPONG), 0.0000001f);
        assertEquals((float) (0.2), eval(e, 0.2f, 1, PINGPONG), 0f);
    }

    @Test
    public void generateBitPatterns() {
        toSampleString(2 + 3f, expToFloatExp("2 + 3"), "2 + 3");
        toSampleString((float) Math.sin(2 / 3f), expToFloatExp("sin(2 / 3)"), "sin(2 / 3)");
        toSampleString((float) Math.cos(2 / 3f), expToFloatExp("cos(2 / 3)"), "cos(2 / 3)");
        toSampleString((float) Math.tan(2 / 3f), expToFloatExp("tan(2 / 3)"), "tan(2 / 3)");
        toSampleString((float) Math.sin(2 / 3f), expToFloatExp("sin(2 / 3)"), "sin(2 / 3)");
        toSampleString((float) Math.min(2f, 3f), expToFloatExp("min(2 , 3)"), "min(2 , 3)");
        toSampleString((float) Math.max(2f, 3f), expToFloatExp("max(2 , 3)"), "max(2 , 3)");
        toSampleString((float) Math.pow(2f, 3f), expToFloatExp("pow(2 , 3)"), "pow(2 , 3)");
        toSampleString((float) Math.sqrt(4f), expToFloatExp("sqrt(4)"), "sqrt(4)");
        toSampleString((float) Math.abs(-4f), expToFloatExp("abs(-4)"), "abs(-4)");
        toSampleString((float) Math.signum(-4f), expToFloatExp("sign(-4)"), "sign(-4)");
        toSampleString(
                (float) Math.copySign(3f, -4f), expToFloatExp("copySign(3,-4)"), "copySign(3,-4)");
        toSampleString(
                (float) Math.copySign(3f, 4f), expToFloatExp("copySign(3, 4)"), "copySign(3, 4)");
        toSampleString((float) Math.exp(-4f), expToFloatExp("exp(-4)"), "exp(-4)");
        toSampleString((float) Math.floor(-4f), expToFloatExp("floor(-4)"), "floor(-4)");
        toSampleString((float) Math.log(4f), expToFloatExp("ln(4)"), "ln(4)");
        toSampleString((float) Math.log10(4f), expToFloatExp("log(4)"), "log(4)");
        toSampleString((float) Math.round(4.45f), expToFloatExp("round(4.45)"), "round(4.45)");
        toSampleString((float) Math.asin(2 / 3f), expToFloatExp("asin(2/3)"), "asin(2/3)");
        toSampleString((float) Math.acos(2 / 3f), expToFloatExp("acos(2/3)"), "acos(2/3)");
        toSampleString((float) Math.atan(2 / 3f), expToFloatExp("atan(2/3)"), "atan(2/3)");
        toSampleString((float) Math.atan2(3, 4), expToFloatExp("atan2(3, 4)"), "atan2(3, 4)");
        toSampleString(
                (float) (3 > 0 ? 4 : 2), expToFloatExp("ifElse(3, 4, 2)"), "ifElse(3, 4, 2)");
        toSampleString(2 * 3f + 4, expToFloatExp("mad(2,3,4)"), "mad(2,3,4)");

        toSampleString2("( 2f, 3f, DIV, ACOS)", Math.acos(2 / 3f), 2f, 3f, DIV, ACOS);
        toSampleString2("( 2f, 3f, DIV, ASIN)", Math.asin(2 / 3f), 2f, 3f, DIV, ASIN);
        toSampleString2("( 2f, 3f, DIV, ATAN)", Math.atan(2 / 3f), 2f, 3f, DIV, ATAN);
        toSampleString2("( 2f, 3f, ATAN2)", Math.atan2(2, 3f), 2f, 3f, ATAN2);
        toSampleString2("( 2f, 3f, 4f, MAD)", (2f * 3f + 4f), 2f, 3f, 4f, MAD);
        toSampleString2("( 3f, 4f, 1, IFELSE)", ((1 > 0) ? 4f : 3f), 3f, 4f, 1, IFELSE);
        toSampleString2("( 3f, 4f, 0, IFELSE)", ((0 > 0) ? 4f : 3f), 3f, 4f, 0, IFELSE);
        toSampleString2(
                "43f, 20f, 0f, CLAMP",
                (float) Math.min(Math.max(43f, 0f), 20f),
                43f,
                20f,
                0f,
                CLAMP);
        toSampleString2("( 32, CBRT)", (Math.pow(32, 1 / 3.)), 32, CBRT);
        toSampleString2("( 32, DEG)", (Math.toDegrees((float) 32)), 32, DEG);
        toSampleString2("( 32, RAD)", (Math.toRadians((float) 32)), 32, RAD);
        toSampleString2("( 234.2f, CEIL)", (Math.ceil((float) 234.2)), 234.2f, CEIL);
        toSampleString2("( 234.2f, FLOOR)", (Math.floor((float) 234.2)), 234.2f, FLOOR);
        toSampleString2("( 21f, 32f, MAX)", (Math.max(21f, 32f)), 21f, 32f, MAX);
        toSampleString2("( 21f, 32f, MIN)", (Math.min(21f, 32f)), 21f, 32f, MIN);
        toSampleString2("( 2, 16, POW)", (Math.pow(2, 16)), 2, 16, POW);
        toSampleString2("( 144f, SQRT)", (Math.sqrt(144f)), 144f, SQRT);
        toSampleString2("( -234.2f, ABS)", (Math.abs(-234.2f)), -234.2f, ABS);
        toSampleString2("( -234.2f, SIGN)", (Math.signum(-234.2f)), -234.2f, SIGN);
        toSampleString2("( 1.2f, EXP)", (Math.exp(1.2f)), 1.2f, EXP);

        toSampleString2("( 1.2f, LOG)", (Math.log10(1.2f)), 1.2f, LOG);
        toSampleString2("( 1.2f, LN)", (Math.log(1.2f)), 1.2f, LN);
        toSampleString2("( 1.2f, ROUND)", (Math.round(1.2f)), 1.2f, ROUND);
        toSampleString2(
                "2, -1.2f, COPY_SIGN", (float) (Math.copySign(2, -1.2f)), 2, -1.2f, COPY_SIGN);
        toSampleString2("( -1.2f, TAN)", (Math.tan(-1.2f)), -1.2f, TAN);

        AnimatedFloatExpression e = new AnimatedFloatExpression();
        toSampleString2("( -1.2f, TAN)", (Math.tan(-1.2f)), -1.2f, TAN);
    }

    @NonNull
    static String toSampleString(float answer, @NonNull float[] exp, String str) {
        String s =
                "{name:[\""
                        + str
                        + "\"]"
                        + ", test: ["
                        + Integer.toHexString(Float.floatToRawIntBits(answer))
                        + ", ";
        for (int i = 0; i < exp.length; i++) {
            s += i == 0 ? "" : ",";
            s += Integer.toHexString(Float.floatToRawIntBits(exp[i]));
        }
        s += "]}";
        System.out.println(s);
        return s;
    }

    @NonNull
    static String toSampleString2(String str, double answer, @NonNull float... exp) {
        String s =
                "{name:[\""
                        + str
                        + "\"]"
                        + ", test: ["
                        + Integer.toHexString(Float.floatToRawIntBits((float) answer))
                        + ", ";
        for (int i = 0; i < exp.length; i++) {
            s += i == 0 ? "" : ",";
            s += Integer.toHexString(Float.floatToRawIntBits(exp[i]));
        }
        s += "]";
        System.out.println(s);
        return s;
    }
}
