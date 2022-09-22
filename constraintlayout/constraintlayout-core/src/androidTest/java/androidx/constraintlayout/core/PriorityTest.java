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

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import org.junit.Test;

public class PriorityTest {

    @Test
    public void testPriorityChainHorizontal() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(400, 20);
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

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.LEFT);

        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT);

        b.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 400);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(a.getLeft(), 300);
        assertEquals(b.getLeft(), 400);
        assertEquals(c.getLeft(), 500);

        b.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 400);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(a.getLeft(), 300);
        assertEquals(b.getLeft(), 367);
        assertEquals(c.getLeft(), 533);

        b.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 400);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(a.getLeft(), 300);
        assertEquals(b.getLeft(), 300);
        assertEquals(c.getLeft(), 600);
    }

    @Test
    public void testPriorityChainVertical() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget a = new ConstraintWidget(400, 400);
        ConstraintWidget b = new ConstraintWidget(100, 100);
        ConstraintWidget c = new ConstraintWidget(100, 100);
        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, c, ConstraintAnchor.Type.TOP);

        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM);

        b.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), 400);
        assertEquals(b.getHeight(), 100);
        assertEquals(c.getHeight(), 100);
        assertEquals(a.getTop(), 300);
        assertEquals(b.getTop(), 400);
        assertEquals(c.getTop(), 500);

        b.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), 400);
        assertEquals(b.getHeight(), 100);
        assertEquals(c.getHeight(), 100);
        assertEquals(a.getTop(), 300);
        assertEquals(b.getTop(), 367);
        assertEquals(c.getTop(), 533);

        b.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getHeight(), 400);
        assertEquals(b.getHeight(), 100);
        assertEquals(c.getHeight(), 100);
        assertEquals(a.getTop(), 300);
        assertEquals(b.getTop(), 300);
        assertEquals(c.getTop(), 600);
    }
}

