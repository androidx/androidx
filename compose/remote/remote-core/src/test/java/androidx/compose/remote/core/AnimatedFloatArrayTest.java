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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_AVG;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_DEREF;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_LEN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_LERP;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE_LOOP;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SUM;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SUM_SQR;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SUM_TILL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SUM_XY;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB;
import static androidx.compose.remote.core.operations.utilities.NanMap.ID_REGION_ARRAY;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;

import org.jspecify.annotations.Nullable;
import org.junit.Test;

public class AnimatedFloatArrayTest {

    @Test
    public void testNewFunctions() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        float a = Utils.asNan(0x123 | ID_REGION_ARRAY);
        float b = Utils.asNan(0x124 | ID_REGION_ARRAY);
        CollectionsAccess ca = new CollectionsAccess() {
            @Override
            public float getFloatValue(int id, int index) {
                return (id == AnimatedFloatExpression.fromNaN(a)) ? (index + 1) : (index + 2);
            }

            @Override
            public float[] getFloats(int id) {
                return (id == AnimatedFloatExpression.fromNaN(a)) ? new float[]{1, 2, 3, 4}
                        : new float[]{2, 3, 4, 5};
            }

            @Override
            public float @Nullable [] getDynamicFloats(int id) {
                return getFloats(id);
            }

            @Override
            public @Nullable ArrayAccess getArray(int id) {
                return null;
            }

            @Override
            public int getListLength(int id) {
                return 4;
            }

            @Override
            public int getId(int listId, int index) {
                return 0;
            }
        };

        // A_SUM_TILL: array a till index 1 -> 1 + 2 = 3
        assertEquals(3f, eval(e, ca, a, 1, A_SUM_TILL), 0.001f);

        // A_SUM_XY: array a dot array b -> 1*2 + 2*3 + 3*4 + 4*5 = 2 + 6 + 12 + 20 = 40
        assertEquals(40f, eval(e, ca, a, b, A_SUM_XY), 0.001f);

        // A_SUM_SQR: array a -> 1^2 + 2^2 + 3^2 + 4^2 = 1 + 4 + 9 + 16 = 30
        assertEquals(30f, eval(e, ca, a, A_SUM_SQR), 0.001f);

        // A_LERP: array a at 0.5 -> 2.5
        assertEquals(2.5f, eval(e, ca, a, 0.5f, A_LERP), 0.001f);
        // A_LERP: array a at 0 -> 1
        assertEquals(1f, eval(e, ca, a, 0f, A_LERP), 0.001f);
        // A_LERP: array a at 1 -> 4
        assertEquals(4f, eval(e, ca, a, 1f, A_LERP), 0.001f);
    }

    @Test
    public void simpleTest() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        // (3+5)*(2-8) -> [3, 5, +, 2, 8, -, *]
        float[] rpn = new float[] {3, 5, ADD, 2, 8, SUB, MUL};
        assertEquals(-48.0f, e.eval(rpn), 0f);
        e.evalDB(new float[] {3, 5, ADD, 2, 8, SUB, MUL});

        float a = Utils.asNan(0x123 | ID_REGION_ARRAY);
        assertEquals(2.5f, eval(e, ra(1, 2, 3, 4), a, A_AVG), 0.002f);
        assertEquals(4f, eval(e, ra(1, 2, 3, 4), a, A_LEN), 0.002f);
        assertEquals(4f, eval(e, ra(1, 2, 3, 4), a, A_MAX), 0.002f);
        assertEquals(1f, eval(e, ra(1, 2, 3, 4), a, A_MIN), 0.002f);
        assertEquals(3f, eval(e, ra(1, 2, 3, 4), a, 2, A_DEREF), 0.002f);
        assertEquals(10f, eval(e, ra(1, 2, 3, 4), a, A_SUM), 0.002f);
        CollectionsAccess array = ra(1, 2, 3, 4);
        for (float fi = 0; fi <= 1f; fi += 0.2f) {
            System.out.println(">>>>>>> " + eval(e, array, a, fi, A_SPLINE));
        }
        assertEquals(2.5f, eval(e, ra(1, 2, 3, 4), a, 0.5f, A_SPLINE), 0.002f);
    }

    float eval(AnimatedFloatExpression e, CollectionsAccess ca, float... exp) {
        return e.eval(ca, exp, exp.length);
    }

    @Test
    public void testSplineLoop() {
        AnimatedFloatExpression e = new AnimatedFloatExpression();
        float a = Utils.asNan(0x123 | ID_REGION_ARRAY);
        assertEquals(2.5f, eval(e, ra(1, 2, 3, 4), a, 0.5f, A_SPLINE_LOOP), 0.002f);
        assertEquals(2.5f, eval(e, ra(1, 2, 3, 4), a, 1.5f, A_SPLINE_LOOP), 0.002f);
        assertEquals(2.5f, eval(e, ra(1, 2, 3, 4), a, -0.5f, A_SPLINE_LOOP), 0.002f);
    }
    static CollectionsAccess ra(float... data) {
        return new CollectionsAccess() {

            @Override
            public float getFloatValue(int id, int index) {
                return data[index];
            }

            @Override
            public float[] getFloats(int id) {
                return data;
            }

            @Override
            public float @Nullable [] getDynamicFloats(int id) {
                return data;
            }

            @Override
            @Nullable
            public ArrayAccess getArray(int id) {
                return null;
            }

            @Override
            public int getListLength(int id) {
                return data.length;
            }

            @Override
            public int getId(int listId, int index) {
                return 0;
            }
        };
    }
}
