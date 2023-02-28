/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.constraintlayout.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AmountTest {
    Amount mA1 = new Amount(2, 3);
    Amount mA2 = new Amount(3, 5);

    @Before
    public void setUp() {
        mA1.set(2, 3);
        mA2.set(3, 5);
    }

    @Test
    public void testAdd() {
        mA1.add(mA2);
        assertEquals(mA1.getNumerator(), 19);
        assertEquals(mA1.getDenominator(), 15);
    }

    @Test
    public void testSubtract() {
        mA1.subtract(mA2);
        assertEquals(mA1.getNumerator(), 1);
        assertEquals(mA1.getDenominator(), 15);
    }

    @Test
    public void testMultiply() {
        mA1.multiply(mA2);
        assertEquals(mA1.getNumerator(), 2);
        assertEquals(mA1.getDenominator(), 5);
    }

    @Test
    public void testDivide() {
        mA1.divide(mA2);
        assertEquals(mA1.getNumerator(), 10);
        assertEquals(mA1.getDenominator(), 9);
    }

    @Test
    public void testSimplify() {
        mA1.set(20, 30);
        assertEquals(mA1.getNumerator(), 2);
        assertEquals(mA1.getDenominator(), 3);
        mA1.set(77, 88);
        assertEquals(mA1.getNumerator(), 7);
        assertEquals(mA1.getDenominator(), 8);
    }

    @Test
    public void testEquality() {
        mA2.set(mA1.getNumerator(), mA1.getDenominator());
        assertTrue(mA1.equals(mA2));
    }
}
