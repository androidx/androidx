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

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import org.junit.Test;

public class LayoutTest {

    @Test
    public void testPositions() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(1000, 1000);
        root.setDebugName("root");
        ConstraintWidget button3 = new ConstraintWidget(200, 20);
        button3.setDebugName("button3");
        ConstraintWidget button4 = new ConstraintWidget(200, 20);
        button4.setDebugName("button4");
        ConstraintWidget button5 = new ConstraintWidget(200, 20);
        button5.setDebugName("button5");
        ConstraintWidget editText = new ConstraintWidget(200, 20);
        editText.setDebugName("editText");
        ConstraintWidget button6 = new ConstraintWidget(200, 20);
        button6.setDebugName("button6");
        ConstraintWidget button7 = new ConstraintWidget(200, 20);
        button7.setDebugName("button7");
        ConstraintWidget button8 = new ConstraintWidget(200, 20);
        button8.setDebugName("button8");
        ConstraintWidget button9 = new ConstraintWidget(200, 20);
        button9.setDebugName("button9");
        ConstraintWidget editText2 = new ConstraintWidget(200, 20);
        editText2.setDebugName("editText2");
        ConstraintWidget toggleButton = new ConstraintWidget(200, 20);
        toggleButton.setDebugName("toggleButton");
        ConstraintWidget toggleButton2 = new ConstraintWidget(200, 20);
        toggleButton2.setDebugName("toggleButton2");
        ConstraintWidget toggleButton3 = new ConstraintWidget(200, 20);
        toggleButton3.setDebugName("toggleButton3");
        ConstraintWidget toggleButton4 = new ConstraintWidget(200, 20);
        toggleButton4.setDebugName("toggleButton4");
        ConstraintWidget textView = new ConstraintWidget(200, 20);
        textView.setDebugName("textView");
        ConstraintWidget textView2 = new ConstraintWidget(200, 20);
        textView2.setDebugName("textView2");
        ConstraintWidget back = new ConstraintWidget(200, 20);
        back.setDebugName("back");

        button3.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        button3.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        button4.connect(ConstraintAnchor.Type.LEFT, button3, ConstraintAnchor.Type.LEFT);
        button4.connect(ConstraintAnchor.Type.TOP, button3, ConstraintAnchor.Type.BOTTOM);
        button4.connect(ConstraintAnchor.Type.RIGHT, button3, ConstraintAnchor.Type.RIGHT);

        button5.connect(ConstraintAnchor.Type.LEFT, button4, ConstraintAnchor.Type.LEFT);
        button5.connect(ConstraintAnchor.Type.TOP, button4, ConstraintAnchor.Type.BOTTOM);
        button5.connect(ConstraintAnchor.Type.RIGHT, button4, ConstraintAnchor.Type.RIGHT);

        editText.connect(ConstraintAnchor.Type.LEFT, button5, ConstraintAnchor.Type.LEFT);
        editText.connect(ConstraintAnchor.Type.TOP, button5, ConstraintAnchor.Type.BOTTOM);

        button6.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        button6.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        button7.connect(ConstraintAnchor.Type.LEFT, button6, ConstraintAnchor.Type.LEFT);
        button7.connect(ConstraintAnchor.Type.TOP, button6, ConstraintAnchor.Type.BOTTOM);
        button7.connect(ConstraintAnchor.Type.RIGHT, button6, ConstraintAnchor.Type.RIGHT);

        button8.connect(ConstraintAnchor.Type.LEFT, button5, ConstraintAnchor.Type.LEFT);
        button8.connect(ConstraintAnchor.Type.TOP, button5, ConstraintAnchor.Type.BOTTOM);
        button8.connect(ConstraintAnchor.Type.RIGHT, button5, ConstraintAnchor.Type.RIGHT);

        button9.connect(ConstraintAnchor.Type.LEFT, toggleButton2, ConstraintAnchor.Type.LEFT);
        button9.connect(ConstraintAnchor.Type.RIGHT, toggleButton2, ConstraintAnchor.Type.RIGHT);
        button9.connect(ConstraintAnchor.Type.BASELINE, button8, ConstraintAnchor.Type.BASELINE);

        editText2.connect(ConstraintAnchor.Type.LEFT, editText, ConstraintAnchor.Type.LEFT);
        editText2.connect(ConstraintAnchor.Type.TOP, editText, ConstraintAnchor.Type.BOTTOM);
        editText2.connect(ConstraintAnchor.Type.RIGHT, editText, ConstraintAnchor.Type.RIGHT);

        toggleButton.connect(ConstraintAnchor.Type.LEFT, button8, ConstraintAnchor.Type.LEFT);
        toggleButton.connect(ConstraintAnchor.Type.TOP, button8, ConstraintAnchor.Type.BOTTOM);
        toggleButton.connect(ConstraintAnchor.Type.RIGHT, button8, ConstraintAnchor.Type.RIGHT);

        toggleButton2.connect(ConstraintAnchor.Type.LEFT, toggleButton,
                ConstraintAnchor.Type.RIGHT);
        toggleButton2.connect(ConstraintAnchor.Type.TOP, button9, ConstraintAnchor.Type.BOTTOM);

        toggleButton3.connect(ConstraintAnchor.Type.LEFT, toggleButton2,
                ConstraintAnchor.Type.RIGHT);
        toggleButton3.connect(ConstraintAnchor.Type.TOP, toggleButton2, ConstraintAnchor.Type.TOP);

        toggleButton4.connect(ConstraintAnchor.Type.LEFT, toggleButton3,
                ConstraintAnchor.Type.RIGHT);
        toggleButton4.connect(ConstraintAnchor.Type.BASELINE,
                toggleButton3, ConstraintAnchor.Type.BASELINE);

        textView.connect(ConstraintAnchor.Type.LEFT, textView2, ConstraintAnchor.Type.LEFT);
        textView.connect(ConstraintAnchor.Type.TOP, textView2, ConstraintAnchor.Type.BOTTOM);

        textView2.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        textView2.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        back.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        back.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(button3);
        root.add(button4);
        root.add(button5);
        root.add(editText);
        root.add(button6);
        root.add(button7);
        root.add(button8);
        root.add(button9);
        root.add(editText2);
        root.add(toggleButton);
        root.add(toggleButton2);
        root.add(toggleButton3);
        root.add(toggleButton4);
        root.add(textView);
        root.add(textView2);
        root.add(button3);
        root.add(back);

        while (true) {
            long time = System.nanoTime();
            for (int i = 0; i < 1; i++) {
                root.layout();
            }
            long time2 = System.nanoTime() - time;
            System.out.println("Time spent: " + time2 / 1E6 + " ms");
            root.getSystem().displaySystemInformation();
            return;
        }
    }
}

