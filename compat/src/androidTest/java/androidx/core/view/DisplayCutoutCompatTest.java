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

import static android.os.Build.VERSION.SDK_INT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.graphics.Rect;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DisplayCutoutCompatTest {

    DisplayCutoutCompat mCutoutTop;
    DisplayCutoutCompat mCutoutTopBottom;
    DisplayCutoutCompat mCutoutTopBottomClone;
    DisplayCutoutCompat mCutoutLeftRight;

    @Before
    public void setUp() throws Exception {
        mCutoutTop = new DisplayCutoutCompat(new Rect(0, 10, 0, 0), Arrays.asList(
                new Rect(50, 0, 60, 10)));
        mCutoutTopBottom = new DisplayCutoutCompat(new Rect(0, 10, 0, 20), Arrays.asList(
                new Rect(50, 0, 60, 10),
                new Rect(50, 100, 60, 120)));
        mCutoutTopBottomClone = new DisplayCutoutCompat(new Rect(0, 10, 0, 20), Arrays.asList(
                new Rect(50, 0, 60, 10),
                new Rect(50, 100, 60, 120)));
        mCutoutLeftRight = new DisplayCutoutCompat(new Rect(30, 0, 40, 0), Arrays.asList(
                new Rect(0, 50, 30, 60),
                new Rect(100, 60, 140, 50)));
    }

    @Test
    public void testSafeInsets() {
        if (SDK_INT >= 28) {
            assertEquals("left", 30, mCutoutLeftRight.getSafeInsetLeft());
            assertEquals("top", 10, mCutoutTopBottom.getSafeInsetTop());
            assertEquals("right", 40, mCutoutLeftRight.getSafeInsetRight());
            assertEquals("bottom", 20, mCutoutTopBottom.getSafeInsetBottom());
        } else {
            assertEquals("left", 0, mCutoutLeftRight.getSafeInsetLeft());
            assertEquals("top", 0, mCutoutTopBottom.getSafeInsetTop());
            assertEquals("right", 0, mCutoutLeftRight.getSafeInsetRight());
            assertEquals("bottom", 0, mCutoutTopBottom.getSafeInsetBottom());
        }
    }

    @Test
    public void testBoundingRects() {
        if (SDK_INT >= 28) {
            assertEquals(Arrays.asList(new Rect(50, 0, 60, 10)), mCutoutTop.getBoundingRects());
        } else {
            assertNull(mCutoutTop.getBoundingRects());
        }
    }

    @Test
    public void testEquals() {
        assertEquals(mCutoutTopBottomClone, mCutoutTopBottom);

        if (SDK_INT >= 28) {
            assertNotEquals(mCutoutTopBottom, mCutoutLeftRight);
        }
    }
    @Test
    public void testHashCode() {
        assertEquals(mCutoutTopBottomClone.hashCode(), mCutoutTopBottom.hashCode());
    }
}
