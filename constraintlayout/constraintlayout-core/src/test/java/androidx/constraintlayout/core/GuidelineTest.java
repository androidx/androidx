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
import androidx.constraintlayout.core.widgets.Guideline;

import org.junit.Test;

public class GuidelineTest {

    @Test
    public void testWrapGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guidelineRight = new Guideline();
        guidelineRight.setOrientation(Guideline.VERTICAL);
        Guideline guidelineBottom = new Guideline();
        guidelineBottom.setOrientation(Guideline.HORIZONTAL);
        guidelineRight.setGuidePercent(0.64f);
        guidelineBottom.setGuideEnd(60);
        root.setDebugName("Root");
        a.setDebugName("A");
        guidelineRight.setDebugName("GuidelineRight");
        guidelineBottom.setDebugName("GuidelineBottom");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, guidelineRight, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.BOTTOM, guidelineBottom, ConstraintAnchor.Type.TOP);
        root.add(a);
        root.add(guidelineRight);
        root.add(guidelineBottom);
        root.layout();
        System.out.println("a) root: " + root + " guideline right: " + guidelineRight
                + " guideline bottom: " + guidelineBottom + " A: " + a);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " guideline right: " + guidelineRight
                + " guideline bottom: " + guidelineBottom + " A: " + a);
        assertEquals(root.getHeight(), 80);
    }

    @Test
    public void testWrapGuideline2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.VERTICAL);
        guideline.setGuideBegin(60);
        root.setDebugName("Root");
        a.setDebugName("A");
        guideline.setDebugName("Guideline");
        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT, 5);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 5);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.add(a);
        root.add(guideline);
//        root.layout();
        System.out.println("a) root: " + root + " guideline: " + guideline + " A: " + a);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " guideline: " + guideline + " A: " + a);
        assertEquals(root.getWidth(), 70);
    }
}
