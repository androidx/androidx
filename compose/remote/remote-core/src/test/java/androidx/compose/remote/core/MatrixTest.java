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

import org.junit.Test;

public class MatrixTest {
    @Test
    public void test() {
        assert (true);
        String expected;
        Matrix m2x2 = new Matrix();
        m2x2.setDimensions(2, 2);
        m2x2.setIdentity();
        System.out.println(m2x2);
        expected = "  1.00   0.00\n" + "  0.00   1.00\n";
        assertEquals(expected, m2x2.toString());

        Matrix m4x4 = new Matrix();
        m4x4.setDimensions(4, 4);
        m4x4.setIdentity();
        System.out.println(m4x4);
        m4x4.rotateX(90f);
        expected =
                "  1.00   0.00   0.00   0.00\n"
                        + "  0.00  -0.00  -1.00   0.00\n"
                        + "  0.00   1.00  -0.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m4x4.toString());

        System.out.println(m4x4);
        m4x4.setIdentity();
        m4x4.rotateZ(60f);
        expected =
                "  0.50  -0.87   0.00   0.00\n"
                        + "  0.87   0.50   0.00   0.00\n"
                        + "  0.00   0.00   1.00   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m4x4.toString());
        System.out.println(m4x4);

        m4x4.setIdentity();
        m4x4.rotateY(60f);
        expected =
                "  0.50   0.00   0.87   0.00\n"
                        + "  0.00   1.00   0.00   0.00\n"
                        + " -0.87   0.00   0.50   0.00\n"
                        + "  0.00   0.00   0.00   1.00\n";
        assertEquals(expected, m4x4.toString());
        System.out.println(m4x4);

        m2x2.rotateZ(60f);
        System.out.println(m2x2);
        expected = "  0.50  -0.87   0.00   0.00\n" + "  0.87   0.50   0.00   0.00\n";
        assertEquals(expected, m2x2.toString());
    }
}
