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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;
import android.os.Build;

import androidx.core.graphics.Insets;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DisplayCutoutCompatTest {
    private static final Rect ZERO_RECT = new Rect();
    DisplayCutoutCompat mCutoutTop;
    DisplayCutoutCompat mCutoutTopBottom;
    DisplayCutoutCompat mCutoutTopBottomClone;
    DisplayCutoutCompat mCutoutLeftRight;
    DisplayCutoutCompat mCutoutWaterfall;

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
        mCutoutWaterfall = new DisplayCutoutCompat(Insets.of(0, 20, 0, 20), ZERO_RECT, ZERO_RECT,
                ZERO_RECT, ZERO_RECT, Insets.of(0, 20, 0, 20));
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
            assertThat(mCutoutTop.getBoundingRects(), hasItem(new Rect(50, 0, 60, 10)));
        } else {
            assertThat(mCutoutTop.getBoundingRects(), empty());
        }
    }

    @Test
    public void testWaterfallInsets() {
        if (Build.VERSION.SDK_INT >= 30) {
            assertEquals(Insets.of(0, 20, 0, 20), mCutoutWaterfall.getWaterfallInsets());
        } else {
            assertEquals(Insets.NONE, mCutoutWaterfall.getWaterfallInsets());
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
