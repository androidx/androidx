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

import org.junit.Test;

/**
 * Basic visibility behavior test in the solver
 */
public class VisibilityTest {

    @Test
    public void testGoneSingleConnection() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");

        int margin = 175;
        int goneMargin = 42;
        root.add(a);
        root.add(b);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, margin);
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT, margin);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM, margin);

        root.layout();
        System.out.println("a) A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);
        assertEquals(a.getLeft(), root.getLeft() + margin);
        assertEquals(a.getTop(), root.getTop() + margin);
        assertEquals(b.getLeft(), a.getRight() + margin);
        assertEquals(b.getTop(), a.getBottom() + margin);

        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(a.getWidth(), 0);
        assertEquals(a.getHeight(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);
        assertEquals(a.getLeft(), root.getLeft());
        assertEquals(a.getTop(), root.getTop());
        assertEquals(b.getLeft(), a.getRight() + margin);
        assertEquals(b.getTop(), a.getBottom() + margin);

        b.setGoneMargin(ConstraintAnchor.Type.LEFT, goneMargin);
        b.setGoneMargin(ConstraintAnchor.Type.TOP, goneMargin);

        root.layout();
        System.out.println("c) A: " + a + " B: " + b);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(a.getWidth(), 0);
        assertEquals(a.getHeight(), 0);
        assertEquals(b.getWidth(), 100);
        assertEquals(b.getHeight(), 20);
        assertEquals(a.getLeft(), root.getLeft());
        assertEquals(a.getTop(), root.getTop());
        assertEquals(b.getLeft(), a.getRight() + goneMargin);
        assertEquals(b.getTop(), a.getBottom() + goneMargin);
    }

    @Test
    public void testGoneDualConnection() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setGuidePercent(0.5f);
        guideline.setOrientation(ConstraintWidget.HORIZONTAL);
        root.setDebugSolverName(root.getSystem(), "root");
        a.setDebugSolverName(root.getSystem(), "A");
        b.setDebugSolverName(root.getSystem(), "B");

        root.add(a);
        root.add(b);
        root.add(guideline);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, guideline, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.layout();
        System.out.println("a) A: " + a + " B: " + b + " guideline " + guideline);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(a.getLeft(), root.getLeft());
        assertEquals(a.getRight(), root.getRight());
        assertEquals(b.getLeft(), root.getLeft());
        assertEquals(b.getRight(), root.getRight());
        assertEquals(guideline.getTop(), root.getHeight() / 2);
        assertEquals(a.getTop(), root.getTop());
        assertEquals(a.getBottom(), guideline.getTop());
        assertEquals(b.getTop(), a.getBottom());
        assertEquals(b.getBottom(), root.getBottom());

        a.setVisibility(ConstraintWidget.GONE);
        root.layout();
        System.out.println("b) A: " + a + " B: " + b + " guideline " + guideline);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(a.getWidth(), 0);
        assertEquals(a.getHeight(), 0);
        assertEquals(a.getLeft(), 400);
        assertEquals(a.getRight(), 400);
        assertEquals(b.getLeft(), root.getLeft());
        assertEquals(b.getRight(), root.getRight());
        assertEquals(guideline.getTop(), root.getHeight() / 2);
        assertEquals(a.getTop(), 150);
        assertEquals(a.getBottom(), 150);
        assertEquals(b.getTop(), 150);
        assertEquals(b.getBottom(), root.getBottom());
    }
}
