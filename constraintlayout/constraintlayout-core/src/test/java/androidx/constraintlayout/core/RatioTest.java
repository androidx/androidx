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
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;

public class RatioTest {

    @Test
    public void testWrapRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 700, 1920);
        ConstraintWidget a = new ConstraintWidget(231, 126);
        ConstraintWidget b = new ConstraintWidget(231, 126);
        ConstraintWidget c = new ConstraintWidget(231, 126);

        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);

        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        a.setHorizontalBiasPercent(0.3f);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 171);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);


        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);

        assertEquals(a.getLeft() >= 0, true);
        assertEquals(a.getWidth(), a.getHeight());
        assertEquals(a.getWidth(), 402);
        assertEquals(root.getWidth(), 402);
        assertEquals(root.getHeight(), 654);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getTop(), 402);
        assertEquals(b.getLeft(), 171);
        assertEquals(c.getTop(), 528);
        assertEquals(c.getLeft(), 171);
    }

    @Test
    public void testGuidelineRatioChainWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 700, 1920);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.HORIZONTAL);
        guideline.setGuideBegin(100);

        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(guideline);

        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, guideline, ConstraintAnchor.Type.TOP);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);


        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setDimensionRatio("1:1");
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setHeight(0);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);

        assertEquals(root.getHeight(), 1500);

        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 100);

        assertEquals(b.getWidth(), 700);
        assertEquals(b.getHeight(), 700);

        assertEquals(c.getWidth(), 700);
        assertEquals(c.getHeight(), 700);

        assertEquals(a.getTop(), 0);
        assertEquals(b.getTop(), a.getBottom());
        assertEquals(c.getTop(), b.getBottom());

        assertEquals(a.getLeft(), 300);
        assertEquals(b.getLeft(), 0);
        assertEquals(c.getLeft(), 0);

        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);

        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 300);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 100);
    }

    @Test
    public void testComplexRatioChainWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 700, 1920);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 40);
        ConstraintWidget x = new ConstraintWidget(100, 20);
        ConstraintWidget y = new ConstraintWidget(100, 20);
        ConstraintWidget z = new ConstraintWidget(100, 40);

        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(x);
        root.add(y);
        root.add(z);

        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        x.setDebugName("X");
        y.setDebugName("Y");
        z.setDebugName("Z");

        x.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        x.connect(ConstraintAnchor.Type.BOTTOM, y, ConstraintAnchor.Type.TOP);
        x.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        x.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        x.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        x.setHeight(40);

        y.connect(ConstraintAnchor.Type.TOP, x, ConstraintAnchor.Type.BOTTOM);
        y.connect(ConstraintAnchor.Type.BOTTOM, z, ConstraintAnchor.Type.TOP);
        y.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        y.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        y.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        y.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        y.setDimensionRatio("1:1");

        z.connect(ConstraintAnchor.Type.TOP, y, ConstraintAnchor.Type.BOTTOM);
        z.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        z.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        z.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        z.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        z.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        z.setDimensionRatio("1:1");

        root.setWidth(700);
        root.setHeight(0);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("X: " + x);
        System.out.println("Y: " + y);
        System.out.println("Z: " + z);

        assertEquals(root.getWidth(), 700);
        assertEquals(root.getHeight(), 1440);

        assertEquals(x.getLeft(), 0);
        assertEquals(x.getTop(), 0);
        assertEquals(x.getWidth(), 700);
        assertEquals(x.getHeight(), 40);

        assertEquals(y.getLeft(), 0);
        assertEquals(y.getTop(), 40);
        assertEquals(y.getWidth(), 700);
        assertEquals(y.getHeight(), 700);

        assertEquals(z.getLeft(), 0);
        assertEquals(z.getTop(), 740);
        assertEquals(z.getWidth(), 700);
        assertEquals(z.getHeight(), 700);

        a.connect(ConstraintAnchor.Type.TOP, x, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.LEFT, x, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");

        b.connect(ConstraintAnchor.Type.TOP, x, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");

        c.connect(ConstraintAnchor.Type.TOP, x, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, d, ConstraintAnchor.Type.LEFT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setDimensionRatio("1:1");

        d.connect(ConstraintAnchor.Type.TOP, x, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, x, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.BOTTOM, x, ConstraintAnchor.Type.BOTTOM);
        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setDimensionRatio("1:1");

        root.setHeight(0);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("root: " + root);
        System.out.println("X: " + x);
        System.out.println("Y: " + y);
        System.out.println("Z: " + z);

        assertEquals(root.getWidth(), 700);
        assertEquals(root.getHeight(), 1440);

        assertEquals(x.getLeft(), 0);
        assertEquals(x.getTop(), 0);
        assertEquals(x.getWidth(), 700);
        assertEquals(x.getHeight(), 40);

        assertEquals(y.getLeft(), 0);
        assertEquals(y.getTop(), 40);
        assertEquals(y.getWidth(), 700);
        assertEquals(y.getHeight(), 700);

        assertEquals(z.getLeft(), 0);
        assertEquals(z.getTop(), 740);
        assertEquals(z.getWidth(), 700);
        assertEquals(z.getHeight(), 700);

    }

    @Test
    public void testRatioChainWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 40);
        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        d.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.connect(ConstraintAnchor.Type.LEFT, d, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        a.setDimensionRatio("1:1");

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        b.setDimensionRatio("1:1");

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, d, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.BOTTOM);
        c.setDimensionRatio("1:1");

//        root.layout();
//        System.out.println("a) root: " + root + " D: " + D + " A: " + A
//          + " B: " + B + " C: " + C);
//
//        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " D: " + d + " A: " + a + " B: " + b + " C: " + c);

        assertEquals(root.getWidth(), 120);
        assertEquals(d.getWidth(), 120);
        assertEquals(a.getWidth(), 40);
        assertEquals(a.getHeight(), 40);
        assertEquals(b.getWidth(), 40);
        assertEquals(b.getHeight(), 40);
        assertEquals(c.getWidth(), 40);
        assertEquals(c.getHeight(), 40);
    }

    @Test
    public void testRatioChainWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1536);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        ConstraintWidget d = new ConstraintWidget(100, 40);
        ConstraintWidget e = new ConstraintWidget(100, 40);
        ConstraintWidget f = new ConstraintWidget(100, 40);
        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);
        root.add(d);
        root.add(e);
        root.add(f);
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        f.setDebugName("F");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);

        e.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        e.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        f.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        f.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        d.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.BOTTOM, e, ConstraintAnchor.Type.TOP);

        a.connect(ConstraintAnchor.Type.LEFT, d, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        a.setDimensionRatio("1:1");

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        b.setDimensionRatio("1:1");

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, d, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.BOTTOM);
        c.setDimensionRatio("1:1");

        e.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        e.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        e.connect(ConstraintAnchor.Type.TOP, d, ConstraintAnchor.Type.BOTTOM);
        e.connect(ConstraintAnchor.Type.BOTTOM, f, ConstraintAnchor.Type.TOP);
        e.setDimensionRatio("1:1");

        f.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        f.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        f.connect(ConstraintAnchor.Type.TOP, e, ConstraintAnchor.Type.BOTTOM);
        f.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        f.setDimensionRatio("1:1");

        root.layout();
        System.out.println("a) root: " + root + " D: " + d + " A: " + a + " B: " + b
                + " C: " + c + " D: " + d + " E: " + e + " F: " + f);

        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " D: " + d + " A: " + a + " B: " + b
                + " C: " + c + " D: " + d + " E: " + e + " F: " + f);

        //assertEquals(root.getWidth(), 748);
        assertEquals(d.getWidth(), root.getWidth());
        assertEquals(a.getWidth(), d.getHeight());
        assertEquals(a.getHeight(), d.getHeight());
        assertEquals(b.getWidth(), d.getHeight());
        assertEquals(b.getHeight(), d.getHeight());
        assertEquals(c.getWidth(), d.getHeight());
        assertEquals(c.getHeight(), d.getHeight());
    }

    @Test
    public void testRatioMax() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        root.setDebugName("root");
        root.add(a);
        a.setDebugName("A");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 150, 0);
        a.setDimensionRatio("W,16:9");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 267);
        assertEquals(a.getHeight(), 150);
        assertEquals(a.getTop(), 425);
    }

    @Test
    public void testRatioMax2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        root.setDebugName("root");
        root.add(a);
        a.setDebugName("A");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 150, 0);
        a.setDimensionRatio("16:9");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 267, 1);
        assertEquals(a.getHeight(), 150);
        assertEquals(a.getTop(), 425);
    }

    @Test
    public void testRatioSingleTarget() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 100);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(a);
        root.add(b);
        a.setDebugName("A");
        b.setDebugName("B");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("2:3");
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, 50);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(b.getHeight(), 150);
        assertEquals(b.getTop(), a.getBottom() - b.getHeight() / 2);
    }

    @Test
    public void testSimpleWrapRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(a);
        a.setDebugName("A");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);


        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 1000);
        assertEquals(root.getHeight(), 1000);
        assertEquals(a.getWidth(), 1000);
        assertEquals(a.getHeight(), 1000);
    }

    @Test
    public void testSimpleWrapRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(a);
        a.setDebugName("A");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);


        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 1000);
        assertEquals(root.getHeight(), 1000);
        assertEquals(a.getWidth(), 1000);
        assertEquals(a.getHeight(), 1000);
    }

    @Test
    public void testNestedRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        a.setDimensionRatio("1:1");
        b.setDimensionRatio("1:1");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 500);
        assertEquals(a.getWidth(), 500);
        assertEquals(b.getWidth(), 500);
        assertEquals(root.getHeight(), 1000);
        assertEquals(a.getHeight(), 500);
        assertEquals(b.getHeight(), 500);
    }

    @Test
    public void testNestedRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 700, 1200);
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
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        d.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalBiasPercent(0);

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalBiasPercent(0.5f);

        d.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        d.setVerticalBiasPercent(1);

        a.setDimensionRatio("1:1");
        b.setDimensionRatio("4:1");
        c.setDimensionRatio("4:1");
        d.setDimensionRatio("4:1");

        root.layout();

        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getWidth(), 700);
        assertEquals(a.getHeight(), 700);
        assertEquals(b.getWidth(), a.getWidth());
        assertEquals(b.getHeight(), b.getWidth() / 4);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getWidth(), a.getWidth());
        assertEquals(c.getHeight(), c.getWidth() / 4);
        assertEquals(c.getTop(), (root.getHeight() - c.getHeight()) / 2, 1);
        assertEquals(d.getWidth(), a.getWidth());
        assertEquals(d.getHeight(), d.getWidth() / 4);
        assertEquals(d.getTop(), a.getBottom() - d.getHeight());

        root.setWidth(300);
        root.layout();

        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getWidth(), root.getWidth());
        assertEquals(a.getHeight(), root.getWidth());
        assertEquals(b.getWidth(), a.getWidth());
        assertEquals(b.getHeight(), b.getWidth() / 4);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getWidth(), a.getWidth());
        assertEquals(c.getHeight(), c.getWidth() / 4);
        assertEquals(c.getTop(), (root.getHeight() - c.getHeight()) / 2, 1);
        assertEquals(d.getWidth(), a.getWidth());
        assertEquals(d.getHeight(), d.getWidth() / 4);
        assertEquals(d.getTop(), a.getBottom() - d.getHeight());

        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("c) root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertTrue("root width should be bigger than zero", root.getWidth() > 0);
        assertEquals(a.getWidth(), root.getWidth());
        assertEquals(a.getHeight(), root.getWidth());
        assertEquals(b.getWidth(), a.getWidth());
        assertEquals(b.getHeight(), b.getWidth() / 4);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getWidth(), a.getWidth());
        assertEquals(c.getHeight(), c.getWidth() / 4);
        assertEquals(c.getTop(), (root.getHeight() - c.getHeight()) / 2, 1);
        assertEquals(d.getWidth(), a.getWidth());
        assertEquals(d.getHeight(), d.getWidth() / 4);
        assertEquals(d.getTop(), a.getBottom() - d.getHeight());

        root.setWidth(700);
        root.setHeight(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("d) root: " + root + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertTrue("root width should be bigger than zero", root.getHeight() > 0);
        assertEquals(a.getWidth(), root.getWidth());
        assertEquals(a.getHeight(), root.getWidth());
        assertEquals(b.getWidth(), a.getWidth());
        assertEquals(b.getHeight(), b.getWidth() / 4);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getWidth(), a.getWidth());
        assertEquals(c.getHeight(), c.getWidth() / 4, 1);
        assertEquals(c.getTop(), (root.getHeight() - c.getHeight()) / 2, 1);
        assertEquals(d.getWidth(), a.getWidth());
        assertEquals(d.getHeight(), d.getWidth() / 4);
        assertEquals(d.getTop(), a.getBottom() - d.getHeight());
    }

    @Test
    public void testNestedRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1536);
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

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("3.5:1");

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setDimensionRatio("5:2");

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.setVerticalBiasPercent(0.9f);

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.BOTTOM);
        c.setVerticalBiasPercent(0.9f);

//        root.layout();
//        System.out.println("A: " + A);
//        System.out.println("B: " + B);
//        System.out.println("C: " + C);
//
//        assertEquals((float)A.getWidth() / A.getHeight(), 1f, .1f);
//        assertEquals((float)B.getWidth() / B.getHeight(), 3.5f, .1f);
//        assertEquals((float)C.getWidth() / C.getHeight(), 2.5f, .1f);
//        assertEquals(B.getTop() >= A.getTop(), true);
//        assertEquals(B.getTop() <= A.getBottom(), true);
//        assertEquals(C.getTop() >= B.getTop(), true);
//        assertEquals(C.getBottom() <= B.getBottom(), true);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("\nA: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);
        assertEquals((float) a.getWidth() / a.getHeight(), 1f, .1f);
        assertEquals((float) b.getWidth() / b.getHeight(), 3.5f, .1f);
        assertEquals((float) c.getWidth() / c.getHeight(), 2.5f, .1f);
    }

    @Test
    public void testNestedRatio4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(264, 144);
        ConstraintWidget b = new ConstraintWidget(264, 144);

        Guideline verticalGuideline = new Guideline();
        verticalGuideline.setGuidePercent(0.34f);
        verticalGuideline.setOrientation(Guideline.VERTICAL);

        Guideline horizontalGuideline = new Guideline();
        horizontalGuideline.setGuidePercent(0.66f);
        horizontalGuideline.setOrientation(Guideline.HORIZONTAL);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        horizontalGuideline.setDebugName("hGuideline");
        verticalGuideline.setDebugName("vGuideline");

        root.add(a);
        root.add(b);
        root.add(verticalGuideline);
        root.add(horizontalGuideline);

        a.setWidth(200);
        a.setHeight(200);
        a.connect(ConstraintAnchor.Type.BOTTOM, horizontalGuideline, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.LEFT, verticalGuideline, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, verticalGuideline, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, horizontalGuideline, ConstraintAnchor.Type.TOP);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        b.setDimensionRatio("H,1:1");
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.3f);
        b.connect(ConstraintAnchor.Type.BOTTOM, horizontalGuideline, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, verticalGuideline, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, verticalGuideline, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, horizontalGuideline, ConstraintAnchor.Type.TOP);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("\nroot: " + root);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("hG: " + horizontalGuideline);
        System.out.println("vG: " + verticalGuideline);

        assertEquals(verticalGuideline.getLeft(), 0.34f * root.getWidth(), 1);
        assertEquals(horizontalGuideline.getTop(), 0.66f * root.getHeight(), 1);
        assertTrue(a.getLeft() >= 0);
        assertTrue(b.getLeft() >= 0);
        assertEquals(a.getLeft(), verticalGuideline.getLeft() - a.getWidth() / 2);
        assertEquals(a.getTop(), horizontalGuideline.getTop() - a.getHeight() / 2);

        assertEquals(b.getLeft(), verticalGuideline.getLeft() - b.getWidth() / 2);
        assertEquals(b.getTop(), horizontalGuideline.getTop() - b.getHeight() / 2);

    }

    @Test
    public void testBasicCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 450);
        assertEquals(a.getTop(), 290);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 450);
        assertEquals(a.getTop(), 290);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
    }

    @Test
    public void testBasicCenter2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 150, 0);
        a.setDimensionRatio("W,16:9");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 0);
        assertEquals((float) a.getWidth() / a.getHeight(), 16f / 9f, .1f);
        assertEquals(a.getHeight(), 150);
        assertEquals((float) a.getTop(), (root.getHeight() - a.getHeight()) / 2f, 0f);
    }

    @Test
    public void testBasicRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setVerticalBiasPercent(0);
        a.setHorizontalBiasPercent(0);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);
        a.setVerticalBiasPercent(1);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 400);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);

        a.setVerticalBiasPercent(0);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 0);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);
    }

    @Test
    public void testBasicRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 450);
        assertEquals(a.getTop(), 250);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 100);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(a.getLeft(), 450);
        assertEquals(a.getTop(), 250);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 100);
    }

    @Test
    public void testSimpleRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 200, 600);
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
        a.setDimensionRatio("3:2");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals((float) a.getWidth() / a.getHeight(), 3.f / 2.f, .1f);
        assertTrue("A.top > 0", a.getTop() >= 0);
        assertTrue("A.left > 0", a.getLeft() >= 0);
        assertEquals("A vertically centered", a.getTop(), root.getHeight() - a.getBottom());
        assertEquals("A horizontally centered", a.getLeft(), root.getRight() - a.getRight());
        a.setDimensionRatio("1:2");
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals((float) a.getWidth() / a.getHeight(), 1.f / 2.f, .1f);
        assertTrue("A.top > 0", a.getTop() >= 0);
        assertTrue("A.left > 0", a.getLeft() >= 0);
        assertEquals("A vertically centered", a.getTop(), root.getHeight() - a.getBottom());
        assertEquals("A horizontally centered", a.getLeft(), root.getRight() - a.getRight());
    }

    @Test
    public void testRatioGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 400, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(ConstraintWidget.VERTICAL);
        guideline.setGuideBegin(200);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        root.add(guideline);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, guideline, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("3:2");
        root.layout();
        System.out.println("a) root: " + root + " guideline: " + guideline + " A: " + a);
        assertEquals(a.getWidth() / a.getHeight(), 3 / 2);
        assertTrue("A.top > 0", a.getTop() >= 0);
        assertTrue("A.left > 0", a.getLeft() >= 0);
        assertEquals("A vertically centered", a.getTop(), root.getHeight() - a.getBottom());
        assertEquals("A horizontally centered", a.getLeft(), guideline.getLeft() - a.getRight());
        a.setDimensionRatio("1:2");
        root.layout();
        System.out.println("b) root: " + root + " guideline: " + guideline + " A: " + a);
        assertEquals(a.getWidth() / a.getHeight(), 1 / 2);
        assertTrue("A.top > 0", a.getTop() >= 0);
        assertTrue("A.left > 0", a.getLeft() >= 0);
        assertEquals("A vertically centered", a.getTop(), root.getHeight() - a.getBottom());
        assertEquals("A horizontally centered", a.getLeft(), guideline.getLeft() - a.getRight());
    }

    @Test
    public void testRatioWithMinimum() {
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
        a.setDimensionRatio("16:9");
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 0);
        assertEquals(root.getHeight(), 0);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 56);
        a.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 178);
        assertEquals(root.getHeight(), 100);
    }

    @Test
    public void testRatioWithPercent() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
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
        a.setDimensionRatio("1:1");
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.7f);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        int w = (int) (0.7 * root.getWidth());
        assertEquals(a.getWidth(), w);
        assertEquals(a.getHeight(), w);
        assertEquals(a.getLeft(), (root.getWidth() - w) / 2);
        assertEquals(a.getTop(), (root.getHeight() - w) / 2);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(a.getWidth(), w);
        assertEquals(a.getHeight(), w);
        assertEquals(a.getLeft(), (root.getWidth() - w) / 2);
        assertEquals(a.getTop(), (root.getHeight() - w) / 2);
    }

    @Test
    public void testRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("16:9");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 1067);
        assertEquals(a.getHeight(), 600);
    }

    @Test
    public void testRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1920);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalBiasPercent(0.9f);
        a.setDimensionRatio("3.5:1");

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalBiasPercent(0.5f);
        b.setVerticalBiasPercent(0.9f);
        b.setDimensionRatio("4:2");

        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        // A: id: A (0, 414) - (600 x 172) B: (129, 414) - (342 x 172)
        assertEquals(a.getWidth() / (float) a.getHeight(), 3.5f, 0.1);
        assertEquals(b.getWidth() / (float) b.getHeight(), 2f, 0.1);
        assertEquals(a.getWidth(), 1080, 1);
        assertEquals(a.getHeight(), 309, 1);
        assertEquals(b.getWidth(), 618, 1);
        assertEquals(b.getHeight(), 309, 1);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 1450);
        assertEquals(b.getLeft(), 231);
        assertEquals(b.getTop(), a.getTop());
    }

    @Test
    public void testRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1920);
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
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalBiasPercent(0.5f);
        a.setDimensionRatio("1:1");

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalBiasPercent(0.5f);
        b.setVerticalBiasPercent(0.9f);
        b.setDimensionRatio("3.5:1");

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.BOTTOM);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalBiasPercent(0.5f);
        c.setVerticalBiasPercent(0.9f);
        c.setDimensionRatio("5:2");

        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        // A: id: A (0, 414) - (600 x 172) B: (129, 414) - (342 x 172)
        assertEquals(a.getWidth() / (float) a.getHeight(), 1.0f, 0.1);
        assertEquals(b.getWidth() / (float) b.getHeight(), 3.5f, 0.1);
        assertEquals(c.getWidth() / (float) c.getHeight(), 2.5f, 0.1);
        assertEquals(a.getWidth(), 1080, 1);
        assertEquals(a.getHeight(), 1080, 1);
        assertEquals(b.getWidth(), 1080, 1);
        assertEquals(b.getHeight(), 309, 1);
        assertEquals(c.getWidth(), 772, 1);
        assertEquals(c.getHeight(), 309, 1);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 420);
        assertEquals(b.getTop(), 1114);
        assertEquals(c.getLeft(), 154);
        assertEquals(c.getTop(), b.getTop());
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
        a.setDimensionRatio("1:1");
//        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
//        assertEquals(A.getWidth(), 1000);
//        assertEquals(A.getHeight(), 1000);
        a.setWidth(100);
        a.setHeight(20);
        a.setDimensionRatio("W,1:1");
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 1000);
        assertEquals(a.getHeight(), 1000);
    }

    @Test
    public void testDanglingRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(300, 200);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        b.setDebugName("B");
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 15);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(b.getLeft(), 335);
        assertEquals(b.getTop(), 100);
        assertEquals(b.getWidth(), 200);
        assertEquals(b.getHeight(), 200);
    }

    @Test
    public void testDanglingRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(300, 200);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        b.setDebugName("B");
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("h,1:1");
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, 15);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("w,1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 20);
        assertEquals(a.getTop(), 100);
        assertEquals(a.getWidth(), 300);
        assertEquals(a.getHeight(), 300);
        assertEquals(b.getLeft(), 335);
        assertEquals(b.getTop(), 100);
        assertEquals(b.getWidth(), 300);
        assertEquals(b.getHeight(), 300);
    }

    @Test
    public void testChainRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(300, 20);
        ConstraintWidget c = new ConstraintWidget(300, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 100);
        assertEquals(a.getWidth(), 400);
        assertEquals(a.getHeight(), 400);

        assertEquals(b.getLeft(), 400);
        assertEquals(b.getTop(), 0);
        assertEquals(b.getWidth(), 300);
        assertEquals(b.getHeight(), 20);

        assertEquals(c.getLeft(), 700);
        assertEquals(c.getTop(), 0);
        assertEquals(c.getWidth(), 300);
        assertEquals(c.getHeight(), 20);
    }

    @Test
    public void testChainRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
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
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 300);
        assertEquals(a.getWidth(), 400);
        assertEquals(a.getHeight(), 400);

        assertEquals(b.getLeft(), 400);
        assertEquals(b.getTop(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);

        assertEquals(c.getLeft(), 500);
        assertEquals(c.getTop(), 0);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 20);
    }


    @Test
    public void testChainRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
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
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 90);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);

        assertEquals(b.getLeft(), 0);
        assertEquals(b.getTop(), 780);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);

        assertEquals(c.getLeft(), 0);
        assertEquals(c.getTop(), 890);
        assertEquals(c.getWidth(), 100);
        assertEquals(c.getHeight(), 20);
    }

    @Test
    public void testChainRatio4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("4:3");
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 113, 1);
        assertEquals(a.getWidth(), 500);
        assertEquals(a.getHeight(), 375);

        assertEquals(b.getLeft(), 500);
        assertEquals(b.getTop(), 113, 1);
        assertEquals(b.getWidth(), 500);
        assertEquals(b.getHeight(), 375);
    }

    @Test
    public void testChainRatio5() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 700, 1200);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(b);
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio("1:1");
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 60, 0, 0);

        root.layout();

        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 300);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);

        assertEquals(b.getLeft(), 600);
        assertEquals(b.getTop(), 590);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);

        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        root.layout();

        System.out.println("b) root: " + root + " A: " + a + " B: " + b);
        assertEquals(a.getLeft(), 0);
        assertEquals(a.getTop(), 300);
        assertEquals(a.getWidth(), 600);
        assertEquals(a.getHeight(), 600);

        assertEquals(b.getLeft(), 600);
        assertEquals(b.getTop(), 590);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);

        root.setWidth(1080);
        root.setHeight(1536);
        a.setWidth(180);
        a.setHeight(180);
        b.setWidth(900);
        b.setHeight(106);
        a.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 180, 0, 0);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a + " B: " + b);
    }

    @Test
    public void testChainRatio6() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(264, 144);
        ConstraintWidget b = new ConstraintWidget(264, 144);
        ConstraintWidget c = new ConstraintWidget(264, 144);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);
        a.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalBiasPercent(0.501f);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio("1:1");
        a.setBaselineDistance(88);
        c.setBaselineDistance(88);
        root.setWidth(1080);
        root.setHeight(2220);
//        root.setHorizontalDimensionBehaviour(
//          ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
//        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
//        root.layout();
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root);
        System.out.println(" A: " + a);
        System.out.println(" B: " + b);
        System.out.println(" C: " + c);
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(b.getWidth(), b.getHeight());
        assertEquals(root.getWidth(), c.getWidth());
        assertEquals(root.getHeight(), a.getHeight() + b.getHeight() + c.getHeight());
    }

}
