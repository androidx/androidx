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

import androidx.constraintlayout.core.state.ConstraintReference;
import androidx.constraintlayout.core.state.Dimension;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import org.junit.Test;

public class ComposeLayoutsTest {

    static BasicMeasure.Measurer sMeasurer = new BasicMeasure.Measurer() {

        @Override
        public void measure(ConstraintWidget widget, BasicMeasure.Measure measure) {
            ConstraintWidget.DimensionBehaviour horizontalBehavior = measure.horizontalBehavior;
            ConstraintWidget.DimensionBehaviour verticalBehavior = measure.verticalBehavior;
            int horizontalDimension = measure.horizontalDimension;
            int verticalDimension = measure.verticalDimension;

            System.out.println("Measure (strategy : " + measure.measureStrategy + ") : "
                    + widget.getCompanionWidget()
                    + " " + horizontalBehavior + " (" + horizontalDimension + ") x "
                    + verticalBehavior + " (" + verticalDimension + ")");

            if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredWidth = horizontalDimension;
            } else if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                measure.measuredWidth = horizontalDimension;
                if (widget.getCompanionWidget().equals("box")
                        // && measure.measuredWidth == 0
                        && measure.measureStrategy == BasicMeasure.Measure.SELF_DIMENSIONS) {
                    measure.measuredWidth = 1080;
                }
            } else if (horizontalBehavior == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                if (widget.getCompanionWidget().equals("box")) {
                    measure.measuredWidth = 1080;
                }
            }
            if (verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                measure.measuredHeight = verticalDimension;
            } else if (verticalBehavior == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                if (widget.getCompanionWidget().equals("box")) {
                    measure.measuredHeight = measure.measuredWidth / 2;
                }
            }
            System.out.println("Measure widget " + widget.getCompanionWidget()
                    + " => " + measure.measuredWidth + " x " + measure.measuredHeight);
        }

        @Override
        public void didMeasures() {

        }
    };

    @Test
    public void dividerMatchTextHeight_inWrapConstraintLayout_longText() {
        State state = new State();
        ConstraintReference parent = state.constraints(State.PARENT);
        state.verticalGuideline("guideline").percent(0.5f);

        state.constraints("box")
                .centerHorizontally(parent)
                .centerVertically(parent)
                .startToEnd("guideline")
                .width(Dimension.createSuggested(Dimension.WRAP_DIMENSION))
                .height(Dimension.createWrap())
                .setView("box");
        state.constraints("divider")
                .centerHorizontally(parent)
                .centerVertically(parent)
                .width(Dimension.createFixed(1))
                .height(Dimension.createPercent(0, 0.8f).suggested(0))
                .setView("divider");
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1977);
        state.apply(root);
        root.setWidth(1080);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        ConstraintWidget box = state.constraints("box").getConstraintWidget();
        ConstraintWidget guideline = state.guideline("guideline",
                ConstraintWidget.VERTICAL).getConstraintWidget();
        ConstraintWidget divider = state.constraints("divider").getConstraintWidget();
        root.setDebugName("root");
        box.setDebugName("box");
        guideline.setDebugName("guideline");
        divider.setDebugName("divider");

        root.setMeasurer(sMeasurer);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        //root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.measure(root.getOptimizationLevel(), 0, 0, 0, 0, 0, 0, 0, 0);

        System.out.println("root: " + root);
        System.out.println("box: " + box);
        System.out.println("guideline: " + guideline);
        System.out.println("divider: " + divider);

        assertEquals(root.getWidth() / 2, box.getWidth());
        assertEquals(root.getWidth() / 2 / 2, box.getHeight());
        assertEquals(1, divider.getWidth());
        assertEquals((int) (box.getHeight() * 0.8), divider.getHeight());
    }
}
