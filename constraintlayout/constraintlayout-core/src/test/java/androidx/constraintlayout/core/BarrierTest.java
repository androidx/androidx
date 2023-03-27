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

import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;


/**
 * Tests for Barriers
 */
public class BarrierTest {

    @Test
    public void barrierConstrainedWidth() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(200, 20);
        Barrier barrier = new Barrier();
        Guideline guidelineStart = new Guideline();
        Guideline guidelineEnd = new Guideline();
        guidelineStart.setOrientation(ConstraintWidget.VERTICAL);
        guidelineEnd.setOrientation(ConstraintWidget.VERTICAL);
        guidelineStart.setGuideBegin(30);
        guidelineEnd.setGuideEnd(20);

        root.setDebugSolverName(root.getSystem(), "root");
        guidelineStart.setDebugSolverName(root.getSystem(), "guidelineStart");
        guidelineEnd.setDebugSolverName(root.getSystem(), "guidelineEnd");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.LEFT);

        barrier.add(a);
        barrier.add(b);

        root.add(a);
        root.add(b);
        root.add(guidelineStart);
        root.add(guidelineEnd);
        root.add(barrier);

        a.connect(ConstraintAnchor.Type.LEFT, guidelineStart, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, guidelineEnd, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, guidelineStart, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, guidelineEnd, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalBiasPercent(1);
        b.setHorizontalBiasPercent(1);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("guidelineStart: " + guidelineStart);
        System.out.println("guidelineEnd: " + guidelineEnd);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("barrier: " + barrier);
        assertEquals(root.getWidth(), 250);
        assertEquals(guidelineStart.getLeft(), 30);
        assertEquals(guidelineEnd.getLeft(), 230);
        assertEquals(a.getLeft(), 130);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getLeft(), 30);
        assertEquals(b.getWidth(), 200);
        assertEquals(barrier.getLeft(), 30);
    }

    @Test
    public void barrierImage() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(200, 20);
        ConstraintWidget c = new ConstraintWidget(60, 60);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.RIGHT);

        barrier.add(a);
        barrier.add(b);

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);

        c.setHorizontalBiasPercent(1);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c + " barrier: " + barrier);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getLeft(), 0);
        assertEquals(b.getTop(), 580);
        assertEquals(c.getLeft(), 740);
        assertEquals(c.getTop(), 270);
        assertEquals(barrier.getLeft(), 200);
    }

    @Test
    public void barrierTooStrong() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(60, 60);
        ConstraintWidget b = new ConstraintWidget(100, 200);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.BOTTOM);

        barrier.add(b);

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        b.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c + " barrier: " + barrier);
        assertEquals(a.getLeft(), 740);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getLeft(), 0);
        assertEquals(b.getTop(), 60);
        assertEquals(b.getWidth(), 800);
        assertEquals(b.getHeight(), 200);
        assertEquals(c.getLeft(), 0);
        assertEquals(c.getTop(), 0);
        assertEquals(c.getWidth(), 800);
        assertEquals(c.getHeight(), 60);
        assertEquals(barrier.getBottom(), 260);
    }

    @Test
    public void barrierMax() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);

        root.add(a);
        root.add(barrier);
        root.add(b);

        barrier.setBarrierType(Barrier.RIGHT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.setHorizontalBiasPercent(0);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 150, 1);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(a.getLeft(), 0);
        assertEquals(barrier.getLeft(), 100);
        assertEquals(b.getLeft(), 100);
        assertEquals(b.getWidth(), 150);
    }

    @Test
    public void barrierCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);

        root.add(a);
        root.add(barrier);

        barrier.setBarrierType(Barrier.RIGHT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, barrier, ConstraintAnchor.Type.RIGHT, 30);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        root.layout();

        System.out.println("A: " + a + " barrier: " + barrier);
        assertEquals(a.getLeft(), 10);
        assertEquals(barrier.getLeft(), 140);
    }

    @Test
    public void barrierCenter2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);

        root.add(a);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        a.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT, 30);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        root.layout();

        System.out.println("A: " + a + " barrier: " + barrier);
        assertEquals(a.getRight(), root.getWidth() - 10);
        assertEquals(barrier.getLeft(), a.getLeft() - 30);
    }

    @Test
    public void barrierCenter3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);
        barrier.add(b);

        root.add(a);
        root.add(b);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        a.setWidth(100);
        b.setWidth(200);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        a.setHorizontalBiasPercent(1);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        b.setHorizontalBiasPercent(1);

        root.layout();

        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 200);
        assertEquals(barrier.getLeft(), b.getLeft());
    }

    @Test
    public void barrierCenter4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(150, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);
        barrier.add(b);

        root.add(a);
        root.add(b);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalBiasPercent(0);
        b.setHorizontalBiasPercent(0);

        root.layout();

        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(a.getRight(), root.getWidth());
        assertEquals(barrier.getLeft(), Math.min(a.getLeft(), b.getLeft()));
        assertEquals(a.getLeft(), barrier.getLeft());
        assertEquals(b.getLeft(), barrier.getLeft());
    }

    @Test
    public void barrierCenter5() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        ConstraintWidget c = new ConstraintWidget(200, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(a);
        barrier.add(b);
        barrier.add(c);

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        barrier.setBarrierType(Barrier.RIGHT);

        a.connect(ConstraintAnchor.Type.RIGHT, barrier, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.RIGHT, barrier, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);

        c.connect(ConstraintAnchor.Type.RIGHT, barrier, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalBiasPercent(0);
        b.setHorizontalBiasPercent(0);
        c.setHorizontalBiasPercent(0);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        c.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        root.layout();

        System.out.println("A: " + a + " B: " + b + " C: " + c + " barrier: " + barrier);
        assertEquals(barrier.getRight(),
                Math.max(Math.max(a.getRight(), b.getRight()), c.getRight()));
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 150);
        assertEquals(c.getWidth(), 200);
    }


    @Test
    public void basic() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 20);

        barrier.add(a);
        barrier.add(b);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), b.getLeft());

        barrier.setBarrierType(Barrier.RIGHT);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(barrier.getRight(), b.getRight());

        barrier.setBarrierType(Barrier.LEFT);
        b.setWidth(10);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), a.getLeft());
    }

    @Test
    public void basic2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(barrier);

        barrier.setBarrierType(Barrier.BOTTOM);

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, barrier, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        barrier.add(a);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(barrier.getTop(), a.getBottom());
        float actual = barrier.getBottom()
                + (root.getBottom() - barrier.getBottom() - b.getHeight()) / 2f;
        assertEquals((float) b.getTop(), actual, 1f);
    }

    @Test
    public void basic3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(barrier);

        barrier.setBarrierType(Barrier.RIGHT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        barrier.add(a);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("root: " + root + " A: " + a + " B: " + b + " barrier: " + barrier);
        assertEquals(barrier.getRight(), a.getRight());
        assertEquals(root.getWidth(), a.getWidth() + b.getWidth());
    }

    @Test
    public void basic4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.setVisibility(ConstraintWidget.GONE);

        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.TOP, barrier, ConstraintAnchor.Type.TOP);

        barrier.add(a);
        barrier.add(b);

        barrier.setBarrierType(Barrier.BOTTOM);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " barrier: " + barrier);
        assertEquals(b.getTop(), a.getBottom());
        assertEquals(barrier.getTop(), b.getBottom());
        assertEquals(c.getTop(), barrier.getTop());
    }

    @Test
    public void growArray() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        ConstraintWidget c = new ConstraintWidget(175, 20);
        ConstraintWidget d = new ConstraintWidget(200, 20);
        ConstraintWidget e = new ConstraintWidget(125, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        e.setDebugSolverName(root.getSystem(), "E");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 20);

        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, 20);

        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM, 20);


        e.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        e.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        e.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.BOTTOM, 20);

        barrier.add(a);
        barrier.add(b);
        barrier.add(c);
        barrier.add(d);
        barrier.add(e);

        root.layout();

        System.out.println("A: " + a
                + " B: " + b + " C: " + c + " D: " + d + " E: " + e + " barrier: " + barrier);
        assertEquals(a.getLeft(), (root.getWidth() - a.getWidth()) / 2, 1);
        assertEquals(b.getLeft(), (root.getWidth() - b.getWidth()) / 2, 1);
        assertEquals(c.getLeft(), (root.getWidth() - c.getWidth()) / 2, 1);
        assertEquals(d.getLeft(), (root.getWidth() - d.getWidth()) / 2, 1);
        assertEquals(e.getLeft(), (root.getWidth() - e.getWidth()) / 2, 1);
        assertEquals(barrier.getLeft(), d.getLeft());
    }

    @Test
    public void connection() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(150, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 20);

        c.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT, 0);
        barrier.add(a);
        barrier.add(b);

        root.layout();

        System.out.println("A: " + a + " B: " + b + " C: " + c + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), b.getLeft());
        assertEquals(c.getLeft(), barrier.getLeft());

    }

    @Test
    public void withGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();
        Guideline guideline = new Guideline();

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        guideline.setDebugSolverName(root.getSystem(), "Guideline");

        guideline.setOrientation(ConstraintWidget.VERTICAL);
        guideline.setGuideBegin(200);
        barrier.setBarrierType(Barrier.RIGHT);

        root.add(a);
        root.add(barrier);
        root.add(guideline);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        barrier.add(a);
        barrier.add(guideline);

        root.layout();

        System.out.println("A: " + a + " guideline: " + guideline + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), guideline.getLeft());
    }

    @Test
    public void wrapIssue() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.BOTTOM);

        root.add(a);
        root.add(b);
        root.add(barrier);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);

        barrier.add(a);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.TOP, barrier, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("1/ root: " + root + " A: " + a + " B: " + b + " barrier: " + barrier);

        assertEquals(barrier.getTop(), a.getBottom());
        assertEquals(b.getTop(), barrier.getBottom());
        assertEquals(root.getHeight(), a.getHeight() + b.getHeight());

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        root.layout();
        System.out.println("2/ root: " + root + " A: " + a + " B: " + b + " barrier: " + barrier);

        assertEquals(barrier.getTop(), a.getBottom());
        assertEquals(b.getTop(), barrier.getBottom());
        assertEquals(root.getHeight(), a.getHeight() + b.getHeight());
    }
}
