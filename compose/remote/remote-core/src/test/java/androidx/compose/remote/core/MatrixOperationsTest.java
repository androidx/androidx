/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.Matrix;
import androidx.compose.remote.core.operations.utilities.MatrixOperations;

import org.junit.Test;

public class MatrixOperationsTest {
    @Test
    public void testRotation() {
        assert (true);
        String expected;
        MatrixOperations me = new MatrixOperations();
        Matrix m;

        m = me.eval(new float[] {MatrixOperations.IDENTITY});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
        me = new MatrixOperations();
        m = me.eval(new float[] {60f, MatrixOperations.ROT_X});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00   0.50  -0.87   0.00\n"
                        + "  0.00   0.87   0.50   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
        m = me.eval(new float[] {60f, MatrixOperations.ROT_Y});
        expected =
                "  0.50   0.00   0.87   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + " -0.87   0.00   0.50   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
        m = me.eval(new float[] {60f, MatrixOperations.ROT_Z});
        expected =
                "  0.50  -0.87   0.00   0.00\n"
                        + "  0.87   0.50   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
    }

    @Test
    public void testTranslation() {
        assert (true);
        String expected;
        MatrixOperations me = new MatrixOperations();
        Matrix m;

        m = me.eval(new float[] {12.34f, MatrixOperations.TRANSLATE_X});
        expected =
                "  1.00   0.00   0.00  12.34\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
        m = me.eval(new float[] {12.34f, MatrixOperations.TRANSLATE_Y});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00   1.00   0.00  12.34\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
        m = me.eval(new float[] {12.34f, MatrixOperations.TRANSLATE_Z});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + "  0.00   0.00   1.00  12.34\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());

        System.out.println(m);
        m = me.eval(new float[] {12.34f, 56.78f, 90.12f, MatrixOperations.TRANSLATE3});
        expected =
                "  1.00   0.00   0.00  12.34\n"
                        + "  0.00   1.00   0.00  56.78\n"
                        + "  0.00   0.00   1.00  90.12\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
    }

    @Test
    public void testScale() {
        assert (true);
        String expected;
        MatrixOperations me = new MatrixOperations();
        Matrix m;

        m = me.eval(new float[] {12.34f, MatrixOperations.SCALE_X});
        expected =
                " 12.34   0.00   0.00   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);

        m = me.eval(new float[] {12.34f, MatrixOperations.SCALE_Y});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00  12.34   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);

        m = me.eval(new float[] {12.34f, MatrixOperations.SCALE_Z});
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + "  0.00   0.00  12.34   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);

        m = me.eval(new float[] {12.34f, 56.78f, MatrixOperations.SCALE2});
        expected =
                " 12.34   0.00   0.00   0.00\n"
                        + "  0.00  56.78   0.00   0.00\n"
                        + "  0.00   0.00   0.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);

        m = me.eval(new float[] {12.34f, 45.67f, 78.90f, MatrixOperations.SCALE3});
        expected =
                " 12.34   0.00   0.00   0.00\n"
                        + "  0.00  45.67   0.00   0.00\n"
                        + "  0.00   0.00  78.90   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
    }

    @Test
    public void testMultiply() {
        assert (true);
        String expected;
        MatrixOperations me = new MatrixOperations();
        Matrix m;

        m =
                me.eval(
                        new float[] {
                            12.34f,
                            MatrixOperations.SCALE_X,
                            MatrixOperations.IDENTITY,
                            60f,
                            MatrixOperations.ROT_X,
                            MatrixOperations.MUL
                        });
        expected =
                " 12.34   0.00   0.00   0.00\n"
                        + "  0.00   0.50  -0.87   0.00\n"
                        + "  0.00   0.87   0.50   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m.toString());
        System.out.println(m);
    }
}
