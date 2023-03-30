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

import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.EXACTLY;
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.WRAP_CONTENT;

import static org.junit.Assert.assertEquals;

import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import org.junit.Test;

import java.util.ArrayList;

public class BasicTest {

    static BasicMeasure.Measurer sMeasurer = new BasicMeasure.Measurer() {

        @Override
        public void measure(ConstraintWidget widget, BasicMeasure.Measure measure) {
            ConstraintWidget.DimensionBehaviour horizontalBehavior = measure.horizontalBehavior;
            ConstraintWidget.DimensionBehaviour verticalBehavior = measure.verticalBehavior;
            int horizontalDimension = measure.horizontalDimension;
            int verticalDimension = measure.verticalDimension;

            if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredWidth = horizontalDimension;
            } else if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                measure.measuredWidth = horizontalDimension;
            }
            if (verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredHeight = verticalDimension;
            }
            widget.setMeasureRequested(false);
        }

        @Override
        public void didMeasures() {

        }
    };

    @Test
    public void testWrapPercent() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        root.setDebugName("root");
        a.setDebugName("A");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, WRAP_CONTENT, 0, 0.5f);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        root.add(a);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("root: " + root);
        System.out.println("A: " + a);
        assertEquals(a.getWidth(), 100);
        assertEquals(root.getWidth(), a.getWidth() * 2);
    }

    @Test
    public void testMiddleSplit() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(400, 30);
        ConstraintWidget b = new ConstraintWidget(400, 60);
        Guideline guideline = new Guideline();
        ConstraintWidget divider = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        guideline.setDebugName("guideline");
        divider.setDebugName("divider");

        root.add(a);
        root.add(b);
        root.add(guideline);
        root.add(divider);

        guideline.setOrientation(Guideline.VERTICAL);
        guideline.setGuidePercent(0.5f);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, guideline, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        divider.setWidth(1);
        divider.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        divider.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        divider.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
//        root.layout();
        root.updateHierarchy();
        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("guideline: " + guideline);
        System.out.println("divider: " + divider);

        assertEquals(a.getWidth(), 300);
        assertEquals(b.getWidth(), 300);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 300);
        assertEquals(divider.getHeight(), 60);
        assertEquals(root.getWidth(), 600);
        assertEquals(root.getHeight(), 60);
    }

    @Test
    public void testSimpleConstraint() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");

        root.add(a);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GRAPH);
        root.measure(Optimizer.OPTIMIZATION_GRAPH, 0, 0, 0, 0, 0, 0, 0, 0);
//        root.layout();

        System.out.println("1) A: " + a);
    }

    @Test
    public void testSimpleWrapConstraint9() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");

        root.add(a);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        int margin = 8;
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, margin);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, margin);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);


        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GRAPH_WRAP);
        root.measure(Optimizer.OPTIMIZATION_GRAPH_WRAP, 0, 0, 0, 0, 0, 0, 0, 0);
//        root.layout();

        System.out.println("root: " + root);
        System.out.println("1) A: " + a);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.measure(Optimizer.OPTIMIZATION_GRAPH_WRAP, 0, 0, 0, 0, 0, 0, 0, 0);

        System.out.println("root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 116);
        assertEquals(root.getHeight(), 46);
    }

    @Test
    public void testSimpleWrapConstraint10() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");

        root.add(a);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        int margin = 8;
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, margin);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, margin);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);


        //root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.measure(Optimizer.OPTIMIZATION_NONE, 0, 0, 0, 0, 0, 0, 0, 0);
        root.layout();

        System.out.println("root: " + root);
        System.out.println("1) A: " + a);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, EXACTLY, 800, 0, 0, 0, 0);

        System.out.println("root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 116);
        assertEquals(root.getHeight(), 800);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 385);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
    }

    @Test
    public void testSimpleWrapConstraint11() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(10, 30);
        ConstraintWidget b = new ConstraintWidget(800, 30);
        ConstraintWidget c = new ConstraintWidget(10, 30);
        ConstraintWidget d = new ConstraintWidget(800, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);


        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);

        root.layout();

        System.out.println("root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("1) B: " + b);
        System.out.println("1) C: " + c);
        System.out.println("1) D: " + d);

        assertEquals(a.getLeft(), 0);
        assertEquals(a.getWidth(), 10);
        assertEquals(c.getWidth(), 10);
        assertEquals(b.getLeft(), a.getRight());
        assertEquals(b.getWidth(), root.getWidth() - a.getWidth() - c.getWidth());
        assertEquals(c.getLeft(), root.getWidth() - c.getWidth());
        assertEquals(d.getWidth(), 800);
        assertEquals(d.getLeft(), -99);
    }

    @Test
    public void testSimpleWrapConstraint() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 60);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");

        root.add(a);
        root.add(b);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(root.getWidth(), 216);
        assertEquals(root.getHeight(), 68);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 116);
        assertEquals(b.getTop(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 60);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(root.getWidth(), 216);
        assertEquals(root.getHeight(), 68);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 116);
        assertEquals(b.getTop(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 60);
    }


    @Test
    public void testSimpleWrapConstraint2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(120, 60);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");

        root.add(a);
        root.add(b);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 8);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
//        root.layout();

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(root.getWidth(), 128);
        assertEquals(root.getHeight(), 114);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 8);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 120);
        assertEquals(b.getHeight(), 60);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(root.getWidth(), 128);
        assertEquals(root.getHeight(), 114);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 8);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 120);
        assertEquals(b.getHeight(), 60);
    }

    @Test
    public void testSimpleWrapConstraint3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");

        root.add(a);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 116);
        assertEquals(root.getHeight(), 46);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 116);
        assertEquals(root.getHeight(), 46);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);

    }

    @Test
    public void testSimpleWrapConstraint4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 8);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 8);

        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 8);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 8);

        d.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 8);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);


        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 532);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 216);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 324);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 432);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 532);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 216);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 324);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 432);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);
    }

    @Test
    public void testSimpleWrapConstraint5() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 8);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 8);
        b.setHorizontalBiasPercent(0.2f);

        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 8);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 8);

        d.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 8);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);


        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 376);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 60);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 168);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 276);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 376);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 60);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 168);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 276);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);
    }

    @Test
    public void testSimpleWrapConstraint6() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, 8);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 33);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 16);
        b.setHorizontalBiasPercent(0.15f);

        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP, 8);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT, 12);

        d.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP, 8);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);


        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 389);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 69);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 181);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 289);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 389);
        assertEquals(root.getHeight(), 76);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 30);
        assertEquals(b.getLeft(), 69);
        assertEquals(b.getTop(), 46);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 30);
        assertEquals(c.getLeft(), 181);
        assertEquals(c.getTop(), 8);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 30);
        assertEquals(d.getLeft(), 289);
        assertEquals(d.getTop(), -28, 2);
        assertEquals(d.getWidth(), 100);
        assertEquals(d.getHeight(), 30);
    }

    @Test
    public void testSimpleWrapConstraint7() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");

        root.add(a);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 8);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 16);
        assertEquals(root.getHeight(), 38);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 0);
        assertEquals(a.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        assertEquals(root.getWidth(), 16);
        assertEquals(root.getHeight(), 38);
        assertEquals(a.getLeft(), 8);
        assertEquals(a.getTop(), 8);
        assertEquals(a.getWidth(), 0);
        assertEquals(a.getHeight(), 30);

    }


    @Test
    public void testSimpleWrapConstraint8() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(10, 30);
        ConstraintWidget c = new ConstraintWidget(10, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        applyChain(ConstraintWidget.HORIZONTAL, a, b, c, d);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.measure(Optimizer.OPTIMIZATION_STANDARD,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 110);
        assertEquals(root.getHeight(), 30);

        root.measure(Optimizer.OPTIMIZATION_GRAPH,
                BasicMeasure.WRAP_CONTENT, 0, BasicMeasure.WRAP_CONTENT, 0, 0, 0, 0, 0);
        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
        assertEquals(root.getWidth(), 110);
        assertEquals(root.getHeight(), 30);

    }


    @Test
    public void testSimpleCircleConstraint() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");

        root.add(a);
        root.add(b);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 8);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 8);
        b.connectCircularConstraint(a, 30, 50);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GRAPH);
        root.measure(Optimizer.OPTIMIZATION_GRAPH, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);
//        root.layout();

        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
    }

    public void applyChain(ArrayList<ConstraintWidget> widgets, int direction) {
        ConstraintWidget previous = widgets.get(0);
        for (int i = 1; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            if (direction == 0) { // horizontal
                widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT);
                previous.connect(ConstraintAnchor.Type.RIGHT, widget, ConstraintAnchor.Type.LEFT);
            } else {
                widget.connect(ConstraintAnchor.Type.TOP, previous, ConstraintAnchor.Type.BOTTOM);
                previous.connect(ConstraintAnchor.Type.BOTTOM, widget, ConstraintAnchor.Type.TOP);
            }
            previous = widget;
        }
    }

    public void applyChain(int direction, ConstraintWidget... widgets) {
        ConstraintWidget previous = widgets[0];
        for (int i = 1; i < widgets.length; i++) {
            ConstraintWidget widget = widgets[i];
            if (direction == ConstraintWidget.HORIZONTAL) {
                widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT);
                previous.connect(ConstraintAnchor.Type.RIGHT, widget, ConstraintAnchor.Type.LEFT);
            } else {
                widget.connect(ConstraintAnchor.Type.TOP, previous, ConstraintAnchor.Type.BOTTOM);
                previous.connect(ConstraintAnchor.Type.BOTTOM, widget, ConstraintAnchor.Type.TOP);
            }
            previous = widget;
        }
    }

    @Test
    public void testRatioChainConstraint() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(0, 30);
        ConstraintWidget c = new ConstraintWidget(0, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");

        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        b.setDimensionRatio("w,1:1");

        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        applyChain(ConstraintWidget.HORIZONTAL, a, b, c, d);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_GRAPH);
        root.measure(Optimizer.OPTIMIZATION_GRAPH, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);
//        root.layout();

        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);
    }


    @Test
    public void testCycleConstraints() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(40, 20);
        ConstraintWidget c = new ConstraintWidget(40, 20);
        ConstraintWidget d = new ConstraintWidget(30, 30);

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
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.LEFT);

        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.LEFT, d, ConstraintAnchor.Type.RIGHT);

        d.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);

        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);

        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), 30);
        assertEquals(c.getTop(), 50);
        assertEquals(d.getTop(), 35);
    }

    @Test
    public void testGoneChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
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
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVisibility(ConstraintWidget.GONE);
        c.setVisibility(ConstraintWidget.GONE);

        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);

        assertEquals(b.getWidth(), root.getWidth());
    }

    @Test
    public void testGoneChainWithCenterWidget() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);
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
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVisibility(ConstraintWidget.GONE);
        c.setVisibility(ConstraintWidget.GONE);
        d.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        d.setVisibility(ConstraintWidget.GONE);

        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        System.out.println("3) C: " + c);
        System.out.println("4) D: " + d);

        assertEquals(b.getWidth(), root.getWidth());
    }

    @Test
    public void testBarrier() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 800);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(100, 30);
        ConstraintWidget b = new ConstraintWidget(100, 30);
        ConstraintWidget c = new ConstraintWidget(100, 30);
        ConstraintWidget d = new ConstraintWidget(100, 30);
        Barrier barrier1 = new Barrier();
        //Barrier barrier2 = new Barrier();
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        barrier1.setDebugName("barrier1");
        //barrier2.setDebugName("barrier2");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(barrier1);
        //root.add(barrier2);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        barrier1.add(a);
        barrier1.setBarrierType(Barrier.BOTTOM);

        b.connect(ConstraintAnchor.Type.TOP, barrier1, ConstraintAnchor.Type.BOTTOM);
        //barrier2.add(B);
        //barrier2.setBarrierType(Barrier.TOP);

        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 600, EXACTLY, 800, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) barrier1: " + barrier1);
        System.out.println("3) B: " + b);
        //System.out.println("4) barrier2: " + barrier2);
        System.out.println("5) C: " + c);
        System.out.println("6) D: " + d);

        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), a.getBottom());
        assertEquals(barrier1.getTop(), a.getBottom());
        assertEquals(c.getTop(), b.getBottom());
        assertEquals(d.getTop(), 430);
//        assertEquals(barrier2.getTop(), B.getTop());

    }


    @Test
    public void testDirectCentering() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 192, 168);
        root.setMeasurer(sMeasurer);
        ConstraintWidget a = new ConstraintWidget(43, 43);
        ConstraintWidget b = new ConstraintWidget(59, 59);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(b);
        root.add(a);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.measure(Optimizer.OPTIMIZATION_NONE, EXACTLY, 100, EXACTLY, 100, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(a.getTop(), 63);
        assertEquals(a.getLeft(), 75);
        assertEquals(b.getTop(), 55);
        assertEquals(b.getLeft(), 67);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.measure(Optimizer.OPTIMIZATION_STANDARD, EXACTLY, 100, EXACTLY, 100, 0, 0, 0, 0);

        System.out.println("0) root: " + root);
        System.out.println("1) A: " + a);
        System.out.println("2) B: " + b);
        assertEquals(63, a.getTop());
        assertEquals(75, a.getLeft());
        assertEquals(55, b.getTop());
        assertEquals(67, b.getLeft());
    }

}
