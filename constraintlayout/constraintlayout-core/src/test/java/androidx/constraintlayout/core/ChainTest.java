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
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;

import java.util.ArrayList;

public class ChainTest {

    @Test
    public void testCenteringElementsWithSpreadChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);
        ConstraintWidget e = new ConstraintWidget(600, 20);

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

        a.connect(ConstraintAnchor.Type.LEFT, e, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, e, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);

        d.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);

        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c + " D: " + d + " E: " + e);
        assertEquals(a.getWidth(), 300);
        assertEquals(b.getWidth(), a.getWidth());
    }

    @Test
    public void testBasicChainMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_CHAIN);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getRight(), 200);
        assertEquals(b.getLeft(), 200);
        assertEquals(b.getRight(), 400);
        assertEquals(c.getLeft(), 400);
        assertEquals(c.getRight(), 600);
    }

    @Test
    public void testSpreadChainGone() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        a.setVisibility(ConstraintWidget.GONE);

        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getRight(), 0);
        assertEquals(b.getLeft(), 133);
        assertEquals(b.getRight(), 233);
        assertEquals(c.getLeft(), 367);
        assertEquals(c.getRight(), 467);
    }

    @Test
    public void testPackChainGone() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 100);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 20);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        b.setGoneMargin(ConstraintAnchor.Type.RIGHT, 100);
        c.setVisibility(ConstraintWidget.GONE);

        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 200);
        assertEquals(b.getLeft(), 300);
        assertEquals(c.getLeft(), 500);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 0);
    }

    @Test
    public void testSpreadInsideChain2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 25);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getRight(), 100);
        assertEquals(b.getLeft(), 100);
        assertEquals(b.getRight(), 475);
        assertEquals(c.getLeft(), 500);
        assertEquals(c.getRight(), 600);
    }


    @Test
    public void testPackChain2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 1);
        root.layout();
        System.out.println("e) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        // e) A: id: A (200, 0) - (100 x 20) B: id: B (300, 0) - (100 x 20) - pass
        // e) A: id: A (0, 0) - (100 x 20) B: id: B (100, 0) - (100 x 20)
    }

    @Test
    public void testPackChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 0);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        root.layout();
        System.out.println("d) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 0);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        a.setHeight(20);
        b.setVisibility(ConstraintWidget.VISIBLE);
        b.setWidth(100);
        b.setHeight(20);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 1);
        root.layout();
        System.out.println("e) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        root.layout();
        System.out.println("f) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 500);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 100);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 50, 1);
        root.layout();
        System.out.println("g) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 50);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.3f);
        root.layout();
        System.out.println("h) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), (int) (0.3f * 600));
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setDimensionRatio("16:9");
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 0, 1);
        root.layout();
        System.out.println("i) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), (int) (16f / 9f * 20), 1);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight(), 1);
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        b.setDimensionRatio(0, 0);
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        a.setHeight(20);
        b.setVisibility(ConstraintWidget.VISIBLE);
        b.setWidth(100);
        b.setHeight(20);
        root.layout();
        System.out.println("j) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getWidth() + b.getWidth(), root.getWidth());
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(3);
        root.layout();
        System.out.println("k) A: " + a + " B: " + b);
        assertEquals(a.getWidth() * 3, b.getWidth());
        assertEquals(a.getWidth() + b.getWidth(), root.getWidth());
    }

    /**
     * testPackChain with current Chain Optimizations.
     */
    @Test
    public void testPackChainOpt() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_DIRECT | Optimizer.OPTIMIZATION_BARRIER
                | Optimizer.OPTIMIZATION_CHAIN);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 0);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        root.layout();
        System.out.println("d) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 0);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        a.setHeight(20);
        b.setVisibility(ConstraintWidget.VISIBLE);
        b.setWidth(100);
        b.setHeight(20);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 1);
        root.layout();
        System.out.println("e) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        root.layout();
        System.out.println("f) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 500);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 100);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 50, 1);
        root.layout();
        System.out.println("g) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 50);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.3f);
        root.layout();
        System.out.println("h) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), (int) (0.3f * 600));
        assertEquals(a.getLeft(), root.getWidth() - b.getRight());
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        b.setDimensionRatio("16:9");
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 0, 1);
        root.layout();
        System.out.println("i) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), (int) (16f / 9f * 20), 1);
        assertEquals(a.getLeft(), root.getWidth() - b.getRight(), 1);
        assertEquals(b.getLeft(), a.getLeft() + a.getWidth());
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 0, 1);
        b.setDimensionRatio(0, 0);
        a.setVisibility(ConstraintWidget.VISIBLE);
        a.setWidth(100);
        a.setHeight(20);
        b.setVisibility(ConstraintWidget.VISIBLE);
        b.setWidth(100);
        b.setHeight(20);
        root.layout();
        System.out.println("j) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getWidth() + b.getWidth(), root.getWidth());
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(3);
        root.layout();
        System.out.println("k) A: " + a + " B: " + b);
        assertEquals(a.getWidth() * 3, b.getWidth());
        assertEquals(a.getWidth() + b.getWidth(), root.getWidth());
    }

    @Test
    public void testSpreadChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getLeft(), b.getLeft() - a.getRight(), 1);
        assertEquals(b.getLeft() - a.getRight(), root.getWidth() - b.getRight(), 1);
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
    }

    @Test
    public void testSpreadInsideChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getRight(), root.getWidth());

        b.reset();
        root.add(b);
        b.setDebugName("B");
        b.setWidth(100);
        b.setHeight(20);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(b.getLeft() - a.getRight(), c.getLeft() - b.getRight());
        int gap = (root.getWidth() - a.getWidth() - b.getWidth() - c.getWidth()) / 2;
        assertEquals(b.getLeft(), a.getRight() + gap);
    }

    @Test
    public void testBasicChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(root);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - b.getRight(), 1);
        assertEquals(a.getLeft() - root.getLeft(), b.getLeft() - a.getRight(), 1);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
        assertEquals(a.getWidth(), root.getWidth() - b.getWidth());
        assertEquals(b.getWidth(), 100);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setWidth(100);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(b.getWidth(), root.getWidth() - a.getWidth());
        assertEquals(a.getWidth(), 100);
    }

    @Test
    public void testBasicVerticalChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        a.setDebugName("A");
        b.setDebugName("B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(root);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - b.getBottom(), 1);
        assertEquals(a.getTop() - root.getTop(), b.getTop() - a.getBottom(), 1);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
        assertEquals(a.getHeight(), root.getHeight() - b.getHeight());
        assertEquals(b.getHeight(), 20);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setHeight(20);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b);
        assertEquals(b.getHeight(), root.getHeight() - a.getHeight());
        assertEquals(a.getHeight(), 20);
    }

    @Test
    public void testBasicChainThreeElements1() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        // all elements spread equally
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - c.getRight(), 1);
        assertEquals(a.getLeft() - root.getLeft(), b.getLeft() - a.getRight(), 1);
        assertEquals(b.getLeft() - a.getRight(), c.getLeft() - b.getRight(), 1);
        // a) A: id: A (125, 0) - (100 x 20) B: id: B (350, 0) - (100 x 20)
        // C: id: C (575, 0) - (100 x 20)
        // a) A: id: A (0, 0) - (100 x 20) B: id: B (100, 0) - (100 x 20)
        // C: id: C (450, 0) - (100 x 20)
    }

    @Test
    public void testBasicChainThreeElements() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        // all elements spread equally
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - c.getRight(), 1);
        assertEquals(a.getLeft() - root.getLeft(), b.getLeft() - a.getRight(), 1);
        assertEquals(b.getLeft() - a.getRight(), c.getLeft() - b.getRight(), 1);
        // A marked as 0dp, B == C, A takes the rest
        a.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        a.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        b.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        b.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        c.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        c.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft() - root.getLeft() - marginL,
                root.getRight() - c.getRight() - marginR);
        assertEquals(c.getLeft() - b.getRight(), b.getLeft() - a.getRight());
        int matchWidth = root.getWidth() - b.getWidth() - c.getWidth()
                - marginL - marginR - 4 * (b.getLeft() - a.getRight());
        assertEquals(a.getWidth(), 498);
        assertEquals(b.getWidth(), c.getWidth());
        assertEquals(b.getWidth(), 100);
        checkPositions(a, b, c);
        // B marked as 0dp, A == C, B takes the rest
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setWidth(100);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getWidth(), 498);
        assertEquals(a.getWidth(), c.getWidth());
        assertEquals(a.getWidth(), 100);
        checkPositions(a, b, c);
        // C marked as 0dp, A == B, C takes the rest
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setWidth(100);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("d) A: " + a + " B: " + b + " C: " + c);
        assertEquals(c.getWidth(), 498);
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getWidth(), 100);
        checkPositions(a, b, c);
        // A & B marked as 0dp, C == 100
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setWidth(100);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("e) A: " + a + " B: " + b + " C: " + c);
        assertEquals(c.getWidth(), 100);
        assertEquals(a.getWidth(), b.getWidth()); // L
        assertEquals(a.getWidth(), 299);
        checkPositions(a, b, c);
        // A & C marked as 0dp, B == 100
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setWidth(100);
        root.layout();
        System.out.println("f) A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getWidth(), 100);
        assertEquals(a.getWidth(), c.getWidth());
        assertEquals(a.getWidth(), 299);
        checkPositions(a, b, c);
        // B & C marked as 0dp, A == 100
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setWidth(100);
        root.layout();
        System.out.println("g) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), c.getWidth());
        assertEquals(b.getWidth(), 299);
        checkPositions(a, b, c);
        // A == 0dp, B & C == 100, C is gone
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setWidth(100);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setWidth(100);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setWidth(100);
        c.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("h) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 632);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 0);
        checkPositions(a, b, c);
    }

    private void checkPositions(ConstraintWidget a, ConstraintWidget b, ConstraintWidget c) {
        assertEquals(a.getLeft() <= a.getRight(), true);
        assertEquals(a.getRight() <= b.getLeft(), true);
        assertEquals(b.getLeft() <= b.getRight(), true);
        assertEquals(b.getRight() <= c.getLeft(), true);
        assertEquals(c.getLeft() <= c.getRight(), true);
    }

    @Test
    public void testBasicVerticalChainThreeElements() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, 0);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        // all elements spread equally
        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(b.getHeight(), c.getHeight(), 1);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - c.getBottom(), 1);
        assertEquals(a.getTop() - root.getTop(), b.getTop() - a.getBottom(), 1);
        assertEquals(b.getTop() - a.getBottom(), c.getTop() - b.getBottom(), 1);
        // A marked as 0dp, B == C, A takes the rest
        a.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        a.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        b.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        b.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        c.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        c.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getTop(), 7);
        assertEquals(c.getBottom(), 573);
        assertEquals(b.getBottom(), 519);
        assertEquals(a.getHeight(), 458);
        assertEquals(b.getHeight(), c.getHeight());
        assertEquals(b.getHeight(), 20);
        checkVerticalPositions(a, b, c);
        // B marked as 0dp, A == C, B takes the rest
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setHeight(20);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getHeight(), 458);
        assertEquals(a.getHeight(), c.getHeight());
        assertEquals(a.getHeight(), 20);
        checkVerticalPositions(a, b, c);
        // C marked as 0dp, A == B, C takes the rest
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHeight(20);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("d) A: " + a + " B: " + b + " C: " + c);
        assertEquals(c.getHeight(), 458);
        assertEquals(a.getHeight(), b.getHeight());
        assertEquals(a.getHeight(), 20);
        checkVerticalPositions(a, b, c);
        // A & B marked as 0dp, C == 20
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setHeight(20);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("e) A: " + a + " B: " + b + " C: " + c);
        assertEquals(c.getHeight(), 20);
        assertEquals(a.getHeight(), b.getHeight()); // L
        assertEquals(a.getHeight(), 239);
        checkVerticalPositions(a, b, c);
        // A & C marked as 0dp, B == 20
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHeight(20);
        root.layout();
        System.out.println("f) A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getHeight(), 20);
        assertEquals(a.getHeight(), c.getHeight());
        assertEquals(a.getHeight(), 239);
        checkVerticalPositions(a, b, c);
        // B & C marked as 0dp, A == 20
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setHeight(20);
        root.layout();
        System.out.println("g) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), 20);
        assertEquals(b.getHeight(), c.getHeight());
        assertEquals(b.getHeight(), 239);
        checkVerticalPositions(a, b, c);
        // A == 0dp, B & C == 20, C is gone
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHeight(20);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHeight(20);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setHeight(20);
        c.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("h) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), 512);
        assertEquals(b.getHeight(), 20);
        assertEquals(c.getHeight(), 0);
        checkVerticalPositions(a, b, c);
    }

    private void checkVerticalPositions(ConstraintWidget a,
            ConstraintWidget b,
            ConstraintWidget c) {
        assertEquals(a.getTop() <= a.getBottom(), true);
        assertEquals(a.getBottom() <= b.getTop(), true);
        assertEquals(b.getTop() <= b.getBottom(), true);
        assertEquals(b.getBottom() <= c.getTop(), true);
        assertEquals(c.getTop() <= c.getBottom(), true);
    }

    @Test
    public void testHorizontalChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, marginR);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, marginL);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, marginR);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, marginL);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(1);
        c.setHorizontalWeight(1);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(2);
        c.setHorizontalWeight(1);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(2 * a.getWidth(), b.getWidth(), 1);
        assertEquals(a.getWidth(), c.getWidth(), 1);
    }

    @Test
    public void testVerticalChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, marginB);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, marginT);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, marginB);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, marginT);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalWeight(1);
        b.setVerticalWeight(1);
        c.setVerticalWeight(1);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(b.getHeight(), c.getHeight(), 1);
        a.setVerticalWeight(1);
        b.setVerticalWeight(2);
        c.setVerticalWeight(1);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(2 * a.getHeight(), b.getHeight(), 1);
        assertEquals(a.getHeight(), c.getHeight(), 1);
    }

    @Test
    public void testHorizontalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, marginR);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, marginL);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, marginR);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, marginL);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft() - root.getLeft() - marginL,
                root.getRight() - marginR - c.getRight(), 1);
    }

    @Test
    public void testVerticalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        widgets.add(root);
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, marginB);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, marginT);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, marginB);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, marginT);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getTop() - root.getTop() - marginT,
                root.getBottom() - marginB - c.getBottom(), 1);
    }

    @Test
    public void testHorizontalChainComplex() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(50, 20);
        ConstraintWidget e = new ConstraintWidget(50, 20);
        ConstraintWidget f = new ConstraintWidget(50, 20);
        int marginL = 7;
        int marginR = 19;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        e.setDebugSolverName(root.getSystem(), "E");
        f.setDebugSolverName(root.getSystem(), "F");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);
        root.add(f);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, marginR);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, marginL);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, marginR);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, marginL);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 0);
        d.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, 0);
        e.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT, 0);
        e.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT, 0);
        f.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 0);
        f.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        System.out.println("a) D: " + d + " E: " + e + " F: " + f);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getWidth(), 307, 1);
    }

    @Test
    public void testVerticalChainComplex() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(50, 20);
        ConstraintWidget e = new ConstraintWidget(50, 20);
        ConstraintWidget f = new ConstraintWidget(50, 20);
        int marginT = 7;
        int marginB = 19;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        e.setDebugSolverName(root.getSystem(), "E");
        f.setDebugSolverName(root.getSystem(), "F");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);
        root.add(f);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, marginB);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, marginT);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, marginB);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, marginT);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 0);
        d.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM, 0);
        e.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP, 0);
        e.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.BOTTOM, 0);
        f.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 0);
        f.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        System.out.println("a) D: " + d + " E: " + e + " F: " + f);
        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(b.getHeight(), c.getHeight(), 1);
        assertEquals(a.getHeight(), 174, 1);
    }


    @Test
    public void testHorizontalChainComplex2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 379, 591);
        ConstraintWidget a = new ConstraintWidget(100, 185);
        ConstraintWidget b = new ConstraintWidget(100, 185);
        ConstraintWidget c = new ConstraintWidget(100, 185);
        ConstraintWidget d = new ConstraintWidget(53, 17);
        ConstraintWidget e = new ConstraintWidget(42, 17);
        ConstraintWidget f = new ConstraintWidget(47, 17);
        int marginL = 0;
        int marginR = 0;
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        e.setDebugSolverName(root.getSystem(), "E");
        f.setDebugSolverName(root.getSystem(), "F");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);
        root.add(f);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 16);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 16);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, marginR);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, marginL);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, marginR);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, marginL);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 0);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 0);
        d.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, 0);
        d.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        e.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT, 0);
        e.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT, 0);
        e.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        f.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 0);
        f.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, 0);
        f.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        System.out.println("a) D: " + d + " E: " + e + " F: " + f);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getWidth(), 126);
    }

    @Test
    public void testVerticalChainBaseline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.add(a);
        root.add(b);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        int Ay = a.getTop();
        int By = b.getTop();
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - b.getBottom(), 1);
        assertEquals(b.getTop() - a.getBottom(), a.getTop() - root.getTop(), 1);
        root.add(c);
        a.setBaselineDistance(7);
        c.setBaselineDistance(7);
        c.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE, 0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(Ay, c.getTop(), 1);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a + " B: " + b + " C: " + c);
    }

    @Test
    public void testWrapHorizontalChain() {
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
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getHeight(), a.getHeight());
        assertEquals(root.getHeight(), b.getHeight());
        assertEquals(root.getHeight(), c.getHeight());
        assertEquals(root.getWidth(), a.getWidth() + b.getWidth() + c.getWidth());
    }

    @Test
    public void testWrapVerticalChain() {
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
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, 0);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b);
        assertEquals(root.getWidth(), a.getWidth());
        assertEquals(root.getWidth(), b.getWidth());
        assertEquals(root.getWidth(), c.getWidth());
        assertEquals(root.getHeight(), a.getHeight() + b.getHeight() + c.getHeight());
    }

    @Test
    public void testPackWithBaseline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 411, 603);
        ConstraintWidget a = new ConstraintWidget(118, 93, 88, 48);
        ConstraintWidget b = new ConstraintWidget(206, 93, 88, 48);
        ConstraintWidget c = new ConstraintWidget(69, 314, 88, 48);
        ConstraintWidget d = new ConstraintWidget(83, 458, 88, 48);
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");
        c.setDebugSolverName(root.getSystem(), "C");
        d.setDebugSolverName(root.getSystem(), "D");
        a.setBaselineDistance(29);
        b.setBaselineDistance(29);
        c.setBaselineDistance(29);
        d.setBaselineDistance(29);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        c.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        System.out.println("a) root: " + root + " C: " + c + " D: " + d);
        c.getAnchor(ConstraintAnchor.Type.TOP).reset();
        root.layout();
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        System.out.println("a) root: " + root + " C: " + c + " D: " + d);
        assertEquals(c.getBottom(), d.getTop());
    }

    @Test
    public void testBasicGoneChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, d, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getLeft(), 0);
        assertEquals(c.getLeft(), 250);
        assertEquals(d.getLeft(), 500);
        b.setVisibility(ConstraintWidget.VISIBLE);
        d.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c + " D: " + d);
    }

    @Test
    public void testGonePackChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        ConstraintWidget d = new ConstraintWidget(100, 20);
        guideline.setOrientation(Guideline.VERTICAL);
        guideline.setGuideBegin(200);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        guideline.setDebugName("guideline");
        d.setDebugName("D");
        root.add(a);
        root.add(b);
        root.add(guideline);
        root.add(d);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, guideline, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        a.setVisibility(ConstraintWidget.GONE);
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 0);
        assertEquals(guideline.getLeft(), 200);
        assertEquals(d.getLeft(), 350);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 0);
        assertEquals(guideline.getLeft(), 200);
        assertEquals(d.getLeft(), 350);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 0);
        assertEquals(guideline.getLeft(), 200);
        assertEquals(d.getLeft(), 350);
    }

    @Test
    public void testVerticalGonePackChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        ConstraintWidget d = new ConstraintWidget(100, 20);
        guideline.setOrientation(Guideline.HORIZONTAL);
        guideline.setGuideBegin(200);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        guideline.setDebugName("guideline");
        d.setDebugName("D");
        root.add(a);
        root.add(b);
        root.add(guideline);
        root.add(d);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, guideline, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        a.setVisibility(ConstraintWidget.GONE);
        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getHeight(), 0);
        assertEquals(b.getHeight(), 0);
        assertEquals(guideline.getTop(), 200);
        assertEquals(d.getTop(), 390);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getHeight(), 0);
        assertEquals(b.getHeight(), 0);
        assertEquals(guideline.getTop(), 200);
        assertEquals(d.getTop(), 390);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " guideline: " + guideline + " D: " + d);
        assertEquals(a.getHeight(), 0);
        assertEquals(b.getHeight(), 0);
        assertEquals(guideline.getTop(), 200);
        assertEquals(d.getTop(), 390);
    }

    @Test
    public void testVerticalDanglingChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 7);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 9);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), a.getHeight() + Math.max(7, 9));
    }

    @Test
    public void testHorizontalWeightChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Guideline guidelineLeft = new Guideline();
        Guideline guidelineRight = new Guideline();

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        guidelineLeft.setDebugName("guidelineLeft");
        guidelineRight.setDebugName("guidelineRight");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(guidelineLeft);
        root.add(guidelineRight);

        guidelineLeft.setOrientation(Guideline.VERTICAL);
        guidelineRight.setOrientation(Guideline.VERTICAL);
        guidelineLeft.setGuideBegin(20);
        guidelineRight.setGuideEnd(20);

        a.connect(ConstraintAnchor.Type.LEFT, guidelineLeft, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, guidelineRight, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalWeight(1);
        b.setHorizontalWeight(1);
        c.setHorizontalWeight(1);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 20);
        assertEquals(b.getLeft(), 207);
        assertEquals(c.getLeft(), 393);
        assertEquals(a.getWidth(), 187);
        assertEquals(b.getWidth(), 186);
        assertEquals(c.getWidth(), 187);
        c.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 20);
        assertEquals(b.getLeft(), 300);
        assertEquals(c.getLeft(), 580);
        assertEquals(a.getWidth(), 280);
        assertEquals(b.getWidth(), 280);
        assertEquals(c.getWidth(), 0);
    }

    @Test
    public void testVerticalGoneChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(root);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 16);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        a.getAnchor(ConstraintAnchor.Type.BOTTOM).setGoneMargin(16);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 16);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - b.getBottom(), 1);
        assertEquals(a.getBottom(), b.getTop());

        b.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
        assertEquals(root.getHeight(), 52);
    }

    @Test
    public void testVerticalGoneChain2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 16);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        b.getAnchor(ConstraintAnchor.Type.TOP).setGoneMargin(16);
        b.getAnchor(ConstraintAnchor.Type.BOTTOM).setGoneMargin(16);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 16);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - c.getBottom(), 1);
        assertEquals(a.getBottom(), b.getTop());

        a.setVisibility(ConstraintWidget.GONE);
        c.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getTop() - root.getTop(), root.getBottom() - b.getBottom());
        assertEquals(root.getHeight(), 52);
    }

    @Test
    public void testVerticalSpreadInsideChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 16);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 16);

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getHeight(), b.getHeight(), 1);
        assertEquals(b.getHeight(), c.getHeight(), 1);
        assertEquals(a.getHeight(), (root.getHeight() - 32) / 3, 1);
    }

    @Test
    public void testHorizontalSpreadMaxChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getWidth(), 200, 1);

        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 50, 1);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 50, 1);
        c.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 50, 1);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), b.getWidth(), 1);
        assertEquals(b.getWidth(), c.getWidth(), 1);
        assertEquals(a.getWidth(), 50, 1);
    }

    @Test
    public void testPackCenterChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 16);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setMinHeight(300);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getHeight(), 300);
        assertEquals(c.getTop(), (root.getHeight() - c.getHeight()) / 2);
        assertEquals(a.getTop(), (root.getHeight() - a.getHeight() - b.getHeight()) / 2);
    }

    @Test
    public void testPackCenterChainGone() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 16);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(600, root.getHeight());
        assertEquals(20, a.getHeight());
        assertEquals(20, b.getHeight());
        assertEquals(20, c.getHeight());
        assertEquals(270, a.getTop());
        assertEquals(290, b.getTop());
        assertEquals(310, c.getTop());

        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(600, root.getHeight());
        assertEquals(0, a.getHeight());
        assertEquals(20, b.getHeight());
        assertEquals(20, c.getHeight()); // todo not done
        assertEquals(a.getTop(), b.getTop());
        assertEquals((600 - 40) / 2, b.getTop());
        assertEquals(b.getTop() + b.getHeight(), c.getTop());
    }

    @Test
    public void testSpreadInsideChainWithMargins() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
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

        int marginOut = 0;

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginOut);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginOut);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), marginOut);
        assertEquals(c.getRight(), root.getWidth() - marginOut);
        assertEquals(b.getLeft(), a.getRight() + (c.getLeft() - a.getRight() - b.getWidth()) / 2);

        marginOut = 20;
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginOut);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginOut);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), marginOut);
        assertEquals(c.getRight(), root.getWidth() - marginOut);
        assertEquals(b.getLeft(), a.getRight() + (c.getLeft() - a.getRight() - b.getWidth()) / 2);
    }


}
