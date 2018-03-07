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

package androidx.transition;

import static org.junit.Assert.fail;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SlideBadEdgeTest {

    private static final Object[][] sBadGravity = {
            {Gravity.AXIS_CLIP, "AXIS_CLIP"},
            {Gravity.AXIS_PULL_AFTER, "AXIS_PULL_AFTER"},
            {Gravity.AXIS_PULL_BEFORE, "AXIS_PULL_BEFORE"},
            {Gravity.AXIS_SPECIFIED, "AXIS_SPECIFIED"},
            {Gravity.AXIS_Y_SHIFT, "AXIS_Y_SHIFT"},
            {Gravity.AXIS_X_SHIFT, "AXIS_X_SHIFT"},
            {Gravity.CENTER, "CENTER"},
            {Gravity.CLIP_VERTICAL, "CLIP_VERTICAL"},
            {Gravity.CLIP_HORIZONTAL, "CLIP_HORIZONTAL"},
            {Gravity.CENTER_VERTICAL, "CENTER_VERTICAL"},
            {Gravity.CENTER_HORIZONTAL, "CENTER_HORIZONTAL"},
            {Gravity.DISPLAY_CLIP_VERTICAL, "DISPLAY_CLIP_VERTICAL"},
            {Gravity.DISPLAY_CLIP_HORIZONTAL, "DISPLAY_CLIP_HORIZONTAL"},
            {Gravity.FILL_VERTICAL, "FILL_VERTICAL"},
            {Gravity.FILL, "FILL"},
            {Gravity.FILL_HORIZONTAL, "FILL_HORIZONTAL"},
            {Gravity.HORIZONTAL_GRAVITY_MASK, "HORIZONTAL_GRAVITY_MASK"},
            {Gravity.NO_GRAVITY, "NO_GRAVITY"},
            {Gravity.RELATIVE_LAYOUT_DIRECTION, "RELATIVE_LAYOUT_DIRECTION"},
            {Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK, "RELATIVE_HORIZONTAL_GRAVITY_MASK"},
            {Gravity.VERTICAL_GRAVITY_MASK, "VERTICAL_GRAVITY_MASK"},
    };

    @Test
    public void testBadSide() {
        for (int i = 0; i < sBadGravity.length; i++) {
            int badEdge = (Integer) sBadGravity[i][0];
            String edgeName = (String) sBadGravity[i][1];
            try {
                new Slide(badEdge);
                fail("Should not be able to set slide edge to " + edgeName);
            } catch (IllegalArgumentException e) {
                // expected
            }

            try {
                Slide slide = new Slide();
                slide.setSlideEdge(badEdge);
                fail("Should not be able to set slide edge to " + edgeName);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

}
