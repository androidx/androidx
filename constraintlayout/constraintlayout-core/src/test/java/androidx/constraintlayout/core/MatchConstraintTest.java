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
package androidx.constraintlayout.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;

import org.junit.Test;

public class MatchConstraintTest {

    @Test
    public void testSimpleMinMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 150, 200, 1);
        root.add(a);
        root.add(b);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 150);
        assertEquals(b.getWidth(), 100);
        assertEquals(root.getWidth(), 150);
        b.setWidth(200);
        root.setWidth(0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 200);
        assertEquals(b.getWidth(), 200);
        assertEquals(root.getWidth(), 200);
        b.setWidth(300);
        root.setWidth(0);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 200);
        assertEquals(b.getWidth(), 300);
        assertEquals(root.getWidth(), 300);
    }

    @Test
    public void testMinMaxMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        Guideline guidelineA = new Guideline();
        guidelineA.setOrientation(Guideline.VERTICAL);
        guidelineA.setGuideBegin(100);
        Guideline guidelineB = new Guideline();
        guidelineB.setOrientation(Guideline.VERTICAL);
        guidelineB.setGuideEnd(100);
        root.add(guidelineA);
        root.add(guidelineB);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        a.connect(ConstraintAnchor.Type.LEFT, guidelineA, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, guidelineB, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 150, 200, 1);
        root.add(a);
        root.setDebugName("root");
        guidelineA.setDebugName("guideline A");
        guidelineB.setDebugName("guideline B");
        a.setDebugName("A");
        root.layout();
        System.out.println("a) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 800);
        assertEquals(a.getWidth(), 200);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setWidth(100);
        root.layout();
        System.out.println("b) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 350);
        assertEquals(a.getWidth(), 150);

        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 150, 200, 1);
        root.layout();
        System.out.println("c) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 350);
        assertEquals(a.getWidth(), 150);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.setWidth(800);
        root.layout();
        System.out.println("d) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 800);
        assertEquals(a.getWidth(), 150); // because it's wrap
        a.setWidth(250);
        root.layout();
        System.out.println("e) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 800);
        assertEquals(a.getWidth(), 200);

        a.setWidth(700);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 150, 0, 1);
        root.layout();
        System.out.println("f) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 800);
        assertEquals(a.getWidth(), 600);
        a.setWidth(700);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 150, 0, 1);
        root.layout();
        System.out.println("g) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 800);
        assertEquals(a.getWidth(), 600);

        a.setWidth(700);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setWidth(0);
        root.layout();
        System.out.println("h) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
        assertEquals(root.getWidth(), 900);
        assertEquals(a.getWidth(), 700);
        a.setWidth(700);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 150, 0, 1);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        assertEquals(root.getWidth(), 350);
        assertEquals(a.getWidth(), 150);
        System.out.println("i) root: " + root + " guideA: " + guidelineA
                + " A: " + a + " guideB: " + guidelineB);
    }

    @Test
    public void testSimpleHorizontalMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);

        root.add(a);
        root.add(b);
        root.add(c);

        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertTrue(c.getLeft() >= a.getRight());
        assertTrue(c.getRight() <= b.getLeft());
        assertEquals(c.getLeft() - a.getRight(), b.getLeft() - c.getRight());

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 600);
        assertTrue(c.getLeft() >= a.getRight());
        assertTrue(c.getRight() <= b.getLeft());
        assertEquals(c.getLeft() - a.getRight(), b.getLeft() - c.getRight());

        c.setWidth(144);
        c.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 144);
        assertTrue(c.getLeft() >= a.getRight());
        assertTrue(c.getRight() <= b.getLeft());
        assertEquals(c.getLeft() - a.getRight(), b.getLeft() - c.getRight());

        c.setWidth(1000);
        c.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        root.layout();
        System.out.println("d) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 600);
        assertTrue(c.getLeft() >= a.getRight());
        assertTrue(c.getRight() <= b.getLeft());
        assertEquals(c.getLeft() - a.getRight(), b.getLeft() - c.getRight());
    }


    @Test
    public void testDanglingRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
    }
}
