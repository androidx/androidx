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

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class WidgetsPositioningTest {

    LinearSystem mLS = new LinearSystem();
    boolean mOptimize = false;

    @Before
    public void setUp() {
        mLS = new LinearSystem();
        LinearEquation.resetNaming();
    }

    @Test
    public void testCentering() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        final ConstraintWidget b = new ConstraintWidget(20, 100);
        final ConstraintWidget c = new ConstraintWidget(100, 20);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 200);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, 0);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM, 0);
        c.connect(ConstraintAnchor.Type.TOP, b, ConstraintAnchor.Type.TOP, 0);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.BOTTOM, 0);
        root.add(a);
        root.add(b);
        root.add(c);
        root.layout();
        System.out.println("A: " + a + " B: " + b + " C: " + c);
    }

    @Test
    public void testDimensionRatio() {
        final ConstraintWidget a = new ConstraintWidget(0, 0, 600, 600);
        final ConstraintWidget b = new ConstraintWidget(100, 100);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        final int margin = 10;
        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.LEFT, margin);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.RIGHT, margin);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.TOP, margin);
        b.connect(ConstraintAnchor.Type.BOTTOM, a, ConstraintAnchor.Type.BOTTOM, margin);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        a.setDebugName("A");
        b.setDebugName("B");
        final float ratio = 0.3f;
        // First, let's check vertical ratio
        b.setDimensionRatio(ratio, ConstraintWidget.VERTICAL);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("a) A: " + a + " B: " + b);
                assertEquals(b.getWidth(), a.getWidth() - 2 * margin);
                assertEquals(b.getHeight(), (int) (ratio * b.getWidth()));
                assertEquals(b.getTop() - a.getTop(), (int) ((a.getHeight() - b.getHeight()) / 2));
                assertEquals(a.getBottom() - b.getBottom(),
                        (int) ((a.getHeight() - b.getHeight()) / 2));
                assertEquals(b.getTop() - a.getTop(), a.getBottom() - b.getBottom());
            }
        });
        b.setVerticalBiasPercent(1);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("b) A: " + a + " B: " + b);
                assertEquals(b.getWidth(), a.getWidth() - 2 * margin);
                assertEquals(b.getHeight(), (int) (ratio * b.getWidth()));
                assertEquals(b.getTop(), a.getHeight() - b.getHeight() - margin);
                assertEquals(a.getBottom(), b.getBottom() + margin);
            }
        });
        b.setVerticalBiasPercent(0);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("c) A: " + a + " B: " + b);
                assertEquals(b.getWidth(), a.getWidth() - 2 * margin);
                assertEquals(b.getHeight(), (int) (ratio * b.getWidth()));
                assertEquals(b.getTop(), a.getTop() + margin);
                assertEquals(b.getBottom(), a.getTop() + b.getHeight() + margin);
            }
        });
        // Then, let's check horizontal ratio
        b.setDimensionRatio(ratio, ConstraintWidget.HORIZONTAL);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("d) A: " + a + " B: " + b);
                assertEquals(b.getHeight(), a.getHeight() - 2 * margin);
                assertEquals(b.getWidth(), (int) (ratio * b.getHeight()));
                assertEquals(b.getLeft() - a.getLeft(), (int) ((a.getWidth() - b.getWidth()) / 2));
                assertEquals(a.getRight() - b.getRight(),
                        (int) ((a.getWidth() - b.getWidth()) / 2));
            }
        });
        b.setHorizontalBiasPercent(1);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("e) A: " + a + " B: " + b);
                assertEquals(b.getHeight(), a.getHeight() - 2 * margin);
                assertEquals(b.getWidth(), (int) (ratio * b.getHeight()));
                assertEquals(b.getRight(), a.getRight() - margin);
                assertEquals(b.getLeft(), a.getRight() - b.getWidth() - margin);
            }
        });
        b.setHorizontalBiasPercent(0);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("f) A: " + a + " B: " + b);
                assertEquals(b.getHeight(), a.getHeight() - 2 * margin);
                assertEquals(b.getWidth(), (int) (ratio * b.getHeight()));
                assertEquals(b.getRight(), a.getLeft() + margin + b.getWidth());
                assertEquals(b.getLeft(), a.getLeft() + margin);
            }
        });
    }

    @Test
    public void testCreateManyVariables() {
        final ConstraintWidgetContainer rootWidget = new ConstraintWidgetContainer(0, 0, 600, 400);
        ConstraintWidget previous = new ConstraintWidget(0, 0, 100, 20);
        rootWidget.add(previous);
        for (int i = 0; i < 100; i++) {
            ConstraintWidget w = new ConstraintWidget(0, 0, 100, 20);
            w.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT, 20);
            w.connect(ConstraintAnchor.Type.RIGHT, rootWidget, ConstraintAnchor.Type.RIGHT, 20);
            rootWidget.add(w);
        }
        rootWidget.layout();
    }

    @Test
    public void testWidgetCenterPositioning() {
        final int x = 20;
        final int y = 30;
        final ConstraintWidget rootWidget = new ConstraintWidget(x, y, 600, 400);
        final ConstraintWidget centeredWidget = new ConstraintWidget(100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        centeredWidget.resetSolverVariables(mLS.getCache());
        rootWidget.resetSolverVariables(mLS.getCache());
        widgets.add(centeredWidget);
        widgets.add(rootWidget);

        centeredWidget.setDebugName("A");
        rootWidget.setDebugName("Root");
        centeredWidget.connect(ConstraintAnchor.Type.CENTER_X,
                rootWidget, ConstraintAnchor.Type.CENTER_X);
        centeredWidget.connect(ConstraintAnchor.Type.CENTER_Y,
                rootWidget, ConstraintAnchor.Type.CENTER_Y);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("\n*** rootWidget: " + rootWidget
                        + " centeredWidget: " + centeredWidget);
                int left = centeredWidget.getLeft();
                int top = centeredWidget.getTop();
                int right = centeredWidget.getRight();
                int bottom = centeredWidget.getBottom();
                assertEquals(left, x + 250);
                assertEquals(right, x + 350);
                assertEquals(top, y + 190);
                assertEquals(bottom, y + 210);
            }
        });
    }

    @Test
    public void testBaselinePositioning() {
        final ConstraintWidget a = new ConstraintWidget(20, 230, 200, 70);
        final ConstraintWidget b = new ConstraintWidget(200, 60, 200, 100);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        a.setDebugName("A");
        b.setDebugName("B");
        a.setBaselineDistance(40);
        b.setBaselineDistance(60);
        b.connect(ConstraintAnchor.Type.BASELINE, a, ConstraintAnchor.Type.BASELINE);
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.layout();
        assertEquals(b.getTop() + b.getBaselineDistance(),
                a.getTop() + a.getBaselineDistance());
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(b.getTop() + b.getBaselineDistance(),
                        a.getTop() + a.getBaselineDistance());
            }
        });
    }

    //@Test
    public void testAddingWidgets() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        ArrayList<ConstraintWidget> widgetsA = new ArrayList();
        ArrayList<ConstraintWidget> widgetsB = new ArrayList();
        for (int i = 0; i < 1000; i++) {
            final ConstraintWidget a = new ConstraintWidget(0, 0, 200, 20);
            final ConstraintWidget b = new ConstraintWidget(0, 0, 200, 20);
            a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
            a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
            a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
            b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
            b.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
            b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
            widgetsA.add(a);
            widgetsB.add(b);
            root.add(a);
            root.add(b);
        }
        root.layout();
        for (ConstraintWidget widget : widgetsA) {
            assertEquals(widget.getLeft(), 200);
            assertEquals(widget.getTop(), 0);
        }
        for (ConstraintWidget widget : widgetsB) {
            assertEquals(widget.getLeft(), 600);
            assertEquals(widget.getTop(), 980);
        }
    }

    @Test
    public void testWidgetTopRightPositioning() {
        // Easy to tweak numbers to test larger systems
        int numLoops = 10;
        int numWidgets = 100;

        for (int j = 0; j < numLoops; j++) {
            mLS.reset();
            ArrayList<ConstraintWidget> widgets = new ArrayList();
            int w = 100 + j;
            int h = 20 + j;
            ConstraintWidget first = new ConstraintWidget(w, h);
            widgets.add(first);
            ConstraintWidget previous = first;
            int margin = 20;
            for (int i = 0; i < numWidgets; i++) {
                ConstraintWidget widget = new ConstraintWidget(w, h);
                widget.connect(ConstraintAnchor.Type.LEFT,
                        previous, ConstraintAnchor.Type.RIGHT, margin);
                widget.connect(ConstraintAnchor.Type.TOP,
                        previous, ConstraintAnchor.Type.BOTTOM, margin);
                widgets.add(widget);
                previous = widget;
            }
            for (ConstraintWidget widget : widgets) {
                widget.addToSolver(mLS, mOptimize);
            }
            try {
                mLS.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < widgets.size(); i++) {
                ConstraintWidget widget = widgets.get(i);
                widget.updateFromSolver(mLS, mOptimize);
                int left = widget.getLeft();
                int top = widget.getTop();
                int right = widget.getRight();
                int bottom = widget.getBottom();
                assertEquals(left, i * (w + margin));
                assertEquals(right, i * (w + margin) + w);
                assertEquals(top, i * (h + margin));
                assertEquals(bottom, i * (h + margin) + h);
            }
        }
    }

    @Test
    public void testWrapSimpleWrapContent() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        final ConstraintWidget a = new ConstraintWidget(0, 0, 200, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(a);

        root.setDebugSolverName(mLS, "root");
        a.setDebugSolverName(mLS, "A");

        root.add(a);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("Simple Wrap: " + root + ", " + a);
                assertEquals(root.getWidth(), a.getWidth());
                assertEquals(root.getHeight(), a.getHeight());
                assertEquals(a.getWidth(), 200);
                assertEquals(a.getHeight(), 20);
            }
        });
    }

    @Test
    public void testMatchConstraint() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(50, 50, 500, 500);
        final ConstraintWidget a = new ConstraintWidget(10, 20, 100, 30);
        final ConstraintWidget b = new ConstraintWidget(150, 200, 100, 30);
        final ConstraintWidget c = new ConstraintWidget(50, 50);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        root.setDebugName("root");
        root.add(a);
        root.add(b);
        root.add(c);
        widgets.add(root);
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);

        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        c.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        c.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        c.connect(ConstraintAnchor.Type.BOTTOM, b, ConstraintAnchor.Type.TOP);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(c.getX(), a.getRight());
                assertEquals(c.getRight(), b.getX());
                assertEquals(c.getY(), a.getBottom());
                assertEquals(c.getBottom(), b.getY());
            }
        });
    }

    // Obsolete @Test
    public void testWidgetStrengthPositioning() {
        final ConstraintWidget root = new ConstraintWidget(400, 400);
        final ConstraintWidget a = new ConstraintWidget(20, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(a);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        System.out.println("Widget A centered inside Root");
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getLeft(), 190);
                assertEquals(a.getRight(), 210);
                assertEquals(a.getTop(), 190);
                assertEquals(a.getBottom(), 210);
            }
        });
        System.out.println("Widget A weak left, should move to the right");
        a.getAnchor(ConstraintAnchor.Type.LEFT); //.setStrength(ConstraintAnchor.Strength.WEAK);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getLeft(), 380);
                assertEquals(a.getRight(), 400);
            }
        });
        System.out.println("Widget A weak right, should go back to center");
        a.getAnchor(ConstraintAnchor.Type.RIGHT); //.setStrength(ConstraintAnchor.Strength.WEAK);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getLeft(), 190);
                assertEquals(a.getRight(), 210);
            }
        });
        System.out.println("Widget A strong left, should move to the left");
        a.getAnchor(ConstraintAnchor.Type.LEFT); //.setStrength(ConstraintAnchor.Strength.STRONG);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getLeft(), 0);
                assertEquals(a.getRight(), 20);
                assertEquals(root.getWidth(), 400);
            }
        });
    }

    @Test
    public void testWidgetPositionMove() {
        final ConstraintWidget a = new ConstraintWidget(0, 0, 100, 20);
        final ConstraintWidget b = new ConstraintWidget(0, 30, 200, 20);
        final ConstraintWidget c = new ConstraintWidget(0, 60, 100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(a);
        widgets.add(b);
        widgets.add(c);
        a.setDebugSolverName(mLS, "A");
        b.setDebugSolverName(mLS, "B");
        c.setDebugSolverName(mLS, "C");

        b.connect(ConstraintAnchor.Type.LEFT, a, ConstraintAnchor.Type.RIGHT);
        c.setOrigin(200, 0);
        b.connect(ConstraintAnchor.Type.RIGHT, c, ConstraintAnchor.Type.RIGHT);

        Runnable check = new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getWidth(), 100);
                assertEquals(b.getWidth(), 200);
                assertEquals(c.getWidth(), 100);
            }
        };
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        c.setOrigin(100, 0);
//        runTestOnUIWidgets(widgets);
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + a + " B: " + b + " C: " + c);
        c.setOrigin(50, 0);
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + a + " B: " + b + " C: " + c);
    }

    @Test
    public void testWrapProblem() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(400, 400);
        final ConstraintWidget a = new ConstraintWidget(80, 300);
        final ConstraintWidget b = new ConstraintWidget(250, 80);
        final ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(b);
        widgets.add(a);
        a.setParent(root);
        b.setParent(root);
        root.setDebugSolverName(mLS, "root");
        a.setDebugSolverName(mLS, "A");
        b.setDebugSolverName(mLS, "B");

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
//        B.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.WEAK);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getWidth(), 80);
                assertEquals(a.getHeight(), 300);
                assertEquals(b.getWidth(), 250);
                assertEquals(b.getHeight(), 80);
                assertEquals(a.getY(), 0);
                assertEquals(b.getY(), 110);
            }
        });
    }

    @Test
    public void testGuideline() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(400, 400);
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        final Guideline guideline = new Guideline();
        root.add(guideline);
        root.add(a);
        guideline.setGuidePercent(0.50f);
        guideline.setOrientation(Guideline.VERTICAL);
        root.setDebugName("root");
        a.setDebugName("A");
        guideline.setDebugName("guideline");

        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(a);
        widgets.add(guideline);

        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT);
        Runnable check = new Runnable() {
            @Override
            public void run() {
                System.out.println("" + root + " " + a + " " + guideline);
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getX(), 200);
            }
        };
        runTest(root, check);
        guideline.setGuidePercent(0);
        runTest(root, new Runnable() {
            @Override
            public void run() {
                System.out.println("" + root + " " + a + " " + guideline);
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getX(), 0);
            }
        });
        guideline.setGuideBegin(150);
        runTest(root, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getX(), 150);
            }
        });
        System.out.println("" + root + " " + a + " " + guideline);
        guideline.setGuideEnd(150);
        runTest(root, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getX(), 250);
            }
        });
        System.out.println("" + root + " " + a + " " + guideline);
        guideline.setOrientation(Guideline.HORIZONTAL);
        a.resetAnchors();
        a.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.TOP);
        guideline.setGuideBegin(150);
        runTest(root, new Runnable() {
            @Override
            public void run() {
                System.out.println("" + root + " " + a + " " + guideline);
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getY(), 150);
            }
        });
        System.out.println("" + root + " " + a + " " + guideline);
        a.resetAnchors();
        a.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.BOTTOM);
        runTest(root, new Runnable() {
            @Override
            public void run() {
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getY(), 150);
            }
        });
        System.out.println("" + root + " " + a + " " + guideline);
    }

    private void runTest(ConstraintWidgetContainer root, Runnable check) {
        root.layout();
        check.run();
    }


    @Test
    public void testGuidelinePosition() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(800, 400);
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        final ConstraintWidget b = new ConstraintWidget(100, 20);
        final Guideline guideline = new Guideline();
        root.add(guideline);
        root.add(a);
        root.add(b);
        guideline.setGuidePercent(0.651f);
        guideline.setOrientation(Guideline.VERTICAL);
        root.setDebugSolverName(mLS, "root");
        a.setDebugSolverName(mLS, "A");
        b.setDebugSolverName(mLS, "B");
        guideline.setDebugSolverName(mLS, "guideline");

        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(a);
        widgets.add(b);
        widgets.add(guideline);

        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.RIGHT);
        b.connect(ConstraintAnchor.Type.RIGHT, guideline, ConstraintAnchor.Type.RIGHT);
        Runnable check = new Runnable() {
            @Override
            public void run() {
                System.out.println("" + root + " A: " + a + " "
                        + " B: " + b + " guideline: " + guideline);
                assertEquals(a.getWidth(), 100);
                assertEquals(a.getHeight(), 20);
                assertEquals(a.getX(), 521);
                assertEquals(b.getRight(), 521);
            }
        };
        runTestOnWidgets(widgets, check);
    }

    @Test
    public void testWidgetInfeasiblePosition() {
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        final ConstraintWidget b = new ConstraintWidget(100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(b);
        widgets.add(a);
        a.resetSolverVariables(mLS.getCache());
        b.resetSolverVariables(mLS.getCache());

        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, a, ConstraintAnchor.Type.LEFT);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                // TODO: this fail -- need to figure the best way to fix this.
//                assertEquals(A.getWidth(), 100);
//                assertEquals(B.getWidth(), 100);
            }
        });
    }

    @Test
    public void testWidgetMultipleDependentPositioning() {
        final ConstraintWidget root = new ConstraintWidget(400, 400);
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        final ConstraintWidget b = new ConstraintWidget(100, 20);
        root.setDebugSolverName(mLS, "root");
        a.setDebugSolverName(mLS, "A");
        b.setDebugSolverName(mLS, "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(b);
        widgets.add(a);

        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 10);
        b.connect(ConstraintAnchor.Type.TOP, a, ConstraintAnchor.Type.BOTTOM);
        b.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.resetSolverVariables(mLS.getCache());
        a.resetSolverVariables(mLS.getCache());
        b.resetSolverVariables(mLS.getCache());

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("root: " + root + " A: " + a + " B: " + b);
                assertEquals(root.getHeight(), 400);
                assertEquals(root.getHeight(), 400);
                assertEquals(a.getHeight(), 20);
                assertEquals(b.getHeight(), 20);
                assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
                assertEquals(b.getTop() - a.getBottom(), root.getBottom() - b.getBottom());
            }
        });
    }

    @Test
    public void testMinSize() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(600, 400);
        final ConstraintWidget a = new ConstraintWidget(100, 20);
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setDebugName("root");
        a.setDebugName("A");
        root.add(a);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 600);
        assertEquals(root.getHeight(), 400);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - a.getRight());
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " A: " + a);
        assertEquals(root.getHeight(), a.getHeight());
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - a.getRight());
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
        root.setMinHeight(200);
        root.layout();
        System.out.println("c) root: " + root + " A: " + a);
        assertEquals(root.getHeight(), 200);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - a.getRight());
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("d) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), a.getWidth());
        assertEquals(root.getHeight(), 200);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - a.getRight());
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
        root.setMinWidth(300);
        root.layout();
        System.out.println("e) root: " + root + " A: " + a);
        assertEquals(root.getWidth(), 300);
        assertEquals(root.getHeight(), 200);
        assertEquals(a.getWidth(), 100);
        assertEquals(a.getHeight(), 20);
        assertEquals(a.getLeft() - root.getLeft(), root.getRight() - a.getRight());
        assertEquals(a.getTop() - root.getTop(), root.getBottom() - a.getBottom());
    }
    /*
     * Insert the widgets in all permutations
     * (to test that the insert order
     * doesn't impact the resolution)
     */

    private void runTestOnWidgets(ArrayList<ConstraintWidget> widgets, Runnable check) {
        ArrayList<Integer> tail = new ArrayList<Integer>();
        for (int i = 0; i < widgets.size(); i++) {
            tail.add(i);
        }
        addToSolverWithPermutation(widgets, new ArrayList<Integer>(), tail, check);
    }

    private void runTestOnUIWidgets(ArrayList<ConstraintWidget> widgets) {
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            if (widget.getDebugName() != null) {
                widget.setDebugSolverName(mLS, widget.getDebugName());
            }
            widget.resetSolverVariables(mLS.getCache());
            widget.addToSolver(mLS, mOptimize);
        }
        try {
            mLS.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int j = 0; j < widgets.size(); j++) {
            ConstraintWidget w = widgets.get(j);
            w.updateFromSolver(mLS, mOptimize);
            System.out.println(" " + w);
        }
    }

    private void addToSolverWithPermutation(ArrayList<ConstraintWidget> widgets,
            ArrayList<Integer> list, ArrayList<Integer> tail, Runnable check) {
        if (tail.size() > 0) {
            int n = tail.size();
            for (int i = 0; i < n; i++) {
                list.add(tail.get(i));
                ArrayList<Integer> permuted = new ArrayList<Integer>(tail);
                permuted.remove(i);
                addToSolverWithPermutation(widgets, list, permuted, check);
                list.remove(list.size() - 1);
            }
        } else {
//            System.out.print("Adding widgets in order: ");
            mLS.reset();
            for (int i = 0; i < list.size(); i++) {
                int index = list.get(i);
//                System.out.print(" " + index);
                ConstraintWidget widget = widgets.get(index);
                widget.resetSolverVariables(mLS.getCache());
            }
            for (int i = 0; i < list.size(); i++) {
                int index = list.get(i);
//                System.out.print(" " + index);
                ConstraintWidget widget = widgets.get(index);
                if (widget.getDebugName() != null) {
                    widget.setDebugSolverName(mLS, widget.getDebugName());
                }
                widget.addToSolver(mLS, mOptimize);
            }
//            System.out.println("");
//            s.displayReadableRows();
            try {
                mLS.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < widgets.size(); j++) {
                ConstraintWidget w = widgets.get(j);
                w.updateFromSolver(mLS, mOptimize);
            }
//            try {
            check.run();
//            } catch (AssertionError e) {
//                System.out.println("Assertion error: " + e);
//                runTestOnUIWidgets(widgets);
//            }
        }
    }

}
