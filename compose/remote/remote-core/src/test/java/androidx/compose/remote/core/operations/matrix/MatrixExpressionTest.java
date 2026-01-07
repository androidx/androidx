/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.remote.core.operations.matrix;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.utilities.MatrixOperations;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MatrixExpressionTest {

    @Test
    public void testRotation() {
        RemoteContext context = mock(RemoteContext.class);

        // IDENTITY
        float[] exp1 = new float[]{MatrixOperations.IDENTITY};
        MatrixExpression expr1 = new MatrixExpression(1, 0, exp1);
        expr1.updateVariables(context);
        expr1.apply(context);
        assertEquals(
                "MatrixExpression[1] = IDENTITY -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr1.toString());

        // ROT_X
        float[] exp2 = new float[]{60f, MatrixOperations.ROT_X};
        MatrixExpression expr2 = new MatrixExpression(1, 0, exp2);
        expr2.updateVariables(context);
        expr2.apply(context);
        assertEquals(
                "MatrixExpression[1] = 60.0 ROT_X -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 0.49999997, -0.86602545, 0.0, "
                        + "0.0, 0.86602545, 0.49999997, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr2.toString());

        // ROT_Y
        float[] exp3 = new float[]{60f, MatrixOperations.ROT_Y};
        MatrixExpression expr3 = new MatrixExpression(1, 0, exp3);
        expr3.updateVariables(context);
        expr3.apply(context);
        assertEquals(
                "MatrixExpression[1] = 60.0 ROT_Y -> ["
                        + "0.49999997, 0.0, 0.86602545, 0.0, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "-0.86602545, 0.0, 0.49999997, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr3.toString());

        // ROT_Z
        float[] exp4 = new float[]{60f, MatrixOperations.ROT_Z};
        MatrixExpression expr4 = new MatrixExpression(1, 0, exp4);
        expr4.updateVariables(context);
        expr4.apply(context);
        assertEquals(
                "MatrixExpression[1] = 60.0 ROT_Z -> ["
                        + "0.49999997, -0.86602545, 0.0, 0.0, "
                        + "0.86602545, 0.49999997, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr4.toString());
    }

    @Test
    public void testTranslation() {
        RemoteContext context = mock(RemoteContext.class);

        // TRANSLATE_X
        float[] exp1 = new float[]{12.34f, MatrixOperations.TRANSLATE_X};
        MatrixExpression expr1 = new MatrixExpression(1, 0, exp1);
        expr1.updateVariables(context);
        expr1.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 TRANSLATE_X -> ["
                        + "1.0, 0.0, 0.0, 12.34, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr1.toString());

        // TRANSLATE_Y
        float[] exp2 = new float[]{12.34f, MatrixOperations.TRANSLATE_Y};
        MatrixExpression expr2 = new MatrixExpression(1, 0, exp2);
        expr2.updateVariables(context);
        expr2.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 TRANSLATE_Y -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 1.0, 0.0, 12.34, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr2.toString());

        // TRANSLATE_Z
        float[] exp3 = new float[]{12.34f, MatrixOperations.TRANSLATE_Z};
        MatrixExpression expr3 = new MatrixExpression(1, 0, exp3);
        expr3.updateVariables(context);
        expr3.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 TRANSLATE_Z -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 12.34, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr3.toString());

        // TRANSLATE3
        float[] exp4 = new float[]{12.34f, 56.78f, 90.12f, MatrixOperations.TRANSLATE3};
        MatrixExpression expr4 = new MatrixExpression(1, 0, exp4);
        expr4.updateVariables(context);
        expr4.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 56.78 90.12 TRANSLATE_3 -> ["
                        + "1.0, 0.0, 0.0, 12.34, "
                        + "0.0, 1.0, 0.0, 56.78, "
                        + "0.0, 0.0, 1.0, 90.12, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr4.toString());
    }

    @Test
    public void testScale() {
        RemoteContext context = mock(RemoteContext.class);

        // SCALE_X
        float[] exp1 = new float[]{12.34f, MatrixOperations.SCALE_X};
        MatrixExpression expr1 = new MatrixExpression(1, 0, exp1);
        expr1.updateVariables(context);
        expr1.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 SCALE_X -> ["
                        + "12.34, 0.0, 0.0, 0.0, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr1.toString());

        // SCALE_Y
        float[] exp2 = new float[]{12.34f, MatrixOperations.SCALE_Y};
        MatrixExpression expr2 = new MatrixExpression(1, 0, exp2);
        expr2.updateVariables(context);
        expr2.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 SCALE_Y -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 12.34, 0.0, 0.0, "
                        + "0.0, 0.0, 1.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr2.toString());

        // SCALE_Z
        float[] exp3 = new float[]{12.34f, MatrixOperations.SCALE_Z};
        MatrixExpression expr3 = new MatrixExpression(1, 0, exp3);
        expr3.updateVariables(context);
        expr3.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 SCALE_Z -> ["
                        + "1.0, 0.0, 0.0, 0.0, "
                        + "0.0, 1.0, 0.0, 0.0, "
                        + "0.0, 0.0, 12.34, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr3.toString());

        // SCALE2
        float[] exp4 = new float[]{12.34f, 56.78f, MatrixOperations.SCALE2};
        MatrixExpression expr4 = new MatrixExpression(1, 0, exp4);
        expr4.updateVariables(context);
        expr4.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 56.78 SCALE2 -> ["
                        + "12.34, 0.0, 0.0, 0.0, "
                        + "0.0, 56.78, 0.0, 0.0, "
                        + "0.0, 0.0, 0.0, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr4.toString());

        // SCALE3
        float[] exp5 = new float[]{12.34f, 45.67f, 78.90f, MatrixOperations.SCALE3};
        MatrixExpression expr5 = new MatrixExpression(1, 0, exp5);
        expr5.updateVariables(context);
        expr5.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 45.67 78.9 SCALE3 -> ["
                        + "12.34, 0.0, 0.0, 0.0, "
                        + "0.0, 45.67, 0.0, 0.0, "
                        + "0.0, 0.0, 78.9, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr5.toString());
    }

    @Test
    public void testMultiply() {
        RemoteContext context = mock(RemoteContext.class);

        float[] exp = new float[]{
                12.34f,
                MatrixOperations.SCALE_X,
                MatrixOperations.IDENTITY,
                60f,
                MatrixOperations.ROT_X,
                MatrixOperations.MUL
        };
        MatrixExpression expr = new MatrixExpression(1, 0, exp);
        expr.updateVariables(context);
        expr.apply(context);
        assertEquals(
                "MatrixExpression[1] = 12.34 SCALE_X IDENTITY 60.0 ROT_X MUL -> ["
                        + "12.34, 0.0, 0.0, 0.0, "
                        + "0.0, 0.49999997, -0.86602545, 0.0, "
                        + "0.0, 0.86602545, 0.49999997, 0.0, "
                        + "0.0, 0.0, 0.0, 1.0]",
                expr.toString());
    }
}
