/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.core.view;

import static org.junit.Assert.assertEquals;

import android.graphics.Rect;
import android.os.Build;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.testutils.TestUtils;
import android.view.Gravity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GravityCompatTest {
    @Test
    public void testConstants() {
        // Compat constants must match core constants since they can be OR'd with
        // other core constants.
        assertEquals("Start constants", Gravity.START, GravityCompat.START);
        assertEquals("End constants", Gravity.END, GravityCompat.END);
    }

    @Test
    public void testGetAbsoluteGravity() {
        assertEquals("Left under LTR",
                GravityCompat.getAbsoluteGravity(Gravity.LEFT, ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.LEFT);
        assertEquals("Right under LTR",
                GravityCompat.getAbsoluteGravity(Gravity.RIGHT, ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.RIGHT);
        assertEquals("Left under RTL",
                GravityCompat.getAbsoluteGravity(Gravity.LEFT, ViewCompat.LAYOUT_DIRECTION_RTL),
                Gravity.LEFT);
        assertEquals("Right under RTL",
                GravityCompat.getAbsoluteGravity(Gravity.RIGHT, ViewCompat.LAYOUT_DIRECTION_RTL),
                Gravity.RIGHT);

        assertEquals("Start under LTR",
                GravityCompat.getAbsoluteGravity(GravityCompat.START,
                        ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.LEFT);
        assertEquals("End under LTR",
                GravityCompat.getAbsoluteGravity(GravityCompat.END,
                        ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.RIGHT);

        if (Build.VERSION.SDK_INT >= 17) {
            // The following tests are only expected to pass on v17+ devices
            assertEquals("Start under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.START,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.RIGHT);
            assertEquals("End under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.END,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.LEFT);
        } else {
            // And on older devices START is always LEFT, END is always RIGHT
            assertEquals("Start under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.START,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.LEFT);
            assertEquals("End under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.END,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.RIGHT);
        }
    }

    @Test
    public void testApplyNoOffsetsLtr() {
        Rect outRect = new Rect();

        // Left / top aligned under LTR direction
        GravityCompat.apply(Gravity.LEFT | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Left / top aligned under LTR: ",
                outRect, 0, 0, 100, 50);

        // Center / top aligned under LTR direction
        GravityCompat.apply(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Center / top aligned under LTR: ",
                outRect, 50, 0, 150, 50);

        // Right / top aligned under LTR direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Right / top aligned under LTR: ",
                outRect, 100, 0, 200, 50);

        // Left / center aligned under LTR direction
        GravityCompat.apply(Gravity.LEFT | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Left / center aligned under LTR: ",
                outRect, 0, 25, 100, 75);

        // Center / center aligned under LTR direction
        GravityCompat.apply(Gravity.CENTER, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Center / center aligned under LTR: ",
                outRect, 50, 25, 150, 75);

        // Right / center aligned under LTR direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Right / center aligned under LTR: ",
                outRect, 100, 25, 200, 75);

        // Left / bottom aligned under LTR direction
        GravityCompat.apply(Gravity.LEFT | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Left / bottom aligned under LTR: ",
                outRect, 0, 50, 100, 100);

        // Center / bottom aligned under LTR direction
        GravityCompat.apply(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Center / bottom aligned under LTR: ",
                outRect, 50, 50, 150, 100);

        // Right / bottom aligned under LTR direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Right / bottom aligned under LTR: ",
                outRect, 100, 50, 200, 100);

        // The following tests are expected to pass on all devices since START under LTR is LEFT
        // and END under LTR is RIGHT on pre-v17 and v17+ versions of the platform.

        // Start / top aligned under LTR direction
        GravityCompat.apply(GravityCompat.START | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Start / top aligned under LTR: ",
                outRect, 0, 0, 100, 50);

        // End / top aligned under LTR direction
        GravityCompat.apply(GravityCompat.END | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("End / top aligned under LTR: ",
                outRect, 100, 0, 200, 50);

        // Start / center aligned under LTR direction
        GravityCompat.apply(GravityCompat.START | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Start / center aligned under LTR: ",
                outRect, 0, 25, 100, 75);

        // End / center aligned under LTR direction
        GravityCompat.apply(GravityCompat.END | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("End / center aligned under LTR: ",
                outRect, 100, 25, 200, 75);

        // Start / bottom aligned under LTR direction
        GravityCompat.apply(GravityCompat.START | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("Start / bottom aligned under LTR: ",
                outRect, 0, 50, 100, 100);

        // End / bottom aligned under LTR direction
        GravityCompat.apply(GravityCompat.END | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        TestUtils.assertRectangleBounds("End / bottom aligned under LTR: ",
                outRect, 100, 50, 200, 100);
    }

    @Test
    public void testApplyNoOffsetsRtl() {
        Rect outRect = new Rect();

        // The following tests are expected to pass on all devices since they are using
        // Gravity constants that are not RTL-aware

        // Left / top aligned under RTL direction
        GravityCompat.apply(Gravity.LEFT | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Left / top aligned under RTL: ",
                outRect, 0, 0, 100, 50);

        // Center / top aligned under RTL direction
        GravityCompat.apply(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Center / top aligned under RTL: ",
                outRect, 50, 0, 150, 50);

        // Right / top aligned under RTL direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.TOP, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Right / top aligned under RTL: ",
                outRect, 100, 0, 200, 50);

        // Left / center aligned under RTL direction
        GravityCompat.apply(Gravity.LEFT | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Left / center aligned under RTL: ",
                outRect, 0, 25, 100, 75);

        // Center / center aligned under RTL direction
        GravityCompat.apply(Gravity.CENTER, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Center / center aligned under RTL: ",
                outRect, 50, 25, 150, 75);

        // Right / center aligned under RTL direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.CENTER_VERTICAL, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Right / center aligned under RTL: ",
                outRect, 100, 25, 200, 75);

        // Left / bottom aligned under RTL direction
        GravityCompat.apply(Gravity.LEFT | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Left / bottom aligned under RTL: ",
                outRect, 0, 50, 100, 100);

        // Center / bottom aligned under RTL direction
        GravityCompat.apply(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Center / bottom aligned under RTL: ",
                outRect, 50, 50, 150, 100);

        // Right / bottom aligned under RTL direction
        GravityCompat.apply(Gravity.RIGHT | Gravity.BOTTOM, 100, 50,
                new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
        TestUtils.assertRectangleBounds("Right / bottom aligned under RTL: ",
                outRect, 100, 50, 200, 100);


        if (Build.VERSION.SDK_INT >= 17) {
            // The following tests are only expected to pass on v17+ devices since START under
            // RTL is RIGHT and END under RTL is LEFT only on those devices.

            // Start / top aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.TOP, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / top aligned under RTL: ",
                    outRect, 100, 0, 200, 50);

            // End / top aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.TOP, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / top aligned under RTL: ",
                    outRect, 0, 0, 100, 50);

            // Start / center aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.CENTER_VERTICAL, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / center aligned under RTL: ",
                    outRect, 100, 25, 200, 75);

            // End / center aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.CENTER_VERTICAL, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / center aligned under RTL: ",
                    outRect, 0, 25, 100, 75);

            // Start / bottom aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.BOTTOM, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / bottom aligned under RTL: ",
                    outRect, 100, 50, 200, 100);

            // End / bottom aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.BOTTOM, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / bottom aligned under RTL: ",
                    outRect, 0, 50, 100, 100);
        } else {
            // And on older devices START is always LEFT, END is always RIGHT

            // Start / top aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.TOP, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / top aligned under RTL: ",
                    outRect, 0, 0, 100, 50);

            // End / top aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.TOP, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / top aligned under RTL: ",
                    outRect, 100, 0, 200, 50);

            // Start / center aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.CENTER_VERTICAL, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / center aligned under RTL: ",
                    outRect, 0, 25, 100, 75);

            // End / center aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.CENTER_VERTICAL, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / center aligned under RTL: ",
                    outRect, 100, 25, 200, 75);

            // Start / bottom aligned under RTL direction
            GravityCompat.apply(GravityCompat.START | Gravity.BOTTOM, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("Start / bottom aligned under RTL: ",
                    outRect, 0, 50, 100, 100);

            // End / bottom aligned under RTL direction
            GravityCompat.apply(GravityCompat.END | Gravity.BOTTOM, 100, 50,
                    new Rect(0, 0, 200, 100), outRect, ViewCompat.LAYOUT_DIRECTION_RTL);
            TestUtils.assertRectangleBounds("End / bottom aligned under RTL: ",
                    outRect, 100, 50, 200, 100);
        }
    }
}
