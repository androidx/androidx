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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class EquationVariableTest {
    LinearSystem mLinearSystem;
    EquationVariable mEV1;
    EquationVariable mEV2;

    @Before
    public void setUp() {
        mLinearSystem = new LinearSystem();
        mEV1 = new EquationVariable(mLinearSystem, 200);
        mEV2 = new EquationVariable(mLinearSystem, 200);
    }

    @Test
    public void testEquality() {
        assertTrue(mEV1.getAmount().equals(mEV2.getAmount()));
    }

    @Test
    public void testAddition() {
        mEV1.add(mEV2);
        assertEquals(mEV1.getAmount().getNumerator(), 400);
    }

    @Test
    public void testSubtraction() {
        mEV1.subtract(mEV2);
        assertEquals(mEV1.getAmount().getNumerator(), 0);
    }

    @Test
    public void testMultiply() {
        mEV1.multiply(mEV2);
        assertEquals(mEV1.getAmount().getNumerator(), 40000);
    }

    @Test
    public void testDivide() {
        mEV1.divide(mEV2);
        assertEquals(mEV1.getAmount().getNumerator(), 1);
    }

    @Test
    public void testCompatible() {
        assertTrue(mEV1.isCompatible(mEV2));
        mEV2 = new EquationVariable(mLinearSystem, 200, "TEST", SolverVariable.Type.UNRESTRICTED);
        assertFalse(mEV1.isCompatible(mEV2));
    }
}
