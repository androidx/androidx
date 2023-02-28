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
 * Basic wrap test
 */
public class WrapTest {

    @Test
    public void testBasic() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("a) root: " + root + " A: " + a);

        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 100, 0);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 60, 0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
    }

    @Test
    public void testBasic2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 100, 1);
        b.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 60, 1);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 200);
        assertEquals(root.getHeight(), 40);

        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 20, 100, 1);
        b.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 30, 60, 1);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 220);
        assertEquals(root.getHeight(), 70);
    }

    @Test
    public void testRatioWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 100, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");

        root.setHeight(0);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 100);

        root.setHeight(600);
        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 600);
        assertEquals(root.getHeight(), 600);

        root.setWidth(100);
        root.setHeight(600);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 0);
        assertEquals(root.getHeight(), 0);
    }

    @Test
    public void testRatioWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 120);
    }

    @Test
    public void testRatioWrap3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 60);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);

        a.setBaselineDistance(100);
        b.setBaselineDistance(10);
        c.setBaselineDistance(10);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.setVerticalBiasPercent(0);

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.BASELINE, b, ConstraintAnchor.Type.BASELINE);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getWidth(), 300);
        assertEquals(a.getHeight(), 300);
        assertEquals(b.getLeft(), 300);
        assertEquals(b.getTop(), 90);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);
        assertEquals(c.getLeft(), 400);
        assertEquals(c.getTop(), 90);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 20);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setBaselineDistance(10);

        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 220);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testGoneChainWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(root.getHeight(), 40);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(root.getHeight(), 40);
    }

    @Test
    public void testWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 0);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 40);
        ConstraintWidget e = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);

        e.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        e.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        e.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + a
                + " B: " + b + " C: " + c + " D: " + d + " E: " + e);
        assertEquals(root.getHeight(), 80);
        assertEquals(e.getTop(), 30);
    }

    @Test
    public void testWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM, 30);
        a.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.TOP, 40);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(c.getTop(), 0);
        assertEquals(a.getTop(), c.getBottom() + 30);
        assertEquals(d.getTop(), a.getBottom() + 40);
        assertEquals(root.getHeight(), 20 + 30 + 20 + 40 + 20);

    }

    @Test
    public void testWrap3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 200);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 250);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b);
        assertEquals(root.getWidth(), a.getWidth() + 200);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 250);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getRight() > root.getWidth(), true);
    }

    @Test
    public void testWrap4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(80, 80);
        ConstraintWidget b = new ConstraintWidget(60, 60);
        ConstraintWidget c = new ConstraintWidget(50, 100);
        Barrier barrier1 = new Barrier();
        barrier1.setBarrierType(Barrier.BOTTOM);
        Barrier barrier2 = new Barrier();
        barrier2.setBarrierType(Barrier.BOTTOM);

        barrier1.add(a);
        barrier1.add(b);

        barrier2.add(c);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        barrier1.setDebugName("B1");
        barrier2.setDebugName("B2");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier1);
        root.add(barrier2);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, barrier1, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, barrier1, ConstraintAnchor.Type.BOTTOM);

        c.connect(ConstraintAnchor.Type.TOP, barrier1, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, barrier2, ConstraintAnchor.Type.TOP);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        System.out.println("B1: " + barrier1);
        System.out.println("B2: " + barrier2);
        assertEquals(a.getTop() >= 0, true);
        assertEquals(b.getTop() >= 0, true);
        assertEquals(c.getTop() >= 0, true);
        assertEquals(root.getHeight(), Math.max(a.getHeight(), b.getHeight()) + c.getHeight());

    }

    @Test
    public void testWrap5() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 8);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 8);

        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        d.setHorizontalBiasPercent(0.557f);
        d.setVerticalBiasPercent(0.8f);

        d.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        d.setHorizontalBiasPercent(0.557f);
        d.setVerticalBiasPercent(0.28f);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        System.out.println("D: " + d);
    }

    @Test
    public void testWrap6() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(ConstraintWidget.VERTICAL);
        guideline.setGuidePercent(0.5f);
        root.setDebugName("root");
        a.setDebugName("A");
        guideline.setDebugName("guideline");

        root.add(a);
        root.add(guideline);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("guideline: " + guideline);

        assertEquals(root.getWidth(), a.getWidth() * 2);
        assertEquals(root.getHeight(), a.getHeight() + 8);
        assertEquals((float) guideline.getLeft(), root.getWidth() / 2f, 0f);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
    }

    @Test
    public void testWrap7() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget divider = new ConstraintWidget(1, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(ConstraintWidget.VERTICAL);
        guideline.setGuidePercent(0.5f);
        root.setDebugName("root");
        a.setDebugName("A");
        divider.setDebugName("divider");
        guideline.setDebugName("guideline");

        root.add(a);
        root.add(divider);
        root.add(guideline);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        divider.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        divider.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        divider.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        divider.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        divider.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("divider: " + divider);
        System.out.println("guideline: " + guideline);

        assertEquals(root.getWidth(), a.getWidth() * 2);
        assertEquals(root.getHeight(), a.getHeight());
        assertEquals((float) guideline.getLeft(), root.getWidth() / 2f, 0f);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
    }

    @Test
    public void testWrap8() {
        // check_048
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1080);
        ConstraintWidget button56 = new ConstraintWidget(231, 126);
        ConstraintWidget button60 = new ConstraintWidget(231, 126);
        ConstraintWidget button63 = new ConstraintWidget(368, 368);
        ConstraintWidget button65 = new ConstraintWidget(231, 126);

        button56.setDebugName("button56");
        button60.setDebugName("button60");
        button63.setDebugName("button63");
        button65.setDebugName("button65");

        root.add(button56);
        root.add(button60);
        root.add(button63);
        root.add(button65);

        button56.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 42);
        button56.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 42);
        //button56.setBaselineDistance(77);

        button60.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 42);
        button60.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 79);
        //button60.setBaselineDistance(77);

        button63.connect(ConstraintAnchor.Type.LEFT, button56, ConstraintAnchor.Type.RIGHT, 21);
        button63.connect(ConstraintAnchor.Type.RIGHT, button60, ConstraintAnchor.Type.LEFT, 21);
        button63.connect(ConstraintAnchor.Type.TOP, button56, ConstraintAnchor.Type.BOTTOM, 21);
        button63.connect(ConstraintAnchor.Type.BOTTOM, button60, ConstraintAnchor.Type.TOP, 21);
        //button63.setBaselineDistance(155);
        button63.setVerticalBiasPercent(0.8f);

        button65.connect(ConstraintAnchor.Type.LEFT, button56, ConstraintAnchor.Type.RIGHT, 21);
        button65.connect(ConstraintAnchor.Type.RIGHT, button60, ConstraintAnchor.Type.LEFT, 21);
        button65.connect(ConstraintAnchor.Type.TOP, button56, ConstraintAnchor.Type.BOTTOM, 21);
        button65.connect(ConstraintAnchor.Type.BOTTOM, button60, ConstraintAnchor.Type.TOP, 21);
        //button65.setBaselineDistance(77);
        button65.setVerticalBiasPercent(0.28f);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("button56: " + button56);
        System.out.println("button60: " + button60);
        System.out.println("button63: " + button63);
        System.out.println("button65: " + button65);

        assertEquals(root.getWidth(), 1080);
        assertEquals(root.getHeight(), 783);
        assertEquals(button56.getLeft(), 42);
        assertEquals(button56.getTop(), 42);
        assertEquals(button60.getLeft(), 807);
        assertEquals(button60.getTop(), 578);
        assertEquals(button63.getLeft(), 356);
        assertEquals(button63.getTop(), 189);
        assertEquals(button65.getLeft(), 425);
        assertEquals(button65.getTop(), 257);
    }

    @Test
    public void testWrap9() {
        // b/161826272
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1080);
        ConstraintWidget text = new ConstraintWidget(270, 30);
        ConstraintWidget view = new ConstraintWidget(10, 10);

        root.setDebugName("root");
        text.setDebugName("text");
        view.setDebugName("view");

        root.add(text);
        root.add(view);

        text.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        text.connect(ConstraintAnchor.Type.TOP, view, ConstraintAnchor.Type.TOP);

        view.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        view.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        view.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        view.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        view.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        view.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        view.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.2f);
        view.setDimensionRatio("1:1");

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("root: " + root);
        System.out.println("text: " + text);
        System.out.println("view: " + view);

        assertEquals(view.getWidth(), view.getHeight());
        assertEquals(view.getHeight(), (int) (0.2 * root.getHeight()));
        assertEquals(root.getWidth(), Math.max(text.getWidth(), view.getWidth()));
    }

    @Test
    public void testBarrierWrap() {
        // b/165028374

        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1080);
        ConstraintWidget view = new ConstraintWidget(200, 200);
        ConstraintWidget space = new ConstraintWidget(50, 50);
        ConstraintWidget button = new ConstraintWidget(100, 80);
        ConstraintWidget text = new ConstraintWidget(90, 30);

        Barrier barrier = new Barrier();
        barrier.setBarrierType(Barrier.BOTTOM);
        barrier.add(button);
        barrier.add(space);

        root.setDebugName("root");
        view.setDebugName("view");
        space.setDebugName("space");
        button.setDebugName("button");
        text.setDebugName("text");
        barrier.setDebugName("barrier");

        root.add(view);
        root.add(space);
        root.add(button);
        root.add(text);
        root.add(barrier);

        view.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        space.connect(ConstraintAnchor.Type.TOP, view, ConstraintAnchor.Type.BOTTOM);
        button.connect(ConstraintAnchor.Type.TOP, view, ConstraintAnchor.Type.BOTTOM);
        button.connect(ConstraintAnchor.Type.BOTTOM, text, ConstraintAnchor.Type.TOP);
        text.connect(ConstraintAnchor.Type.TOP, barrier, ConstraintAnchor.Type.BOTTOM);
        text.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        button.setVerticalBiasPercent(1f);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("root: " + root);
        System.out.println("view: " + view);
        System.out.println("space: " + space);
        System.out.println("button: " + button);
        System.out.println("barrier: " + barrier);
        System.out.println("text: " + text);

        assertEquals(view.getTop(), 0);
        assertEquals(view.getBottom(), 200);
        assertEquals(space.getTop(), 200);
        assertEquals(space.getBottom(), 250);
        assertEquals(button.getTop(), 200);
        assertEquals(button.getBottom(), 280);
        assertEquals(barrier.getTop(), 280);
        assertEquals(text.getTop(), barrier.getTop());
    }

}
