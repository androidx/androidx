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

import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintAnchor.Type;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import org.junit.Test;

public class OptimizationsTest {
    @Test
    public void testGoneMatchConstraint() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        ConstraintWidget a = new ConstraintWidget("A", 0, 10);
        ConstraintWidget b = new ConstraintWidget("B", 10, 10);
        root.setDebugName("root");

        root.add(a);
        root.add(b);

        a.connect(Type.TOP, root, Type.TOP, 8);
        a.connect(Type.LEFT, root, Type.LEFT, 8);
        a.connect(Type.RIGHT, root, Type.RIGHT, 8);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 8);
        a.setVerticalBiasPercent(0.2f);
        a.setHorizontalBiasPercent(0.2f);
        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.connect(Type.TOP, a, Type.BOTTOM);

        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();

        System.out.println("1) A: " + a);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 163);
        assertEquals(a.getRight(), 592);
        assertEquals(a.getBottom(), 173);

        a.setVisibility(ConstraintWidget.GONE);
        root.layout();

        System.out.println("2) A: " + a);
        assertEquals(a.getLeft(), 120);
        assertEquals(a.getTop(), 160);
        assertEquals(a.getRight(), 120);
        assertEquals(a.getBottom(), 160);
    }

    @Test
    public void test3EltsChain() {
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

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 40);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 30);

        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
//        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("1) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 40);
        assertEquals(b.getLeft(), 255);
        assertEquals(c.getLeft(), 470);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("2) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 40);
        assertEquals(b.getLeft(), 217, 1);
        assertEquals(c.getLeft(), 393);
        assertEquals(a.getWidth(), 177, 1);
        assertEquals(b.getWidth(), 176, 1);
        assertEquals(c.getWidth(), 177, 1);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 7);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 3);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT, 7);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 3);

        root.layout();
        System.out.println("3) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        System.out.println(metrics);

        assertEquals(a.getLeft(), 40);
        assertEquals(b.getLeft(), 220);
        assertEquals(c.getLeft(), 400, 1);
        assertEquals(a.getWidth(), 170, 1);
        assertEquals(b.getWidth(), 170, 1);
        assertEquals(c.getWidth(), 170, 1);

        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);

        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("4) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 3);
        assertEquals(c.getLeft(), 292, 1);
        assertEquals(a.getWidth(), 0);
        assertEquals(b.getWidth(), 279, 1);
        assertEquals(c.getWidth(), 278, 1);
    }

    @Test
    public void testBasicChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("1) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 133);
        assertEquals(b.getLeft(), 367, 1);

        ConstraintWidget c = new ConstraintWidget(100, 20);
        c.setDebugName("C");
        root.add(c);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        root.layout();
        System.out.println("2) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 133);
        assertEquals(b.getLeft(), 367, 1);
        assertEquals(c.getLeft(), b.getRight());

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 40);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 100);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);

        root.layout();
        System.out.println("3) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 170);
        assertEquals(b.getLeft(), 370);

        a.setHorizontalBiasPercent(0);
        root.layout();
        System.out.println("4) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 40);
        assertEquals(b.getLeft(), 240);

        a.setHorizontalBiasPercent(0.5f);
        a.setVisibility(ConstraintWidget.GONE);
//        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("5) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 250);
        assertEquals(b.getLeft(), 250);
    }

    @Test
    public void testBasicChain2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        ConstraintWidget c = new ConstraintWidget(100, 20);
        c.setDebugName("C");
        root.add(c);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 40);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT, 100);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);

        a.setHorizontalBiasPercent(0.5f);
        a.setVisibility(ConstraintWidget.GONE);
//        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("5) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 250);
        assertEquals(b.getLeft(), 250);
    }

    @Test
    public void testBasicRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("1) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getHeight(), a.getWidth());
        assertEquals(b.getTop(), (a.getHeight() - b.getHeight()) / 2);
    }

    @Test
    public void testBasicBaseline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE);
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("1) root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getTop(), 290);
        assertEquals(b.getTop(), a.getTop());
    }

    @Test
    public void testBasicMatchConstraints() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("1) root: " + root + " A: " + a);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(a.getRight(), root.getWidth());
        assertEquals(a.getBottom(), root.getHeight());
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 20);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 30);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 40);
        root.layout();
        System.out.println("2) root: " + root + " A: " + a);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 30);
        assertEquals(a.getTop(), 10);
        assertEquals(a.getRight(), root.getWidth() - 40);
        assertEquals(a.getBottom(), root.getHeight() - 20);
    }

    @Test
    public void testBasicCenteringPositioning() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        root.add(a);
        long time = System.nanoTime();
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        time = System.nanoTime() - time;
        System.out.println("A) execution time: " + time);
        System.out.println("1) root: " + root + " A: " + a);
        System.out.println(metrics);
        assertEquals(a.getLeft(), (root.getWidth() - a.getWidth()) / 2);
        assertEquals(a.getTop(), (root.getHeight() - a.getHeight()) / 2);
        a.setHorizontalBiasPercent(0.3f);
        a.setVerticalBiasPercent(0.3f);
        root.layout();
        System.out.println("2) root: " + root + " A: " + a);
        System.out.println(metrics);
        assertEquals(a.getLeft(), (int) ((root.getWidth() - a.getWidth()) * 0.3f));
        assertEquals(a.getTop(), (int) ((root.getHeight() - a.getHeight()) * 0.3f));
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 30);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 20);
        root.layout();
        System.out.println("3) root: " + root + " A: " + a);
        System.out.println(metrics);
        assertEquals(a.getLeft(), (int) ((root.getWidth() - a.getWidth() - 40) * 0.3f) + 10);
        assertEquals(a.getTop(), (int) ((root.getHeight() - a.getHeight() - 70) * 0.3f) + 50);
    }

    @Test
    public void testBasicVerticalPositioning() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        int margin = 13;
        int marginR = 27;

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 31);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 27);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 27);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 104);
        root.add(a);
        root.add(b);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        long time = System.nanoTime();
//        root.layout();
//        time = System.nanoTime() - time;
//        System.out.println("A) execution time: " + time);
//        System.out.println("a - root: " + root + " A: " + A + " B: " + B);
//
//        assertEquals(A.getLeft(), 27);
//        assertEquals(A.getTop(), 31);
//        assertEquals(B.getLeft(), 27);
//        assertEquals(B.getTop(), 155);

        a.setVisibility(ConstraintWidget.GONE);
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        root.layout();
        System.out.println("b - root: " + root + " A: " + a + " B: " + b);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getLeft(), 27);
        assertEquals(b.getTop(), 104);
        // root: id: root (0, 0) - (600 x 600) wrap: (0 x 0) A: id: A (27, 31) - (100 x 20)
        // wrap: (0 x 0) B: id: B (27, 155) - (100 x 20) wrap: (0 x 0)

    }

    @Test
    public void testBasicVerticalGuidelinePositioning() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guidelineA = new Guideline();
        guidelineA.setOrientation(Guideline.HORIZONTAL);
        guidelineA.setGuideEnd(67);
        root.setDebugName("root");
        a.setDebugName("A");
        guidelineA.setDebugName("guideline");
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 31);
        a.connect(ConstraintAnchor.Type.BOTTOM, guidelineA, ConstraintAnchor.Type.TOP, 12);
        root.add(a);
        root.add(guidelineA);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        long time = System.nanoTime();
        root.layout();
        time = System.nanoTime() - time;
        System.out.println("A) execution time: " + time);
        System.out.println("root: " + root + " A: " + a + " guide: " + guidelineA);
        assertEquals(a.getTop(), 266);
        assertEquals(guidelineA.getTop(), 533);
    }

    @Test
    public void testSimpleCenterPositioning() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        int margin = 13;
        int marginR = 27;
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, margin);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, -margin);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, -marginR);
        root.add(a);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        long time = System.nanoTime();
        root.layout();
        time = System.nanoTime() - time;
        System.out.println("A) execution time: " + time);
        System.out.println("root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 270, 1);
        assertEquals(a.getTop(), 303, 1);
    }

    @Test
    public void testSimpleGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        Guideline guidelineA = new Guideline();
        ConstraintWidget a = new ConstraintWidget(100, 20);
        guidelineA.setOrientation(Guideline.VERTICAL);
        guidelineA.setGuideBegin(100);
        root.setDebugName("root");
        a.setDebugName("A");
        guidelineA.setDebugName("guidelineA");
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 32);
        a.connect(ConstraintAnchor.Type.LEFT, guidelineA, ConstraintAnchor.Type.LEFT, 2);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 7);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.add(guidelineA);
        root.add(a);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        Metrics metrics = new Metrics();
        root.fillMetrics(metrics);
        long time = System.nanoTime();
        root.layout();
        assertEquals(a.getLeft(), 102);
        assertEquals(a.getTop(), 32);
        assertEquals(a.getWidth(), 491);
        assertEquals(a.getHeight(), 20);
        assertEquals(guidelineA.getLeft(), 100);
        time = System.nanoTime() - time;
        System.out.println("A) execution time: " + time);
        System.out.println("root: " + root + " A: " + a + " guideline: " + guidelineA);
        System.out.println(metrics);
        root.setWidth(700);
        time = System.nanoTime();
        root.layout();
        time = System.nanoTime() - time;
        System.out.println("B) execution time: " + time);
        System.out.println("root: " + root + " A: " + a + " guideline: " + guidelineA);
        System.out.println(metrics);
        assertEquals(a.getLeft(), 102);
        assertEquals(a.getTop(), 32);
        assertEquals(a.getWidth(), 591);
        assertEquals(a.getHeight(), 20);
        assertEquals(guidelineA.getLeft(), 100);
    }

    @Test
    public void testSimple() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 10);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 20);
        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 30);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM, 20);
        root.add(a);
        root.add(b);
        root.add(c);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);

        long time = System.nanoTime();
        root.layout();
        time = System.nanoTime() - time;
        System.out.println("execution time: " + time);
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getLeft(), 10);
        assertEquals(a.getTop(), 20);
        assertEquals(b.getLeft(), 120);
        assertEquals(b.getTop(), 60);
        assertEquals(c.getLeft(), 140);
        assertEquals(c.getTop(), 100);
    }

    @Test
    public void testGuideline() {
        testVerticalGuideline(Optimizer.OPTIMIZATION_NONE);
        testVerticalGuideline(Optimizer.OPTIMIZATION_STANDARD);
        testHorizontalGuideline(Optimizer.OPTIMIZATION_NONE);
        testHorizontalGuideline(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testVerticalGuideline(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.VERTICAL);
        root.setDebugName("root");
        a.setDebugName("A");
        guideline.setDebugName("guideline");
        root.add(a);
        root.add(guideline);
        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT, 16);
        guideline.setGuideBegin(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), 100);
        assertEquals(a.getLeft(), 116);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getTop(), 0);
        guideline.setGuidePercent(0.5f);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), root.getWidth() / 2);
        assertEquals(a.getLeft(), 316);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getTop(), 0);
        guideline.setGuideEnd(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), 500);
        assertEquals(a.getLeft(), 516);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getTop(), 0);
    }

    public void testHorizontalGuideline(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.HORIZONTAL);
        root.setDebugName("root");
        a.setDebugName("A");
        guideline.setDebugName("guideline");
        root.add(a);
        root.add(guideline);
        a.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.TOP, 16);
        guideline.setGuideBegin(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getTop(), 100);
        assertEquals(a.getTop(), 116);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft(), 0);
        guideline.setGuidePercent(0.5f);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getTop(), root.getHeight() / 2);
        assertEquals(a.getTop(), 316);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft(), 0);
        guideline.setGuideEnd(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " guideline: " + guideline);
        assertEquals(guideline.getTop(), 500);
        assertEquals(a.getTop(), 516);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft(), 0);
    }

    @Test
    public void testBasicCentering() {
        testBasicCentering(Optimizer.OPTIMIZATION_NONE);
        testBasicCentering(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testBasicCentering(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 10);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 250);
        assertEquals(a.getTop(), 290);
    }

    @Test
    public void testPercent() {
        testPercent(Optimizer.OPTIMIZATION_NONE);
        testPercent(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testPercent(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.5f);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.5f);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 10);
        assertEquals(a.getTop(), 10);
        assertEquals(a.getWidth(), 300);
        assertEquals(a.getHeight(), 300);
    }

    @Test
    public void testDependency() {
        testDependency(Optimizer.OPTIMIZATION_NONE);
        testDependency(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testDependency(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
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
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.BASELINE, b, ConstraintAnchor.Type.BASELINE);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 16);
        b.connect(ConstraintAnchor.Type.BASELINE, c, ConstraintAnchor.Type.BASELINE);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 48);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 10);
        assertEquals(a.getTop(), 32);
        assertEquals(b.getLeft(), 126);
        assertEquals(b.getTop(), 32);
        assertEquals(c.getLeft(), 274);
        assertEquals(c.getTop(), 32);
    }

    @Test
    public void testDependency2() {
        testDependency2(Optimizer.OPTIMIZATION_NONE);
        testDependency2(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testDependency2(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 12);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 12);
        assertEquals(a.getTop(), 580);
        assertEquals(b.getLeft(), 12);
        assertEquals(b.getTop(), 560);
        assertEquals(c.getLeft(), 12);
        assertEquals(c.getTop(), 540);
    }

    @Test
    public void testDependency3() {
        testDependency3(Optimizer.OPTIMIZATION_NONE);
        testDependency3(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testDependency3(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
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
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 30);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 60);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 20);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 10);
        assertEquals(a.getTop(), 20);
        assertEquals(b.getLeft(), 260);
        assertEquals(b.getTop(), 520);
        assertEquals(c.getLeft(), 380);
        assertEquals(c.getTop(), 500);
    }

    @Test
    public void testDependency4() {
        testDependency4(Optimizer.OPTIMIZATION_NONE);
        testDependency4(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testDependency4(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 20);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, 30);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM, 60);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 250);
        assertEquals(a.getTop(), 290);
        assertEquals(b.getLeft(), 220);
        assertEquals(b.getTop(), 230);
    }

    @Test
    public void testDependency5() {
        testDependency5(Optimizer.OPTIMIZATION_NONE);
        testDependency5(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testDependency5(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
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
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        d.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 10);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT, 20);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.RIGHT, 20);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getLeft(), 250);
        assertEquals(a.getTop(), 197);
        assertEquals(b.getLeft(), 250);
        assertEquals(b.getTop(), 393);
        assertEquals(c.getLeft(), 230);
        assertEquals(c.getTop(), 413);
        assertEquals(d.getLeft(), 210);
        assertEquals(d.getTop(), 433);
    }

    @Test
    public void testUnconstrainedDependency() {
        testUnconstrainedDependency(Optimizer.OPTIMIZATION_NONE);
        testUnconstrainedDependency(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testUnconstrainedDependency(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
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
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        a.setFrame(142, 96, 242, 130);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 10);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 100);
        c.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 142);
        assertEquals(a.getTop(), 96);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 34);
        assertEquals(b.getLeft(), 252);
        assertEquals(b.getTop(), 196);
        assertEquals(c.getLeft(), 42);
        assertEquals(c.getTop(), 96);
    }

    @Test
    public void testFullLayout() {
        testFullLayout(Optimizer.OPTIMIZATION_NONE);
        testFullLayout(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testFullLayout(int directResolution) {
        // Horizontal :
        // r <- A
        // r <- B <- C <- D
        //      B <- E
        // r <- F
        // r <- G
        // Vertical:
        // r <- A <- B <- C <- D <- E
        // r <- F <- G
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 20);
        ConstraintWidget e = new ConstraintWidget(100, 20);
        ConstraintWidget f = new ConstraintWidget(100, 20);
        ConstraintWidget g = new ConstraintWidget(100, 20);
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        f.setDebugName("F");
        g.setDebugName("G");
        root.add(g);
        root.add(a);
        root.add(b);
        root.add(e);
        root.add(c);
        root.add(d);
        root.add(f);
        a.setBaselineDistance(8);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        d.setBaselineDistance(8);
        e.setBaselineDistance(8);
        f.setBaselineDistance(8);
        g.setBaselineDistance(8);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 40);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 16);
        c.connect(ConstraintAnchor.Type.BASELINE, b, ConstraintAnchor.Type.BASELINE);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.LEFT);
        e.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        e.connect(ConstraintAnchor.Type.BASELINE, d, ConstraintAnchor.Type.BASELINE);
        f.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        f.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        g.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        g.connect(ConstraintAnchor.Type.BASELINE, f, ConstraintAnchor.Type.BASELINE);
        root.layout();

        System.out.println(" direct: " + directResolution + " -> A: " + a + " B: " + b
                + " C: " + c + " D: " + d + " E: " + e + " F: " + f + " G: " + g);
        assertEquals(a.getLeft(), 250);
        assertEquals(a.getTop(), 20);
        assertEquals(b.getLeft(), 16);
        assertEquals(b.getTop(), 80);
        assertEquals(c.getLeft(), 132);
        assertEquals(c.getTop(), 80);
        assertEquals(d.getLeft(), 132);
        assertEquals(d.getTop(), 100);
        assertEquals(e.getLeft(), 16);
        assertEquals(e.getTop(), 100);
        assertEquals(f.getLeft(), 500);
        assertEquals(f.getTop(), 580);
        assertEquals(g.getLeft(), 16);
        assertEquals(g.getTop(), 580);
    }

    static BasicMeasure.Measurer sMeasurer = new BasicMeasure.Measurer() {

        @Override
        public void measure(ConstraintWidget widget, BasicMeasure.Measure measure) {
            ConstraintWidget.DimensionBehaviour horizontalBehavior = measure.horizontalBehavior;
            ConstraintWidget.DimensionBehaviour verticalBehavior = measure.verticalBehavior;
            int horizontalDimension = measure.horizontalDimension;
            int verticalDimension = measure.verticalDimension;
            System.out.println("*** MEASURE " + widget + " ***");

            if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredWidth = horizontalDimension;
            } else if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                measure.measuredWidth = horizontalDimension;
            }
            if (verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredHeight = verticalDimension;
                measure.measuredBaseline = 8;
            } else {
                measure.measuredHeight = verticalDimension;
                measure.measuredBaseline = 8;
            }
        }

        @Override
        public void didMeasures() {

        }
    };

    @Test
    public void testComplexLayout() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(30, 30);
        ConstraintWidget e = new ConstraintWidget(30, 30);
        ConstraintWidget f = new ConstraintWidget(30, 30);
        ConstraintWidget g = new ConstraintWidget(100, 20);
        ConstraintWidget h = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        f.setDebugName("F");
        g.setDebugName("G");
        h.setDebugName("H");
        root.add(g);
        root.add(a);
        root.add(b);
        root.add(e);
        root.add(c);
        root.add(d);
        root.add(f);
        root.add(h);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        d.setBaselineDistance(8);
        e.setBaselineDistance(8);
        f.setBaselineDistance(8);
        g.setBaselineDistance(8);
        h.setBaselineDistance(8);

        a.connect(Type.TOP, root, Type.TOP, 16);
        a.connect(Type.LEFT, root, Type.LEFT, 16);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 16);

        b.connect(Type.TOP, a, Type.TOP);
        b.connect(Type.LEFT, a, Type.RIGHT, 16);

        c.connect(Type.TOP, root, Type.TOP);
        c.connect(Type.LEFT, a, Type.RIGHT, 16);
        c.connect(Type.BOTTOM, root, Type.BOTTOM);

        d.connect(Type.BOTTOM, a, Type.BOTTOM);
        d.connect(Type.LEFT, a, Type.RIGHT, 16);

        e.connect(Type.BOTTOM, d, Type.BOTTOM);
        e.connect(Type.LEFT, d, Type.RIGHT, 16);

        f.connect(Type.BOTTOM, e, Type.BOTTOM);
        f.connect(Type.LEFT, e, Type.RIGHT, 16);

        g.connect(Type.TOP, root, Type.TOP);
        g.connect(Type.RIGHT, root, Type.RIGHT, 16);
        g.connect(Type.BOTTOM, root, Type.BOTTOM);

        h.connect(Type.BOTTOM, root, Type.BOTTOM, 16);
        h.connect(Type.RIGHT, root, Type.RIGHT, 16);

        root.setMeasurer(sMeasurer);
        root.layout();
        System.out.println(" direct: -> A: " + a + " B: " + b + " C: " + c
                + " D: " + d + " E: " + e + " F: " + f + " G: " + g + " H: " + h);

        assertEquals(a.getLeft(), 16);
        assertEquals(a.getTop(), 250);

        assertEquals(b.getLeft(), 132);
        assertEquals(b.getTop(), 250);

        assertEquals(c.getLeft(), 132);
        assertEquals(c.getTop(), 290);

        assertEquals(d.getLeft(), 132);
        assertEquals(d.getTop(), 320);

        assertEquals(e.getLeft(), 178);
        assertEquals(e.getTop(), 320);

        assertEquals(f.getLeft(), 224);
        assertEquals(f.getTop(), 320);

        assertEquals(g.getLeft(), 484);
        assertEquals(g.getTop(), 290);

        assertEquals(h.getLeft(), 484);
        assertEquals(h.getTop(), 564);
    }

    @Test
    public void testComplexLayoutWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_DIRECT);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(30, 30);
        ConstraintWidget e = new ConstraintWidget(30, 30);
        ConstraintWidget f = new ConstraintWidget(30, 30);
        ConstraintWidget g = new ConstraintWidget(100, 20);
        ConstraintWidget h = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        f.setDebugName("F");
        g.setDebugName("G");
        h.setDebugName("H");
        root.add(g);
        root.add(a);
        root.add(b);
        root.add(e);
        root.add(c);
        root.add(d);
        root.add(f);
        root.add(h);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        d.setBaselineDistance(8);
        e.setBaselineDistance(8);
        f.setBaselineDistance(8);
        g.setBaselineDistance(8);
        h.setBaselineDistance(8);

        a.connect(Type.TOP, root, Type.TOP, 16);
        a.connect(Type.LEFT, root, Type.LEFT, 16);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 16);

        b.connect(Type.TOP, a, Type.TOP);
        b.connect(Type.LEFT, a, Type.RIGHT, 16);

        c.connect(Type.TOP, root, Type.TOP);
        c.connect(Type.LEFT, a, Type.RIGHT, 16);
        c.connect(Type.BOTTOM, root, Type.BOTTOM);

        d.connect(Type.BOTTOM, a, Type.BOTTOM);
        d.connect(Type.LEFT, a, Type.RIGHT, 16);

        e.connect(Type.BOTTOM, d, Type.BOTTOM);
        e.connect(Type.LEFT, d, Type.RIGHT, 16);

        f.connect(Type.BOTTOM, e, Type.BOTTOM);
        f.connect(Type.LEFT, e, Type.RIGHT, 16);

        g.connect(Type.TOP, root, Type.TOP);
        g.connect(Type.RIGHT, root, Type.RIGHT, 16);
        g.connect(Type.BOTTOM, root, Type.BOTTOM);

        h.connect(Type.BOTTOM, root, Type.BOTTOM, 16);
        h.connect(Type.RIGHT, root, Type.RIGHT, 16);

        root.setMeasurer(sMeasurer);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " B: " + b + " C: " + c
                + " D: " + d + " E: " + e + " F: " + f + " G: " + g + " H: " + h);

        assertEquals(a.getLeft(), 16);
        assertEquals(a.getTop(), 16);

        assertEquals(b.getLeft(), 132);
        assertEquals(b.getTop(), 16);

        assertEquals(c.getLeft(), 132);
        assertEquals(c.getTop(), 56);

        assertEquals(d.getLeft(), 132);
        assertEquals(d.getTop(), 86);

        assertEquals(e.getLeft(), 178);
        assertEquals(e.getTop(), 86);

        assertEquals(f.getLeft(), 224);
        assertEquals(f.getTop(), 86);

        assertEquals(g.getLeft(), 484);
        assertEquals(g.getTop(), 56);

        assertEquals(h.getLeft(), 484);
        assertEquals(h.getTop(), 96);
    }

    @Test
    public void testChainLayoutWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);
        a.setBaselineDistance(28);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);

        a.connect(Type.TOP, root, Type.TOP, 16);
        a.connect(Type.LEFT, root, Type.LEFT, 16);
        a.connect(Type.RIGHT, b, Type.LEFT);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 16);

        b.connect(Type.BASELINE, a, Type.BASELINE);
        b.connect(Type.LEFT, a, Type.RIGHT);
        b.connect(Type.RIGHT, c, Type.LEFT);

        c.connect(Type.BASELINE, b, Type.BASELINE);
        c.connect(Type.LEFT, b, Type.RIGHT);
        c.connect(Type.RIGHT, root, Type.RIGHT, 16);

        root.setMeasurer(sMeasurer);
        //root.setWidth(332);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        //root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getLeft(), 16);
        assertEquals(a.getTop(), 250);

        assertEquals(b.getLeft(), 116);
        assertEquals(b.getTop(), 270);

        assertEquals(c.getLeft(), 216);
        assertEquals(c.getTop(), 270);
    }

    @Test
    public void testChainLayoutWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(100, 100);
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
        a.setBaselineDistance(28);
        b.setBaselineDistance(8);
        c.setBaselineDistance(8);
        d.setBaselineDistance(8);

        a.connect(Type.TOP, root, Type.TOP, 16);
        a.connect(Type.LEFT, root, Type.LEFT, 16);
        a.connect(Type.RIGHT, b, Type.LEFT);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 16);

        b.connect(Type.BASELINE, a, Type.BASELINE);
        b.connect(Type.LEFT, a, Type.RIGHT);
        b.connect(Type.RIGHT, c, Type.LEFT);

        c.connect(Type.BASELINE, b, Type.BASELINE);
        c.connect(Type.LEFT, b, Type.RIGHT);
        c.connect(Type.RIGHT, d, Type.LEFT, 16);

        d.connect(Type.RIGHT, root, Type.RIGHT);
        d.connect(Type.BOTTOM, root, Type.BOTTOM);

        root.setMeasurer(sMeasurer);
        //root.setWidth(332);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        //root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " B: " + b + " C: " + c + " D: " + d);

        assertEquals(a.getLeft(), 16);
        assertEquals(a.getTop(), 250);

        assertEquals(b.getLeft(), 116);
        assertEquals(b.getTop(), 270);

        assertEquals(c.getLeft(), 216);
        assertEquals(c.getTop(), 270);

        assertEquals(d.getLeft(), 332);
        assertEquals(d.getTop(), 580);
    }

    @Test
    public void testChainLayoutWrapGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.VERTICAL);
        guideline.setGuideEnd(100);
        root.setDebugName("root");
        a.setDebugName("A");
        guideline.setDebugName("guideline");
        root.add(a);
        root.add(guideline);
        a.setBaselineDistance(28);

        a.connect(Type.LEFT, guideline, Type.LEFT, 16);
        a.connect(Type.BOTTOM, root, Type.BOTTOM, 16);


        root.setMeasurer(sMeasurer);
        //root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " guideline: " + guideline);

        assertEquals(a.getLeft(), 516);
        assertEquals(a.getTop(), 0);

        assertEquals(guideline.getLeft(), 500);
    }


    @Test
    public void testChainLayoutWrapGuidelineChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(20, 20);
        ConstraintWidget b = new ConstraintWidget(20, 20);
        ConstraintWidget c = new ConstraintWidget(20, 20);
        ConstraintWidget d = new ConstraintWidget(20, 20);
        ConstraintWidget a2 = new ConstraintWidget(20, 20);
        ConstraintWidget b2 = new ConstraintWidget(20, 20);
        ConstraintWidget c2 = new ConstraintWidget(20, 20);
        ConstraintWidget d2 = new ConstraintWidget(20, 20);
        Guideline guidelineStart = new Guideline();
        Guideline guidelineEnd = new Guideline();
        guidelineStart.setOrientation(Guideline.VERTICAL);
        guidelineEnd.setOrientation(Guideline.VERTICAL);
        guidelineStart.setGuideBegin(30);
        guidelineEnd.setGuideEnd(30);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        a2.setDebugName("A2");
        b2.setDebugName("B2");
        c2.setDebugName("C2");
        d2.setDebugName("D2");
        guidelineStart.setDebugName("guidelineStart");
        guidelineEnd.setDebugName("guidelineEnd");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(a2);
        root.add(b2);
        root.add(c2);
        root.add(d2);
        root.add(guidelineStart);
        root.add(guidelineEnd);

        c.setVisibility(ConstraintWidget.GONE);
        chainConnect(Type.LEFT, guidelineStart, Type.RIGHT, guidelineEnd, a, b, c, d);
        chainConnect(Type.LEFT, root, Type.RIGHT, root, a2, b2, c2, d2);


        root.setMeasurer(sMeasurer);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        //root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " guideline: " + guidelineStart
                + " ebnd " + guidelineEnd + " B: " + b + " C: " + c + " D: " + d);
        System.out.println(" direct: -> A2: " + a2 + " B2: " + b2 + " C2: " + c2 + " D2: " + d2);

        assertEquals(a.getLeft(), 30);
        assertEquals(b.getLeft(), 50);
        assertEquals(c.getLeft(), 70);
        assertEquals(d.getLeft(), 70);
        assertEquals(guidelineStart.getLeft(), 30);
        assertEquals(guidelineEnd.getLeft(), 90);
        assertEquals(a2.getLeft(), 8);
        assertEquals(b2.getLeft(), 36);
        assertEquals(c2.getLeft(), 64);
        assertEquals(d2.getLeft(), 92);
    }

    private void chainConnect(Type start, ConstraintWidget startTarget, Type end,
            ConstraintWidget endTarget, ConstraintWidget... widgets) {
        widgets[0].connect(start, startTarget, start);
        ConstraintWidget previousWidget = null;
        for (int i = 0; i < widgets.length; i++) {
            if (previousWidget != null) {
                widgets[i].connect(start, previousWidget, end);
            }
            if (i < widgets.length - 1) {
                widgets[i].connect(end, widgets[i + 1], start);
            }
            previousWidget = widgets[i];
        }
        if (previousWidget != null) {
            previousWidget.connect(end, endTarget, end);
        }
    }

    @Test
    public void testChainLayoutWrapGuidelineChainVertical() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(20, 20);
        ConstraintWidget b = new ConstraintWidget(20, 20);
        ConstraintWidget c = new ConstraintWidget(20, 20);
        ConstraintWidget d = new ConstraintWidget(20, 20);
        ConstraintWidget a2 = new ConstraintWidget(20, 20);
        ConstraintWidget b2 = new ConstraintWidget(20, 20);
        ConstraintWidget c2 = new ConstraintWidget(20, 20);
        ConstraintWidget d2 = new ConstraintWidget(20, 20);
        Guideline guidelineStart = new Guideline();
        Guideline guidelineEnd = new Guideline();
        guidelineStart.setOrientation(Guideline.HORIZONTAL);
        guidelineEnd.setOrientation(Guideline.HORIZONTAL);
        guidelineStart.setGuideBegin(30);
        guidelineEnd.setGuideEnd(30);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        a2.setDebugName("A2");
        b2.setDebugName("B2");
        c2.setDebugName("C2");
        d2.setDebugName("D2");
        guidelineStart.setDebugName("guidelineStart");
        guidelineEnd.setDebugName("guidelineEnd");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(a2);
        root.add(b2);
        root.add(c2);
        root.add(d2);
        root.add(guidelineStart);
        root.add(guidelineEnd);

        c.setVisibility(ConstraintWidget.GONE);
        chainConnect(Type.TOP, guidelineStart, Type.BOTTOM, guidelineEnd, a, b, c, d);
        chainConnect(Type.TOP, root, Type.BOTTOM, root, a2, b2, c2, d2);


        root.setMeasurer(sMeasurer);
        //root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> A: " + a + " guideline: " + guidelineStart
                + " ebnd " + guidelineEnd + " B: " + b + " C: " + c + " D: " + d);
        System.out.println(" direct: -> A2: " + a2 + " B2: " + b2 + " C2: " + c2 + " D2: " + d2);

        assertEquals(a.getTop(), 30);
        assertEquals(b.getTop(), 50);
        assertEquals(c.getTop(), 70);
        assertEquals(d.getTop(), 70);
        assertEquals(guidelineStart.getTop(), 30);
        assertEquals(guidelineEnd.getTop(), 90);
        assertEquals(a2.getTop(), 8);
        assertEquals(b2.getTop(), 36);
        assertEquals(c2.getTop(), 64);
        assertEquals(d2.getTop(), 92);

        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 0);
        assertEquals(c.getLeft(), 0);
        assertEquals(d.getLeft(), 0);
        assertEquals(a2.getLeft(), 0);
        assertEquals(b2.getLeft(), 0);
        assertEquals(c2.getLeft(), 0);
        assertEquals(d2.getLeft(), 0);
    }

    @Test
    public void testChainLayoutWrapRatioChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget(20, 20);
        ConstraintWidget b = new ConstraintWidget(20, 20);
        ConstraintWidget c = new ConstraintWidget(20, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);

        chainConnect(Type.TOP, root, Type.BOTTOM, root, a, b, c);
        a.connect(Type.LEFT, root, Type.LEFT);
        b.connect(Type.LEFT, root, Type.LEFT);
        c.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.RIGHT, root, Type.RIGHT);
        b.connect(Type.RIGHT, root, Type.RIGHT);
        c.connect(Type.RIGHT, root, Type.RIGHT);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");

        root.setMeasurer(sMeasurer);
        //root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
//        root.layout();
//
//        System.out.println(" direct: -> A: " + A + " B: " + B +  " C: "  + C);
//
//        assertEquals(A.getTop(), 0);
//        assertEquals(B.getTop(), 20);
//        assertEquals(C.getTop(), 580);
//        assertEquals(A.getLeft(), 290);
//        assertEquals(B.getLeft(), 20);
//        assertEquals(C.getLeft(), 290);
//        assertEquals(B.getWidth(), 560);
//        assertEquals(B.getHeight(), B.getWidth());
//
//        //root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
//        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
//        root.layout();

        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
        root.setHeight(600);
        root.layout();

        System.out.println(" direct: -> A: " + a + " B: " + b + " C: " + c);

        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), 290);
        assertEquals(c.getTop(), 580);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 0);
        assertEquals(c.getLeft(), 0);
        assertEquals(b.getWidth(), 20);
        assertEquals(b.getHeight(), b.getWidth());
    }

    @Test
    public void testLayoutWrapBarrier() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        ConstraintWidget a = new ConstraintWidget("A", 20, 20);
        ConstraintWidget b = new ConstraintWidget("B", 20, 20);
        ConstraintWidget c = new ConstraintWidget("C", 20, 20);
        Barrier barrier = new Barrier("Barrier");
        barrier.setBarrierType(Barrier.BOTTOM);
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);

        a.connect(Type.TOP, root, Type.TOP);
        b.connect(Type.TOP, a, Type.BOTTOM);
        b.setVisibility(ConstraintWidget.GONE);
        c.connect(Type.TOP, barrier, Type.TOP);
        barrier.add(a);
        barrier.add(b);

        root.setMeasurer(sMeasurer);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> root: " + root + " A: " + a + " B: " + b
                + " C: " + c + " Barrier: " + barrier.getTop());

        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(b.getLeft(), 0);
        assertEquals(b.getTop(), 20);
        assertEquals(c.getLeft(), 0);
        assertEquals(c.getTop(), 20);
        assertEquals(barrier.getTop(), 20);
        assertEquals(root.getHeight(), 40);
    }

    @Test
    public void testLayoutWrapGuidelinesMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        //root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        ConstraintWidget a = new ConstraintWidget("A", 20, 20);

        Guideline left = new Guideline();
        left.setOrientation(Guideline.VERTICAL);
        left.setGuideBegin(30);
        left.setDebugName("L");

        Guideline right = new Guideline();
        right.setOrientation(Guideline.VERTICAL);
        right.setGuideEnd(30);
        right.setDebugName("R");

        Guideline top = new Guideline();
        top.setOrientation(Guideline.HORIZONTAL);
        top.setGuideBegin(30);
        top.setDebugName("T");

        Guideline bottom = new Guideline();
        bottom.setOrientation(Guideline.HORIZONTAL);
        bottom.setGuideEnd(30);
        bottom.setDebugName("B");

        root.add(a);
        root.add(left);
        root.add(right);
        root.add(top);
        root.add(bottom);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(Type.LEFT, left, Type.LEFT);
        a.connect(Type.RIGHT, right, Type.RIGHT);
        a.connect(Type.TOP, top, Type.TOP);
        a.connect(Type.BOTTOM, bottom, Type.BOTTOM);

        root.setMeasurer(sMeasurer);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> root: " + root + " A: " + a + " L: " + left + " R: " + right
                + " T: " + top + " B: " + bottom);

        assertEquals(root.getHeight(), 60);
        assertEquals(a.getLeft(), 30);
        assertEquals(a.getTop(), 30);
        assertEquals(a.getWidth(), 540);
        assertEquals(a.getHeight(), 0);

    }

    @Test
    public void testLayoutWrapMatch() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
//        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        ConstraintWidget a = new ConstraintWidget("A", 50, 20);
        ConstraintWidget b = new ConstraintWidget("B", 50, 30);
        ConstraintWidget c = new ConstraintWidget("C", 50, 20);

        root.add(a);
        root.add(b);
        root.add(c);

        a.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.TOP, root, Type.TOP);
        b.connect(Type.LEFT, a, Type.RIGHT);
        b.connect(Type.RIGHT, c, Type.LEFT);
        b.connect(Type.TOP, a, Type.BOTTOM);
        b.connect(Type.BOTTOM, c, Type.TOP);
        c.connect(Type.RIGHT, root, Type.RIGHT);
        c.connect(Type.BOTTOM, root, Type.BOTTOM);

        b.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        root.setMeasurer(sMeasurer);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(b.getTop(), 20);
        assertEquals(b.getBottom(), 50);
        assertEquals(b.getLeft(), 50);
        assertEquals(b.getRight(), 550);
        assertEquals(root.getHeight(), 70);
    }

    @Test
    public void testLayoutWrapBarrier2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        //root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        ConstraintWidget a = new ConstraintWidget("A", 50, 20);
        ConstraintWidget b = new ConstraintWidget("B", 50, 30);
        ConstraintWidget c = new ConstraintWidget("C", 50, 20);

        Guideline guideline = new Guideline();
        guideline.setDebugName("end");
        guideline.setGuideEnd(40);
        guideline.setOrientation(ConstraintWidget.VERTICAL);

        Barrier barrier = new Barrier();
        barrier.setBarrierType(Barrier.LEFT);
        barrier.setDebugName("barrier");
        barrier.add(b);
        barrier.add(c);

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);
        root.add(guideline);

        a.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.RIGHT, barrier, Type.LEFT);
        b.connect(Type.RIGHT, guideline, Type.RIGHT);
        c.connect(Type.RIGHT, root, Type.RIGHT);

        root.setMeasurer(sMeasurer);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 140);
    }

    @Test
    public void testLayoutWrapBarrier3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GROUPING);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        ConstraintWidget a = new ConstraintWidget("A", 50, 20);
        ConstraintWidget b = new ConstraintWidget("B", 50, 30);
        ConstraintWidget c = new ConstraintWidget("C", 50, 20);

        Guideline guideline = new Guideline();
        guideline.setDebugName("end");
        guideline.setGuideEnd(40);
        guideline.setOrientation(ConstraintWidget.VERTICAL);

        Barrier barrier = new Barrier();
        barrier.setBarrierType(Barrier.LEFT);
        barrier.setDebugName("barrier");
        barrier.add(b);
        barrier.add(c);

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(barrier);
        root.add(guideline);

        a.connect(Type.LEFT, root, Type.LEFT);
        a.connect(Type.RIGHT, barrier, Type.LEFT);
        b.connect(Type.RIGHT, guideline, Type.RIGHT);
        c.connect(Type.RIGHT, root, Type.RIGHT);

        root.setMeasurer(sMeasurer);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println(" direct: -> root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 140);
    }

    @Test
    public void testSimpleGuideline2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer("root", 600, 600);
        Guideline guidelineStart = new Guideline();
        guidelineStart.setDebugName("start");
        guidelineStart.setGuidePercent(0.1f);
        guidelineStart.setOrientation(ConstraintWidget.VERTICAL);

        Guideline guidelineEnd = new Guideline();
        guidelineEnd.setDebugName("end");
        guidelineEnd.setGuideEnd(40);
        guidelineEnd.setOrientation(ConstraintWidget.VERTICAL);

        ConstraintWidget a = new ConstraintWidget("A", 50, 20);
        root.add(a);
        root.add(guidelineStart);
        root.add(guidelineEnd);

        a.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(Type.LEFT, guidelineStart, Type.LEFT);
        a.connect(Type.RIGHT, guidelineEnd, Type.RIGHT);

        root.setMeasurer(sMeasurer);
        //root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println(" root: " + root);
        System.out.println("guideline start: " + guidelineStart);
        System.out.println("guideline end: " + guidelineEnd);
    }
}
