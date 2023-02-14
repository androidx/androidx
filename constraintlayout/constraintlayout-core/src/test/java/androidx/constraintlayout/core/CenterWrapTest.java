/*
 * Copyright (C) 2017 The Android Open Source Project
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

public class CenterWrapTest {

    @Test
    public void testRatioCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        a.setDebugName("A");
        b.setDebugName("B");
        root.add(a);
        root.add(b);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDimensionRatio(0.3f, ConstraintWidget.VERTICAL);

        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setDimensionRatio(1f, ConstraintWidget.VERTICAL);
//        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + a);
    }

    @Test
    public void testSimpleWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testSimpleWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        a.setDebugName("A");
        root.add(a);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + a);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.add(a);
        root.add(b);
        root.add(c);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP);
        c.connect(ConstraintAnchor.Type.LEFT, b, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.BOTTOM);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(b.getHeight(), 20);
        assertEquals(c.getHeight(), 20);
    }

    @Test
    public void testWrapHeight() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget tl = new ConstraintWidget(100, 20);
        ConstraintWidget trl = new ConstraintWidget(100, 20);
        ConstraintWidget tbl = new ConstraintWidget(100, 20);
        ConstraintWidget img = new ConstraintWidget(100, 100);

        root.setDebugName("root");
        tl.setDebugName("TL");
        trl.setDebugName("TRL");
        tbl.setDebugName("TBL");
        img.setDebugName("IMG");

        // vertical

        tl.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        tl.connect(ConstraintAnchor.Type.BOTTOM, tbl, ConstraintAnchor.Type.BOTTOM);
        trl.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        //TRL.connect(ConstraintAnchor.Type.BOTTOM, TBL, ConstraintAnchor.Type.TOP);
        tbl.connect(ConstraintAnchor.Type.TOP, trl, ConstraintAnchor.Type.BOTTOM);

        img.connect(ConstraintAnchor.Type.TOP, tbl, ConstraintAnchor.Type.BOTTOM);
        img.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(tl);
        root.add(trl);
        root.add(tbl);
        root.add(img);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " TL: " + tl
                + " TRL: " + trl + " TBL: " + tbl + " IMG: " + img);
        assertEquals(root.getHeight(), 140);
    }

    @Test
    public void testComplexLayout() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget img = new ConstraintWidget(100, 100);

        int margin = 16;

        ConstraintWidget button = new ConstraintWidget(50, 50);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);

        img.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        img.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        img.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        img.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        button.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        button.connect(ConstraintAnchor.Type.TOP, img, ConstraintAnchor.Type.BOTTOM);
        button.connect(ConstraintAnchor.Type.BOTTOM, img, ConstraintAnchor.Type.BOTTOM);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        a.connect(ConstraintAnchor.Type.TOP, button, ConstraintAnchor.Type.BOTTOM, margin);

        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        b.connect(ConstraintAnchor.Type.TOP, button, ConstraintAnchor.Type.BOTTOM, margin);

        c.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        c.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, margin);
        c.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(img);
        root.add(button);
        root.add(a);
        root.add(b);
        root.add(c);

        root.setDebugName("root");
        img.setDebugName("IMG");
        button.setDebugName("BUTTON");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");

        root.layout();
        System.out.println("a) root: " + root + " IMG: " + img
                + " BUTTON: " + button + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(img.getWidth(), root.getWidth());
        assertEquals(button.getWidth(), 50);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(img.getHeight(), 100);
        assertEquals(button.getHeight(), 50);
        assertEquals(a.getHeight(), 20);
        assertEquals(b.getHeight(), 20);
        assertEquals(c.getHeight(), 20);
        assertEquals(img.getLeft(), 0);
        assertEquals(img.getRight(), root.getRight());
        assertEquals(button.getLeft(), 734);
        assertEquals(button.getTop(), img.getBottom() - button.getHeight() / 2);
        assertEquals(a.getLeft(), margin);
        assertEquals(a.getTop(), button.getBottom() + margin);
        assertEquals(b.getRight(), root.getRight() - margin);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getLeft(), 350);
        assertEquals(c.getRight(), 450);
        assertEquals(c.getTop(), 379, 1);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        root.setOptimizationLevel(0);
        System.out.println("b) root: " + root + " IMG: " + img
                + " BUTTON: " + button + " A: " + a + " B: " + b + " C: " + c);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 197);
        assertEquals(img.getWidth(), root.getWidth());
        assertEquals(button.getWidth(), 50);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 100);
        assertEquals(c.getWidth(), 100);
        assertEquals(img.getHeight(), 100);
        assertEquals(button.getHeight(), 50);
        assertEquals(a.getHeight(), 20);
        assertEquals(b.getHeight(), 20);
        assertEquals(c.getHeight(), 20);
        assertEquals(img.getLeft(), 0);
        assertEquals(img.getRight(), root.getRight());
        assertEquals(button.getLeft(), 734);
        assertEquals(button.getTop(), img.getBottom() - button.getHeight() / 2);
        assertEquals(a.getLeft(), margin);
        assertEquals(a.getTop(), button.getBottom() + margin);
        assertEquals(b.getRight(), root.getRight() - margin);
        assertEquals(b.getTop(), a.getTop());
        assertEquals(c.getLeft(), 350);
        assertEquals(c.getRight(), 450);
        assertEquals(c.getTop(), a.getBottom() + margin);
    }

    @Test
    public void testWrapCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget TextBox = new ConstraintWidget(100, 50);
        ConstraintWidget TextBoxGone = new ConstraintWidget(100, 50);
        ConstraintWidget ValueBox = new ConstraintWidget(20, 20);

        root.setDebugName("root");
        TextBox.setDebugName("TextBox");
        TextBoxGone.setDebugName("TextBoxGone");
        ValueBox.setDebugName("ValueBox");

        // vertical

        TextBox.setHorizontalDimensionBehaviour(
                ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        TextBox.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        TextBox.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        TextBox.connect(ConstraintAnchor.Type.RIGHT, ValueBox, ConstraintAnchor.Type.LEFT, 10);

        ValueBox.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 10);
        ValueBox.connect(ConstraintAnchor.Type.TOP, TextBox, ConstraintAnchor.Type.TOP);
        ValueBox.connect(ConstraintAnchor.Type.BOTTOM, TextBox, ConstraintAnchor.Type.BOTTOM);

        TextBoxGone.setHorizontalDimensionBehaviour(
                ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        TextBoxGone.connect(ConstraintAnchor.Type.TOP, TextBox, ConstraintAnchor.Type.BOTTOM, 10);
        TextBoxGone.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        TextBoxGone.connect(ConstraintAnchor.Type.RIGHT, TextBox, ConstraintAnchor.Type.RIGHT);
        TextBoxGone.setVisibility(ConstraintWidget.GONE);

        root.add(TextBox);
        root.add(ValueBox);
        root.add(TextBoxGone);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " TextBox: " + TextBox
                + " ValueBox: " + ValueBox + " TextBoxGone: " + TextBoxGone);
        assertEquals(ValueBox.getTop(),
                TextBox.getTop() + ((TextBox.getHeight() - ValueBox.getHeight()) / 2));
        assertEquals(root.getHeight(), 60);
    }
}
