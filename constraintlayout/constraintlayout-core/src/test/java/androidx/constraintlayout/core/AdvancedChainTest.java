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

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintAnchor.Type;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;

import java.util.ArrayList;


public class AdvancedChainTest {

    @Test
    public void testComplexChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");

        a.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.RIGHT, root, Type.RIGHT);
        b.connect(Type.LEFT, root, Type.LEFT);
        b.connect(Type.RIGHT, root, Type.RIGHT);

        a.connect(Type.TOP, root, Type.TOP, 0);
        a.connect(Type.BOTTOM, b, Type.TOP, 0);

        b.connect(Type.TOP, a, Type.BOTTOM, 0);
        b.connect(Type.BOTTOM, root, Type.BOTTOM, 0);

        root.add(a);
        root.add(b);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);

        assertEquals(a.getWidth(), 800);
        assertEquals(b.getWidth(), 800);
        assertEquals(a.getHeight(), 400);
        assertEquals(b.getHeight(), 400);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), 400);

        a.setDimensionRatio("16:3");

        root.layout();

        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);

        assertEquals(a.getWidth(), 800);
        assertEquals(b.getWidth(), 800);
        assertEquals(a.getHeight(), 150);
        assertEquals(b.getHeight(), 150);
        assertEquals(a.getTop(), 167);
        assertEquals(b.getTop(), 483);

        b.setVerticalWeight(1);

        root.layout();

        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);

        assertEquals(a.getWidth(), 800);
        assertEquals(b.getWidth(), 800);
        assertEquals(a.getHeight(), 150);
        assertEquals(b.getHeight(), 650);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), 150);

        a.setVerticalWeight(1);

        root.layout();

        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);

        assertEquals(a.getWidth(), 800);
        assertEquals(b.getWidth(), 800);
        assertEquals(a.getHeight(), 150);
        assertEquals(b.getHeight(), 150);
        assertEquals(a.getTop(), 167);
        assertEquals(b.getTop(), 483);
    }

    @Test
    public void testTooSmall() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");

        root.add(a);
        root.add(b);
        root.add(c);

        a.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.TOP, root, Type.TOP);
        a.connect(Type.BOTTOM, root, Type.BOTTOM);

        b.connect(Type.LEFT, a, Type.RIGHT, 100);
        c.connect(Type.LEFT, a, Type.RIGHT, 100);

        b.connect(Type.TOP, a, Type.TOP);
        b.connect(Type.BOTTOM, c, Type.TOP);
        c.connect(Type.TOP, b, Type.BOTTOM);
        c.connect(Type.BOTTOM, a, Type.BOTTOM);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        assertEquals(a.getTop(), 390);
        assertEquals(b.getTop(), 380);
        assertEquals(c.getTop(), 400);
    }

    @Test
    public void testChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");

        a.connect(Type.LEFT, root, Type.LEFT, 0);
        a.connect(Type.RIGHT, b, Type.LEFT, 0);

        b.connect(Type.LEFT, a, Type.RIGHT, 0);
        b.connect(Type.RIGHT, root, Type.RIGHT, 0);

        root.add(a);
        root.add(b);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(0);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        assertEquals(a.getWidth(), 800, 1);
        assertEquals(b.getWidth(), 0, 1);
        assertEquals(a.getLeft(), 0, 1);
        assertEquals(b.getLeft(), 800, 1);
    }

    @Test
    public void testChain3Weights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");

        a.connect(Type.LEFT, root, Type.LEFT, 0);
        a.connect(Type.RIGHT, b, Type.LEFT, 0);

        b.connect(Type.LEFT, a, Type.RIGHT, 0);
        b.connect(Type.RIGHT, c, Type.LEFT, 0);

        c.connect(Type.LEFT, b, Type.RIGHT, 0);
        c.connect(Type.RIGHT, root, Type.RIGHT, 0);

        root.add(a);
        root.add(b);
        root.add(c);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);

        a.setHorizontalWeight(1);
        b.setHorizontalWeight(0);
        c.setHorizontalWeight(1);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);

        assertEquals(a.getWidth(), 400);
        assertEquals(b.getWidth(), 0);
        assertEquals(c.getWidth(), 400);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 400);
        assertEquals(c.getLeft(), 400);
    }

    @Test
    public void testChainLastGone() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.connect(Type.LEFT, root, Type.LEFT, 0);
        a.connect(Type.RIGHT, root, Type.RIGHT, 0);

        b.connect(Type.LEFT, root, Type.LEFT, 0);
        b.connect(Type.RIGHT, root, Type.RIGHT, 0);

        c.connect(Type.LEFT, root, Type.LEFT, 0);
        c.connect(Type.RIGHT, root, Type.RIGHT, 0);

        d.connect(Type.LEFT, root, Type.LEFT, 0);
        d.connect(Type.RIGHT, root, Type.RIGHT, 0);

        a.connect(Type.TOP, root, Type.TOP, 0);
        a.connect(Type.BOTTOM, b, Type.TOP, 0);
        b.connect(Type.TOP, a, Type.BOTTOM, 0);
        b.connect(Type.BOTTOM, c, Type.TOP, 0);
        c.connect(Type.TOP, b, Type.BOTTOM, 0);
        c.connect(Type.BOTTOM, d, Type.TOP, 0);
        d.connect(Type.TOP, c, Type.BOTTOM, 0);
        d.connect(Type.BOTTOM, root, Type.BOTTOM, 0);

        b.setVisibility(ConstraintWidget.GONE);
        d.setVisibility(ConstraintWidget.GONE);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        System.out.println("D: " + d);

        assertEquals(a.getTop(), 253);
        assertEquals(c.getTop(), 527);
    }

    @Test
    public void testRatioChainGone() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget ratio = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ratio.setDebugSolverName(root.getSystem(), "ratio");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(ratio);

        a.connect(Type.LEFT, root, Type.LEFT, 0);
        a.connect(Type.RIGHT, root, Type.RIGHT, 0);

        b.connect(Type.LEFT, root, Type.LEFT, 0);
        b.connect(Type.RIGHT, root, Type.RIGHT, 0);

        c.connect(Type.LEFT, root, Type.LEFT, 0);
        c.connect(Type.RIGHT, root, Type.RIGHT, 0);

        ratio.connect(Type.TOP, root, Type.TOP, 0);
        ratio.connect(Type.LEFT, root, Type.LEFT, 0);
        ratio.connect(Type.RIGHT, root, Type.RIGHT, 0);

        a.connect(Type.TOP, root, Type.TOP, 0);
        a.connect(Type.BOTTOM, b, Type.TOP, 0);
        b.connect(Type.TOP, a, Type.BOTTOM, 0);
        b.connect(Type.BOTTOM, ratio, Type.BOTTOM, 0);
        c.connect(Type.TOP, b, Type.TOP, 0);
        c.connect(Type.BOTTOM, ratio, Type.BOTTOM, 0);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        ratio.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);

        a.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        ratio.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        ratio.setDimensionRatio("4:3");

        b.setVisibility(ConstraintWidget.GONE);
        c.setVisibility(ConstraintWidget.GONE);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        System.out.println("ratio: " + ratio);

        assertEquals(a.getHeight(), 600);

        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        root.layout();

        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        System.out.println("ratio: " + ratio);
        System.out.println("root: " + root);

        assertEquals(a.getHeight(), 600);
    }

    @Test
    public void testSimpleHorizontalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(root);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - b.getRight(), 1);
        assertEquals(b.getLeft() - a.getRight(), 0, 1);
    }

    @Test
    public void testSimpleVerticalTChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(root);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - b.getBottom(), 1);
        assertEquals(b.getTop() - a.getBottom(), 0, 1);
    }

    @Test
    public void testHorizontalChainStyles() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.add(a);
        root.add(b);
        root.add(c);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("       spread) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        int gap = (root.getWidth() - a.getWidth() - b.getWidth() - c.getWidth()) / 4;
        int size = 100;
        assertEquals(a.getWidth(), size);
        assertEquals(b.getWidth(), size);
        assertEquals(c.getWidth(), size);
        assertEquals(gap, a.getLeft());
        assertEquals(a.getRight() + gap, b.getLeft());
        assertEquals(root.getWidth() - gap - c.getWidth(), c.getLeft());
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("spread inside) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        gap = (root.getWidth() - a.getWidth() - b.getWidth() - c.getWidth()) / 2;
        assertEquals(a.getWidth(), size);
        assertEquals(b.getWidth(), size);
        assertEquals(c.getWidth(), size);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getRight() + gap, b.getLeft());
        assertEquals(root.getWidth(), c.getRight());
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("       packed) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), size);
        assertEquals(b.getWidth(), size);
        assertEquals(c.getWidth(), size);
        assertEquals(a.getLeft(), gap);
        assertEquals(root.getWidth() - gap, c.getRight());
    }

    @Test
    public void testVerticalChainStyles() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.add(a);
        root.add(b);
        root.add(c);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, 0);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("       spread) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        int gap = (root.getHeight() - a.getHeight() - b.getHeight() - c.getHeight()) / 4;
        int size = 20;
        assertEquals(a.getHeight(), size);
        assertEquals(b.getHeight(), size);
        assertEquals(c.getHeight(), size);
        assertEquals(gap, a.getTop());
        assertEquals(a.getBottom() + gap, b.getTop());
        assertEquals(root.getHeight() - gap - c.getHeight(), c.getTop());
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("spread inside) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        gap = (root.getHeight() - a.getHeight() - b.getHeight() - c.getHeight()) / 2;
        assertEquals(a.getHeight(), size);
        assertEquals(b.getHeight(), size);
        assertEquals(c.getHeight(), size);
        assertEquals(a.getTop(), 0);
        assertEquals(a.getBottom() + gap, b.getTop());
        assertEquals(root.getHeight(), c.getBottom());
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("       packed) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), size);
        assertEquals(b.getHeight(), size);
        assertEquals(c.getHeight(), size);
        assertEquals(a.getTop(), gap);
        assertEquals(root.getHeight() - gap, c.getBottom());
    }

    @Test
    public void testPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.add(a);
        root.add(b);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        int gap = (root.getWidth() - a.getWidth() - b.getWidth()) / 2;
        int size = 100;
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        root.setOptimizationLevel(0);
        System.out.println("       packed) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getWidth(), size);
        assertEquals(b.getWidth(), size);
        assertEquals(a.getLeft(), gap);
    }
}
