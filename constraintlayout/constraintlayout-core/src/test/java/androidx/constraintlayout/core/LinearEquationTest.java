/*
 * Copyright (C) 2021 The Android Open Source Project
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

public class LinearEquationTest {
    LinearSystem mLS;
    LinearEquation mLE;

    @Before
    public void setUp() {
        mLS = new LinearSystem();
        mLE = new LinearEquation();
        mLE.setSystem(mLS);
        LinearEquation.resetNaming();
    }

    @Test
    public void testDisplay1() {
        mLE.var("A").equalsTo().var(100);
        assertEquals(mLE.toString(), "A = 100");
    }

    @Test
    public void testDisplay2() {
        mLE.var("A").equalsTo().var("B");
        assertEquals(mLE.toString(), "A = B");
    }

    @Test
    public void testDisplay3() {
        mLE.var("A").greaterThan().var("B");
        assertEquals(mLE.toString(), "A >= B");
    }

    @Test
    public void testDisplay4() {
        mLE.var("A").lowerThan().var("B");
        assertEquals(mLE.toString(), "A <= B");
    }

    @Test
    public void testDisplay5() {
        mLE.var("A").greaterThan().var("B").plus(100);
        assertEquals(mLE.toString(), "A >= B + 100");
    }

    @Test
    public void testDisplay6() {
        mLE.var("A").plus("B").minus("C").plus(50).greaterThan().var("B").plus("C").minus(100);
        assertEquals(mLE.toString(), "A + B - C + 50 >= B + C - 100");
    }

    @Test
    public void testDisplay7() {
        mLE.var("A").lowerThan().var("B");
        mLE.normalize();
        assertEquals(mLE.toString(), "A + s1 = B");
    }

    @Test
    public void testDisplay8() {
        mLE.var("A").greaterThan().var("B");
        mLE.normalize();
        assertEquals(mLE.toString(), "A - s1 = B");
    }

    @Test
    public void testDisplay9() {
        mLE.var("A").greaterThan().var("B").withError();
        mLE.normalize();
        assertEquals(mLE.toString(), "A - s1 = B + e1+ - e1-");
    }

    @Test
    public void testDisplaySimplify() {
        mLE.var("A").plus(5).minus(2).plus(2, "B").minus(3, "B")
                .greaterThan().var("C").minus(3, "C").withError();
        assertEquals(mLE.toString(), "A + 5 - 2 + 2 B - 3 B >= C - 3 C + e1+ - e1-");
        mLE.normalize();
        assertEquals(mLE.toString(), "A + 5 - 2 + 2 B - 3 B - s1 = C - 3 C + e1+ - e1-");
        mLE.simplify();
        assertEquals(mLE.toString(), "3 + A - B - s1 = - 2 C + e1+ - e1-");
    }

    @Test
    public void testDisplayBalance1() {
        mLE.var("A").plus(5).minus(2).plus(2, "B").minus(3, "B")
                .greaterThan().var("C").minus(3, "C").withError();
        mLE.normalize();
        try {
            mLE.balance();
        } catch (Exception e) {
            System.err.println("Exception raised: " + e);
        }
        assertEquals(mLE.toString(), "A = - 3 + B - 2 C + e1+ - e1- + s1");
    }

    @Test
    public void testDisplayBalance2() {
        mLE.plus(5).minus(2).minus(2, "A").minus(3, "B").equalsTo().var(5, "C");
        try {
            mLE.balance();
        } catch (Exception e) {
            System.err.println("Exception raised: " + e);
        }
        assertEquals(mLE.toString(), "A = 3/2 - 3/2 B - 5/2 C");
    }

    @Test
    public void testDisplayBalance3() {
        mLE.plus(5).equalsTo().var(3);
        try {
            mLE.balance();
        } catch (Exception e) {
            assertTrue(true);
        }
        assertFalse(false);
    }

    @Test
    public void testDisplayBalance4() {
        // s1 = - 200 - e1- + 236 + e1- + e2+ - e2-
        mLE.withSlack().equalsTo().var(-200).withError("e1-", -1).plus(236);
        mLE.withError("e1-", 1).withError("e2+", 1).withError("e2-", -1);
        try {
            mLE.balance();
        } catch (Exception e) {
            System.err.println("Exception raised: " + e);
        }
        assertEquals(mLE.toString(), "s1 = 36 + e2+ - e2-");
    }

    @Test
    public void testDisplayBalance5() {
        // 236 + e1- + e2+ - e2- = e1- - e2+ + e2-
        mLE.var(236).withError("e1-", 1).withError("e2+", 1).withError("e2-", -1);
        mLE.equalsTo().withError("e1-", 1).withError("e2+", -1).withError("e2-", 1);
        try {
            mLE.balance();
        } catch (Exception e) {
            System.err.println("Exception raised: " + e);
        }
        // 236 + e1- + e2+ - e2- = e1- - e2+ + e2-
        // 0 = e1- - e2+ + e2- -236 -e1- - e2+ + e2-
        // 0 =     - e2+ + e2- -236      - e2+ + e2-
        // 0 = -236 - 2 e2+ + 2 e2-
        // 2 e2+ = -236 + 2 e2-
        // e2+ = -118 + e2-
        assertEquals(mLE.toString(), "e2+ = - 118 + e2-");
    }
}
