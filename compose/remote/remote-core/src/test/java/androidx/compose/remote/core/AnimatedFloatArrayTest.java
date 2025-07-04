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
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SUM;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB;
import static androidx.compose.remote.core.operations.utilities.NanMap.ID_REGION_ARRAY;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;

import org.junit.Test;

public class AnimatedFloatArrayTest {

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
