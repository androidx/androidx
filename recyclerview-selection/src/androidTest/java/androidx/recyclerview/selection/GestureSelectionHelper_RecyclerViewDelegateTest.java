/*
 * Copyright 2017 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.selection.GestureSelectionHelper.RecyclerViewDelegate;
import androidx.recyclerview.selection.testing.TestEvents;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GestureSelectionHelper_RecyclerViewDelegateTest {

    // Simulate a (20, 20) box locating at (20, 20)
    static final int LEFT_BORDER = 20;
    static final int RIGHT_BORDER = 40;
    static final int TOP_BORDER = 20;
    static final int BOTTOM_BORDER = 40;

    @Test
    public void testLtrPastLastItem() {
        MotionEvent event = createEvent(100, 100);
        assertTrue(RecyclerViewDelegate.isPastLastItem(
                TOP_BORDER, LEFT_BORDER, RIGHT_BORDER, event, View.LAYOUT_DIRECTION_LTR));
    }

    @Test
    public void testLtrPastLastItem_Inverse() {
        MotionEvent event = createEvent(10, 10);
        assertFalse(RecyclerViewDelegate.isPastLastItem(
                TOP_BORDER, LEFT_BORDER, RIGHT_BORDER, event, View.LAYOUT_DIRECTION_LTR));
    }

    @Test
    public void testRtlPastLastItem() {
        MotionEvent event = createEvent(10, 30);
        assertTrue(RecyclerViewDelegate.isPastLastItem(
                TOP_BORDER, LEFT_BORDER, RIGHT_BORDER, event, View.LAYOUT_DIRECTION_RTL));
    }

    @Test
    public void testRtlPastLastItem_Inverse() {
        MotionEvent event = createEvent(100, 100);
        assertFalse(RecyclerViewDelegate.isPastLastItem(
                TOP_BORDER, LEFT_BORDER, RIGHT_BORDER, event, View.LAYOUT_DIRECTION_RTL));
    }

    private static MotionEvent createEvent(int x, int y) {
        return TestEvents.builder().action(MotionEvent.ACTION_MOVE).location(x, y).build();
    }
}
