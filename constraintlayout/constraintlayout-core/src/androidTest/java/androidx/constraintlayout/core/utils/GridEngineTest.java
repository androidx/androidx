/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.constraintlayout.core.utils.GridEngine;

import org.junit.Test;

public class GridEngineTest {

    @Test
    public void testNoArgument() {
        GridEngine engine = new GridEngine();
        engine.setRows(3);
        engine.setColumns(3);
        engine.setNumWidgets(5);
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(0, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(0, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));

    }

    @Test
    public void testSimpleEngineHorizontal() {
        GridEngine engine = new GridEngine(3, 3, 5);

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(0, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(0, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testSimpleEngineVertical() {
        // Grid engine with 3 rows, 3 columns, and 5 widgets
        GridEngine engine = new GridEngine(3, 3, 5);
        // Set the orientation to Vertical
        engine.setOrientation(1);
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(0, engine.leftOfWidget(1));
        assertEquals(1, engine.topOfWidget(1));
        assertEquals(0, engine.rightOfWidget(1));
        assertEquals(1, engine.bottomOfWidget(1));

        assertEquals(0, engine.leftOfWidget(2));
        assertEquals(2, engine.topOfWidget(2));
        assertEquals(0, engine.rightOfWidget(2));
        assertEquals(2, engine.bottomOfWidget(2));

        assertEquals(1, engine.leftOfWidget(3));
        assertEquals(0, engine.topOfWidget(3));
        assertEquals(1, engine.rightOfWidget(3));
        assertEquals(0, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testSpansSkipsHorizontal() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setSkips("0:2x2,6:1x1");
        engine.setSpans("12:1x3");
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(3, engine.topOfWidget(0));
        assertEquals(2, engine.rightOfWidget(0));
        assertEquals(3, engine.bottomOfWidget(0));

        assertEquals(2, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(2, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(3, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(3, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(3, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(3, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(0, engine.leftOfWidget(4));
        assertEquals(2, engine.topOfWidget(4));
        assertEquals(0, engine.rightOfWidget(4));
        assertEquals(2, engine.bottomOfWidget(4));
    }

    @Test
    public void testSpansSkipsVertical() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setOrientation(1);
        engine.setSkips("0:2x2,6:2x1");
        engine.setSpans("12:3x1");
        engine.setup();

        assertEquals(3, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(3, engine.rightOfWidget(0));
        assertEquals(2, engine.bottomOfWidget(0));

        assertEquals(0, engine.leftOfWidget(1));
        assertEquals(2, engine.topOfWidget(1));
        assertEquals(0, engine.rightOfWidget(1));
        assertEquals(2, engine.bottomOfWidget(1));

        assertEquals(0, engine.leftOfWidget(2));
        assertEquals(3, engine.topOfWidget(2));
        assertEquals(0, engine.rightOfWidget(2));
        assertEquals(3, engine.bottomOfWidget(2));

        assertEquals(2, engine.leftOfWidget(3));
        assertEquals(0, engine.topOfWidget(3));
        assertEquals(2, engine.rightOfWidget(3));
        assertEquals(0, engine.bottomOfWidget(3));

        assertEquals(2, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(2, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testSetRows() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setRows(3);
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(3, engine.leftOfWidget(3));
        assertEquals(0, engine.topOfWidget(3));
        assertEquals(3, engine.rightOfWidget(3));
        assertEquals(0, engine.bottomOfWidget(3));

        assertEquals(0, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(0, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testSetColumns() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setColumns(3);
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(0, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(0, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testSetNumWidgets() {
        GridEngine engine = new GridEngine(3, 3);
        engine.setNumWidgets(5);
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(0, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(0, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testToggleSkips() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setSkips("0:2x2,6:1x1");
        engine.setup();

        assertEquals(2, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(2, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(3, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(3, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(3, engine.leftOfWidget(2));
        assertEquals(1, engine.topOfWidget(2));
        assertEquals(3, engine.rightOfWidget(2));
        assertEquals(1, engine.bottomOfWidget(2));

        assertEquals(0, engine.leftOfWidget(3));
        assertEquals(2, engine.topOfWidget(3));
        assertEquals(0, engine.rightOfWidget(3));
        assertEquals(2, engine.bottomOfWidget(3));

        assertEquals(1, engine.leftOfWidget(4));
        assertEquals(2, engine.topOfWidget(4));
        assertEquals(1, engine.rightOfWidget(4));
        assertEquals(2, engine.bottomOfWidget(4));

        engine.setSkips("");
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(3, engine.leftOfWidget(3));
        assertEquals(0, engine.topOfWidget(3));
        assertEquals(3, engine.rightOfWidget(3));
        assertEquals(0, engine.bottomOfWidget(3));

        assertEquals(0, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(0, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }

    @Test
    public void testToggleSpans() {
        GridEngine engine = new GridEngine(4, 4, 5);
        engine.setSpans("0:2x2,3:2x1");
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(1, engine.rightOfWidget(0));
        assertEquals(1, engine.bottomOfWidget(0));

        assertEquals(3, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(3, engine.rightOfWidget(1));
        assertEquals(1, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(2, engine.leftOfWidget(3));
        assertEquals(1, engine.topOfWidget(3));
        assertEquals(2, engine.rightOfWidget(3));
        assertEquals(1, engine.bottomOfWidget(3));

        assertEquals(0, engine.leftOfWidget(4));
        assertEquals(2, engine.topOfWidget(4));
        assertEquals(0, engine.rightOfWidget(4));
        assertEquals(2, engine.bottomOfWidget(4));

        engine.setSpans("");
        engine.setup();

        assertEquals(0, engine.leftOfWidget(0));
        assertEquals(0, engine.topOfWidget(0));
        assertEquals(0, engine.rightOfWidget(0));
        assertEquals(0, engine.bottomOfWidget(0));

        assertEquals(1, engine.leftOfWidget(1));
        assertEquals(0, engine.topOfWidget(1));
        assertEquals(1, engine.rightOfWidget(1));
        assertEquals(0, engine.bottomOfWidget(1));

        assertEquals(2, engine.leftOfWidget(2));
        assertEquals(0, engine.topOfWidget(2));
        assertEquals(2, engine.rightOfWidget(2));
        assertEquals(0, engine.bottomOfWidget(2));

        assertEquals(3, engine.leftOfWidget(3));
        assertEquals(0, engine.topOfWidget(3));
        assertEquals(3, engine.rightOfWidget(3));
        assertEquals(0, engine.bottomOfWidget(3));

        assertEquals(0, engine.leftOfWidget(4));
        assertEquals(1, engine.topOfWidget(4));
        assertEquals(0, engine.rightOfWidget(4));
        assertEquals(1, engine.bottomOfWidget(4));
    }
}
