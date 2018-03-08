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

package androidx.wear.widget.util;

import static org.junit.Assert.assertEquals;

import android.graphics.RectF;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link ArcSwipe}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ArcSwipeTest {
    private ArcSwipe mArcSwipeUnderTest;
    private final RectF mFakeBounds = new RectF(0, 0, 400, 400);

    @Before
    public void setup() {
        mArcSwipeUnderTest = new ArcSwipe(ArcSwipe.Gesture.FAST_CLOCKWISE, mFakeBounds);
    }

    @Test
    public void testSweepAngleClockwise() {
        assertEquals(0, mArcSwipeUnderTest.getSweepAngle(0, 0, true), 0.0f);
        assertEquals(360, mArcSwipeUnderTest.getSweepAngle(0, 360, true), 0.0f);
        assertEquals(90, mArcSwipeUnderTest.getSweepAngle(0, 90, true), 0.0f);
        assertEquals(90, mArcSwipeUnderTest.getSweepAngle(90, 180, true), 0.0f);
        assertEquals(225, mArcSwipeUnderTest.getSweepAngle(45, 270, true), 0.0f);
        assertEquals(270, mArcSwipeUnderTest.getSweepAngle(90, 0, true), 0.0f);
        assertEquals(170, mArcSwipeUnderTest.getSweepAngle(280, 90, true), 0.0f);
    }

    @Test
    public void testSweepAngleAntiClockwise() {
        assertEquals(360, mArcSwipeUnderTest.getSweepAngle(0, 0, false), 0.0f);
        assertEquals(0, mArcSwipeUnderTest.getSweepAngle(0, 360, false), 0.0f);
        assertEquals(270, mArcSwipeUnderTest.getSweepAngle(0, 90, false), 0.0f);
        assertEquals(270, mArcSwipeUnderTest.getSweepAngle(90, 180, false), 0.0f);
        assertEquals(135, mArcSwipeUnderTest.getSweepAngle(45, 270, false), 0.0f);
        assertEquals(90, mArcSwipeUnderTest.getSweepAngle(90, 0, false), 0.0f);
        assertEquals(190, mArcSwipeUnderTest.getSweepAngle(280, 90, false), 0.0f);
    }

    @Test
    public void testGetAngle() {
        assertEquals(0, mArcSwipeUnderTest.getAngle(200, 0), 0.0f);
        assertEquals(90, mArcSwipeUnderTest.getAngle(400, 200), 0.0f);
        assertEquals(180, mArcSwipeUnderTest.getAngle(200, 400), 0.0f);
        assertEquals(270, mArcSwipeUnderTest.getAngle(0, 200), 0.0f);
    }
}
