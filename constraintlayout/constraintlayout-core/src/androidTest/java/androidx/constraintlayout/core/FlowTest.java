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
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.UNSPECIFIED;

import static org.junit.Assert.assertEquals;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Flow;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.VirtualLayout;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import org.junit.Test;

public class FlowTest {
    static BasicMeasure.Measurer sMeasurer = new BasicMeasure.Measurer() {

        @Override
        public void measure(ConstraintWidget widget, BasicMeasure.Measure measure) {
            ConstraintWidget.DimensionBehaviour horizontalBehavior = measure.horizontalBehavior;
            ConstraintWidget.DimensionBehaviour verticalBehavior = measure.verticalBehavior;
            int horizontalDimension = measure.horizontalDimension;
            int verticalDimension = measure.verticalDimension;

            if (widget instanceof VirtualLayout) {
                VirtualLayout layout = (VirtualLayout) widget;
                int widthMode = UNSPECIFIED;
                int heightMode = UNSPECIFIED;
                int widthSize = 0;
                int heightSize = 0;
                if (layout.getHorizontalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                    widthSize = layout.getParent() != null ? layout.getParent().getWidth() : 0;
                    widthMode = EXACTLY;
                } else if (horizontalBehavior
                        == ConstraintWidget.DimensionBehaviour.FIXED) {
                    widthSize = horizontalDimension;
                    widthMode = EXACTLY;
                }
                if (layout.getVerticalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                    heightSize = layout.getParent() != null ? layout.getParent().getHeight() : 0;
                    heightMode = EXACTLY;
                } else if (verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                    heightSize = verticalDimension;
                    heightMode = EXACTLY;
                }
                layout.measure(widthMode, widthSize, heightMode, heightSize);
                measure.measuredWidth = layout.getMeasuredWidth();
                measure.measuredHeight = layout.getMeasuredHeight();
            } else {
                if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                    measure.measuredWidth = horizontalDimension;
                }
                if (verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                    measure.measuredHeight = verticalDimension;
                }
            }
        }

        @Override
        public void didMeasures() {

        }
    };

    @Test
    public void testFlowBaseline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1536);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(20, 15);
        Flow flow = new Flow();

        root.setMeasurer(sMeasurer);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        flow.setDebugName("Flow");

        flow.setVerticalAlign(Flow.VERTICAL_ALIGN_BASELINE);
        flow.add(a);
        flow.add(b);
        a.setBaselineDistance(15);

        flow.setHeight(30);
        flow.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        flow.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        flow.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        flow.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        flow.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        flow.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(flow);
        root.add(a);
        root.add(b);

        root.measure(Optimizer.OPTIMIZATION_NONE,
                0, 0, 0, 0, 0, 0, 0, 0);
        root.layout();
        System.out.println("a) root: " + root);
        System.out.println("flow: " + flow);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        assertEquals(flow.getWidth(), 1080);
        assertEquals(flow.getHeight(), 30);
        assertEquals(flow.getTop(), 753);
        assertEquals(a.getLeft(), 320);
        assertEquals(a.getTop(), 758);
        assertEquals(b.getLeft(), 740);
        assertEquals(b.getTop(), 761);
    }

    @Test
    public void testComplexChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1536);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget b = new ConstraintWidget(100, 20);
        ConstraintWidget c = new ConstraintWidget(100, 20);
        Flow flow = new Flow();

        root.setMeasurer(sMeasurer);

        root.setDebugName("root");
        a.setDebugName("A");
        b.setDebugName("B");
        c.setDebugName("C");
        flow.setDebugName("Flow");

        flow.setWrapMode(Flow.WRAP_CHAIN);
        flow.setMaxElementsWrap(2);

        flow.add(a);
        flow.add(b);
        flow.add(c);

        root.add(flow);
        root.add(a);
        root.add(b);
        root.add(c);

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        b.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        c.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        flow.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        flow.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        flow.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        flow.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        flow.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
        flow.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.measure(Optimizer.OPTIMIZATION_NONE,
                0, 0, 0, 0, 0, 0, 0, 0);
        root.layout();
        System.out.println("a) root: " + root);
        System.out.println("flow: " + flow);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        System.out.println("C: " + c);

        assertEquals(a.getWidth(), 540);
        assertEquals(b.getWidth(), 540);
        assertEquals(c.getWidth(), 1080);
        assertEquals(flow.getWidth(), root.getWidth());
        assertEquals(flow.getHeight(), Math.max(a.getHeight(), b.getHeight()) + c.getHeight());
        assertEquals(flow.getTop(), 748);
    }

    @Test
    public void testFlowText() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(20, 5);
        ConstraintWidget a = new ConstraintWidget(7, 1);
        ConstraintWidget b = new ConstraintWidget(6, 1);
        a.setDebugName("A");
        b.setDebugName("B");
        Flow flow = new Flow();
        flow.setDebugName("flow");
        flow.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        flow.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        flow.setWidth(20);
        flow.setHeight(2);
        flow.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        flow.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        flow.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        flow.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        flow.add(a);
        flow.add(b);
        root.add(flow);
        root.add(a);
        root.add(b);
        root.setMeasurer(new BasicMeasure.Measurer() {
            @Override
            public void measure(ConstraintWidget widget, BasicMeasure.Measure measure) {
                measure.measuredWidth = widget.getWidth();
                measure.measuredHeight = widget.getHeight();
            }

            @Override
            public void didMeasures() {

            }
        });
        root.setMeasurer(sMeasurer);
        root.measure(Optimizer.OPTIMIZATION_NONE,
                0, 0, 0, 0, 0, 0, 0, 0);
        //root.layout();
        System.out.println("root: " + root);
        System.out.println("flow: " + flow);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
    }
}
