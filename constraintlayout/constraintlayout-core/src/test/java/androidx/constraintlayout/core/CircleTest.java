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

public class CircleTest {

    @Test
    public void basic() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget a = new ConstraintWidget(100, 20);
        ConstraintWidget w1 = new ConstraintWidget(10, 10);
        ConstraintWidget w2 = new ConstraintWidget(10, 10);
        ConstraintWidget w3 = new ConstraintWidget(10, 10);
        ConstraintWidget w4 = new ConstraintWidget(10, 10);
        ConstraintWidget w5 = new ConstraintWidget(10, 10);
        ConstraintWidget w6 = new ConstraintWidget(10, 10);
        ConstraintWidget w7 = new ConstraintWidget(10, 10);
        ConstraintWidget w8 = new ConstraintWidget(10, 10);
        ConstraintWidget w9 = new ConstraintWidget(10, 10);
        ConstraintWidget w10 = new ConstraintWidget(10, 10);
        ConstraintWidget w11 = new ConstraintWidget(10, 10);
        ConstraintWidget w12 = new ConstraintWidget(10, 10);

        root.setDebugName("root");
        a.setDebugName("A");
        w1.setDebugName("w1");
        w2.setDebugName("w2");
        w3.setDebugName("w3");
        w4.setDebugName("w4");
        w5.setDebugName("w5");
        w6.setDebugName("w6");
        w7.setDebugName("w7");
        w8.setDebugName("w8");
        w9.setDebugName("w9");
        w10.setDebugName("w10");
        w11.setDebugName("w11");
        w12.setDebugName("w12");

        root.add(a);

        root.add(w1);
        root.add(w2);
        root.add(w3);
        root.add(w4);
        root.add(w5);
        root.add(w6);
        root.add(w7);
        root.add(w8);
        root.add(w9);
        root.add(w10);
        root.add(w11);
        root.add(w12);

        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        w1.connectCircularConstraint(a, 30, 50);
        w2.connectCircularConstraint(a, 60, 50);
        w3.connectCircularConstraint(a, 90, 50);
        w4.connectCircularConstraint(a, 120, 50);
        w5.connectCircularConstraint(a, 150, 50);
        w6.connectCircularConstraint(a, 180, 50);
        w7.connectCircularConstraint(a, 210, 50);
        w8.connectCircularConstraint(a, 240, 50);
        w9.connectCircularConstraint(a, 270, 50);
        w10.connectCircularConstraint(a, 300, 50);
        w11.connectCircularConstraint(a, 330, 50);
        w12.connectCircularConstraint(a, 360, 50);

        root.layout();

        System.out.println("w1: " + w1);
        System.out.println("w2: " + w2);
        System.out.println("w3: " + w3);
        System.out.println("w4: " + w4);
        System.out.println("w5: " + w5);
        System.out.println("w6: " + w6);
        System.out.println("w7: " + w7);
        System.out.println("w8: " + w8);
        System.out.println("w9: " + w9);
        System.out.println("w10: " + w10);
        System.out.println("w11: " + w11);
        System.out.println("w12: " + w12);

        assertEquals(w1.getLeft(), 520);
        assertEquals(w1.getTop(), 252);
        assertEquals(w2.getLeft(), 538);
        assertEquals(w2.getTop(), 270);
        assertEquals(w3.getLeft(), 545);
        assertEquals(w3.getTop(), 295);
        assertEquals(w4.getLeft(), 538);
        assertEquals(w4.getTop(), 320);
        assertEquals(w5.getLeft(), 520);
        assertEquals(w5.getTop(), 338);
        assertEquals(w6.getLeft(), 495);
        assertEquals(w6.getTop(), 345);
        assertEquals(w7.getLeft(), 470);
        assertEquals(w7.getTop(), 338);
        assertEquals(w8.getLeft(), 452);
        assertEquals(w8.getTop(), 320);
        assertEquals(w9.getLeft(), 445);
        assertEquals(w9.getTop(), 295);
        assertEquals(w10.getLeft(), 452);
        assertEquals(w10.getTop(), 270);
        assertEquals(w11.getLeft(), 470);
        assertEquals(w11.getTop(), 252);
        assertEquals(w12.getLeft(), 495);
        assertEquals(w12.getTop(), 245);
    }
}
