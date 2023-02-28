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

public class LinearSystemTest {

    LinearSystem mLS;

    @Before
    public void setUp() {
        mLS = new LinearSystem();
        LinearEquation.resetNaming();
    }

    void add(LinearEquation equation) {
        ArrayRow row1 = LinearEquation.createRowFromEquation(mLS, equation);
        mLS.addConstraint(row1);
    }

    void add(LinearEquation equation, int strength) {
        System.out.println("Add equation <" + equation + ">");
        ArrayRow row1 = LinearEquation.createRowFromEquation(mLS, equation);
        System.out.println("Add equation row <" + row1 + ">");
        row1.addError(mLS, strength);
        mLS.addConstraint(row1);
    }

    @Test
    public void testMinMax() {
        // this shows how basic min/max + wrap works.
        // Need to modify ConstraintWidget to generate this.
//        solver.addConstraint(new ClLinearEquation(Rl, 0));
//        solver.addConstraint(new ClLinearEquation(Al, 0));
//        solver.addConstraint(new ClLinearEquation(Bl, 0));
//        solver.addConstraint(new ClLinearEquation(Br, Plus(Bl, 1000)));
//        solver.addConstraint(new ClLinearEquation(Al,
//              new ClLinearExpression(Rl), ClStrength.weak));
//        solver.addConstraint(new ClLinearEquation(Ar,
//              new ClLinearExpression(Rr), ClStrength.weak));
//        solver.addConstraint(new ClLinearInequality(Ar, GEQ, Plus(Al, 150), ClStrength.medium));
//        solver.addConstraint(new ClLinearInequality(Ar, LEQ, Plus(Al, 200), ClStrength.medium));
//        solver.addConstraint(new ClLinearInequality(Rr, GEQ, new ClLinearExpression(Br)));
//        solver.addConstraint(new ClLinearInequality(Rr, GEQ, new ClLinearExpression(Ar)));
        add(new LinearEquation(mLS).var("Rl").equalsTo().var(0));
//        add(new LinearEquation(s).var("Al").equalsTo().var(0));
//        add(new LinearEquation(s).var("Bl").equalsTo().var(0));
        add(new LinearEquation(mLS).var("Br").equalsTo().var("Bl").plus(300));
        add(new LinearEquation(mLS).var("Al").equalsTo().var("Rl"), 1);
        add(new LinearEquation(mLS).var("Ar").equalsTo().var("Rr"), 1);
        add(new LinearEquation(mLS).var("Ar").greaterThan().var("Al").plus(150), 2);
        add(new LinearEquation(mLS).var("Ar").lowerThan().var("Al").plus(200), 2);
        add(new LinearEquation(mLS).var("Rr").greaterThan().var("Ar"));
        add(new LinearEquation(mLS).var("Rr").greaterThan().var("Br"));
        add(new LinearEquation(mLS).var("Al").minus("Rl").equalsTo().var("Rr").minus("Ar"));
        add(new LinearEquation(mLS).var("Bl").minus("Rl").equalsTo().var("Rr").minus("Br"));
        try {
            mLS.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Result: ");
        mLS.displayReadableRows();
        assertEquals(mLS.getValueFor("Al"), 50.0f, 0f);
        assertEquals(mLS.getValueFor("Ar"), 250.0f, 0f);
        assertEquals(mLS.getValueFor("Bl"), 0.0f, 0f);
        assertEquals(mLS.getValueFor("Br"), 300.0f, 0f);
        assertEquals(mLS.getValueFor("Rr"), 300.0f, 0f);
    }

    @Test
    public void testPriorityBasic() {
        add(new LinearEquation(mLS).var(2, "Xm").equalsTo().var("Xl").plus("Xr"));
        add(new LinearEquation(mLS).var("Xl").plus(10).lowerThan().var("Xr"));
        //       add(new LinearEquation(s).var("Xl").greaterThan().var(0));
        add(new LinearEquation(mLS).var("Xr").lowerThan().var(100));
        add(new LinearEquation(mLS).var("Xm").equalsTo().var(50), 2);
        add(new LinearEquation(mLS).var("Xl").equalsTo().var(30), 1);
        add(new LinearEquation(mLS).var("Xr").equalsTo().var(60), 1);
        try {
            mLS.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Result: ");
        mLS.displayReadableRows();
        assertEquals(mLS.getValueFor("Xm"), 50.0f, 0f); // 50
        assertEquals(mLS.getValueFor("Xl"), 40.0f, 0f); // 30
        assertEquals(mLS.getValueFor("Xr"), 60.0f, 0f); // 70
    }

    @Test
    public void testPriorities() {
        // | <- a -> | b
        // a - zero = c - a
        // 2a = c + zero
        // a = (c + zero ) / 2
        add(new LinearEquation(mLS).var("b").equalsTo().var(100), 3);
        add(new LinearEquation(mLS).var("zero").equalsTo().var(0), 3);
        add(new LinearEquation(mLS).var("a").equalsTo().var(300), 0);
        add(new LinearEquation(mLS).var("c").equalsTo().var(200), 0);

        add(new LinearEquation(mLS).var("c").lowerThan().var("b").minus(10), 2);
        add(new LinearEquation(mLS).var("a").lowerThan().var("c"), 2);

        add(new LinearEquation(mLS).var("a").minus("zero").equalsTo().var("c").minus("a"), 1);

        try {
            mLS.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Result: ");
        mLS.displayReadableRows();
        assertEquals(mLS.getValueFor("zero"), 0.0f, 0f);
        assertEquals(mLS.getValueFor("a"), 45.0f, 0f);
        assertEquals(mLS.getValueFor("b"), 100.0f, 0f);
        assertEquals(mLS.getValueFor("c"), 90.0f, 0f);
    }

    @Test
    public void testOptimizeAndPriority() {
        mLS.reset();
        LinearEquation eq1 = new LinearEquation(mLS);
        LinearEquation eq2 = new LinearEquation(mLS);
        LinearEquation eq3 = new LinearEquation(mLS);
        LinearEquation eq4 = new LinearEquation(mLS);
        LinearEquation eq5 = new LinearEquation(mLS);
        LinearEquation eq6 = new LinearEquation(mLS);
        LinearEquation eq7 = new LinearEquation(mLS);
        LinearEquation eq8 = new LinearEquation(mLS);
        LinearEquation eq9 = new LinearEquation(mLS);
        LinearEquation eq10 = new LinearEquation(mLS);

        eq1.var("Root.left").equalsTo().var(0);
        eq2.var("Root.right").equalsTo().var(600);
        eq3.var("A.right").equalsTo().var("A.left").plus(100); //*
        eq4.var("A.left").greaterThan().var("Root.left"); //*
        eq10.var("A.left").equalsTo().var("Root.left"); //*
        eq5.var("A.right").lowerThan().var("B.left");
        eq6.var("B.right").greaterThan().var("B.left");
        eq7.var("B.right").lowerThan().var("Root.right");
        eq8.var("B.left").equalsTo().var("A.right");
        eq9.var("B.right").greaterThan().var("Root.right");

        ArrayRow row1 = LinearEquation.createRowFromEquation(mLS, eq1);
        mLS.addConstraint(row1);

        ArrayRow row2 = LinearEquation.createRowFromEquation(mLS, eq2);
        mLS.addConstraint(row2);

        ArrayRow row3 = LinearEquation.createRowFromEquation(mLS, eq3);
        mLS.addConstraint(row3);

        ArrayRow row10 = LinearEquation.createRowFromEquation(mLS, eq10);
        mLS.addSingleError(row10, 1, SolverVariable.STRENGTH_MEDIUM);
        mLS.addSingleError(row10, -1, SolverVariable.STRENGTH_MEDIUM);
        mLS.addConstraint(row10);

        ArrayRow row4 = LinearEquation.createRowFromEquation(mLS, eq4);
        mLS.addSingleError(row4, -1, SolverVariable.STRENGTH_HIGH);
        mLS.addConstraint(row4);

        ArrayRow row5 = LinearEquation.createRowFromEquation(mLS, eq5);
        mLS.addSingleError(row5, 1, SolverVariable.STRENGTH_MEDIUM);
        mLS.addConstraint(row5);

        ArrayRow row6 = LinearEquation.createRowFromEquation(mLS, eq6);
        mLS.addSingleError(row6, -1, SolverVariable.STRENGTH_LOW);
        mLS.addConstraint(row6);

        ArrayRow row7 = LinearEquation.createRowFromEquation(mLS, eq7);
        mLS.addSingleError(row7, 1, SolverVariable.STRENGTH_LOW);
        mLS.addConstraint(row7);

        ArrayRow row8 = LinearEquation.createRowFromEquation(mLS, eq8);
        row8.addError(mLS, SolverVariable.STRENGTH_LOW);
        mLS.addConstraint(row8);

        ArrayRow row9 = LinearEquation.createRowFromEquation(mLS, eq9);
        mLS.addSingleError(row9, -1, SolverVariable.STRENGTH_LOW);
        mLS.addConstraint(row9);

        try {
            mLS.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPriority() {
        for (int i = 0; i < 3; i++) {
            System.out.println("\n*** TEST PRIORITY ***\n");
            mLS.reset();
            LinearEquation eq1 = new LinearEquation(mLS);
            eq1.var("A").equalsTo().var(10);
            ArrayRow row1 = LinearEquation.createRowFromEquation(mLS, eq1);
            row1.addError(mLS, i % 3);
            mLS.addConstraint(row1);

            LinearEquation eq2 = new LinearEquation(mLS);
            eq2.var("A").equalsTo().var(100);
            ArrayRow row2 = LinearEquation.createRowFromEquation(mLS, eq2);
            row2.addError(mLS, (i + 1) % 3);
            mLS.addConstraint(row2);

            LinearEquation eq3 = new LinearEquation(mLS);
            eq3.var("A").equalsTo().var(1000);
            ArrayRow row3 = LinearEquation.createRowFromEquation(mLS, eq3);
            row3.addError(mLS, (i + 2) % 3);
            mLS.addConstraint(row3);

            try {
                mLS.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Check at iteration " + i);
            mLS.displayReadableRows();
            if (i == 0) {
                assertEquals(mLS.getValueFor("A"), 1000.0f, 0f);
            } else if (i == 1) {
                assertEquals(mLS.getValueFor("A"), 100.0f, 0f);
            } else if (i == 2) {
                assertEquals(mLS.getValueFor("A"), 10.0f, 0f);
            }
        }
    }

    @Test
    public void testAddEquation1() {
        LinearEquation e1 = new LinearEquation(mLS);
        e1.var("W3.left").equalsTo().var(0);
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e1));
        //s.rebuildGoalFromErrors();
        String result = mLS.getGoal().toString();
        assertTrue((result.equals("0 = 0.0") || result.equals(" goal -> (0.0) : ")));
        assertEquals(mLS.getValueFor("W3.left"), 0.0f, 0f);
    }

    @Test
    public void testAddEquation2() {
        LinearEquation e1 = new LinearEquation(mLS);
        e1.var("W3.left").equalsTo().var(0);
        LinearEquation e2 = new LinearEquation(mLS);
        e2.var("W3.right").equalsTo().var(600);
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e1));
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e2));
        //s.rebuildGoalFromErrors();
        String result = mLS.getGoal().toString();
        assertTrue((result.equals("0 = 0.0") || result.equals(" goal -> (0.0) : ")));
        assertEquals(mLS.getValueFor("W3.left"), 0.0f, 0f);
        assertEquals(mLS.getValueFor("W3.right"), 600.0f, 0f);
    }

    @Test
    public void testAddEquation3() {
        LinearEquation e1 = new LinearEquation(mLS);
        e1.var("W3.left").equalsTo().var(0);
        LinearEquation e2 = new LinearEquation(mLS);
        e2.var("W3.right").equalsTo().var(600);
        LinearEquation left_constraint = new LinearEquation(mLS);
        left_constraint.var("W4.left").equalsTo().var("W3.left"); // left constraint
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e1));
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e2));
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, left_constraint)); // left
        //s.rebuildGoalFromErrors();
        assertEquals(mLS.getValueFor("W3.left"), 0.0f, 0f);
        assertEquals(mLS.getValueFor("W3.right"), 600.0f, 0f);
        assertEquals(mLS.getValueFor("W4.left"), 0.0f, 0f);
    }

    @Test
    public void testAddEquation4() {
        LinearEquation e1 = new LinearEquation(mLS);
        LinearEquation e2 = new LinearEquation(mLS);
        LinearEquation e3 = new LinearEquation(mLS);
        LinearEquation e4 = new LinearEquation(mLS);
        e1.var(2, "Xm").equalsTo().var("Xl").plus("Xr");
        LinearSystem.Row goalRow = mLS.getGoal();
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e1)); // 2 Xm = Xl + Xr
        goalRow.addError(mLS.getVariable("Xm", SolverVariable.Type.ERROR));
        goalRow.addError(mLS.getVariable("Xl", SolverVariable.Type.ERROR));
//        assertEquals(s.getRow(0).toReadableString(), "Xm = 0.5 Xl + 0.5 Xr", 0f);
        e2.var("Xl").plus(10).lowerThan().var("Xr");
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e2)); // Xl + 10 <= Xr

//        assertEquals(s.getRow(0).toReadableString(), "Xm = 5.0 + Xl + 0.5 s1", 0f);
//        assertEquals(s.getRow(1).toReadableString(), "Xr = 10.0 + Xl + s1", 0f);
        e3.var("Xl").greaterThan().var(-10);
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e3)); // Xl >= -10
//        assertEquals(s.getRow(0).toReadableString(), "Xm = -5.0 + 0.5 s1 + s2", 0f);
//        assertEquals(s.getRow(1).toReadableString(), "Xr = s1 + s2", 0f);
//        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2", 0f);
        e4.var("Xr").lowerThan().var(100);
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e4)); // Xr <= 100
//        assertEquals(s.getRow(0).toReadableString(), "Xm = 45.0 + 0.5 s2 - 0.5 s3", 0f);
//        assertEquals(s.getRow(1).toReadableString(), "Xr = 100.0 - s3", 0f);
//        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2", 0f);
//        assertEquals(s.getRow(3).toReadableString(), "s1 = 100.0 - s2 - s3", 0f);
        //s.rebuildGoalFromErrors();
//        assertEquals(s.getGoal().toString(), "Goal: ", 0f);
        LinearEquation goal = new LinearEquation(mLS);
        goal.var("Xm").minus("Xl");
        try {
            mLS.minimizeGoal(LinearEquation.createRowFromEquation(mLS, goal)); //s.getGoal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int xl = (int) mLS.getValueFor("Xl");
        int xm = (int) mLS.getValueFor("Xm");
        int xr = (int) mLS.getValueFor("Xr");
//        assertEquals(xl, -10, 0f);
//        assertEquals(xm, 45, 0f);
//        assertEquals(xr, 100, 0f);
        LinearEquation e5 = new LinearEquation(mLS);
        e5.var("Xm").equalsTo().var(50);
        mLS.addConstraint(LinearEquation.createRowFromEquation(mLS, e5));
        try {
//            s.minimizeGoal(s.getGoal());
//            s.minimizeGoal(LinearEquation.createRowFromEquation(s, goal)); //s.getGoal());
            mLS.minimizeGoal(goalRow);
        } catch (Exception e) {
            e.printStackTrace();
        }
        xl = (int) mLS.getValueFor("Xl");
        xm = (int) mLS.getValueFor("Xm");
        xr = (int) mLS.getValueFor("Xr");
        assertEquals(xl, 0);
        assertEquals(xm, 50);
        assertEquals(xr, 100);
    }

}
