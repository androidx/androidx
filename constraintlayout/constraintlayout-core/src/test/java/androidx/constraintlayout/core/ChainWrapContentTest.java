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
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;

public class ChainWrapContentTest {

    @Test
    public void testVerticalWrapContentChain() {
        testVerticalWrapContentChain(Optimizer.OPTIMIZATION_NONE);
        testVerticalWrapContentChain(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testVerticalWrapContentChain(int directResolution) {
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
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getTop(), 10);
        assertEquals(b.getTop(), 30);
        assertEquals(c.getTop(), 30);
        assertEquals(root.getHeight(), 82);
    }

    @Test
    public void testHorizontalWrapContentChain() {
        testHorizontalWrapContentChain(Optimizer.OPTIMIZATION_NONE);
        testHorizontalWrapContentChain(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testHorizontalWrapContentChain(int directResolution) {
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
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 32);
        root.layout();
        System.out.println("1/ res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("2/ res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 10);
        assertEquals(b.getLeft(), 110);
        assertEquals(c.getLeft(), 110);
        assertEquals(root.getWidth(), 242);
        root.setMinWidth(400);
        root.layout();
        System.out.println("3/ res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 10);
        assertEquals(b.getLeft(), 110);
        assertEquals(c.getLeft(), 268);
        assertEquals(root.getWidth(), 400);
    }

    @Test
    public void testVerticalWrapContentChain3Elts() {
        testVerticalWrapContentChain3Elts(Optimizer.OPTIMIZATION_NONE);
        testVerticalWrapContentChain3Elts(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testVerticalWrapContentChain3Elts(int directResolution) {
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
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, d, ConstraintAnchor.Type.TOP);
        d.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        d.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getTop(), 10);
        assertEquals(b.getTop(), 30);
        assertEquals(c.getTop(), 30);
        assertEquals(d.getTop(), 30);
        assertEquals(root.getHeight(), 82);
        root.setMinHeight(300);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getTop(), 10);
        assertEquals(b.getTop(), 30);
        assertEquals(c.getTop(), 139);
        assertEquals(d.getTop(), 248);
        assertEquals(root.getHeight(), 300);
        root.setHeight(600);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getTop(), 10);
        assertEquals(b.getTop(), 30);
        assertEquals(c.getTop(), 289);
        assertEquals(d.getTop(), 548);
        assertEquals(root.getHeight(), 600);
    }

    @Test
    public void testHorizontalWrapContentChain3Elts() {
        testHorizontalWrapContentChain3Elts(Optimizer.OPTIMIZATION_NONE);
        testHorizontalWrapContentChain3Elts(Optimizer.OPTIMIZATION_STANDARD);
    }

    public void testHorizontalWrapContentChain3Elts(int directResolution) {
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
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, d, ConstraintAnchor.Type.LEFT);
        d.connect(ConstraintAnchor.Type.LEFT, c, ConstraintAnchor.Type.RIGHT);
        d.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getLeft(), 10);
        assertEquals(b.getLeft(), 110);
        assertEquals(c.getLeft(), 110);
        assertEquals(d.getLeft(), 110);
        assertEquals(root.getWidth(), 242);
        root.setMinWidth(300);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getLeft(), 10);
        assertEquals(b.getLeft(), 110);
        assertEquals(c.getLeft(), 139);
        assertEquals(d.getLeft(), 168);
        assertEquals(root.getWidth(), 300);
        root.setWidth(600);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + a + " B: " + b + " C: " + c + " D: " + d);
        assertEquals(a.getLeft(), 10);
        assertEquals(b.getLeft(), 110);
        assertEquals(c.getLeft(), 289);
        assertEquals(d.getLeft(), 468);
        assertEquals(root.getWidth(), 600);
    }

    @Test
    public void testHorizontalWrapChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget a = new ConstraintWidget(20, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(20, 20);
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
        b.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);
        b.setWidth(600);
        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 20);
        assertEquals(c.getLeft(), 580);
        a.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        b.setWidth(600);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 20);
        assertEquals(c.getLeft(), 580); // doesn't expand beyond
        b.setWidth(100);
        root.layout();
        System.out.println("c) A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getLeft(), 230);
        assertEquals(b.getLeft(), 250);
        assertEquals(c.getLeft(), 350);
        b.setWidth(600);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        root.layout();
        System.out.println("d) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getHeight(), 20);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 20);
        assertEquals(c.getLeft(), 580);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        b.setWidth(600);
        root.setWidth(0);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("e) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getHeight(), 20);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 20);
        assertEquals(c.getLeft(), 620);
    }

    @Test
    public void testWrapChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1440, 1944);
        ConstraintWidget a = new ConstraintWidget(308, 168);
        ConstraintWidget b = new ConstraintWidget(308, 168);
        ConstraintWidget c = new ConstraintWidget(308, 168);
        ConstraintWidget d = new ConstraintWidget(308, 168);
        ConstraintWidget e = new ConstraintWidget(308, 168);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        d.setDebugName("D");
        e.setDebugName("E");
        root.add(e);
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
        e.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        e.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        e.connect(ConstraintAnchor.Type.TOP, c, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a
                + " B: " + b + " C: " + c + " D: " + d + " E: " + e);
        assertEquals(root.getWidth(), 1440);
        assertEquals(root.getHeight(), 336);
    }

    @Test
    public void testWrapDanglingChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1440, 1944);
        ConstraintWidget a = new ConstraintWidget(308, 168);
        ConstraintWidget b = new ConstraintWidget(308, 168);
        ConstraintWidget c = new ConstraintWidget(308, 168);
        ConstraintWidget d = new ConstraintWidget(308, 168);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 616);
        assertEquals(root.getHeight(), 168);
        assertEquals(a.getLeft(), 0);
        assertEquals(b.getLeft(), 308);
        assertEquals(a.getWidth(), 308);
        assertEquals(b.getWidth(), 308);
    }
}
