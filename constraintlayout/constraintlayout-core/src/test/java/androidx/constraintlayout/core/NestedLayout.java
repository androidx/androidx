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

/**
 * Test nested layout
 */
public class NestedLayout {

    // @Test
    public void testNestedLayout() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(20, 20, 1000, 1000);
        ConstraintWidgetContainer container = new ConstraintWidgetContainer(0, 0, 100, 100);
        root.setDebugName("root");
        container.setDebugName("container");
        container.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        container.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        root.add(container);
        root.layout();
        System.out.println("container: " + container);
        assertEquals(container.getLeft(), 450);
        assertEquals(container.getWidth(), 100);

        ConstraintWidget a = new ConstraintWidget(0, 0, 100, 20);
        ConstraintWidget b = new ConstraintWidget(0, 0, 50, 20);
        container.add(a);
        container.add(b);
        container.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.connect(ConstraintAnchor.Type.LEFT, container, ConstraintAnchor.Type.LEFT);
        a.connect(ConstraintAnchor.Type.RIGHT, b, ConstraintAnchor.Type.LEFT);
        b.connect(ConstraintAnchor.Type.RIGHT, container, ConstraintAnchor.Type.RIGHT);
        root.layout();
        System.out.println("container: " + container);
        System.out.println("A: " + a);
        System.out.println("B: " + b);
        assertEquals(container.getWidth(), 150);
        assertEquals(container.getLeft(), 425);
        assertEquals(a.getLeft(), 425);
        assertEquals(b.getLeft(), 525);
        assertEquals(a.getWidth(), 100);
        assertEquals(b.getWidth(), 50);
    }
}
