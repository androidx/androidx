/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static androidx.compose.remote.core.IntegerTestIUtils.L_ABS;
import static androidx.compose.remote.core.IntegerTestIUtils.L_ADD;
import static androidx.compose.remote.core.IntegerTestIUtils.L_AND;
import static androidx.compose.remote.core.IntegerTestIUtils.L_CLAMP;
import static androidx.compose.remote.core.IntegerTestIUtils.L_COPY_SIGN;
import static androidx.compose.remote.core.IntegerTestIUtils.L_DECR;
import static androidx.compose.remote.core.IntegerTestIUtils.L_DIV;
import static androidx.compose.remote.core.IntegerTestIUtils.L_IFELSE;
import static androidx.compose.remote.core.IntegerTestIUtils.L_INCR;
import static androidx.compose.remote.core.IntegerTestIUtils.L_MAD;
import static androidx.compose.remote.core.IntegerTestIUtils.L_MAX;
import static androidx.compose.remote.core.IntegerTestIUtils.L_MIN;
import static androidx.compose.remote.core.IntegerTestIUtils.L_MOD;
import static androidx.compose.remote.core.IntegerTestIUtils.L_MUL;
import static androidx.compose.remote.core.IntegerTestIUtils.L_NEG;
import static androidx.compose.remote.core.IntegerTestIUtils.L_NOT;
import static androidx.compose.remote.core.IntegerTestIUtils.L_OR;
import static androidx.compose.remote.core.IntegerTestIUtils.L_SHL;
import static androidx.compose.remote.core.IntegerTestIUtils.L_SHR;
import static androidx.compose.remote.core.IntegerTestIUtils.L_SIGN;
import static androidx.compose.remote.core.IntegerTestIUtils.L_SUB;
import static androidx.compose.remote.core.IntegerTestIUtils.L_USHR;
import static androidx.compose.remote.core.IntegerTestIUtils.L_XOR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_ADD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MUL;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.Arrays;

public class IntegerExpressionEvaluatorTest {

    @Test
    public void foo() {
        IntegerExpressionEvaluator e = new IntegerExpressionEvaluator();
        int ans = e.eval(3 << 3, new int[] {5, 3, 3, I_ADD, I_MUL});
        System.out.println(ans);
        assertEquals((3 + 3) * 5, ans);
        System.out.println(eval(5, 3, 3, L_ADD, L_ADD));
    }

    @Test
    public void allOperations() {
        assertEquals(20 - 3, eval(20, 3, L_SUB));
        assertEquals(20 * 3, eval(20, 3, L_MUL));
        assertEquals(20 / 3, eval(20, 3, L_DIV));
        assertEquals(20 % 3, eval(20, 3, L_MOD));
        assertEquals(128 << 3, eval(128, 3, L_SHL));
        assertEquals(128 >> 3, eval(128, 3, L_SHR));
        assertEquals((-1) >>> 3, eval(-1, 3, L_USHR));
        assertEquals(128 | 3, eval(128, 3, L_OR));
        assertEquals(128 & 3, eval(128, 3, L_AND));
        assertEquals(128 ^ 3, eval(128, 3, L_XOR));
        assertEquals((int) Math.copySign(128, 3), eval(128, 3, L_COPY_SIGN));
        assertEquals((int) Math.copySign(128, -3), eval(128, -3, L_COPY_SIGN));
        assertEquals(Math.min(128, 3), eval(128, 3, L_MIN));
        assertEquals(Math.max(128, 3), eval(128, 3, L_MAX));
        assertEquals(-(-3), eval(-3, L_NEG));
        assertEquals(Math.abs(3), eval(3, L_ABS));
        assertEquals(Math.abs(-3), eval(-3, L_ABS));
        assertEquals(3 + 1, eval(3, L_INCR));
        assertEquals(3 - 1, eval(3, L_DECR));
        assertEquals(~3, eval(3, L_NOT));
        assertEquals((int) Math.signum(3), eval(3, L_SIGN));
        assertEquals((int) Math.signum(0), eval(0, L_SIGN));
        assertEquals((int) Math.signum(-3), eval(-3, L_SIGN));
        assertEquals(clamp(44, 33), eval(44, 33, 9, L_CLAMP));
        assertEquals(Math.max(1, 9), eval(1, 33, 9, L_CLAMP));
        assertEquals((1 > 0) ? 2 : 3, eval(1, 2, 3, L_IFELSE));
        assertEquals((int) Math.fma(3, 5, 7), eval(3, 5, 7, L_MAD));

        System.out.println("------- allOperations --------");
    }

    static int clamp(int c, int max) {
        int n = max;
        c &= ~(c >> 31);
        c -= n;
        c &= (c >> 31);
        c += n;
        return c;
    }

    static int eval(@NonNull long... v) {
        int mask = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > Integer.MAX_VALUE) {
                mask |= 1 << i;
            }
        }
        int[] vint = new int[v.length];
        for (int i = 0; i < vint.length; i++) {
            vint[i] = (int) v[i];
        }
        IntegerExpressionEvaluator e = new IntegerExpressionEvaluator();
        System.out.println(
                "---------------\n"
                        + IntegerExpressionEvaluator.toStringInfix(mask, vint)
                        + " = "
                        + e.eval(mask, Arrays.copyOf(vint, vint.length)));

        return e.eval(mask, vint);
    }
}
