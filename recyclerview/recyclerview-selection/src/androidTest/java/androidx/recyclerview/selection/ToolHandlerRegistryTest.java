/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;

import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ToolHandlerRegistryTest {

    private static final class Events {
        private static final MotionEvent ERASER = TestEvents.builder()
                .type(MotionEvent.TOOL_TYPE_ERASER)
                .build();
        private static final MotionEvent FINGER = TestEvents.builder()
                .type(MotionEvent.TOOL_TYPE_FINGER)
                .build();
        private static final MotionEvent MOUSE = TestEvents.builder()
                .type(MotionEvent.TOOL_TYPE_MOUSE)
                .build();
        private static final MotionEvent STYLUS = TestEvents.builder()
                .type(MotionEvent.TOOL_TYPE_STYLUS)
                .build();
        private static final MotionEvent UNKNOWN = TestEvents.builder()
                .type(MotionEvent.TOOL_TYPE_UNKNOWN)
                .build();
    }

    private static final class Handlers {
        private static final String DEFAULT = "default";
        private static final String ERASER = "eraser";
        private static final String FINGER = "finger";
        private static final String MOUSE = "mouse";
        private static final String STYLUS = "stylus";
        private static final String UNKNOWN = "unknown";
    }

    private ToolHandlerRegistry mRegistry;

    @Before
    public void setUp() {
        mRegistry = new ToolHandlerRegistry(Handlers.DEFAULT);
    }

    @Test
    public void testFallsBackToDefaultHandler() {
        assertEquals(Handlers.DEFAULT, mRegistry.get(Events.ERASER));
        assertEquals(Handlers.DEFAULT, mRegistry.get(Events.FINGER));
        assertEquals(Handlers.DEFAULT, mRegistry.get(Events.MOUSE));
        assertEquals(Handlers.DEFAULT, mRegistry.get(Events.STYLUS));
        assertEquals(Handlers.DEFAULT, mRegistry.get(Events.UNKNOWN));
    }

    @Test
    public void testGets() {
        assertGets(Events.ERASER, Handlers.ERASER);
        assertGets(Events.FINGER, Handlers.FINGER);
        assertGets(Events.MOUSE, Handlers.MOUSE);
        assertGets(Events.STYLUS, Handlers.STYLUS);
        assertGets(Events.UNKNOWN, Handlers.UNKNOWN);
    }

    private void assertGets(MotionEvent event, String handler) {
        mRegistry.set(event.getToolType(0), handler);
        assertEquals(handler, mRegistry.get(event));
    }
}
