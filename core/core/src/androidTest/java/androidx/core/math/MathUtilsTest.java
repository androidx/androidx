/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MathUtilsTest {

    @Test
    public void testAddExact() {
        assertEquals(2, MathUtils.addExact(1, 1));
        assertEquals(2L, MathUtils.addExact(1L, 1L));
        assertEquals(0, MathUtils.addExact(1, -1));
        assertEquals(0L, MathUtils.addExact(1L, -1L));
        assertThrows(ArithmeticException.class, () -> MathUtils.addExact(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> MathUtils.addExact(Long.MAX_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> MathUtils.addExact(Integer.MIN_VALUE, -1));
        assertThrows(ArithmeticException.class, () -> MathUtils.addExact(Long.MIN_VALUE, -1L));
    }

    @Test
    public void testSubtractExact() {
        assertEquals(0, MathUtils.subtractExact(1, 1));
        assertEquals(0L, MathUtils.subtractExact(1L, 1L));
        assertEquals(2, MathUtils.subtractExact(1, -1));
        assertEquals(2L, MathUtils.subtractExact(1L, -1L));
        assertThrows(ArithmeticException.class,
                () -> MathUtils.subtractExact(Integer.MAX_VALUE, -1));
        assertThrows(ArithmeticException.class, () -> MathUtils.subtractExact(Long.MAX_VALUE, -1L));
        assertThrows(ArithmeticException.class,
                () -> MathUtils.subtractExact(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> MathUtils.subtractExact(Long.MIN_VALUE, 1L));
    }

    @Test
    public void testMultiplyExact() {
        assertEquals(4, MathUtils.multiplyExact(2, 2));
        assertEquals(4L, MathUtils.multiplyExact(2L, 2L));
        assertEquals(0, MathUtils.multiplyExact(2, 0));
        assertEquals(0L, MathUtils.multiplyExact(2L, 0L));
        assertEquals(-4, MathUtils.multiplyExact(2, -2));
        assertEquals(-4L, MathUtils.multiplyExact(2L, -2L));
        assertThrows(ArithmeticException.class,
                () -> MathUtils.multiplyExact(Integer.MAX_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> MathUtils.multiplyExact(Long.MAX_VALUE, 2L));
        assertThrows(ArithmeticException.class,
                () -> MathUtils.multiplyExact(Integer.MIN_VALUE, 2));
        assertThrows(ArithmeticException.class, () -> MathUtils.multiplyExact(Long.MIN_VALUE, 2L));
    }

    @Test
    public void testIncrementExact() {
        assertEquals(1, MathUtils.incrementExact(0));
        assertEquals(1L, MathUtils.incrementExact(0L));
        assertThrows(ArithmeticException.class, () -> MathUtils.incrementExact(Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> MathUtils.incrementExact(Long.MAX_VALUE));
    }

    @Test
    public void testDecrementExact() {
        assertEquals(-1, MathUtils.decrementExact(0));
        assertEquals(-1L, MathUtils.decrementExact(0L));
        assertThrows(ArithmeticException.class, () -> MathUtils.decrementExact(Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> MathUtils.decrementExact(Long.MIN_VALUE));
    }

    @Test
    public void testNegateExact() {
        assertEquals(Integer.MIN_VALUE + 1, MathUtils.negateExact(Integer.MAX_VALUE));
        assertEquals(Long.MIN_VALUE + 1, MathUtils.negateExact(Long.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> MathUtils.negateExact(Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> MathUtils.negateExact(Long.MIN_VALUE));
    }

    @Test
    public void testToIntExact() {
        assertEquals(1, MathUtils.toIntExact(1L));
        assertThrows(ArithmeticException.class, () -> MathUtils.toIntExact(Long.MAX_VALUE));
    }

    @Test
    public void testClamp() {
        // Int
        assertEquals(0, MathUtils.clamp(-4, 0, 7));
        assertEquals(3, MathUtils.clamp(3, -2, 7));
        assertEquals(0, MathUtils.clamp(0, 0, 7));
        assertEquals(7, MathUtils.clamp(7, 0, 7));
        assertEquals(7, MathUtils.clamp(8, -2, 7));

        // Long
        assertEquals(0L, MathUtils.clamp(-4L, 0L, 7L));
        assertEquals(3L, MathUtils.clamp(3L, -2L, 7L));
        assertEquals(0L, MathUtils.clamp(0L, 0L, 7L));
        assertEquals(7L, MathUtils.clamp(7L, 0L, 7L));
        assertEquals(7L, MathUtils.clamp(8L, -2L, 7L));
        assertEquals(9220000000000000000L, MathUtils.clamp(9222000000000000000L, 0L,
                9220000000000000000L));

        // Double
        assertEquals(0.0, MathUtils.clamp(-0.4, 0.0, 7.0), 0.0);
        assertEquals(3.0, MathUtils.clamp(3.0, 0.0, 7.0), 0.0);
        assertEquals(0.1, MathUtils.clamp(0.1, 0.0, 7.0), 0.0);
        assertEquals(7.0, MathUtils.clamp(7.0, 0.0, 7.0), 0.0);
        assertEquals(-0.6, MathUtils.clamp(-0.7, -0.6, 7.0), 0.0);

        // Float
        assertEquals(0.0f, MathUtils.clamp(-0.4f, 0.0f, 7.0f), 0.0f);
        assertEquals(3.0f, MathUtils.clamp(3.0f, 0.0f, 7.0f), 0.0f);
        assertEquals(0.1f, MathUtils.clamp(0.1f, 0.0f, 7.0f), 0.0f);
        assertEquals(7.0f, MathUtils.clamp(7.0f, 0.0f, 7.0f), 0.0f);
        assertEquals(-0.6f, MathUtils.clamp(-0.7f, -0.6f, 7.0f), 0.0f);
    }
}
